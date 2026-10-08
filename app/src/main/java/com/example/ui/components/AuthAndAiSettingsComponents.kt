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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AiProviderDescriptor
import com.example.data.local.AiProviderRegistry
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.UserAiConfigEntity
import com.example.data.local.UserEntity

/**
 * Authentication Portal (Login & Sign Up) with validation, password visibility toggle,
 * and Top 3 preferred language selection from the 100-language catalogue.
 */
@Composable
fun AuthPortalScreen(
    isSignUpMode: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    defaultLanguage: LanguageDescriptor,
    onToggleMode: (Boolean) -> Unit,
    onLoginSubmit: (email: String, password: String) -> Unit,
    onSignUpSubmit: (name: String, email: String, password: String, preferredLanguage: String) -> Unit,
    onLoginAsSuperadminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var signUpTopThree by remember {
        mutableStateOf(
            LanguageRegistry.parseTopThreeLanguages(null, defaultLanguage.name)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("auth_portal_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isSignUpMode) "Create Your Account" else "Welcome Back",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSignUpMode) {
                                "Choose your Top 3 languages & connect your own AI keys"
                            } else {
                                "Sign in to access your books, Top 3 languages & AI settings"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isSignUpMode) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("Aarav Patel") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_name_input")
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("reader@example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text("At least 6 characters") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                if (isSignUpMode) {
                    Text(
                        text = "Your Top 3 Languages (${signUpTopThree.joinToString(", ") { it.name }})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageRegistry.ALL_LANGUAGES.forEach { lang ->
                            val selected = signUpTopThree.any { it.code == lang.code }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    signUpTopThree = if (selected) {
                                        if (signUpTopThree.size > 1) {
                                            signUpTopThree.filterNot { it.code == lang.code }
                                        } else {
                                            signUpTopThree
                                        }
                                    } else if (signUpTopThree.size < LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES) {
                                        signUpTopThree + lang
                                    } else {
                                        signUpTopThree.dropLast(1) + lang
                                    }
                                },
                                label = { Text("${lang.nativeName} (${lang.name})") }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isSignUpMode) {
                            onSignUpSubmit(
                                displayName,
                                email,
                                password,
                                LanguageRegistry.formatTopThreeCsv(signUpTopThree)
                            )
                        } else {
                            onLoginSubmit(email, password)
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 13.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSignUpMode) "Create Account & Continue" else "Sign In",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account?" else "New to Polyglot?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSignUpMode) "Sign In" else "Create Account",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleMode(!isSignUpMode) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("auth_toggle_mode_button")
                    )
                }

                if (onLoginAsSuperadminClick != null && !isSignUpMode) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    OutlinedButton(
                        onClick = onLoginAsSuperadminClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_superadmin_quick_login_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Superadmin Console Login",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In as Platform Superadmin",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authenticated User Account, Top 3 Languages Selector & AI Settings Screen:
 * 1. Top 3 Active Languages Selector (out of 100+ Indian, Classical & Global languages):
 *    - Users pick their Top 3 languages.
 *    - Only those 3 languages are shown in the Reader and queried on the server, reducing server load.
 * 2. User-Owned AI Provider & Credential Vault (Encrypted AES-256-GCM at rest).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserAiSettingsScreen(
    currentUser: UserEntity,
    savedConfigs: List<UserAiConfigEntity>,
    isTestingConnection: Boolean,
    statusBanner: String?,
    statusIsError: Boolean,
    onSaveConfig: (
        existingConfigId: Long?,
        providerId: String,
        modelName: String,
        rawApiKey: String,
        customBaseUrl: String,
        useForOcr: Boolean,
        useForTranslation: Boolean,
        useForChat: Boolean
    ) -> Unit,
    onTestConnection: (
        configId: Long?,
        providerId: String,
        modelName: String,
        rawApiKeyInput: String,
        customBaseUrl: String
    ) -> Unit,
    onDeleteConfig: (Long) -> Unit,
    onLogout: () -> Unit,
    userTopLanguages: List<LanguageDescriptor> = LanguageRegistry.parseTopThreeLanguages(
        currentUser.topThreeLanguagesCsv,
        currentUser.preferredLanguage
    ),
    onSaveTopThreeLanguages: (List<LanguageDescriptor>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedProvider by remember { mutableStateOf(AiProviderRegistry.PROVIDERS.first()) }
    var editingConfigId by remember { mutableStateOf<Long?>(null) }
    var selectedModel by remember { mutableStateOf(selectedProvider.availableModels.first()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var customBaseUrlInput by remember { mutableStateOf(selectedProvider.defaultBaseUrl) }
    var useForOcr by remember { mutableStateOf(true) }
    var useForTranslation by remember { mutableStateOf(true) }
    var useForChat by remember { mutableStateOf(true) }
    var showRawKey by remember { mutableStateOf(false) }

    // Top 3 Languages Selector state
    var draftTopThree by remember(userTopLanguages) {
        mutableStateOf(userTopLanguages.take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES))
    }
    var languageSearchQuery by rememberSaveable { mutableStateOf("") }
    var languageRegionFilter by rememberSaveable { mutableStateOf("ALL") } // ALL, INDIAN, GLOBAL
    var showAllCatalogueExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(userTopLanguages) {
        draftTopThree = userTopLanguages.take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES)
    }

    val filteredCatalogue = remember(languageSearchQuery, languageRegionFilter) {
        val base = when (languageRegionFilter) {
            "INDIAN" -> LanguageRegistry.INDIAN_LANGUAGES
            "GLOBAL" -> LanguageRegistry.GLOBAL_LANGUAGES
            else -> LanguageRegistry.ALL_LANGUAGES
        }
        val q = languageSearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            base
        } else {
            base.filter {
                it.name.lowercase().contains(q) ||
                    it.nativeName.lowercase().contains(q) ||
                    it.scriptName.lowercase().contains(q) ||
                    it.code.lowercase().contains(q)
            }
        }
    }

    fun selectProviderDescriptor(provider: AiProviderDescriptor, existing: UserAiConfigEntity? = null) {
        selectedProvider = provider
        editingConfigId = existing?.id
        selectedModel = existing?.modelName ?: provider.availableModels.first()
        customBaseUrlInput = existing?.customBaseUrl?.ifBlank { provider.defaultBaseUrl } ?: provider.defaultBaseUrl
        apiKeyInput = "" // Never pre-fill raw secret key into frontend input
        useForOcr = existing?.isDefaultForOcr ?: true
        useForTranslation = existing?.isDefaultForTranslation ?: true
        useForChat = existing?.isDefaultForSummaryAndChat ?: true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Account Summary Header
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentUser.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = currentUser.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentUser.email}  •  Active Top 3: ${draftTopThree.joinToString(", ") { it.name }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Text("Log Out")
                        }
                    }
                }
            }
        }

        // Status Banner (Top 3 Languages Save / Test Connection / AI Save feedback)
        if (!statusBanner.isNullOrBlank()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = if (statusIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 820.dp)
                            .testTag("ai_settings_status_banner")
                    ) {
                        Text(
                            text = statusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (statusIsError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TOP 3 LANGUAGES SELECTOR CARD (Out of 100+ Languages — Less Burden on Server)
        // =====================================================================
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp)
                        .testTag("top_three_languages_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Your Top 3 Languages",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${LanguageRegistry.ALL_LANGUAGES.size} languages available (${LanguageRegistry.INDIAN_LANGUAGES.size} Indian & Classical + ${LanguageRegistry.GLOBAL_LANGUAGES.size} Global). Only your Top 3 are shown in the reader and queried on the server.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Active Top 3 Language Slots
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            draftTopThree.forEachIndexed { index, lang ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (index == 0) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            // Promote tapped slot to #1 Primary
                                            if (index > 0) {
                                                val reordered = listOf(lang) + draftTopThree.filterNot { it.code == lang.code }
                                                draftTopThree = reordered
                                                onSaveTopThreeLanguages(reordered)
                                            }
                                        }
                                        .testTag("active_top_lang_slot_${index + 1}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = if (index == 0) "#1 Primary • ${lang.name}" else "#${index + 1} • ${lang.name}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (index == 0) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer
                                                }
                                            )
                                            Text(
                                                text = "${lang.nativeName} (${lang.scriptName})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (index == 0) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                }
                                            )
                                        }
                                        if (draftTopThree.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    val updated = draftTopThree.filterNot { it.code == lang.code }
                                                    draftTopThree = updated
                                                    onSaveTopThreeLanguages(updated)
                                                },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("remove_top_lang_${lang.code}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove ${lang.name}",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Search & Filter across all 100 languages
                        OutlinedTextField(
                            value = languageSearchQuery,
                            onValueChange = { languageSearchQuery = it },
                            placeholder = {
                                Text("Search ${LanguageRegistry.ALL_LANGUAGES.size} languages (e.g., Gujarati, Sanskrit, Pali, Tamil, Japanese, Spanish…)")
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (languageSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { languageSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_language_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = languageRegionFilter == "ALL",
                                onClick = { languageRegionFilter = "ALL" },
                                label = { Text("All Languages (${LanguageRegistry.ALL_LANGUAGES.size})") }
                            )
                            FilterChip(
                                selected = languageRegionFilter == "INDIAN",
                                onClick = { languageRegionFilter = "INDIAN" },
                                label = { Text("Indian & Classical (${LanguageRegistry.INDIAN_LANGUAGES.size})") }
                            )
                            FilterChip(
                                selected = languageRegionFilter == "GLOBAL",
                                onClick = { languageRegionFilter = "GLOBAL" },
                                label = { Text("Global (${LanguageRegistry.GLOBAL_LANGUAGES.size})") }
                            )
                        }

                        val displayedLanguages = if (showAllCatalogueExpanded || languageSearchQuery.isNotBlank()) {
                            filteredCatalogue
                        } else {
                            filteredCatalogue.take(24)
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayedLanguages.forEach { lang ->
                                val slotIndex = draftTopThree.indexOfFirst { it.code == lang.code }
                                val isSelected = slotIndex >= 0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = if (isSelected) {
                                            if (draftTopThree.size > 1) {
                                                draftTopThree.filterNot { it.code == lang.code }
                                            } else {
                                                draftTopThree
                                            }
                                        } else if (draftTopThree.size < LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES) {
                                            draftTopThree + lang
                                        } else {
                                            // Replace slot #3 when 3 are already selected
                                            draftTopThree.dropLast(1) + lang
                                        }
                                        draftTopThree = updated
                                        onSaveTopThreeLanguages(updated)
                                    },
                                    label = {
                                        Text(
                                            text = if (isSelected) {
                                                "#${slotIndex + 1} ${lang.name} • ${lang.nativeName}"
                                            } else {
                                                "${lang.name} • ${lang.nativeName}"
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.testTag("top_lang_chip_${lang.code}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (filteredCatalogue.size > 24 && languageSearchQuery.isBlank()) {
                                OutlinedButton(
                                    onClick = { showAllCatalogueExpanded = !showAllCatalogueExpanded },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("toggle_all_languages_catalogue_button")
                                ) {
                                    Text(
                                        if (showAllCatalogueExpanded) {
                                            "Show Fewer Languages"
                                        } else {
                                            "Show All ${filteredCatalogue.size} Languages"
                                        }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Button(
                                onClick = { onSaveTopThreeLanguages(draftTopThree) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_top_three_languages_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Top 3 (${draftTopThree.size}/3)")
                            }
                        }
                    }
                }
            }
        }

        // Security Architecture Guarantee Callout
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bring Your Own AI Key • Encrypted At Rest (AES-256-GCM)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your API keys are encrypted in your isolated user vault, never logged, and never shared with other users. Configure OpenAI, Anthropic Claude, Google Gemini, or OpenRouter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Saved User AI Providers List
        if (savedConfigs.isNotEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Your Configured AI Providers (${savedConfigs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 820.dp)
                    )
                }
            }

            items(savedConfigs, key = { it.id }) { cfg ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 820.dp)
                            .testTag("saved_ai_config_${cfg.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${cfg.providerDisplayName} • ${cfg.modelName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Encrypted Key: ${cfg.maskedKeyPreview} • Status: ${cfg.lastTestedStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    OutlinedButton(
                                        onClick = {
                                            onTestConnection(
                                                cfg.id,
                                                cfg.providerId,
                                                cfg.modelName,
                                                "",
                                                cfg.customBaseUrl
                                            )
                                        },
                                        enabled = !isTestingConnection,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Test", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteConfig(cfg.id) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove credential",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (cfg.isDefaultForOcr) {
                                    TaskRoleBadge("Active for OCR")
                                }
                                if (cfg.isDefaultForTranslation) {
                                    TaskRoleBadge("Active for Translation")
                                }
                                if (cfg.isDefaultForSummaryAndChat) {
                                    TaskRoleBadge("Active for Study Chat")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add / Update AI Provider Form Card
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 820.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Configure AI Provider & Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "1. Select AI Provider",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiProviderRegistry.PROVIDERS.forEach { provider ->
                                val isSelected = provider.id == selectedProvider.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val existing = savedConfigs.find { it.providerId == provider.id }
                                        selectProviderDescriptor(provider, existing)
                                    },
                                    label = { Text(provider.displayName) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("provider_chip_${provider.id.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = selectedProvider.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "2. Select or Enter Model",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedProvider.availableModels.forEach { m ->
                                FilterChip(
                                    selected = selectedModel == m,
                                    onClick = { selectedModel = m },
                                    label = { Text(m) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = { selectedModel = it },
                            label = { Text("Model Identifier") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_model_input")
                        )

                        Text(
                            text = "3. Enter Your Personal ${selectedProvider.displayName} API Key",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("API Key (${selectedProvider.keyPlaceholder})") },
                            placeholder = { Text("Paste your private API key here") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            visualTransformation = if (showRawKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showRawKey = !showRawKey }) {
                                    Icon(
                                        imageVector = if (showRawKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_api_key_input")
                        )

                        OutlinedTextField(
                            value = customBaseUrlInput,
                            onValueChange = { customBaseUrlInput = it },
                            label = { Text("API Base Endpoint URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "4. Assign AI Tasks to this Provider/Model",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useForTranslation, onCheckedChange = { useForTranslation = it })
                                Text(
                                    text = "Use for Chapter & Section Translation (Active Top 3 of ${LanguageRegistry.ALL_LANGUAGES.size} Languages)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useForOcr, onCheckedChange = { useForOcr = it })
                                Text(
                                    text = "Use for PDF Vision OCR & Structural Extraction",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useForChat, onCheckedChange = { useForChat = it })
                                Text(
                                    text = "Use for Summarization & AI Study Chat",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onTestConnection(
                                        editingConfigId,
                                        selectedProvider.id,
                                        selectedModel,
                                        apiKeyInput,
                                        customBaseUrlInput
                                    )
                                },
                                enabled = !isTestingConnection,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_ai_connection_button")
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection")
                            }

                            Button(
                                onClick = {
                                    onSaveConfig(
                                        editingConfigId,
                                        selectedConfigIdOrNull(editingConfigId, selectedProvider.id),
                                        selectedModel,
                                        apiKeyInput,
                                        customBaseUrlInput,
                                        useForOcr,
                                        useForTranslation,
                                        useForChat
                                    )
                                    apiKeyInput = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_ai_config_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Credentials")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun selectedConfigIdOrNull(@Suppress("UNUSED_PARAMETER") existingConfigId: Long?, providerId: String): String = providerId

@Composable
private fun TaskRoleBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
