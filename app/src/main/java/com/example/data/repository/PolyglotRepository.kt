package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.api.AccessApi
import com.example.data.api.AudioApi
import com.example.data.api.BookApi
import com.example.data.api.ContentApi
import com.example.data.api.SearchApi
import com.example.data.api.SuperadminBackendApi
import com.example.data.api.TranslationApi
import com.example.data.local.AccountStatus
import com.example.data.local.AdminAuditLogEntity
import com.example.data.local.AiProviderRegistry
import com.example.data.local.BookAccessPolicy
import com.example.data.local.BookEntity
import com.example.data.local.BookPageEntity
import com.example.data.local.BookPublicationStatus
import com.example.data.local.BookRightsCategory
import com.example.data.local.BookmarkHighlightEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.CommercialPlanEntity
import com.example.data.local.ContributorConsentEntity
import com.example.data.local.CorpusAnalyticsEventEntity
import com.example.data.local.CrossBookSearchResultItem
import com.example.data.local.EntitlementKind
import com.example.data.local.JobStatus
import com.example.data.local.JobType
import com.example.data.local.LanguageDescriptor
import com.example.data.local.LanguageRegistry
import com.example.data.local.OcrCorrectionAuditEntity
import com.example.data.local.OfflineBookPackageEntity
import com.example.data.local.ParagraphEntity
import com.example.data.local.ParagraphTranslationEntity
import com.example.data.local.PlatformMetadataEntity
import com.example.data.local.PolyglotDao
import com.example.data.local.ProcessingJobEntity
import com.example.data.local.ReadingHistoryEntity
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.SampleAnthologySeeder
import com.example.data.local.SectionEntity
import com.example.data.local.SubscriptionTier
import com.example.data.local.TranslationIssueReportEntity
import com.example.data.local.TranslationMemoryEntity
import com.example.data.local.TranslationReviewStatus
import com.example.data.local.TransliterationLearningEntity
import com.example.data.local.UserAiConfigEntity
import com.example.data.local.UserBookAccessEntity
import com.example.data.local.UserEntitlementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.data.local.UserSessionEntity
import com.example.data.remote.MultiProviderAiGateway
import com.example.data.remote.OcrChapterDto
import com.example.data.remote.OcrParagraphDto
import com.example.data.remote.OcrSectionDto
import com.example.data.remote.ResolvedUserAiCredential
import com.example.util.EphemeralExtractedDocument
import com.example.util.ExtractedFormat
import com.example.util.PdfProcessor
import com.example.util.SecurityVault
import kotlinx.coroutines.flow.Flow
import java.io.File

enum class AiTaskKind {
    OCR,
    TRANSLATION,
    SUMMARY_AND_CHAT
}

data class SectionTranslationOutcome(
    val translations: List<ParagraphTranslationEntity>,
    val reusedCount: Int,
    val newlyGeneratedCount: Int,
    val contributedToSharedLibrary: Boolean,
    val noticeMessage: String
)

class PolyglotRepository(
    private val dao: PolyglotDao,
    private val appContext: Context
) {

    // Internal Decoupled APIs (#22 + Superadmin Backend API)
    val accessApi = AccessApi(dao)
    val bookApi = BookApi(dao, accessApi)
    val contentApi = ContentApi(dao)
    val translationApi = TranslationApi(dao, accessApi)
    val searchApi = SearchApi(dao, accessApi)
    val audioApi = AudioApi(dao)
    val superadminApi = SuperadminBackendApi(dao, accessApi)

    // =====================================================================
    // 1. AUTHENTICATION, REGISTRATION, SESSIONS & ENTITLEMENTS (#18, #19)
    // =====================================================================

    suspend fun restoreAuthenticatedUser(): UserEntity? {
        val saved = SecurityVault.getSavedSession(appContext) ?: return null
        val (userId, token) = saved
        val validSession = dao.getValidSession(userId, token) ?: run {
            SecurityVault.clearSavedSession(appContext)
            return null
        }
        val user = dao.getUserById(validSession.userId) ?: return null
        if (user.accountStatus == AccountStatus.SUSPENDED) {
            dao.clearSessionsForUser(user.id)
            SecurityVault.clearSavedSession(appContext)
            return null
        }
        return user
    }

    suspend fun registerUser(
        displayName: String,
        email: String,
        password: String,
        preferredLanguage: String
    ): Result<UserEntity> {
        val cleanName = displayName.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name (at least 2 characters)."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }
        if (dao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists. Please sign in."))
        }

        val salt = SecurityVault.generateSalt()
        val hash = SecurityVault.hashPassword(password, salt)

        val resolvedTopThree = LanguageRegistry.parseTopThreeLanguages(
            csv = if (preferredLanguage.contains(",")) preferredLanguage else null,
            fallbackPrimary = preferredLanguage.substringBefore(",").trim().ifBlank { "Gujarati" }
        )
        val primaryLangName = resolvedTopThree.first().name
        val topThreeCsv = LanguageRegistry.formatTopThreeCsv(resolvedTopThree)
        SecurityVault.saveTopThreeLanguagesPref(appContext, topThreeCsv)

        val newUser = UserEntity(
            email = cleanEmail,
            displayName = cleanName,
            passwordHash = hash,
            passwordSalt = salt,
            preferredLanguage = primaryLangName,
            topThreeLanguagesCsv = topThreeCsv,
            activeTierCode = SubscriptionTier.FREE,
            hasAcceptedContributorLicense = true
        )
        val userId = dao.insertUser(newUser)
        val createdUser = newUser.copy(id = userId)

        // Create persistent session token
        val token = SecurityVault.generateSessionToken()
        dao.insertSession(UserSessionEntity(userId = userId, token = token))
        SecurityVault.savePersistentSession(appContext, userId, token)

        // Grant default Free tier entitlement record (#18, #19)
        dao.insertEntitlement(
            UserEntitlementEntity(
                userId = userId,
                entitlementKind = EntitlementKind.SUBSCRIPTION_TIER,
                targetRef = SubscriptionTier.FREE,
                displayTitle = "Free Registered Account (Public & Free Books + BYO AI)"
            )
        )

        // Grant automatic ownership/access to public & free books
        val allBooks = dao.getAllBooksInSystem()
        allBooks.filter { it.accessPolicy in listOf(BookAccessPolicy.PUBLIC, BookAccessPolicy.FREE) }.forEach { freeBook ->
            dao.insertUserBookAccess(
                UserBookAccessEntity(
                    userId = userId,
                    bookId = freeBook.id,
                    accessType = "FREE_CLAIMED"
                )
            )
        }

        return Result.success(createdUser)
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both email and password."))
        }
        val user = dao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for '$cleanEmail'. Please sign up first."))

        if (user.accountStatus == AccountStatus.SUSPENDED) {
            return Result.failure(IllegalArgumentException("Account '${user.email}' has been suspended by an administrator."))
        }

        val valid = SecurityVault.verifyPassword(password, user.passwordSalt, user.passwordHash)
        if (!valid) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        dao.updateUser(updatedUser)

        val token = SecurityVault.generateSessionToken()
        dao.insertSession(UserSessionEntity(userId = user.id, token = token))
        SecurityVault.savePersistentSession(appContext, user.id, token)

        // Ensure user has at least 1 active tier entitlement
        val existingEnt = dao.getActiveEntitlementsForUser(user.id)
        if (existingEnt.none { it.entitlementKind == EntitlementKind.SUBSCRIPTION_TIER }) {
            dao.insertEntitlement(
                UserEntitlementEntity(
                    userId = user.id,
                    entitlementKind = EntitlementKind.SUBSCRIPTION_TIER,
                    targetRef = user.activeTierCode.ifBlank { SubscriptionTier.FREE },
                    displayTitle = "Subscription Tier: ${user.activeTierCode}"
                )
            )
        }

        return Result.success(updatedUser)
    }

    suspend fun logoutUser(userId: Long) {
        dao.clearSessionsForUser(userId)
        SecurityVault.clearSavedSession(appContext)
    }

    suspend fun updateUserPreferredLanguage(user: UserEntity, languageName: String): UserEntity {
        val currentTopThree = LanguageRegistry.parseTopThreeLanguages(user.topThreeLanguagesCsv, user.preferredLanguage)
        val chosen = LanguageRegistry.findByNameOrCode(languageName)
        val updatedTopThree = if (currentTopThree.any { it.code == chosen.code }) {
            currentTopThree
        } else {
            (listOf(chosen) + currentTopThree).distinctBy { it.code }.take(LanguageRegistry.MAX_USER_ACTIVE_LANGUAGES)
        }
        val csv = LanguageRegistry.formatTopThreeCsv(updatedTopThree)
        SecurityVault.saveTopThreeLanguagesPref(appContext, csv)
        val updated = user.copy(
            preferredLanguage = chosen.name,
            topThreeLanguagesCsv = csv
        )
        dao.updateUser(updated)
        return updated
    }

    suspend fun updateUserTopThreeLanguages(
        user: UserEntity?,
        selectedLanguages: List<LanguageDescriptor>
    ): Pair<UserEntity?, List<LanguageDescriptor>> {
        val normalized = LanguageRegistry.parseTopThreeLanguages(
            csv = selectedLanguages.joinToString(",") { it.name },
            fallbackPrimary = selectedLanguages.firstOrNull()?.name ?: user?.preferredLanguage ?: "Gujarati"
        )
        val csv = LanguageRegistry.formatTopThreeCsv(normalized)
        SecurityVault.saveTopThreeLanguagesPref(appContext, csv)

        if (user == null) {
            return null to normalized
        }

        val newPrimary = if (normalized.any { it.name.equals(user.preferredLanguage, ignoreCase = true) }) {
            user.preferredLanguage
        } else {
            normalized.first().name
        }
        val updatedUser = user.copy(
            preferredLanguage = newPrimary,
            topThreeLanguagesCsv = csv
        )
        dao.updateUser(updatedUser)
        return updatedUser to normalized
    }

    fun getRestoredTopThreeLanguages(user: UserEntity?): List<LanguageDescriptor> {
        val sourceCsv = user?.topThreeLanguagesCsv?.takeIf { it.isNotBlank() }
            ?: SecurityVault.getTopThreeLanguagesPref(appContext)
        return LanguageRegistry.parseTopThreeLanguages(sourceCsv, user?.preferredLanguage)
    }

    suspend fun switchUserSubscriptionTier(user: UserEntity, newTier: String): UserEntity {
        dao.clearTierEntitlementsForUser(user.id)
        val label = when (newTier) {
            SubscriptionTier.READER -> "Reader Plan ($12/mo • Offline, Enhanced TTS, Ask This Book)"
            SubscriptionTier.PRO_RESEARCH -> "Pro / Research ($29/mo • Cross-Book Search, Scholarly Mode, Restricted Archive)"
            SubscriptionTier.PUBLISHER_INSTITUTION -> "Publisher / Institution CMS (Book & Rights Admin, Official Review, Analytics)"
            else -> "Free Tier ($0 • Free/Public Books, Shared Translations, BYO AI)"
        }
        dao.insertEntitlement(
            UserEntitlementEntity(
                userId = user.id,
                entitlementKind = EntitlementKind.SUBSCRIPTION_TIER,
                targetRef = newTier,
                displayTitle = label
            )
        )
        val updated = user.copy(activeTierCode = newTier)
        dao.updateUser(updated)
        return updated
    }

    suspend fun grantInstitutionMembership(userId: Long, institutionCode: String): UserEntitlementEntity {
        val ent = UserEntitlementEntity(
            userId = userId,
            entitlementKind = EntitlementKind.INSTITUTION_MEMBERSHIP,
            targetRef = institutionCode.ifBlank { "INST_BHARAT_VIDYAPITH" },
            displayTitle = "Institution Access: ${institutionCode.ifBlank { "Bharat Vidyapith Research Network" }}"
        )
        val id = dao.insertEntitlement(ent)
        return ent.copy(id = id)
    }

    suspend fun acceptContributorAgreementForBook(userId: Long, book: BookEntity): ContributorConsentEntity {
        return accessApi.recordContributorConsent(userId, book)
    }

    fun getActiveEntitlementsForUserFlow(userId: Long): Flow<List<UserEntitlementEntity>> =
        dao.getActiveEntitlementsForUserFlow(userId)

    fun getContributorConsentsForUserFlow(userId: Long): Flow<List<ContributorConsentEntity>> =
        dao.getContributorConsentsForUserFlow(userId)

    // =====================================================================
    // 2. USER-OWNED AI CONFIGURATIONS & ENCRYPTED CREDENTIAL VAULT
    // =====================================================================

    fun getAiConfigsForUserFlow(userId: Long): Flow<List<UserAiConfigEntity>> =
        dao.getAiConfigsForUserFlow(userId)

    suspend fun saveOrUpdateUserAiConfig(
        userId: Long,
        existingConfigId: Long?,
        providerId: String,
        modelName: String,
        rawApiKey: String,
        customBaseUrl: String,
        useForOcr: Boolean,
        useForTranslation: Boolean,
        useForChat: Boolean
    ): Result<UserAiConfigEntity> {
        if (userId <= 0L) {
            return Result.failure(IllegalStateException("You must be signed in to configure AI credentials."))
        }
        val provider = AiProviderRegistry.findById(providerId)
        val cleanModel = modelName.trim().ifBlank { provider.availableModels.first() }
        val cleanBaseUrl = customBaseUrl.trim().ifBlank { provider.defaultBaseUrl }

        val encryptedKey: String
        val maskedPreview: String

        if (rawApiKey.isNotBlank()) {
            encryptedKey = SecurityVault.encryptApiKey(appContext, rawApiKey.trim())
            maskedPreview = SecurityVault.maskApiKey(rawApiKey.trim())
        } else if (existingConfigId != null) {
            val existing = dao.getAiConfigById(existingConfigId, userId)
                ?: return Result.failure(IllegalArgumentException("Configuration not found."))
            encryptedKey = existing.encryptedApiKey
            maskedPreview = existing.maskedKeyPreview
        } else {
            return Result.failure(IllegalArgumentException("Please enter your ${provider.displayName} API key."))
        }

        if (useForOcr) dao.clearDefaultOcrForUser(userId)
        if (useForTranslation) dao.clearDefaultTranslationForUser(userId)
        if (useForChat) dao.clearDefaultChatForUser(userId)

        val entity = UserAiConfigEntity(
            id = existingConfigId ?: 0L,
            userId = userId,
            providerId = provider.id,
            providerDisplayName = provider.displayName,
            modelName = cleanModel,
            customBaseUrl = cleanBaseUrl,
            encryptedApiKey = encryptedKey,
            maskedKeyPreview = maskedPreview,
            isDefaultForOcr = useForOcr,
            isDefaultForTranslation = useForTranslation,
            isDefaultForSummaryAndChat = useForChat,
            lastTestedStatus = "Saved",
            updatedAt = System.currentTimeMillis()
        )

        val savedId = dao.insertAiConfig(entity)
        return Result.success(entity.copy(id = savedId))
    }

    suspend fun testUserAiConfigConnection(
        userId: Long,
        configId: Long?,
        providerId: String,
        modelName: String,
        rawApiKeyInput: String,
        customBaseUrl: String
    ): Result<String> {
        val provider = AiProviderRegistry.findById(providerId)
        val keyToTest = if (rawApiKeyInput.isNotBlank()) {
            rawApiKeyInput.trim()
        } else if (configId != null && userId > 0L) {
            val saved = dao.getAiConfigById(configId, userId)
            if (saved != null) SecurityVault.decryptApiKey(appContext, saved.encryptedApiKey) else ""
        } else {
            ""
        }

        if (keyToTest.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter an API key to test the connection."))
        }

        val credential = ResolvedUserAiCredential(
            providerId = provider.id,
            providerName = provider.displayName,
            modelName = modelName.trim().ifBlank { provider.availableModels.first() },
            baseUrl = customBaseUrl.trim().ifBlank { provider.defaultBaseUrl },
            decryptedApiKey = keyToTest
        )

        val testResult = MultiProviderAiGateway.testConnection(credential)
        if (configId != null && userId > 0L) {
            val saved = dao.getAiConfigById(configId, userId)
            if (saved != null) {
                val statusLabel = if (testResult.isSuccess) {
                    "Verified ✓"
                } else {
                    "Failed: ${testResult.exceptionOrNull()?.localizedMessage?.take(40) ?: "Error"}"
                }
                dao.updateAiConfig(saved.copy(lastTestedStatus = statusLabel, updatedAt = System.currentTimeMillis()))
            }
        }
        return testResult
    }

    suspend fun deleteUserAiConfig(userId: Long, configId: Long) {
        dao.deleteAiConfig(configId, userId)
    }

    private suspend fun resolveUserCredentialForTask(
        userId: Long,
        taskKind: AiTaskKind
    ): Result<ResolvedUserAiCredential> {
        if (userId <= 0L) {
            return Result.failure(
                IllegalStateException("Please sign in and configure your AI provider in AI Settings to run live AI tasks.")
            )
        }
        val configs = dao.getAiConfigsForUser(userId)
        if (configs.isEmpty()) {
            return Result.failure(
                IllegalStateException("No AI provider configured for your account yet. Open 'AI Settings' to add your OpenAI, Gemini, Anthropic, or OpenRouter API key.")
            )
        }

        val chosen = when (taskKind) {
            AiTaskKind.OCR -> configs.find { it.isDefaultForOcr } ?: configs.first()
            AiTaskKind.TRANSLATION -> configs.find { it.isDefaultForTranslation } ?: configs.first()
            AiTaskKind.SUMMARY_AND_CHAT -> configs.find { it.isDefaultForSummaryAndChat } ?: configs.first()
        }

        val decryptedKey = SecurityVault.decryptApiKey(appContext, chosen.encryptedApiKey)
        if (decryptedKey.isBlank()) {
            return Result.failure(
                IllegalStateException("Could not unlock API key for ${chosen.providerDisplayName}. Please re-enter your key in AI Settings.")
            )
        }

        val provider = AiProviderRegistry.findById(chosen.providerId)
        return Result.success(
            ResolvedUserAiCredential(
                providerId = chosen.providerId,
                providerName = chosen.providerDisplayName,
                modelName = chosen.modelName,
                baseUrl = chosen.customBaseUrl.ifBlank { provider.defaultBaseUrl },
                decryptedApiKey = decryptedKey
            )
        )
    }

    // =====================================================================
    // 3. BOOKS, ACCESS CONTROL CATALOGUE, OFFLINE & CMS (#4, #17, #20)
    // =====================================================================

    fun getAllBooksInSystemFlow(): Flow<List<BookEntity>> = dao.getAllBooksInSystemFlow()

    fun getPublicDirectoryBooksFlow(): Flow<List<BookEntity>> = dao.getPublicDirectoryBooksFlow()

    fun getAccessibleBooksForUserFlow(userId: Long): Flow<List<BookEntity>> =
        dao.getAccessibleBooksForUserFlow(userId)

    fun getUserBookAccessFlow(userId: Long): Flow<List<UserBookAccessEntity>> =
        dao.getUserBookAccessFlow(userId)

    fun getOfflinePackagesForUserFlow(userId: Long): Flow<List<OfflineBookPackageEntity>> =
        dao.getOfflinePackagesForUserFlow(userId)

    suspend fun ensurePublicCatalogInitialized(): BookEntity {
        val refPdfFile = PdfProcessor.createReferenceAnthologyPdf(appContext)
        return SampleAnthologySeeder.ensurePublicCatalogSeeded(
            dao = dao,
            referencePdfPath = refPdfFile.absolutePath,
            referencePdfSize = refPdfFile.length()
        )
    }

    suspend fun getBookById(bookId: Long): BookEntity? = dao.getBookById(bookId)

    suspend fun canUserReadBook(userId: Long, book: BookEntity): Boolean {
        val entitlements = if (userId > 0L) dao.getActiveEntitlementsForUser(userId) else emptyList()
        val accessRow = if (userId > 0L) listOfNotNull(dao.getUserAccessForBook(userId, book.id)) else emptyList()
        return accessApi.canUserReadBook(userId, entitlements, accessRow, book)
    }

    suspend fun unlockOrPurchaseBookForUser(userId: Long, book: BookEntity): Result<UserBookAccessEntity> {
        if (userId <= 0L) {
            return Result.failure(IllegalStateException("Please sign in to unlock or purchase books."))
        }
        val accessType = if (book.isPaid) "PURCHASED" else "FREE_CLAIMED"
        val entity = UserBookAccessEntity(
            userId = userId,
            bookId = book.id,
            accessType = accessType
        )
        val id = dao.insertUserBookAccess(entity)

        // Also record a granular BOOK_PURCHASE entitlement (#19)
        if (book.isPaid) {
            dao.insertEntitlement(
                UserEntitlementEntity(
                    userId = userId,
                    entitlementKind = EntitlementKind.BOOK_PURCHASE,
                    targetRef = book.id.toString(),
                    displayTitle = "Book Licence: ${book.title} (${book.priceDisplay})"
                )
            )
        }

        dao.insertReadingHistory(
            ReadingHistoryEntity(
                userId = userId,
                bookId = book.id,
                bookTitle = book.title,
                chapterTitle = "Entitlement & Library Access",
                sectionId = 0L,
                sectionHeading = if (book.isPaid) "Purchased Book Entitlement (${book.priceDisplay})" else "Added Free Book to Library",
                languageRead = book.preferredTargetLanguage,
                actionSummary = if (book.isPaid) "Purchased '${book.title}'" else "Claimed '${book.title}'"
            )
        )
        return Result.success(entity.copy(id = id))
    }

    suspend fun downloadBookOfflinePackage(
        userId: Long,
        book: BookEntity,
        targetLanguage: String
    ): Result<OfflineBookPackageEntity> {
        if (userId <= 0L) {
            return Result.failure(IllegalStateException("Sign in to download offline reading packages."))
        }
        val pkg = bookApi.downloadBookForOfflineReading(userId, book, targetLanguage)
        return Result.success(pkg)
    }

    suspend fun removeOfflinePackage(userId: Long, bookId: Long) {
        dao.removeOfflineBookPackage(userId, bookId)
    }

    // =====================================================================
    // 4. STRUCTURED READING, VERSIONED TRANSLATIONS, OCR AUDIT & ANALYTICS
    // =====================================================================

    fun getChaptersForBookFlow(bookId: Long): Flow<List<ChapterEntity>> =
        dao.getChaptersForBookFlow(bookId)

    fun getSectionsForBookFlow(bookId: Long): Flow<List<SectionEntity>> =
        dao.getSectionsForBookFlow(bookId)

    fun getParagraphsForBookFlow(bookId: Long): Flow<List<ParagraphEntity>> =
        dao.getParagraphsForBookFlow(bookId)

    fun getActiveTranslationsForBookFlow(
        bookId: Long,
        targetLanguage: String
    ): Flow<List<ParagraphTranslationEntity>> =
        dao.getActiveTranslationsForBookFlow(bookId, targetLanguage)

    fun getAllTranslationVersionsForBookFlow(
        bookId: Long,
        targetLanguage: String
    ): Flow<List<ParagraphTranslationEntity>> =
        dao.getAllTranslationVersionsForBookFlow(bookId, targetLanguage)

    fun getTransliterationsForBookFlow(bookId: Long): Flow<List<TransliterationLearningEntity>> =
        dao.getTransliterationsForBookFlow(bookId)

    fun getOcrAuditsForBookFlow(bookId: Long): Flow<List<OcrCorrectionAuditEntity>> =
        dao.getOcrAuditsForBookFlow(bookId)

    fun getCachedLanguagesForBookFlow(bookId: Long): Flow<List<String>> =
        dao.getCachedTranslationLanguagesForBookFlow(bookId)

    fun getAllReadingProgressForUserFlow(userId: Long): Flow<List<ReadingProgressEntity>> =
        dao.getAllReadingProgressForUserFlow(userId)

    fun getAllBookmarksForUserFlow(userId: Long): Flow<List<BookmarkHighlightEntity>> =
        dao.getAllBookmarksForUserFlow(userId)

    fun getBookmarksForUserAndBookFlow(userId: Long, bookId: Long): Flow<List<BookmarkHighlightEntity>> =
        dao.getBookmarksForUserAndBookFlow(userId, bookId)

    fun getReadingHistoryForUserFlow(userId: Long): Flow<List<ReadingHistoryEntity>> =
        dao.getReadingHistoryForUserFlow(userId)

    fun getMessagesForUserAndBookFlow(userId: Long, bookId: Long): Flow<List<ChatMessageEntity>> =
        dao.getMessagesForUserAndBookFlow(userId, bookId)

    fun getRecentProcessingJobsFlow(): Flow<List<ProcessingJobEntity>> =
        dao.getRecentProcessingJobsFlow()

    fun getAllCanonicalTranslationsInSystemFlow(): Flow<List<ParagraphTranslationEntity>> =
        dao.getAllCanonicalTranslationsInSystemFlow()

    fun getAllTranslationMemoryFlow(): Flow<List<TranslationMemoryEntity>> =
        dao.getAllTranslationMemoryFlow()

    fun getAllTranslationIssueReportsFlow(): Flow<List<TranslationIssueReportEntity>> =
        dao.getAllTranslationIssueReportsFlow()

    fun getAllAnalyticsEventsFlow(): Flow<List<CorpusAnalyticsEventEntity>> =
        dao.getAllAnalyticsEventsFlow()

    suspend fun getMostRecentReadingProgressForUser(userId: Long): ReadingProgressEntity? =
        dao.getMostRecentReadingProgressForUser(userId)

    suspend fun getUserReadingProgressForBook(userId: Long, bookId: Long): ReadingProgressEntity? =
        dao.getUserReadingProgressForBook(userId, bookId)

    suspend fun getChaptersForBook(bookId: Long): List<ChapterEntity> =
        dao.getChaptersForBook(bookId)

    suspend fun getSectionsForChapter(chapterId: Long): List<SectionEntity> =
        dao.getSectionsForChapter(chapterId)

    suspend fun searchInBook(bookId: Long, query: String): List<ParagraphEntity> {
        if (query.isBlank()) return emptyList()
        return dao.searchParagraphsInBook(bookId, query.trim())
    }

    suspend fun searchAcrossAllEntitledBooks(
        userId: Long,
        query: String,
        preferredLanguage: String
    ): List<CrossBookSearchResultItem> {
        val entitlements = if (userId > 0L) dao.getActiveEntitlementsForUser(userId) else emptyList()
        return searchApi.searchAcrossEntitledBooks(query, userId, entitlements, preferredLanguage)
    }

    /**
     * Requirement #8 & #16: Submit OCR Correction with full audit trail and source hash update.
     */
    suspend fun submitOcrCorrection(
        paragraphId: Long,
        correctedText: String,
        userId: Long,
        userName: String,
        reason: String
    ): Result<ParagraphEntity> {
        return contentApi.submitOcrCorrectionWithAudit(
            paragraphId = paragraphId,
            newCorrectedText = correctedText,
            authorUserId = userId,
            authorName = userName,
            reason = reason
        )
    }

    /**
     * Requirement #7: Switch preferred translation version, submit human-reviewed correction,
     * promote review status, or report an issue.
     */
    suspend fun markPreferredTranslationVersion(
        paragraphId: Long,
        targetLanguage: String,
        versionId: Long
    ) {
        translationApi.setPreferredTranslationVersion(paragraphId, targetLanguage, versionId)
    }

    suspend fun submitHumanReviewedTranslationVersion(
        userId: Long,
        reviewerName: String,
        book: BookEntity,
        paragraph: ParagraphEntity,
        targetLanguage: String,
        correctedTranslationText: String,
        notes: String
    ): ParagraphTranslationEntity {
        return translationApi.saveContributedCanonicalTranslation(
            userId = userId,
            translatorName = reviewerName.ifBlank { "Human Editorial Reviewer" },
            book = book,
            paragraph = paragraph,
            targetLanguage = targetLanguage,
            translatedText = correctedTranslationText,
            notes = notes,
            providerName = "Human Editorial Review",
            modelName = "Scholarly Revision",
            reviewStatus = TranslationReviewStatus.HUMAN_REVIEWED,
            userAcceptedContributorLicense = true
        )
    }

    suspend fun promoteTranslationReviewStatus(
        translationId: Long,
        newStatus: String,
        reviewerName: String
    ) {
        translationApi.updateReviewStatus(translationId, newStatus, reviewerName)
    }

    suspend fun reportTranslationIssue(
        bookId: Long,
        paragraphId: Long,
        translationId: Long,
        targetLanguage: String,
        userId: Long,
        reporterName: String,
        category: String,
        description: String,
        suggestedCorrection: String
    ) {
        translationApi.reportIssue(
            bookId = bookId,
            paragraphId = paragraphId,
            translationId = translationId,
            targetLanguage = targetLanguage,
            userId = userId,
            reporterName = reporterName,
            category = category,
            description = description,
            suggestedCorrection = suggestedCorrection
        )
    }

    suspend fun resolveReportedIssueInCms(reportId: Long, status: String) {
        dao.updateTranslationIssueStatus(reportId, status)
    }

    suspend fun updateBookAccessPolicyInCms(
        bookId: Long,
        accessPolicy: String,
        rightsCategory: String,
        isPaid: Boolean,
        priceDisplay: String
    ) {
        bookApi.updateBookAccessAndRights(bookId, accessPolicy, rightsCategory, isPaid, priceDisplay)
    }

    suspend fun saveUserReadingProgress(
        userId: Long,
        book: BookEntity,
        chapter: ChapterEntity,
        section: SectionEntity,
        paragraphId: Long,
        preferredLanguage: String,
        completedSections: Int,
        totalSections: Int
    ) {
        val progressFraction = if (totalSections > 0) {
            completedSections.toFloat() / totalSections.toFloat()
        } else 0f

        val existing = dao.getUserReadingProgressForBook(userId, book.id)
        val entity = ReadingProgressEntity(
            id = existing?.id ?: 0L,
            userId = userId,
            bookId = book.id,
            bookTitle = book.title,
            currentChapterId = chapter.id,
            currentChapterTitle = chapter.title,
            currentSectionId = section.id,
            currentSectionHeading = section.heading,
            currentParagraphId = paragraphId,
            preferredLanguage = preferredLanguage,
            completedSectionsCount = completedSections,
            totalSectionsCount = totalSections,
            progressPercent = progressFraction.coerceIn(0f, 1f),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertReadingProgress(entity)

        if (userId > 0L && existing?.currentSectionId != section.id) {
            dao.insertReadingHistory(
                ReadingHistoryEntity(
                    userId = userId,
                    bookId = book.id,
                    bookTitle = book.title,
                    chapterTitle = chapter.title,
                    sectionId = section.id,
                    sectionHeading = section.heading,
                    languageRead = preferredLanguage,
                    actionSummary = "Read ${section.heading} in $preferredLanguage"
                )
            )
        }
    }

    suspend fun toggleUserBookmark(
        userId: Long,
        book: BookEntity,
        chapter: ChapterEntity?,
        section: SectionEntity?,
        paragraph: ParagraphEntity,
        translatedSnippet: String,
        existingBookmarks: List<BookmarkHighlightEntity>
    ) {
        val existing = existingBookmarks.find { it.paragraphId == paragraph.id && it.userId == userId }
        if (existing != null) {
            dao.removeBookmarkForUserAndParagraph(userId, paragraph.id)
        } else {
            dao.insertBookmarkHighlight(
                BookmarkHighlightEntity(
                    userId = userId,
                    bookId = book.id,
                    bookTitle = book.title,
                    chapterId = chapter?.id ?: paragraph.chapterId,
                    sectionId = section?.id ?: paragraph.sectionId,
                    sectionHeading = section?.heading ?: "Section",
                    paragraphId = paragraph.id,
                    selectedSnippet = paragraph.originalText,
                    translatedSnippet = translatedSnippet
                )
            )
        }
    }

    suspend fun renderReferencePdfPage(filePath: String, pageIndex: Int): Bitmap? {
        return PdfProcessor.renderPageToBitmap(File(filePath), pageIndex)
    }

    // =====================================================================
    // 5. PERSISTENT JOB QUEUE, SHARED REPOSITORY REUSE & AI CONTRIBUTION
    // =====================================================================

    /**
     * Requirement #1, #2, #3, #6, #9, #10, #11:
     * Central Section Translation Pipeline:
     * 1. Enqueues a persistent `TRANSLATION` or `RETRANSLATION` job in `processing_jobs`.
     * 2. Unless `forceRetranslate == true`, searches the Shared Translation Repository AND
     *    Segment-Level Translation Memory BEFORE calling AI!
     * 3. Any paragraph already satisfied by an eligible canonical translation or Translation Memory
     *    is reused immediately (0 AI calls, increments `aiCallsAvoidedByReuse`).
     * 4. Only paragraphs without an eligible match are sent to the user's configured AI provider,
     *    validated, attached with full provenance & source content hash, and saved permanently
     *    into the Shared Translation Repository and Translation Memory.
     */
    suspend fun translateSectionWithSharedRepoAndUserAi(
        userId: Long,
        userDisplayName: String,
        userAcceptedContributorLicense: Boolean,
        book: BookEntity,
        chapter: ChapterEntity,
        section: SectionEntity,
        targetLanguage: String,
        forceRetranslate: Boolean = false
    ): Result<SectionTranslationOutcome> {
        val paragraphs = dao.getParagraphsForSection(section.id)
        if (paragraphs.isEmpty()) {
            return Result.success(
                SectionTranslationOutcome(
                    translations = emptyList(),
                    reusedCount = 0,
                    newlyGeneratedCount = 0,
                    contributedToSharedLibrary = false,
                    noticeMessage = "No paragraphs in section."
                )
            )
        }

        val entitlements = if (userId > 0L) dao.getActiveEntitlementsForUser(userId) else emptyList()
        val resolvedTranslations = mutableListOf<ParagraphTranslationEntity>()
        val paragraphsNeedingAi = mutableListOf<ParagraphEntity>()
        var reusedCount = 0

        if (!forceRetranslate) {
            for (paragraph in paragraphs) {
                val reused = translationApi.findOrReuseExistingTranslationForParagraph(
                    userId = userId,
                    entitlements = entitlements,
                    book = book,
                    paragraph = paragraph,
                    targetLanguage = targetLanguage
                )
                if (reused != null) {
                    resolvedTranslations.add(reused)
                    reusedCount++
                } else {
                    paragraphsNeedingAi.add(paragraph)
                }
            }
        } else {
            paragraphsNeedingAi.addAll(paragraphs)
        }

        // If 100% of paragraphs were satisfied by the Shared Translation Repository or Translation Memory, return immediately!
        if (paragraphsNeedingAi.isEmpty()) {
            return Result.success(
                SectionTranslationOutcome(
                    translations = resolvedTranslations,
                    reusedCount = reusedCount,
                    newlyGeneratedCount = 0,
                    contributedToSharedLibrary = false,
                    noticeMessage = "✓ Reused $reusedCount existing canonical/Translation Memory translation(s) from the Polyglot Shared Repository (0 AI calls needed)."
                )
            )
        }

        // Otherwise, resolve the user's own AI credential and run a persistent processing job (#10)
        val credentialResult = resolveUserCredentialForTask(userId, AiTaskKind.TRANSLATION)
        val credential = credentialResult.getOrElse { return Result.failure(it) }

        val jobId = dao.insertProcessingJob(
            ProcessingJobEntity(
                jobType = if (forceRetranslate) JobType.RETRANSLATION else JobType.TRANSLATION,
                status = JobStatus.PROCESSING,
                bookId = book.id,
                bookTitle = book.title,
                sectionId = section.id,
                targetLanguage = targetLanguage,
                requestedByUserId = userId,
                providerAndModel = "${credential.providerName} • ${credential.modelName}",
                progressPercent = 25,
                statusDetail = "Translating ${paragraphsNeedingAi.size} paragraph(s) into $targetLanguage…"
            )
        )

        val pairs = paragraphsNeedingAi.map { it.id to it.originalText }
        val aiResult = MultiProviderAiGateway.translateStoredParagraphs(
            credential = credential,
            bookTitle = book.title,
            chapterTitle = chapter.title,
            sectionHeading = section.heading,
            sourceLanguage = book.originalLanguage,
            targetLanguage = targetLanguage,
            paragraphsToTranslate = pairs
        )

        return aiResult.fold(
            onSuccess = { dtoList ->
                var newlySavedCount = 0
                var sharedRepoFlag = false

                for (paragraph in paragraphsNeedingAi) {
                    val matchedDto = dtoList.find { it.paragraphId == paragraph.id }
                        ?: dtoList.getOrNull(paragraphsNeedingAi.indexOf(paragraph))
                    if (matchedDto != null && matchedDto.translatedText.isNotBlank()) {
                        val saved = translationApi.saveContributedCanonicalTranslation(
                            userId = userId,
                            translatorName = userDisplayName.ifBlank { "Authenticated Contributor" },
                            book = book,
                            paragraph = paragraph,
                            targetLanguage = targetLanguage,
                            translatedText = matchedDto.translatedText,
                            notes = matchedDto.notes,
                            providerName = credential.providerName,
                            modelName = credential.modelName,
                            reviewStatus = TranslationReviewStatus.AI_GENERATED,
                            userAcceptedContributorLicense = userAcceptedContributorLicense
                        )
                        resolvedTranslations.add(saved)
                        newlySavedCount++
                        if (saved.isSharedRepositoryAsset) {
                            sharedRepoFlag = true
                        }
                    }
                }

                dao.updateProcessingJob(
                    ProcessingJobEntity(
                        id = jobId,
                        jobType = if (forceRetranslate) JobType.RETRANSLATION else JobType.TRANSLATION,
                        status = JobStatus.COMPLETED,
                        bookId = book.id,
                        bookTitle = book.title,
                        sectionId = section.id,
                        targetLanguage = targetLanguage,
                        requestedByUserId = userId,
                        providerAndModel = "${credential.providerName} • ${credential.modelName}",
                        progressPercent = 100,
                        statusDetail = "Saved $newlySavedCount canonical translation(s) (Reused $reusedCount from Shared Repo/TM)"
                    )
                )

                if (userId > 0L) {
                    dao.insertReadingHistory(
                        ReadingHistoryEntity(
                            userId = userId,
                            bookId = book.id,
                            bookTitle = book.title,
                            chapterTitle = chapter.title,
                            sectionId = section.id,
                            sectionHeading = section.heading,
                            languageRead = targetLanguage,
                            actionSummary = "Contributed $targetLanguage translation for '${section.heading}' via ${credential.providerName}"
                        )
                    )
                }

                val notice = if (sharedRepoFlag) {
                    "✓ Your $targetLanguage translation (${credential.providerName} • ${credential.modelName}) has been added to the Polyglot shared translation library."
                } else {
                    "✓ Saved private $targetLanguage translation for your personal library."
                }

                Result.success(
                    SectionTranslationOutcome(
                        translations = resolvedTranslations,
                        reusedCount = reusedCount,
                        newlyGeneratedCount = newlySavedCount,
                        contributedToSharedLibrary = sharedRepoFlag,
                        noticeMessage = notice
                    )
                )
            },
            onFailure = { err ->
                dao.updateProcessingJob(
                    ProcessingJobEntity(
                        id = jobId,
                        jobType = if (forceRetranslate) JobType.RETRANSLATION else JobType.TRANSLATION,
                        status = JobStatus.FAILED,
                        bookId = book.id,
                        bookTitle = book.title,
                        sectionId = section.id,
                        targetLanguage = targetLanguage,
                        requestedByUserId = userId,
                        providerAndModel = "${credential.providerName} • ${credential.modelName}",
                        progressPercent = 0,
                        statusDetail = "Failed: ${err.localizedMessage?.take(80)}"
                    )
                )
                Result.failure(err)
            }
        )
    }

    /**
     * Zero-Host Peer-to-Peer Multi-Format Extraction (PDF, EPUB, Markdown, TXT, HTML, P2P JSON):
     * - Any user (signed-in or guest, with or without an external AI key) can convert their local
     *   document into structured OCR/text chapters, sections, and paragraphs.
     * - The user's original certified/restricted file is processed ephemerally and purged immediately
     *   (`0 bytes` hosted on server).
     * - VidyaKosha owns and retains only the structured OCR derivative (`P2P_DERIVATIVE_OWNED`)
     *   and all resulting canonical translations in the Shared Translation Repository.
     */
    suspend fun ingestPdfIntoStructuredBookForUser(
        userId: Long,
        uri: Uri,
        preferredTargetLanguage: String,
        onProgress: (stage: String, processedPages: Int, totalPages: Int) -> Unit
    ): Result<BookEntity> {
        val extractResult = PdfProcessor.extractDocumentEphemerally(appContext, uri)
        val extracted = extractResult.getOrElse { return Result.failure(it) }
        return ingestEphemeralExtractedDocument(
            userId = userId,
            extracted = extracted,
            preferredTargetLanguage = preferredTargetLanguage,
            onProgress = onProgress
        )
    }

    suspend fun ingestSampleEpubForP2PCommons(
        userId: Long,
        preferredTargetLanguage: String,
        onProgress: (stage: String, processedPages: Int, totalPages: Int) -> Unit
    ): Result<BookEntity> {
        val sampleEpubBytes = PdfProcessor.createSampleEpubBytes()
        val extracted = PdfProcessor.extractFromEpubBytes(
            rawBytes = sampleEpubBytes,
            fallbackTitle = "Kathopanishad: Dialogue on Immortal Knowledge (P2P EPUB)"
        )
        return ingestEphemeralExtractedDocument(
            userId = userId,
            extracted = extracted,
            preferredTargetLanguage = preferredTargetLanguage,
            onProgress = onProgress
        )
    }

    suspend fun ingestEphemeralExtractedDocument(
        userId: Long,
        extracted: EphemeralExtractedDocument,
        preferredTargetLanguage: String,
        onProgress: (stage: String, processedPages: Int, totalPages: Int) -> Unit
    ): Result<BookEntity> {
        val credentialResult = resolveUserCredentialForTask(userId, AiTaskKind.OCR)
        val optionalCredential = credentialResult.getOrNull()

        return try {
            val fingerprint = extracted.sourceFingerprintHash
            val existingByHash = dao.getBookByContentHash(fingerprint)
            if (existingByHash != null) {
                PdfProcessor.purgeEphemeralFile(extracted.ephemeralPdfFile)
                if (userId > 0L) {
                    dao.insertUserBookAccess(
                        UserBookAccessEntity(
                            userId = userId,
                            bookId = existingByHash.id,
                            accessType = "OWNER"
                        )
                    )
                }
                return Result.success(existingByHash)
            }

            val engineLabel = if (extracted.format == ExtractedFormat.PDF && optionalCredential != null) {
                "${optionalCredential.providerName} (${optionalCredential.modelName}) + Zero-Host Purge"
            } else {
                "VidyaKosha P2P ${extracted.format.badgeLabel} Extractor (Original Purged)"
            }

            onProgress(
                "Extracting '${extracted.title}' (${extracted.format.code}) via $engineLabel…",
                0,
                extracted.unitCount.coerceAtLeast(1)
            )

            val initialBook = BookEntity(
                title = extracted.title,
                subtitle = "P2P Extracted ${extracted.format.badgeLabel} • Original File Never Hosted",
                authorOrSource = extracted.authorOrSource,
                description = "Converted peer-to-peer into persistent chapters, sections, and paragraphs with immutable SHA-256 content hashes. Original ${extracted.format.code} binary purged immediately after extraction; VidyaKosha owns and retains only the structured OCR and translation derivatives.",
                category = "P2P Knowledge Commons (${extracted.format.code})",
                sourcePdfPath = extracted.originalPurgedUriMarker,
                contentHash = fingerprint,
                totalPages = extracted.unitCount.coerceAtLeast(1),
                fileSizeBytes = 0L, // 0 bytes of original file hosted on server!
                originalLanguage = extracted.detectedLanguage,
                preferredTargetLanguage = preferredTargetLanguage,
                accessPolicy = BookAccessPolicy.PUBLIC,
                rightsCategory = BookRightsCategory.P2P_DERIVATIVE_OWNED,
                isPublicCatalog = true,
                isSampleBook = false,
                isPaid = false,
                priceDisplay = "P2P Commons",
                publisherName = "VidyaKosha P2P Derivative Commons",
                uploadedByUserId = userId.takeIf { it > 0L },
                currentOcrVersion = 1,
                ocrStatus = "IN_PROGRESS",
                ocrProviderUsed = engineLabel,
                ocrProgressPages = 0
            )
            val bookId = dao.insertBook(initialBook)
            if (userId > 0L) {
                dao.insertUserBookAccess(
                    UserBookAccessEntity(
                        userId = userId,
                        bookId = bookId,
                        accessType = "OWNER"
                    )
                )
            }

            val ocrJobId = dao.insertProcessingJob(
                ProcessingJobEntity(
                    jobType = JobType.OCR,
                    status = JobStatus.PROCESSING,
                    bookId = bookId,
                    bookTitle = extracted.title,
                    requestedByUserId = userId,
                    providerAndModel = engineLabel,
                    progressPercent = 20,
                    statusDetail = "Extracting ${extracted.format.code} into structured OCR derivative (original file purged after conversion)…"
                )
            )

            val allExtractedChapters = mutableListOf<OcrChapterDto>()
            val pageEntities = mutableListOf<BookPageEntity>()
            var detectedSourceLang = extracted.detectedLanguage

            val scratchPdf = extracted.ephemeralPdfFile
            if (extracted.format == ExtractedFormat.PDF && scratchPdf != null && scratchPdf.exists() && optionalCredential != null) {
                val batchSize = 2
                val pagesToIngest = extracted.unitCount.coerceAtMost(10)
                var pageIdx = 0
                while (pageIdx < pagesToIngest) {
                    val batchIndices = (pageIdx until minOf(pageIdx + batchSize, pagesToIngest)).toList()
                    val batchPageNums = batchIndices.map { it + 1 }
                    onProgress(
                        "P2P OCR via ${optionalCredential.providerName}: page(s) ${batchPageNums.joinToString(", ")}…",
                        pageIdx,
                        pagesToIngest
                    )

                    val bitmapsBase64 = batchIndices.mapNotNull { idx ->
                        PdfProcessor.renderPageToBitmap(scratchPdf, idx, maxDimension = 1200)?.let { bmp ->
                            PdfProcessor.bitmapToBase64(bmp, quality = 80)
                        }
                    }

                    val ocrResult = MultiProviderAiGateway.performStructuredOcrOnPages(
                        credential = optionalCredential,
                        bookTitle = extracted.title,
                        pageNumbers = batchPageNums,
                        pageImagesBase64 = bitmapsBase64
                    )

                    ocrResult.fold(
                        onSuccess = { dto ->
                            if (dto.detectedLanguage.isNotBlank()) {
                                detectedSourceLang = dto.detectedLanguage
                            }
                            allExtractedChapters.addAll(dto.chapters)
                            batchPageNums.forEach { pNum ->
                                val text = dto.chapters.flatMap { it.sections }.flatMap { it.paragraphs }
                                    .filter { it.pageNumber == pNum }
                                    .joinToString("\n\n") { it.cleanText }
                                pageEntities.add(
                                    BookPageEntity(
                                        bookId = bookId,
                                        pageNumber = pNum,
                                        rawOcrText = text.ifBlank { "Page $pNum extracted." },
                                        detectedSourceLanguage = detectedSourceLang,
                                        ocrConfidenceNote = "P2P OCR by ${optionalCredential.providerName} (Original PDF Purged)"
                                    )
                                )
                            }
                        },
                        onFailure = {
                            // Fall back gracefully to local P2P extracted chapters so user conversion never fails
                            allExtractedChapters.addAll(extracted.preExtractedChapters)
                        }
                    )
                    pageIdx += batchSize
                }
            } else {
                // Direct P2P Extraction for EPUB, Markdown, Plain Text, HTML, P2P JSON, or keyless PDF
                allExtractedChapters.addAll(extracted.preExtractedChapters)
                extracted.preExtractedChapters.forEachIndexed { idx, ch ->
                    val pageNum = idx + 1
                    val joined = ch.sections.flatMap { it.paragraphs }.joinToString("\n\n") { it.cleanText }
                    pageEntities.add(
                        BookPageEntity(
                            bookId = bookId,
                            pageNumber = pageNum,
                            rawOcrText = joined.ifBlank { "Section $pageNum extracted." },
                            detectedSourceLanguage = detectedSourceLang,
                            ocrConfidenceNote = "Extracted via $engineLabel (Original ${extracted.format.code} Never Hosted)"
                        )
                    )
                }
            }

            if (pageEntities.isNotEmpty()) {
                dao.insertBookPages(pageEntities)
            }

            var chapterOrder = 1
            var totalSectionsCount = 0
            var totalParagraphsCount = 0

            val chaptersToSave = allExtractedChapters.ifEmpty {
                extracted.preExtractedChapters.ifEmpty {
                    listOf(
                        OcrChapterDto(
                            chapterTitle = "Chapter 1: ${extracted.title}",
                            sections = listOf(
                                OcrSectionDto(
                                    sectionHeading = "Section 1.1",
                                    paragraphs = listOf(
                                        OcrParagraphDto(1, "PARAGRAPH", "Document extracted into VidyaKosha Commons.")
                                    )
                                )
                            )
                        )
                    )
                }
            }

            for (chDto in chaptersToSave) {
                val pages = chDto.sections.flatMap { it.paragraphs }.map { it.pageNumber }
                val startP = pages.minOrNull() ?: 1
                val endP = pages.maxOrNull() ?: startP
                val chId = dao.insertChapter(
                    ChapterEntity(
                        bookId = bookId,
                        chapterOrder = chapterOrder++,
                        title = chDto.chapterTitle.ifBlank { "Chapter ${chapterOrder - 1}" },
                        summary = chDto.chapterSummary,
                        startPage = startP,
                        endPage = endP
                    )
                )

                var secOrder = 1
                for (secDto in chDto.sections) {
                    val sPages = secDto.paragraphs.map { it.pageNumber }
                    val secId = dao.insertSection(
                        SectionEntity(
                            bookId = bookId,
                            chapterId = chId,
                            sectionOrder = secOrder++,
                            heading = secDto.sectionHeading.ifBlank { "Section ${secOrder - 1}" },
                            startPage = sPages.minOrNull() ?: startP,
                            endPage = sPages.maxOrNull() ?: endP
                        )
                    )
                    totalSectionsCount++

                    val paraEntities = secDto.paragraphs.mapIndexed { idx, pDto ->
                        val cleaned = pDto.cleanText.trim()
                        val hash = SecurityVault.computeContentFingerprint(cleaned)
                        ParagraphEntity(
                            bookId = bookId,
                            chapterId = chId,
                            sectionId = secId,
                            paragraphOrder = idx + 1,
                            originalPageNumber = pDto.pageNumber.coerceAtLeast(1),
                            blockType = pDto.blockType.uppercase(),
                            rawOcrText = cleaned,
                            originalText = cleaned,
                            originalLanguage = detectedSourceLang,
                            normalizedSearchText = cleaned.lowercase(),
                            ocrVersion = 1,
                            sourceContentHash = hash
                        )
                    }
                    val ids = dao.insertParagraphs(paraEntities)
                    totalParagraphsCount += ids.size
                }
            }

            val completedBook = initialBook.copy(
                id = bookId,
                originalLanguage = detectedSourceLang,
                ocrStatus = "COMPLETED",
                ocrProgressPages = extracted.unitCount.coerceAtLeast(1),
                totalChapters = chapterOrder - 1,
                totalSections = totalSectionsCount,
                totalParagraphs = totalParagraphsCount
            )
            dao.updateBook(completedBook)

            dao.updateProcessingJob(
                ProcessingJobEntity(
                    id = ocrJobId,
                    jobType = JobType.OCR,
                    status = JobStatus.COMPLETED,
                    bookId = bookId,
                    bookTitle = completedBook.title,
                    requestedByUserId = userId,
                    providerAndModel = engineLabel,
                    progressPercent = 100,
                    statusDetail = "P2P Extracted ${completedBook.totalChapters} Ch, ${completedBook.totalSections} Sec, ${completedBook.totalParagraphs} Para • Original ${extracted.format.code} Purged (0B Hosted)"
                )
            )

            Result.success(completedBook)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // CRITICAL ZERO-HOST GUARANTEE: Always delete the ephemeral scratch file immediately!
            PdfProcessor.purgeEphemeralFile(extracted.ephemeralPdfFile)
        }
    }

    /**
     * Requirement #12: "Ask This Book" — Book-Grounded Research Tool with Passage Citations & Open Passage!
     * Retrieves actual matching passages across the entire book (`SearchApi.retrieveGroundedPassagesInBook`),
     * attaches clickable `citedParagraphId` and `citationLabel` (`Chapter X, Section Y, Paragraph Z • Page P`),
     * and answers either via the user's configured AI provider OR via built-in deterministic corpus synthesis
     * so "Ask This Book" works immediately even before an external API key is entered!
     */
    suspend fun askThisBookWithCitations(
        userId: Long,
        book: BookEntity,
        targetLanguage: String,
        userQuestion: String
    ): Result<ChatMessageEntity> {
        val groundedPassages = searchApi.retrieveGroundedPassagesInBook(
            bookId = book.id,
            question = userQuestion,
            targetLanguage = targetLanguage
        )
        val primaryMatch = groundedPassages.firstOrNull()
        val primaryChapter = primaryMatch?.first
        val primarySection = primaryMatch?.second
        val primaryParaItem = primaryMatch?.third

        val citationBadge = if (primaryChapter != null && primarySection != null && primaryParaItem != null) {
            "${primaryChapter.title} • ${primarySection.heading} • Paragraph #${primaryParaItem.paragraph.paragraphOrder} (Page ${primaryParaItem.paragraph.originalPageNumber})"
        } else {
            "Book Corpus Reference"
        }

        dao.insertMessage(
            ChatMessageEntity(
                userId = userId,
                bookId = book.id,
                chapterId = primaryChapter?.id,
                sectionId = primarySection?.id,
                citedParagraphId = primaryParaItem?.paragraph?.id,
                citationLabel = citationBadge,
                role = "user",
                content = userQuestion,
                targetLanguage = targetLanguage,
                referencedPage = primaryParaItem?.paragraph?.originalPageNumber ?: 1,
                actionType = "ASK_THIS_BOOK"
            )
        )

        val passagesContextText = groundedPassages.joinToString("\n\n") { (ch, sec, item) ->
            buildString {
                appendLine("[Citation: ${ch.title} | ${sec.heading} | Paragraph #${item.paragraph.paragraphOrder} | Page ${item.paragraph.originalPageNumber}]")
                appendLine("Original (${book.originalLanguage}): ${item.paragraph.originalText}")
                item.transliterationLearning?.let { tl ->
                    appendLine("IAST: ${tl.iastTransliteration}")
                    appendLine("Word Breakdown: ${tl.wordByWordBreakdown}")
                    appendLine("Meaning: ${tl.naturalEnglishTranslation}")
                }
                item.activeTranslation?.let { tr ->
                    appendLine("Translation ($targetLanguage): ${tr.translatedText}")
                }
            }
        }

        val credentialResult = resolveUserCredentialForTask(userId, AiTaskKind.SUMMARY_AND_CHAT)
        if (credentialResult.isSuccess && primaryChapter != null && primarySection != null) {
            val credential = credentialResult.getOrThrow()
            val history = dao.getMessagesListForUserAndBook(userId, book.id)
                .dropLast(1)
                .map { it.role to it.content }

            val aiResponse = MultiProviderAiGateway.chatWithStructuredBook(
                credential = credential,
                bookTitle = book.title,
                chapterTitle = primaryChapter.title,
                sectionHeading = primarySection.heading,
                sourceLanguage = book.originalLanguage,
                targetLanguage = targetLanguage,
                sectionStructuredText = passagesContextText,
                userPrompt = "Answer this question strictly grounded in the book's passages above and cite the exact Chapter, Section, and Paragraph: $userQuestion",
                priorHistory = history
            )

            if (aiResponse.isSuccess) {
                val answerText = buildString {
                    appendLine(aiResponse.getOrThrow())
                    appendLine()
                    appendLine("📖 **Primary Citation:** $citationBadge")
                }
                val msg = ChatMessageEntity(
                    userId = userId,
                    bookId = book.id,
                    chapterId = primaryChapter.id,
                    sectionId = primarySection.id,
                    citedParagraphId = primaryParaItem?.paragraph?.id,
                    citationLabel = citationBadge,
                    role = "assistant",
                    content = answerText,
                    targetLanguage = targetLanguage,
                    providerAndModelUsed = "${credential.providerName} • ${credential.modelName} (Book-Grounded)",
                    referencedPage = primaryParaItem?.paragraph?.originalPageNumber ?: 1,
                    actionType = "ASK_THIS_BOOK"
                )
                val id = dao.insertMessage(msg)
                return Result.success(msg.copy(id = id))
            }
        }

        // Grounded Corpus Synthesis Fallback (works immediately with 0 API calls using stored canonical translations & scholarly notes!)
        val synthesizedAnswer = buildString {
            appendLine("### Grounded Book Research Result ($targetLanguage)")
            if (groundedPassages.isEmpty()) {
                appendLine("No matching passages were found in '${book.title}'.")
            } else {
                groundedPassages.forEachIndexed { idx, (ch, sec, item) ->
                    appendLine("**${idx + 1}. ${ch.title} → ${sec.heading} (Paragraph #${item.paragraph.paragraphOrder}, Page ${item.paragraph.originalPageNumber})**")
                    appendLine("• **Original (${book.originalLanguage}):** ${item.paragraph.originalText}")
                    item.activeTranslation?.let { tr ->
                        appendLine("• **$targetLanguage (${tr.reviewStatus} • v${tr.versionNumber}):** ${tr.translatedText}")
                    }
                    item.transliterationLearning?.let { tl ->
                        appendLine("• **IAST:** *${tl.iastTransliteration}*")
                        appendLine("• **Word Breakdown:** ${tl.wordByWordBreakdown}")
                        appendLine("• **Scholarly Note:** ${tl.scholarlyCommentary}")
                    }
                    appendLine()
                }
                appendLine("💡 *Tap **Open Passage** below to jump directly to the cited paragraph in the Bilingual Book Reader.*")
            }
        }

        val groundedMsg = ChatMessageEntity(
            userId = userId,
            bookId = book.id,
            chapterId = primaryChapter?.id,
            sectionId = primarySection?.id,
            citedParagraphId = primaryParaItem?.paragraph?.id,
            citationLabel = citationBadge,
            role = "assistant",
            content = synthesizedAnswer.trim(),
            targetLanguage = targetLanguage,
            providerAndModelUsed = "Polyglot Book-Grounded Citation Engine",
            referencedPage = primaryParaItem?.paragraph?.originalPageNumber ?: 1,
            actionType = "ASK_THIS_BOOK"
        )
        val id = dao.insertMessage(groundedMsg)
        return Result.success(groundedMsg.copy(id = id))
    }

    suspend fun clearUserBookChat(userId: Long, book: BookEntity) {
        dao.clearMessagesForUserAndBook(userId, book.id)
    }

    // =====================================================================
    // 8. SUPERADMIN BACKEND FLOWS & DELEGATING OPERATIONS
    // =====================================================================

    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()

    fun getAllEntitlementsInSystemFlow(): Flow<List<UserEntitlementEntity>> = dao.getAllEntitlementsInSystemFlow()

    fun getAllCommercialPlansFlow(): Flow<List<CommercialPlanEntity>> = dao.getAllCommercialPlansFlow()

    fun getEnabledCommercialPlansFlow(): Flow<List<CommercialPlanEntity>> = dao.getEnabledCommercialPlansFlow()

    fun getAllPlatformMetadataFlow(): Flow<List<PlatformMetadataEntity>> = dao.getAllPlatformMetadataFlow()

    fun getAllAdminAuditLogsFlow(): Flow<List<AdminAuditLogEntity>> = dao.getAllAdminAuditLogsFlow()

    suspend fun authenticateDefaultSuperadminSession(): Result<UserEntity> {
        ensurePublicCatalogInitialized()
        return loginUser(
            email = SampleAnthologySeeder.DEFAULT_SUPERADMIN_EMAIL,
            password = SampleAnthologySeeder.DEFAULT_SUPERADMIN_PASSWORD
        )
    }
}
