package com.example.data.api

import com.example.data.local.AccountStatus
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.BookAccessPolicy
import com.example.data.local.BookEntity
import com.example.data.local.BookPageEntity
import com.example.data.local.BookPublicationStatus
import com.example.data.local.BookRightsCategory
import com.example.data.local.ChapterEntity
import com.example.data.local.CommercialPlanEntity
import com.example.data.local.EntitlementKind
import com.example.data.local.ParagraphEntity
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.PlatformMetadataEntity
import com.example.data.local.PolyglotDao
import com.example.data.local.SectionEntity
import com.example.data.local.SubscriptionTier
import com.example.data.local.SuperadminDashboardOverview
import com.example.data.local.TranslationMemoryEntity
import com.example.data.local.TranslationReviewStatus
import com.example.data.local.UserBookAccessEntity
import com.example.data.local.UserEntitlementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.util.SecurityVault

/**
 * Backend-First Superadmin & Administrative API Service.
 *
 * Security & Governance Guarantees:
 * 1. Every administrative endpoint enforces strict backend RBAC (`requireSuperadmin`).
 *    Unauthorized calls from Guests, Users, or ordinary Admins are rejected AND recorded in the immutable audit trail.
 * 2. Commercial plans, pricing, feature limits, book access policies, entitlements, translations, and metadata
 *    are 100% database-backed without hardcoded rules.
 * 3. Updating book metadata or access policy NEVER destroys existing OCR pages, chapters, sections, paragraphs,
 *    translations, or user reading progress.
 * 4. Translation governance preserves all historical translation versions; retranslations create a new incremented version.
 * 5. Every administrative action writes an append-only `AdminAuditLogEntity` record.
 */
class SuperadminBackendApi(
    private val dao: PolyglotDao,
    private val accessApi: AccessApi
) {

    private suspend fun requireSuperadmin(
        adminUserId: Long,
        attemptedAction: String,
        targetEntity: String,
        targetEntityId: String
    ): UserEntity {
        val user = if (adminUserId > 0L) dao.getUserById(adminUserId) else null
        if (user == null || !user.role.equals(UserRole.SUPERADMIN, ignoreCase = true) || user.accountStatus != AccountStatus.ACTIVE) {
            dao.insertAdminAuditLog(
                AdminAuditLogEntity(
                    adminUserId = user?.id ?: 0L,
                    adminEmailOrName = user?.email ?: "unauthenticated-caller",
                    adminRole = user?.role ?: "guest",
                    action = "UNAUTHORIZED_SUPERADMIN_ATTEMPT",
                    entityAffected = targetEntity,
                    entityId = targetEntityId,
                    previousValue = "Attempted action: $attemptedAction",
                    newValue = "DENIED (403 Forbidden)",
                    reasonOrNote = "Caller does not hold active superadmin role",
                    deviceOrContextInfo = "Backend RBAC Guard"
                )
            )
            throw SecurityException("403 Forbidden: Superadmin role required for '$attemptedAction'.")
        }
        return user
    }

    private suspend fun recordAudit(
        admin: UserEntity,
        action: String,
        entityAffected: String,
        entityId: String,
        previousValue: String,
        newValue: String,
        reasonOrNote: String
    ) {
        dao.insertAdminAuditLog(
            AdminAuditLogEntity(
                adminUserId = admin.id,
                adminEmailOrName = "${admin.displayName} <${admin.email}>",
                adminRole = admin.role,
                action = action,
                entityAffected = entityAffected,
                entityId = entityId,
                previousValue = previousValue,
                newValue = newValue,
                reasonOrNote = reasonOrNote.ifBlank { "Superadmin console configuration update" },
                deviceOrContextInfo = "VidyaKosha Superadmin Console • Verified Session"
            )
        )
    }

    // =====================================================================
    // 0. DASHBOARD OVERVIEW AGGREGATOR (Requirement #2)
    // =====================================================================

    fun buildDashboardOverview(
        users: List<UserEntity>,
        entitlements: List<UserEntitlementEntity>,
        books: List<BookEntity>,
        translations: List<ParagraphTranslationEntity>,
        auditLogs: List<AdminAuditLogEntity>
    ): SuperadminDashboardOverview {
        val now = System.currentTimeMillis()
        val activeEntitlements = entitlements.filter {
            it.status == "ACTIVE" && (it.expiresAt == null || it.expiresAt > now)
        }
        val activeSubscriptionEntitlements = activeEntitlements.filter {
            it.entitlementKind == EntitlementKind.SUBSCRIPTION_TIER &&
                !it.targetRef.equals(SubscriptionTier.FREE, ignoreCase = true)
        }
        val paidUserIds = activeEntitlements
            .filter {
                (it.entitlementKind == EntitlementKind.SUBSCRIPTION_TIER &&
                    !it.targetRef.equals(SubscriptionTier.FREE, ignoreCase = true)) ||
                    it.entitlementKind == EntitlementKind.BOOK_PURCHASE ||
                    it.entitlementKind == EntitlementKind.INSTITUTION_MEMBERSHIP ||
                    it.entitlementKind == EntitlementKind.PUBLISHER_ROLE
            }
            .map { it.userId }
            .toSet()

        val totalUsers = users.size
        val paidUsersCount = users.count { it.id in paidUserIds }
        val freeUsersCount = (totalUsers - paidUsersCount).coerceAtLeast(0)

        val publishedBooksCount = books.count { it.publicationStatus.equals(BookPublicationStatus.PUBLISHED, ignoreCase = true) }
        val draftOrUnpublishedCount = books.count {
            it.publicationStatus.equals(BookPublicationStatus.DRAFT, ignoreCase = true) ||
                it.publicationStatus.equals(BookPublicationStatus.UNPUBLISHED, ignoreCase = true)
        }
        val archivedBooksCount = books.count { it.publicationStatus.equals(BookPublicationStatus.ARCHIVED, ignoreCase = true) }

        val booksByOriginalLanguage = books
            .groupBy { it.originalLanguage.ifBlank { "Unknown" } }
            .mapValues { it.value.size }

        val translationsByLanguage = translations
            .groupBy { it.targetLanguage.ifBlank { "Unknown" } }
            .mapValues { it.value.size }

        val suspendedUsersCount = users.count { it.accountStatus.equals(AccountStatus.SUSPENDED, ignoreCase = true) }

        return SuperadminDashboardOverview(
            totalUsers = totalUsers,
            activeSubscriptionsCount = activeSubscriptionEntitlements.size,
            freeUsersCount = freeUsersCount,
            paidUsersCount = paidUsersCount,
            suspendedUsersCount = suspendedUsersCount,
            totalBooksCount = books.size,
            publishedBooksCount = publishedBooksCount,
            draftOrUnpublishedBooksCount = draftOrUnpublishedCount,
            archivedBooksCount = archivedBooksCount,
            booksByOriginalLanguage = booksByOriginalLanguage,
            totalTranslationsCount = translations.size,
            translationsByLanguage = translationsByLanguage,
            recentRegistrations = users.sortedByDescending { it.createdAt }.take(6),
            recentTranslations = translations.sortedByDescending { it.createdAt }.take(8),
            recentAuditLogs = auditLogs.sortedByDescending { it.timestamp }.take(10)
        )
    }

    // =====================================================================
    // 1. COMMERCIAL PLAN & PRICING MANAGEMENT (Requirement #3)
    // =====================================================================

    suspend fun saveCommercialPlan(
        adminUserId: Long,
        existingPlanId: Long?,
        planCode: String,
        name: String,
        description: String,
        isEnabled: Boolean,
        monthlyPriceAmount: Double,
        yearlyPriceAmount: Double,
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
    ): Result<CommercialPlanEntity> = runCatching {
        val normalizedCode = planCode.trim().uppercase()
        val admin = requireSuperadmin(adminUserId, "SAVE_COMMERCIAL_PLAN", "CommercialPlan", normalizedCode)
        require(normalizedCode.isNotBlank()) { "Plan code cannot be empty." }
        require(name.isNotBlank()) { "Plan name cannot be empty." }
        require(monthlyPriceAmount >= 0.0 && yearlyPriceAmount >= 0.0) { "Prices cannot be negative." }

        val existing = existingPlanId?.let { dao.getCommercialPlanById(it) }
            ?: dao.getCommercialPlanByCode(normalizedCode)
        val now = System.currentTimeMillis()

        val entity = CommercialPlanEntity(
            id = existing?.id ?: 0L,
            planCode = normalizedCode,
            name = name.trim(),
            description = description.trim(),
            isEnabled = isEnabled,
            monthlyPriceAmount = monthlyPriceAmount,
            yearlyPriceAmount = yearlyPriceAmount,
            currencyCode = currencyCode.trim().uppercase().ifBlank { "USD" },
            trialDays = trialDays.coerceAtLeast(0),
            displayOrder = displayOrder,
            isFeatured = isFeatured,
            featuresCsv = featuresCsv.trim(),
            allowedBookAccessPoliciesCsv = allowedBookAccessPoliciesCsv.trim(),
            dailyTranslationLimit = dailyTranslationLimit,
            monthlyManagedAiTokensLimit = monthlyManagedAiTokensLimit,
            offlineAccessEnabled = offlineAccessEnabled,
            advancedSearchAndScholarlyEnabled = advancedSearchAndScholarlyEnabled,
            ttsAudioDailyMinutesLimit = ttsAudioDailyMinutesLimit,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        val savedId = if (existing == null) {
            dao.insertCommercialPlan(entity)
        } else {
            dao.updateCommercialPlan(entity)
            existing.id
        }
        val saved = dao.getCommercialPlanById(savedId) ?: entity.copy(id = savedId)

        recordAudit(
            admin = admin,
            action = if (existing == null) "PLAN_CREATED" else "PLAN_SAVED",
            entityAffected = "CommercialPlan",
            entityId = saved.planCode,
            previousValue = existing?.let { "${it.name} (${it.formatPriceSummary()}, enabled=${it.isEnabled})" } ?: "NONE",
            newValue = "${saved.name} (${saved.formatPriceSummary()}, enabled=${saved.isEnabled}, limits=${saved.dailyTranslationLimit}/day)",
            reasonOrNote = reasonOrNote
        )
        saved
    }

    suspend fun toggleCommercialPlanEnabled(
        adminUserId: Long,
        planId: Long,
        enabled: Boolean,
        reasonOrNote: String
    ): Result<CommercialPlanEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "TOGGLE_COMMERCIAL_PLAN", "CommercialPlan", planId.toString())
        val existing = dao.getCommercialPlanById(planId)
            ?: throw IllegalArgumentException("Commercial plan #$planId not found.")
        val updated = existing.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis())
        dao.updateCommercialPlan(updated)

        recordAudit(
            admin = admin,
            action = if (enabled) "PLAN_ENABLED" else "PLAN_DISABLED",
            entityAffected = "CommercialPlan",
            entityId = existing.planCode,
            previousValue = "isEnabled=${existing.isEnabled}",
            newValue = "isEnabled=$enabled",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    // =====================================================================
    // 2. CENTRAL ACCESS & ENTITLEMENT MANAGEMENT (Requirement #4)
    // =====================================================================

    suspend fun grantUserEntitlement(
        adminUserId: Long,
        targetUserId: Long,
        entitlementKind: String,
        targetRef: String,
        displayTitle: String,
        durationDays: Int,
        reasonOrNote: String
    ): Result<UserEntitlementEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "GRANT_ENTITLEMENT", "UserEntitlement", "user:$targetUserId")
        val targetUser = dao.getUserById(targetUserId)
            ?: throw IllegalArgumentException("Target user #$targetUserId not found.")
        val now = System.currentTimeMillis()
        val expiresAt = if (durationDays > 0) {
            now + durationDays * 24L * 60L * 60L * 1000L
        } else {
            now + 100L * 365L * 24L * 60L * 60L * 1000L
        }

        val normalizedKind = entitlementKind.trim().uppercase()
        val normalizedRef = targetRef.trim()

        // If granting individual book purchase access, also ensure UserBookAccessEntity row exists
        if (normalizedKind == EntitlementKind.BOOK_PURCHASE) {
            val bookId = normalizedRef.toLongOrNull()
            if (bookId != null && dao.getUserAccessForBook(targetUserId, bookId) == null) {
                dao.insertUserBookAccess(
                    UserBookAccessEntity(
                        userId = targetUserId,
                        bookId = bookId,
                        accessType = "PURCHASED"
                    )
                )
            }
        }

        val entitlement = UserEntitlementEntity(
            userId = targetUserId,
            entitlementKind = normalizedKind,
            targetRef = normalizedRef,
            displayTitle = displayTitle.trim().ifBlank { "$normalizedKind: $normalizedRef" },
            status = "ACTIVE",
            grantedByAdminId = admin.id,
            grantedByAdminName = admin.displayName,
            reasonOrNote = reasonOrNote.trim().ifBlank { "Admin-granted access" },
            grantedAt = now,
            expiresAt = expiresAt
        )
        val id = dao.insertEntitlement(entitlement)
        val saved = entitlement.copy(id = id)

        recordAudit(
            admin = admin,
            action = "ENTITLEMENT_GRANTED",
            entityAffected = "UserEntitlement",
            entityId = "entitlement:$id",
            previousValue = "NONE",
            newValue = "User=${targetUser.email}, Kind=$normalizedKind, Ref=$normalizedRef, Expires=$expiresAt",
            reasonOrNote = reasonOrNote
        )
        saved
    }

    suspend fun revokeUserEntitlement(
        adminUserId: Long,
        entitlementId: Long,
        reasonOrNote: String
    ): Result<UserEntitlementEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "REVOKE_ENTITLEMENT", "UserEntitlement", entitlementId.toString())
        val existing = dao.getEntitlementById(entitlementId)
            ?: throw IllegalArgumentException("Entitlement #$entitlementId not found.")
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = "REVOKED",
            revokedAt = now,
            reasonOrNote = if (reasonOrNote.isNotBlank()) "${existing.reasonOrNote} | Revoked: $reasonOrNote" else existing.reasonOrNote
        )
        dao.updateEntitlement(updated)

        recordAudit(
            admin = admin,
            action = "ENTITLEMENT_REVOKED",
            entityAffected = "UserEntitlement",
            entityId = "entitlement:$entitlementId",
            previousValue = "status=${existing.status} (${existing.entitlementKind}:${existing.targetRef})",
            newValue = "status=REVOKED",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    suspend fun extendUserEntitlement(
        adminUserId: Long,
        entitlementId: Long,
        additionalDays: Int,
        reasonOrNote: String
    ): Result<UserEntitlementEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "EXTEND_ENTITLEMENT", "UserEntitlement", entitlementId.toString())
        require(additionalDays > 0) { "Additional days must be positive." }
        val existing = dao.getEntitlementById(entitlementId)
            ?: throw IllegalArgumentException("Entitlement #$entitlementId not found.")
        val now = System.currentTimeMillis()
        val baseTime = existing.expiresAt.takeIf { it > now } ?: now
        val newExpiry = baseTime + additionalDays * 24L * 60L * 60L * 1000L
        val updated = existing.copy(
            status = "ACTIVE",
            expiresAt = newExpiry,
            revokedAt = null
        )
        dao.updateEntitlement(updated)

        recordAudit(
            admin = admin,
            action = "ENTITLEMENT_EXTENDED",
            entityAffected = "UserEntitlement",
            entityId = "entitlement:$entitlementId",
            previousValue = "expiresAt=${existing.expiresAt}, status=${existing.status}",
            newValue = "expiresAt=$newExpiry (+$additionalDays days), status=ACTIVE",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    // =====================================================================
    // 3. BOOK, TITLE & ACCESS CONFIGURATION MANAGEMENT (Requirements #5 & #6)
    // =====================================================================

    suspend fun createBookByAdmin(
        adminUserId: Long,
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
    ): Result<BookEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "CREATE_BOOK", "Book", title)
        require(title.isNotBlank()) { "Book title is required." }

        val normalizedPolicy = accessPolicy.trim().uppercase().ifBlank { BookAccessPolicy.PUBLIC }
        val normalizedPubStatus = publicationStatus.trim().uppercase().ifBlank { BookPublicationStatus.PUBLISHED }
        val isPublicDirectoryVisible = normalizedPubStatus == BookPublicationStatus.PUBLISHED &&
            normalizedPolicy != BookAccessPolicy.PRIVATE
        val isPaidBook = normalizedPolicy in setOf(
            BookAccessPolicy.PAID,
            BookAccessPolicy.SPECIFIC_PLAN_ONLY,
            BookAccessPolicy.INDIVIDUAL_PURCHASE,
            BookAccessPolicy.INSTITUTION_ONLY,
            BookAccessPolicy.RESTRICTED,
            BookAccessPolicy.PUBLISHER_ONLY
        )

        val contentFingerprint = SecurityVault.computeContentFingerprint("$title|$authorOrSource|$workId|$editionLabel")
        val now = System.currentTimeMillis()

        val newBook = BookEntity(
            uploadedByUserId = admin.id,
            title = title.trim(),
            subtitle = subtitle.trim(),
            authorOrSource = authorOrSource.trim().ifBlank { "Classical Archive" },
            contributors = contributors.trim(),
            description = description.trim(),
            originalLanguage = originalLanguage.trim().ifBlank { "Sanskrit" },
            category = category.trim().ifBlank { "Philosophy & Wisdom" },
            genresCsv = genresCsv.trim().ifBlank { "Philosophy, Classical Literature" },
            tagsCsv = tagsCsv.trim().ifBlank { "Bilingual, Canonical" },
            coverImageUrl = coverImageUrl.trim(),
            workId = workId.trim().ifBlank { "WORK-${System.currentTimeMillis() % 100000}" },
            editionLabel = editionLabel.trim().ifBlank { "1st Critical Edition" },
            isbnOrIdentifier = isbnOrIdentifier.trim(),
            publicationYear = publicationYear.trim().ifBlank { "2026" },
            publicationInfo = publicationInfo.trim().ifBlank { publisherName.trim().ifBlank { "VidyaKosha Press" } },
            customMetadataJson = "{}",
            publicationStatus = normalizedPubStatus,
            searchIndexStatus = if (normalizedPubStatus == BookPublicationStatus.PUBLISHED) "INDEXED" else "NOT_INDEXED",
            requiredPlanCode = requiredPlanCode.trim().uppercase(),
            isPublicCatalog = isPublicDirectoryVisible,
            isSampleBook = false,
            isPaid = isPaidBook,
            accessPolicy = normalizedPolicy,
            rightsCategory = rightsCategory.trim().uppercase().ifBlank { BookRightsCategory.PUBLIC_DOMAIN },
            publisherName = publisherName.trim().ifBlank { "VidyaKosha Archival Press" },
            priceDisplay = priceDisplay.trim().ifBlank { if (isPaidBook) "$9.99" else "Free" },
            coverBadgeColorHex = coverBadgeColorHex.trim().ifBlank { "#37474F" },
            sourcePdfPath = "ADMIN_CATALOG_WORK://${workId.ifBlank { "EDITION" }}",
            contentHash = contentFingerprint,
            fileSizeBytes = 0L,
            totalPages = 1,
            ocrStatus = "COMPLETED",
            ocrProgressPages = 1,
            totalChapters = 1,
            totalSections = 1,
            totalParagraphs = 1,
            createdAt = now,
            lastReadAt = now
        )

        val bookId = dao.insertBook(newBook)
        val initialText = initialSampleParagraphText.trim().ifBlank {
            "अथातो ब्रह्मजिज्ञासा। जन्माद्यस्य यतः॥ — Opening canonical passage of ${title.trim()}."
        }
        dao.insertBookPages(
            listOf(
                BookPageEntity(
                    bookId = bookId,
                    pageNumber = 1,
                    rawOcrText = initialText,
                    detectedSourceLanguage = newBook.originalLanguage,
                    ocrConfidenceNote = "Cataloged by Superadmin (${admin.displayName})"
                )
            )
        )
        val chId = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: Opening Text",
                summary = newBook.description.ifBlank { "Opening chapter of ${newBook.title}" },
                startPage = 1,
                endPage = 1
            )
        )
        val secId = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = chId,
                sectionOrder = 1,
                heading = "Section 1.1: Canonical Passage",
                startPage = 1,
                endPage = 1
            )
        )
        dao.insertParagraphs(
            listOf(
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 1,
                    originalPageNumber = 1,
                    blockType = "VERSE",
                    rawOcrText = initialText,
                    originalText = initialText,
                    originalLanguage = newBook.originalLanguage,
                    normalizedSearchText = initialText.lowercase(),
                    ocrVersion = 1,
                    sourceContentHash = SecurityVault.computeContentFingerprint(initialText)
                )
            )
        )

        val savedBook = dao.getBookById(bookId) ?: newBook.copy(id = bookId)
        recordAudit(
            admin = admin,
            action = "BOOK_CREATED",
            entityAffected = "Book",
            entityId = "book:$bookId",
            previousValue = "NONE",
            newValue = "Title='${savedBook.title}', Work='${savedBook.workId}', Status=${savedBook.publicationStatus}, Access=${savedBook.accessPolicy}",
            reasonOrNote = reasonOrNote
        )
        savedBook
    }

    /**
     * Updates book metadata, work/edition identifiers, publication status, search indexing status,
     * and access policy WITHOUT touching or deleting any existing OCR pages, chapters, sections,
     * paragraphs, translations, or reading progress.
     */
    suspend fun updateBookMetadataAndAccess(
        adminUserId: Long,
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
    ): Result<BookEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "UPDATE_BOOK_METADATA_AND_ACCESS", "Book", bookId.toString())
        val existing = dao.getBookById(bookId)
            ?: throw IllegalArgumentException("Book #$bookId not found.")

        val normalizedPolicy = accessPolicy.trim().uppercase().ifBlank { existing.accessPolicy }
        val normalizedPubStatus = publicationStatus.trim().uppercase().ifBlank { existing.publicationStatus }
        val isPublicDirectoryVisible = normalizedPubStatus == BookPublicationStatus.PUBLISHED &&
            normalizedPolicy != BookAccessPolicy.PRIVATE
        val isPaidBook = normalizedPolicy in setOf(
            BookAccessPolicy.PAID,
            BookAccessPolicy.SPECIFIC_PLAN_ONLY,
            BookAccessPolicy.INDIVIDUAL_PURCHASE,
            BookAccessPolicy.INSTITUTION_ONLY,
            BookAccessPolicy.RESTRICTED,
            BookAccessPolicy.PUBLISHER_ONLY
        )

        val updated = existing.copy(
            title = title.trim().ifBlank { existing.title },
            subtitle = subtitle.trim(),
            authorOrSource = authorOrSource.trim().ifBlank { existing.authorOrSource },
            contributors = contributors.trim(),
            publisherName = publisherName.trim().ifBlank { existing.publisherName },
            description = description.trim(),
            originalLanguage = originalLanguage.trim().ifBlank { existing.originalLanguage },
            category = category.trim().ifBlank { existing.category },
            genresCsv = genresCsv.trim(),
            tagsCsv = tagsCsv.trim(),
            workId = workId.trim().ifBlank { existing.workId },
            editionLabel = editionLabel.trim().ifBlank { existing.editionLabel },
            isbnOrIdentifier = isbnOrIdentifier.trim(),
            publicationYear = publicationYear.trim(),
            publicationInfo = publicationInfo.trim(),
            coverBadgeColorHex = coverBadgeColorHex.trim().ifBlank { existing.coverBadgeColorHex },
            coverImageUrl = coverImageUrl.trim(),
            customMetadataJson = customMetadataJson.trim().ifBlank { "{}" },
            searchIndexStatus = searchIndexStatus.trim().uppercase().ifBlank { existing.searchIndexStatus },
            publicationStatus = normalizedPubStatus,
            accessPolicy = normalizedPolicy,
            requiredPlanCode = requiredPlanCode.trim().uppercase(),
            rightsCategory = rightsCategory.trim().uppercase().ifBlank { existing.rightsCategory },
            priceDisplay = priceDisplay.trim().ifBlank { existing.priceDisplay },
            isPublicCatalog = isPublicDirectoryVisible,
            isPaid = isPaidBook
        )
        dao.updateBook(updated)

        recordAudit(
            admin = admin,
            action = "BOOK_METADATA_AND_ACCESS_UPDATED",
            entityAffected = "Book",
            entityId = "book:$bookId",
            previousValue = "Title='${existing.title}', Status=${existing.publicationStatus}, Access=${existing.accessPolicy}, Plan=${existing.requiredPlanCode}",
            newValue = "Title='${updated.title}', Status=${updated.publicationStatus}, Access=${updated.accessPolicy}, Plan=${updated.requiredPlanCode}",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    suspend fun setBookPublicationStatus(
        adminUserId: Long,
        bookId: Long,
        newPublicationStatus: String,
        reasonOrNote: String
    ): Result<BookEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "SET_BOOK_PUBLICATION_STATUS", "Book", bookId.toString())
        val existing = dao.getBookById(bookId)
            ?: throw IllegalArgumentException("Book #$bookId not found.")
        val normalizedStatus = newPublicationStatus.trim().uppercase()
        val isPublicDirectoryVisible = normalizedStatus == BookPublicationStatus.PUBLISHED &&
            existing.accessPolicy != BookAccessPolicy.PRIVATE
        val updated = existing.copy(
            publicationStatus = normalizedStatus,
            isPublicCatalog = isPublicDirectoryVisible,
            searchIndexStatus = if (normalizedStatus == BookPublicationStatus.PUBLISHED) "INDEXED" else "NOT_INDEXED"
        )
        dao.updateBook(updated)

        recordAudit(
            admin = admin,
            action = "BOOK_PUBLICATION_STATUS_CHANGED",
            entityAffected = "Book",
            entityId = "book:$bookId",
            previousValue = "publicationStatus=${existing.publicationStatus}",
            newValue = "publicationStatus=$normalizedStatus",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    // =====================================================================
    // 4. PERMANENT TRANSLATION REPOSITORY GOVERNANCE (Requirement #7)
    // =====================================================================

    suspend fun approveOrRejectTranslation(
        adminUserId: Long,
        translationId: Long,
        newReviewStatus: String,
        reasonOrNote: String
    ): Result<ParagraphTranslationEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "REVIEW_TRANSLATION", "ParagraphTranslation", translationId.toString())
        val existing = dao.getTranslationById(translationId)
            ?: throw IllegalArgumentException("Translation #$translationId not found.")
        val normalizedStatus = newReviewStatus.trim().uppercase()
        dao.updateTranslationReviewStatus(
            translationId = translationId,
            newStatus = normalizedStatus,
            reviewerName = "${admin.displayName} (Superadmin)"
        )
        val updated = dao.getTranslationById(translationId) ?: existing.copy(reviewStatus = normalizedStatus)

        recordAudit(
            admin = admin,
            action = "TRANSLATION_REVIEW_STATUS_CHANGED",
            entityAffected = "ParagraphTranslation",
            entityId = "translation:$translationId",
            previousValue = "reviewStatus=${existing.reviewStatus}",
            newValue = "reviewStatus=$normalizedStatus",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    suspend fun setPreferredTranslationByAdmin(
        adminUserId: Long,
        translationId: Long,
        reasonOrNote: String
    ): Result<ParagraphTranslationEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "SET_PREFERRED_TRANSLATION", "ParagraphTranslation", translationId.toString())
        val existing = dao.getTranslationById(translationId)
            ?: throw IllegalArgumentException("Translation #$translationId not found.")

        // Deactivate active flag on other versions of the same paragraph+language WITHOUT deleting them
        dao.deactivateOldTranslationsForParagraph(existing.paragraphId, existing.targetLanguage)
        dao.markTranslationAsPreferred(translationId)
        val updated = dao.getTranslationById(translationId) ?: existing.copy(isActiveVersion = true)

        recordAudit(
            admin = admin,
            action = "TRANSLATION_MARKED_PREFERRED",
            entityAffected = "ParagraphTranslation",
            entityId = "translation:$translationId",
            previousValue = "isActiveVersion=${existing.isActiveVersion} (Paragraph #${existing.paragraphId}, ${existing.targetLanguage})",
            newValue = "isActiveVersion=true (v${updated.versionNumber} preferred; historical versions preserved)",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    suspend fun updateTranslationAccessPolicyByAdmin(
        adminUserId: Long,
        translationId: Long,
        newAccessPolicy: String,
        reasonOrNote: String
    ): Result<ParagraphTranslationEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "UPDATE_TRANSLATION_ACCESS_POLICY", "ParagraphTranslation", translationId.toString())
        val existing = dao.getTranslationById(translationId)
            ?: throw IllegalArgumentException("Translation #$translationId not found.")
        val normalizedPolicy = newAccessPolicy.trim().uppercase()
        dao.updateTranslationAccessPolicy(translationId, normalizedPolicy)
        val updated = dao.getTranslationById(translationId) ?: existing.copy(accessPolicy = normalizedPolicy)

        recordAudit(
            admin = admin,
            action = "TRANSLATION_ACCESS_POLICY_CHANGED",
            entityAffected = "ParagraphTranslation",
            entityId = "translation:$translationId",
            previousValue = "accessPolicy=${existing.accessPolicy}",
            newValue = "accessPolicy=$normalizedPolicy",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    /**
     * Never deletes historical translations! Creates a new versioned translation row (v+1),
     * marks it preferred, and preserves all earlier versions in `paragraph_translations`.
     */
    suspend fun triggerAdminRetranslationVersion(
        adminUserId: Long,
        translationId: Long,
        newTranslatedText: String,
        newNotes: String,
        modelUsed: String,
        reasonOrNote: String
    ): Result<ParagraphTranslationEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "TRIGGER_RETRANSLATION_VERSION", "ParagraphTranslation", translationId.toString())
        val existing = dao.getTranslationById(translationId)
            ?: throw IllegalArgumentException("Translation #$translationId not found.")
        val allVersions = dao.getTranslationVersionsForParagraph(existing.paragraphId, existing.targetLanguage)
        val nextVersion = (allVersions.maxOfOrNull { it.versionNumber } ?: existing.versionNumber) + 1
        val now = System.currentTimeMillis()

        dao.deactivateOldTranslationsForParagraph(existing.paragraphId, existing.targetLanguage)

        val refinedText = newTranslatedText.trim().ifBlank {
            "${existing.translatedText} [Superadmin Revised Edition v$nextVersion]"
        }
        val newTranslation = existing.copy(
            id = 0L,
            translatedText = refinedText,
            transliterationOrNotes = newNotes.trim().ifBlank { existing.transliterationOrNotes },
            versionNumber = nextVersion,
            isActiveVersion = true,
            reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
            reviewedByDisplayName = "${admin.displayName} (Superadmin)",
            createdByUserId = admin.id,
            translatorDisplayName = "${admin.displayName} (Superadmin Editorial)",
            providerUsed = "SUPERADMIN_EDITORIAL",
            modelUsed = modelUsed.trim().ifBlank { "Superadmin-Curated-v$nextVersion" },
            createdAt = now,
            updatedAt = now
        )
        val newId = dao.insertTranslation(newTranslation)
        val saved = dao.getTranslationById(newId) ?: newTranslation.copy(id = newId)

        // Update Translation Memory with the newly curated translation
        val paragraph = dao.getParagraphById(existing.paragraphId)
        if (paragraph != null) {
            dao.insertTranslationMemory(
                TranslationMemoryEntity(
                    normalizedSourceHash = paragraph.sourceContentHash,
                    sourceLanguage = existing.sourceLanguage,
                    targetLanguage = existing.targetLanguage,
                    canonicalSourceText = paragraph.originalText,
                    translatedText = refinedText,
                    originBookId = existing.bookId,
                    originParagraphId = existing.paragraphId,
                    originTranslationId = saved.id,
                    providerUsed = saved.providerUsed,
                    modelUsed = saved.modelUsed,
                    reviewStatus = saved.reviewStatus,
                    updatedAt = now
                )
            )
        }

        recordAudit(
            admin = admin,
            action = "TRANSLATION_RETRANSLATE_TRIGGERED",
            entityAffected = "ParagraphTranslation",
            entityId = "translation:$newId",
            previousValue = "Prior translation #${existing.id} (v${existing.versionNumber}) preserved in history",
            newValue = "Created new preferred translation #$newId (v$nextVersion) in ${existing.targetLanguage}",
            reasonOrNote = reasonOrNote
        )
        saved
    }

    // =====================================================================
    // 5. REUSABLE PLATFORM METADATA MANAGEMENT (Requirement #8)
    // =====================================================================

    suspend fun upsertPlatformMetadata(
        adminUserId: Long,
        metadataId: Long?,
        metadataType: String,
        keyCode: String,
        displayLabel: String,
        description: String,
        sortOrder: Int,
        isActive: Boolean,
        extraAttributesJson: String,
        reasonOrNote: String
    ): Result<PlatformMetadataEntity> = runCatching {
        val normType = metadataType.trim().uppercase()
        val normKey = keyCode.trim().uppercase()
        val admin = requireSuperadmin(adminUserId, "UPSERT_PLATFORM_METADATA", "PlatformMetadata", "$normType:$normKey")
        require(normType.isNotBlank() && normKey.isNotBlank() && displayLabel.isNotBlank()) {
            "Metadata type, keyCode, and displayLabel are required."
        }

        val existing = metadataId?.let { dao.getPlatformMetadataById(it) }
            ?: dao.getPlatformMetadataByKey(normType, normKey)
        val now = System.currentTimeMillis()

        val entity = PlatformMetadataEntity(
            id = existing?.id ?: 0L,
            metadataType = normType,
            keyCode = normKey,
            displayLabel = displayLabel.trim(),
            description = description.trim(),
            sortOrder = sortOrder,
            isActive = isActive,
            extraAttributesJson = extraAttributesJson.trim().ifBlank { "{}" },
            updatedAt = now
        )
        val id = if (existing == null) {
            dao.insertPlatformMetadata(entity)
        } else {
            dao.updatePlatformMetadata(entity)
            existing.id
        }
        val saved = dao.getPlatformMetadataById(id) ?: entity.copy(id = id)

        recordAudit(
            admin = admin,
            action = if (existing == null) "METADATA_CREATED" else "METADATA_UPDATED",
            entityAffected = "PlatformMetadata",
            entityId = "${saved.metadataType}:${saved.keyCode}",
            previousValue = existing?.let { "${it.displayLabel} (active=${it.isActive})" } ?: "NONE",
            newValue = "${saved.displayLabel} (active=${saved.isActive}, order=${saved.sortOrder})",
            reasonOrNote = reasonOrNote
        )
        saved
    }

    // =====================================================================
    // 6. USER & ROLE MANAGEMENT (Requirement #9)
    // =====================================================================

    suspend fun setUserAccountStatus(
        adminUserId: Long,
        targetUserId: Long,
        newStatus: String,
        reasonOrNote: String
    ): Result<UserEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "SET_USER_ACCOUNT_STATUS", "User", targetUserId.toString())
        val targetUser = dao.getUserById(targetUserId)
            ?: throw IllegalArgumentException("User #$targetUserId not found.")
        val normalizedStatus = newStatus.trim().uppercase()
        if (targetUser.id == admin.id && normalizedStatus == AccountStatus.SUSPENDED) {
            throw IllegalArgumentException("Superadmin cannot suspend their own active session account.")
        }

        val updated = targetUser.copy(accountStatus = normalizedStatus)
        dao.updateUser(updated)
        if (normalizedStatus == AccountStatus.SUSPENDED) {
            dao.clearSessionsForUser(targetUserId)
        }

        recordAudit(
            admin = admin,
            action = if (normalizedStatus == AccountStatus.SUSPENDED) "USER_SUSPENDED" else "USER_REACTIVATED",
            entityAffected = "User",
            entityId = "user:$targetUserId",
            previousValue = "accountStatus=${targetUser.accountStatus}",
            newValue = "accountStatus=$normalizedStatus",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    /**
     * Enforces strict privilege escalation rules:
     * - Only an authenticated `superadmin` can call this endpoint (`requireSuperadmin`).
     * - Ordinary admins can NEVER escalate themselves or anyone else to `superadmin`.
     */
    suspend fun updateUserRole(
        adminUserId: Long,
        targetUserId: Long,
        newRole: String,
        reasonOrNote: String
    ): Result<UserEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "UPDATE_USER_ROLE", "User", targetUserId.toString())
        val targetUser = dao.getUserById(targetUserId)
            ?: throw IllegalArgumentException("User #$targetUserId not found.")
        val normalizedRole = newRole.trim().lowercase()
        require(normalizedRole in UserRole.ALL) {
            "Invalid role '$newRole'."
        }
        if (targetUser.id == admin.id && normalizedRole != UserRole.SUPERADMIN) {
            throw IllegalArgumentException("Primary Superadmin cannot demote their own active Superadmin role.")
        }

        val updated = targetUser.copy(role = normalizedRole)
        dao.updateUser(updated)

        recordAudit(
            admin = admin,
            action = "USER_ROLE_CHANGED",
            entityAffected = "User",
            entityId = "user:$targetUserId",
            previousValue = "role=${targetUser.role}",
            newValue = "role=$normalizedRole",
            reasonOrNote = reasonOrNote
        )
        updated
    }

    /**
     * Allows a Superadmin to directly reset any user's password (e.g., to a temporary recovery password)
     * and invalidates all active sessions for that user, recording an immutable audit log entry.
     */
    suspend fun resetUserPasswordByAdmin(
        adminUserId: Long,
        targetUserId: Long,
        newPassword: String,
        reasonOrNote: String
    ): Result<UserEntity> = runCatching {
        val admin = requireSuperadmin(adminUserId, "RESET_USER_PASSWORD_BY_ADMIN", "User", targetUserId.toString())
        val targetUser = dao.getUserById(targetUserId)
            ?: throw IllegalArgumentException("User #$targetUserId not found.")
        val cleanPwd = newPassword.trim()
        require(cleanPwd.length >= 6) {
            "New password must be at least 6 characters long."
        }

        val newSalt = SecurityVault.generateSalt()
        val newHash = SecurityVault.hashPassword(cleanPwd, newSalt)
        val updated = targetUser.copy(
            passwordSalt = newSalt,
            passwordHash = newHash
        )
        dao.updateUser(updated)
        dao.clearSessionsForUser(targetUserId)

        recordAudit(
            admin = admin,
            action = "ADMIN_USER_PASSWORD_RESET",
            entityAffected = "User",
            entityId = "user:$targetUserId (${targetUser.email})",
            previousValue = "passwordHash=<redacted>, activeSessions=cleared",
            newValue = "passwordHash=<updated sha256>, temporaryPasswordSet=true",
            reasonOrNote = reasonOrNote.ifBlank { "Password reset by Superadmin for ${targetUser.email}" }
        )
        updated
    }
}
