package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.data.local.CorpusAnalyticsSummary
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.ProcessingJobEntity
import com.example.data.local.SubscriptionTier
import com.example.data.local.TranslationIssueReportEntity
import com.example.data.local.TranslationMemoryEntity
import com.example.data.local.UserEntitlementEntity
import com.example.data.local.UserEntity

enum class CorpusHubSection(val label: String) {
    OVERVIEW_ANALYTICS("Reuse & Analytics"),
    CANONICAL_REPOSITORY("Shared Translations"),
    TRANSLATION_MEMORY("Translation Memory (TM)"),
    QUALITY_AND_RIGHTS_CMS("Editorial & Rights CMS"),
    BACKGROUND_JOBS("Job Queue"),
    LAUNCH_DEPLOY_MONETIZE("Git, Deploy & Monetize")
}

/**
 * Requirements #1, #2, #6, #7, #8, #9, #10, #20, #21:
 * Shared Translation Repository, Segment Translation Memory, Editorial Quality & Rights CMS,
 * Persistent Background Job Queue, Corpus Reuse Analytics Dashboard, and Founder's Launchpad.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedCorpusAndGovernanceHubScreen(
    currentUser: UserEntity?,
    activeEntitlements: List<UserEntitlementEntity>,
    summary: CorpusAnalyticsSummary,
    allBooks: List<BookEntity>,
    canonicalTranslations: List<ParagraphTranslationEntity>,
    translationMemoryEntries: List<TranslationMemoryEntity>,
    issueReports: List<TranslationIssueReportEntity>,
    processingJobs: List<ProcessingJobEntity>,
    onResolveIssueReport: (Long, String) -> Unit,
    onOpenPlansDialog: () -> Unit,
    onOpenBookInReader: (BookEntity) -> Unit,
    onExtractAnyFileClick: () -> Unit = {},
    onExtractSampleEpubClick: () -> Unit = {}
) {
    var activeSection by remember { mutableStateOf(CorpusHubSection.OVERVIEW_ANALYTICS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Plan Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VidyaKosha Commons & Editorial CMS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Zero-Host P2P OCR & Translation Derivatives • Segment Memory • Rights, Git & Monetization",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onOpenPlansDialog,
                modifier = Modifier.testTag("corpus_open_plans_button")
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(currentUser?.activeTierCode ?: "FREE")
            }
        }

        // Section Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CorpusHubSection.entries.forEach { sec ->
                FilterChip(
                    selected = activeSection == sec,
                    onClick = { activeSection = sec },
                    label = { Text(sec.label) },
                    modifier = Modifier.testTag("corpus_tab_${sec.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            when (activeSection) {
                CorpusHubSection.OVERVIEW_ANALYTICS -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Compounding Multilingual Library Asset",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "Every eligible translation is permanently versioned by source hash, language, provider, and license policy—Checked before every AI request so no passage is ever translated twice unnecessarily.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // Key Metrics Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricStatCard(
                                    title = "Canonical Translations",
                                    value = "${summary.totalCanonicalTranslations}",
                                    subtitle = "Permanent versioned records",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "AI Calls Avoided",
                                    value = "${summary.aiCallsAvoidedByReuse}",
                                    subtitle = "Reused from Shared Repo & TM",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricStatCard(
                                    title = "Tokens Saved",
                                    value = "${summary.estimatedTokensSaved}",
                                    subtitle = "Zero-cost instant retrieval",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "Translation Memory",
                                    value = "${summary.totalTranslationMemorySegments}",
                                    subtitle = "Reusable cross-book segments",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricStatCard(
                                    title = "Verified / Approved",
                                    value = "${summary.expertVerifiedTranslationsCount}",
                                    subtitle = "Human & Expert Verified",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricStatCard(
                                    title = "Books & Open Issues",
                                    value = "${summary.totalBooksInCatalog} Books • ${summary.openQualityReportsCount} Issues",
                                    subtitle = "${summary.activeOrCompletedJobsCount} Pipeline Jobs",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Language Coverage Breakdown
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Active Canonical Translations by Language",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (summary.languageCoverageBreakdown.isEmpty()) {
                                    Text(
                                        text = "No translations recorded yet.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        summary.languageCoverageBreakdown.entries.forEach { (lang, count) ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "$lang: $count passages",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CorpusHubSection.CANONICAL_REPOSITORY -> {
                    item {
                        Text(
                            text = "Permanent Shared Translation Repository (${canonicalTranslations.size} records)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Translations are never deleted and never treated as temporary cache. Each record preserves source content hash, provider, model, prompt version, contributor provenance, and access policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(canonicalTranslations.take(40), key = { "canon_${it.id}" }) { trans ->
                        val bookTitle = allBooks.find { it.id == trans.bookId }?.title ?: "Book #${trans.bookId}"
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$bookTitle • ${trans.sourceLanguage} → ${trans.targetLanguage}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReviewStatusBadge(status = trans.reviewStatus, versionNumber = trans.versionNumber)
                                }
                                Text(
                                    text = trans.translatedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Model: ${trans.providerUsed} (${trans.modelUsed} ${trans.modelVersion}) • Prompt: ${trans.promptVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Access: ${trans.accessPolicy} • Rights: ${trans.commercialRightsStatus} • Hash: ${trans.sourceContentHash.take(12)}… • Reused ${trans.reuseCount}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                CorpusHubSection.TRANSLATION_MEMORY -> {
                    item {
                        Text(
                            text = "Segment-Level Translation Memory (${translationMemoryEntries.size} segments)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Normalized source sentences, shlokas, and recurring phrases matched across books by SHA-256 hash so identical segments are reused automatically across the entire library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(translationMemoryEntries, key = { "tm_${it.id}" }) { tm ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${tm.sourceLanguage} → ${tm.targetLanguage} • Hash ${tm.normalizedSourceHash.take(10)}…",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Reused ${tm.matchReuseCount}x • ${tm.reviewStatus}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Source: ${tm.canonicalSourceText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Target: ${tm.translatedText}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (tm.notesOrGlossary.isNotBlank()) {
                                    Text(
                                        text = "Glossary/Notes: ${tm.notesOrGlossary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                CorpusHubSection.QUALITY_AND_RIGHTS_CMS -> {
                    item {
                        Text(
                            text = "Publisher & Editorial Governance CMS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage Book Access Policies (PUBLIC, FREE, PAID, RESTRICTED, PRIVATE, PUBLISHER_ONLY), Rights Categories, and Reader-Reported Translation Issues.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "Open & Resolved Quality Issue Queue (${issueReports.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (issueReports.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No open translation quality issues reported. Readers can flag terminology, sandhi, or OCR issues from any paragraph card.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(issueReports, key = { "issue_${it.id}" }) { report ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${report.issueCategory} • ${report.targetLanguage} (Paragraph #${report.paragraphId})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = report.resolutionStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (report.resolutionStatus == "OPEN") {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = report.issueDescription,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (report.suggestedCorrection.isNotBlank()) {
                                        Text(
                                            text = "Suggested Fix: ${report.suggestedCorrection}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (report.resolutionStatus == "OPEN") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onResolveIssueReport(report.id, "RESOLVED_NEW_VERSION") },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Mark Resolved", style = MaterialTheme.typography.labelSmall)
                                            }
                                            OutlinedButton(
                                                onClick = { onResolveIssueReport(report.id, "APPROVED_AS_IS") },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Approve As-Is", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Book Rights & Access Catalogue Audit (${allBooks.size} books)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(allBooks, key = { "cms_book_${it.id}" }) { book ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = book.accessPolicy,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Rights: ${book.rightsCategory} • Publisher: ${book.publisherName} • OCR v${book.currentOcrVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Reads: ${book.readCountAnalytics} • Audio Listens: ${book.listenCountAnalytics} • ${book.totalChapters} Ch / ${book.totalSections} Sec / ${book.totalParagraphs} Para",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                CorpusHubSection.BACKGROUND_JOBS -> {
                    item {
                        Text(
                            text = "Persistent Processing Job Queue (${processingJobs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Asynchronous pipeline tracking OCR ingestion, shared translation lookups, re-translations, TTS synthesis, and full-text search indexing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(processingJobs, key = { "job_${it.id}" }) { job ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "[${job.jobType}] ${job.bookTitle}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${job.status} (${job.progressPercent}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = job.statusDetail,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Engine: ${job.providerAndModel} • Target: ${job.targetLanguage.ifBlank { "All" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                CorpusHubSection.LAUNCH_DEPLOY_MONETIZE -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "1. Zero-Host Peer-to-Peer Extraction & Derivative IP Model",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Formats Supported: PDF (.pdf), EPUB (.epub), Markdown (.md), Plain Text (.txt), HTML (.html/.xhtml), and VidyaKosha P2P JSON.\n" +
                                        "• Zero Original Hosting: Any user converts their own document ephemerally. The original binary is purged immediately (0 bytes hosted on server).\n" +
                                        "• Derivative Ownership (P2P_DERIVATIVE_OWNED): VidyaKosha owns and permanently retains the converted structured OCR segments, SHA-256 content hashes, and canonical translations—never the original certified/restricted file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onExtractAnyFileClick,
                                        modifier = Modifier.testTag("launchpad_extract_file_button")
                                    ) {
                                        Text("Extract Local File (PDF/EPUB/TXT)")
                                    }
                                    OutlinedButton(
                                        onClick = onExtractSampleEpubClick,
                                        modifier = Modifier.testTag("launchpad_extract_sample_epub_button")
                                    ) {
                                        Text("Test P2P EPUB")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "2. Private Git Repository & Version Control",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Local Git Initialized: Workspace repository is initialized with a strict .gitignore excluding .env, local.properties, debug.keystore, and build outputs.\n" +
                                        "• Push to Private GitHub from AI Studio: Open Settings (top-right gear icon) → Export to GitHub → Select 'Private Repository' (e.g. vidyakosha-core).\n" +
                                        "• Or via GitHub CLI after ZIP Export:\n" +
                                        "  gh repo create vidyakosha --private --source=. --remote=origin --push",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "3. How to Deploy (Android + Zero-Host Derivative Cloud)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• Android App (Google Play): Use AI Studio Settings → Generate Signed AAB/APK → Upload to Google Play Console (Internal Testing → Production).\n" +
                                        "• Shared Derivative Backend (Cloud Run + PostgreSQL / Supabase): Sync only `paragraphs` (normalized text + SHA-256 `sourceContentHash`), `paragraph_translations`, and `translation_memory`. Because original PDFs/EPUBs are never uploaded, you need 0 object-storage buckets for user documents and avoid hosting copyrighted binaries.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "4. How to Charge for Premium Content & Derivative Assets",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "• What You Monetize: Never charge for raw third-party restricted files. Charge for VidyaKosha-owned value-added derivatives: Expert/Publisher-Verified Translations (`PAID_USERS`), Scholarly Word-by-Word Pada Commentary, Cross-Book Research (`PRO_RESEARCH` $29/mo), Offline Sync & Enhanced TTS (`READER` $12/mo), and University/Seat Licences (`INSTITUTION_MEMBERSHIP`).\n" +
                                        "• Payment Gateways: Connect Google Play Billing Library (Android in-app subscriptions) and Stripe / Razorpay (UPI, Cards, NetBanking) webhooks directly to the `user_entitlements` table (`SUBSCRIPTION_TIER`, `BOOK_PURCHASE`, `INSTITUTION_MEMBERSHIP`).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = onOpenPlansDialog) {
                                    Text("Preview Active Entitlement & Pricing Tiers")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Requirement #3, #4, #18 & #19: Dynamic Backend-Driven Subscription Tiers, Individual Book Entitlements & Institutional Membership Dialog.
 * Zero hard-coded plan prices or feature permissions—retrieves current plan configuration from the backend database.
 */
@Composable
fun SubscriptionTiersAndEntitlementsDialog(
    currentUser: UserEntity?,
    activeEntitlements: List<UserEntitlementEntity>,
    commercialPlans: List<com.example.data.local.CommercialPlanEntity> = emptyList(),
    onSelectTier: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var institutionCodeInput by remember {
        mutableStateOf(currentUser?.institutionCode.orEmpty())
    }

    val currentTier = currentUser?.activeTierCode ?: SubscriptionTier.FREE
    val enabledPlans = remember(commercialPlans) {
        commercialPlans.filter { it.isEnabled }.sortedBy { it.displayOrder }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Plans, Book Rights & Institutional Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Live Backend Plan Configuration: Prices, features, trial periods, and usage limits are retrieved dynamically from the platform backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (enabledPlans.isEmpty()) {
                    Text(
                        text = "Loading active commercial plans from backend...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    enabledPlans.forEach { plan ->
                        val priceLabel = plan.formatPriceSummary()
                        val trialBadge = if (plan.trialDays > 0) " • ${plan.trialDays}-day trial" else ""
                        val featuredBadge = if (plan.isFeatured) " ★ Recommended" else ""
                        val formattedFeatures = buildString {
                            if (plan.description.isNotBlank()) {
                                append(plan.description).append("\n")
                            }
                            plan.featuresCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { feat ->
                                append("• ").append(feat).append("\n")
                            }
                            append("• Access Policies: ").append(plan.allowedBookAccessPoliciesCsv).append("\n")
                            append("• Limits: Translations/day=")
                            append(if (plan.dailyTranslationLimit < 0) "Unlimited" else plan.dailyTranslationLimit)
                            append(", Offline=")
                            append(if (plan.offlineAccessEnabled) "Yes" else "No")
                            append(", Adv Search=")
                            append(if (plan.advancedSearchAndScholarlyEnabled) "Yes" else "No")
                        }

                        TierOptionCard(
                            tierCode = plan.planCode,
                            title = "${plan.name} ($priceLabel$trialBadge)$featuredBadge",
                            features = formattedFeatures.trim(),
                            isCurrent = currentTier.equals(plan.planCode, ignoreCase = true),
                            onSelect = { onSelectTier(plan.planCode, institutionCodeInput) }
                        )
                    }
                }

                OutlinedTextField(
                    value = institutionCodeInput,
                    onValueChange = { institutionCodeInput = it },
                    label = { Text("Institution Access Code (Optional, e.g. INST_BHARAT_VIDYA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (activeEntitlements.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Your Active Entitlement Grants (${activeEntitlements.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    activeEntitlements.forEach { ent ->
                        Text(
                            text = "✓ [${ent.entitlementKind}] ${ent.displayTitle} (${ent.status})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TierOptionCard(
    tierCode: String,
    title: String,
    features: String,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isCurrent) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = features,
                style = MaterialTheme.typography.bodySmall
            )
            if (!isCurrent) {
                Button(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("activate_tier_${tierCode.lowercase()}")
                ) {
                    Text("Switch to $tierCode", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
