package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PolyglotDao {

    // =====================================================================
    // 1. USER AUTHENTICATION, SESSIONS, ENTITLEMENTS & CONTRIBUTOR CONSENT
    // =====================================================================

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity): Long

    @Query("SELECT * FROM user_sessions WHERE userId = :userId AND token = :token AND expiresAt > :now LIMIT 1")
    suspend fun getValidSession(userId: Long, token: String, now: Long = System.currentTimeMillis()): UserSessionEntity?

    @Query("DELETE FROM user_sessions WHERE userId = :userId")
    suspend fun clearSessionsForUser(userId: Long)

    // --- Entitlements (#18, #19 & Superadmin Central Entitlements) ---
    @Query("SELECT * FROM user_entitlements WHERE userId = :userId AND status = 'ACTIVE' ORDER BY grantedAt DESC")
    fun getActiveEntitlementsForUserFlow(userId: Long): Flow<List<UserEntitlementEntity>>

    @Query("SELECT * FROM user_entitlements WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun getActiveEntitlementsForUser(userId: Long): List<UserEntitlementEntity>

    @Query("SELECT * FROM user_entitlements WHERE userId = :userId ORDER BY grantedAt DESC")
    fun getAllEntitlementsForUserFlow(userId: Long): Flow<List<UserEntitlementEntity>>

    @Query("SELECT * FROM user_entitlements WHERE userId = :userId ORDER BY grantedAt DESC")
    suspend fun getAllEntitlementsForUser(userId: Long): List<UserEntitlementEntity>

    @Query("SELECT * FROM user_entitlements ORDER BY grantedAt DESC")
    fun getAllEntitlementsInSystemFlow(): Flow<List<UserEntitlementEntity>>

    @Query("SELECT * FROM user_entitlements ORDER BY grantedAt DESC")
    suspend fun getAllEntitlementsInSystem(): List<UserEntitlementEntity>

    @Query("SELECT * FROM user_entitlements WHERE id = :entitlementId LIMIT 1")
    suspend fun getEntitlementById(entitlementId: Long): UserEntitlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntitlement(entitlement: UserEntitlementEntity): Long

    @Update
    suspend fun updateEntitlement(entitlement: UserEntitlementEntity)

    @Query("DELETE FROM user_entitlements WHERE userId = :userId AND entitlementKind = 'SUBSCRIPTION_TIER'")
    suspend fun clearTierEntitlementsForUser(userId: Long)

    // --- Contributor Consents (#3) ---
    @Query("SELECT * FROM contributor_consents WHERE userId = :userId ORDER BY acceptedAt DESC")
    fun getContributorConsentsForUserFlow(userId: Long): Flow<List<ContributorConsentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributorConsent(consent: ContributorConsentEntity): Long

    // =====================================================================
    // 2. USER-OWNED AI CONFIGURATIONS (ISOLATED BY USER ID)
    // =====================================================================

    @Query("SELECT * FROM user_ai_configs WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAiConfigsForUserFlow(userId: Long): Flow<List<UserAiConfigEntity>>

    @Query("SELECT * FROM user_ai_configs WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAiConfigsForUser(userId: Long): List<UserAiConfigEntity>

    @Query("SELECT * FROM user_ai_configs WHERE id = :configId AND userId = :userId LIMIT 1")
    suspend fun getAiConfigById(configId: Long, userId: Long): UserAiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiConfig(config: UserAiConfigEntity): Long

    @Update
    suspend fun updateAiConfig(config: UserAiConfigEntity)

    @Query("DELETE FROM user_ai_configs WHERE id = :configId AND userId = :userId")
    suspend fun deleteAiConfig(configId: Long, userId: Long)

    @Query("UPDATE user_ai_configs SET isDefaultForOcr = 0 WHERE userId = :userId")
    suspend fun clearDefaultOcrForUser(userId: Long)

    @Query("UPDATE user_ai_configs SET isDefaultForTranslation = 0 WHERE userId = :userId")
    suspend fun clearDefaultTranslationForUser(userId: Long)

    @Query("UPDATE user_ai_configs SET isDefaultForSummaryAndChat = 0 WHERE userId = :userId")
    suspend fun clearDefaultChatForUser(userId: Long)

    // =====================================================================
    // 3. BOOKS, ACCESS CONTROL CATALOGUE & USER OWNERSHIP (#4)
    // =====================================================================

    @Query("SELECT * FROM books ORDER BY isPaid ASC, createdAt ASC")
    fun getAllBooksInSystemFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY isPaid ASC, createdAt ASC")
    suspend fun getAllBooksInSystem(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isPublicCatalog = 1 ORDER BY isPaid ASC, createdAt ASC")
    fun getPublicDirectoryBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isPublicCatalog = 1 ORDER BY isPaid ASC, createdAt ASC")
    suspend fun getPublicDirectoryBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isSampleBook = 1 LIMIT 1")
    suspend fun getSampleBook(): BookEntity?

    @Query(
        """
        SELECT b.* FROM books b
        LEFT JOIN user_book_access uba ON b.id = uba.bookId AND uba.userId = :userId
        WHERE b.uploadedByUserId = :userId OR uba.userId = :userId OR b.accessPolicy IN ('PUBLIC', 'FREE')
        ORDER BY b.lastReadAt DESC
        """
    )
    fun getAccessibleBooksForUserFlow(userId: Long): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE contentHash = :hash AND contentHash != '' LIMIT 1")
    suspend fun getBookByContentHash(hash: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: Long)

    @Query("SELECT * FROM user_book_access WHERE userId = :userId")
    fun getUserBookAccessFlow(userId: Long): Flow<List<UserBookAccessEntity>>

    @Query("SELECT * FROM user_book_access WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getUserAccessForBook(userId: Long, bookId: Long): UserBookAccessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBookAccess(access: UserBookAccessEntity): Long

    // =====================================================================
    // 4. STRUCTURED OCR CONTENT, VERSIONING & CORRECTION AUDIT TRAIL (#8, #16)
    // =====================================================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookPages(pages: List<BookPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterOrder ASC")
    fun getChaptersForBookFlow(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterOrder ASC")
    suspend fun getChaptersForBook(bookId: Long): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Query("SELECT * FROM sections WHERE bookId = :bookId ORDER BY chapterId ASC, sectionOrder ASC")
    fun getSectionsForBookFlow(bookId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY sectionOrder ASC")
    suspend fun getSectionsForChapter(chapterId: Long): List<SectionEntity>

    @Query("SELECT * FROM sections WHERE id = :sectionId LIMIT 1")
    suspend fun getSectionById(sectionId: Long): SectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraphs(paragraphs: List<ParagraphEntity>): List<Long>

    @Update
    suspend fun updateParagraph(paragraph: ParagraphEntity)

    @Query("SELECT * FROM paragraphs WHERE id = :paragraphId LIMIT 1")
    suspend fun getParagraphById(paragraphId: Long): ParagraphEntity?

    @Query("SELECT * FROM paragraphs WHERE sectionId = :sectionId ORDER BY paragraphOrder ASC")
    fun getParagraphsForSectionFlow(sectionId: Long): Flow<List<ParagraphEntity>>

    @Query("SELECT * FROM paragraphs WHERE sectionId = :sectionId ORDER BY paragraphOrder ASC")
    suspend fun getParagraphsForSection(sectionId: Long): List<ParagraphEntity>

    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY id ASC")
    fun getParagraphsForBookFlow(bookId: Long): Flow<List<ParagraphEntity>>

    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY id ASC")
    suspend fun getParagraphsForBook(bookId: Long): List<ParagraphEntity>

    @Query("SELECT * FROM paragraphs ORDER BY id ASC")
    suspend fun getAllParagraphsInSystem(): List<ParagraphEntity>

    @Query(
        """
        SELECT * FROM paragraphs 
        WHERE bookId = :bookId 
        AND (normalizedSearchText LIKE '%' || LOWER(:query) || '%' OR originalText LIKE '%' || :query || '%')
        ORDER BY originalPageNumber ASC, paragraphOrder ASC
        """
    )
    suspend fun searchParagraphsInBook(bookId: Long, query: String): List<ParagraphEntity>

    // --- OCR Correction Audit Trail (#16) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrCorrectionAudit(audit: OcrCorrectionAuditEntity): Long

    @Query("SELECT * FROM ocr_correction_audits WHERE paragraphId = :paragraphId ORDER BY correctedAt DESC")
    suspend fun getOcrAuditsForParagraph(paragraphId: Long): List<OcrCorrectionAuditEntity>

    @Query("SELECT * FROM ocr_correction_audits WHERE bookId = :bookId ORDER BY correctedAt DESC")
    fun getOcrAuditsForBookFlow(bookId: Long): Flow<List<OcrCorrectionAuditEntity>>

    // =====================================================================
    // 5. SHARED TRANSLATION REPOSITORY, VERSIONING, MEMORY & QUALITY (#1, #7, #9)
    // =====================================================================

    @Query(
        """
        SELECT * FROM paragraph_translations 
        WHERE bookId = :bookId AND targetLanguage = :targetLanguage AND isActiveVersion = 1
        """
    )
    fun getActiveTranslationsForBookFlow(
        bookId: Long,
        targetLanguage: String
    ): Flow<List<ParagraphTranslationEntity>>

    @Query(
        """
        SELECT * FROM paragraph_translations 
        WHERE bookId = :bookId AND targetLanguage = :targetLanguage
        ORDER BY paragraphId ASC, versionNumber DESC
        """
    )
    fun getAllTranslationVersionsForBookFlow(
        bookId: Long,
        targetLanguage: String
    ): Flow<List<ParagraphTranslationEntity>>

    @Query("SELECT * FROM paragraph_translations ORDER BY createdAt DESC")
    fun getAllCanonicalTranslationsInSystemFlow(): Flow<List<ParagraphTranslationEntity>>

    @Query("SELECT * FROM paragraph_translations ORDER BY createdAt DESC")
    suspend fun getAllCanonicalTranslationsInSystem(): List<ParagraphTranslationEntity>

    @Query(
        """
        SELECT * FROM paragraph_translations 
        WHERE sectionId = :sectionId AND targetLanguage = :targetLanguage AND isActiveVersion = 1
        """
    )
    fun getActiveTranslationsForSectionFlow(
        sectionId: Long,
        targetLanguage: String
    ): Flow<List<ParagraphTranslationEntity>>

    @Query(
        """
        SELECT * FROM paragraph_translations 
        WHERE paragraphId = :paragraphId AND targetLanguage = :targetLanguage 
        ORDER BY versionNumber DESC
        """
    )
    suspend fun getTranslationVersionsForParagraph(
        paragraphId: Long,
        targetLanguage: String
    ): List<ParagraphTranslationEntity>

    @Query(
        """
        SELECT * FROM paragraph_translations 
        WHERE paragraphId = :paragraphId AND targetLanguage = :targetLanguage AND isActiveVersion = 1
        LIMIT 1
        """
    )
    suspend fun getPreferredTranslationForParagraph(
        paragraphId: Long,
        targetLanguage: String
    ): ParagraphTranslationEntity?

    @Query(
        """
        UPDATE paragraph_translations 
        SET isActiveVersion = 0 
        WHERE paragraphId = :paragraphId AND targetLanguage = :targetLanguage
        """
    )
    suspend fun deactivateOldTranslationsForParagraph(paragraphId: Long, targetLanguage: String)

    @Query(
        """
        UPDATE paragraph_translations 
        SET isActiveVersion = 1, updatedAt = :now
        WHERE id = :translationId
        """
    )
    suspend fun markTranslationAsPreferred(translationId: Long, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE paragraph_translations 
        SET reviewStatus = :newStatus, reviewedByDisplayName = :reviewerName, updatedAt = :now
        WHERE id = :translationId
        """
    )
    suspend fun updateTranslationReviewStatus(
        translationId: Long,
        newStatus: String,
        reviewerName: String,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM paragraph_translations WHERE id = :translationId LIMIT 1")
    suspend fun getTranslationById(translationId: Long): ParagraphTranslationEntity?

    @Query(
        """
        UPDATE paragraph_translations 
        SET accessPolicy = :newAccessPolicy, updatedAt = :now
        WHERE id = :translationId
        """
    )
    suspend fun updateTranslationAccessPolicy(
        translationId: Long,
        newAccessPolicy: String,
        now: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE paragraph_translations 
        SET reuseCount = reuseCount + 1, updatedAt = :now
        WHERE id = :translationId
        """
    )
    suspend fun incrementTranslationReuseCount(translationId: Long, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: ParagraphTranslationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslations(translations: List<ParagraphTranslationEntity>)

    @Query("SELECT DISTINCT targetLanguage FROM paragraph_translations WHERE bookId = :bookId")
    fun getCachedTranslationLanguagesForBookFlow(bookId: Long): Flow<List<String>>

    // --- Translation Memory (#9) ---
    @Query(
        """
        SELECT * FROM translation_memory 
        WHERE normalizedSourceHash = :sourceHash 
        AND LOWER(sourceLanguage) = LOWER(:sourceLang) 
        AND LOWER(targetLanguage) = LOWER(:targetLang)
        LIMIT 1
        """
    )
    suspend fun findExactTranslationMemoryMatch(
        sourceHash: String,
        sourceLang: String,
        targetLang: String
    ): TranslationMemoryEntity?

    @Query("SELECT * FROM translation_memory ORDER BY matchReuseCount DESC, updatedAt DESC")
    fun getAllTranslationMemoryFlow(): Flow<List<TranslationMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationMemory(entry: TranslationMemoryEntity): Long

    @Query("UPDATE translation_memory SET matchReuseCount = matchReuseCount + 1, updatedAt = :now WHERE id = :tmId")
    suspend fun incrementTranslationMemoryReuse(tmId: Long, now: Long = System.currentTimeMillis())

    // --- Translation Issue Reports (#7, #20) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationIssueReport(report: TranslationIssueReportEntity): Long

    @Query("SELECT * FROM translation_issue_reports ORDER BY createdAt DESC")
    fun getAllTranslationIssueReportsFlow(): Flow<List<TranslationIssueReportEntity>>

    @Query("UPDATE translation_issue_reports SET resolutionStatus = :status WHERE id = :reportId")
    suspend fun updateTranslationIssueStatus(reportId: Long, status: String)

    // --- Transliteration & Scholarly Learning Layer (#14, #15) ---
    @Query("SELECT * FROM transliteration_learning WHERE bookId = :bookId")
    fun getTransliterationsForBookFlow(bookId: Long): Flow<List<TransliterationLearningEntity>>

    @Query("SELECT * FROM transliteration_learning")
    suspend fun getAllTransliterationsInSystem(): List<TransliterationLearningEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransliterationLearning(item: TransliterationLearningEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransliterationList(items: List<TransliterationLearningEntity>)

    // =====================================================================
    // 6. PERSISTENT PROCESSING JOB QUEUE, OFFLINE PACKAGES & ANALYTICS (#10, #17, #21)
    // =====================================================================

    @Query("SELECT * FROM processing_jobs ORDER BY createdAt DESC LIMIT 40")
    fun getRecentProcessingJobsFlow(): Flow<List<ProcessingJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessingJob(job: ProcessingJobEntity): Long

    @Update
    suspend fun updateProcessingJob(job: ProcessingJobEntity)

    @Query("SELECT * FROM offline_book_packages WHERE userId = :userId ORDER BY downloadedAt DESC")
    fun getOfflinePackagesForUserFlow(userId: Long): Flow<List<OfflineBookPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOfflineBookPackage(pkg: OfflineBookPackageEntity): Long

    @Query("DELETE FROM offline_book_packages WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeOfflineBookPackage(userId: Long, bookId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyticsEvent(event: CorpusAnalyticsEventEntity): Long

    @Query("SELECT * FROM corpus_analytics_events ORDER BY timestamp DESC")
    fun getAllAnalyticsEventsFlow(): Flow<List<CorpusAnalyticsEventEntity>>

    // =====================================================================
    // 7. PER-USER ISOLATED READING PROGRESS, BOOKMARKS, HISTORY & CHAT
    // =====================================================================

    @Query("SELECT * FROM reading_progress WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllReadingProgressForUserFlow(userId: Long): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentReadingProgressForUser(userId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getUserReadingProgressForBook(userId: Long, bookId: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    @Query("SELECT * FROM bookmarks_highlights WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllBookmarksForUserFlow(userId: Long): Flow<List<BookmarkHighlightEntity>>

    @Query("SELECT * FROM bookmarks_highlights WHERE userId = :userId AND bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForUserAndBookFlow(userId: Long, bookId: Long): Flow<List<BookmarkHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarkHighlight(item: BookmarkHighlightEntity): Long

    @Query("DELETE FROM bookmarks_highlights WHERE userId = :userId AND paragraphId = :paragraphId")
    suspend fun removeBookmarkForUserAndParagraph(userId: Long, paragraphId: Long)

    @Query("SELECT * FROM reading_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT 25")
    fun getReadingHistoryForUserFlow(userId: Long): Flow<List<ReadingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingHistory(entry: ReadingHistoryEntity)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND bookId = :bookId ORDER BY timestamp ASC")
    fun getMessagesForUserAndBookFlow(userId: Long, bookId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getMessagesListForUserAndBook(userId: Long, bookId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE userId = :userId AND bookId = :bookId")
    suspend fun clearMessagesForUserAndBook(userId: Long, bookId: Long)

    // =====================================================================
    // 8. SUPERADMIN BACKEND: COMMERCIAL PLANS, PLATFORM METADATA & IMMUTABLE AUDIT LOGS
    // =====================================================================

    @Query("SELECT * FROM commercial_plans ORDER BY displayOrder ASC, id ASC")
    fun getAllCommercialPlansFlow(): Flow<List<CommercialPlanEntity>>

    @Query("SELECT * FROM commercial_plans ORDER BY displayOrder ASC, id ASC")
    suspend fun getAllCommercialPlans(): List<CommercialPlanEntity>

    @Query("SELECT * FROM commercial_plans WHERE isEnabled = 1 ORDER BY displayOrder ASC, id ASC")
    fun getEnabledCommercialPlansFlow(): Flow<List<CommercialPlanEntity>>

    @Query("SELECT * FROM commercial_plans WHERE isEnabled = 1 ORDER BY displayOrder ASC, id ASC")
    suspend fun getEnabledCommercialPlans(): List<CommercialPlanEntity>

    @Query("SELECT * FROM commercial_plans WHERE UPPER(planCode) = UPPER(:planCode) LIMIT 1")
    suspend fun getCommercialPlanByCode(planCode: String): CommercialPlanEntity?

    @Query("SELECT * FROM commercial_plans WHERE id = :planId LIMIT 1")
    suspend fun getCommercialPlanById(planId: Long): CommercialPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommercialPlan(plan: CommercialPlanEntity): Long

    @Update
    suspend fun updateCommercialPlan(plan: CommercialPlanEntity)

    @Query("SELECT * FROM platform_metadata ORDER BY metadataType ASC, sortOrder ASC, displayLabel ASC")
    fun getAllPlatformMetadataFlow(): Flow<List<PlatformMetadataEntity>>

    @Query("SELECT * FROM platform_metadata ORDER BY metadataType ASC, sortOrder ASC, displayLabel ASC")
    suspend fun getAllPlatformMetadata(): List<PlatformMetadataEntity>

    @Query("SELECT * FROM platform_metadata WHERE metadataType = :type ORDER BY sortOrder ASC, displayLabel ASC")
    fun getPlatformMetadataByTypeFlow(type: String): Flow<List<PlatformMetadataEntity>>

    @Query("SELECT * FROM platform_metadata WHERE metadataType = :type AND UPPER(keyCode) = UPPER(:keyCode) LIMIT 1")
    suspend fun getPlatformMetadataByKey(type: String, keyCode: String): PlatformMetadataEntity?

    @Query("SELECT * FROM platform_metadata WHERE id = :metadataId LIMIT 1")
    suspend fun getPlatformMetadataById(metadataId: Long): PlatformMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformMetadata(item: PlatformMetadataEntity): Long

    @Update
    suspend fun updatePlatformMetadata(item: PlatformMetadataEntity)

    // Immutable Administrative Audit Trail (Strictly Append-Only: NO update or delete methods)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdminAuditLog(log: AdminAuditLogEntity): Long

    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC")
    fun getAllAdminAuditLogsFlow(): Flow<List<AdminAuditLogEntity>>

    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAdminAuditLogs(): List<AdminAuditLogEntity>
}

@Database(
    entities = [
        UserEntity::class,
        UserSessionEntity::class,
        UserEntitlementEntity::class,
        ContributorConsentEntity::class,
        UserAiConfigEntity::class,
        BookEntity::class,
        UserBookAccessEntity::class,
        BookPageEntity::class,
        ChapterEntity::class,
        SectionEntity::class,
        ParagraphEntity::class,
        OcrCorrectionAuditEntity::class,
        ParagraphTranslationEntity::class,
        TranslationMemoryEntity::class,
        TranslationIssueReportEntity::class,
        TransliterationLearningEntity::class,
        ProcessingJobEntity::class,
        OfflineBookPackageEntity::class,
        CorpusAnalyticsEventEntity::class,
        BookmarkHighlightEntity::class,
        ReadingProgressEntity::class,
        ReadingHistoryEntity::class,
        ChatMessageEntity::class,
        CommercialPlanEntity::class,
        PlatformMetadataEntity::class,
        AdminAuditLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun polyglotDao(): PolyglotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'")
                db.execSQL("ALTER TABLE users ADD COLUMN accountStatus TEXT NOT NULL DEFAULT 'ACTIVE'")

                db.execSQL("ALTER TABLE user_entitlements ADD COLUMN grantedByAdminId INTEGER")
                db.execSQL("ALTER TABLE user_entitlements ADD COLUMN grantedByAdminName TEXT NOT NULL DEFAULT 'System'")
                db.execSQL("ALTER TABLE user_entitlements ADD COLUMN reasonOrNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_entitlements ADD COLUMN revokedAt INTEGER")

                db.execSQL("ALTER TABLE books ADD COLUMN contributors TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN genresCsv TEXT NOT NULL DEFAULT 'Philosophy, Classical Literature'")
                db.execSQL("ALTER TABLE books ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT 'Sanskrit, Bilingual, Canonical'")
                db.execSQL("ALTER TABLE books ADD COLUMN coverImageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN workId TEXT NOT NULL DEFAULT 'WORK-DEFAULT'")
                db.execSQL("ALTER TABLE books ADD COLUMN editionLabel TEXT NOT NULL DEFAULT '1st Digital Critical Edition'")
                db.execSQL("ALTER TABLE books ADD COLUMN isbnOrIdentifier TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE books ADD COLUMN publicationYear TEXT NOT NULL DEFAULT '2026'")
                db.execSQL("ALTER TABLE books ADD COLUMN publicationInfo TEXT NOT NULL DEFAULT 'VidyaKosha Digital Archival Press'")
                db.execSQL("ALTER TABLE books ADD COLUMN customMetadataJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE books ADD COLUMN publicationStatus TEXT NOT NULL DEFAULT 'PUBLISHED'")
                db.execSQL("ALTER TABLE books ADD COLUMN searchIndexStatus TEXT NOT NULL DEFAULT 'INDEXED'")
                db.execSQL("ALTER TABLE books ADD COLUMN requiredPlanCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_books_workId ON books(workId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS commercial_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        planCode TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        monthlyPriceAmount REAL NOT NULL,
                        yearlyPriceAmount REAL NOT NULL,
                        currencyCode TEXT NOT NULL,
                        trialDays INTEGER NOT NULL,
                        displayOrder INTEGER NOT NULL,
                        isFeatured INTEGER NOT NULL,
                        featuresCsv TEXT NOT NULL,
                        allowedBookAccessPoliciesCsv TEXT NOT NULL,
                        dailyTranslationLimit INTEGER NOT NULL,
                        monthlyManagedAiTokensLimit INTEGER NOT NULL,
                        offlineAccessEnabled INTEGER NOT NULL,
                        advancedSearchAndScholarlyEnabled INTEGER NOT NULL,
                        ttsAudioDailyMinutesLimit INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_commercial_plans_planCode ON commercial_plans(planCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_commercial_plans_displayOrder ON commercial_plans(displayOrder)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS platform_metadata (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        metadataType TEXT NOT NULL,
                        keyCode TEXT NOT NULL,
                        displayLabel TEXT NOT NULL,
                        description TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        extraAttributesJson TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_platform_metadata_metadataType_keyCode ON platform_metadata(metadataType, keyCode)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS admin_audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        adminUserId INTEGER NOT NULL,
                        adminEmailOrName TEXT NOT NULL,
                        adminRole TEXT NOT NULL,
                        action TEXT NOT NULL,
                        entityAffected TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        previousValue TEXT NOT NULL,
                        newValue TEXT NOT NULL,
                        reasonOrNote TEXT NOT NULL,
                        deviceOrContextInfo TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_admin_audit_logs_timestamp ON admin_audit_logs(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_admin_audit_logs_entityAffected_entityId ON admin_audit_logs(entityAffected, entityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_admin_audit_logs_adminUserId ON admin_audit_logs(adminUserId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polyglot_enterprise_corpus_db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
