package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PolyglotUiState
import com.example.ui.PolyglotViewModel
import com.example.ui.ReaderWorkspaceTab
import com.example.ui.TopLevelDestination
import com.example.ui.components.AskThisBookResearchPane
import com.example.ui.components.AuthPortalScreen
import com.example.ui.components.BilingualBookReaderPane
import com.example.ui.components.ContributorLicenseAgreementDialog
import com.example.ui.components.HowItWorksScreen
import com.example.ui.components.PublicBookDirectoryScreen
import com.example.ui.components.SearchAndBookmarksPane
import com.example.ui.components.SharedCorpusAndGovernanceHubScreen
import com.example.ui.components.SubscriptionTiersAndEntitlementsDialog
import com.example.ui.components.SuperadminDashboardScreen
import com.example.ui.components.UserAiSettingsScreen
import com.example.ui.components.UserHomeDashboardScreen
import com.example.data.local.UserRole
import com.example.ui.theme.LoraSerif
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val polyglotViewModel: PolyglotViewModel = viewModel()
                val uiState by polyglotViewModel.uiState.collectAsStateWithLifecycle()

                PolyglotPlatformApp(
                    uiState = uiState,
                    viewModel = polyglotViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolyglotPlatformApp(
    uiState: PolyglotUiState,
    viewModel: PolyglotViewModel
) {
    val multiFormatMimeTypes = arrayOf(
        "application/pdf",
        "application/epub+zip",
        "text/plain",
        "text/markdown",
        "text/html",
        "application/xhtml+xml",
        "application/json",
        "*/*"
    )
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadAndIngestPdf(uri)
        }
    }

    val isLoggedIn = uiState.currentUser != null
    val isSuperadmin = uiState.currentUser?.role == UserRole.SUPERADMIN
    var isFocusModeEnabled by rememberSaveable { mutableStateOf(false) }

    val inFocusedReading = isFocusModeEnabled &&
        uiState.topDestination == TopLevelDestination.START_READING &&
        uiState.workspaceTab == ReaderWorkspaceTab.READER

    // Back button exits Focus Mode first, then returns to home/landing
    BackHandler(
        enabled = inFocusedReading ||
            uiState.topDestination != if (isLoggedIn) TopLevelDestination.MY_HOME else TopLevelDestination.HOW_IT_WORKS
    ) {
        if (inFocusedReading) {
            isFocusModeEnabled = false
        } else {
            viewModel.navigateToDestination(
                if (isLoggedIn) TopLevelDestination.MY_HOME else TopLevelDestination.HOW_IT_WORKS
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                AnimatedVisibility(
                    visible = !inFocusedReading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (uiState.topDestination == TopLevelDestination.START_READING && uiState.activeBook != null) {
                                            uiState.activeBook.title
                                        } else {
                                            "VidyaKosha"
                                        },
                                        fontFamily = LoraSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 19.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                if (uiState.topDestination == TopLevelDestination.START_READING &&
                                    uiState.workspaceTab == ReaderWorkspaceTab.READER
                                ) {
                                    TextButton(
                                        onClick = { isFocusModeEnabled = true },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("top_bar_enter_focus_mode_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Fullscreen,
                                            contentDescription = "Focus Mode",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Focus", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                IconButton(
                                    onClick = { pdfPickerLauncher.launch(multiFormatMimeTypes) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_bar_upload_pdf_button")
                                ) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = "Extract PDF / EPUB / Text (Zero-Host P2P)",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.openPlansDialog() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_bar_plans_button")
                                ) {
                                    Icon(
                                        Icons.Default.WorkspacePremium,
                                        contentDescription = "Plans & Access Rights",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isSuperadmin) {
                                    IconButton(
                                        onClick = { viewModel.navigateToDestination(TopLevelDestination.SUPERADMIN_DASHBOARD) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("top_bar_superadmin_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = "Superadmin Backend Console",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (isLoggedIn) {
                                    IconButton(
                                        onClick = { viewModel.navigateToDestination(TopLevelDestination.AI_SETTINGS) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("top_bar_ai_settings_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Key,
                                            contentDescription = "AI Credentials & Account",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.openAuthPortal(signUpMode = false) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .minimumInteractiveComponentSize()
                                            .testTag("top_bar_login_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sign In", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !inFocusedReading && !isExpandedScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp
                        ) {
                            if (isLoggedIn) {
                                NavigationBarItem(
                                    selected = uiState.topDestination == TopLevelDestination.MY_HOME,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.MY_HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    modifier = Modifier.testTag("bottom_nav_my_home")
                                )
                            } else {
                                NavigationBarItem(
                                    selected = uiState.topDestination == TopLevelDestination.HOW_IT_WORKS,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.HOW_IT_WORKS) },
                                    icon = { Icon(Icons.Default.Info, contentDescription = "Overview") },
                                    label = { Text("Overview") },
                                    modifier = Modifier.testTag("bottom_nav_how_it_works")
                                )
                            }

                            NavigationBarItem(
                                selected = uiState.topDestination == TopLevelDestination.DIRECTORY,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.DIRECTORY) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Catalogue") },
                                label = { Text("Catalogue") },
                                modifier = Modifier.testTag("bottom_nav_directory")
                            )

                            NavigationBarItem(
                                selected = uiState.topDestination == TopLevelDestination.START_READING,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.START_READING) },
                                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read") },
                                label = { Text("Reader") },
                                modifier = Modifier.testTag("bottom_nav_reader")
                            )

                            NavigationBarItem(
                                selected = uiState.topDestination == TopLevelDestination.SHARED_CORPUS_HUB,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.SHARED_CORPUS_HUB) },
                                icon = { Icon(Icons.Default.Hub, contentDescription = "Corpus") },
                                label = { Text("Corpus") },
                                modifier = Modifier.testTag("bottom_nav_corpus")
                            )

                            NavigationBarItem(
                                selected = uiState.topDestination == TopLevelDestination.AI_SETTINGS ||
                                    uiState.topDestination == TopLevelDestination.AUTH_PORTAL,
                                onClick = {
                                    if (isLoggedIn) {
                                        viewModel.navigateToDestination(TopLevelDestination.AI_SETTINGS)
                                    } else {
                                        viewModel.openAuthPortal(signUpMode = false)
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (isLoggedIn) Icons.Default.Key else Icons.Default.AccountCircle,
                                        contentDescription = "Account"
                                    )
                                },
                                label = { Text(if (isLoggedIn) "Account" else "Sign In") },
                                modifier = Modifier.testTag("bottom_nav_account")
                            )

                            if (isSuperadmin) {
                                NavigationBarItem(
                                    selected = uiState.topDestination == TopLevelDestination.SUPERADMIN_DASHBOARD,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.SUPERADMIN_DASHBOARD) },
                                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Superadmin") },
                                    label = { Text("Admin") },
                                    modifier = Modifier.testTag("bottom_nav_superadmin")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Canonical Navigation Rail for Tablet / Desktop (Hidden during Focus Mode)
                AnimatedVisibility(visible = isExpandedScreen && !inFocusedReading) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.background
                        ) {
                            Spacer(modifier = Modifier.size(12.dp))
                            if (isLoggedIn) {
                                NavigationRailItem(
                                    selected = uiState.topDestination == TopLevelDestination.MY_HOME,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.MY_HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    modifier = Modifier.testTag("nav_chip_my_home")
                                )
                            } else {
                                NavigationRailItem(
                                    selected = uiState.topDestination == TopLevelDestination.HOW_IT_WORKS,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.HOW_IT_WORKS) },
                                    icon = { Icon(Icons.Default.Info, contentDescription = "Overview") },
                                    label = { Text("Overview") },
                                    modifier = Modifier.testTag("nav_chip_how_it_works")
                                )
                            }

                            NavigationRailItem(
                                selected = uiState.topDestination == TopLevelDestination.DIRECTORY,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.DIRECTORY) },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Catalogue") },
                                label = { Text("Catalogue") },
                                modifier = Modifier.testTag("nav_chip_directory")
                            )

                            NavigationRailItem(
                                selected = uiState.topDestination == TopLevelDestination.START_READING,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.START_READING) },
                                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Reader") },
                                label = { Text("Reader") },
                                modifier = Modifier.testTag("nav_chip_start_reading")
                            )

                            NavigationRailItem(
                                selected = uiState.topDestination == TopLevelDestination.SHARED_CORPUS_HUB,
                                onClick = { viewModel.navigateToDestination(TopLevelDestination.SHARED_CORPUS_HUB) },
                                icon = { Icon(Icons.Default.Hub, contentDescription = "Corpus") },
                                label = { Text("Corpus") },
                                modifier = Modifier.testTag("nav_chip_shared_corpus")
                            )

                            NavigationRailItem(
                                selected = uiState.topDestination == TopLevelDestination.AI_SETTINGS ||
                                    uiState.topDestination == TopLevelDestination.AUTH_PORTAL,
                                onClick = {
                                    if (isLoggedIn) {
                                        viewModel.navigateToDestination(TopLevelDestination.AI_SETTINGS)
                                    } else {
                                        viewModel.openAuthPortal(signUpMode = false)
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (isLoggedIn) Icons.Default.Key else Icons.Default.AccountCircle,
                                        contentDescription = "Account"
                                    )
                                },
                                label = { Text(if (isLoggedIn) "Account" else "Sign In") },
                                modifier = Modifier.testTag("nav_chip_ai_settings")
                            )

                            if (isSuperadmin) {
                                NavigationRailItem(
                                    selected = uiState.topDestination == TopLevelDestination.SUPERADMIN_DASHBOARD,
                                    onClick = { viewModel.navigateToDestination(TopLevelDestination.SUPERADMIN_DASHBOARD) },
                                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Superadmin") },
                                    label = { Text("Admin") },
                                    modifier = Modifier.testTag("nav_chip_superadmin")
                                )
                            }
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // Dismissible Error Banner
                    if (uiState.errorBannerMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorBannerMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearErrorBanner() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Destination Router
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState.topDestination) {
                            TopLevelDestination.HOW_IT_WORKS -> {
                                HowItWorksScreen(
                                    onLoginClick = { viewModel.openAuthPortal(signUpMode = false) },
                                    onSignUpClick = { viewModel.openAuthPortal(signUpMode = true) },
                                    onTrySampleBookClick = { viewModel.openPublicSampleBook() },
                                    onBrowseDirectoryClick = { viewModel.navigateToDestination(TopLevelDestination.DIRECTORY) },
                                    onExtractFileClick = { pdfPickerLauncher.launch(multiFormatMimeTypes) },
                                    onExtractSampleEpubClick = { viewModel.extractSampleEpubPeerToPeer() }
                                )
                            }

                            TopLevelDestination.DIRECTORY -> {
                                PublicBookDirectoryScreen(
                                    books = uiState.directoryBooks,
                                    userAccessList = uiState.userBookAccessList,
                                    isLoggedIn = isLoggedIn,
                                    searchQuery = uiState.directorySearchQuery,
                                    activeFilter = uiState.directoryActiveFilter,
                                    onSearchQueryChange = { viewModel.updateDirectorySearchQuery(it) },
                                    onFilterChange = { viewModel.updateDirectoryFilter(it) },
                                    onOpenBookForReading = { book -> viewModel.openBookForReading(book) },
                                    onUnlockOrPurchaseBook = { book -> viewModel.unlockOrPurchaseBook(book) }
                                )
                            }

                            TopLevelDestination.MY_HOME -> {
                                val user = uiState.currentUser
                                if (user == null) {
                                    AuthPortalScreen(
                                        isSignUpMode = uiState.isSignUpMode,
                                        isLoading = uiState.isAuthLoading,
                                        errorMessage = uiState.authErrorMessage,
                                        defaultLanguage = uiState.selectedLanguage,
                                        onToggleMode = { viewModel.toggleAuthMode(it) },
                                        onLoginSubmit = { email, pass -> viewModel.login(email, pass) },
                                        onSignUpSubmit = { name, email, pass, lang -> viewModel.signUp(name, email, pass, lang) },
                                        onLoginAsSuperadminClick = { viewModel.authenticateAsSuperadminAndOpenDashboard() },
                                        onRequestPasswordReset = { email -> viewModel.requestPasswordReset(email) },
                                        onConfirmPasswordReset = { email, code, newPwd -> viewModel.confirmPasswordReset(email, code, newPwd) }
                                    )
                                } else {
                                    UserHomeDashboardScreen(
                                        user = user,
                                        userAccessibleBooks = uiState.userAccessibleBooks.ifEmpty { uiState.directoryBooks },
                                        readingProgressList = uiState.readingProgressList,
                                        bookmarks = uiState.allUserBookmarks,
                                        readingHistory = uiState.readingHistory,
                                        aiConfigs = uiState.userAiConfigs,
                                        isIngestingPdf = uiState.isIngestingPdf,
                                        ingestionStageMessage = uiState.ingestionStageMessage,
                                        ingestionProcessedPages = uiState.ingestionProcessedPages,
                                        ingestionTotalPages = uiState.ingestionTotalPages,
                                        onContinueReadingClick = { book ->
                                            if (book != null) {
                                                viewModel.openBookForReading(book)
                                            } else {
                                                viewModel.openPublicSampleBook()
                                            }
                                        },
                                        onOpenBookClick = { book -> viewModel.openBookForReading(book) },
                                        onUploadNewPdfClick = { pdfPickerLauncher.launch(multiFormatMimeTypes) },
                                        onExtractSampleEpubClick = { viewModel.extractSampleEpubPeerToPeer() },
                                        onOpenAiSettingsClick = { viewModel.navigateToDestination(TopLevelDestination.AI_SETTINGS) },
                                        onBrowsePublicDirectoryClick = { viewModel.navigateToDestination(TopLevelDestination.DIRECTORY) }
                                    )
                                }
                            }

                            TopLevelDestination.START_READING -> {
                                val activeBook = uiState.activeBook ?: uiState.directoryBooks.firstOrNull()
                                if (activeBook != null) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Understated Workspace Segmented Switcher (Hidden in Focus Mode)
                                        AnimatedVisibility(visible = !inFocusedReading) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.background,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    ReaderModePill(
                                                        label = "Read",
                                                        selected = uiState.workspaceTab == ReaderWorkspaceTab.READER,
                                                        onClick = { viewModel.setWorkspaceTab(ReaderWorkspaceTab.READER) },
                                                        modifier = Modifier.testTag("workspace_tab_reader")
                                                    )
                                                    ReaderModePill(
                                                        label = "Search & Bookmarks",
                                                        selected = uiState.workspaceTab == ReaderWorkspaceTab.SEARCH_AND_BOOKMARKS,
                                                        onClick = { viewModel.setWorkspaceTab(ReaderWorkspaceTab.SEARCH_AND_BOOKMARKS) },
                                                        modifier = Modifier.testTag("workspace_tab_search")
                                                    )
                                                    ReaderModePill(
                                                        label = "Ask This Book",
                                                        selected = uiState.workspaceTab == ReaderWorkspaceTab.ASK_BOOK,
                                                        onClick = { viewModel.setWorkspaceTab(ReaderWorkspaceTab.ASK_BOOK) },
                                                        modifier = Modifier.testTag("workspace_tab_ask_book")
                                                    )
                                                }
                                            }
                                        }

                                        when (uiState.workspaceTab) {
                                            ReaderWorkspaceTab.READER -> {
                                                BilingualBookReaderPane(
                                                    book = activeBook,
                                                    chapters = uiState.chapters,
                                                    sections = uiState.sections,
                                                    activeChapter = uiState.activeChapter,
                                                    activeSection = uiState.activeSection,
                                                    paragraphs = uiState.sectionParagraphs,
                                                    discoveryStatus = uiState.discoveryStatus,
                                                    cachedLanguages = uiState.cachedLanguagesForActiveBook,
                                                    ocrAuditsForBook = uiState.ocrAuditsForActiveBook,
                                                    selectedLanguage = uiState.selectedLanguage,
                                                    displayMode = uiState.readerDisplayMode,
                                                    fontScaleMultiplier = uiState.fontScaleMultiplier,
                                                    showTransliteration = uiState.showTransliteration,
                                                    showWordByWordBreakdown = uiState.showWordByWordBreakdown,
                                                    autoTranslateOnScroll = uiState.autoTranslateOnScroll,
                                                    continuousScrollNotice = uiState.continuousScrollNotice,
                                                    isTranslatingSection = uiState.isTranslatingSection,
                                                    speakingParagraphId = uiState.speakingParagraphId,
                                                    isSpeakingOriginal = uiState.isSpeakingOriginal,
                                                    isPlayingFullSectionAudio = uiState.isPlayingFullSectionAudio,
                                                    onSelectSection = { viewModel.selectSection(it) },
                                                    onAdvanceNextSectionSeamless = { viewModel.advanceToNextSectionSeamless() },
                                                    onPreviousSectionSeamless = { viewModel.goToPreviousSectionSeamless() },
                                                    onDismissScrollNotice = { viewModel.clearContinuousScrollNotice() },
                                                    onSelectLanguage = { viewModel.selectLanguage(it) },
                                                    onSelectDisplayMode = { viewModel.setReaderDisplayMode(it) },
                                                    onAdjustFontScale = { viewModel.adjustFontScale(it) },
                                                    onToggleTransliteration = { viewModel.toggleTransliteration() },
                                                    onToggleWordByWord = { viewModel.toggleWordByWordBreakdown() },
                                                    onToggleAutoTranslate = { viewModel.toggleAutoTranslateOnScroll() },
                                                    onTranslateSection = { force -> viewModel.translateCurrentSection(force) },
                                                    onSelectPreferredVersion = { paraId, verId ->
                                                        viewModel.selectPreferredTranslationVersion(paraId, verId)
                                                    },
                                                    onSubmitHumanReviewOrEdit = { transId, paraId, text, notes, status ->
                                                        viewModel.submitHumanReviewOrEdit(transId, paraId, text, notes, status)
                                                    },
                                                    onReportTranslationIssue = { transId, paraId, cat, desc, fix ->
                                                        viewModel.reportTranslationIssue(transId, paraId, cat, desc, fix)
                                                    },
                                                    onCorrectParagraphOcr = { paraId, text, reason ->
                                                        viewModel.correctParagraphOcrText(paraId, text, reason)
                                                    },
                                                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                                                    onSpeakParagraph = { paraId, text, lang, isOrig ->
                                                        viewModel.speakParagraph(paraId, text, lang, isOrig)
                                                    },
                                                    onToggleFullSectionAudio = { speakTrans ->
                                                        viewModel.toggleFullSectionAudio(speakTrans)
                                                    },
                                                    onDownloadOfflinePackage = { viewModel.downloadOfflinePackageForActiveBook() },
                                                    isFocusModeEnabled = isFocusModeEnabled,
                                                    onToggleFocusMode = { isFocusModeEnabled = !isFocusModeEnabled },
                                                    userTopLanguages = uiState.userTopLanguages,
                                                    onOpenLanguageSettings = {
                                                        isFocusModeEnabled = false
                                                        viewModel.navigateToDestination(TopLevelDestination.AI_SETTINGS)
                                                    }
                                                )
                                            }

                                            ReaderWorkspaceTab.SEARCH_AND_BOOKMARKS -> {
                                                SearchAndBookmarksPane(
                                                    activeBook = activeBook,
                                                    selectedLanguage = uiState.selectedLanguage,
                                                    searchQuery = uiState.searchQuery,
                                                    searchAcrossAllBooks = uiState.searchAcrossAllBooks,
                                                    searchResults = uiState.crossBookSearchResults,
                                                    isSearching = uiState.isSearching,
                                                    bookmarksForActiveBook = uiState.bookmarksForActiveBook,
                                                    onUpdateSearchQuery = { viewModel.updateSearchQuery(it) },
                                                    onToggleSearchScope = { viewModel.toggleSearchScopeAcrossAllBooks() },
                                                    onJumpToBookAndSection = { bookId, secId ->
                                                        viewModel.jumpToBookAndSectionFromSearch(bookId, secId)
                                                    }
                                                )
                                            }

                                            ReaderWorkspaceTab.ASK_BOOK -> {
                                                AskThisBookResearchPane(
                                                    book = activeBook,
                                                    selectedLanguage = uiState.selectedLanguage,
                                                    messages = uiState.chatMessages,
                                                    isAskingBook = uiState.isAskingBook,
                                                    onAskQuestion = { viewModel.askBookQuestion(it) },
                                                    onClearHistory = { viewModel.clearChatHistory() },
                                                    onOpenCitedSection = { bookId, secId ->
                                                        viewModel.jumpToBookAndSectionFromSearch(bookId, secId)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            TopLevelDestination.SHARED_CORPUS_HUB -> {
                                SharedCorpusAndGovernanceHubScreen(
                                    currentUser = uiState.currentUser,
                                    activeEntitlements = uiState.activeEntitlements,
                                    summary = uiState.corpusAnalyticsSummary,
                                    allBooks = uiState.allSystemBooks,
                                    canonicalTranslations = uiState.allCanonicalTranslations,
                                    translationMemoryEntries = uiState.translationMemoryEntries,
                                    issueReports = uiState.translationIssueReports,
                                    processingJobs = uiState.recentProcessingJobs,
                                    onResolveIssueReport = { reportId, status ->
                                        viewModel.resolveReportedIssue(reportId, status)
                                    },
                                    onOpenPlansDialog = { viewModel.openPlansDialog() },
                                    onOpenBookInReader = { book -> viewModel.openBookForReading(book) },
                                    onExtractAnyFileClick = { pdfPickerLauncher.launch(multiFormatMimeTypes) },
                                    onExtractSampleEpubClick = { viewModel.extractSampleEpubPeerToPeer() }
                                )
                            }

                            TopLevelDestination.AI_SETTINGS -> {
                                val user = uiState.currentUser
                                if (user == null) {
                                    AuthPortalScreen(
                                        isSignUpMode = uiState.isSignUpMode,
                                        isLoading = uiState.isAuthLoading,
                                        errorMessage = uiState.authErrorMessage,
                                        defaultLanguage = uiState.selectedLanguage,
                                        onToggleMode = { viewModel.toggleAuthMode(it) },
                                        onLoginSubmit = { email, pass -> viewModel.login(email, pass) },
                                        onSignUpSubmit = { name, email, pass, lang -> viewModel.signUp(name, email, pass, lang) },
                                        onLoginAsSuperadminClick = { viewModel.authenticateAsSuperadminAndOpenDashboard() },
                                        onRequestPasswordReset = { email -> viewModel.requestPasswordReset(email) },
                                        onConfirmPasswordReset = { email, code, newPwd -> viewModel.confirmPasswordReset(email, code, newPwd) }
                                    )
                                } else {
                                    UserAiSettingsScreen(
                                        currentUser = user,
                                        savedConfigs = uiState.userAiConfigs,
                                        isTestingConnection = uiState.isTestingAiConnection,
                                        statusBanner = uiState.aiSettingsStatusBanner,
                                        statusIsError = uiState.aiSettingsStatusIsError,
                                        onSaveConfig = { id, provider, model, key, url, ocr, trans, chat ->
                                            viewModel.saveUserAiConfig(id, provider, model, key, url, ocr, trans, chat)
                                        },
                                        onTestConnection = { id, provider, model, key, url ->
                                            viewModel.testUserAiConnection(id, provider, model, key, url)
                                        },
                                        onDeleteConfig = { viewModel.deleteUserAiConfig(it) },
                                        onLogout = { viewModel.logout() },
                                        userTopLanguages = uiState.userTopLanguages,
                                        onSaveTopThreeLanguages = { viewModel.updateUserTopThreeLanguages(it) },
                                        onChangePassword = { currentPwd, newPwd -> viewModel.changeUserPassword(currentPwd, newPwd) },
                                        initialCoolifyBackendUrl = viewModel.getSavedCoolifyBackendUrl(),
                                        onSaveCoolifyBackendUrl = { url -> viewModel.saveAndSyncCoolifyBackendUrl(url) }
                                    )
                                }
                            }

                            TopLevelDestination.AUTH_PORTAL -> {
                                AuthPortalScreen(
                                    isSignUpMode = uiState.isSignUpMode,
                                    isLoading = uiState.isAuthLoading,
                                    errorMessage = uiState.authErrorMessage,
                                    defaultLanguage = uiState.selectedLanguage,
                                    onToggleMode = { viewModel.toggleAuthMode(it) },
                                    onLoginSubmit = { email, pass -> viewModel.login(email, pass) },
                                    onSignUpSubmit = { name, email, pass, lang -> viewModel.signUp(name, email, pass, lang) },
                                    onLoginAsSuperadminClick = { viewModel.authenticateAsSuperadminAndOpenDashboard() },
                                    onRequestPasswordReset = { email -> viewModel.requestPasswordReset(email) },
                                    onConfirmPasswordReset = { email, code, newPwd -> viewModel.confirmPasswordReset(email, code, newPwd) }
                                )
                            }

                            TopLevelDestination.SUPERADMIN_DASHBOARD -> {
                                SuperadminDashboardScreen(
                                    currentUser = uiState.currentUser,
                                    overview = uiState.superadminOverview,
                                    plans = uiState.commercialPlans,
                                    books = uiState.allSystemBooks,
                                    users = uiState.allSystemUsers,
                                    entitlements = uiState.allSystemEntitlements,
                                    translations = uiState.allCanonicalTranslations,
                                    metadataItems = uiState.platformMetadataList,
                                    auditLogs = uiState.adminAuditLogs,
                                    adminActionBanner = uiState.adminActionBanner,
                                    adminActionIsError = uiState.adminActionIsError,
                                    onDismissBanner = { viewModel.clearAdminActionBanner() },
                                    onBackToLibrary = {
                                        viewModel.navigateToDestination(
                                            if (isLoggedIn) TopLevelDestination.MY_HOME else TopLevelDestination.HOW_IT_WORKS
                                        )
                                    },
                                    onLogoutSuperadmin = { viewModel.logout() },
                                    onSavePlan = { existingPlanId, planCode, name, desc, isEnabled, monthlyPrice, yearlyPrice, currencyCode, trialDays, displayOrder, isFeatured, featuresCsv, allowedPoliciesCsv, dailyTransLimit, monthlyAiTokens, offlineEnabled, advSearchEnabled, ttsMinutesLimit, reason ->
                                        viewModel.adminSaveCommercialPlan(
                                            existingPlanId = existingPlanId,
                                            planCode = planCode,
                                            name = name,
                                            description = desc,
                                            isEnabled = isEnabled,
                                            monthlyPrice = monthlyPrice,
                                            yearlyPrice = yearlyPrice,
                                            currencyCode = currencyCode,
                                            trialDays = trialDays,
                                            displayOrder = displayOrder,
                                            isFeatured = isFeatured,
                                            featuresCsv = featuresCsv,
                                            allowedBookAccessPoliciesCsv = allowedPoliciesCsv,
                                            dailyTranslationLimit = dailyTransLimit,
                                            monthlyManagedAiTokensLimit = monthlyAiTokens,
                                            offlineAccessEnabled = offlineEnabled,
                                            advancedSearchAndScholarlyEnabled = advSearchEnabled,
                                            ttsAudioDailyMinutesLimit = ttsMinutesLimit,
                                            reasonOrNote = reason
                                        )
                                    },
                                    onTogglePlanEnabled = { planId, enabled, reason ->
                                        viewModel.adminTogglePlanEnabled(planId, enabled, reason)
                                    },
                                    onGrantEntitlement = { targetUserId, kind, targetRef, displayTitle, durationDays, reason ->
                                        viewModel.adminGrantUserEntitlement(
                                            targetUserId = targetUserId,
                                            entitlementKind = kind,
                                            targetRef = targetRef,
                                            displayTitle = displayTitle,
                                            durationDays = durationDays,
                                            reasonOrNote = reason
                                        )
                                    },
                                    onRevokeEntitlement = { entId, reason ->
                                        viewModel.adminRevokeUserEntitlement(entId, reason)
                                    },
                                    onExtendEntitlement = { entId, extraDays, reason ->
                                        viewModel.adminExtendUserEntitlement(entId, extraDays, reason)
                                    },
                                    onCreateBook = { title, subtitle, authorOrSource, contributors, publisherName, desc, origLang, category, genresCsv, tagsCsv, workId, editionLabel, isbnOrId, pubYear, pubInfo, coverHex, coverUrl, accessPolicy, reqPlanCode, rightsCat, pubStatus, priceDisplay, sampleText, reason ->
                                        viewModel.adminCreateBook(
                                            title = title,
                                            subtitle = subtitle,
                                            authorOrSource = authorOrSource,
                                            contributors = contributors,
                                            publisherName = publisherName,
                                            description = desc,
                                            originalLanguage = origLang,
                                            category = category,
                                            genresCsv = genresCsv,
                                            tagsCsv = tagsCsv,
                                            workId = workId,
                                            editionLabel = editionLabel,
                                            isbnOrIdentifier = isbnOrId,
                                            publicationYear = pubYear,
                                            publicationInfo = pubInfo,
                                            coverBadgeColorHex = coverHex,
                                            coverImageUrl = coverUrl,
                                            accessPolicy = accessPolicy,
                                            requiredPlanCode = reqPlanCode,
                                            rightsCategory = rightsCat,
                                            publicationStatus = pubStatus,
                                            priceDisplay = priceDisplay,
                                            initialSampleParagraphText = sampleText,
                                            reasonOrNote = reason
                                        )
                                    },
                                    onUpdateBookMetadataAndAccess = { bookId, title, subtitle, authorOrSource, contributors, publisherName, desc, origLang, category, genresCsv, tagsCsv, workId, editionLabel, isbnOrId, pubYear, pubInfo, coverHex, coverUrl, customMetaJson, searchIdxStatus, pubStatus, accessPolicy, reqPlanCode, rightsCat, priceDisplay, reason ->
                                        viewModel.adminUpdateBookMetadataAndAccess(
                                            bookId = bookId,
                                            title = title,
                                            subtitle = subtitle,
                                            authorOrSource = authorOrSource,
                                            contributors = contributors,
                                            publisherName = publisherName,
                                            description = desc,
                                            originalLanguage = origLang,
                                            category = category,
                                            genresCsv = genresCsv,
                                            tagsCsv = tagsCsv,
                                            workId = workId,
                                            editionLabel = editionLabel,
                                            isbnOrIdentifier = isbnOrId,
                                            publicationYear = pubYear,
                                            publicationInfo = pubInfo,
                                            coverBadgeColorHex = coverHex,
                                            coverImageUrl = coverUrl,
                                            customMetadataJson = customMetaJson,
                                            searchIndexStatus = searchIdxStatus,
                                            publicationStatus = pubStatus,
                                            accessPolicy = accessPolicy,
                                            requiredPlanCode = reqPlanCode,
                                            rightsCategory = rightsCat,
                                            priceDisplay = priceDisplay,
                                            reasonOrNote = reason
                                        )
                                    },
                                    onSetBookPublicationStatus = { bookId, status, reason ->
                                        viewModel.adminSetBookPublicationStatus(bookId, status, reason)
                                    },
                                    onApproveOrRejectTranslation = { transId, status, reason ->
                                        viewModel.adminApproveOrRejectTranslation(transId, status, reason)
                                    },
                                    onSetPreferredTranslation = { transId, reason ->
                                        viewModel.adminSetPreferredTranslation(transId, reason)
                                    },
                                    onUpdateTranslationAccessPolicy = { transId, policy, reason ->
                                        viewModel.adminUpdateTranslationAccessPolicy(transId, policy, reason)
                                    },
                                    onTriggerRetranslationVersion = { transId, newText, newNotes, modelUsed, reason ->
                                        viewModel.adminTriggerRetranslationVersion(transId, newText, newNotes, modelUsed, reason)
                                    },
                                    onUpsertPlatformMetadata = { metadataId, metadataType, keyCode, displayLabel, desc, sortOrder, isActive, extraJson, reason ->
                                        viewModel.adminUpsertPlatformMetadata(
                                            metadataId = metadataId,
                                            metadataType = metadataType,
                                            keyCode = keyCode,
                                            displayLabel = displayLabel,
                                            description = desc,
                                            sortOrder = sortOrder,
                                            isActive = isActive,
                                            extraAttributesJson = extraJson,
                                            reasonOrNote = reason
                                        )
                                    },
                                    onSetUserAccountStatus = { targetUserId, status, reason ->
                                        viewModel.adminSetUserAccountStatus(targetUserId, status, reason)
                                    },
                                    onUpdateUserRole = { targetUserId, role, reason ->
                                        viewModel.adminUpdateUserRole(targetUserId, role, reason)
                                    },
                                    onResetUserPassword = { targetUserId, newPwd, reason ->
                                        viewModel.adminResetUserPassword(targetUserId, newPwd, reason)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Contributor Licence Agreement Modal (#3)
    uiState.pendingContributorAction?.let { pending ->
        ContributorLicenseAgreementDialog(
            pendingAction = pending,
            onConfirmAndProceed = { viewModel.confirmContributorAgreementAndProceed() },
            onDismiss = { viewModel.dismissContributorAgreementDialog() }
        )
    }

    // Subscription Tiers & Granular Entitlements Modal (#18, #19)
    if (uiState.isPlansDialogVisible) {
        SubscriptionTiersAndEntitlementsDialog(
            currentUser = uiState.currentUser,
            activeEntitlements = uiState.activeEntitlements,
            commercialPlans = uiState.commercialPlans,
            onSelectTier = { tier, instCode ->
                viewModel.switchSubscriptionTier(tier, instCode)
            },
            onDismiss = { viewModel.closePlansDialog() }
        )
    }
}

@Composable
private fun ReaderModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        border = if (selected) {
            null
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
