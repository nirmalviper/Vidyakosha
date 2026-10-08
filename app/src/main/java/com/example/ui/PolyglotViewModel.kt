package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.BilingualParagraphItem
import com.example.data.local.BookEntity
import com.example.data.local.BookRightsCategory
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommercialPlanEntity
import com.example.data.local.ContinuousSectionBlock
import com.example.data.local.ContributorConsentEntity
import com.example.data.local.CorpusAnalyticsEventEntity
import com.example.data.local.CorpusAnalyticsSummary
import com.example.data.local.CrossBookSearchResultItem
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.OcrCorrectionAuditEntity
import com.example.data.local.OfflineBookPackageEntity
import com.example.data.local.ParagraphEntity
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.PlatformMetadataEntity
import com.example.data.local.ProcessingJobEntity
import com.example.data.local.ReadingHistoryEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.SectionEntity
import com.example.data.local.SuperadminDashboardOverview
import com.example.data.local.TranslationDiscoveryStatus
import com.example.data.local.TranslationIssueReportEntity
import com.example.data.local.TranslationMemoryEntity
import com.example.data.local.TransliterationLearningEntity
import com.example.data.local.UserAiConfigEntity
import com.example.data.local.UserBookAccessEntity
import com.example.data.local.UserEntitlementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.repository.PolyglotRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class TopLevelDestination {
    HOW_IT_WORKS,
    DIRECTORY,
    MY_HOME,
    START_READING,
    SHARED_CORPUS_HUB,
    AI_SETTINGS,
    AUTH_PORTAL,
    SUPERADMIN_DASHBOARD
}

enum class ReaderWorkspaceTab {
    READER,
    SEARCH_AND_BOOKMARKS,
    ASK_BOOK
}

enum class ReaderDisplayMode(val label: String) {
    BILINGUAL_SIDE_BY_SIDE("Side-by-Side"),
    STACKED_CARDS("Stacked"),
    ORIGINAL_ONLY("Original Only"),
    TRANSLATION_ONLY("Translated Only"),
    SCHOLARLY_SCRIPT_MODE("Script & Word Study")
}

data class PendingContributorAction(
    val book: BookEntity,
    val sectionId: Long?,
    val paragraphId: Long?,
    val targetLanguage: String,
    val forceRetranslate: Boolean
)

data class PolyglotUiState(
    // Auth & Account
    val currentUser: UserEntity? = null,
    val isSignUpMode: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,

    // User-Owned AI Credentials
    val userAiConfigs: List<UserAiConfigEntity> = emptyList(),
    val isTestingAiConnection: Boolean = false,
    val aiSettingsStatusBanner: String? = null,
    val aiSettingsStatusIsError: Boolean = false,

    // Entitlements, Plans, Rights & Contributor Consents
    val activeEntitlements: List<UserEntitlementEntity> = emptyList(),
    val commercialPlans: List<CommercialPlanEntity> = emptyList(),
    val contributorConsents: List<ContributorConsentEntity> = emptyList(),
    val pendingContributorAction: PendingContributorAction? = null,
    val isPlansDialogVisible: Boolean = false,

    // Superadmin Backend State (Protected by Superadmin Role)
    val allSystemUsers: List<UserEntity> = emptyList(),
    val allSystemEntitlements: List<UserEntitlementEntity> = emptyList(),
    val platformMetadataList: List<PlatformMetadataEntity> = emptyList(),
    val adminAuditLogs: List<AdminAuditLogEntity> = emptyList(),
    val superadminOverview: SuperadminDashboardOverview = SuperadminDashboardOverview(),
    val adminActionBanner: String? = null,
    val adminActionIsError: Boolean = false,

    // Navigation & Book Catalog
    val topDestination: TopLevelDestination = TopLevelDestination.HOW_IT_WORKS,
    val directoryBooks: List<BookEntity> = emptyList(),
    val allSystemBooks: List<BookEntity> = emptyList(),
    val userAccessibleBooks: List<BookEntity> = emptyList(),
    val userBookAccessList: List<UserBookAccessEntity> = emptyList(),
    val directorySearchQuery: String = "",
    val directoryActiveFilter: String = "ALL",

    // Personal Continuity, Bookmarks, History & Offline Packages
    val readingProgressList: List<ReadingProgressEntity> = emptyList(),
    val allUserBookmarks: List<BookmarkHighlightEntity> = emptyList(),
    val readingHistory: List<ReadingHistoryEntity> = emptyList(),
    val offlinePackages: List<OfflineBookPackageEntity> = emptyList(),

    // Shared Translation Repository, TM, Issues, Jobs & Corpus Analytics
    val allCanonicalTranslations: List<ParagraphTranslationEntity> = emptyList(),
    val translationMemoryEntries: List<TranslationMemoryEntity> = emptyList(),
    val translationIssueReports: List<TranslationIssueReportEntity> = emptyList(),
    val recentProcessingJobs: List<ProcessingJobEntity> = emptyList(),
    val analyticsEvents: List<CorpusAnalyticsEventEntity> = emptyList(),
    val corpusAnalyticsSummary: CorpusAnalyticsSummary = CorpusAnalyticsSummary(),

    // Active Book & Structured Reader State
    val activeBook: BookEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val sections: List<SectionEntity> = emptyList(),
    val activeChapter: ChapterEntity? = null,
    val activeSection: SectionEntity? = null,
    val sectionParagraphs: List<BilingualParagraphItem> = emptyList(),
    val continuousBlocks: List<ContinuousSectionBlock> = emptyList(),
    val discoveryStatus: TranslationDiscoveryStatus? = null,
    val cachedLanguagesForActiveBook: List<String> = emptyList(),
    val bookmarksForActiveBook: List<BookmarkHighlightEntity> = emptyList(),
    val ocrAuditsForActiveBook: List<OcrCorrectionAuditEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),

    // Reader Preferences & Controls
    val selectedLanguage: LanguageDescriptor = LanguageRegistry.findByNameOrCode("Gujarati"),
    val userTopLanguages: List<LanguageDescriptor> = LanguageRegistry.parseTopThreeLanguages(null),
    val workspaceTab: ReaderWorkspaceTab = ReaderWorkspaceTab.READER,
    val readerDisplayMode: ReaderDisplayMode = ReaderDisplayMode.BILINGUAL_SIDE_BY_SIDE,
    val fontScaleMultiplier: Float = 1.0f,
    val showTransliteration: Boolean = true,
    val showWordByWordBreakdown: Boolean = false,
    val autoTranslateOnScroll: Boolean = true,
    val continuousScrollNotice: String? = null,

    // Processing Status
    val isIngestingPdf: Boolean = false,
    val ingestionStageMessage: String = "",
    val ingestionProcessedPages: Int = 0,
    val ingestionTotalPages: Int = 0,
    val isTranslatingSection: Boolean = false,
    val translatingParagraphIds: Set<Long> = emptySet(),
    val isAskingBook: Boolean = false,
    val errorBannerMessage: String? = null,

    // Cross-Book & In-Book Search
    val searchQuery: String = "",
    val searchAcrossAllBooks: Boolean = false,
    val crossBookSearchResults: List<CrossBookSearchResultItem> = emptyList(),
    val isSearching: Boolean = false,

    // TTS Audio State
    val speakingParagraphId: Long? = null,
    val isSpeakingOriginal: Boolean = false,
    val isPlayingFullSectionAudio: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class PolyglotViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).polyglotDao()
    private val repository = PolyglotRepository(
        dao = dao,
        appContext = application.applicationContext
    )

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    private val _isSignUpMode = MutableStateFlow(false)
    private val _isAuthLoading = MutableStateFlow(false)
    private val _authErrorMessage = MutableStateFlow<String?>(null)

    private val _isTestingAiConnection = MutableStateFlow(false)
    private val _aiSettingsStatusBanner = MutableStateFlow<String?>(null)
    private val _aiSettingsStatusIsError = MutableStateFlow(false)

    private val _pendingContributorAction = MutableStateFlow<PendingContributorAction?>(null)
    private val _isPlansDialogVisible = MutableStateFlow(false)

    private val _topDestination = MutableStateFlow(TopLevelDestination.HOW_IT_WORKS)
    private val _directorySearchQuery = MutableStateFlow("")
    private val _directoryActiveFilter = MutableStateFlow("ALL")

    private val _activeBookId = MutableStateFlow<Long?>(null)
    private val _activeSectionId = MutableStateFlow<Long?>(null)
    private val _selectedLanguage = MutableStateFlow(LanguageRegistry.findByNameOrCode("Gujarati"))
    private val _userTopLanguages = MutableStateFlow(LanguageRegistry.parseTopThreeLanguages(null))
    private val _workspaceTab = MutableStateFlow(ReaderWorkspaceTab.READER)
    private val _readerDisplayMode = MutableStateFlow(ReaderDisplayMode.BILINGUAL_SIDE_BY_SIDE)
    private val _fontScaleMultiplier = MutableStateFlow(1.0f)
    private val _showTransliteration = MutableStateFlow(true)
    private val _showWordByWordBreakdown = MutableStateFlow(false)
    private val _autoTranslateOnScroll = MutableStateFlow(true)
    private val _continuousScrollNotice = MutableStateFlow<String?>(null)

    private val _isIngestingPdf = MutableStateFlow(false)
    private val _ingestionStageMessage = MutableStateFlow("")
    private val _ingestionProcessedPages = MutableStateFlow(0)
    private val _ingestionTotalPages = MutableStateFlow(0)
    private val _isTranslatingSection = MutableStateFlow(false)
    private val _translatingParagraphIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isAskingBook = MutableStateFlow(false)
    private val _errorBannerMessage = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    private val _searchAcrossAllBooks = MutableStateFlow(false)
    private val _crossBookSearchResults = MutableStateFlow<List<CrossBookSearchResultItem>>(emptyList())
    private val _isSearching = MutableStateFlow(false)

    private val _speakingParagraphId = MutableStateFlow<Long?>(null)
    private val _isSpeakingOriginal = MutableStateFlow(false)
    private val _isPlayingFullSectionAudio = MutableStateFlow(false)
    private var fullSectionQueue: List<BilingualParagraphItem> = emptyList()
    private var fullSectionQueueIndex: Int = 0
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val allSystemBooksFlow = repository.getAllBooksInSystemFlow()
    private val allCanonicalTranslationsFlow = repository.getAllCanonicalTranslationsInSystemFlow()
    private val allTranslationMemoryFlow = repository.getAllTranslationMemoryFlow()
    private val allTranslationIssueReportsFlow = repository.getAllTranslationIssueReportsFlow()
    private val recentJobsFlow = repository.getRecentProcessingJobsFlow()
    private val analyticsEventsFlow = repository.getAllAnalyticsEventsFlow()
    private val allUsersFlow = repository.getAllUsersFlow()
    private val allSystemEntitlementsFlow = repository.getAllEntitlementsInSystemFlow()
    private val commercialPlansFlow = repository.getAllCommercialPlansFlow()
    private val platformMetadataFlow = repository.getAllPlatformMetadataFlow()
    private val adminAuditLogsFlow = repository.getAllAdminAuditLogsFlow()
    private val _adminActionBanner = MutableStateFlow<String?>(null)
    private val _adminActionIsError = MutableStateFlow(false)

    private val userAccessibleBooksFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getAccessibleBooksForUserFlow(user.id)
    }

    private val userBookAccessFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getUserBookAccessFlow(user.id)
    }

    private val userAiConfigsFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getAiConfigsForUserFlow(user.id)
    }

    private val activeEntitlementsFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getActiveEntitlementsForUserFlow(user.id)
    }

    private val contributorConsentsFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getContributorConsentsForUserFlow(user.id)
    }

    private val offlinePackagesFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getOfflinePackagesForUserFlow(user.id)
    }

    private val readingProgressListFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getAllReadingProgressForUserFlow(user.id)
    }

    private val allUserBookmarksFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getAllBookmarksForUserFlow(user.id)
    }

    private val readingHistoryFlow = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else repository.getReadingHistoryForUserFlow(user.id)
    }

    private val chaptersFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getChaptersForBookFlow(bookId)
    }

    private val sectionsFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getSectionsForBookFlow(bookId)
    }

    private val bookParagraphsFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getParagraphsForBookFlow(bookId)
    }

    private val activeTranslationsForBookFlow = combine(_activeBookId, _selectedLanguage) { bookId, lang ->
        bookId to lang.name
    }.flatMapLatest { pair ->
        val bookId = pair.first
        val langName = pair.second
        if (bookId == null) flowOf(emptyList()) else repository.getActiveTranslationsForBookFlow(bookId, langName)
    }

    private val allTranslationVersionsForBookFlow = combine(_activeBookId, _selectedLanguage) { bookId, lang ->
        bookId to lang.name
    }.flatMapLatest { pair ->
        val bookId = pair.first
        val langName = pair.second
        if (bookId == null) flowOf(emptyList()) else repository.getAllTranslationVersionsForBookFlow(bookId, langName)
    }

    private val transliterationsForBookFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getTransliterationsForBookFlow(bookId)
    }

    private val ocrAuditsForBookFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getOcrAuditsForBookFlow(bookId)
    }

    private val cachedLanguagesFlow = _activeBookId.flatMapLatest { bookId ->
        if (bookId == null) flowOf(emptyList()) else repository.getCachedLanguagesForBookFlow(bookId)
    }

    private val bookmarksForActiveBookFlow = combine(_currentUser, _activeBookId) { user, bookId ->
        user?.id to bookId
    }.flatMapLatest { pair ->
        val userId = pair.first
        val bookId = pair.second
        if (userId == null || bookId == null) flowOf(emptyList())
        else repository.getBookmarksForUserAndBookFlow(userId, bookId)
    }

    private val chatMessagesFlow = combine(_currentUser, _activeBookId) { user, bookId ->
        user?.id to bookId
    }.flatMapLatest { pair ->
        val userId = pair.first
        val bookId = pair.second
        if (bookId == null) flowOf(emptyList())
        else repository.getMessagesForUserAndBookFlow(userId ?: 0L, bookId)
    }

    val uiState: StateFlow<PolyglotUiState> = combine(
        listOf(
            _currentUser,
            _isSignUpMode,
            _isAuthLoading,
            _authErrorMessage,
            userAiConfigsFlow,
            _isTestingAiConnection,
            _aiSettingsStatusBanner,
            _aiSettingsStatusIsError,
            activeEntitlementsFlow,
            contributorConsentsFlow,
            _pendingContributorAction,
            _isPlansDialogVisible,
            _topDestination,
            allSystemBooksFlow,
            userAccessibleBooksFlow,
            userBookAccessFlow,
            _directorySearchQuery,
            _directoryActiveFilter,
            readingProgressListFlow,
            allUserBookmarksFlow,
            readingHistoryFlow,
            offlinePackagesFlow,
            allCanonicalTranslationsFlow,
            allTranslationMemoryFlow,
            allTranslationIssueReportsFlow,
            recentJobsFlow,
            analyticsEventsFlow,
            _activeBookId,
            chaptersFlow,
            sectionsFlow,
            _activeSectionId,
            bookParagraphsFlow,
            activeTranslationsForBookFlow,
            allTranslationVersionsForBookFlow,
            transliterationsForBookFlow,
            ocrAuditsForBookFlow,
            cachedLanguagesFlow,
            bookmarksForActiveBookFlow,
            chatMessagesFlow,
            _selectedLanguage,
            _workspaceTab,
            _readerDisplayMode,
            _fontScaleMultiplier,
            _showTransliteration,
            _showWordByWordBreakdown,
            _autoTranslateOnScroll,
            _continuousScrollNotice,
            _isIngestingPdf,
            _ingestionStageMessage,
            _ingestionProcessedPages,
            _ingestionTotalPages,
            _isTranslatingSection,
            _translatingParagraphIds,
            _isAskingBook,
            _errorBannerMessage,
            _searchQuery,
            _searchAcrossAllBooks,
            _crossBookSearchResults,
            _isSearching,
            _speakingParagraphId,
            _isSpeakingOriginal,
            _isPlayingFullSectionAudio,
            _userTopLanguages,
            allUsersFlow,
            allSystemEntitlementsFlow,
            commercialPlansFlow,
            platformMetadataFlow,
            adminAuditLogsFlow,
            _adminActionBanner,
            _adminActionIsError
        )
    ) { values ->
        val currentUser = values[0] as UserEntity?
        val isSignUpMode = values[1] as Boolean
        val isAuthLoading = values[2] as Boolean
        val authErrorMessage = values[3] as String?
        @Suppress("UNCHECKED_CAST")
        val userAiConfigs = values[4] as List<UserAiConfigEntity>
        val isTestingAiConnection = values[5] as Boolean
        val aiSettingsStatusBanner = values[6] as String?
        val aiSettingsStatusIsError = values[7] as Boolean
        @Suppress("UNCHECKED_CAST")
        val activeEntitlements = values[8] as List<UserEntitlementEntity>
        @Suppress("UNCHECKED_CAST")
        val contributorConsents = values[9] as List<ContributorConsentEntity>
        val pendingContributorAction = values[10] as PendingContributorAction?
        val isPlansDialogVisible = values[11] as Boolean
        val topDestination = values[12] as TopLevelDestination
        @Suppress("UNCHECKED_CAST")
        val allSystemBooks = values[13] as List<BookEntity>
        @Suppress("UNCHECKED_CAST")
        val userAccessibleBooks = values[14] as List<BookEntity>
        @Suppress("UNCHECKED_CAST")
        val userBookAccessList = values[15] as List<UserBookAccessEntity>
        val directorySearchQuery = values[16] as String
        val directoryActiveFilter = values[17] as String
        @Suppress("UNCHECKED_CAST")
        val readingProgressList = values[18] as List<ReadingProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val allUserBookmarks = values[19] as List<BookmarkHighlightEntity>
        @Suppress("UNCHECKED_CAST")
        val readingHistory = values[20] as List<ReadingHistoryEntity>
        @Suppress("UNCHECKED_CAST")
        val offlinePackages = values[21] as List<OfflineBookPackageEntity>
        @Suppress("UNCHECKED_CAST")
        val allCanonicalTranslations = values[22] as List<ParagraphTranslationEntity>
        @Suppress("UNCHECKED_CAST")
        val translationMemoryEntries = values[23] as List<TranslationMemoryEntity>
        @Suppress("UNCHECKED_CAST")
        val translationIssueReports = values[24] as List<TranslationIssueReportEntity>
        @Suppress("UNCHECKED_CAST")
        val recentProcessingJobs = values[25] as List<ProcessingJobEntity>
        @Suppress("UNCHECKED_CAST")
        val analyticsEvents = values[26] as List<CorpusAnalyticsEventEntity>
        val activeBookId = values[27] as Long?
        @Suppress("UNCHECKED_CAST")
        val chapters = values[28] as List<ChapterEntity>
        @Suppress("UNCHECKED_CAST")
        val sections = values[29] as List<SectionEntity>
        val activeSectionId = values[30] as Long?
        @Suppress("UNCHECKED_CAST")
        val bookParagraphs = values[31] as List<ParagraphEntity>
        @Suppress("UNCHECKED_CAST")
        val activeTranslations = values[32] as List<ParagraphTranslationEntity>
        @Suppress("UNCHECKED_CAST")
        val allVersionsForBook = values[33] as List<ParagraphTranslationEntity>
        @Suppress("UNCHECKED_CAST")
        val transliterationsForBook = values[34] as List<TransliterationLearningEntity>
        @Suppress("UNCHECKED_CAST")
        val ocrAuditsForBook = values[35] as List<OcrCorrectionAuditEntity>
        @Suppress("UNCHECKED_CAST")
        val cachedLanguages = values[36] as List<String>
        @Suppress("UNCHECKED_CAST")
        val bookmarksForActiveBook = values[37] as List<BookmarkHighlightEntity>
        @Suppress("UNCHECKED_CAST")
        val chatMessages = values[38] as List<ChatMessageEntity>
        val selectedLanguage = values[39] as LanguageDescriptor
        val workspaceTab = values[40] as ReaderWorkspaceTab
        val readerDisplayMode = values[41] as ReaderDisplayMode
        val fontScale = values[42] as Float
        val showTransliteration = values[43] as Boolean
        val showWordByWord = values[44] as Boolean
        val autoTranslate = values[45] as Boolean
        val scrollNotice = values[46] as String?
        val isIngestingPdf = values[47] as Boolean
        val ingestionStageMessage = values[48] as String
        val ingestionProcessedPages = values[49] as Int
        val ingestionTotalPages = values[50] as Int
        val isTranslatingSection = values[51] as Boolean
        @Suppress("UNCHECKED_CAST")
        val translatingParagraphIds = values[52] as Set<Long>
        val isAskingBook = values[53] as Boolean
        val errorBannerMessage = values[54] as String?
        val searchQuery = values[55] as String
        val searchAcrossAllBooks = values[56] as Boolean
        @Suppress("UNCHECKED_CAST")
        val crossBookSearchResults = values[57] as List<CrossBookSearchResultItem>
        val isSearching = values[58] as Boolean
        val speakingParagraphId = values[59] as Long?
        val isSpeakingOriginal = values[60] as Boolean
        val isPlayingFullSectionAudio = values[61] as Boolean
        @Suppress("UNCHECKED_CAST")
        val rawUserTopLanguages = values[62] as List<LanguageDescriptor>
        val userTopLanguages = if (currentUser != null && currentUser.topThreeLanguagesCsv.isNotBlank()) {
            LanguageRegistry.parseTopThreeLanguages(currentUser.topThreeLanguagesCsv, currentUser.preferredLanguage)
        } else {
            rawUserTopLanguages.take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES)
        }
        val activeTopCodes = userTopLanguages.map { it.name.lowercase() }.toSet()
        val scopedCachedLanguages = cachedLanguages.filter { it.lowercase() in activeTopCodes }

        val userId = currentUser?.id ?: 0L
        @Suppress("UNCHECKED_CAST")
        val allUsers = values[63] as List<UserEntity>
        @Suppress("UNCHECKED_CAST")
        val allSystemEntitlements = values[64] as List<UserEntitlementEntity>
        @Suppress("UNCHECKED_CAST")
        val commercialPlans = values[65] as List<CommercialPlanEntity>
        @Suppress("UNCHECKED_CAST")
        val platformMetadataList = values[66] as List<PlatformMetadataEntity>
        @Suppress("UNCHECKED_CAST")
        val adminAuditLogs = values[67] as List<AdminAuditLogEntity>
        val adminActionBanner = values[68] as String?
        val adminActionIsError = values[69] as Boolean

        val discoverableDirectoryBooks = repository.bookApi.filterDiscoverableBooks(
            allBooks = allSystemBooks,
            userId = userId,
            entitlements = activeEntitlements,
            commercialPlans = commercialPlans
        )

        val superadminOverview = repository.superadminApi.buildDashboardOverview(
            users = allUsers,
            entitlements = allSystemEntitlements,
            books = allSystemBooks,
            translations = allCanonicalTranslations,
            auditLogs = adminAuditLogs
        )

        val activeBook = allSystemBooks.find { it.id == activeBookId }
        val activeSection = sections.find { it.id == activeSectionId } ?: sections.firstOrNull()
        val activeChapter = chapters.find { it.id == activeSection?.chapterId } ?: chapters.firstOrNull()

        val activeTranslationMap = activeTranslations.associateBy { it.paragraphId }
        val versionsByParagraphMap = allVersionsForBook.groupBy { it.paragraphId }
        val transliterationMap = transliterationsForBook.associateBy { it.paragraphId }
        val bookmarkMap = bookmarksForActiveBook.associateBy { it.paragraphId }

        val activeSectionParagraphs = bookParagraphs
            .filter { it.sectionId == activeSection?.id }
            .sortedBy { it.paragraphOrder }
            .map { p ->
                val bookmark = bookmarkMap[p.id]
                val activeTrans = activeTranslationMap[p.id]
                val versions = versionsByParagraphMap[p.id] ?: emptyList()
                val requiresReview = activeTrans != null &&
                    activeTrans.sourceContentHash.isNotBlank() &&
                    p.sourceContentHash.isNotBlank() &&
                    activeTrans.sourceContentHash != p.sourceContentHash

                BilingualParagraphItem(
                    paragraph = p,
                    activeTranslation = activeTrans,
                    allVersions = versions,
                    allVersionsCount = versions.size,
                    requiresSourceReview = requiresReview,
                    transliterationLearning = transliterationMap[p.id],
                    isBookmarked = bookmark != null,
                    highlightSnippet = bookmark?.userNote
                )
            }

        val discoveryStatus = if (activeBook != null && bookParagraphs.isNotEmpty()) {
            repository.translationApi.buildDiscoveryStatus(
                targetLanguage = selectedLanguage,
                paragraphs = bookParagraphs,
                allVersionsForLanguage = allVersionsForBook
            )
        } else null

        val reuseEvents = analyticsEvents.filter {
            it.eventType == "TRANSLATION_REUSED" || it.eventType == "TM_EXACT_MATCH_REUSED"
        }
        val langBreakdown = allCanonicalTranslations
            .filter { it.isActiveVersion }
            .groupingBy { it.targetLanguage }
            .eachCount()

        val corpusSummary = CorpusAnalyticsSummary(
            totalBooksInCatalog = allSystemBooks.size,
            totalParagraphsStored = bookParagraphs.size.coerceAtLeast(allCanonicalTranslations.map { it.paragraphId }.distinct().size),
            totalCanonicalTranslations = allCanonicalTranslations.size,
            totalTranslationMemorySegments = translationMemoryEntries.size,
            aiCallsAvoidedByReuse = reuseEvents.size + allCanonicalTranslations.sumOf { (it.reuseCount - 1).coerceAtLeast(0) },
            estimatedTokensSaved = reuseEvents.sumOf { it.tokensSavedOrUsed } + allCanonicalTranslations.sumOf { (it.reuseCount - 1).coerceAtLeast(0) * it.estimatedTokens },
            expertVerifiedTranslationsCount = allCanonicalTranslations.count {
                it.reviewStatus == "EXPERT_VERIFIED" || it.reviewStatus == "PUBLISHER_APPROVED" || it.reviewStatus == "HUMAN_REVIEWED"
            },
            openQualityReportsCount = translationIssueReports.count { it.resolutionStatus == "OPEN" },
            activeOrCompletedJobsCount = recentProcessingJobs.size,
            languageCoverageBreakdown = langBreakdown
        )

        PolyglotUiState(
            currentUser = currentUser,
            isSignUpMode = isSignUpMode,
            isAuthLoading = isAuthLoading,
            authErrorMessage = authErrorMessage,
            userAiConfigs = userAiConfigs,
            isTestingAiConnection = isTestingAiConnection,
            aiSettingsStatusBanner = aiSettingsStatusBanner,
            aiSettingsStatusIsError = aiSettingsStatusIsError,
            activeEntitlements = activeEntitlements,
            commercialPlans = commercialPlans,
            contributorConsents = contributorConsents,
            pendingContributorAction = pendingContributorAction,
            isPlansDialogVisible = isPlansDialogVisible,
            allSystemUsers = allUsers,
            allSystemEntitlements = allSystemEntitlements,
            platformMetadataList = platformMetadataList,
            adminAuditLogs = adminAuditLogs,
            superadminOverview = superadminOverview,
            adminActionBanner = adminActionBanner,
            adminActionIsError = adminActionIsError,
            topDestination = topDestination,
            directoryBooks = discoverableDirectoryBooks,
            allSystemBooks = allSystemBooks,
            userAccessibleBooks = userAccessibleBooks,
            userBookAccessList = userBookAccessList,
            directorySearchQuery = directorySearchQuery,
            directoryActiveFilter = directoryActiveFilter,
            readingProgressList = readingProgressList,
            allUserBookmarks = allUserBookmarks,
            readingHistory = readingHistory,
            offlinePackages = offlinePackages,
            allCanonicalTranslations = allCanonicalTranslations,
            translationMemoryEntries = translationMemoryEntries,
            translationIssueReports = translationIssueReports,
            recentProcessingJobs = recentProcessingJobs,
            analyticsEvents = analyticsEvents,
            corpusAnalyticsSummary = corpusSummary,
            activeBook = activeBook,
            chapters = chapters,
            sections = sections,
            activeChapter = activeChapter,
            activeSection = activeSection,
            sectionParagraphs = activeSectionParagraphs,
            discoveryStatus = discoveryStatus,
            cachedLanguagesForActiveBook = scopedCachedLanguages,
            bookmarksForActiveBook = bookmarksForActiveBook,
            ocrAuditsForActiveBook = ocrAuditsForBook,
            chatMessages = chatMessages,
            selectedLanguage = selectedLanguage,
            userTopLanguages = userTopLanguages,
            workspaceTab = workspaceTab,
            readerDisplayMode = readerDisplayMode,
            fontScaleMultiplier = fontScale,
            showTransliteration = showTransliteration,
            showWordByWordBreakdown = showWordByWord,
            autoTranslateOnScroll = autoTranslate,
            continuousScrollNotice = scrollNotice,
            isIngestingPdf = isIngestingPdf,
            ingestionStageMessage = ingestionStageMessage,
            ingestionProcessedPages = ingestionProcessedPages,
            ingestionTotalPages = ingestionTotalPages,
            isTranslatingSection = isTranslatingSection,
            translatingParagraphIds = translatingParagraphIds,
            isAskingBook = isAskingBook,
            errorBannerMessage = errorBannerMessage,
            searchQuery = searchQuery,
            searchAcrossAllBooks = searchAcrossAllBooks,
            crossBookSearchResults = crossBookSearchResults,
            isSearching = isSearching,
            speakingParagraphId = speakingParagraphId,
            isSpeakingOriginal = isSpeakingOriginal,
            isPlayingFullSectionAudio = isPlayingFullSectionAudio
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PolyglotUiState()
    )

    init {
        initTextToSpeech()
        initializeSessionAndPublicCatalog()
    }

    private fun initializeSessionAndPublicCatalog() {
        viewModelScope.launch {
            val sampleBook = repository.ensurePublicCatalogInitialized()
            val restoredUser = repository.restoreAuthenticatedUser()
            _currentUser.value = restoredUser
            val topThree = repository.getRestoredTopThreeLanguages(restoredUser)
            _userTopLanguages.value = topThree

            if (restoredUser != null) {
                val pref = LanguageRegistry.findByNameOrCode(restoredUser.preferredLanguage)
                _selectedLanguage.value = if (topThree.any { it.code == pref.code }) pref else topThree.first()
                _topDestination.value = TopLevelDestination.MY_HOME
                val recent = repository.getMostRecentReadingProgressForUser(restoredUser.id)
                val targetBookId = recent?.bookId ?: sampleBook.id
                selectBookInternal(targetBookId, navigateToReader = false)
            } else {
                _selectedLanguage.value = topThree.first()
                _topDestination.value = TopLevelDestination.HOW_IT_WORKS
                selectBookInternal(sampleBook.id, navigateToReader = false)
            }
        }
    }

    // =====================================================================
    // 1. NAVIGATION, AUTHENTICATION & SUBSCRIPTION TIERS
    // =====================================================================

    fun navigateToDestination(destination: TopLevelDestination) {
        stopSpeaking()
        val user = _currentUser.value
        if (destination == TopLevelDestination.SUPERADMIN_DASHBOARD) {
            if (user == null || !user.role.equals(UserRole.SUPERADMIN, ignoreCase = true)) {
                _errorBannerMessage.value =
                    "Access Denied (403): Only authenticated users with the 'superadmin' role can access the Superadmin Backend Dashboard."
                if (user == null) {
                    openAuthPortal(signUpMode = false)
                }
                return
            }
        }
        if (user == null && (destination == TopLevelDestination.MY_HOME || destination == TopLevelDestination.AI_SETTINGS)) {
            openAuthPortal(signUpMode = false)
            return
        }
        _topDestination.value = destination
    }

    fun authenticateAsSuperadminAndOpenDashboard() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.authenticateDefaultSuperadminSession()
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { superadminUser ->
                    _currentUser.value = superadminUser
                    val topThree = repository.getRestoredTopThreeLanguages(superadminUser)
                    _userTopLanguages.value = topThree
                    _topDestination.value = TopLevelDestination.SUPERADMIN_DASHBOARD
                },
                onFailure = { err ->
                    _authErrorMessage.value = err.message ?: "Unable to authenticate Superadmin session."
                }
            )
        }
    }

    fun openAuthPortal(signUpMode: Boolean = false) {
        _isSignUpMode.value = signUpMode
        _authErrorMessage.value = null
        _topDestination.value = TopLevelDestination.AUTH_PORTAL
    }

    fun toggleAuthMode(signUpMode: Boolean) {
        _isSignUpMode.value = signUpMode
        _authErrorMessage.value = null
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.loginUser(email, password)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    val topThree = repository.getRestoredTopThreeLanguages(user)
                    _userTopLanguages.value = topThree
                    val pref = LanguageRegistry.findByNameOrCode(user.preferredLanguage)
                    _selectedLanguage.value = if (topThree.any { it.code == pref.code }) pref else topThree.first()
                    _topDestination.value = TopLevelDestination.MY_HOME
                },
                onFailure = { err ->
                    _authErrorMessage.value = err.message ?: "Unable to sign in."
                }
            )
        }
    }

    fun signUp(displayName: String, email: String, password: String, preferredLanguage: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            val result = repository.registerUser(displayName, email, password, preferredLanguage)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    val topThree = repository.getRestoredTopThreeLanguages(user)
                    _userTopLanguages.value = topThree
                    val pref = LanguageRegistry.findByNameOrCode(user.preferredLanguage)
                    _selectedLanguage.value = if (topThree.any { it.code == pref.code }) pref else topThree.first()
                    _topDestination.value = TopLevelDestination.MY_HOME
                },
                onFailure = { err ->
                    _authErrorMessage.value = err.message ?: "Unable to create account."
                }
            )
        }
    }

    fun logout() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            stopSpeaking()
            repository.logoutUser(user.id)
            _currentUser.value = null
            _topDestination.value = TopLevelDestination.HOW_IT_WORKS
        }
    }

    fun openPlansDialog() {
        _isPlansDialogVisible.value = true
    }

    fun closePlansDialog() {
        _isPlansDialogVisible.value = false
    }

    fun switchSubscriptionTier(newTier: String, institutionCode: String = "") {
        val user = _currentUser.value
        if (user == null) {
            _isPlansDialogVisible.value = false
            openAuthPortal(signUpMode = false)
            return
        }
        viewModelScope.launch {
            val updated = repository.switchUserSubscriptionTier(user, newTier)
            if (institutionCode.isNotBlank()) {
                repository.grantInstitutionMembership(user.id, institutionCode)
            }
            _currentUser.value = updated
            _isPlansDialogVisible.value = false
        }
    }

    // =====================================================================
    // 2. USER-OWNED AI CREDENTIALS & MULTI-PROVIDER VAULT
    // =====================================================================

    fun saveUserAiConfig(
        existingConfigId: Long?,
        providerId: String,
        modelName: String,
        rawApiKey: String,
        customBaseUrl: String,
        useForOcr: Boolean,
        useForTranslation: Boolean,
        useForChat: Boolean
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val result = repository.saveOrUpdateUserAiConfig(
                userId = user.id,
                existingConfigId = existingConfigId,
                providerId = providerId,
                modelName = modelName,
                rawApiKey = rawApiKey,
                customBaseUrl = customBaseUrl,
                useForOcr = useForOcr,
                useForTranslation = useForTranslation,
                useForChat = useForChat
            )
            result.fold(
                onSuccess = { saved ->
                    _aiSettingsStatusIsError.value = false
                    _aiSettingsStatusBanner.value =
                        "Saved ${saved.providerDisplayName} (${saved.modelName}) encrypted in your account vault."
                },
                onFailure = { err ->
                    _aiSettingsStatusIsError.value = true
                    _aiSettingsStatusBanner.value = err.message ?: "Failed to save AI configuration."
                }
            )
        }
    }

    fun testUserAiConnection(
        configId: Long?,
        providerId: String,
        modelName: String,
        rawApiKeyInput: String,
        customBaseUrl: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isTestingAiConnection.value = true
            _aiSettingsStatusIsError.value = false
            _aiSettingsStatusBanner.value = "Testing connection with provider…"
            val result = repository.testUserAiConfigConnection(
                userId = user.id,
                configId = configId,
                providerId = providerId,
                modelName = modelName,
                rawApiKeyInput = rawApiKeyInput,
                customBaseUrl = customBaseUrl
            )
            _isTestingAiConnection.value = false
            result.fold(
                onSuccess = { msg ->
                    _aiSettingsStatusIsError.value = false
                    _aiSettingsStatusBanner.value = "Connection Verified: $msg"
                },
                onFailure = { err ->
                    _aiSettingsStatusIsError.value = true
                    _aiSettingsStatusBanner.value = "Connection Failed: ${err.message}"
                }
            )
        }
    }

    fun deleteUserAiConfig(configId: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteUserAiConfig(user.id, configId)
            _aiSettingsStatusIsError.value = false
            _aiSettingsStatusBanner.value = "Removed AI provider credential."
        }
    }

    // =====================================================================
    // 3. PUBLIC DIRECTORY, BOOK ACCESS & CONTINUOUS READING
    // =====================================================================

    fun updateDirectorySearchQuery(query: String) {
        _directorySearchQuery.value = query
    }

    fun updateDirectoryFilter(filter: String) {
        _directoryActiveFilter.value = filter
    }

    fun openPublicSampleBook() {
        viewModelScope.launch {
            val sampleBook = repository.ensurePublicCatalogInitialized()
            selectBookInternal(sampleBook.id, navigateToReader = true)
        }
    }

    fun openBookForReading(book: BookEntity) {
        viewModelScope.launch {
            val userId = _currentUser.value?.id ?: 0L
            val canRead = repository.canUserReadBook(userId, book)
            if (!canRead) {
                if (userId <= 0L) {
                    _errorBannerMessage.value = "Please sign in to unlock or read '${book.title}'."
                    openAuthPortal(signUpMode = false)
                } else {
                    _errorBannerMessage.value =
                        "Unlock '${book.title}' (${book.priceDisplay}) or upgrade to Pro/Research to read this edition."
                    openPlansDialog()
                }
                return@launch
            }
            selectBookInternal(book.id, navigateToReader = true)
        }
    }

    fun unlockOrPurchaseBook(book: BookEntity) {
        val user = _currentUser.value
        if (user == null) {
            openAuthPortal(signUpMode = false)
            return
        }
        viewModelScope.launch {
            val result = repository.unlockOrPurchaseBookForUser(user.id, book)
            result.fold(
                onSuccess = {
                    selectBookInternal(book.id, navigateToReader = true)
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message
                }
            )
        }
    }

    private suspend fun selectBookInternal(bookId: Long, navigateToReader: Boolean) {
        stopSpeaking()
        _activeBookId.value = bookId
        _errorBannerMessage.value = null

        val userId = _currentUser.value?.id ?: 0L
        val savedProgress = if (userId > 0L) {
            repository.getUserReadingProgressForBook(userId, bookId)
        } else null

        if (savedProgress != null) {
            _activeSectionId.value = savedProgress.currentSectionId
            val savedLang = LanguageRegistry.findByNameOrCode(savedProgress.preferredLanguage)
            val topThree = _userTopLanguages.value
            _selectedLanguage.value = if (topThree.any { it.code == savedLang.code }) savedLang else topThree.first()
        } else {
            val chapters = repository.getChaptersForBook(bookId)
            val firstChapter = chapters.firstOrNull()
            val firstSection = if (firstChapter != null) {
                repository.getSectionsForChapter(firstChapter.id).firstOrNull()
            } else null
            _activeSectionId.value = firstSection?.id
        }

        if (navigateToReader) {
            _topDestination.value = TopLevelDestination.START_READING
            _workspaceTab.value = ReaderWorkspaceTab.READER
            val targetSectionId = _activeSectionId.value
            if (targetSectionId != null && _autoTranslateOnScroll.value) {
                triggerAutoTranslationOrReuseForSection(bookId, targetSectionId, _selectedLanguage.value.name)
            }
        }
    }

    fun selectSection(sectionId: Long, triggeredBySwipe: Boolean = false) {
        stopSpeaking()
        _activeSectionId.value = sectionId
        val book = uiState.value.activeBook ?: return

        viewModelScope.launch {
            val section = dao.getSectionById(sectionId) ?: return@launch
            val chapter = dao.getChaptersForBook(book.id).find { it.id == section.chapterId } ?: return@launch
            val paragraphs = dao.getParagraphsForSection(sectionId)
            val firstParaId = paragraphs.firstOrNull()?.id ?: 0L

            if (triggeredBySwipe) {
                _continuousScrollNotice.value = "Loaded ${section.heading}"
            }

            val allBookSections = uiState.value.sections
            val secIndex = allBookSections.indexOfFirst { it.id == sectionId }.coerceAtLeast(0)

            repository.saveUserReadingProgress(
                userId = _currentUser.value?.id ?: 0L,
                book = book,
                chapter = chapter,
                section = section,
                paragraphId = firstParaId,
                preferredLanguage = _selectedLanguage.value.name,
                completedSections = secIndex + 1,
                totalSections = allBookSections.size.coerceAtLeast(1)
            )

            if (_autoTranslateOnScroll.value) {
                triggerAutoTranslationOrReuseForSection(book.id, sectionId, _selectedLanguage.value.name)
            }
        }
    }

    fun advanceToNextSectionSeamless() {
        val currentSections = uiState.value.sections
        val currentActive = uiState.value.activeSection ?: return
        val currentIndex = currentSections.indexOfFirst { it.id == currentActive.id }
        if (currentIndex >= 0 && currentIndex < currentSections.size - 1) {
            val nextSection = currentSections[currentIndex + 1]
            selectSection(nextSection.id, triggeredBySwipe = true)
        } else {
            _continuousScrollNotice.value = "You have reached the final section of this book."
        }
    }

    fun goToPreviousSectionSeamless() {
        val currentSections = uiState.value.sections
        val currentActive = uiState.value.activeSection ?: return
        val currentIndex = currentSections.indexOfFirst { it.id == currentActive.id }
        if (currentIndex > 0) {
            val prevSection = currentSections[currentIndex - 1]
            selectSection(prevSection.id, triggeredBySwipe = true)
        }
    }

    fun clearContinuousScrollNotice() {
        _continuousScrollNotice.value = null
    }

    fun selectLanguage(descriptor: LanguageDescriptor) {
        stopSpeaking()
        _selectedLanguage.value = descriptor
        val user = _currentUser.value
        val book = uiState.value.activeBook
        val section = uiState.value.activeSection
        viewModelScope.launch {
            if (user != null) {
                val updatedUser = repository.updateUserPreferredLanguage(user, descriptor.name)
                _currentUser.value = updatedUser
                _userTopLanguages.value = repository.getRestoredTopThreeLanguages(updatedUser)
            } else {
                val currentTop = _userTopLanguages.value
                if (currentTop.none { it.code == descriptor.code }) {
                    val (_, normalized) = repository.updateUserTopThreeLanguages(
                        user = null,
                        selectedLanguages = (listOf(descriptor) + currentTop).take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES)
                    )
                    _userTopLanguages.value = normalized
                }
            }
            if (book != null && section != null && _autoTranslateOnScroll.value) {
                triggerAutoTranslationOrReuseForSection(book.id, section.id, descriptor.name)
            }
        }
    }

    fun updateUserTopThreeLanguages(selectedLanguages: List<LanguageDescriptor>) {
        if (selectedLanguages.isEmpty()) return
        stopSpeaking()
        val user = _currentUser.value
        viewModelScope.launch {
            val (updatedUser, normalizedTopThree) = repository.updateUserTopThreeLanguages(user, selectedLanguages)
            if (updatedUser != null) {
                _currentUser.value = updatedUser
            }
            _userTopLanguages.value = normalizedTopThree

            // Ensure active reading language is always one of the user's Top 3 languages
            if (normalizedTopThree.none { it.code == _selectedLanguage.value.code }) {
                val newActive = normalizedTopThree.first()
                _selectedLanguage.value = newActive
                val book = uiState.value.activeBook
                val section = uiState.value.activeSection
                if (book != null && section != null && _autoTranslateOnScroll.value) {
                    triggerAutoTranslationOrReuseForSection(book.id, section.id, newActive.name)
                }
            }

            _aiSettingsStatusIsError.value = false
            _aiSettingsStatusBanner.value =
                "Saved your Top ${normalizedTopThree.size} languages (${normalizedTopThree.joinToString(", ") { it.name }}). Only these 3 languages are active in the reader and queried on the server."
        }
    }

    fun setWorkspaceTab(tab: ReaderWorkspaceTab) {
        _workspaceTab.value = tab
    }

    fun setReaderDisplayMode(mode: ReaderDisplayMode) {
        _readerDisplayMode.value = mode
    }

    fun adjustFontScale(delta: Float) {
        _fontScaleMultiplier.value = (_fontScaleMultiplier.value + delta).coerceIn(0.85f, 1.45f)
    }

    fun toggleTransliteration() {
        _showTransliteration.value = !_showTransliteration.value
    }

    fun toggleWordByWordBreakdown() {
        _showWordByWordBreakdown.value = !_showWordByWordBreakdown.value
    }

    fun toggleAutoTranslateOnScroll() {
        _autoTranslateOnScroll.value = !_autoTranslateOnScroll.value
    }

    // =====================================================================
    // 4. SHARED TRANSLATION REPOSITORY, CONTRIBUTOR CONSENT & QUALITY WORKFLOW
    // =====================================================================

    private suspend fun triggerAutoTranslationOrReuseForSection(
        bookId: Long,
        sectionId: Long,
        targetLanguage: String
    ) {
        if (_isTranslatingSection.value) return
        val book = repository.getBookById(bookId) ?: return
        val section = dao.getSectionById(sectionId) ?: return
        val chapter = dao.getChaptersForBook(bookId).find { it.id == section.chapterId } ?: return
        val paragraphs = dao.getParagraphsForSection(sectionId)
        if (paragraphs.isEmpty()) return

        // Check if any paragraph lacks an active translation in this book
        val missingAny = paragraphs.any { p ->
            dao.getPreferredTranslationForParagraph(p.id, targetLanguage) == null
        }
        if (!missingAny) return

        val user = _currentUser.value
        val userId = user?.id ?: 0L
        val hasAiKey = userId > 0L && dao.getAiConfigsForUser(userId).isNotEmpty()
        val hasConsentedForBook = uiState.value.contributorConsents.any { it.bookId == book.id }

        // If the user has an AI key and hasn't accepted the contributor licence for this shareable book yet, prompt once
        if (hasAiKey &&
            BookRightsCategory.allowsSharedRepositoryContribution(book.rightsCategory) &&
            !hasConsentedForBook
        ) {
            _pendingContributorAction.value = PendingContributorAction(
                book = book,
                sectionId = sectionId,
                paragraphId = null,
                targetLanguage = targetLanguage,
                forceRetranslate = false
            )
            return
        }

        _isTranslatingSection.value = true
        val outcome = repository.translateSectionWithSharedRepoAndUserAi(
            userId = userId,
            userDisplayName = user?.displayName ?: "Guest Reader",
            userAcceptedContributorLicense = true,
            book = book,
            chapter = chapter,
            section = section,
            targetLanguage = targetLanguage,
            forceRetranslate = false
        )
        _isTranslatingSection.value = false

        outcome.fold(
            onSuccess = { res ->
                if (res.reusedCount > 0 || res.newlyGeneratedCount > 0) {
                    _continuousScrollNotice.value = res.noticeMessage
                }
            },
            onFailure = { err ->
                if (hasAiKey) {
                    _errorBannerMessage.value = err.message
                }
            }
        )
    }

    fun translateCurrentSection(forceRetranslate: Boolean = false) {
        val book = uiState.value.activeBook ?: return
        val section = uiState.value.activeSection ?: return
        val chapter = uiState.value.activeChapter ?: return
        val targetLang = _selectedLanguage.value.name
        val user = _currentUser.value

        if (user == null) {
            _errorBannerMessage.value =
                "Sign in and configure your AI API key in AI Settings to generate new AI translations."
            openAuthPortal(signUpMode = false)
            return
        }

        val hasConsentedForBook = uiState.value.contributorConsents.any { it.bookId == book.id }
        if (BookRightsCategory.allowsSharedRepositoryContribution(book.rightsCategory) && !hasConsentedForBook) {
            _pendingContributorAction.value = PendingContributorAction(
                book = book,
                sectionId = section.id,
                paragraphId = null,
                targetLanguage = targetLang,
                forceRetranslate = forceRetranslate
            )
            return
        }

        viewModelScope.launch {
            _isTranslatingSection.value = true
            _errorBannerMessage.value = null
            val outcome = repository.translateSectionWithSharedRepoAndUserAi(
                userId = user.id,
                userDisplayName = user.displayName,
                userAcceptedContributorLicense = true,
                book = book,
                chapter = chapter,
                section = section,
                targetLanguage = targetLang,
                forceRetranslate = forceRetranslate
            )
            _isTranslatingSection.value = false
            outcome.fold(
                onSuccess = { res ->
                    _continuousScrollNotice.value = res.noticeMessage
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message ?: "Section translation failed."
                }
            )
        }
    }

    fun retranslateParagraph(paragraphId: Long) {
        // Trigger section retranslation or single paragraph update
        translateCurrentSection(forceRetranslate = true)
    }

    fun confirmContributorAgreementAndProceed() {
        val pending = _pendingContributorAction.value ?: return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.acceptContributorAgreementForBook(user.id, pending.book)
            _pendingContributorAction.value = null
            if (pending.sectionId != null) {
                val section = dao.getSectionById(pending.sectionId) ?: return@launch
                val chapter = dao.getChaptersForBook(pending.book.id).find { it.id == section.chapterId } ?: return@launch
                _isTranslatingSection.value = true
                val outcome = repository.translateSectionWithSharedRepoAndUserAi(
                    userId = user.id,
                    userDisplayName = user.displayName,
                    userAcceptedContributorLicense = true,
                    book = pending.book,
                    chapter = chapter,
                    section = section,
                    targetLanguage = pending.targetLanguage,
                    forceRetranslate = pending.forceRetranslate
                )
                _isTranslatingSection.value = false
                outcome.fold(
                    onSuccess = { res -> _continuousScrollNotice.value = res.noticeMessage },
                    onFailure = { err -> _errorBannerMessage.value = err.message }
                )
            }
        }
    }

    fun dismissContributorAgreementDialog() {
        _pendingContributorAction.value = null
    }

    fun selectPreferredTranslationVersion(paragraphId: Long, versionId: Long) {
        viewModelScope.launch {
            repository.markPreferredTranslationVersion(
                paragraphId = paragraphId,
                targetLanguage = _selectedLanguage.value.name,
                versionId = versionId
            )
        }
    }

    fun submitHumanReviewOrEdit(
        translationId: Long,
        paragraphId: Long,
        editedTranslationText: String,
        editedNotes: String,
        newReviewStatus: String
    ) {
        val book = uiState.value.activeBook ?: return
        val user = _currentUser.value
        viewModelScope.launch {
            val paragraph = dao.getParagraphById(paragraphId) ?: return@launch
            val existing = dao.getTranslationVersionsForParagraph(paragraphId, _selectedLanguage.value.name)
                .find { it.id == translationId }

            if (existing != null && editedTranslationText.trim() == existing.translatedText.trim()) {
                repository.promoteTranslationReviewStatus(
                    translationId = translationId,
                    newStatus = newReviewStatus,
                    reviewerName = user?.displayName ?: "Editorial Reviewer"
                )
            } else {
                val created = repository.submitHumanReviewedTranslationVersion(
                    userId = user?.id ?: 0L,
                    reviewerName = user?.displayName ?: "Human Editorial Reviewer",
                    book = book,
                    paragraph = paragraph,
                    targetLanguage = _selectedLanguage.value.name,
                    correctedTranslationText = editedTranslationText,
                    notes = editedNotes
                )
                if (newReviewStatus != created.reviewStatus) {
                    repository.promoteTranslationReviewStatus(
                        translationId = created.id,
                        newStatus = newReviewStatus,
                        reviewerName = user?.displayName ?: "Editorial Reviewer"
                    )
                }
            }
            _continuousScrollNotice.value =
                "✓ Saved canonical human review ($newReviewStatus) to Shared Translation Repository."
        }
    }

    fun reportTranslationIssue(
        translationId: Long,
        paragraphId: Long,
        category: String,
        description: String,
        suggestedCorrection: String
    ) {
        val book = uiState.value.activeBook ?: return
        val user = _currentUser.value
        viewModelScope.launch {
            repository.reportTranslationIssue(
                bookId = book.id,
                paragraphId = paragraphId,
                translationId = translationId,
                targetLanguage = _selectedLanguage.value.name,
                userId = user?.id ?: 0L,
                reporterName = user?.displayName ?: "Reader",
                category = category,
                description = description,
                suggestedCorrection = suggestedCorrection
            )
            _continuousScrollNotice.value = "✓ Reported translation issue for editorial review."
        }
    }

    fun resolveReportedIssue(reportId: Long, status: String = "RESOLVED_NEW_VERSION") {
        viewModelScope.launch {
            repository.resolveReportedIssueInCms(reportId, status)
        }
    }

    fun correctParagraphOcrText(
        paragraphId: Long,
        correctedText: String,
        reasonNote: String
    ) {
        val user = _currentUser.value
        viewModelScope.launch {
            val result = repository.submitOcrCorrection(
                paragraphId = paragraphId,
                correctedText = correctedText,
                userId = user?.id ?: 0L,
                userName = user?.displayName ?: "Scholarly Editor",
                reason = reasonNote
            )
            result.fold(
                onSuccess = { updated ->
                    _continuousScrollNotice.value =
                        "✓ Updated source text to OCR v${updated.ocrVersion} (raw OCR v1 preserved)."
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message
                }
            )
        }
    }

    // =====================================================================
    // 5. PDF INGESTION, OFFLINE PACKAGES, BOOKMARKS, SEARCH & ASK THIS BOOK
    // =====================================================================

    fun uploadAndIngestPdf(uri: Uri) {
        val userId = _currentUser.value?.id ?: 0L

        viewModelScope.launch {
            _isIngestingPdf.value = true
            _errorBannerMessage.value = null
            _ingestionStageMessage.value =
                "Extracting document peer-to-peer (PDF / EPUB / TXT / MD / HTML) • Original file will be purged immediately…"

            val result = repository.ingestPdfIntoStructuredBookForUser(
                userId = userId,
                uri = uri,
                preferredTargetLanguage = _selectedLanguage.value.name,
                onProgress = { stage, processed, total ->
                    _ingestionStageMessage.value = stage
                    _ingestionProcessedPages.value = processed
                    _ingestionTotalPages.value = total
                }
            )

            _isIngestingPdf.value = false
            result.fold(
                onSuccess = { book ->
                    _continuousScrollNotice.value =
                        "✓ Extracted '${book.title}' (${book.totalChapters} Ch, ${book.totalParagraphs} Para). Original file purged (0B hosted); VidyaKosha owns OCR & translation derivatives."
                    selectBookInternal(book.id, navigateToReader = true)
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message ?: "Failed to extract document."
                }
            )
        }
    }

    fun extractSampleEpubPeerToPeer() {
        val userId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            _isIngestingPdf.value = true
            _errorBannerMessage.value = null
            _ingestionStageMessage.value =
                "Extracting Kathopanishad (.epub) peer-to-peer • Purging original .epub binary…"

            val result = repository.ingestSampleEpubForP2PCommons(
                userId = userId,
                preferredTargetLanguage = _selectedLanguage.value.name,
                onProgress = { stage, processed, total ->
                    _ingestionStageMessage.value = stage
                    _ingestionProcessedPages.value = processed
                    _ingestionTotalPages.value = total
                }
            )

            _isIngestingPdf.value = false
            result.fold(
                onSuccess = { book ->
                    _continuousScrollNotice.value =
                        "✓ Extracted EPUB '${book.title}' peer-to-peer. Original .epub binary purged (0B hosted); structured OCR & translations owned by VidyaKosha."
                    selectBookInternal(book.id, navigateToReader = true)
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message ?: "Failed to extract sample EPUB."
                }
            )
        }
    }

    fun downloadOfflinePackageForActiveBook() {
        val book = uiState.value.activeBook ?: return
        val user = _currentUser.value
        if (user == null) {
            openAuthPortal(signUpMode = false)
            return
        }
        viewModelScope.launch {
            val result = repository.downloadBookOfflinePackage(
                userId = user.id,
                book = book,
                targetLanguage = _selectedLanguage.value.name
            )
            result.fold(
                onSuccess = { pkg ->
                    _continuousScrollNotice.value =
                        "✓ Offline package synced (${pkg.paragraphsCount} paragraphs, ${pkg.includedLanguagesCsv})."
                },
                onFailure = { err ->
                    _errorBannerMessage.value = err.message
                }
            )
        }
    }

    fun toggleBookmark(paragraph: ParagraphEntity) {
        val user = _currentUser.value
        if (user == null) {
            openAuthPortal(signUpMode = false)
            return
        }
        val book = uiState.value.activeBook ?: return
        val chapter = uiState.value.activeChapter
        val section = uiState.value.activeSection
        val activeTrans = uiState.value.sectionParagraphs
            .find { it.paragraph.id == paragraph.id }
            ?.activeTranslation?.translatedText.orEmpty()

        viewModelScope.launch {
            repository.toggleUserBookmark(
                userId = user.id,
                book = book,
                chapter = chapter,
                section = section,
                paragraph = paragraph,
                translatedSnippet = activeTrans,
                existingBookmarks = uiState.value.bookmarksForActiveBook
            )
        }
    }

    fun updateSearchQuery(query: String, acrossAllBooks: Boolean = _searchAcrossAllBooks.value) {
        _searchQuery.value = query
        _searchAcrossAllBooks.value = acrossAllBooks
        if (query.trim().length < 2) {
            _crossBookSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            val userId = _currentUser.value?.id ?: 0L
            val allHits = repository.searchAcrossAllEntitledBooks(
                userId = userId,
                query = query,
                preferredLanguage = _selectedLanguage.value.name
            )
            val activeBookId = _activeBookId.value
            _crossBookSearchResults.value = if (acrossAllBooks || activeBookId == null) {
                allHits
            } else {
                allHits.filter { it.book.id == activeBookId }
            }
            _isSearching.value = false
        }
    }

    fun toggleSearchScopeAcrossAllBooks() {
        val nextScope = !_searchAcrossAllBooks.value
        updateSearchQuery(_searchQuery.value, acrossAllBooks = nextScope)
    }

    fun jumpToBookAndSectionFromSearch(bookId: Long, sectionId: Long) {
        viewModelScope.launch {
            if (_activeBookId.value != bookId) {
                selectBookInternal(bookId, navigateToReader = true)
            }
            selectSection(sectionId, triggeredBySwipe = false)
            _workspaceTab.value = ReaderWorkspaceTab.READER
        }
    }

    fun askBookQuestion(question: String) {
        val book = uiState.value.activeBook ?: return
        if (question.isBlank() || _isAskingBook.value) return

        viewModelScope.launch {
            _isAskingBook.value = true
            _errorBannerMessage.value = null

            val result = repository.askThisBookWithCitations(
                userId = _currentUser.value?.id ?: 0L,
                book = book,
                targetLanguage = _selectedLanguage.value.name,
                userQuestion = question.trim()
            )

            _isAskingBook.value = false
            result.onFailure { err ->
                _errorBannerMessage.value = err.message ?: "Failed to answer question."
            }
        }
    }

    fun clearChatHistory() {
        val book = uiState.value.activeBook ?: return
        viewModelScope.launch {
            repository.clearUserBookChat(_currentUser.value?.id ?: 0L, book)
        }
    }

    fun clearErrorBanner() {
        _errorBannerMessage.value = null
    }

    fun clearAdminActionBanner() {
        _adminActionBanner.value = null
    }

    // =====================================================================
    // 5B. SUPERADMIN BACKEND ADMINISTRATION HANDLERS (ALL REQUIRE SUPERADMIN RBAC)
    // =====================================================================

    fun adminSaveCommercialPlan(
        existingPlanId: Long?,
        planCode: String,
        name: String,
        description: String,
        isEnabled: Boolean,
        monthlyPrice: Double,
        yearlyPrice: Double,
        currencyCode: String,
        trialDays: Int,
        displayOrder: Int,
        isFeatured: Boolean,
        featuresCsv: String,
        allowedBookAccessPoliciesCsv: String,
        dailyTranslationLimit: Int,
        monthlyManagedAiTokensLimit: Int,
        offlineAccessEnabled: Boolean,
        advancedSearchAndScholarlyEnabled: Boolean,
        ttsAudioDailyMinutesLimit: Int,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.saveCommercialPlan(
                adminUserId = adminId,
                existingPlanId = existingPlanId,
                planCode = planCode,
                name = name,
                description = description,
                isEnabled = isEnabled,
                monthlyPriceAmount = monthlyPrice,
                yearlyPriceAmount = yearlyPrice,
                currencyCode = currencyCode,
                trialDays = trialDays,
                displayOrder = displayOrder,
                isFeatured = isFeatured,
                featuresCsv = featuresCsv,
                allowedBookAccessPoliciesCsv = allowedBookAccessPoliciesCsv,
                dailyTranslationLimit = dailyTranslationLimit,
                monthlyManagedAiTokensLimit = monthlyManagedAiTokensLimit,
                offlineAccessEnabled = offlineAccessEnabled,
                advancedSearchAndScholarlyEnabled = advancedSearchAndScholarlyEnabled,
                ttsAudioDailyMinutesLimit = ttsAudioDailyMinutesLimit,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { saved ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Plan '${saved.name}' (${saved.formatPriceSummary()}) saved & logged to immutable audit trail."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message ?: "Failed to save commercial plan."
                }
            )
        }
    }

    fun adminTogglePlanEnabled(planId: Long, enabled: Boolean, reason: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.toggleCommercialPlanEnabled(adminId, planId, enabled, reason)
            result.fold(
                onSuccess = { plan ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Plan '${plan.planCode}' ${if (enabled) "enabled" else "disabled"}."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminGrantUserEntitlement(
        targetUserId: Long,
        entitlementKind: String,
        targetRef: String,
        displayTitle: String,
        durationDays: Int,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.grantUserEntitlement(
                adminUserId = adminId,
                targetUserId = targetUserId,
                entitlementKind = entitlementKind,
                targetRef = targetRef,
                displayTitle = displayTitle,
                durationDays = durationDays,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { ent ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Granted ${ent.entitlementKind} (${ent.targetRef}) to User #${ent.userId}."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminRevokeUserEntitlement(entitlementId: Long, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.revokeUserEntitlement(adminId, entitlementId, reasonOrNote)
            result.fold(
                onSuccess = { ent ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Revoked entitlement #${ent.id} (${ent.targetRef})."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminExtendUserEntitlement(entitlementId: Long, additionalDays: Int, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.extendUserEntitlement(adminId, entitlementId, additionalDays, reasonOrNote)
            result.fold(
                onSuccess = { ent ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Extended entitlement #${ent.id} by +$additionalDays days."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminCreateBook(
        title: String,
        subtitle: String,
        authorOrSource: String,
        contributors: String,
        publisherName: String,
        description: String,
        originalLanguage: String,
        category: String,
        genresCsv: String,
        tagsCsv: String,
        workId: String,
        editionLabel: String,
        isbnOrIdentifier: String,
        publicationYear: String,
        publicationInfo: String,
        coverBadgeColorHex: String,
        coverImageUrl: String,
        accessPolicy: String,
        requiredPlanCode: String,
        rightsCategory: String,
        publicationStatus: String,
        priceDisplay: String,
        initialSampleParagraphText: String,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.createBookByAdmin(
                adminUserId = adminId,
                title = title,
                subtitle = subtitle,
                authorOrSource = authorOrSource,
                contributors = contributors,
                publisherName = publisherName,
                description = description,
                originalLanguage = originalLanguage,
                category = category,
                genresCsv = genresCsv,
                tagsCsv = tagsCsv,
                workId = workId,
                editionLabel = editionLabel,
                isbnOrIdentifier = isbnOrIdentifier,
                publicationYear = publicationYear,
                publicationInfo = publicationInfo,
                coverBadgeColorHex = coverBadgeColorHex,
                coverImageUrl = coverImageUrl,
                accessPolicy = accessPolicy,
                requiredPlanCode = requiredPlanCode,
                rightsCategory = rightsCategory,
                publicationStatus = publicationStatus,
                priceDisplay = priceDisplay,
                initialSampleParagraphText = initialSampleParagraphText,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { book ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Created book '${book.title}' (Work: ${book.workId}, Status: ${book.publicationStatus})."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminUpdateBookMetadataAndAccess(
        bookId: Long,
        title: String,
        subtitle: String,
        authorOrSource: String,
        contributors: String,
        publisherName: String,
        description: String,
        originalLanguage: String,
        category: String,
        genresCsv: String,
        tagsCsv: String,
        workId: String,
        editionLabel: String,
        isbnOrIdentifier: String,
        publicationYear: String,
        publicationInfo: String,
        coverBadgeColorHex: String,
        coverImageUrl: String,
        customMetadataJson: String,
        searchIndexStatus: String,
        publicationStatus: String,
        accessPolicy: String,
        requiredPlanCode: String,
        rightsCategory: String,
        priceDisplay: String,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.updateBookMetadataAndAccess(
                adminUserId = adminId,
                bookId = bookId,
                title = title,
                subtitle = subtitle,
                authorOrSource = authorOrSource,
                contributors = contributors,
                publisherName = publisherName,
                description = description,
                originalLanguage = originalLanguage,
                category = category,
                genresCsv = genresCsv,
                tagsCsv = tagsCsv,
                workId = workId,
                editionLabel = editionLabel,
                isbnOrIdentifier = isbnOrIdentifier,
                publicationYear = publicationYear,
                publicationInfo = publicationInfo,
                coverBadgeColorHex = coverBadgeColorHex,
                coverImageUrl = coverImageUrl,
                customMetadataJson = customMetadataJson,
                searchIndexStatus = searchIndexStatus,
                publicationStatus = publicationStatus,
                accessPolicy = accessPolicy,
                requiredPlanCode = requiredPlanCode,
                rightsCategory = rightsCategory,
                priceDisplay = priceDisplay,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { book ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Updated '${book.title}' (${book.publicationStatus} • ${book.accessPolicy}) while preserving all OCR & translations."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminSetBookPublicationStatus(bookId: Long, newPublicationStatus: String, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.setBookPublicationStatus(adminId, bookId, newPublicationStatus, reasonOrNote)
            result.fold(
                onSuccess = { book ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Book '${book.title}' status set to ${book.publicationStatus} (OCR & translations preserved)."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminApproveOrRejectTranslation(translationId: Long, newReviewStatus: String, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.approveOrRejectTranslation(adminId, translationId, newReviewStatus, reasonOrNote)
            result.fold(
                onSuccess = { tr ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Translation #${tr.id} (${tr.targetLanguage} v${tr.versionNumber}) status set to ${tr.reviewStatus}."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminSetPreferredTranslation(translationId: Long, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.setPreferredTranslationByAdmin(adminId, translationId, reasonOrNote)
            result.fold(
                onSuccess = { tr ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Marked translation #${tr.id} (v${tr.versionNumber}) as preferred (prior versions preserved)."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminUpdateTranslationAccessPolicy(translationId: Long, newAccessPolicy: String, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.updateTranslationAccessPolicyByAdmin(adminId, translationId, newAccessPolicy, reasonOrNote)
            result.fold(
                onSuccess = { tr ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Updated translation #${tr.id} access policy to ${tr.accessPolicy}."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminTriggerRetranslationVersion(
        translationId: Long,
        newTranslatedText: String,
        newNotes: String,
        modelUsed: String,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.triggerAdminRetranslationVersion(
                adminUserId = adminId,
                translationId = translationId,
                newTranslatedText = newTranslatedText,
                newNotes = newNotes,
                modelUsed = modelUsed,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { tr ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Created new translation version v${tr.versionNumber} (#${tr.id}) preserving historical versions."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminUpsertPlatformMetadata(
        metadataId: Long?,
        metadataType: String,
        keyCode: String,
        displayLabel: String,
        description: String,
        sortOrder: Int,
        isActive: Boolean,
        extraAttributesJson: String,
        reasonOrNote: String
    ) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.upsertPlatformMetadata(
                adminUserId = adminId,
                metadataId = metadataId,
                metadataType = metadataType,
                keyCode = keyCode,
                displayLabel = displayLabel,
                description = description,
                sortOrder = sortOrder,
                isActive = isActive,
                extraAttributesJson = extraAttributesJson,
                reasonOrNote = reasonOrNote
            )
            result.fold(
                onSuccess = { meta ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Saved metadata [${meta.metadataType}] ${meta.keyCode} (${meta.displayLabel})."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminSetUserAccountStatus(targetUserId: Long, newStatus: String, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.setUserAccountStatus(adminId, targetUserId, newStatus, reasonOrNote)
            result.fold(
                onSuccess = { updatedUser ->
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "User '${updatedUser.email}' account status set to ${updatedUser.accountStatus}."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    fun adminUpdateUserRole(targetUserId: Long, newRole: String, reasonOrNote: String) {
        val adminId = _currentUser.value?.id ?: 0L
        viewModelScope.launch {
            val result = repository.superadminApi.updateUserRole(adminId, targetUserId, newRole, reasonOrNote)
            result.fold(
                onSuccess = { updatedUser ->
                    if (_currentUser.value?.id == updatedUser.id) {
                        _currentUser.value = updatedUser
                    }
                    _adminActionIsError.value = false
                    _adminActionBanner.value = "Updated role for '${updatedUser.email}' to '${updatedUser.role}'."
                },
                onFailure = { err ->
                    _adminActionIsError.value = true
                    _adminActionBanner.value = err.message
                }
            )
        }
    }

    // =====================================================================
    // 6. NATIVE ANDROID TEXT-TO-SPEECH (TTS) FOR ALL INDIAN LANGUAGES
    // =====================================================================

    private fun initTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            if (_isPlayingFullSectionAudio.value) {
                                playNextParagraphInQueue()
                            } else {
                                _speakingParagraphId.value = null
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _speakingParagraphId.value = null
                            _isPlayingFullSectionAudio.value = false
                        }
                    })
                }
            }
        } catch (_: Exception) {
            isTtsInitialized = false
        }
    }

    fun speakParagraph(paragraphId: Long, text: String, languageName: String, isOriginal: Boolean) {
        if (!isTtsInitialized || text.isBlank()) return
        if (_speakingParagraphId.value == paragraphId && _isSpeakingOriginal.value == isOriginal) {
            stopSpeaking()
            return
        }

        _isPlayingFullSectionAudio.value = false
        _speakingParagraphId.value = paragraphId
        _isSpeakingOriginal.value = isOriginal

        val bookId = _activeBookId.value
        if (bookId != null) {
            viewModelScope.launch {
                repository.audioApi.recordListenEvent(
                    bookId = bookId,
                    languageName = languageName,
                    userId = _currentUser.value?.id ?: 0L
                )
            }
        }

        speakInternal(paragraphId, text, languageName)
    }

    fun toggleFullSectionAudio(speakTranslated: Boolean = true) {
        if (_isPlayingFullSectionAudio.value) {
            stopSpeaking()
            return
        }
        val items = uiState.value.sectionParagraphs
        if (items.isEmpty() || !isTtsInitialized) return

        fullSectionQueue = items
        fullSectionQueueIndex = 0
        _isPlayingFullSectionAudio.value = true
        _isSpeakingOriginal.value = !speakTranslated
        playCurrentParagraphInQueue()
    }

    private fun playCurrentParagraphInQueue() {
        if (fullSectionQueueIndex >= fullSectionQueue.size) {
            stopSpeaking()
            return
        }
        val item = fullSectionQueue[fullSectionQueueIndex]
        val useOriginal = _isSpeakingOriginal.value || item.activeTranslation == null
        val textToSpeak = if (useOriginal) {
            item.paragraph.originalText
        } else {
            item.activeTranslation?.translatedText ?: item.paragraph.originalText
        }
        val langName = if (useOriginal) {
            uiState.value.activeBook?.originalLanguage ?: "Sanskrit"
        } else {
            _selectedLanguage.value.name
        }
        _speakingParagraphId.value = item.paragraph.id
        speakInternal(item.paragraph.id, textToSpeak, langName)
    }

    private fun playNextParagraphInQueue() {
        fullSectionQueueIndex += 1
        if (fullSectionQueueIndex < fullSectionQueue.size) {
            playCurrentParagraphInQueue()
        } else {
            stopSpeaking()
        }
    }

    private fun speakInternal(paragraphId: Long, text: String, languageName: String) {
        val langDesc = LanguageRegistry.findByNameOrCode(languageName)
        val locale = Locale.forLanguageTag(langDesc.ttsLocaleTag)
        textToSpeech?.language = locale
        val params = Bundle()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "polyglot_para_$paragraphId")
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
        _speakingParagraphId.value = null
        _isPlayingFullSectionAudio.value = false
    }

    override fun onCleared() {
        super.onCleared()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}
