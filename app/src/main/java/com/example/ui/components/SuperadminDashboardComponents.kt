package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountStatus
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.BookAccessPolicy
import com.example.data.local.BookEntity
import com.example.data.local.BookPublicationStatus
import com.example.data.local.BookRightsCategory
import com.example.data.local.CommercialPlanEntity
import com.example.data.local.EntitlementKind
import com.example.data.local.MetadataCategoryType
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.PlatformMetadataEntity
import com.example.data.local.SubscriptionTier
import com.example.data.local.SuperadminDashboardOverview
import com.example.data.local.TranslationAccessPolicy
import com.example.data.local.TranslationReviewStatus
import com.example.data.local.UserEntitlementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SuperadminSection(val title: String) {
    OVERVIEW("Overview"),
    PLANS("Plans & Pricing"),
    ENTITLEMENTS("Entitlements"),
    BOOKS("Books & Access"),
    TRANSLATIONS("Translations"),
    METADATA("Metadata"),
    USERS("Users & Roles"),
    AUDIT_LOG("Audit Trail")
}

@Composable
fun SuperadminDashboardScreen(
    currentUser: UserEntity?,
    overview: SuperadminDashboardOverview,
    plans: List<CommercialPlanEntity>,
    books: List<BookEntity>,
    users: List<UserEntity>,
    entitlements: List<UserEntitlementEntity>,
    translations: List<ParagraphTranslationEntity>,
    metadataItems: List<PlatformMetadataEntity>,
    auditLogs: List<AdminAuditLogEntity>,
    adminActionBanner: String?,
    adminActionIsError: Boolean,
    onDismissBanner: () -> Unit,
    onBackToLibrary: () -> Unit,
    onLogoutSuperadmin: () -> Unit,
    onSavePlan: (
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
    ) -> Unit,
    onTogglePlanEnabled: (Long, Boolean, String) -> Unit,
    onGrantEntitlement: (
        targetUserId: Long,
        entitlementKind: String,
        targetRef: String,
        displayTitle: String,
        durationDays: Int,
        reasonOrNote: String
    ) -> Unit,
    onRevokeEntitlement: (Long, String) -> Unit,
    onExtendEntitlement: (Long, Int, String) -> Unit,
    onCreateBook: (
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
    ) -> Unit,
    onUpdateBookMetadataAndAccess: (
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
    ) -> Unit,
    onSetBookPublicationStatus: (Long, String, String) -> Unit,
    onApproveOrRejectTranslation: (Long, String, String) -> Unit,
    onSetPreferredTranslation: (Long, String) -> Unit,
    onUpdateTranslationAccessPolicy: (Long, String, String) -> Unit,
    onTriggerRetranslationVersion: (Long, String, String, String, String) -> Unit,
    onUpsertPlatformMetadata: (
        metadataId: Long?,
        metadataType: String,
        keyCode: String,
        displayLabel: String,
        description: String,
        sortOrder: Int,
        isActive: Boolean,
        extraAttributesJson: String,
        reasonOrNote: String
    ) -> Unit,
    onSetUserAccountStatus: (Long, String, String) -> Unit,
    onUpdateUserRole: (Long, String, String) -> Unit,
    onResetUserPassword: ((Long, String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToLibrary)

    if (currentUser == null || !currentUser.role.equals(UserRole.SUPERADMIN, ignoreCase = true)) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Restricted Area",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Superadmin Authorization Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "This administration area is strictly protected by backend RBAC enforcement. Only authenticated accounts with the SUPERADMIN role and ACTIVE status can access or invoke platform management endpoints.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onBackToLibrary,
                        modifier = Modifier.testTag("superadmin_unauthorized_back_btn")
                    ) {
                        Text("Return to Library")
                    }
                }
            }
        }
        return
    }

    var selectedSection by rememberSaveable { mutableStateOf(SuperadminSection.OVERVIEW) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("superadmin_dashboard_screen")
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Superadmin Backend Console",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Superadmin Backend Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${currentUser.displayName} (${currentUser.email}) • Role: ${currentUser.role}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onLogoutSuperadmin,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("superadmin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "End Admin Session",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logout", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (!adminActionBanner.isNullOrBlank()) {
                    Surface(
                        color = if (adminActionIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = adminActionBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (adminActionIsError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissBanner,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss Banner",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedSection.ordinal,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    SuperadminSection.entries.forEach { section ->
                        Tab(
                            selected = selectedSection == section,
                            onClick = { selectedSection = section },
                            modifier = Modifier.testTag("superadmin_tab_${section.name.lowercase()}"),
                            text = {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selectedSection == section) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        when (selectedSection) {
            SuperadminSection.OVERVIEW -> SuperadminOverviewPane(
                overview = overview,
                onNavigateSection = { selectedSection = it }
            )
            SuperadminSection.PLANS -> SuperadminPlansManagementPane(
                plans = plans,
                onSavePlan = onSavePlan,
                onTogglePlanEnabled = onTogglePlanEnabled
            )
            SuperadminSection.ENTITLEMENTS -> SuperadminEntitlementsManagementPane(
                users = users,
                books = books,
                plans = plans,
                entitlements = entitlements,
                onGrantEntitlement = onGrantEntitlement,
                onRevokeEntitlement = onRevokeEntitlement,
                onExtendEntitlement = onExtendEntitlement
            )
            SuperadminSection.BOOKS -> SuperadminBooksManagementPane(
                books = books,
                plans = plans,
                onCreateBook = onCreateBook,
                onUpdateBookMetadataAndAccess = onUpdateBookMetadataAndAccess,
                onSetBookPublicationStatus = onSetBookPublicationStatus
            )
            SuperadminSection.TRANSLATIONS -> SuperadminTranslationsManagementPane(
                books = books,
                translations = translations,
                onApproveOrRejectTranslation = onApproveOrRejectTranslation,
                onSetPreferredTranslation = onSetPreferredTranslation,
                onUpdateTranslationAccessPolicy = onUpdateTranslationAccessPolicy,
                onTriggerRetranslationVersion = onTriggerRetranslationVersion
            )
            SuperadminSection.METADATA -> SuperadminMetadataManagementPane(
                metadataItems = metadataItems,
                onUpsertPlatformMetadata = onUpsertPlatformMetadata
            )
            SuperadminSection.USERS -> SuperadminUsersManagementPane(
                users = users,
                entitlements = entitlements,
                translations = translations,
                onSetUserAccountStatus = onSetUserAccountStatus,
                onUpdateUserRole = onUpdateUserRole,
                onResetUserPassword = onResetUserPassword
            )
            SuperadminSection.AUDIT_LOG -> SuperadminAuditTrailPane(
                auditLogs = auditLogs
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminOverviewPane(
    overview: SuperadminDashboardOverview,
    onNavigateSection: (SuperadminSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Platform Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                MetricSummaryCard(
                    label = "Total Users",
                    value = overview.totalUsers.toString(),
                    subtitle = "${overview.freeUsersCount} Free • ${overview.paidUsersCount} Paid",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    label = "Active Subscriptions",
                    value = overview.activeSubscriptionsCount.toString(),
                    subtitle = "${overview.suspendedUsersCount} Suspended",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    label = "Total Titles",
                    value = overview.totalBooksCount.toString(),
                    subtitle = "${overview.publishedBooksCount} Published • ${overview.draftOrUnpublishedBooksCount} Draft",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    label = "Archived Titles",
                    value = overview.archivedBooksCount.toString(),
                    subtitle = "OCR & translations preserved",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    label = "Translations",
                    value = overview.totalTranslationsCount.toString(),
                    subtitle = "${overview.translationsByLanguage.size} Target languages",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Books by language
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Books by Original Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (overview.booksByOriginalLanguage.isEmpty()) {
                        Text(
                            text = "No books cataloged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            overview.booksByOriginalLanguage.entries.forEach { entry ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${entry.key}: ${entry.value}",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateSection(SuperadminSection.PLANS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manage Plans", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNavigateSection(SuperadminSection.BOOKS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manage Books", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNavigateSection(SuperadminSection.ENTITLEMENTS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Entitlements", maxLines = 1)
                }
            }
        }

        // Recent registrations
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent User Registrations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    overview.recentRegistrations.take(5).forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${user.displayName} (${user.email})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Role: ${user.role} • Status: ${user.accountStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatAdminTimestamp(user.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Recent translations/contributions
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent Translations & Contributions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (overview.recentTranslations.isEmpty()) {
                        Text(
                            text = "No translations recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        overview.recentTranslations.take(6).forEach { tr ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tr.sourceLanguage} → ${tr.targetLanguage} (v${tr.versionNumber}) • ${tr.reviewStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = tr.translatedText.take(90),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = tr.translatorDisplayName.take(18),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Recent administrative activity
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent Administrative Activity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    overview.recentAuditLogs.take(6).forEach { log ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.action} • ${log.entityAffected}#${log.entityId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatAdminTimestamp(log.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "By ${log.adminEmailOrName}: ${log.newValue.take(100)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuperadminPlansManagementPane(
    plans: List<CommercialPlanEntity>,
    onSavePlan: (
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
    ) -> Unit,
    onTogglePlanEnabled: (Long, Boolean, String) -> Unit
) {
    var editingPlan by remember { mutableStateOf<CommercialPlanEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_plans_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Commercial Plans & Pricing Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Database-backed plans dynamically govern Android app pricing, quotas, AI limits, offline access, and TTS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingPlan = CommercialPlanEntity(
                            id = 0L,
                            planCode = "CUSTOM_${System.currentTimeMillis() % 1000}",
                            name = "New Custom Plan",
                            description = "Custom subscription tier configured by Superadmin.",
                            isEnabled = true,
                            monthlyPriceAmount = 14.99,
                            yearlyPriceAmount = 149.0,
                            currencyCode = "USD",
                            trialDays = 7,
                            displayOrder = plans.size + 1,
                            isFeatured = false,
                            featuresCsv = "Full multilingual library,Cloud sync",
                            allowedBookAccessPoliciesCsv = "PUBLIC,FREE,PAID",
                            dailyTranslationLimit = 250,
                            monthlyManagedAiTokensLimit = 100000,
                            offlineAccessEnabled = true,
                            advancedSearchAndScholarlyEnabled = true,
                            ttsAudioDailyMinutesLimit = 180
                        )
                    },
                    modifier = Modifier.testTag("superadmin_create_plan_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Create Plan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Plan")
                }
            }
        }

        items(plans, key = { it.id }) { plan ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_plan_card_${plan.planCode}"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (plan.isFeatured) 1.5.dp else 1.dp,
                    color = if (plan.isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${plan.name} (${plan.planCode})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (plan.isFeatured) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Featured",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Order #${plan.displayOrder} • Allowed Book Policies: ${plan.allowedBookAccessPoliciesCsv}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (plan.isEnabled) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = plan.isEnabled,
                                onCheckedChange = { enabled ->
                                    onTogglePlanEnabled(
                                        plan.id,
                                        enabled,
                                        "Superadmin toggled plan ${plan.planCode} enabled=$enabled"
                                    )
                                },
                                modifier = Modifier.testTag("superadmin_toggle_plan_${plan.planCode}")
                            )
                        }
                    }

                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Pricing: ${plan.formatPriceSummary()} • Trial: ${plan.trialDays} days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Quotas: Daily Translations=${if (plan.dailyTranslationLimit < 0) "Unlimited" else plan.dailyTranslationLimit} • Managed AI Tokens/mo=${if (plan.monthlyManagedAiTokensLimit < 0) "Unlimited" else plan.monthlyManagedAiTokensLimit} • TTS Mins/day=${if (plan.ttsAudioDailyMinutesLimit < 0) "Unlimited" else plan.ttsAudioDailyMinutesLimit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Capabilities: Offline=${plan.offlineAccessEnabled} • Advanced Search=${plan.advancedSearchAndScholarlyEnabled} • Features: ${plan.featuresCsv}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { editingPlan = plan },
                            modifier = Modifier.testTag("superadmin_edit_plan_${plan.planCode}")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Plan", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Pricing & Limits")
                        }
                    }
                }
            }
        }
    }

    editingPlan?.let { planToEdit ->
        CommercialPlanEditorDialog(
            initialPlan = planToEdit,
            onDismiss = { editingPlan = null },
            onConfirmSave = { existingId, code, name, desc, enabled, monthly, yearly, currency, trial, order, featured, features, policies, dailyTrans, aiTokens, offline, advSearch, ttsMins, reason ->
                onSavePlan(
                    existingId, code, name, desc, enabled, monthly, yearly, currency, trial,
                    order, featured, features, policies, dailyTrans, aiTokens, offline, advSearch, ttsMins, reason
                )
                editingPlan = null
            }
        )
    }
}

@Composable
private fun CommercialPlanEditorDialog(
    initialPlan: CommercialPlanEntity,
    onDismiss: () -> Unit,
    onConfirmSave: (
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
    ) -> Unit
) {
    var planCode by rememberSaveable { mutableStateOf(initialPlan.planCode) }
    var name by rememberSaveable { mutableStateOf(initialPlan.name) }
    var description by rememberSaveable { mutableStateOf(initialPlan.description) }
    var monthlyPriceText by rememberSaveable { mutableStateOf(initialPlan.monthlyPriceAmount.toString()) }
    var yearlyPriceText by rememberSaveable { mutableStateOf(initialPlan.yearlyPriceAmount.toString()) }
    var currencyCode by rememberSaveable { mutableStateOf(initialPlan.currencyCode) }
    var trialDaysText by rememberSaveable { mutableStateOf(initialPlan.trialDays.toString()) }
    var displayOrderText by rememberSaveable { mutableStateOf(initialPlan.displayOrder.toString()) }
    var isFeatured by rememberSaveable { mutableStateOf(initialPlan.isFeatured) }
    var isEnabled by rememberSaveable { mutableStateOf(initialPlan.isEnabled) }
    var featuresCsv by rememberSaveable { mutableStateOf(initialPlan.featuresCsv) }
    var allowedPoliciesCsv by rememberSaveable { mutableStateOf(initialPlan.allowedBookAccessPoliciesCsv) }
    var dailyTranslationLimitText by rememberSaveable { mutableStateOf(initialPlan.dailyTranslationLimit.toString()) }
    var managedAiTokensText by rememberSaveable { mutableStateOf(initialPlan.monthlyManagedAiTokensLimit.toString()) }
    var ttsMinutesText by rememberSaveable { mutableStateOf(initialPlan.ttsAudioDailyMinutesLimit.toString()) }
    var offlineEnabled by rememberSaveable { mutableStateOf(initialPlan.offlineAccessEnabled) }
    var advancedSearchEnabled by rememberSaveable { mutableStateOf(initialPlan.advancedSearchAndScholarlyEnabled) }
    var auditReason by rememberSaveable { mutableStateOf("Updated commercial plan pricing and limits") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Commercial Plan") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = planCode,
                        onValueChange = { planCode = it.uppercase() },
                        label = { Text("Plan Code (e.g. READER, PRO_RESEARCH)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("plan_editor_code_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Plan Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("plan_editor_name_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = monthlyPriceText,
                            onValueChange = { monthlyPriceText = it },
                            label = { Text("Monthly Price") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("plan_editor_monthly_input")
                        )
                        OutlinedTextField(
                            value = yearlyPriceText,
                            onValueChange = { yearlyPriceText = it },
                            label = { Text("Yearly Price") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("plan_editor_yearly_input")
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencyCode,
                            onValueChange = { currencyCode = it.uppercase() },
                            label = { Text("Currency") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = trialDaysText,
                            onValueChange = { trialDaysText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Trial Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = displayOrderText,
                            onValueChange = { displayOrderText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Order") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = allowedPoliciesCsv,
                        onValueChange = { allowedPoliciesCsv = it },
                        label = { Text("Allowed Book Policies CSV (PUBLIC,FREE,PAID,RESTRICTED)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = featuresCsv,
                        onValueChange = { featuresCsv = it },
                        label = { Text("Plan Features (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dailyTranslationLimitText,
                            onValueChange = { dailyTranslationLimitText = it },
                            label = { Text("Daily Trans (-1=unlim)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = managedAiTokensText,
                            onValueChange = { managedAiTokensText = it },
                            label = { Text("AI Tokens/mo") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ttsMinutesText,
                            onValueChange = { ttsMinutesText = it },
                            label = { Text("TTS Mins/day") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                            Text("Featured", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = offlineEnabled, onCheckedChange = { offlineEnabled = it })
                            Text("Offline", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = advancedSearchEnabled, onCheckedChange = { advancedSearchEnabled = it })
                            Text("Adv Search", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = auditReason,
                        onValueChange = { auditReason = it },
                        label = { Text("Audit Log Reason / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSave(
                        initialPlan.id.takeIf { it > 0L },
                        planCode.trim().ifBlank { initialPlan.planCode },
                        name.trim().ifBlank { "Unnamed Plan" },
                        description.trim(),
                        isEnabled,
                        monthlyPriceText.toDoubleOrNull() ?: 0.0,
                        yearlyPriceText.toDoubleOrNull() ?: 0.0,
                        currencyCode.trim().ifBlank { "USD" },
                        trialDaysText.toIntOrNull() ?: 0,
                        displayOrderText.toIntOrNull() ?: 1,
                        isFeatured,
                        featuresCsv.trim(),
                        allowedPoliciesCsv.trim().ifBlank { "PUBLIC,FREE" },
                        dailyTranslationLimitText.toIntOrNull() ?: 50,
                        managedAiTokensText.toIntOrNull() ?: 0,
                        offlineEnabled,
                        advancedSearchEnabled,
                        ttsMinutesText.toIntOrNull() ?: 30,
                        auditReason
                    )
                },
                modifier = Modifier.testTag("plan_editor_save_button")
            ) {
                Text("Save Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminEntitlementsManagementPane(
    users: List<UserEntity>,
    books: List<BookEntity>,
    plans: List<CommercialPlanEntity>,
    entitlements: List<UserEntitlementEntity>,
    onGrantEntitlement: (
        targetUserId: Long,
        entitlementKind: String,
        targetRef: String,
        displayTitle: String,
        durationDays: Int,
        reasonOrNote: String
    ) -> Unit,
    onRevokeEntitlement: (Long, String) -> Unit,
    onExtendEntitlement: (Long, Int, String) -> Unit
) {
    var selectedUserId by rememberSaveable { mutableStateOf(users.firstOrNull()?.id ?: 1L) }
    var selectedKind by rememberSaveable { mutableStateOf(EntitlementKind.SUBSCRIPTION_TIER) }
    var targetRefInput by rememberSaveable { mutableStateOf(SubscriptionTier.PRO_RESEARCH) }
    var displayTitleInput by rememberSaveable { mutableStateOf("Pro Research Entitlement Grant") }
    var durationDaysInput by rememberSaveable { mutableStateOf("365") }
    var reasonNote by rememberSaveable { mutableStateOf("Granted by Superadmin via Access Console") }

    val kinds = listOf(
        EntitlementKind.FREE_ACCOUNT,
        EntitlementKind.SUBSCRIPTION_TIER,
        EntitlementKind.BOOK_PURCHASE,
        EntitlementKind.PROMOTIONAL_ACCESS,
        EntitlementKind.CONTRIBUTOR_ACCESS,
        EntitlementKind.INSTITUTION_MEMBERSHIP,
        EntitlementKind.PUBLISHER_ROLE,
        EntitlementKind.ADMIN_GRANTED_ACCESS
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_entitlements_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Grant Access or Assign Entitlement",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Assign subscriptions, individual book purchases, promotional grants, contributor access, institution memberships, publisher access, or admin-granted access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Target User:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.id }) { user ->
                            FilterChip(
                                selected = selectedUserId == user.id,
                                onClick = { selectedUserId = user.id },
                                label = { Text("${user.displayName} (#${user.id})") }
                            )
                        }
                    }

                    Text("Entitlement Kind:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        kinds.forEach { kind ->
                            FilterChip(
                                selected = selectedKind == kind,
                                onClick = {
                                    selectedKind = kind
                                    if (kind == EntitlementKind.BOOK_PURCHASE && books.isNotEmpty()) {
                                        val b = books.first()
                                        targetRefInput = b.id.toString()
                                        displayTitleInput = "Individual Book Access: ${b.title}"
                                    } else if (kind == EntitlementKind.SUBSCRIPTION_TIER && plans.isNotEmpty()) {
                                        val p = plans.first()
                                        targetRefInput = p.planCode
                                        displayTitleInput = "${p.name} Subscription"
                                    }
                                },
                                label = { Text(kind) }
                            )
                        }
                    }

                    if (selectedKind == EntitlementKind.BOOK_PURCHASE) {
                        Text("Select Individual Book:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(books, key = { it.id }) { book ->
                                FilterChip(
                                    selected = targetRefInput == book.id.toString(),
                                    onClick = {
                                        targetRefInput = book.id.toString()
                                        displayTitleInput = "Book Access: ${book.title}"
                                    },
                                    label = { Text("${book.title.take(24)} (#${book.id})") }
                                )
                            }
                        }
                    } else if (selectedKind == EntitlementKind.SUBSCRIPTION_TIER) {
                        Text("Select Plan:", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            plans.forEach { plan ->
                                FilterChip(
                                    selected = targetRefInput == plan.planCode,
                                    onClick = {
                                        targetRefInput = plan.planCode
                                        displayTitleInput = "${plan.name} Subscription"
                                    },
                                    label = { Text("${plan.name} (${plan.planCode})") }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetRefInput,
                            onValueChange = { targetRefInput = it },
                            label = { Text("Target Ref (Plan Code / Book ID / Inst Code)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("entitlement_target_ref_input")
                        )
                        OutlinedTextField(
                            value = durationDaysInput,
                            onValueChange = { durationDaysInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Duration Days") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = displayTitleInput,
                        onValueChange = { displayTitleInput = it },
                        label = { Text("Entitlement Display Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reasonNote,
                        onValueChange = { reasonNote = it },
                        label = { Text("Internal Reason / Audit Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("entitlement_reason_input")
                    )

                    Button(
                        onClick = {
                            onGrantEntitlement(
                                selectedUserId,
                                selectedKind,
                                targetRefInput.trim(),
                                displayTitleInput.trim(),
                                durationDaysInput.toIntOrNull() ?: 365,
                                reasonNote.trim()
                            )
                        },
                        modifier = Modifier.testTag("superadmin_grant_entitlement_button")
                    ) {
                        Text("Grant Entitlement")
                    }
                }
            }
        }

        item {
            Text(
                text = "Entitlement History & Active Grants (${entitlements.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(entitlements, key = { it.id }) { ent ->
            val user = users.find { it.id == ent.userId }
            val isActive = ent.status == "ACTIVE"
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_entitlement_row_${ent.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${user?.displayName ?: "User #${ent.userId}"} • ${ent.entitlementKind} (${ent.targetRef})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ent.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = ent.displayTitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Granted: ${formatAdminTimestamp(ent.grantedAt)} • Expires: ${formatAdminTimestamp(ent.expiresAt)} • By: ${ent.grantedByAdminName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ent.reasonOrNote.isNotBlank()) {
                        Text(
                            text = "Reason/Note: ${ent.reasonOrNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExtendEntitlement(
                                    ent.id,
                                    30,
                                    "Extended by +30 days by Superadmin"
                                )
                            },
                            modifier = Modifier.testTag("extend_entitlement_${ent.id}")
                        ) {
                            Text("Extend +30d")
                        }
                        if (isActive) {
                            OutlinedButton(
                                onClick = {
                                    onRevokeEntitlement(
                                        ent.id,
                                        "Revoked by Superadmin via Entitlement Console"
                                    )
                                },
                                modifier = Modifier.testTag("revoke_entitlement_${ent.id}")
                            ) {
                                Text("Revoke")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminBooksManagementPane(
    books: List<BookEntity>,
    plans: List<CommercialPlanEntity>,
    onCreateBook: (
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
    ) -> Unit,
    onUpdateBookMetadataAndAccess: (
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
    ) -> Unit,
    onSetBookPublicationStatus: (Long, String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<BookEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_books_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Books, Editions & Access Policy Governance (${books.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Editing metadata or archiving titles never destroys existing OCR pages, translations, or reading progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("superadmin_add_book_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Book", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Book")
                }
            }
        }

        items(books, key = { it.id }) { book ->
            val accessSummary = remember(book.publicationStatus, book.accessPolicy, book.requiredPlanCode, book.priceDisplay) {
                describeBookAccessSummary(book)
            }

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_book_card_${book.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (book.subtitle.isNotBlank()) {
                                Text(
                                    text = book.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Author: ${book.authorOrSource} • Lang: ${book.originalLanguage} • Pages: ${book.totalPages} • Work ID: ${book.workId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = when (book.publicationStatus) {
                                BookPublicationStatus.PUBLISHED -> MaterialTheme.colorScheme.secondaryContainer
                                BookPublicationStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = book.publicationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Access Policy: ${book.accessPolicy} • Search Status: ${book.searchIndexStatus}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = accessSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Category: ${book.category} • Genres: ${book.genresCsv} • Tags: ${book.tagsCsv} • ISBN: ${book.isbnOrIdentifier.ifBlank { "—" }} • Edition: ${book.editionLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingBook = book },
                            modifier = Modifier.testTag("superadmin_edit_book_${book.id}")
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit Book", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Metadata & Access")
                        }

                        if (book.publicationStatus != BookPublicationStatus.PUBLISHED) {
                            OutlinedButton(
                                onClick = {
                                    onSetBookPublicationStatus(
                                        book.id,
                                        BookPublicationStatus.PUBLISHED,
                                        "Published book #${book.id}"
                                    )
                                },
                                modifier = Modifier.testTag("superadmin_publish_book_${book.id}")
                            ) {
                                Text("Publish")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onSetBookPublicationStatus(
                                        book.id,
                                        BookPublicationStatus.UNPUBLISHED,
                                        "Unpublished book #${book.id}"
                                    )
                                },
                                modifier = Modifier.testTag("superadmin_unpublish_book_${book.id}")
                            ) {
                                Text("Unpublish")
                            }
                        }

                        if (book.publicationStatus != BookPublicationStatus.ARCHIVED) {
                            OutlinedButton(
                                onClick = {
                                    onSetBookPublicationStatus(
                                        book.id,
                                        BookPublicationStatus.ARCHIVED,
                                        "Archived book #${book.id} preserving OCR and translations"
                                    )
                                },
                                modifier = Modifier.testTag("superadmin_archive_book_${book.id}")
                            ) {
                                Text("Archive")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrEditBookDialog(
            initialBook = null,
            onDismiss = { showCreateDialog = false },
            onCreateNew = { title, subtitle, author, contributors, publisher, desc, lang, cat, genres, tags, workId, edition, isbn, pubYear, pubInfo, badgeHex, coverUrl, policy, reqPlan, rightsCat, pubStatus, priceDisp, samplePara, reason ->
                onCreateBook(
                    title, subtitle, author, contributors, publisher, desc, lang, cat, genres, tags,
                    workId, edition, isbn, pubYear, pubInfo, badgeHex, coverUrl, policy, reqPlan,
                    rightsCat, pubStatus, priceDisp, samplePara, reason
                )
                showCreateDialog = false
            },
            onSaveExisting = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }

    editingBook?.let { bookToEdit ->
        CreateOrEditBookDialog(
            initialBook = bookToEdit,
            onDismiss = { editingBook = null },
            onCreateNew = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onSaveExisting = { bookId, title, subtitle, author, contributors, publisher, desc, lang, cat, genres, tags, workId, edition, isbn, pubYear, pubInfo, badgeHex, coverUrl, customJson, searchIdx, pubStatus, policy, reqPlan, rightsCat, priceDisp, reason ->
                onUpdateBookMetadataAndAccess(
                    bookId, title, subtitle, author, contributors, publisher, desc, lang, cat, genres, tags,
                    workId, edition, isbn, pubYear, pubInfo, badgeHex, coverUrl, customJson, searchIdx,
                    pubStatus, policy, reqPlan, rightsCat, priceDisp, reason
                )
                editingBook = null
            }
        )
    }
}

private fun describeBookAccessSummary(book: BookEntity): String {
    if (book.publicationStatus != BookPublicationStatus.PUBLISHED) {
        return "Hidden from public directory (${book.publicationStatus}). Accessible only to Superadmin & authorized editors."
    }
    return when (book.accessPolicy) {
        BookAccessPolicy.PUBLIC -> "Public: Discoverable and readable by everyone (including logged-out guests)."
        BookAccessPolicy.FREE -> "Free Registered Users: Discoverable in directory; requires free signed-in account."
        BookAccessPolicy.PAID -> "Paid Subscribers: Hidden from public guests; accessible to active paid subscribers."
        BookAccessPolicy.SPECIFIC_PLAN_ONLY -> "Specific Plan Only (${book.requiredPlanCode.ifBlank { "PRO_RESEARCH" }}): Accessible only to users on the required plan."
        BookAccessPolicy.INDIVIDUAL_PURCHASE -> "Individual Purchase (${book.priceDisplay}): Requires per-book purchase entitlement."
        BookAccessPolicy.INSTITUTION_ONLY -> "Institution Only: Restricted to verified university/organisation members."
        BookAccessPolicy.PUBLISHER_ONLY -> "Publisher Controlled: Restricted to authorized publisher role."
        BookAccessPolicy.PRIVATE -> "Private: Hidden from public directory; accessible only via explicit admin grant."
        else -> "Governed by backend entitlement policy (${book.accessPolicy})."
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateOrEditBookDialog(
    initialBook: BookEntity?,
    onDismiss: () -> Unit,
    onCreateNew: (
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
    ) -> Unit,
    onSaveExisting: (
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
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialBook?.title ?: "") }
    var subtitle by rememberSaveable { mutableStateOf(initialBook?.subtitle ?: "") }
    var author by rememberSaveable { mutableStateOf(initialBook?.authorOrSource ?: "") }
    var contributors by rememberSaveable { mutableStateOf(initialBook?.contributors ?: "") }
    var publisher by rememberSaveable { mutableStateOf(initialBook?.publisherName ?: "VidyaKosha Press") }
    var description by rememberSaveable { mutableStateOf(initialBook?.description ?: "") }
    var originalLanguage by rememberSaveable { mutableStateOf(initialBook?.originalLanguage ?: "Sanskrit") }
    var category by rememberSaveable { mutableStateOf(initialBook?.category ?: "Classical Wisdom") }
    var genresCsv by rememberSaveable { mutableStateOf(initialBook?.genresCsv ?: "Philosophy") }
    var tagsCsv by rememberSaveable { mutableStateOf(initialBook?.tagsCsv ?: "Sanskrit, Canonical") }
    var workId by rememberSaveable { mutableStateOf(initialBook?.workId ?: "WORK-NEW") }
    var editionLabel by rememberSaveable { mutableStateOf(initialBook?.editionLabel ?: "1st Critical Edition") }
    var isbn by rememberSaveable { mutableStateOf(initialBook?.isbnOrIdentifier ?: "") }
    var publicationYear by rememberSaveable { mutableStateOf(initialBook?.publicationYear ?: "2026") }
    var publicationInfo by rememberSaveable { mutableStateOf(initialBook?.publicationInfo ?: "") }
    var coverImageUrl by rememberSaveable { mutableStateOf(initialBook?.coverImageUrl ?: "") }
    var customMetadataJson by rememberSaveable { mutableStateOf(initialBook?.customMetadataJson ?: "{}") }
    var searchIndexStatus by rememberSaveable { mutableStateOf(initialBook?.searchIndexStatus ?: "INDEXED") }
    var accessPolicy by rememberSaveable { mutableStateOf(initialBook?.accessPolicy ?: BookAccessPolicy.PUBLIC) }
    var publicationStatus by rememberSaveable { mutableStateOf(initialBook?.publicationStatus ?: BookPublicationStatus.PUBLISHED) }
    var requiredPlanCode by rememberSaveable { mutableStateOf(initialBook?.requiredPlanCode ?: "") }
    var priceDisplay by rememberSaveable { mutableStateOf(initialBook?.priceDisplay ?: "Free") }
    var sampleParagraph by rememberSaveable { mutableStateOf("सत्यमेव जयते नानृतं सत्येन पन्था विततो देवयानः।") }
    var reason by rememberSaveable { mutableStateOf(if (initialBook == null) "Created book via Superadmin console" else "Updated book metadata and access policy") }

    val policies = listOf(
        BookAccessPolicy.PUBLIC,
        BookAccessPolicy.FREE,
        BookAccessPolicy.PAID,
        BookAccessPolicy.SPECIFIC_PLAN_ONLY,
        BookAccessPolicy.INDIVIDUAL_PURCHASE,
        BookAccessPolicy.INSTITUTION_ONLY,
        BookAccessPolicy.PUBLISHER_ONLY,
        BookAccessPolicy.PRIVATE
    )

    val statuses = listOf(
        BookPublicationStatus.PUBLISHED,
        BookPublicationStatus.DRAFT,
        BookPublicationStatus.UNPUBLISHED,
        BookPublicationStatus.ARCHIVED
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBook == null) "Add New Book / Title" else "Edit Book #${initialBook.id}") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("book_editor_title_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("Author") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = originalLanguage,
                            onValueChange = { originalLanguage = it },
                            label = { Text("Original Language") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = contributors,
                        onValueChange = { contributors = it },
                        label = { Text("Contributors (Translators, Editors)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = publisher,
                            onValueChange = { publisher = it },
                            label = { Text("Publisher") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = publicationYear,
                            onValueChange = { publicationYear = it },
                            label = { Text("Pub Year") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = workId,
                            onValueChange = { workId = it },
                            label = { Text("Work ID (Canonical)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editionLabel,
                            onValueChange = { editionLabel = it },
                            label = { Text("Edition Info") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = isbn,
                            onValueChange = { isbn = it },
                            label = { Text("ISBN / Identifier") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = coverImageUrl,
                            onValueChange = { coverImageUrl = it },
                            label = { Text("Cover Image URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tagsCsv,
                            onValueChange = { tagsCsv = it },
                            label = { Text("Tags CSV") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = genresCsv,
                        onValueChange = { genresCsv = it },
                        label = { Text("Genres CSV") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = publicationInfo,
                        onValueChange = { publicationInfo = it },
                        label = { Text("Publication Info") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (initialBook != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customMetadataJson,
                                onValueChange = { customMetadataJson = it },
                                label = { Text("Metadata JSON") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = searchIndexStatus,
                                onValueChange = { searchIndexStatus = it },
                                label = { Text("Search Status") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = sampleParagraph,
                            onValueChange = { sampleParagraph = it },
                            label = { Text("Initial Sample OCR Passage") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item {
                    Text("Publication Status:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        statuses.forEach { st ->
                            FilterChip(
                                selected = publicationStatus == st,
                                onClick = { publicationStatus = st },
                                label = { Text(st) }
                            )
                        }
                    }
                }
                item {
                    Text("Access Policy:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        policies.forEach { pol ->
                            FilterChip(
                                selected = accessPolicy == pol,
                                onClick = { accessPolicy = pol },
                                label = { Text(pol) },
                                modifier = Modifier.testTag("book_editor_policy_$pol")
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = requiredPlanCode,
                            onValueChange = { requiredPlanCode = it.uppercase() },
                            label = { Text("Required Plan Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceDisplay,
                            onValueChange = { priceDisplay = it },
                            label = { Text("Price Display (e.g. $19)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Audit Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (initialBook == null) {
                        onCreateNew(
                            title,
                            subtitle,
                            author,
                            contributors,
                            publisher,
                            description,
                            originalLanguage,
                            category,
                            genresCsv,
                            tagsCsv,
                            workId,
                            editionLabel,
                            isbn,
                            publicationYear,
                            publicationInfo,
                            "#D97706",
                            coverImageUrl,
                            accessPolicy,
                            requiredPlanCode,
                            BookRightsCategory.PUBLIC_DOMAIN,
                            publicationStatus,
                            priceDisplay,
                            sampleParagraph,
                            reason
                        )
                    } else {
                        onSaveExisting(
                            initialBook.id,
                            title,
                            subtitle,
                            author,
                            contributors,
                            publisher,
                            description,
                            originalLanguage,
                            category,
                            genresCsv,
                            tagsCsv,
                            workId,
                            editionLabel,
                            isbn,
                            publicationYear,
                            publicationInfo,
                            initialBook.coverBadgeColorHex,
                            coverImageUrl,
                            customMetadataJson,
                            searchIndexStatus,
                            publicationStatus,
                            accessPolicy,
                            requiredPlanCode,
                            initialBook.rightsCategory,
                            priceDisplay,
                            reason
                        )
                    }
                },
                modifier = Modifier.testTag("book_editor_save_button")
            ) {
                Text("Save Book")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminTranslationsManagementPane(
    books: List<BookEntity>,
    translations: List<ParagraphTranslationEntity>,
    onApproveOrRejectTranslation: (Long, String, String) -> Unit,
    onSetPreferredTranslation: (Long, String) -> Unit,
    onUpdateTranslationAccessPolicy: (Long, String, String) -> Unit,
    onTriggerRetranslationVersion: (Long, String, String, String, String) -> Unit
) {
    var filterBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    var filterReviewStatus by rememberSaveable { mutableStateOf<String?>(null) }

    val reviewStatuses = listOf(
        TranslationReviewStatus.AI_GENERATED,
        TranslationReviewStatus.HUMAN_REVIEWED,
        TranslationReviewStatus.EXPERT_VERIFIED,
        TranslationReviewStatus.PUBLISHER_APPROVED,
        TranslationReviewStatus.REJECTED
    )

    val filteredTranslations = remember(translations, filterBookId, filterReviewStatus) {
        translations.filter { tr ->
            (filterBookId == null || tr.bookId == filterBookId) &&
                (filterReviewStatus == null || tr.reviewStatus == filterReviewStatus)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_translations_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Permanent Translation Repository Governance (${filteredTranslations.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Translations are permanent, versioned platform assets. Approving, preferring, or retranslating never deletes historical versions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Filter by Book:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = filterBookId == null,
                                onClick = { filterBookId = null },
                                label = { Text("All Books") }
                            )
                        }
                        items(books, key = { it.id }) { book ->
                            FilterChip(
                                selected = filterBookId == book.id,
                                onClick = { filterBookId = book.id },
                                label = { Text(book.title.take(24)) }
                            )
                        }
                    }

                    Text("Filter by Review Status:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterReviewStatus == null,
                            onClick = { filterReviewStatus = null },
                            label = { Text("All Statuses") }
                        )
                        reviewStatuses.forEach { st ->
                            FilterChip(
                                selected = filterReviewStatus == st,
                                onClick = { filterReviewStatus = st },
                                label = { Text(st) }
                            )
                        }
                    }
                }
            }
        }

        items(filteredTranslations, key = { it.id }) { tr ->
            val bookTitle = books.find { it.id == tr.bookId }?.title ?: "Book #${tr.bookId}"
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_translation_card_${tr.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$bookTitle • Paragraph #${tr.paragraphId}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (tr.isActiveVersion) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PREFERRED",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = tr.reviewStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Lang: ${tr.sourceLanguage} → ${tr.targetLanguage} • Version v${tr.versionNumber} • Access: ${tr.accessPolicy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Contributor: ${tr.translatorDisplayName} • AI Model: ${tr.providerUsed}/${tr.modelUsed} • Created: ${formatAdminTimestamp(tr.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tr.translatedText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onApproveOrRejectTranslation(
                                    tr.id,
                                    TranslationReviewStatus.PUBLISHER_APPROVED,
                                    "Approved by Superadmin"
                                )
                            },
                            modifier = Modifier.testTag("approve_translation_${tr.id}")
                        ) {
                            Text("Approve")
                        }
                        if (!tr.isActiveVersion) {
                            OutlinedButton(
                                onClick = {
                                    onSetPreferredTranslation(
                                        tr.id,
                                        "Marked preferred by Superadmin"
                                    )
                                },
                                modifier = Modifier.testTag("prefer_translation_${tr.id}")
                            ) {
                                Text("Mark Preferred")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onApproveOrRejectTranslation(
                                    tr.id,
                                    TranslationReviewStatus.REJECTED,
                                    "Rejected by Superadmin"
                                )
                            },
                            modifier = Modifier.testTag("reject_translation_${tr.id}")
                        ) {
                            Text("Reject")
                        }
                        OutlinedButton(
                            onClick = {
                                val nextPolicy = if (tr.accessPolicy == TranslationAccessPolicy.PAID_USERS) {
                                    TranslationAccessPolicy.PUBLIC
                                } else {
                                    TranslationAccessPolicy.PAID_USERS
                                }
                                onUpdateTranslationAccessPolicy(
                                    tr.id,
                                    nextPolicy,
                                    "Changed translation access policy to $nextPolicy"
                                )
                            }
                        ) {
                            Text(if (tr.accessPolicy == TranslationAccessPolicy.PAID_USERS) "Make Public" else "Restrict Paid")
                        }
                        OutlinedButton(
                            onClick = {
                                onTriggerRetranslationVersion(
                                    tr.id,
                                    "${tr.translatedText} [Superadmin Refined v${tr.versionNumber + 1}]",
                                    "Triggered versioned retranslation from Superadmin console",
                                    "Superadmin-Curated-Engine",
                                    "Superadmin triggered retranslation preserving historical version v${tr.versionNumber}"
                                )
                            },
                            modifier = Modifier.testTag("retranslate_btn_${tr.id}")
                        ) {
                            Text("Retranslate (+v${tr.versionNumber + 1})")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminMetadataManagementPane(
    metadataItems: List<PlatformMetadataEntity>,
    onUpsertPlatformMetadata: (
        metadataId: Long?,
        metadataType: String,
        keyCode: String,
        displayLabel: String,
        description: String,
        sortOrder: Int,
        isActive: Boolean,
        extraAttributesJson: String,
        reasonOrNote: String
    ) -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var keyCode by rememberSaveable { mutableStateOf("") }
    var displayLabel by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryInput by rememberSaveable { mutableStateOf(MetadataCategoryType.CATEGORY) }

    val categories = MetadataCategoryType.ALL

    val filteredItems = remember(metadataItems, selectedCategory) {
        if (selectedCategory == null) metadataItems else metadataItems.filter { it.metadataType == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_metadata_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reusable Platform Metadata Registry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Add or update platform-controlled enumerations (Languages, Categories, Genres, Tags, Statuses, Access Policies, Plan Features) without database restructuring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = categoryInput == cat,
                                onClick = { categoryInput = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = keyCode,
                            onValueChange = { keyCode = it },
                            label = { Text("Code (e.g. SANSKRIT_EPIC)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("metadata_code_input")
                        )
                        OutlinedTextField(
                            value = displayLabel,
                            onValueChange = { displayLabel = it },
                            label = { Text("Display Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("metadata_name_input")
                        )
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (keyCode.isNotBlank() && displayLabel.isNotBlank()) {
                                onUpsertPlatformMetadata(
                                    null,
                                    categoryInput,
                                    keyCode.trim(),
                                    displayLabel.trim(),
                                    description.trim(),
                                    filteredItems.size + 1,
                                    true,
                                    "{}",
                                    "Added metadata entry $categoryInput:$keyCode"
                                )
                                keyCode = ""
                                displayLabel = ""
                                description = ""
                            }
                        },
                        modifier = Modifier.testTag("superadmin_add_metadata_button")
                    ) {
                        Text("Save Metadata Entry")
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${metadataItems.size})") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[${item.metadataType}] ${item.displayLabel} (${item.keyCode})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = item.isActive,
                        onCheckedChange = { enabled ->
                            onUpsertPlatformMetadata(
                                item.id,
                                item.metadataType,
                                item.keyCode,
                                item.displayLabel,
                                item.description,
                                item.sortOrder,
                                enabled,
                                item.extraAttributesJson,
                                "Toggled metadata ${item.keyCode} active=$enabled"
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuperadminUsersManagementPane(
    users: List<UserEntity>,
    entitlements: List<UserEntitlementEntity>,
    translations: List<ParagraphTranslationEntity>,
    onSetUserAccountStatus: (Long, String, String) -> Unit,
    onUpdateUserRole: (Long, String, String) -> Unit,
    onResetUserPassword: ((Long, String, String) -> Unit)? = null
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var inspectedUserId by rememberSaveable { mutableStateOf<Long?>(null) }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users else {
            val q = searchQuery.trim().lowercase()
            users.filter {
                it.email.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.role.lowercase().contains(q)
            }
        }
    }

    val inspectedUser = remember(users, inspectedUserId) {
        users.find { it.id == inspectedUserId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_users_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search users by email, name, or role") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_user_search_input")
            )
        }

        inspectedUser?.let { detailUser ->
            val userEnts = entitlements.filter { it.userId == detailUser.id }
            val userTransCount = translations.count { it.createdByUserId == detailUser.id }
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "User Profile Inspector: ${detailUser.displayName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { inspectedUserId = null }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close inspector")
                            }
                        }
                        Text(
                            text = "Email: ${detailUser.email} • Role: ${detailUser.role} • Status: ${detailUser.accountStatus}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Active Plan/Tier: ${detailUser.activeTierCode} • Contributed Translations: $userTransCount • Top 3 Languages: ${detailUser.topThreeLanguagesCsv}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Entitlement History (${userEnts.size}): " +
                                userEnts.joinToString { "${it.entitlementKind}:${it.targetRef}(${it.status})" }.ifBlank { "None" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(filteredUsers, key = { it.id }) { user ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_user_card_${user.id}"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${user.displayName} (#${user.id})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${user.email} • Plan: ${user.activeTierCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.role,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = if (user.accountStatus == AccountStatus.ACTIVE) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = user.accountStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inspectedUserId = user.id },
                            modifier = Modifier.testTag("inspect_user_${user.id}")
                        ) {
                            Text("Inspect Profile")
                        }

                        if (user.accountStatus == AccountStatus.ACTIVE) {
                            OutlinedButton(
                                onClick = {
                                    onSetUserAccountStatus(
                                        user.id,
                                        AccountStatus.SUSPENDED,
                                        "Suspended account ${user.email} by Superadmin"
                                    )
                                },
                                modifier = Modifier.testTag("suspend_user_${user.id}")
                            ) {
                                Text("Suspend")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    onSetUserAccountStatus(
                                        user.id,
                                        AccountStatus.ACTIVE,
                                        "Reactivated account ${user.email} by Superadmin"
                                    )
                                },
                                modifier = Modifier.testTag("reactivate_user_${user.id}")
                            ) {
                                Text("Reactivate")
                            }
                        }

                        val nextRole = when (user.role) {
                            UserRole.USER -> UserRole.ADMIN
                            UserRole.ADMIN -> UserRole.SUPERADMIN
                            else -> UserRole.USER
                        }
                        OutlinedButton(
                            onClick = {
                                onUpdateUserRole(
                                    user.id,
                                    nextRole,
                                    "Changed role of ${user.email} to $nextRole"
                                )
                            },
                            modifier = Modifier.testTag("cycle_role_user_${user.id}")
                        ) {
                            Text("Set Role: $nextRole")
                        }

                        if (onResetUserPassword != null) {
                            OutlinedButton(
                                onClick = {
                                    onResetUserPassword(
                                        user.id,
                                        "test1234",
                                        "Reset password of ${user.email} to temporary password (test1234) by Superadmin"
                                    )
                                },
                                modifier = Modifier.testTag("reset_pwd_user_${user.id}")
                            ) {
                                Text("Reset Password (test1234)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperadminAuditTrailPane(
    auditLogs: List<AdminAuditLogEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("superadmin_audit_log_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Immutable Administrative Audit Trail (${auditLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Every plan update, book access change, user suspension, role assignment, translation approval, and metadata update is permanently logged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(auditLogs, key = { it.id }) { log ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("superadmin_audit_item_${log.id}"),
                shape = RoundedCornerShape(10.dp)
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
                            text = "${log.action} • ${log.entityAffected} (${log.entityId})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatAdminTimestamp(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Admin: ${log.adminEmailOrName} (#${log.adminUserId}, ${log.adminRole}) • Context: ${log.deviceOrContextInfo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (log.previousValue.isNotBlank()) {
                        Text(
                            text = "Previous: ${log.previousValue}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (log.newValue.isNotBlank()) {
                        Text(
                            text = "New: ${log.newValue}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (log.reasonOrNote.isNotBlank()) {
                        Text(
                            text = "Reason: ${log.reasonOrNote}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatAdminTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(epochMillis))
}
