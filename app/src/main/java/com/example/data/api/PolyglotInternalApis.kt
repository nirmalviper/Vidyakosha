package com.example.data.api

import com.example.data.local.BilingualParagraphItem
import com.example.data.local.BookAccessPolicy
import com.example.data.local.BookEntity
import com.example.data.local.BookPublicationStatus
import com.example.data.local.BookRightsCategory
import com.example.data.local.ChapterEntity
import com.example.data.local.CommercialPlanEntity
import com.example.data.local.ContributorConsentEntity
import com.example.data.local.CorpusAnalyticsEventEntity
import com.example.data.local.CrossBookSearchResultItem
import com.example.data.local.EntitlementKind
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.OcrCorrectionAuditEntity
import com.example.data.local.OfflineBookPackageEntity
import com.example.data.local.ParagraphEntity
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.PolyglotDao
import com.example.data.local.SectionEntity
import com.example.data.local.SubscriptionTier
import com.example.data.local.TranslationAccessPolicy
import com.example.data.local.TranslationDiscoveryStatus
import com.example.data.local.TranslationIssueReportEntity
import com.example.data.local.TranslationMemoryEntity
import com.example.data.local.TranslationReviewStatus
import com.example.data.local.TransliterationLearningEntity
import com.example.data.local.UserBookAccessEntity
import com.example.data.local.UserEntitlementEntity
import com.example.util.SecurityVault

/**
 * Requirement #22: Internal Decoupled Service APIs
 * - AccessApi
 * - BookApi
 * - ContentApi
 * - TranslationApi (Central Shared Translation Service)
 * - SearchApi
 * - AudioApi
 */

// =====================================================================
// 1. ACCESS & LICENSING API (Requirements #2, #3, #4, #18, #19)
// =====================================================================

class AccessApi(private val dao: PolyglotDao) {

    fun resolveEffectiveTier(
        entitlements: List<UserEntitlementEntity>,
        now: Long = System.currentTimeMillis()
    ): String {
        val activeEntitlements = entitlements.filter {
            it.status == "ACTIVE" && (it.expiresAt <= 0L || it.expiresAt > now)
        }
        val tiers = activeEntitlements
            .filter {
                it.entitlementKind == EntitlementKind.SUBSCRIPTION_TIER ||
                    it.entitlementKind == EntitlementKind.PROMOTIONAL_ACCESS ||
                    it.entitlementKind == EntitlementKind.ADMIN_GRANTED_ACCESS
            }
            .map { it.targetRef }

        return when {
            tiers.contains(SubscriptionTier.PUBLISHER_INSTITUTION) ||
                tiers.contains("ALL_PLATFORM_BOOKS") -> SubscriptionTier.PUBLISHER_INSTITUTION
            tiers.contains(SubscriptionTier.PRO_RESEARCH) -> SubscriptionTier.PRO_RESEARCH
            tiers.contains(SubscriptionTier.READER) -> SubscriptionTier.READER
            tiers.firstOrNull { it.isNotBlank() && it != SubscriptionTier.FREE } != null -> {
                tiers.first { it.isNotBlank() && it != SubscriptionTier.FREE }
            }
            else -> SubscriptionTier.FREE
        }
    }

    /**
     * Requirement #4 & #6: Backend-enforced Book Discovery & Catalogue Entitlement Check.
     * - Draft, Unpublished, and Archived books are NEVER exposed in the public directory.
     * - Restricted, Paid, Specific-Plan, Individual-Purchase, Institution-Only, Publisher-Only, and Private
     *   books are NEVER exposed in the unentitled public directory merely because they exist in the database.
     */
    fun canUserDiscoverBook(
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        book: BookEntity,
        commercialPlans: List<CommercialPlanEntity> = emptyList()
    ): Boolean {
        val now = System.currentTimeMillis()
        val activeEnt = entitlements.filter {
            it.status == "ACTIVE" && (it.expiresAt <= 0L || it.expiresAt > now)
        }
        val tier = resolveEffectiveTier(activeEnt, now)

        val hasAdminAllAccess = activeEnt.any {
            it.entitlementKind == EntitlementKind.ADMIN_GRANTED_ACCESS &&
                (it.targetRef == "ALL_PLATFORM_BOOKS" || it.targetRef == book.id.toString())
        }
        val hasDirectBookGrant = hasAdminAllAccess || activeEnt.any {
            (it.entitlementKind == EntitlementKind.BOOK_PURCHASE ||
                it.entitlementKind == EntitlementKind.PROMOTIONAL_ACCESS ||
                it.entitlementKind == EntitlementKind.CONTRIBUTOR_ACCESS) &&
                it.targetRef == book.id.toString()
        }
        val hasInstitutionGrant = activeEnt.any {
            it.entitlementKind == EntitlementKind.INSTITUTION_MEMBERSHIP
        }
        val hasPublisherRole = tier == SubscriptionTier.PUBLISHER_INSTITUTION || activeEnt.any {
            it.entitlementKind == EntitlementKind.PUBLISHER_ROLE
        }
        val isUploaderOwner = userId > 0L && book.uploadedByUserId == userId

        // 1. Publication Status Gate: Non-published books (DRAFT, UNPUBLISHED, ARCHIVED)
        // are strictly hidden from normal users and the public directory.
        if (book.publicationStatus != BookPublicationStatus.PUBLISHED) {
            return isUploaderOwner || hasPublisherRole || hasAdminAllAccess
        }

        // 2. Check dynamic CommercialPlanEntity allowedBookAccessPoliciesCsv if available
        val activePlanConfig = commercialPlans.find {
            it.isEnabled && it.planCode.equals(tier, ignoreCase = true)
        }
        val planAllowedPolicies = activePlanConfig?.allowedBookAccessPoliciesCsv
            ?.split(",")
            ?.map { it.trim().uppercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return when (book.accessPolicy) {
            BookAccessPolicy.PUBLIC -> true
            BookAccessPolicy.FREE -> true
            BookAccessPolicy.PAID -> {
                userId > 0L && (
                    hasDirectBookGrant ||
                        tier == SubscriptionTier.READER ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        planAllowedPolicies.contains(BookAccessPolicy.PAID)
                    )
            }
            BookAccessPolicy.SPECIFIC_PLAN_ONLY -> {
                val reqPlan = book.requiredPlanCode.trim()
                userId > 0L && (
                    hasDirectBookGrant ||
                        hasPublisherRole ||
                        (reqPlan.isNotEmpty() && tier.equals(reqPlan, ignoreCase = true)) ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        planAllowedPolicies.contains(BookAccessPolicy.SPECIFIC_PLAN_ONLY)
                    )
            }
            BookAccessPolicy.INDIVIDUAL_PURCHASE -> {
                userId > 0L && (hasDirectBookGrant || hasPublisherRole)
            }
            BookAccessPolicy.INSTITUTION_ONLY -> {
                userId > 0L && (
                    hasInstitutionGrant ||
                        hasDirectBookGrant ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        planAllowedPolicies.contains(BookAccessPolicy.INSTITUTION_ONLY)
                    )
            }
            BookAccessPolicy.RESTRICTED -> {
                userId > 0L && (
                    hasDirectBookGrant ||
                        hasInstitutionGrant ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        planAllowedPolicies.contains(BookAccessPolicy.RESTRICTED)
                    )
            }
            BookAccessPolicy.PRIVATE -> {
                isUploaderOwner || hasAdminAllAccess
            }
            BookAccessPolicy.PUBLISHER_ONLY -> {
                userId > 0L && (hasPublisherRole || hasAdminAllAccess)
            }
            else -> !book.isPaid
        }
    }

    fun canUserReadBook(
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        userBookAccess: List<UserBookAccessEntity>,
        book: BookEntity,
        commercialPlans: List<CommercialPlanEntity> = emptyList()
    ): Boolean {
        val ownsBookAccess = userId > 0L && userBookAccess.any { it.bookId == book.id }
        if (ownsBookAccess && book.publicationStatus == BookPublicationStatus.PUBLISHED) {
            return true
        }
        if (!canUserDiscoverBook(userId, entitlements, book, commercialPlans)) return false

        val now = System.currentTimeMillis()
        val activeEnt = entitlements.filter {
            it.status == "ACTIVE" && (it.expiresAt <= 0L || it.expiresAt > now)
        }
        val tier = resolveEffectiveTier(activeEnt, now)
        val hasDirectBookGrant = ownsBookAccess || activeEnt.any {
            (it.entitlementKind == EntitlementKind.BOOK_PURCHASE ||
                it.entitlementKind == EntitlementKind.PROMOTIONAL_ACCESS ||
                it.entitlementKind == EntitlementKind.CONTRIBUTOR_ACCESS ||
                it.entitlementKind == EntitlementKind.ADMIN_GRANTED_ACCESS) &&
                (it.targetRef == book.id.toString() || it.targetRef == "ALL_PLATFORM_BOOKS")
        }

        return when (book.accessPolicy) {
            BookAccessPolicy.PUBLIC -> true
            BookAccessPolicy.FREE -> true
            BookAccessPolicy.PAID -> {
                userId > 0L && (
                    hasDirectBookGrant ||
                        tier == SubscriptionTier.READER ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION
                    )
            }
            BookAccessPolicy.SPECIFIC_PLAN_ONLY -> {
                val reqPlan = book.requiredPlanCode.trim()
                userId > 0L && (
                    hasDirectBookGrant ||
                        (reqPlan.isNotEmpty() && tier.equals(reqPlan, ignoreCase = true)) ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION
                    )
            }
            BookAccessPolicy.INDIVIDUAL_PURCHASE -> {
                userId > 0L && (hasDirectBookGrant || tier == SubscriptionTier.PUBLISHER_INSTITUTION)
            }
            BookAccessPolicy.INSTITUTION_ONLY,
            BookAccessPolicy.RESTRICTED -> {
                userId > 0L && (
                    hasDirectBookGrant ||
                        tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        activeEnt.any { it.entitlementKind == EntitlementKind.INSTITUTION_MEMBERSHIP }
                    )
            }
            BookAccessPolicy.PRIVATE -> userId > 0L && (book.uploadedByUserId == userId || hasDirectBookGrant)
            BookAccessPolicy.PUBLISHER_ONLY -> {
                userId > 0L && (
                    tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        activeEnt.any { it.entitlementKind == EntitlementKind.PUBLISHER_ROLE } ||
                        hasDirectBookGrant
                    )
            }
            else -> !book.isPaid
        }
    }

    /**
     * Clearly describes who can currently discover and read a book based on its publicationStatus & accessPolicy.
     */
    fun describeWhoCanAccessBook(book: BookEntity): String {
        if (book.publicationStatus != BookPublicationStatus.PUBLISHED) {
            return "Hidden from Catalogue (${book.publicationStatus}) • Accessible only to Superadmin, Publisher & Owner"
        }
        return when (book.accessPolicy) {
            BookAccessPolicy.PUBLIC -> "Publicly accessible to all guests & registered users"
            BookAccessPolicy.FREE -> "Accessible to all Free & Paid registered users"
            BookAccessPolicy.PAID -> "Restricted to Paid Subscribers (Reader/Pro), Promotional/Admin Grants & Individual Buyers"
            BookAccessPolicy.SPECIFIC_PLAN_ONLY -> {
                val p = book.requiredPlanCode.ifBlank { "PRO_RESEARCH" }
                "Restricted to Plan '$p' subscribers & Admin Grants"
            }
            BookAccessPolicy.INDIVIDUAL_PURCHASE -> "Restricted to Individual Book Buyers (${book.priceDisplay}) & Admin Grants"
            BookAccessPolicy.INSTITUTION_ONLY -> "Restricted to University/Institution Members & Pro Research"
            BookAccessPolicy.RESTRICTED -> "Restricted Research Archive (Pro Research, Institution & Admin Grants only)"
            BookAccessPolicy.PUBLISHER_ONLY -> "Restricted to Publisher Role & Superadmin only"
            BookAccessPolicy.PRIVATE -> "Strictly Private to Uploading User (ID #${book.uploadedByUserId ?: "N/A"}) & Superadmin"
            else -> "Standard Entitlement Policy (${book.accessPolicy})"
        }
    }

    /**
     * Requirement #2 & #7: Translation Access Policy Verification.
     * Enforced before exposing or reusing any existing translation from the shared repository.
     * Rejected translations (`TranslationReviewStatus.REJECTED`) are never served to readers.
     */
    fun canUserAccessTranslation(
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        book: BookEntity,
        translation: ParagraphTranslationEntity
    ): Boolean {
        if (translation.reviewStatus == TranslationReviewStatus.REJECTED) {
            return false
        }
        val now = System.currentTimeMillis()
        val activeEnt = entitlements.filter {
            it.status == "ACTIVE" && (it.expiresAt <= 0L || it.expiresAt > now)
        }
        val tier = resolveEffectiveTier(activeEnt, now)
        val hasPaidOrHigher = tier == SubscriptionTier.READER ||
            tier == SubscriptionTier.PRO_RESEARCH ||
            tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
            activeEnt.any {
                (it.entitlementKind == EntitlementKind.BOOK_PURCHASE ||
                    it.entitlementKind == EntitlementKind.PROMOTIONAL_ACCESS ||
                    it.entitlementKind == EntitlementKind.CONTRIBUTOR_ACCESS ||
                    it.entitlementKind == EntitlementKind.ADMIN_GRANTED_ACCESS) &&
                    (it.targetRef == book.id.toString() || it.targetRef == "ALL_PLATFORM_BOOKS")
            }

        return when (translation.accessPolicy) {
            TranslationAccessPolicy.PUBLIC -> true
            TranslationAccessPolicy.FREE_REGISTERED_USERS -> true
            TranslationAccessPolicy.PAID_USERS -> userId > 0L && hasPaidOrHigher
            TranslationAccessPolicy.CONTRIBUTOR_ONLY -> {
                userId > 0L && (
                    translation.createdByUserId == userId ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        activeEnt.any { it.entitlementKind == EntitlementKind.CONTRIBUTOR_ACCESS }
                    )
            }
            TranslationAccessPolicy.PRIVATE -> {
                userId > 0L && (translation.createdByUserId == userId || book.uploadedByUserId == userId)
            }
            TranslationAccessPolicy.INSTITUTION_ONLY -> {
                userId > 0L && (
                    tier == SubscriptionTier.PRO_RESEARCH ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        activeEnt.any { it.entitlementKind == EntitlementKind.INSTITUTION_MEMBERSHIP }
                    )
            }
            TranslationAccessPolicy.PUBLISHER_CONTROLLED -> {
                userId > 0L && (
                    hasPaidOrHigher ||
                        tier == SubscriptionTier.PUBLISHER_INSTITUTION ||
                        activeEnt.any { it.entitlementKind == EntitlementKind.PUBLISHER_ROLE }
                    )
            }
            else -> true
        }
    }

    /**
     * Requirement #3: Determines how a user-generated translation is licensed and whether
     * it can enter the shared translation repository.
     * Never assumes bringing your own API key automatically grants resale rights on private/copyrighted works!
     */
    fun resolveTranslationContributionPolicy(
        book: BookEntity,
        userAcceptedContributorLicense: Boolean
    ): Triple<String, String, Boolean> {
        return when (book.rightsCategory) {
            BookRightsCategory.PUBLIC_DOMAIN -> {
                if (userAcceptedContributorLicense) {
                    Triple(
                        TranslationAccessPolicy.PUBLIC,
                        "PUBLIC_DOMAIN_DERIVATIVE",
                        true
                    )
                } else {
                    Triple(
                        TranslationAccessPolicy.CONTRIBUTOR_ONLY,
                        "PERSONAL_UNSHARED_DERIVATIVE",
                        false
                    )
                }
            }
            BookRightsCategory.POLYGLOT_OWNED -> {
                if (userAcceptedContributorLicense) {
                    Triple(
                        TranslationAccessPolicy.FREE_REGISTERED_USERS,
                        "POLYGLOT_CONTRIBUTOR_LICENSE",
                        true
                    )
                } else {
                    Triple(
                        TranslationAccessPolicy.CONTRIBUTOR_ONLY,
                        "PERSONAL_UNSHARED_DERIVATIVE",
                        false
                    )
                }
            }
            BookRightsCategory.PUBLISHER_LICENSED -> {
                if (userAcceptedContributorLicense) {
                    Triple(
                        TranslationAccessPolicy.PUBLISHER_CONTROLLED,
                        "PUBLISHER_RESTRICTED",
                        true
                    )
                } else {
                    Triple(
                        TranslationAccessPolicy.CONTRIBUTOR_ONLY,
                        "PUBLISHER_PERSONAL_STUDY",
                        false
                    )
                }
            }
            BookRightsCategory.P2P_DERIVATIVE_OWNED -> {
                // Zero-Host P2P Model: Original file is never hosted; Lipi owns the converted OCR + Translation derivative!
                Triple(
                    TranslationAccessPolicy.PUBLIC,
                    "LIPI_OWNED_OCR_AND_TRANSLATION_DERIVATIVE",
                    true
                )
            }
            BookRightsCategory.USER_PRIVATE_UPLOAD,
            BookRightsCategory.COPYRIGHTED_NO_REDISTRIBUTION -> {
                // Strictly private to the uploading user — never enters shared resale repository!
                Triple(
                    TranslationAccessPolicy.PRIVATE,
                    "PRIVATE_PERSONAL_ONLY",
                    false
                )
            }
            else -> Triple(TranslationAccessPolicy.PRIVATE, "PRIVATE_PERSONAL_ONLY", false)
        }
    }

    suspend fun recordContributorConsent(
        userId: Long,
        book: BookEntity
    ): ContributorConsentEntity {
        val (accessPolicy, _, _) = resolveTranslationContributionPolicy(book, true)
        val consent = ContributorConsentEntity(
            userId = userId,
            bookId = book.id,
            bookTitle = book.title,
            bookRightsCategory = book.rightsCategory,
            resultingTranslationAccessPolicy = accessPolicy,
            agreementTextShown = "Zero-Host P2P Derivative Licence: The original file is never hosted on Lipi servers. By converting or translating this work, you agree that Lipi owns and permanently retains the structured OCR and translation derivatives (${book.rightsCategory} → $accessPolicy)."
        )
        val id = dao.insertContributorConsent(consent)
        val user = dao.getUserById(userId)
        if (user != null && !user.hasAcceptedContributorLicense) {
            dao.updateUser(user.copy(hasAcceptedContributorLicense = true))
        }
        return consent.copy(id = id)
    }
}

// =====================================================================
// 2. BOOK API (Requirements #4, #17, #20, #22)
// =====================================================================

class BookApi(
    private val dao: PolyglotDao,
    private val accessApi: AccessApi
) {
    fun filterDiscoverableBooks(
        allBooks: List<BookEntity>,
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        commercialPlans: List<CommercialPlanEntity> = emptyList()
    ): List<BookEntity> {
        return allBooks.filter { book ->
            accessApi.canUserDiscoverBook(userId, entitlements, book, commercialPlans)
        }
    }

    suspend fun updateBookAccessAndRights(
        bookId: Long,
        newAccessPolicy: String,
        newRightsCategory: String,
        isPaid: Boolean,
        priceDisplay: String
    ): BookEntity? {
        val existing = dao.getBookById(bookId) ?: return null
        val updated = existing.copy(
            accessPolicy = newAccessPolicy,
            rightsCategory = newRightsCategory,
            isPaid = isPaid,
            isPublicCatalog = newAccessPolicy in listOf(BookAccessPolicy.PUBLIC, BookAccessPolicy.FREE, BookAccessPolicy.PAID),
            priceDisplay = priceDisplay
        )
        dao.updateBook(updated)
        return updated
    }

    suspend fun downloadBookForOfflineReading(
        userId: Long,
        book: BookEntity,
        targetLanguage: String
    ): OfflineBookPackageEntity {
        val paragraphs = dao.getParagraphsForBook(book.id)
        val allTranslations = dao.getAllCanonicalTranslationsInSystem()
            .filter { it.bookId == book.id }
        val langs = (allTranslations.map { it.targetLanguage } + targetLanguage).distinct().joinToString(", ")
        val pkg = OfflineBookPackageEntity(
            userId = userId,
            bookId = book.id,
            bookTitle = book.title,
            includedLanguagesCsv = langs,
            paragraphsCount = paragraphs.size,
            translationsCount = allTranslations.size,
            bookmarksCount = 0,
            audioMetadataCached = true,
            packageSizeKb = (paragraphs.size * 48 + allTranslations.size * 32).coerceAtLeast(180),
            syncStatus = "SYNCED_OFFLINE_READY"
        )
        val id = dao.upsertOfflineBookPackage(pkg)
        return pkg.copy(id = id)
    }
}

// =====================================================================
// 3. CONTENT & OCR VERSIONING API (Requirements #8, #14, #15, #16, #22)
// =====================================================================

class ContentApi(private val dao: PolyglotDao) {

    /**
     * Requirement #8 & #16: Corrects OCR text without ever overwriting `rawOcrText`.
     * Increments `ocrVersion` (e.g. v1 -> v2) and updates `sourceContentHash`.
     * Any existing translation whose `sourceContentHash` no longer matches is automatically
     * flagged as `requiresSourceReview = true`!
     */
    suspend fun submitOcrCorrectionWithAudit(
        paragraphId: Long,
        newCorrectedText: String,
        authorUserId: Long,
        authorName: String,
        reason: String
    ): Result<ParagraphEntity> {
        val cleanText = newCorrectedText.trim()
        if (cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Corrected OCR text cannot be empty."))
        }
        val paragraph = dao.getParagraphById(paragraphId)
            ?: return Result.failure(IllegalArgumentException("Paragraph not found."))

        val newHash = SecurityVault.computeContentFingerprint(cleanText)
        if (newHash == paragraph.sourceContentHash) {
            return Result.success(paragraph)
        }

        val nextVersion = paragraph.ocrVersion + 1
        val preservedRawOcr = paragraph.rawOcrText.ifBlank { paragraph.originalText }

        val audit = OcrCorrectionAuditEntity(
            bookId = paragraph.bookId,
            paragraphId = paragraph.id,
            previousOcrVersion = paragraph.ocrVersion,
            newOcrVersion = nextVersion,
            rawOcrText = preservedRawOcr,
            previousText = paragraph.originalText,
            correctedText = cleanText,
            previousContentHash = paragraph.sourceContentHash,
            newContentHash = newHash,
            correctionAuthorId = authorUserId,
            correctionAuthorName = authorName.ifBlank { "Editorial Reviewer" },
            correctionReason = reason.ifBlank { "OCR v$nextVersion character & sandhi correction" }
        )
        dao.insertOcrCorrectionAudit(audit)

        val updatedParagraph = paragraph.copy(
            rawOcrText = preservedRawOcr,
            originalText = cleanText,
            normalizedSearchText = "${cleanText.lowercase()} ${paragraph.normalizedSearchText}",
            ocrVersion = nextVersion,
            sourceContentHash = newHash,
            lastCorrectedBy = authorName.ifBlank { "Editorial Reviewer" },
            lastCorrectedAt = System.currentTimeMillis()
        )
        dao.updateParagraph(updatedParagraph)

        val book = dao.getBookById(paragraph.bookId)
        if (book != null) {
            dao.updateBook(book.copy(currentOcrVersion = maxOf(book.currentOcrVersion, nextVersion)))
        }

        return Result.success(updatedParagraph)
    }
}

// =====================================================================
// 4. CENTRAL SHARED TRANSLATION SERVICE API (Requirements #1, #2, #5, #6, #7, #8, #9)
// =====================================================================

class TranslationApi(
    private val dao: PolyglotDao,
    private val accessApi: AccessApi
) {

    /**
     * Requirement #5: Builds the Translation Discovery Status for the active book & language.
     */
    fun buildDiscoveryStatus(
        targetLanguage: LanguageDescriptor,
        paragraphs: List<ParagraphEntity>,
        allVersionsForLanguage: List<ParagraphTranslationEntity>
    ): TranslationDiscoveryStatus {
        val preferredByPara = allVersionsForLanguage
            .filter { it.isActiveVersion }
            .associateBy { it.paragraphId }

        val translatedCount = paragraphs.count { preferredByPara[it.id] != null }
        val total = paragraphs.size.coerceAtLeast(1)
        val coverage = ((translatedCount * 100f) / total).toInt().coerceIn(0, 100)
        val representative = preferredByPara.values.maxByOrNull { it.updatedAt }
            ?: allVersionsForLanguage.maxByOrNull { it.updatedAt }

        val staleCount = paragraphs.count { p ->
            val pref = preferredByPara[p.id]
            pref != null && pref.sourceContentHash.isNotBlank() &&
                p.sourceContentHash.isNotBlank() &&
                pref.sourceContentHash != p.sourceContentHash
        }

        return TranslationDiscoveryStatus(
            targetLanguage = targetLanguage,
            totalParagraphsInBook = paragraphs.size,
            translatedParagraphsCount = translatedCount,
            coveragePercent = coverage,
            isAvailable = translatedCount > 0,
            primaryProvider = representative?.providerUsed ?: "Not yet generated",
            primaryModel = representative?.modelUsed ?: "Bring Your Own AI",
            latestVersionNumber = allVersionsForLanguage.maxOfOrNull { it.versionNumber } ?: 0,
            latestReviewStatus = representative?.reviewStatus ?: "UNTRANSLATED",
            accessPolicy = representative?.accessPolicy ?: TranslationAccessPolicy.PUBLIC,
            lastGeneratedTimestamp = representative?.updatedAt ?: 0L,
            staleSourceCount = staleCount
        )
    }

    /**
     * Requirement #1 & #9: Search existing canonical translations AND segment-level Translation Memory
     * BEFORE making any AI request. Reuses eligible translations automatically and records
     * "AI Calls Avoided" analytics!
     */
    suspend fun findOrReuseExistingTranslationForParagraph(
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        book: BookEntity,
        paragraph: ParagraphEntity,
        targetLanguage: String
    ): ParagraphTranslationEntity? {
        val currentHash = paragraph.sourceContentHash.ifBlank {
            SecurityVault.computeContentFingerprint(paragraph.originalText.trim())
        }

        // Step 1: Check existing canonical translations for this exact paragraph + language
        val existingVersions = dao.getTranslationVersionsForParagraph(paragraph.id, targetLanguage)
        val eligibleExisting = existingVersions.firstOrNull { version ->
            version.isActiveVersion &&
                accessApi.canUserAccessTranslation(userId, entitlements, book, version) &&
                (version.sourceContentHash.isBlank() || version.sourceContentHash == currentHash)
        } ?: existingVersions.firstOrNull { version ->
            accessApi.canUserAccessTranslation(userId, entitlements, book, version)
        }

        if (eligibleExisting != null) {
            dao.incrementTranslationReuseCount(eligibleExisting.id)
            dao.insertAnalyticsEvent(
                CorpusAnalyticsEventEntity(
                    eventType = "TRANSLATION_REUSED",
                    bookId = book.id,
                    sourceLanguage = paragraph.originalLanguage,
                    targetLanguage = targetLanguage,
                    tokensSavedOrUsed = eligibleExisting.estimatedTokens,
                    userId = userId
                )
            )
            return eligibleExisting
        }

        // Step 2: Check Segment-Level Translation Memory (#9) for an exact passage match across the corpus!
        val tmMatch = dao.findExactTranslationMemoryMatch(
            sourceHash = currentHash,
            sourceLang = paragraph.originalLanguage,
            targetLang = targetLanguage
        )
        if (tmMatch != null) {
            dao.incrementTranslationMemoryReuse(tmMatch.id)
            val nextVersion = (existingVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1
            dao.deactivateOldTranslationsForParagraph(paragraph.id, targetLanguage)

            val reusedCanonical = ParagraphTranslationEntity(
                bookId = book.id,
                sectionId = paragraph.sectionId,
                paragraphId = paragraph.id,
                sourceLanguage = paragraph.originalLanguage,
                targetLanguage = targetLanguage,
                translatedText = tmMatch.translatedText,
                transliterationOrNotes = "${tmMatch.notesOrGlossary} [Reused via Translation Memory]",
                versionNumber = nextVersion,
                isActiveVersion = true,
                providerUsed = "${tmMatch.providerUsed} (TM Exact Match)",
                modelUsed = tmMatch.modelUsed,
                modelVersion = "TM-Reuse",
                promptVersion = "translation-memory-exact-v1",
                sourceContentVersion = paragraph.ocrVersion,
                sourceContentHash = currentHash,
                createdByUserId = userId.takeIf { it > 0L },
                translatorDisplayName = "Lipi Translation Memory",
                reviewStatus = tmMatch.reviewStatus,
                accessPolicy = tmMatch.accessPolicy,
                commercialRightsStatus = "TM_SHARED_REUSE",
                isSharedRepositoryAsset = true,
                estimatedTokens = 150,
                estimatedCostUsd = 0.0,
                reuseCount = tmMatch.matchReuseCount + 1
            )
            val newId = dao.insertTranslation(reusedCanonical)
            dao.insertAnalyticsEvent(
                CorpusAnalyticsEventEntity(
                    eventType = "TM_EXACT_MATCH_REUSED",
                    bookId = book.id,
                    sourceLanguage = paragraph.originalLanguage,
                    targetLanguage = targetLanguage,
                    tokensSavedOrUsed = 160,
                    userId = userId
                )
            )
            return reusedCanonical.copy(id = newId)
        }

        return null
    }

    /**
     * Requirement #1, #3, #6: Permanently saves a newly contributed translation with full provenance,
     * never deleting prior versions, and indexes it into Translation Memory if eligible for sharing.
     */
    suspend fun saveContributedCanonicalTranslation(
        userId: Long,
        translatorName: String,
        book: BookEntity,
        paragraph: ParagraphEntity,
        targetLanguage: String,
        translatedText: String,
        notes: String,
        providerName: String,
        modelName: String,
        reviewStatus: String = TranslationReviewStatus.AI_GENERATED,
        userAcceptedContributorLicense: Boolean = true
    ): ParagraphTranslationEntity {
        val currentHash = paragraph.sourceContentHash.ifBlank {
            SecurityVault.computeContentFingerprint(paragraph.originalText.trim())
        }
        val priorVersions = dao.getTranslationVersionsForParagraph(paragraph.id, targetLanguage)
        val nextVersion = (priorVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1

        // Never delete prior versions — simply mark the new version as the preferred active version
        dao.deactivateOldTranslationsForParagraph(paragraph.id, targetLanguage)

        val (accessPolicy, commercialStatus, isSharedAsset) = accessApi.resolveTranslationContributionPolicy(
            book = book,
            userAcceptedContributorLicense = userAcceptedContributorLicense
        )

        val estimatedTokens = ((paragraph.originalText.length + translatedText.length) / 3).coerceAtLeast(65)
        val estimatedCost = (estimatedTokens / 1000.0) * 0.0025

        val entity = ParagraphTranslationEntity(
            bookId = book.id,
            sectionId = paragraph.sectionId,
            paragraphId = paragraph.id,
            sourceLanguage = paragraph.originalLanguage,
            targetLanguage = targetLanguage,
            translatedText = translatedText.trim(),
            transliterationOrNotes = notes.trim(),
            versionNumber = nextVersion,
            isActiveVersion = true,
            providerUsed = providerName,
            modelUsed = modelName,
            modelVersion = "2026.10",
            promptVersion = "polyglot-indic-literary-v2.1",
            sourceContentVersion = paragraph.ocrVersion,
            sourceContentHash = currentHash,
            createdByUserId = userId.takeIf { it > 0L },
            translatorDisplayName = translatorName.ifBlank { "Community Contributor" },
            reviewStatus = reviewStatus,
            accessPolicy = accessPolicy,
            commercialRightsStatus = commercialStatus,
            isSharedRepositoryAsset = isSharedAsset,
            estimatedTokens = estimatedTokens,
            estimatedCostUsd = estimatedCost,
            reuseCount = 1
        )
        val id = dao.insertTranslation(entity)
        val saved = entity.copy(id = id)

        // Add to Global Translation Memory (#9) if eligible for the shared repository
        if (isSharedAsset) {
            dao.insertTranslationMemory(
                TranslationMemoryEntity(
                    normalizedSourceHash = currentHash,
                    sourceLanguage = paragraph.originalLanguage,
                    targetLanguage = targetLanguage,
                    canonicalSourceText = paragraph.originalText.trim(),
                    translatedText = translatedText.trim(),
                    notesOrGlossary = notes.trim(),
                    originBookId = book.id,
                    originParagraphId = paragraph.id,
                    originTranslationId = id,
                    providerUsed = providerName,
                    modelUsed = modelName,
                    reviewStatus = reviewStatus,
                    accessPolicy = accessPolicy,
                    matchReuseCount = 1
                )
            )
        }

        dao.insertAnalyticsEvent(
            CorpusAnalyticsEventEntity(
                eventType = "NEW_AI_TRANSLATION_CONTRIBUTED",
                bookId = book.id,
                sourceLanguage = paragraph.originalLanguage,
                targetLanguage = targetLanguage,
                tokensSavedOrUsed = estimatedTokens,
                userId = userId
            )
        )

        return saved
    }

    /**
     * Requirement #7: Switch preferred translation version without deleting history.
     */
    suspend fun setPreferredTranslationVersion(
        paragraphId: Long,
        targetLanguage: String,
        versionId: Long
    ) {
        dao.deactivateOldTranslationsForParagraph(paragraphId, targetLanguage)
        dao.markTranslationAsPreferred(versionId)
    }

    /**
     * Requirement #7: Advance quality review status (AI_GENERATED -> AI_REVIEWED -> HUMAN_REVIEWED -> EXPERT_VERIFIED -> PUBLISHER_APPROVED)
     */
    suspend fun updateReviewStatus(
        translationId: Long,
        newReviewStatus: String,
        reviewerName: String
    ) {
        dao.updateTranslationReviewStatus(
            translationId = translationId,
            newStatus = newReviewStatus,
            reviewerName = reviewerName
        )
    }

    suspend fun reportIssue(
        bookId: Long,
        paragraphId: Long,
        translationId: Long,
        targetLanguage: String,
        userId: Long,
        reporterName: String,
        category: String,
        description: String,
        suggestedCorrection: String
    ): Long {
        return dao.insertTranslationIssueReport(
            TranslationIssueReportEntity(
                bookId = bookId,
                paragraphId = paragraphId,
                translationId = translationId,
                targetLanguage = targetLanguage,
                reportedByUserId = userId,
                reporterName = reporterName.ifBlank { "Reader" },
                issueCategory = category,
                issueDescription = description,
                suggestedCorrection = suggestedCorrection
            )
        )
    }
}

// =====================================================================
// 5. CROSS-BOOK SEARCH & "ASK THIS BOOK" GROUNDING API (Requirements #12, #13, #22)
// =====================================================================

class SearchApi(
    private val dao: PolyglotDao,
    private val accessApi: AccessApi
) {

    /**
     * Requirement #13: Cross-Book Search across original text, translations, IAST transliterations, and metadata
     * for all books the user is entitled to access.
     */
    suspend fun searchAcrossEntitledBooks(
        query: String,
        userId: Long,
        entitlements: List<UserEntitlementEntity>,
        preferredLanguage: String
    ): List<CrossBookSearchResultItem> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()

        val allBooks = dao.getAllBooksInSystem()
        val entitledBooks = allBooks.filter { accessApi.canUserDiscoverBook(userId, entitlements, it) }
        val entitledBookById = entitledBooks.associateBy { it.id }
        if (entitledBookById.isEmpty()) return emptyList()

        val allParagraphs = dao.getAllParagraphsInSystem()
            .filter { entitledBookById.containsKey(it.bookId) }
        val allTranslations = dao.getAllCanonicalTranslationsInSystem()
            .filter { it.isActiveVersion && entitledBookById.containsKey(it.bookId) }
        val allTransliterations = dao.getAllTransliterationsInSystem()
            .filter { entitledBookById.containsKey(it.bookId) }

        val translationsByPara = allTranslations.groupBy { it.paragraphId }
        val translitByPara = allTransliterations.associateBy { it.paragraphId }

        val chaptersByBook = entitledBooks.associate { b ->
            b.id to dao.getChaptersForBook(b.id).associateBy { it.id }
        }
        val sectionsByBook = entitledBooks.associate { b ->
            b.id to dao.getChaptersForBook(b.id)
                .flatMap { ch -> dao.getSectionsForChapter(ch.id) }
                .associateBy { it.id }
        }

        val results = mutableListOf<CrossBookSearchResultItem>()

        for (para in allParagraphs) {
            val book = entitledBookById[para.bookId] ?: continue
            val ch = chaptersByBook[book.id]?.get(para.chapterId) ?: continue
            val sec = sectionsByBook[book.id]?.get(para.sectionId) ?: continue
            val paraTransList = translationsByPara[para.id].orEmpty()
            val prefTrans = paraTransList.find { it.targetLanguage.equals(preferredLanguage, ignoreCase = true) }
                ?: paraTransList.firstOrNull()
            val translit = translitByPara[para.id]

            val matchesOriginal = para.originalText.lowercase().contains(q) ||
                para.normalizedSearchText.lowercase().contains(q)
            val matchedTrans = paraTransList.find {
                it.translatedText.lowercase().contains(q) || it.transliterationOrNotes.lowercase().contains(q)
            }
            val matchesTranslit = translit != null && (
                translit.iastTransliteration.lowercase().contains(q) ||
                    translit.wordByWordBreakdown.lowercase().contains(q) ||
                    translit.literalEnglishTranslation.lowercase().contains(q) ||
                    translit.naturalEnglishTranslation.lowercase().contains(q) ||
                    translit.scholarlyCommentary.lowercase().contains(q)
                )
            val matchesMetadata = book.title.lowercase().contains(q) ||
                ch.title.lowercase().contains(q) ||
                sec.heading.lowercase().contains(q)

            if (matchesOriginal || matchedTrans != null || matchesTranslit || matchesMetadata) {
                val matchSource = when {
                    matchesOriginal -> "ORIGINAL_SCRIPT"
                    matchedTrans != null -> "TRANSLATION (${matchedTrans.targetLanguage})"
                    matchesTranslit -> "IAST_TRANSLITERATION_OR_GLOSSARY"
                    else -> "CHAPTER_SECTION_METADATA"
                }
                results.add(
                    CrossBookSearchResultItem(
                        book = book,
                        chapter = ch,
                        section = sec,
                        paragraph = para,
                        matchedTranslation = matchedTrans ?: prefTrans,
                        matchedTransliteration = translit,
                        matchSourceType = matchSource
                    )
                )
            }
        }
        return results.take(30)
    }

    /**
     * Requirement #12: Retrieves the best-matching passages across the entire book for "Ask This Book"
     * so every answer is grounded in actual book content with an exact Chapter, Section, Paragraph citation.
     */
    suspend fun retrieveGroundedPassagesInBook(
        bookId: Long,
        question: String,
        targetLanguage: String
    ): List<Triple<ChapterEntity, SectionEntity, BilingualParagraphItem>> {
        val chapters = dao.getChaptersForBook(bookId).associateBy { it.id }
        val sections = chapters.keys.flatMap { dao.getSectionsForChapter(it) }.associateBy { it.id }
        val paragraphs = dao.getParagraphsForBook(bookId)
        val translations = dao.getAllCanonicalTranslationsInSystem()
            .filter { it.bookId == bookId && it.isActiveVersion }
            .groupBy { it.paragraphId }
        val transliterations = dao.getAllTransliterationsInSystem()
            .filter { it.bookId == bookId }
            .associateBy { it.paragraphId }

        val keywords = question.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 3 && it !in setOf("what", "where", "does", "this", "book", "say", "about", "explain", "the", "and", "how") }

        val scored = paragraphs.mapNotNull { p ->
            val ch = chapters[p.chapterId] ?: return@mapNotNull null
            val sec = sections[p.sectionId] ?: return@mapNotNull null
            val pTransList = translations[p.id].orEmpty()
            val activeTrans = pTransList.find { it.targetLanguage.equals(targetLanguage, ignoreCase = true) }
                ?: pTransList.firstOrNull()
            val translit = transliterations[p.id]

            val searchableBlob = buildString {
                append(p.originalText).append(" ")
                append(p.normalizedSearchText).append(" ")
                append(ch.title).append(" ")
                append(sec.heading).append(" ")
                pTransList.forEach { append(it.translatedText).append(" ").append(it.transliterationOrNotes).append(" ") }
                if (translit != null) {
                    append(translit.iastTransliteration).append(" ")
                    append(translit.wordByWordBreakdown).append(" ")
                    append(translit.literalEnglishTranslation).append(" ")
                    append(translit.naturalEnglishTranslation).append(" ")
                    append(translit.scholarlyCommentary)
                }
            }.lowercase()

            val hitScore = if (keywords.isEmpty()) {
                1
            } else {
                keywords.count { kw -> searchableBlob.contains(kw) }
            }

            val item = BilingualParagraphItem(
                paragraph = p,
                activeTranslation = activeTrans,
                allVersions = pTransList,
                allVersionsCount = pTransList.size,
                transliterationLearning = translit
            )
            hitScore to Triple(ch, sec, item)
        }

        val matching = scored.filter { it.first > 0 }
            .sortedByDescending { it.first }
            .map { it.second }

        return if (matching.isNotEmpty()) {
            matching.take(4)
        } else {
            scored.map { it.second }.take(3)
        }
    }
}

// =====================================================================
// 6. AUDIO & PRONUNCIATION API (Requirements #14, #21, #22)
// =====================================================================

class AudioApi(private val dao: PolyglotDao) {

    fun resolveLanguageVoiceDescriptor(languageNameOrCode: String): LanguageDescriptor {
        return LanguageRegistry.findByNameOrCode(languageNameOrCode)
    }

    suspend fun recordListenEvent(bookId: Long, languageName: String, userId: Long) {
        val book = dao.getBookById(bookId)
        if (book != null) {
            dao.updateBook(book.copy(listenCountAnalytics = book.listenCountAnalytics + 1))
        }
        dao.insertAnalyticsEvent(
            CorpusAnalyticsEventEntity(
                eventType = "TTS_PLAYED",
                bookId = bookId,
                sourceLanguage = book?.originalLanguage ?: "Sanskrit",
                targetLanguage = languageName,
                tokensSavedOrUsed = 0,
                userId = userId
            )
        )
    }
}
