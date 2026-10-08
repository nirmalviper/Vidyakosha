package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.BookAccessPolicy
import com.example.data.local.BookRightsCategory
import com.example.data.local.SubscriptionTier
import com.example.data.local.TranslationReviewStatus
import com.example.data.repository.PolyglotRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: PolyglotRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PolyglotRepository(
            dao = database.polyglotDao(),
            appContext = context
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sharedTranslationRepository_reusesExistingTranslationsAndEnforcesAccessAndVersioning() = runBlocking {
        // 1. Seed the public anthology and verify structured OCR entities
        val sampleBook = repository.ensurePublicCatalogInitialized()
        assertNotNull(sampleBook)
        assertEquals(BookAccessPolicy.PUBLIC, sampleBook.accessPolicy)
        assertEquals(BookRightsCategory.PUBLIC_DOMAIN, sampleBook.rightsCategory)

        val chapters = repository.getChaptersForBook(sampleBook.id)
        assertTrue(chapters.isNotEmpty())
        val firstSection = repository.getSectionsForChapter(chapters.first().id).first()
        val paragraphs = database.polyglotDao().getParagraphsForSection(firstSection.id)
        assertTrue(paragraphs.isNotEmpty())

        // 2. Verify Shared Translation Repository lookup BEFORE any AI call
        val existingGujarati = repository.translationApi.findOrReuseExistingTranslationForParagraph(
            userId = 0L,
            entitlements = emptyList(),
            book = sampleBook,
            paragraph = paragraphs.first(),
            targetLanguage = "Gujarati"
        )
        assertNotNull("Expected pre-seeded Gujarati translation in Shared Repository", existingGujarati)
        assertTrue(existingGujarati!!.translatedText.isNotBlank())

        // 3. Register a user and verify Book Access Catalogue filtering (RESTRICTED hidden from Free tier)
        val userResult = repository.registerUser(
            displayName = "Aarav Scholar",
            email = "aarav@polyglot.org",
            password = "password123",
            preferredLanguage = "Gujarati"
        )
        assertTrue(userResult.isSuccess)
        val user = userResult.getOrThrow()

        val allSystemBooks = database.polyglotDao().getAllBooksInSystem()
        val freeEntitlements = database.polyglotDao().getActiveEntitlementsForUser(user.id)
        val freeDiscoverable = repository.bookApi.filterDiscoverableBooks(
            allBooks = allSystemBooks,
            userId = user.id,
            entitlements = freeEntitlements
        )
        assertFalse(
            "Free user should not discover RESTRICTED research books",
            freeDiscoverable.any { it.accessPolicy == BookAccessPolicy.RESTRICTED }
        )

        // Upgrade user to PRO_RESEARCH and verify RESTRICTED books become discoverable
        repository.switchUserSubscriptionTier(user, SubscriptionTier.PRO_RESEARCH)
        val proEntitlements = database.polyglotDao().getActiveEntitlementsForUser(user.id)
        val proDiscoverable = repository.bookApi.filterDiscoverableBooks(
            allBooks = allSystemBooks,
            userId = user.id,
            entitlements = proEntitlements
        )
        assertTrue(
            "Pro/Research user should discover RESTRICTED research books",
            proDiscoverable.any { it.accessPolicy == BookAccessPolicy.RESTRICTED }
        )

        // 4. Verify Human Editorial Review creates a new permanent version without deleting existing versions
        val initialVersions = database.polyglotDao().getTranslationVersionsForParagraph(paragraphs.first().id, "Gujarati")
        assertTrue("Sample paragraph should have pre-seeded canonical versions", initialVersions.isNotEmpty())
        val nextExpectedVersion = (initialVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1

        val newHumanReviewed = repository.submitHumanReviewedTranslationVersion(
            userId = user.id,
            reviewerName = user.displayName,
            book = sampleBook,
            paragraph = paragraphs.first(),
            targetLanguage = "Gujarati",
            correctedTranslationText = "માનવ સમીક્ષિત ગુજરાતી અનુવાદ (Human Reviewed)",
            notes = "Verified by editorial board"
        )
        assertEquals(nextExpectedVersion, newHumanReviewed.versionNumber)
        assertEquals(TranslationReviewStatus.HUMAN_REVIEWED, newHumanReviewed.reviewStatus)

        val allVersions = database.polyglotDao().getTranslationVersionsForParagraph(paragraphs.first().id, "Gujarati")
        assertEquals("All previous versions plus new version must remain permanently stored", initialVersions.size + 1, allVersions.size)

        // 5. Verify OCR Correction preserves rawOcrText (v1) and increments ocrVersion to v2
        val originalRaw = paragraphs.first().rawOcrText
        val ocrUpdateResult = repository.submitOcrCorrection(
            paragraphId = paragraphs.first().id,
            correctedText = paragraphs.first().originalText + " [OCR Verified]",
            userId = user.id,
            userName = user.displayName,
            reason = "Added diacritic verification mark"
        )
        assertTrue(ocrUpdateResult.isSuccess)
        val updatedPara = ocrUpdateResult.getOrThrow()
        assertEquals(2, updatedPara.ocrVersion)
        assertEquals("Raw OCR v1 must never be overwritten", originalRaw, updatedPara.rawOcrText)

        // 6. Verify 100+ Language Catalogue & Top 3 Languages User Setting Persistence
        assertTrue(
            "LanguageRegistry should provide at least 90 Indian, Classical, and Global languages",
            com.example.data.local.LanguageRegistry.ALL_LANGUAGES.size >= 90
        )
        val chosenFour = listOf(
            com.example.data.local.LanguageRegistry.findByNameOrCode("Sanskrit"),
            com.example.data.local.LanguageRegistry.findByNameOrCode("Tamil"),
            com.example.data.local.LanguageRegistry.findByNameOrCode("Japanese"),
            com.example.data.local.LanguageRegistry.findByNameOrCode("French")
        )
        val (updatedUserWithTop3, normalizedTopThree) = repository.updateUserTopThreeLanguages(user, chosenFour)
        assertEquals("User active language list must be capped at 3", 3, normalizedTopThree.size)
        assertEquals("Sanskrit,Tamil,Japanese", updatedUserWithTop3?.topThreeLanguagesCsv)
        assertEquals(
            listOf("Sanskrit", "Tamil", "Japanese"),
            repository.getRestoredTopThreeLanguages(updatedUserWithTop3).map { it.name }
        )

        // 7. Verify Zero-Host Peer-to-Peer EPUB & Markdown Extraction + Lipi Derivative Ownership
        val epubResult = repository.ingestSampleEpubForP2PCommons(
            userId = user.id,
            preferredTargetLanguage = "Gujarati",
            onProgress = { _, _, _ -> }
        )
        assertTrue("P2P EPUB extraction should succeed", epubResult.isSuccess)
        val epubBook = epubResult.getOrThrow()
        assertEquals(BookRightsCategory.P2P_DERIVATIVE_OWNED, epubBook.rightsCategory)
        assertTrue(
            "Lipi must own and allow shared repository contribution for P2P derivative works",
            BookRightsCategory.allowsSharedRepositoryContribution(epubBook.rightsCategory)
        )
        assertEquals(
            "Original EPUB binary must never be hosted on server (0 bytes stored)",
            0L,
            epubBook.fileSizeBytes
        )
        assertTrue(
            "Source path must record P2P ephemeral purge marker",
            epubBook.sourcePdfPath.startsWith("P2P_EPHEMERAL_PURGED://EPUB/")
        )
        val epubChapters = repository.getChaptersForBook(epubBook.id)
        assertEquals("Sample Kathopanishad EPUB should extract 2 structured chapters", 2, epubChapters.size)

        // Also verify Markdown (.md) P2P extraction
        val markdownBytes = "# Chapter 1: Sankhya Yoga\n## Section 1.1: Imperishable Knowledge\nन हि ज्ञानेन सदृशं पवित्रमिह विद्यते।\n\nThere is nothing in this world as purifying as true knowledge (Gyan).".toByteArray()
        val mdExtracted = com.example.util.PdfProcessor.extractFromTextOrHtmlBytes(
            rawBytes = markdownBytes,
            title = "Gita Sankhya Notes",
            format = com.example.util.ExtractedFormat.MARKDOWN
        )
        val mdBookResult = repository.ingestEphemeralExtractedDocument(
            userId = 0L, // Works even for guest/keyless peer at their free will!
            extracted = mdExtracted,
            preferredTargetLanguage = "Hindi",
            onProgress = { _, _, _ -> }
        )
        assertTrue(mdBookResult.isSuccess)
        val mdBook = mdBookResult.getOrThrow()
        assertEquals(0L, mdBook.fileSizeBytes)
        assertEquals(BookRightsCategory.P2P_DERIVATIVE_OWNED, mdBook.rightsCategory)

        // 8. Verify 12-Page Multi-Page PDF Extraction & OCR across Page 6+ without memory exhaustion or truncation
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        val multiPagePdfFile = java.io.File(context.cacheDir, "test_12_page_book.pdf")
        com.tom_roush.pdfbox.pdmodel.PDDocument().use { doc ->
            for (p in 1..12) {
                val page = com.tom_roush.pdfbox.pdmodel.PDPage()
                doc.addPage(page)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                    cs.newLineAtOffset(50f, 700f)
                    cs.showText("Page $p of 12: Lipi Deterministic Full-Document PDF Extraction Verification.")
                    cs.endText()
                }
            }
            doc.save(multiPagePdfFile)
        }
        val multiPageExtraction = com.example.util.LocalOcrAndTranslationEngine.extractAllPagesFromPdfDeterministically(
            context = context,
            pdfFile = multiPagePdfFile,
            fallbackTitle = "Twelve Page Test Treatise",
            fingerprintPrefix = "test12p"
        )
        assertTrue("12-page PDF extraction including page 6+ must succeed", multiPageExtraction.isSuccess)
        val extracted12 = multiPageExtraction.getOrThrow()
        assertEquals("All 12 pages must be extracted without stopping at page 6", 12, extracted12.totalPages)
        assertEquals(12, extracted12.pages.size)
        assertTrue("Page 6 must be extracted cleanly", extracted12.pages[5].cleanNativeText.contains("Page 6 of 12"))
        assertTrue("Page 12 must be extracted cleanly", extracted12.pages[11].cleanNativeText.contains("Page 12 of 12"))
        multiPagePdfFile.delete()
        Unit
    }

    @Test
    fun superadminBackendEnforcesRbacDynamicPlansEntitlementsBookAccessAndAuditLogs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.polyglotDao()
        val repository = PolyglotRepository(dao, context)

        repository.ensurePublicCatalogInitialized()

        val superadminResult = repository.authenticateDefaultSuperadminSession()
        assertTrue("Seeded superadmin account must authenticate", superadminResult.isSuccess)
        val superadmin = superadminResult.getOrThrow()

        // Create a normal user and an ordinary admin user
        val normalUserId = dao.insertUser(
            com.example.data.local.UserEntity(
                email = "reader@lipi.org",
                displayName = "Normal Reader",
                passwordHash = "hash",
                passwordSalt = "salt",
                role = com.example.data.local.UserRole.USER
            )
        )
        val ordinaryAdminId = dao.insertUser(
            com.example.data.local.UserEntity(
                email = "editor@lipi.org",
                displayName = "Editorial Admin",
                passwordHash = "hash",
                passwordSalt = "salt",
                role = com.example.data.local.UserRole.ADMIN
            )
        )

        val superadminApi = repository.superadminApi
        val accessApi = repository.accessApi

        // 1. Ordinary user & ordinary admin MUST be blocked from Superadmin endpoints
        val readerPlan = dao.getCommercialPlanByCode(SubscriptionTier.READER)!!
        val unauthorizedPlanAttempt = superadminApi.toggleCommercialPlanEnabled(
            adminUserId = normalUserId,
            planId = readerPlan.id,
            enabled = false,
            reasonOrNote = "Malicious disable"
        )
        assertTrue(unauthorizedPlanAttempt.isFailure)

        val unauthorizedEscalationAttempt = superadminApi.updateUserRole(
            adminUserId = ordinaryAdminId,
            targetUserId = ordinaryAdminId,
            newRole = com.example.data.local.UserRole.SUPERADMIN,
            reasonOrNote = "Self escalation attempt"
        )
        assertTrue(unauthorizedEscalationAttempt.isFailure)

        // 2. Superadmin can dynamically update commercial plan price without code changes
        val updatedPlanResult = superadminApi.saveCommercialPlan(
            adminUserId = superadmin.id,
            existingPlanId = readerPlan.id,
            planCode = readerPlan.planCode,
            name = readerPlan.name,
            description = readerPlan.description,
            isEnabled = true,
            monthlyPriceAmount = 15.99,
            yearlyPriceAmount = 159.0,
            currencyCode = "USD",
            trialDays = 7,
            displayOrder = readerPlan.displayOrder,
            isFeatured = readerPlan.isFeatured,
            featuresCsv = readerPlan.featuresCsv,
            allowedBookAccessPoliciesCsv = readerPlan.allowedBookAccessPoliciesCsv,
            dailyTranslationLimit = readerPlan.dailyTranslationLimit,
            monthlyManagedAiTokensLimit = readerPlan.monthlyManagedAiTokensLimit,
            offlineAccessEnabled = readerPlan.offlineAccessEnabled,
            advancedSearchAndScholarlyEnabled = readerPlan.advancedSearchAndScholarlyEnabled,
            ttsAudioDailyMinutesLimit = readerPlan.ttsAudioDailyMinutesLimit,
            reasonOrNote = "Seasonal pricing update"
        )
        assertTrue(updatedPlanResult.isSuccess)
        assertEquals(15.99, dao.getCommercialPlanByCode(SubscriptionTier.READER)?.monthlyPriceAmount ?: 0.0, 0.01)

        // 3. Superadmin can create a restricted book & configure access policy; verify non-exposure in public directory
        val createdBookResult = superadminApi.createBookByAdmin(
            adminUserId = superadmin.id,
            title = "Rare Tantric Manuscript",
            subtitle = "Critical Edition",
            authorOrSource = "Abhinavagupta",
            contributors = "Lipi Scholars",
            publisherName = "Kashmir Series",
            description = "Restricted individual purchase edition",
            originalLanguage = "Sanskrit",
            category = "Philosophy",
            genresCsv = "Tantra,Shaivism",
            tagsCsv = "Kashmir Shaivism",
            workId = "WORK-ABHINAVA-01",
            editionLabel = "2nd Revised Edition",
            isbnOrIdentifier = "ISBN-978-81-9999",
            publicationYear = "1015",
            publicationInfo = "Critical Apparatus",
            coverBadgeColorHex = "#3E2723",
            coverImageUrl = "",
            accessPolicy = BookAccessPolicy.INDIVIDUAL_PURCHASE,
            requiredPlanCode = "",
            rightsCategory = BookRightsCategory.PUBLISHER_LICENSED,
            publicationStatus = com.example.data.local.BookPublicationStatus.PUBLISHED,
            priceDisplay = "$24.99",
            initialSampleParagraphText = "अथातो भक्तिजिज्ञासा।",
            reasonOrNote = "Cataloged new individual-purchase critical edition"
        )
        assertTrue(createdBookResult.isSuccess)
        val restrictedBook = createdBookResult.getOrThrow()

        // Verify public guest cannot discover or read INDIVIDUAL_PURCHASE book
        assertFalse(
            "Public guest must not have read entitlement to INDIVIDUAL_PURCHASE book",
            accessApi.canUserReadBook(userId = 0L, entitlements = emptyList(), userBookAccess = emptyList(), book = restrictedBook)
        )

        // 4. Superadmin grants individual book purchase entitlement to normalUserId -> user gains read access
        val grantResult = superadminApi.grantUserEntitlement(
            adminUserId = superadmin.id,
            targetUserId = normalUserId,
            entitlementKind = com.example.data.local.EntitlementKind.BOOK_PURCHASE,
            targetRef = restrictedBook.id.toString(),
            displayTitle = "Purchased ${restrictedBook.title}",
            durationDays = 365,
            reasonOrNote = "Purchased individual edition access"
        )
        assertTrue(grantResult.isSuccess)

        val userEntitlements = dao.getActiveEntitlementsForUser(normalUserId)
        assertTrue(
            "User with individual BOOK_PURCHASE entitlement must be able to read the book",
            accessApi.canUserReadBook(userId = normalUserId, entitlements = userEntitlements, userBookAccess = emptyList(), book = restrictedBook)
        )

        // 5. Translation governance: versioned retranslation preserves historical translation
        val allTranslationsBefore = dao.getAllCanonicalTranslationsInSystem()
        assertTrue(allTranslationsBefore.isNotEmpty())
        val sampleTrans = allTranslationsBefore.first()
        val retranslateResult = superadminApi.triggerAdminRetranslationVersion(
            adminUserId = superadmin.id,
            translationId = sampleTrans.id,
            newTranslatedText = "Refined poetic translation for scholarly clarity.",
            newNotes = "Refined meter notes",
            modelUsed = "gemini-2.5-flash",
            reasonOrNote = "Improve poetic meter in verse translation"
        )
        assertTrue(retranslateResult.isSuccess)
        val newVersion = retranslateResult.getOrThrow()
        assertTrue("New translation version must increment versionNumber", newVersion.versionNumber > sampleTrans.versionNumber)
        assertNotNull(
            "Historical translation must remain preserved in database",
            dao.getTranslationById(sampleTrans.id)
        )

        // 6. Verify default Superadmin password is "test1234" on superadmin@lipi.org and admin@lipi.org
        val loginDefaultAdmin = repository.loginUser("admin@lipi.org", "test1234")
        assertTrue("Default admin@lipi.org must sign in with test1234", loginDefaultAdmin.isSuccess)

        // 7. Verify Self-Service 6-Digit Password Reset Flow & Change Password Flow
        val resetReq = repository.requestPasswordReset("reader@lipi.org")
        assertTrue("Password reset request must succeed for registered user", resetReq.isSuccess)
        val extractedCode = resetReq.getOrThrow().substringAfterLast(": ").trim()
        assertEquals(6, extractedCode.length)

        val resetConfirm = repository.confirmPasswordReset("reader@lipi.org", extractedCode, "newReaderPass99")
        assertTrue("Password reset with valid 6-digit code must succeed", resetConfirm.isSuccess)
        assertTrue("User must sign in with newly reset password", repository.loginUser("reader@lipi.org", "newReaderPass99").isSuccess)

        val changePwdResult = repository.changeUserPassword(normalUserId, "newReaderPass99", "changedPass123")
        assertTrue("Authenticated password change must succeed", changePwdResult.isSuccess)
        assertTrue(repository.loginUser("reader@lipi.org", "changedPass123").isSuccess)

        val adminResetResult = superadminApi.resetUserPasswordByAdmin(
            adminUserId = superadmin.id,
            targetUserId = normalUserId,
            newPassword = "test1234",
            reasonOrNote = "Superadmin temporary password reset"
        )
        assertTrue(adminResetResult.isSuccess)
        assertTrue(repository.loginUser("reader@lipi.org", "test1234").isSuccess)

        // 8. Immutable audit log records all actions
        val auditLogs = dao.getAllAdminAuditLogs()
        assertTrue(auditLogs.any { it.action == "UNAUTHORIZED_SUPERADMIN_ATTEMPT" })
        assertTrue(auditLogs.any { it.action == "PLAN_SAVED" })
        assertTrue(auditLogs.any { it.action == "BOOK_CREATED" })
        assertTrue(auditLogs.any { it.action == "ENTITLEMENT_GRANTED" })
        assertTrue(auditLogs.any { it.action == "TRANSLATION_RETRANSLATE_TRIGGERED" })
        assertTrue(auditLogs.any { it.action == "PASSWORD_RESET_REQUESTED" })
        assertTrue(auditLogs.any { it.action == "PASSWORD_RESET_COMPLETED" })
        assertTrue(auditLogs.any { it.action == "USER_PASSWORD_CHANGED" })
        assertTrue(auditLogs.any { it.action == "ADMIN_USER_PASSWORD_RESET" })

        db.close()
    }
}
