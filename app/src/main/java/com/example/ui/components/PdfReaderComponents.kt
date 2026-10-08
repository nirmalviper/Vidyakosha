package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BilingualParagraphItem
import com.example.data.local.BookEntity
import com.example.data.local.BookRightsCategory
import com.example.data.local.ChapterEntity
import com.example.data.local.ContinuousSectionBlock
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.OcrCorrectionAuditEntity
import com.example.data.local.ParagraphEntity
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.SectionEntity
import com.example.data.local.TranslationDiscoveryStatus
import com.example.data.local.TranslationReviewStatus
import com.example.ui.PendingContributorAction
import com.example.ui.ReaderDisplayMode
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LoraSerif
import com.example.ui.theme.ReaderCanvasNight
import com.example.ui.theme.ReaderCanvasPaper
import com.example.ui.theme.ReaderCanvasSepia
import com.example.ui.theme.ReaderMutedNight
import com.example.ui.theme.ReaderMutedPaper
import com.example.ui.theme.ReaderMutedSepia
import com.example.ui.theme.ReaderTextNight
import com.example.ui.theme.ReaderTextPaper
import com.example.ui.theme.ReaderTextSepia

private enum class EReaderCanvasTone(val label: String) {
    PAPER("Paper"),
    SEPIA("Warm Sepia"),
    NIGHT("Night Ink")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BilingualBookReaderPane(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    sections: List<SectionEntity>,
    activeChapter: ChapterEntity?,
    activeSection: SectionEntity?,
    paragraphs: List<BilingualParagraphItem>,
    continuousBlocks: List<ContinuousSectionBlock> = emptyList(),
    onViewportPositionChanged: (Long, Long) -> Unit = { _, _ -> },
    discoveryStatus: TranslationDiscoveryStatus?,
    cachedLanguages: List<String>,
    ocrAuditsForBook: List<OcrCorrectionAuditEntity>,
    selectedLanguage: LanguageDescriptor,
    displayMode: ReaderDisplayMode,
    fontScaleMultiplier: Float,
    showTransliteration: Boolean,
    showWordByWordBreakdown: Boolean,
    autoTranslateOnScroll: Boolean,
    continuousScrollNotice: String?,
    isTranslatingSection: Boolean,
    speakingParagraphId: Long?,
    isSpeakingOriginal: Boolean,
    isPlayingFullSectionAudio: Boolean,
    onSelectSection: (Long) -> Unit,
    onAdvanceNextSectionSeamless: () -> Unit,
    onPreviousSectionSeamless: () -> Unit,
    onDismissScrollNotice: () -> Unit,
    onSelectLanguage: (LanguageDescriptor) -> Unit,
    onSelectDisplayMode: (ReaderDisplayMode) -> Unit,
    onAdjustFontScale: (Float) -> Unit,
    onToggleTransliteration: () -> Unit,
    onToggleWordByWord: () -> Unit,
    onToggleAutoTranslate: () -> Unit,
    onTranslateSection: (Boolean) -> Unit,
    onSelectPreferredVersion: (Long, Long) -> Unit,
    onSubmitHumanReviewOrEdit: (Long, Long, String, String, String) -> Unit,
    onReportTranslationIssue: (Long, Long, String, String, String) -> Unit,
    onCorrectParagraphOcr: (Long, String, String) -> Unit,
    onToggleBookmark: (ParagraphEntity) -> Unit,
    onSpeakParagraph: (Long, String, String, Boolean) -> Unit,
    onToggleFullSectionAudio: (Boolean) -> Unit,
    onDownloadOfflinePackage: () -> Unit,
    isFocusModeEnabled: Boolean = false,
    onToggleFocusMode: () -> Unit = {},
    userTopLanguages: List<LanguageDescriptor> = LanguageRegistry.parseTopThreeLanguages(null),
    onOpenLanguageSettings: () -> Unit = {},
    appThemeMode: AppThemeMode = AppThemeMode.WHITE,
    onSelectAppThemeMode: (AppThemeMode) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var focusChromeVisible by rememberSaveable { mutableStateOf(false) }

    var showTocDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguagePickerDialog by remember { mutableStateOf(false) }

    var expandedParagraphId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingReviewTarget by remember { mutableStateOf<BilingualParagraphItem?>(null) }
    var reportingIssueTarget by remember { mutableStateOf<BilingualParagraphItem?>(null) }
    var correctingOcrTarget by remember { mutableStateOf<BilingualParagraphItem?>(null) }
    var comparingVersionsTarget by remember { mutableStateOf<BilingualParagraphItem?>(null) }
    var tocJumpCounter by remember { mutableStateOf(0) }

    LaunchedEffect(book.id, tocJumpCounter) {
        if (tocJumpCounter > 0) {
            listState.scrollToItem(0)
        }
    }

    val activeSectionIndex = remember(sections, activeSection) {
        sections.indexOfFirst { it.id == activeSection?.id }.coerceAtLeast(0)
    }

    val effectiveBlocks = remember(continuousBlocks, chapters, sections, activeChapter, activeSection, paragraphs) {
        if (continuousBlocks.isNotEmpty()) {
            continuousBlocks
        } else if (activeSection != null) {
            val fallbackChapter = activeChapter ?: chapters.firstOrNull() ?: ChapterEntity(
                id = activeSection.chapterId,
                bookId = book.id,
                chapterOrder = 1,
                title = book.title,
                summary = "",
                startPage = activeSection.startPage,
                endPage = activeSection.endPage
            )
            listOf(
                ContinuousSectionBlock(
                    chapter = fallbackChapter,
                    isFirstSectionOfChapter = true,
                    section = activeSection,
                    globalSectionIndex = activeSectionIndex,
                    totalBookSections = sections.size.coerceAtLeast(1),
                    paragraphs = paragraphs,
                    isFullyTranslated = paragraphs.isNotEmpty() && paragraphs.all { it.activeTranslation != null }
                )
            )
        } else {
            emptyList()
        }
    }

    val firstLoadedSectionId = effectiveBlocks.firstOrNull()?.section?.id ?: activeSection?.id
    val lastLoadedSectionId = effectiveBlocks.lastOrNull()?.section?.id ?: activeSection?.id

    val hasPreviousUnloadedSection = remember(sections, firstLoadedSectionId) {
        val idx = sections.indexOfFirst { it.id == firstLoadedSectionId }
        idx > 0
    }
    val hasNextUnloadedSection = remember(sections, lastLoadedSectionId) {
        val idx = sections.indexOfFirst { it.id == lastLoadedSectionId }
        idx >= 0 && idx < sections.lastIndex
    }
    val isLoadingPreviousSection = false

    // Map each lazy list item key to (sectionId, paragraphId) for viewport tracking
    val paragraphKeyLookup = remember(effectiveBlocks) {
        buildMap<String, Pair<Long, Long>> {
            effectiveBlocks.forEach { block ->
                block.paragraphs.forEach { item ->
                    put("para_${block.section.id}_${item.paragraph.id}", block.section.id to item.paragraph.id)
                }
            }
        }
    }

    // Track scroll position to progressively load next/previous sections and keep active section synced
    LaunchedEffect(listState, effectiveBlocks, hasPreviousUnloadedSection, hasNextUnloadedSection) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount
            Triple(visibleItems, totalItems, listState.firstVisibleItemIndex)
        }.collect { (visibleItems, totalItems, firstIndex) ->
            if (visibleItems.isEmpty() || totalItems == 0) return@collect
            val lastVisibleIndex = visibleItems.last().index
            val nearBottom = hasNextUnloadedSection && (lastVisibleIndex >= (totalItems - 4).coerceAtLeast(0))
            val nearTop = hasPreviousUnloadedSection && (firstIndex <= 1) && listState.canScrollBackward

            // Identify the primary visible paragraph in the upper half of the viewport
            val primaryItem = visibleItems.firstOrNull { info ->
                (info.key as? String)?.startsWith("para_") == true
            }
            val matchedPair = (primaryItem?.key as? String)?.let { paragraphKeyLookup[it] }
            if (matchedPair != null) {
                onViewportPositionChanged(matchedPair.first, matchedPair.second)
            }

            if (nearBottom) {
                onAdvanceNextSectionSeamless()
            } else if (nearTop) {
                onPreviousSectionSeamless()
            }
        }
    }

    // During Continuous Listen Mode, smoothly keep the active spoken paragraph in view
    LaunchedEffect(speakingParagraphId, isPlayingFullSectionAudio, effectiveBlocks) {
        if (isPlayingFullSectionAudio && speakingParagraphId != null) {
            val visibleKeys = listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
            val targetSuffix = "_$speakingParagraphId"
            val alreadyVisible = visibleKeys.any { it.startsWith("para_") && it.endsWith(targetSuffix) }
            if (!alreadyVisible) {
                var flatIndex = 0
                if (hasPreviousUnloadedSection) flatIndex += 1
                var foundIndex = -1
                for (block in effectiveBlocks) {
                    flatIndex += 1 // Section transition header item
                    for (item in block.paragraphs) {
                        if (item.paragraph.id == speakingParagraphId) {
                            foundIndex = flatIndex
                            break
                        }
                        flatIndex += 1
                    }
                    if (foundIndex >= 0) break
                }
                if (foundIndex >= 0) {
                    runCatching { listState.animateScrollToItem(foundIndex) }
                }
            }
        }
    }

    val canvasBg = when (appThemeMode) {
        AppThemeMode.WHITE -> ReaderCanvasPaper
        AppThemeMode.SEPIA -> ReaderCanvasSepia
        AppThemeMode.DARK -> ReaderCanvasNight
    }
    val canvasText = when (appThemeMode) {
        AppThemeMode.WHITE -> ReaderTextPaper
        AppThemeMode.SEPIA -> ReaderTextSepia
        AppThemeMode.DARK -> ReaderTextNight
    }
    val canvasMuted = when (appThemeMode) {
        AppThemeMode.WHITE -> ReaderMutedPaper
        AppThemeMode.SEPIA -> ReaderMutedSepia
        AppThemeMode.DARK -> ReaderMutedNight
    }

    val showTopReaderBar = !isFocusModeEnabled || focusChromeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Minimal, Calm E-Reader Header Bar (Automatically hidden in Focus Mode unless toggled)
            AnimatedVisibility(
                visible = showTopReaderBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = canvasBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chapter / Section TOC Trigger
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = canvasText.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showTocDialog = true }
                                    .testTag("open_toc_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Table of Contents",
                                        tint = canvasMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${activeSection?.heading ?: book.title} • ${activeSectionIndex + 1}/${sections.size.coerceAtLeast(1)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = canvasText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = canvasMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            // Essential Reader Controls: Language, Audio, Aa Appearance, Focus Mode
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Subtle Target Language Trigger
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showLanguagePickerDialog = true }
                                        .testTag("reader_language_picker_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = selectedLanguage.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = canvasText
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Language",
                                            tint = canvasMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Audio Listen Section Button
                                IconButton(
                                    onClick = { onToggleFullSectionAudio(true) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("listen_section_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingFullSectionAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingFullSectionAudio) "Stop Section Audio" else "Listen to Section",
                                        tint = canvasText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 'Aa' Reading Appearance & Settings Modal Trigger
                                IconButton(
                                    onClick = { showAppearanceDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("reader_appearance_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = "Typography & Reading Settings",
                                        tint = canvasText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Focus Mode Toggle Button
                                IconButton(
                                    onClick = {
                                        focusChromeVisible = false
                                        onToggleFocusMode()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_focus_mode_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFocusModeEnabled) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFocusModeEnabled) "Exit Focus Mode" else "Enter Focused Reading Mode",
                                        tint = canvasText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isTranslatingSection) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = canvasText.copy(alpha = 0.6f),
                                trackColor = Color.Transparent
                            )
                        } else {
                            HorizontalDivider(color = canvasText.copy(alpha = 0.08f))
                        }

                        if (continuousScrollNotice != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(canvasText.copy(alpha = 0.05f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = continuousScrollNotice,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = canvasMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismissScrollNotice,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss Notice",
                                        tint = canvasMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Continuous Listening Active Banner (allows switching Original/Translation or stopping)
                        if (isPlayingFullSectionAudio) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("continuous_listening_active_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isSpeakingOriginal) {
                                                "Continuous Listen • Original (${book.originalLanguage}) • Auto-advances sections"
                                            } else {
                                                "Continuous Listen • ${selectedLanguage.name} • Auto-advances sections"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TextButton(
                                            onClick = { onToggleFullSectionAudio(isSpeakingOriginal) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("switch_continuous_audio_stream_button")
                                        ) {
                                            Text(
                                                text = if (isSpeakingOriginal) "Switch to ${selectedLanguage.name}" else "Switch to Original",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        TextButton(
                                            onClick = { onToggleFullSectionAudio(!isSpeakingOriginal) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("stop_continuous_audio_button")
                                        ) {
                                            Icon(
                                                Icons.Default.Stop,
                                                contentDescription = "Stop Continuous Listening",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Stop",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Dominant E-Reader Literary Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                val isWideScreen = maxWidth >= 700.dp
                val useSideBySideColumns = displayMode == ReaderDisplayMode.BILINGUAL_SIDE_BY_SIDE && isWideScreen
                val maxSheetWidth = if (useSideBySideColumns) 980.dp else 720.dp
                val horizontalMargin = when {
                    maxWidth >= 840.dp -> 48.dp
                    maxWidth >= 600.dp -> 36.dp
                    else -> 22.dp
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = maxSheetWidth)
                        .testTag("bilingual_reader_list"),
                    contentPadding = PaddingValues(
                        start = horizontalMargin,
                        end = horizontalMargin,
                        top = if (isFocusModeEnabled && !focusChromeVisible) 36.dp else 28.dp,
                        bottom = 64.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Progressive Upward Loading Indicator (when prior sections exist in the chapter/book)
                    if (hasPreviousUnloadedSection) {
                        item(key = "progressive_previous_section_loader") {
                            LaunchedEffect(firstLoadedSectionId) {
                                if (listState.canScrollBackward) {
                                    onPreviousSectionSeamless()
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoadingPreviousSection) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(2.dp),
                                        color = canvasMuted.copy(alpha = 0.6f),
                                        trackColor = Color.Transparent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isLoadingPreviousSection) {
                                        "Loading previous section…"
                                    } else {
                                        "Scroll up to load earlier passages"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = canvasMuted.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Render Progressive Window of Continuous Section Blocks seamlessly
                    effectiveBlocks.forEachIndexed { blockIdx, block ->
                        val previousChapterId = effectiveBlocks.getOrNull(blockIdx - 1)?.chapter?.id
                        val isNewChapter = blockIdx == 0 || block.chapter?.id != previousChapterId

                        item(key = "section_header_${block.section.id}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) {
                                        if (isFocusModeEnabled) {
                                            focusChromeVisible = !focusChromeVisible
                                        }
                                    }
                                    .padding(
                                        top = if (blockIdx == 0) 0.dp else 16.dp,
                                        bottom = 12.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (blockIdx > 0) {
                                    // Subtle literary ornamental divider between sections so transitions feel continuous
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = canvasText.copy(alpha = 0.08f)
                                        )
                                        Text(
                                            text = "  ✦  ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = canvasMuted.copy(alpha = 0.55f)
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.weight(1f),
                                            color = canvasText.copy(alpha = 0.08f)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isNewChapter) {
                                        "${block.chapter.title.uppercase()} • P. ${block.section.startPage}"
                                    } else {
                                        "SECTION ${block.section.sectionOrder} • P. ${block.section.startPage}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    letterSpacing = 1.2.sp,
                                    color = canvasMuted
                                )
                                Text(
                                    text = block.section.heading,
                                    fontFamily = LoraSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (if (blockIdx == 0) (24f * fontScaleMultiplier) else (20f * fontScaleMultiplier)).sp,
                                    lineHeight = (if (blockIdx == 0) (32f * fontScaleMultiplier) else (28f * fontScaleMultiplier)).sp,
                                    color = canvasText
                                )
                                if (isNewChapter && block.chapter.summary.isNotBlank()) {
                                    Text(
                                        text = block.chapter.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = canvasMuted,
                                        lineHeight = 22.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(
                                    modifier = Modifier.width(64.dp),
                                    thickness = 1.5.dp,
                                    color = canvasText.copy(alpha = 0.2f)
                                )

                                val missingTranslationsCount = block.paragraphs.count { it.activeTranslation == null }
                                if (missingTranslationsCount > 0 && displayMode != ReaderDisplayMode.ORIGINAL_ONLY) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "$missingTranslationsCount of ${block.paragraphs.size} passages untranslated in ${selectedLanguage.name}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = canvasText
                                                )
                                                Text(
                                                    text = "Reuses Shared Repo & Local NMT first • No AI key required",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = canvasMuted
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Button(
                                                onClick = { onTranslateSection(false) },
                                                enabled = !isTranslatingSection,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.testTag(
                                                    if (blockIdx == 0) "section_banner_translate_button"
                                                    else "section_banner_translate_button_${block.section.id}"
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Default.Translate,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isTranslatingSection) "Translating..." else "Translate All",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Clean, Unboxed Literary Passages in this Section
                        items(
                            items = block.paragraphs,
                            key = { "para_${block.section.id}_${it.paragraph.id}" }
                        ) { item ->
                            val isExpanded = expandedParagraphId == item.paragraph.id
                            LiteraryPassageBlock(
                                item = item,
                                selectedLanguage = selectedLanguage,
                                displayMode = displayMode,
                                useSideBySideColumns = useSideBySideColumns,
                                fontScaleMultiplier = fontScaleMultiplier,
                                showTransliteration = showTransliteration,
                                showWordByWordBreakdown = showWordByWordBreakdown,
                                canvasText = canvasText,
                                canvasMuted = canvasMuted,
                                isExpanded = isExpanded,
                                isSpeakingThisOriginal = speakingParagraphId == item.paragraph.id && isSpeakingOriginal,
                                isSpeakingThisTranslation = speakingParagraphId == item.paragraph.id && !isSpeakingOriginal,
                                onToggleExpandActions = {
                                    expandedParagraphId = if (isExpanded) null else item.paragraph.id
                                },
                                onSpeakOriginal = {
                                    onSpeakParagraph(
                                        item.paragraph.id,
                                        item.paragraph.originalText,
                                        item.paragraph.originalLanguage,
                                        true
                                    )
                                },
                                onSpeakTranslation = {
                                    val transText = item.activeTranslation?.translatedText ?: return@LiteraryPassageBlock
                                    onSpeakParagraph(
                                        item.paragraph.id,
                                        transText,
                                        selectedLanguage.name,
                                        false
                                    )
                                },
                                onToggleBookmark = { onToggleBookmark(item.paragraph) },
                                onOpenHumanReviewModal = { editingReviewTarget = item },
                                onOpenReportIssueModal = { reportingIssueTarget = item },
                                onOpenOcrCorrectionModal = { correctingOcrTarget = item },
                                onOpenVersionComparisonModal = { comparingVersionsTarget = item },
                                onTranslateMissing = { onTranslateSection(false) }
                            )
                        }
                    }

                    // Progressive Continuous Reading Footer (automatically loads next section on scroll without manual buttons)
                    item(key = "continuous_reading_progressive_footer") {
                        LaunchedEffect(hasNextUnloadedSection, effectiveBlocks.size) {
                            if (hasNextUnloadedSection) {
                                onAdvanceNextSectionSeamless()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = canvasText.copy(alpha = 0.1f))

                            if (hasNextUnloadedSection) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .width(84.dp)
                                            .height(2.dp),
                                        color = canvasMuted.copy(alpha = 0.6f),
                                        trackColor = Color.Transparent
                                    )
                                    Text(
                                        text = "Continuing seamlessly into next section…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = canvasMuted.copy(alpha = 0.75f)
                                    )
                                }
                            } else {
                                Text(
                                    text = "✦  End of Chapter / Book  ✦",
                                    fontFamily = LoraSerif,
                                    fontStyle = FontStyle.Italic,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = canvasMuted.copy(alpha = 0.85f)
                                )
                                TextButton(
                                    onClick = { showTocDialog = true },
                                    modifier = Modifier.testTag("colophon_open_toc_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = canvasText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Browse Table of Contents",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = canvasText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Subtle Floating Focus Mode Pill (Visible when in Focus Mode so user can always exit or open toolbar)
        if (isFocusModeEnabled) {
            Surface(
                shape = CircleShape,
                color = canvasText.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, canvasText.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = { focusChromeVisible = !focusChromeVisible },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (focusChromeVisible) "Hide Bar" else "Controls",
                            style = MaterialTheme.typography.labelSmall,
                            color = canvasText
                        )
                    }
                    TextButton(
                        onClick = {
                            focusChromeVisible = false
                            onToggleFocusMode()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("exit_focus_mode_pill_button")
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "Exit Focus Mode",
                            tint = canvasText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Exit Focus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = canvasText
                        )
                    }
                }
            }
        }
    }

    // =====================================================================
    // E-READER APPEARANCE & READING SETTINGS DIALOG ('Aa')
    // =====================================================================
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = {
                Text(
                    text = "Reading Appearance & Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Font Size Scaling
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Text Size (${(fontScaleMultiplier * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onAdjustFontScale(-0.1f) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("A−  Smaller")
                            }
                            OutlinedButton(
                                onClick = { onAdjustFontScale(0.1f) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("A+  Larger")
                            }
                        }
                    }

                    // 2. App & Page Theme (White, Sepia, Dark)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "App & Page Theme",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = appThemeMode == mode,
                                    onClick = { onSelectAppThemeMode(mode) },
                                    label = { Text(mode.label, maxLines = 1, softWrap = false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 3. Reading Layout Mode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Layout Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReaderDisplayMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = displayMode == mode,
                                    onClick = { onSelectDisplayMode(mode) },
                                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("display_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 4. Scholarly & Automation Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Reading Aids & Automation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = showTransliteration,
                                onClick = onToggleTransliteration,
                                label = { Text("IAST & Phonetics", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("toggle_transliteration_chip")
                            )
                            FilterChip(
                                selected = showWordByWordBreakdown,
                                onClick = onToggleWordByWord,
                                label = { Text("Word-by-Word", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("toggle_word_by_word_chip")
                            )
                            FilterChip(
                                selected = autoTranslateOnScroll,
                                onClick = onToggleAutoTranslate,
                                label = { Text("Auto-Translate on Scroll", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("toggle_auto_translate_chip")
                            )
                        }
                    }

                    // 5. Shared Translation Discovery & Actions
                    HorizontalDivider()
                    discoveryStatus?.let { status ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Translation Status • ${status.targetLanguage.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${status.translatedParagraphsCount} of ${status.totalParagraphsInBook} passages available (${status.coveragePercent}%) • v${status.latestVersionNumber} (${status.latestReviewStatus})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Source: ${status.primaryProvider} (${status.primaryModel}) • Policy: ${status.accessPolicy}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onTranslateSection(false)
                                showAppearanceDialog = false
                            },
                            enabled = !isTranslatingSection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reuse_or_translate_section_button")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate (Local / Repo)", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                onDownloadOfflinePackage()
                                showAppearanceDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_offline_package_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Offline Sync", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onTranslateSection(true)
                            showAppearanceDialog = false
                        },
                        enabled = !isTranslatingSection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_new_version_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retranslate Section (Local NMT or Optional AI)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // =====================================================================
    // TABLE OF CONTENTS DIALOG
    // =====================================================================
    if (showTocDialog) {
        AlertDialog(
            onDismissRequest = { showTocDialog = false },
            title = {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chapters.forEach { chapter ->
                        Text(
                            text = "Chapter ${chapter.chapterOrder} — ${chapter.title}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val chSections = sections.filter { it.chapterId == chapter.id }
                        chSections.forEach { sec ->
                            val isSelected = sec.id == activeSection?.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        tocJumpCounter += 1
                                        onSelectSection(sec.id)
                                        showTocDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sec.heading,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "p. ${sec.startPage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTocDialog = false }) { Text("Close") }
            }
        )
    }

    // =====================================================================
    // LANGUAGE PICKER DIALOG (RESTRICTED TO USER'S TOP 3 LANGUAGES)
    // =====================================================================
    if (showLanguagePickerDialog) {
        val visibleTopLanguages = userTopLanguages
            .take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES)
            .ifEmpty { LanguageRegistry.parseTopThreeLanguages(null) }

        AlertDialog(
            onDismissRequest = { showLanguagePickerDialog = false },
            title = {
                Text(
                    text = "Your Top 3 Reading Languages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Only your Top 3 selected languages are shown here to keep reading focused and reduce server load.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    visibleTopLanguages.forEachIndexed { index, lang ->
                        val isCached = cachedLanguages.any { it.equals(lang.name, ignoreCase = true) }
                        val isSelected = selectedLanguage.code == lang.code
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectLanguage(lang)
                                    showLanguagePickerDialog = false
                                }
                                .testTag("reader_top_lang_option_${lang.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "#${index + 1}  ${lang.name}  •  ${lang.nativeName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${lang.scriptName} script • ${lang.regionGroup}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = when {
                                        isSelected -> "Active"
                                        isCached -> "Ready"
                                        else -> "Select"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OutlinedButton(
                        onClick = {
                            showLanguagePickerDialog = false
                            onOpenLanguageSettings()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reader_change_top_three_languages_button")
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Top 3 in Settings (${LanguageRegistry.ALL_LANGUAGES.size} Languages)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePickerDialog = false }) { Text("Close") }
            }
        )
    }

    // =====================================================================
    // EDITORIAL REVIEW, ISSUE REPORT, OCR AUDIT & VERSION DIALOGS
    // =====================================================================
    editingReviewTarget?.let { target ->
        val activeTrans = target.activeTranslation
        var editedText by remember(target) { mutableStateOf(activeTrans?.translatedText.orEmpty()) }
        var editedNotes by remember(target) { mutableStateOf(activeTrans?.transliterationOrNotes.orEmpty()) }
        var selectedStatus by remember(target) {
            mutableStateOf(activeTrans?.reviewStatus ?: TranslationReviewStatus.HUMAN_REVIEWED)
        }

        AlertDialog(
            onDismissRequest = { editingReviewTarget = null },
            title = { Text("Editorial Review & Quality Lifecycle") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Original (${target.paragraph.originalLanguage} • OCR v${target.paragraph.ocrVersion}):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = target.paragraph.originalText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        label = { Text("${selectedLanguage.name} Canonical Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text("Scholarly / Editorial Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Review Stage:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TranslationReviewStatus.ORDERED_STAGES.forEach { stage ->
                            FilterChip(
                                selected = selectedStatus == stage,
                                onClick = { selectedStatus = stage },
                                label = { Text(stage, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitHumanReviewOrEdit(
                            activeTrans?.id ?: 0L,
                            target.paragraph.id,
                            editedText,
                            editedNotes,
                            selectedStatus
                        )
                        editingReviewTarget = null
                    }
                ) {
                    Text("Save Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReviewTarget = null }) { Text("Cancel") }
            }
        )
    }

    reportingIssueTarget?.let { target ->
        var category by remember { mutableStateOf("TERMINOLOGY") }
        var description by remember { mutableStateOf("") }
        var suggestedCorrection by remember { mutableStateOf("") }
        val categories = listOf("TERMINOLOGY", "SANSKRIT_SANDHI", "GRAMMAR", "OCR_MISMATCH", "CULTURAL_NUANCE")

        AlertDialog(
            onDismissRequest = { reportingIssueTarget = null },
            title = { Text("Report Translation Issue") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = suggestedCorrection,
                        onValueChange = { suggestedCorrection = it },
                        label = { Text("Suggested Correction (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportTranslationIssue(
                            target.activeTranslation?.id ?: 0L,
                            target.paragraph.id,
                            category,
                            description.ifBlank { "Needs editorial verification" },
                            suggestedCorrection
                        )
                        reportingIssueTarget = null
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingIssueTarget = null }) { Text("Cancel") }
            }
        )
    }

    correctingOcrTarget?.let { target ->
        var correctedSource by remember(target) { mutableStateOf(target.paragraph.originalText) }
        var reasonNote by remember { mutableStateOf("Diacritic & sandhi normalization") }
        val paragraphAudits = ocrAuditsForBook.filter { it.paragraphId == target.paragraph.id }

        AlertDialog(
            onDismissRequest = { correctingOcrTarget = null },
            title = { Text("Source Text Versioning (OCR v${target.paragraph.ocrVersion})") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Original Raw OCR (v1 — Immutable):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = target.paragraph.rawOcrText.ifBlank { target.paragraph.originalText },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = correctedSource,
                        onValueChange = { correctedSource = it },
                        label = { Text("Corrected Source Text (v${target.paragraph.ocrVersion + 1})") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = reasonNote,
                        onValueChange = { reasonNote = it },
                        label = { Text("Correction Note") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (paragraphAudits.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "Revision History (${paragraphAudits.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        paragraphAudits.forEach { audit ->
                            Text(
                                text = "• v${audit.previousOcrVersion} → v${audit.newOcrVersion} (${audit.correctionAuthorName}): ${audit.correctionReason}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCorrectParagraphOcr(target.paragraph.id, correctedSource, reasonNote)
                        correctingOcrTarget = null
                    }
                ) {
                    Text("Save OCR v${target.paragraph.ocrVersion + 1}")
                }
            },
            dismissButton = {
                TextButton(onClick = { correctingOcrTarget = null }) { Text("Cancel") }
            }
        )
    }

    comparingVersionsTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { comparingVersionsTarget = null },
            title = { Text("Translation Versions (${target.allVersions.size})") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    target.allVersions.forEach { ver ->
                        val isPreferred = ver.isActiveVersion
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPreferred) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPreferredVersion(target.paragraph.id, ver.id)
                                    comparingVersionsTarget = null
                                }
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
                                        text = "v${ver.versionNumber} • ${ver.reviewStatus}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isPreferred) "Preferred" else "Select",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = ver.translatedText,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${ver.providerUsed} (${ver.modelUsed}) • ${ver.translatorDisplayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { comparingVersionsTarget = null }) { Text("Done") }
            }
        )
    }
}

/**
 * Clean, unboxed literary passage block designed like an e-reader page rather than a dashboard card.
 * Keeps the text dominant and distraction-free, while offering subtle inline access to audio,
 * bookmarks, version history, and scholarly word-by-word notes.
 */
@Composable
private fun LiteraryPassageBlock(
    item: BilingualParagraphItem,
    selectedLanguage: LanguageDescriptor,
    displayMode: ReaderDisplayMode,
    useSideBySideColumns: Boolean,
    fontScaleMultiplier: Float,
    showTransliteration: Boolean,
    showWordByWordBreakdown: Boolean,
    canvasText: Color,
    canvasMuted: Color,
    isExpanded: Boolean,
    isSpeakingThisOriginal: Boolean,
    isSpeakingThisTranslation: Boolean,
    onToggleExpandActions: () -> Unit,
    onSpeakOriginal: () -> Unit,
    onSpeakTranslation: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenHumanReviewModal: () -> Unit,
    onOpenReportIssueModal: () -> Unit,
    onOpenOcrCorrectionModal: () -> Unit,
    onOpenVersionComparisonModal: () -> Unit,
    onTranslateMissing: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    val p = item.paragraph
    val trans = item.activeTranslation
    val learning = item.transliterationLearning

    val proseFontSize = (18f * fontScaleMultiplier).sp
    val proseLineHeight = (30f * fontScaleMultiplier).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("paragraph_card_${p.id}"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Subtle Marginal Header: Quiet passage number + unobtrusive bookmark & menu triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${p.paragraphOrder}",
                    fontFamily = LoraSerif,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = canvasMuted
                )
                if (item.requiresSourceReview) {
                    Text(
                        text = "• OCR Updated (Review)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (item.allVersionsCount > 1) {
                    Text(
                        text = "• v${trans?.versionNumber ?: 1} (${item.allVersionsCount} versions)",
                        style = MaterialTheme.typography.labelSmall,
                        color = canvasMuted.copy(alpha = 0.75f),
                        modifier = Modifier.clickable { onOpenVersionComparisonModal() }
                    )
                }
            }

            // Quiet, subtle inline controls (Audio, Bookmark, More)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onSpeakOriginal,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("speak_original_${p.id}")
                ) {
                    Icon(
                        imageVector = if (isSpeakingThisOriginal) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen to Original",
                        tint = if (isSpeakingThisOriginal) canvasText else canvasMuted.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (trans != null) {
                    IconButton(
                        onClick = onSpeakTranslation,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("speak_translation_${trans.paragraphId}")
                    ) {
                        Icon(
                            imageVector = if (isSpeakingThisTranslation) Icons.Default.Stop else Icons.Default.Translate,
                            contentDescription = "Listen to Translation",
                            tint = if (isSpeakingThisTranslation) canvasText else canvasMuted.copy(alpha = 0.65f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("bookmark_paragraph_${p.id}")
                ) {
                    Icon(
                        imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Passage",
                        tint = if (item.isBookmarked) canvasText else canvasMuted.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("paragraph_more_actions_${p.id}")
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Passage Options",
                            tint = canvasMuted.copy(alpha = 0.65f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isExpanded) "Hide Scholarly Details" else "Show Scholarly & Provenance Details") },
                            onClick = {
                                showOverflowMenu = false
                                onToggleExpandActions()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compare Translation Versions (${item.allVersionsCount})") },
                            leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenVersionComparisonModal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Editorial Review / Edit Translation") },
                            leadingIcon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenHumanReviewModal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Inspect / Correct OCR Source (v${p.ocrVersion})") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenOcrCorrectionModal()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Translation Issue") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenReportIssueModal()
                            }
                        )
                    }
                }
            }
        }

        // Primary Literary Text Layout (Side-by-Side on wide screens, or clean vertical flow)
        if (useSideBySideColumns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = p.originalText,
                    fontFamily = LoraSerif,
                    fontSize = proseFontSize,
                    lineHeight = proseLineHeight,
                    color = canvasText,
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (trans != null) {
                        Text(
                            text = trans.translatedText,
                            fontFamily = LoraSerif,
                            fontSize = proseFontSize,
                            lineHeight = proseLineHeight,
                            color = canvasText.copy(alpha = 0.92f)
                        )
                    } else {
                        InlineMissingTranslationPrompt(
                            languageName = selectedLanguage.name,
                            canvasMuted = canvasMuted,
                            onTranslateMissing = onTranslateMissing
                        )
                    }
                }
            }
        } else {
            when (displayMode) {
                ReaderDisplayMode.ORIGINAL_ONLY -> {
                    Text(
                        text = p.originalText,
                        fontFamily = LoraSerif,
                        fontSize = proseFontSize,
                        lineHeight = proseLineHeight,
                        color = canvasText
                    )
                }
                ReaderDisplayMode.TRANSLATION_ONLY -> {
                    if (trans != null) {
                        Text(
                            text = trans.translatedText,
                            fontFamily = LoraSerif,
                            fontSize = proseFontSize,
                            lineHeight = proseLineHeight,
                            color = canvasText
                        )
                    } else {
                        InlineMissingTranslationPrompt(
                            languageName = selectedLanguage.name,
                            canvasMuted = canvasMuted,
                            onTranslateMissing = onTranslateMissing
                        )
                    }
                }
                else -> {
                    // Clean stacked bilingual flow with distinct editorial accent bar for translation
                    Text(
                        text = p.originalText,
                        fontFamily = LoraSerif,
                        fontSize = proseFontSize,
                        lineHeight = proseLineHeight,
                        color = canvasText
                    )

                    if (trans != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                .background(canvasText.copy(alpha = 0.035f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .padding(top = 6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = trans.translatedText,
                                    fontFamily = LoraSerif,
                                    fontSize = (17f * fontScaleMultiplier).sp,
                                    lineHeight = (28f * fontScaleMultiplier).sp,
                                    color = canvasText.copy(alpha = 0.90f)
                                )
                            }
                        }
                    } else {
                        InlineMissingTranslationPrompt(
                            languageName = selectedLanguage.name,
                            canvasMuted = canvasMuted,
                            onTranslateMissing = onTranslateMissing
                        )
                    }
                }
            }
        }

        // Subtle, quiet Scholarly Footnote Layer (Shown only when enabled or expanded for this passage)
        val shouldShowScholarly = isExpanded ||
            showTransliteration ||
            showWordByWordBreakdown ||
            displayMode == ReaderDisplayMode.SCHOLARLY_SCRIPT_MODE

        if (shouldShowScholarly && (learning != null || !trans?.transliterationOrNotes.isNullOrBlank())) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (learning != null && (showTransliteration || isExpanded || displayMode == ReaderDisplayMode.SCHOLARLY_SCRIPT_MODE)) {
                    Text(
                        text = learning.iastTransliteration,
                        fontFamily = LoraSerif,
                        fontStyle = FontStyle.Italic,
                        fontSize = (14f * fontScaleMultiplier).sp,
                        lineHeight = (22f * fontScaleMultiplier).sp,
                        color = canvasMuted
                    )
                }

                if (learning != null && (showWordByWordBreakdown || isExpanded || displayMode == ReaderDisplayMode.SCHOLARLY_SCRIPT_MODE)) {
                    Text(
                        text = learning.wordByWordBreakdown,
                        style = MaterialTheme.typography.bodySmall,
                        color = canvasMuted,
                        lineHeight = 19.sp
                    )
                    if (learning.literalEnglishTranslation.isNotBlank()) {
                        Text(
                            text = "Literal: ${learning.literalEnglishTranslation}",
                            style = MaterialTheme.typography.labelSmall,
                            color = canvasMuted.copy(alpha = 0.85f)
                        )
                    }
                } else if (isExpanded && !trans?.transliterationOrNotes.isNullOrBlank()) {
                    Text(
                        text = trans?.transliterationOrNotes.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = canvasMuted
                    )
                }

                if (isExpanded && trans != null) {
                    Text(
                        text = "Provenance: ${trans.providerUsed} (${trans.modelUsed}) • ${trans.reviewStatus} • Policy: ${trans.accessPolicy} • Reused ${trans.reuseCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = canvasMuted.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = canvasText.copy(alpha = 0.07f))
    }
}

@Composable
private fun InlineMissingTranslationPrompt(
    languageName: String,
    canvasMuted: Color,
    onTranslateMissing: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Translation in $languageName not yet generated.",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = canvasMuted
        )
        TextButton(
            onClick = onTranslateMissing,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.testTag("translate_missing_inline_button")
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Translate", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ReviewStatusBadge(status: String, versionNumber: Int) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (status == TranslationReviewStatus.EXPERT_VERIFIED || status == TranslationReviewStatus.PUBLISHER_APPROVED) {
                    Icons.Default.Verified
                } else {
                    Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "v$versionNumber • $status",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ContributorLicenseAgreementDialog(
    pendingAction: PendingContributorAction,
    onConfirmAndProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    val book = pendingAction.book
    val isShareable = BookRightsCategory.allowsSharedRepositoryContribution(book.rightsCategory)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Gavel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Lipi Derivative & Translation Licence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${book.title} (${book.rightsCategory})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Zero-Host Peer-to-Peer Policy: Lipi never hosts or stores original restricted PDF/EPUB files. By converting or requesting this translation (${pendingAction.targetLanguage}), you agree that Lipi owns and permanently retains the structured OCR segments and resulting translation derivatives in the Lipi Knowledge Commons.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isShareable) {
                                "Lipi Derivative Asset (${book.rightsCategory}): Original file is never hosted (0B retained); OCR & translation derivatives enter the Shared Translation Repository for reuse."
                            } else {
                                "Private Upload (${book.rightsCategory}): Strictly isolated to your personal account (PRIVATE policy)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmAndProceed,
                modifier = Modifier.testTag("accept_contributor_license_button")
            ) {
                Text("Agree & Translate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
