package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookEntity
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.ReadingHistoryEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.UserAiConfigEntity
import com.example.data.local.UserBookAccessEntity
import com.example.data.local.UserEntity
import com.example.ui.theme.LoraSerif
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =====================================================================
// LOGGED-OUT PAGE 1: HOW IT WORKS (Calm Editorial Overview)
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowItWorksScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onTrySampleBookClick: () -> Unit,
    onBrowseDirectoryClick: () -> Unit,
    onExtractFileClick: () -> Unit = {},
    onExtractSampleEpubClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .testTag("how_it_works_screen"),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Editorial Masthead Hero
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VIDYAKOSHA • DECENTRALIZED KNOWLEDGE COMMONS",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.4.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Zero-host peer-to-peer extraction & distraction-free reading.",
                        fontFamily = LoraSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 30.sp,
                        lineHeight = 38.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Convert your own PDF, EPUB, TXT, Markdown, or HTML files into structured OCR chapters and translate across your Top 3 languages. Original restricted files are purged immediately (0 bytes hosted on server)—VidyaKosha owns and retains only the structured OCR and translation derivatives.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTrySampleBookClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_try_sample_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Read Sample Anthology")
                        }

                        OutlinedButton(
                            onClick = onExtractFileClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_extract_file_button")
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extract PDF / EPUB / Text")
                        }

                        OutlinedButton(
                            onClick = onExtractSampleEpubClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_extract_sample_epub_button")
                        ) {
                            Text("Test P2P EPUB Extraction")
                        }

                        OutlinedButton(
                            onClick = onBrowseDirectoryClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("hero_browse_directory_button")
                        ) {
                            Text("Browse Catalogue")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onLoginClick,
                            modifier = Modifier.testTag("hero_login_button")
                        ) {
                            Text("Sign In to Your Account")
                        }
                        Text("•", color = MaterialTheme.colorScheme.outline)
                        TextButton(
                            onClick = onSignUpClick,
                            modifier = Modifier.testTag("hero_signup_button")
                        ) {
                            Text("Create Free Account")
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Editorial Architecture Pillars
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        text = "How VidyaKosha Peer-to-Peer Extraction Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    EditorialStepRow(
                        number = "01",
                        title = "Multi-Format Zero-Host Extraction (PDF, EPUB, TXT, MD, HTML)",
                        description = "Bring your own PDF, EPUB, Markdown, Plain Text, or HTML file. The original certified/restricted file is processed ephemerally and purged immediately (0 bytes hosted on server)."
                    )
                    EditorialStepRow(
                        number = "02",
                        title = "VidyaKosha Owns the OCR & Translation Derivatives",
                        description = "While the original file is never stored, VidyaKosha owns and permanently versions the converted OCR structure, SHA-256 content hashes, and canonical translations in the shared commons."
                    )
                    EditorialStepRow(
                        number = "03",
                        title = "Kindle-Style Focused Reading & Top-3 Language Scoping",
                        description = "Choose your Top 3 languages from 100+ Indian, classical, and global languages so only your 3 chosen languages are active in the reader and queried on the server."
                    )
                    EditorialStepRow(
                        number = "04",
                        title = "Free-Will Translation & Segment Memory Reuse",
                        description = "Translate passages at your free will using shared canonical translations, segment-level Translation Memory, or your own encrypted Gemini, OpenAI, Claude, or OpenRouter keys."
                    )
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Supported Languages Summary
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Supported Indian & Global Languages",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageRegistry.INDIAN_LANGUAGES.take(14).forEach { lang ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "${lang.name} • ${lang.nativeName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun EditorialStepRow(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            fontFamily = LoraSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================
// LOGGED-IN PAGE 1: MY HOME (Calm Reading Continuity)
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserHomeDashboardScreen(
    user: UserEntity,
    userAccessibleBooks: List<BookEntity>,
    readingProgressList: List<ReadingProgressEntity>,
    bookmarks: List<BookmarkHighlightEntity>,
    readingHistory: List<ReadingHistoryEntity>,
    aiConfigs: List<UserAiConfigEntity>,
    isIngestingPdf: Boolean,
    ingestionStageMessage: String,
    ingestionProcessedPages: Int,
    ingestionTotalPages: Int,
    onContinueReadingClick: (BookEntity?) -> Unit,
    onOpenBookClick: (BookEntity) -> Unit,
    onUploadNewPdfClick: () -> Unit,
    onExtractSampleEpubClick: () -> Unit = {},
    onOpenAiSettingsClick: () -> Unit,
    onBrowsePublicDirectoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mostRecentProgress = readingProgressList.firstOrNull()
    val lastReadBook = remember(mostRecentProgress, userAccessibleBooks) {
        if (mostRecentProgress != null) {
            userAccessibleBooks.find { it.id == mostRecentProgress.bookId }
        } else {
            userAccessibleBooks.firstOrNull()
        }
    }
    val progressByBook = remember(readingProgressList) {
        readingProgressList.associateBy { it.bookId }
    }
    val df = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .testTag("user_home_dashboard"),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Minimal Header & AI Key Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName,
                            fontFamily = LoraSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Preferred Language: ${user.preferredLanguage}  •  Plan: ${user.activeTierCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAiSettingsClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_ai_status_pill")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (aiConfigs.isNotEmpty()) "${aiConfigs.size} AI Key(s)" else "Add AI Key",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Optional PDF Ingestion Progress Banner
            if (isIngestingPdf) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ingestionStageMessage.ifBlank { "Processing PDF into structured chapters…" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (ingestionTotalPages > 0) {
                                LinearProgressIndicator(
                                    progress = {
                                        (ingestionProcessedPages.toFloat() / ingestionTotalPages.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            // 2. Primary "Continue Reading" Surface
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CONTINUE READING",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastReadBook?.title ?: "Bharat Gyan Sangraha",
                            fontFamily = LoraSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (mostRecentProgress != null) {
                            Text(
                                text = "${mostRecentProgress.currentChapterTitle}  •  ${mostRecentProgress.currentSectionHeading} (${mostRecentProgress.preferredLanguage})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (lastReadBook != null) {
                            Text(
                                text = "${lastReadBook.authorOrSource}  •  ${lastReadBook.originalLanguage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { onContinueReadingClick(lastReadBook) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("continue_reading_button")
                            ) {
                                Text("Open in Reader")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onUploadNewPdfClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_upload_pdf_button")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract PDF / EPUB / Text")
                            }

                            OutlinedButton(
                                onClick = onExtractSampleEpubClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_extract_sample_epub_button")
                            ) {
                                Text("Test P2P EPUB")
                            }
                        }
                    }
                }
            }

            // 3. Personal Shelf
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Library (${userAccessibleBooks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onBrowsePublicDirectoryClick) {
                        Text("Full Catalogue →")
                    }
                }
            }

            items(userAccessibleBooks, key = { "shelf_${it.id}" }) { book ->
                PersonalBookRowCard(
                    book = book,
                    progress = progressByBook[book.id],
                    onClick = { onOpenBookClick(book) }
                )
            }

            // 4. Saved Bookmarks (Minimal list)
            if (bookmarks.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saved Passages (${bookmarks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(bookmarks.take(5), key = { "home_bm_${it.id}" }) { bm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = bm.selectedSnippet,
                                fontFamily = LoraSerif,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${bm.bookTitle} • ${bm.sectionHeading}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Recent Activity
            if (readingHistory.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Reading Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(readingHistory.take(5), key = { "hist_${it.id}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.actionSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.bookTitle} • ${item.chapterTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = df.format(Date(item.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalBookRowCard(
    book: BookEntity,
    progress: ReadingProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("personal_book_${book.id}"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    fontFamily = LoraSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (progress != null) {
                        "Resume: ${progress.currentSectionHeading} (${(progress.progressPercent * 100).toInt()}%)"
                    } else {
                        "${book.authorOrSource}  •  ${book.originalLanguage}  •  ${book.totalSections} sections"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(onClick = onClick) {
                Text(if (progress != null) "Resume" else "Read")
            }
        }
    }
}

// =====================================================================
// PUBLIC BOOK DIRECTORY CATALOGUE (Clean Editorial List)
// =====================================================================

@Composable
fun PublicBookDirectoryScreen(
    books: List<BookEntity>,
    userAccessList: List<UserBookAccessEntity>,
    isLoggedIn: Boolean,
    searchQuery: String,
    activeFilter: String, // "ALL", "FREE", "PAID"
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenBookForReading: (BookEntity) -> Unit,
    onUnlockOrPurchaseBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredBooks = remember(books, searchQuery, activeFilter) {
        books.filter { book ->
            val matchesFilter = when (activeFilter) {
                "FREE" -> !book.isPaid
                "PAID" -> book.isPaid
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                book.title.lowercase().contains(q) ||
                book.authorOrSource.lowercase().contains(q) ||
                book.category.lowercase().contains(q) ||
                book.originalLanguage.lowercase().contains(q)
            matchesFilter && matchesSearch
        }
    }

    val ownedBookIds = remember(userAccessList) {
        userAccessList.map { it.bookId }.toSet()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .testTag("book_directory_screen"),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Book Catalogue",
                        fontFamily = LoraSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Structured OCR editions with canonical Indian & global translations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search and Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by title, author, or source language…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("directory_search_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL" to "All Editions", "FREE" to "Open & Free", "PAID" to "Collector & Paid").forEach { (code, label) ->
                            FilterChip(
                                selected = activeFilter == code,
                                onClick = { onFilterChange(code) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("directory_filter_$code")
                            )
                        }
                    }
                }
            }

            items(filteredBooks, key = { "dir_${it.id}" }) { book ->
                val isUnlocked = !book.isPaid || ownedBookIds.contains(book.id)
                PublicDirectoryBookCard(
                    book = book,
                    isUnlocked = isUnlocked,
                    isLoggedIn = isLoggedIn,
                    onReadClick = { onOpenBookForReading(book) },
                    onUnlockClick = { onUnlockOrPurchaseBook(book) }
                )
            }
        }
    }
}

@Composable
private fun PublicDirectoryBookCard(
    book: BookEntity,
    isUnlocked: Boolean,
    isLoggedIn: Boolean,
    onReadClick: () -> Unit,
    onUnlockClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("directory_book_${book.id}")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${book.category.uppercase()}  •  ${book.originalLanguage.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = book.title,
                        fontFamily = LoraSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = book.authorOrSource,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = book.priceDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (book.description.isNotBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${book.totalChapters} Ch  •  ${book.totalSections} Sec  •  ${book.accessPolicy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isUnlocked) {
                    Button(
                        onClick = onReadClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("read_book_button_${book.id}")
                    ) {
                        Text("Read Edition")
                    }
                } else {
                    OutlinedButton(
                        onClick = onUnlockClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("unlock_book_button_${book.id}")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isLoggedIn) "Unlock (${book.priceDisplay})" else "Sign In to Unlock")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectorBottomSheet(
    selectedLanguage: LanguageDescriptor,
    cachedLanguages: List<String>,
    onSelectLanguage: (LanguageDescriptor) -> Unit,
    onDismiss: () -> Unit
) {
    var filterQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(filterQuery) {
        if (filterQuery.isBlank()) {
            LanguageRegistry.ALL_LANGUAGES
        } else {
            val q = filterQuery.trim().lowercase()
            LanguageRegistry.ALL_LANGUAGES.filter {
                it.name.lowercase().contains(q) ||
                    it.nativeName.lowercase().contains(q) ||
                    it.scriptName.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Reading Language",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filter languages…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier.height(360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredLanguages, key = { it.code }) { lang ->
                    val isSelected = lang.code == selectedLanguage.code
                    val isCached = cachedLanguages.any { it.equals(lang.name, ignoreCase = true) }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectLanguage(lang)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${lang.name}  •  ${lang.nativeName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isCached) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Available",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
