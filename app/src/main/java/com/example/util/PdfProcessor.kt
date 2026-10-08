package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import com.example.data.remote.OcrChapterDto
import com.example.data.remote.OcrParagraphDto
import com.example.data.remote.OcrSectionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class ExtractedFormat(val code: String, val badgeLabel: String) {
    PDF("PDF", "PDF (Ephemeral OCR)"),
    EPUB("EPUB", "EPUB eBook (XHTML Spine)"),
    MARKDOWN("MARKDOWN", "Markdown (.md)"),
    PLAIN_TEXT("PLAIN_TEXT", "Plain Text (.txt)"),
    HTML("HTML", "HTML / Web Article"),
    P2P_DERIVATIVE_JSON("P2P_JSON", "Lipi P2P Derivative (.json)")
}

data class ImportedPdfInfo(
    val title: String,
    val file: File,
    val pageCount: Int,
    val fileSizeBytes: Long
)

/**
 * Zero-Host Peer-to-Peer Extraction Payload:
 * - For EPUB, TXT, Markdown, HTML, and P2P JSON, extraction occurs 100% in-memory (0 bytes written to disk).
 * - For PDF, an ephemeral scratch file in `cacheDir` is used only during page rendering/OCR and
 *   immediately deleted in `finally` via `purgeEphemeralFile()`.
 * - Lipi never hosts or retains the user's original certified/restricted file;
 *   Lipi owns and persists only the extracted structured OCR segments and translations.
 */
data class EphemeralExtractedDocument(
    val title: String,
    val authorOrSource: String,
    val format: ExtractedFormat,
    val detectedLanguage: String,
    val originalByteSize: Long,
    val unitCount: Int,
    val sourceFingerprintHash: String,
    val ephemeralPdfFile: File? = null,
    val extractedPages: List<DeterministicPageExtractionResult> = emptyList(),
    val extractionEngineSummary: String = "",
    val preExtractedChapters: List<OcrChapterDto> = emptyList(),
    val originalPurgedUriMarker: String
)

object PdfProcessor {

    fun purgeEphemeralFile(file: File?): Boolean {
        if (file == null) return true
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Multi-format P2P Ephemeral Extractor supporting:
     * - `.epub` (application/epub+zip)
     * - `.pdf` (application/pdf) -> Real per-page PDFBox Native Text + On-Device ML Kit OCR across ALL pages
     * - `.md` / `.markdown` (text/markdown)
     * - `.txt` (text/plain)
     * - `.html` / `.htm` / `.xhtml` (text/html, application/xhtml+xml)
     * - `.json` (application/json)
     */
    suspend fun extractDocumentEphemerally(
        context: Context,
        uri: Uri,
        onProgress: (stage: String, processedPages: Int, totalPages: Int) -> Unit = { _, _, _ -> }
    ): Result<EphemeralExtractedDocument> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var displayName = "Extracted_Knowledge_Work"
            val mimeType = contentResolver.getType(uri)?.lowercase().orEmpty()

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                }
            }

            val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to read selected file."))

            if (rawBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("The selected file is empty."))
            }

            val lowerName = displayName.lowercase()
            val isPdfMagic = rawBytes.size >= 4 &&
                rawBytes[0] == '%'.code.toByte() &&
                rawBytes[1] == 'P'.code.toByte() &&
                rawBytes[2] == 'D'.code.toByte() &&
                rawBytes[3] == 'F'.code.toByte()
            val isZipMagic = rawBytes.size >= 4 &&
                rawBytes[0] == 'P'.code.toByte() &&
                rawBytes[1] == 'K'.code.toByte()

            val format = when {
                lowerName.endsWith(".epub") || mimeType.contains("epub") || (isZipMagic && !isPdfMagic) ->
                    ExtractedFormat.EPUB
                lowerName.endsWith(".pdf") || mimeType.contains("pdf") || isPdfMagic ->
                    ExtractedFormat.PDF
                lowerName.endsWith(".md") || lowerName.endsWith(".markdown") || mimeType.contains("markdown") ->
                    ExtractedFormat.MARKDOWN
                lowerName.endsWith(".html") || lowerName.endsWith(".htm") || lowerName.endsWith(".xhtml") || mimeType.contains("html") ->
                    ExtractedFormat.HTML
                lowerName.endsWith(".json") || mimeType.contains("json") ->
                    ExtractedFormat.P2P_DERIVATIVE_JSON
                else ->
                    ExtractedFormat.PLAIN_TEXT
            }

            val cleanTitle = displayName
                .replace(Regex("\\.(pdf|epub|md|markdown|txt|html|htm|xhtml|json)$", RegexOption.IGNORE_CASE), "")
                .replace("_", " ")
                .trim()
                .ifBlank { "Extracted Knowledge Work" }

            when (format) {
                ExtractedFormat.EPUB -> {
                    Result.success(extractFromEpubBytes(rawBytes, cleanTitle))
                }
                ExtractedFormat.MARKDOWN,
                ExtractedFormat.PLAIN_TEXT,
                ExtractedFormat.HTML,
                ExtractedFormat.P2P_DERIVATIVE_JSON -> {
                    Result.success(extractFromTextOrHtmlBytes(rawBytes, cleanTitle, format))
                }
                ExtractedFormat.PDF -> {
                    val scratchDir = File(context.cacheDir, "ephemeral_p2p_scratch").apply { mkdirs() }
                    val scratchPdf = File(scratchDir, "ephemeral_${System.currentTimeMillis()}.pdf")
                    FileOutputStream(scratchPdf).use { it.write(rawBytes) }

                    val prelimFingerprint = SecurityVault.computeContentFingerprint(
                        "PDF_${cleanTitle}_${rawBytes.size}"
                    )

                    val deterministicResult = LocalOcrAndTranslationEngine.extractAllPagesFromPdfDeterministically(
                        context = context,
                        pdfFile = scratchPdf,
                        fallbackTitle = cleanTitle,
                        fingerprintPrefix = prelimFingerprint,
                        onProgress = onProgress
                    ).getOrElse { err ->
                        purgeEphemeralFile(scratchPdf)
                        return@withContext Result.failure(err)
                    }

                    val contentSampleForHash = deterministicResult.pages
                        .joinToString("|") { "${it.pageNumber}:${it.cleanNativeText.take(250)}" }
                    val fingerprint = SecurityVault.computeContentFingerprint(
                        "PDF_${deterministicResult.documentTitle}_${rawBytes.size}_${deterministicResult.totalPages}_$contentSampleForHash"
                    )

                    Result.success(
                        EphemeralExtractedDocument(
                            title = deterministicResult.documentTitle,
                            authorOrSource = deterministicResult.documentAuthor,
                            format = ExtractedFormat.PDF,
                            detectedLanguage = deterministicResult.detectedPrimaryLanguage,
                            originalByteSize = rawBytes.size.toLong(),
                            unitCount = deterministicResult.totalPages,
                            sourceFingerprintHash = fingerprint,
                            ephemeralPdfFile = scratchPdf,
                            extractedPages = deterministicResult.pages,
                            extractionEngineSummary = deterministicResult.primaryEngineSummary,
                            preExtractedChapters = deterministicResult.chapters,
                            originalPurgedUriMarker = "P2P_EPHEMERAL_PURGED://PDF/${fingerprint.take(16)}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses an `.epub` archive (ZIP container with OPF metadata & XHTML/HTML chapters)
     * 100% in memory without ever storing the `.epub` file on disk.
     */
    fun extractFromEpubBytes(
        rawBytes: ByteArray,
        fallbackTitle: String = "Extracted EPUB Edition"
    ): EphemeralExtractedDocument {
        var epubTitle = fallbackTitle
        var epubAuthor = "Peer-to-Peer EPUB Contributor"
        var epubLanguage = ""
        val htmlEntries = linkedMapOf<String, String>()

        ZipInputStream(ByteArrayInputStream(rawBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name
                if (!entry.isDirectory) {
                    val lower = entryName.lowercase()
                    if (lower.endsWith(".opf") ||
                        lower.endsWith(".xhtml") ||
                        lower.endsWith(".html") ||
                        lower.endsWith(".htm") ||
                        lower.endsWith(".xml")
                    ) {
                        val content = zis.readBytes().toString(StandardCharsets.UTF_8)
                        if (lower.endsWith(".opf")) {
                            extractXmlTagText(content, "dc:title")?.takeIf { it.isNotBlank() }?.let {
                                epubTitle = it
                            }
                            extractXmlTagText(content, "dc:creator")?.takeIf { it.isNotBlank() }?.let {
                                epubAuthor = "$it (P2P EPUB Extracted)"
                            }
                            extractXmlTagText(content, "dc:language")?.takeIf { it.isNotBlank() }?.let {
                                epubLanguage = it
                            }
                        } else if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                            if (!lower.contains("nav.") && !lower.contains("toc.")) {
                                htmlEntries[entryName] = content
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val chapters = mutableListOf<OcrChapterDto>()
        var virtualPage = 1

        htmlEntries.entries.forEachIndexed { idx, (entryPath, htmlContent) ->
            val parsedChapter = parseHtmlOrXhtmlIntoChapter(
                html = htmlContent,
                defaultChapterTitle = "Chapter ${idx + 1}: ${entryPath.substringAfterLast('/').substringBeforeLast('.')}",
                startingPageNumber = virtualPage
            )
            if (parsedChapter.sections.isNotEmpty()) {
                chapters.add(parsedChapter)
                virtualPage += parsedChapter.sections.size.coerceAtLeast(1)
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(
                OcrChapterDto(
                    chapterTitle = "Chapter 1: $epubTitle",
                    chapterSummary = "Extracted from EPUB container",
                    sections = listOf(
                        OcrSectionDto(
                            sectionHeading = "Section 1.1",
                            paragraphs = listOf(
                                OcrParagraphDto(
                                    pageNumber = 1,
                                    blockType = "PARAGRAPH",
                                    cleanText = "EPUB content extracted via Lipi Zero-Host P2P Extractor."
                                )
                            )
                        )
                    )
                )
            )
        }

        val allSampleText = chapters
            .flatMap { it.sections }
            .flatMap { it.paragraphs }
            .joinToString(" ") { it.cleanText }

        val resolvedLang = detectScriptLanguage(allSampleText, epubLanguage)
        val fingerprint = SecurityVault.computeContentFingerprint(
            "EPUB_${epubTitle}_${allSampleText.take(2000)}_${rawBytes.size}"
        )

        return EphemeralExtractedDocument(
            title = epubTitle,
            authorOrSource = epubAuthor,
            format = ExtractedFormat.EPUB,
            detectedLanguage = resolvedLang,
            originalByteSize = rawBytes.size.toLong(),
            unitCount = chapters.size,
            sourceFingerprintHash = fingerprint,
            ephemeralPdfFile = null,
            preExtractedChapters = chapters,
            originalPurgedUriMarker = "P2P_EPHEMERAL_PURGED://EPUB/${fingerprint.take(16)}"
        )
    }

    /**
     * Parses Markdown (.md), Plain Text (.txt), HTML (.html), or Lipi P2P JSON (.json)
     * into structured Chapters, Sections, and Paragraphs 100% in memory.
     */
    fun extractFromTextOrHtmlBytes(
        rawBytes: ByteArray,
        title: String,
        format: ExtractedFormat
    ): EphemeralExtractedDocument {
        val rawText = rawBytes.toString(StandardCharsets.UTF_8)
        val chapters = if (format == ExtractedFormat.HTML) {
            listOf(
                parseHtmlOrXhtmlIntoChapter(
                    html = rawText,
                    defaultChapterTitle = "Chapter 1: $title",
                    startingPageNumber = 1
                )
            )
        } else {
            parseMarkdownOrPlainTextIntoChapters(rawText, title)
        }

        val combinedText = chapters
            .flatMap { it.sections }
            .flatMap { it.paragraphs }
            .joinToString(" ") { it.cleanText }

        val detectedLang = detectScriptLanguage(combinedText, "")
        val fingerprint = SecurityVault.computeContentFingerprint(
            "${format.code}_${title}_${combinedText.take(2000)}"
        )

        return EphemeralExtractedDocument(
            title = title,
            authorOrSource = "Peer-to-Peer ${format.badgeLabel} Extraction",
            format = format,
            detectedLanguage = detectedLang,
            originalByteSize = rawBytes.size.toLong(),
            unitCount = chapters.sumOf { it.sections.size }.coerceAtLeast(1),
            sourceFingerprintHash = fingerprint,
            ephemeralPdfFile = null,
            preExtractedChapters = chapters,
            originalPurgedUriMarker = "P2P_EPHEMERAL_PURGED://${format.code}/${fingerprint.take(16)}"
        )
    }

    /**
     * Generates a standards-compliant sample `.epub` binary in memory ("Kathopanishad & Knowledge Sutra")
     * so users and automated tests can verify real `.epub` ZIP/OPF/XHTML extraction with a single tap.
     */
    fun createSampleEpubBytes(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // 1. mimetype
            zos.putNextEntry(ZipEntry("mimetype"))
            zos.write("application/epub+zip".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. META-INF/container.xml
            zos.putNextEntry(ZipEntry("META-INF/container.xml"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 3. OEBPS/content.opf
            zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>Kathopanishad: Dialogue on Immortal Knowledge (P2P EPUB)</dc:title>
                    <dc:creator>Upanishadic Rishis • Lipi P2P Commons</dc:creator>
                    <dc:language>sa</dc:language>
                  </metadata>
                  <manifest>
                    <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="ch1"/>
                    <itemref idref="ch2"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 4. OEBPS/chapter1.xhtml
            zos.putNextEntry(ZipEntry("OEBPS/chapter1.xhtml"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Adhyaya 1: Nachiketa's Quest for Vidya</title></head>
                <body>
                  <h1>Adhyaya 1: The Distinction Between Shreyas (Wisdom) and Preyas (Pleasure)</h1>
                  <h2>Valli 1.2: Choosing Immortal Knowledge</h2>
                  <p>श्रेयश्च प्रेयश्च मनुष्यमेतः तौ सम्परीत्य विविनक्ति धीरः। श्रेयो हि धीरोऽभिप्रेयसो वृणीते प्रेयो मन्दो योगक्षेमाद्वृणीते॥</p>
                  <p>Both the good (Shreyas — enduring knowledge) and the pleasant (Preyas — fleeting gratification) approach a human being. The wise seeker examines both and chooses Shreyas over Preyas.</p>
                  <h2>Valli 1.3: The Chariot Analogy of Self-Mastery</h2>
                  <p>आत्मानं रथिनं विद्धि शरीरं रथमेव तु। बुद्धिं तु सारथिं विद्धि मनः प्रग्रहमेव च॥</p>
                  <p>Know the Atman as the lord of the chariot, the body as the chariot itself, the discriminative intellect (Buddhi) as the charioteer, and the mind as the reins.</p>
                </body>
                </html>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()

            // 5. OEBPS/chapter2.xhtml
            zos.putNextEntry(ZipEntry("OEBPS/chapter2.xhtml"))
            zos.write(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head><title>Adhyaya 2: The Supreme Light of Consciousness</title></head>
                <body>
                  <h1>Adhyaya 2: Awake, Arise, and Attain Knowledge</h1>
                  <h2>Valli 2.1: उत्तिष्ठत जाग्रत (Uttishthata Jagrata)</h2>
                  <p>उत्तिष्ठत जाग्रत प्राप्य वरान्निबोधत। क्षुरस्य धारा निशिता दुरत्यया दुर्गं पथस्तत्कवयो वदन्ति॥</p>
                  <p>Arise! Awake! Approach the illumined teachers and realize that supreme knowledge. The sages declare that the path is as sharp as a razor's edge, yet liberating for the steadfast seeker.</p>
                </body>
                </html>
                """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            )
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun extractXmlTagText(xml: String, tagName: String): String? {
        val regex = Regex("<$tagName[^>]*>(.*?)</$tagName>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val match = regex.find(xml) ?: return null
        return decodeHtmlEntities(match.groupValues[1].replace(Regex("<[^>]+>"), "").trim())
    }

    private fun parseHtmlOrXhtmlIntoChapter(
        html: String,
        defaultChapterTitle: String,
        startingPageNumber: Int
    ): OcrChapterDto {
        val h1Match = Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        val titleMatch = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        val chapterTitle = decodeHtmlEntities(
            (h1Match?.groupValues?.get(1) ?: titleMatch?.groupValues?.get(1) ?: defaultChapterTitle)
                .replace(Regex("<[^>]+>"), "")
                .trim()
        ).ifBlank { defaultChapterTitle }

        val sections = mutableListOf<OcrSectionDto>()
        var currentHeading = "Section 1"
        var currentParagraphs = mutableListOf<OcrParagraphDto>()
        var pageCursor = startingPageNumber

        val blockRegex = Regex("<(h2|h3|p|blockquote|li)[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = blockRegex.findAll(html).toList()

        if (matches.isNotEmpty()) {
            for (m in matches) {
                val tag = m.groupValues[1].lowercase()
                val text = decodeHtmlEntities(m.groupValues[2].replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim())
                if (text.isBlank()) continue

                if (tag == "h2" || tag == "h3") {
                    if (currentParagraphs.isNotEmpty()) {
                        sections.add(OcrSectionDto(sectionHeading = currentHeading, paragraphs = currentParagraphs.toList()))
                        currentParagraphs = mutableListOf()
                        pageCursor++
                    }
                    currentHeading = text
                } else {
                    val blockType = if (tag == "blockquote" || text.contains("॥")) "VERSE" else "PARAGRAPH"
                    currentParagraphs.add(
                        OcrParagraphDto(
                            pageNumber = pageCursor,
                            blockType = blockType,
                            cleanText = text
                        )
                    )
                }
            }
            if (currentParagraphs.isNotEmpty()) {
                sections.add(OcrSectionDto(sectionHeading = currentHeading, paragraphs = currentParagraphs.toList()))
            }
        } else {
            val stripped = decodeHtmlEntities(
                html.replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
                    .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
                    .replace(Regex("<[^>]+>"), "\n")
            )
            val paras = stripped.split(Regex("\\n\\s*\\n"))
                .map { it.trim().replace(Regex("\\s+"), " ") }
                .filter { it.length > 2 }
                .map {
                    OcrParagraphDto(
                        pageNumber = startingPageNumber,
                        blockType = if (it.contains("॥")) "VERSE" else "PARAGRAPH",
                        cleanText = it
                    )
                }
            if (paras.isNotEmpty()) {
                sections.add(OcrSectionDto(sectionHeading = "Section 1", paragraphs = paras))
            }
        }

        return OcrChapterDto(
            chapterTitle = chapterTitle,
            chapterSummary = "Extracted via Zero-Host P2P Parser",
            sections = sections
        )
    }

    private fun parseMarkdownOrPlainTextIntoChapters(
        rawText: String,
        defaultTitle: String
    ): List<OcrChapterDto> {
        val lines = rawText.lines()
        val chapters = mutableListOf<OcrChapterDto>()
        var currentChapterTitle = "Chapter 1: $defaultTitle"
        var currentSectionHeading = "Section 1.1"
        var currentSections = mutableListOf<OcrSectionDto>()
        var currentParagraphs = mutableListOf<OcrParagraphDto>()
        val paragraphBuffer = StringBuilder()
        var virtualPage = 1

        fun flushParagraph() {
            val text = paragraphBuffer.toString().trim()
            if (text.isNotEmpty()) {
                currentParagraphs.add(
                    OcrParagraphDto(
                        pageNumber = virtualPage,
                        blockType = if (text.contains("॥")) "VERSE" else "PARAGRAPH",
                        cleanText = text
                    )
                )
                paragraphBuffer.clear()
            }
        }

        fun flushSection() {
            flushParagraph()
            if (currentParagraphs.isNotEmpty()) {
                currentSections.add(
                    OcrSectionDto(
                        sectionHeading = currentSectionHeading,
                        paragraphs = currentParagraphs.toList()
                    )
                )
                currentParagraphs = mutableListOf()
                virtualPage++
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("# ") -> {
                    flushSection()
                    if (currentSections.isNotEmpty()) {
                        chapters.add(
                            OcrChapterDto(
                                chapterTitle = currentChapterTitle,
                                chapterSummary = "Structured Markdown/Text Chapter",
                                sections = currentSections.toList()
                            )
                        )
                        currentSections = mutableListOf()
                    }
                    currentChapterTitle = trimmed.removePrefix("# ").trim().ifBlank { currentChapterTitle }
                    currentSectionHeading = "Section 1"
                }
                trimmed.startsWith("## ") || trimmed.startsWith("### ") -> {
                    flushSection()
                    currentSectionHeading = trimmed.replace(Regex("^#+\\s*"), "").trim().ifBlank { "Section" }
                }
                trimmed.isEmpty() -> {
                    flushParagraph()
                }
                else -> {
                    if (paragraphBuffer.isNotEmpty()) paragraphBuffer.append(" ")
                    paragraphBuffer.append(trimmed)
                }
            }
        }

        flushSection()
        if (currentSections.isNotEmpty()) {
            chapters.add(
                OcrChapterDto(
                    chapterTitle = currentChapterTitle,
                    chapterSummary = "Extracted via Lipi P2P Text/Markdown Engine",
                    sections = currentSections.toList()
                )
            )
        }

        if (chapters.isEmpty()) {
            chapters.add(
                OcrChapterDto(
                    chapterTitle = "Chapter 1: $defaultTitle",
                    sections = listOf(
                        OcrSectionDto(
                            sectionHeading = "Section 1.1",
                            paragraphs = listOf(
                                OcrParagraphDto(1, "PARAGRAPH", rawText.trim().ifBlank { "Extracted text document." })
                            )
                        )
                    )
                )
            )
        }
        return chapters
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
    }

    private fun detectScriptLanguage(sampleText: String, metadataLang: String): String {
        val meta = metadataLang.lowercase().trim()
        if (meta == "sa" || meta == "san") return "Sanskrit"
        if (meta == "gu" || meta == "guj") return "Gujarati"
        if (meta == "hi" || meta == "hin") return "Hindi"
        if (meta == "ta" || meta == "tam") return "Tamil"
        if (meta == "te" || meta == "tel") return "Telugu"
        if (meta == "bn" || meta == "ben") return "Bengali"

        for (ch in sampleText) {
            val code = ch.code
            when (code) {
                in 0x0900..0x097F -> return if (sampleText.contains("॥") || sampleText.contains("ः")) "Sanskrit" else "Hindi"
                in 0x0A80..0x0AFF -> return "Gujarati"
                in 0x0980..0x09FF -> return "Bengali"
                in 0x0B80..0x0BFF -> return "Tamil"
                in 0x0C00..0x0C7F -> return "Telugu"
                in 0x0C80..0x0CFF -> return "Kannada"
                in 0x0D00..0x0D7F -> return "Malayalam"
                in 0x0A00..0x0A7F -> return "Punjabi"
                in 0x0B00..0x0B7F -> return "Odia"
            }
        }
        return "English"
    }

    suspend fun importPdfFromUri(context: Context, uri: Uri): Result<ImportedPdfInfo> =
        withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var displayName = "Ingested_Book.pdf"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }

                val cleanTitle = displayName
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")
                    .replace("_", " ")
                val pdfDir = File(context.cacheDir, "ephemeral_p2p_scratch").apply { mkdirs() }
                val destFile = File(pdfDir, "ephemeral_${System.currentTimeMillis()}.pdf")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read selected PDF file."))

                val pageCount = getPdfPageCount(destFile)
                if (pageCount <= 0) {
                    destFile.delete()
                    return@withContext Result.failure(IllegalArgumentException("The selected PDF has no readable pages."))
                }

                Result.success(
                    ImportedPdfInfo(
                        title = cleanTitle,
                        file = destFile,
                        pageCount = pageCount,
                        fileSizeBytes = destFile.length()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun getPdfPageCount(file: File): Int {
        if (!file.exists()) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    suspend fun renderPageToBitmap(
        file: File,
        pageIndex: Int,
        maxDimension: Int = 1350
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val ratio = page.width.toFloat() / page.height.toFloat().coerceAtLeast(1f)
                        val width: Int
                        val height: Int
                        if (ratio >= 1f) {
                            width = maxDimension
                            height = (maxDimension / ratio).toInt().coerceAtLeast(1)
                        } else {
                            height = maxDimension
                            width = (maxDimension * ratio).toInt().coerceAtLeast(1)
                        }

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Generates a reference source PDF file for the built-in Public-Domain Sanskrit/Indic Wisdom Anthology.
     */
    suspend fun createReferenceAnthologyPdf(context: Context): File = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "ingested_pdfs").apply { mkdirs() }
        val file = File(pdfDir, "bharat_gyan_sangraha_reference.pdf")
        if (file.exists() && file.length() > 0) return@withContext file

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 612
            val pageHeight = 792

            val pages = listOf(
                 Triple(
                    "CHAPTER 1 • PAGE 1 • SANSKRIT & CIVIC WISDOM",
                    "अध्याय १: विद्यायाः महत्त्वम् (The Glory of Knowledge)",
                    listOf(
                        "Section 1.1: Subhashita Ratnani (Classical Sanskrit Verses)" to
                            "विद्या ददाति विनयं विनयाद् याति पात्रताम्।\nपात्रत्वाद्धनमाप्नोति धनाद्धर्मं ततः सुखम्॥ १ ॥\n\n" +
                            "स्वदेशे पूज्यते राजा विद्वान् सर्वत्र पूज्यते।\nन चौरहार्यं न च राजहार्यं न भ्रातृभाज्यं न च भारकारि।",
                        "Section 1.2: Universal Harmony (Vasudhaiva Kutumbakam)" to
                            "अयं निजः परो वेति गणना लघुचेतसाम्।\nउदारचरितानां तु वसुधैव कुटुम्बकम्॥ २ ॥\n\n" +
                            "सर्वे भवन्तु सुखिनः सर्वे सन्तु निरामयाः।\nसर्वे भद्राणि पश्यन्तु मा कश्चिद्दुःखभाग्भवेत्॥"
                    )
                ),
                Triple(
                    "CHAPTER 2 • PAGE 2 • AYURVEDA & DAILY WELLNESS",
                    "अध्याय २: आयुर्वेदः स्वास्थ्यसूत्रम् (Foundations of Holistic Health)",
                    listOf(
                        "Section 2.1: Dinacharya & Mindful Living" to
                            "समदोषः समाग्निश्च समधातुमलक्रियः।\nप्रसन्नात्मेन्द्रियमनाः स्वस्थ इत्यभिधीयते॥\n\n" +
                            "A balanced state of biological humors (Doshas), digestive fire (Agni), tissues (Dhatus), and serene senses defines true health according to Acharya Sushruta.",
                        "Section 2.2: Seasonal Nourishment (Ritucharya)" to
                            "आहारशुद्धौ सत्त्वशुद्धिः सत्त्वशुद्धौ ध्रुवा स्मृतिः।\nWholesome, regionally grown nourishment purifies mind and memory, strengthening resilience across changing seasons."
                    )
                ),
                Triple(
                    "CHAPTER 3 • PAGE 3 • DIGITAL MULTILINGUAL INDIA",
                    "अध्याय ३: भारतीय भाषा संगमः (Knowledge in Every Mother Tongue)",
                    listOf(
                        "Section 3.1: Preserving Heritage Through Persistent OCR" to
                            "India's literary heritage spans Sanskrit, Gujarati, Hindi, Marathi, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Odia, Assamese, and Urdu. Converting static PDFs and EPUBs into structured chapters and sections allows every citizen to read and listen in their preferred language.",
                        "Section 3.2: Synchronized Bilingual Learning" to
                            "When original texts and translations are aligned paragraph-by-paragraph with native voice synthesis, students and researchers can compare terminology effortlessly without hosting restricted files."
                    )
                )
            )

            pages.forEachIndexed { idx, (badge, title, sections) ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val bgPaint = Paint().apply { color = Color.parseColor("#FAF8F5") }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                val headerPaint = Paint().apply { color = Color.parseColor("#0F172A") }
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 105f, headerPaint)

                val accentPaint = Paint().apply { color = Color.parseColor("#F59E0B") }
                canvas.drawRect(0f, 105f, pageWidth.toFloat(), 110f, accentPaint)

                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FBBF24")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText(badge, 36f, 36f, badgePaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 17f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText(title, 36f, 70f, titlePaint)

                val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 14f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 12.5f
                }
                val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

                var y = 145f
                sections.forEach { (secHeading, body) ->
                    val rect = RectF(36f, y, pageWidth - 36f, y + 210f)
                    canvas.drawRoundRect(rect, 10f, 10f, boxPaint)
                    canvas.drawText(secHeading, 52f, y + 30f, secPaint)
                    var lineY = y + 60f
                    body.split("\n").forEach { line ->
                        canvas.drawText(line.take(75), 52f, lineY, bodyPaint)
                        lineY += 22f
                    }
                    y += 235f
                }

                pdfDocument.finishPage(page)
            }

            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            pdfDocument.close()
        } catch (_: Exception) {
        }
        file
    }
}
