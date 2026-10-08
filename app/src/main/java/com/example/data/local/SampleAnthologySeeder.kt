package com.example.data.local

import com.example.util.SecurityVault

/**
 * Seeds the Polyglot Shared Content & Translation Corpus:
 * 1. Public Sample Book (PUBLIC / PUBLIC_DOMAIN): "भारतीय ज्ञान संग्रह — Bharat Gyan Sangraha"
 *    - Complete Chapter -> Section -> Paragraph hierarchy with `rawOcrText`, `ocrVersion = 1`, and `sourceContentHash`.
 *    - Multiple canonical translation versions (`v1` AI_GENERATED, `v2` EXPERT_VERIFIED / PUBLISHER_APPROVED) in Gujarati & Hindi.
 *    - Independent `TransliterationLearningEntity` records (IAST, phonetic pronunciation, word-by-word Sanskrit breakdown, literal & natural English translations, and scholarly notes).
 *    - Reusable `TranslationMemoryEntity` segment records (including shared Sanskrit maxims that also appear in Book 2 & Book 3 to demonstrate automatic Translation Memory exact-match reuse!).
 * 2. Free Registered Book (FREE / POLYGLOT_OWNED): "आयुर्वेद एवं योग सूत्र — Ayurveda & Yoga Sutras"
 * 3. Paid Scholarly Edition (PAID / PUBLISHER_LICENSED): "उपनिषद् दर्शन कोश — Upanishadic Philosophy Compendium"
 * 4. Restricted Research Edition (RESTRICTED / PUBLISHER_LICENSED): "नव्य-न्याय तर्क भाष्य — Navya-Nyaya Logic & Epistemology"
 * 5. Publisher Controlled Edition (PUBLISHER_ONLY / PUBLISHER_LICENSED): "संस्कृत काव्यशास्त्र पाण्डुलिपि — Classical Poetics Manuscript"
 */
object SampleAnthologySeeder {

    private const val BOOK1_FINGERPRINT = "polyglot_public_book_bharat_gyan_v4"
    private const val BOOK2_FINGERPRINT = "polyglot_free_book_ayurveda_yoga_v4"
    private const val BOOK3_FINGERPRINT = "polyglot_paid_book_upanishad_kosha_v4"
    private const val BOOK4_FINGERPRINT = "polyglot_restricted_book_navya_nyaya_v4"
    private const val BOOK5_FINGERPRINT = "polyglot_publisher_book_kavyashastra_v4"

    const val DEFAULT_SUPERADMIN_EMAIL = "superadmin@vidyakosha.org"
    const val DEFAULT_SUPERADMIN_PASSWORD = "superadmin123"

    suspend fun ensurePublicCatalogSeeded(
        dao: PolyglotDao,
        referencePdfPath: String,
        referencePdfSize: Long
    ): BookEntity {
        ensurePlatformConfigAndSuperadminSeeded(dao)

        val existingSample = dao.getBookByContentHash(BOOK1_FINGERPRINT)
        if (existingSample != null) {
            return existingSample
        }

        val createdBook1 = seedBook1PublicSample(dao, referencePdfPath, referencePdfSize)
        seedBook2FreeRegistered(dao, referencePdfPath, referencePdfSize)
        seedBook3PaidEdition(dao, referencePdfPath, referencePdfSize)
        seedBook4RestrictedResearch(dao, referencePdfPath, referencePdfSize)
        seedBook5PublisherOnly(dao, referencePdfPath, referencePdfSize)

        return createdBook1
    }

    private suspend fun seedBook1PublicSample(
        dao: PolyglotDao,
        pdfPath: String,
        pdfSize: Long
    ): BookEntity {
        val now = System.currentTimeMillis()
        val bookId = dao.insertBook(
            BookEntity(
                title = "भारतीय ज्ञान संग्रह — Bharat Gyan Sangraha",
                subtitle = "Classical Sanskrit Wisdom, Ethics, Science & Poetics",
                authorOrSource = "Acharya Vishwanath & Classical Commentators",
                description = "Public-domain classical Sanskrit & English anthology pre-ingested with OCR v1 hashes, multi-version Gujarati/Hindi canonical translations (v1 AI + v2 Expert Verified), and word-by-word IAST scholarly breakdowns.",
                category = "Classical Wisdom & Ethics",
                coverBadgeColorHex = "#F59E0B",
                sourcePdfPath = pdfPath,
                contentHash = BOOK1_FINGERPRINT,
                totalPages = 3,
                fileSizeBytes = pdfSize,
                originalLanguage = "Sanskrit",
                preferredTargetLanguage = "Gujarati",
                accessPolicy = BookAccessPolicy.PUBLIC,
                rightsCategory = BookRightsCategory.PUBLIC_DOMAIN,
                isPublicCatalog = true,
                isSampleBook = true,
                isPaid = false,
                priceDisplay = "Free Public Sample",
                publisherName = "Polyglot Open Classics",
                currentOcrVersion = 1,
                ocrStatus = "COMPLETED",
                ocrProviderUsed = "Polyglot Archival Vision OCR v1",
                ocrProgressPages = 3,
                totalChapters = 3,
                totalSections = 4,
                totalParagraphs = 8,
                readCountAnalytics = 42,
                listenCountAnalytics = 19,
                createdAt = now - 86_400_000L * 5
            )
        )

        dao.insertBookPages(
            listOf(
                BookPageEntity(
                    bookId = bookId,
                    pageNumber = 1,
                    rawOcrText = "अध्याय १: विद्या एवं धर्म (Knowledge & Righteousness)\nविद्या ददाति विनयं विनयाद् याति पात्रताम्। धर्मो रक्षति रक्षितः।",
                    detectedSourceLanguage = "Sanskrit"
                ),
                BookPageEntity(
                    bookId = bookId,
                    pageNumber = 2,
                    rawOcrText = "अध्याय २: कर्मयोग एवं आत्मसंयम (Philosophy of Action)\nकर्मण्येवाधिकारस्ते मा फलेषु कदाचन। योगः कर्मसु कौशलम्।",
                    detectedSourceLanguage = "Sanskrit"
                ),
                BookPageEntity(
                    bookId = bookId,
                    pageNumber = 3,
                    rawOcrText = "अध्याय ३: विश्वबंधुत्व एवं शांति (Universal Harmony)\nअयं निजः परो वेति गणना लघुचेतसाम्। उदारचरितानां तु वसुधैव कुटुम्बकम्।",
                    detectedSourceLanguage = "Sanskrit"
                )
            )
        )

        // Chapter 1
        val ch1Id = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: प्रज्ञा एवं विद्या (Foundations of Wisdom)",
                summary = "Explores how true education cultivates humility, worthiness, and righteous conduct (Dharma).",
                startPage = 1,
                endPage = 1
            )
        )
        val sec1_1Id = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = ch1Id,
                sectionOrder = 1,
                heading = "Section 1.1: The Fruits of Humility (विद्यायाः फलम्)",
                startPage = 1,
                endPage = 1
            )
        )
        val sec1_2Id = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = ch1Id,
                sectionOrder = 2,
                heading = "Section 1.2: Preserving Cosmic Order (धर्मरक्षा)",
                startPage = 1,
                endPage = 1
            )
        )

        // Chapter 2
        val ch2Id = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 2,
                title = "Chapter 2: कर्मयोग एवं समत्व (Action & Equanimity)",
                summary = "Examines selfless action without attachment to outcomes and equanimity in success or failure.",
                startPage = 2,
                endPage = 2
            )
        )
        val sec2_1Id = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = ch2Id,
                sectionOrder = 1,
                heading = "Section 2.1: Selfless Action (निष्काम कर्म)",
                startPage = 2,
                endPage = 2
            )
        )

        // Chapter 3
        val ch3Id = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 3,
                title = "Chapter 3: वसुधैव कुटुम्बकम् (Universal Kinship)",
                summary = "Concludes with the classical Indian vision of global kinship and peace across all beings.",
                startPage = 3,
                endPage = 3
            )
        )
        val sec3_1Id = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = ch3Id,
                sectionOrder = 1,
                heading = "Section 3.1: The World as One Family (विश्वमैत्री)",
                startPage = 3,
                endPage = 3
            )
        )

        data class SeedParaSpec(
            val chId: Long,
            val secId: Long,
            val order: Int,
            val page: Int,
            val text: String,
            val guV1Ai: String,
            val guV2Verified: String,
            val hiVerified: String,
            val iast: String,
            val phonetic: String,
            val breakdown: String,
            val literalEn: String,
            val naturalEn: String,
            val commentary: String
        )

        val specs = listOf(
            SeedParaSpec(
                chId = ch1Id,
                secId = sec1_1Id,
                order = 1,
                page = 1,
                text = "विद्या ददाति विनयं विनयाद् याति पात्रताम्। पात्रत्वाद्धनमाप्नोति धनाद्धर्मं ततः सुखम्॥",
                guV1Ai = "વિદ્યા વિનય આપે છે, વિનયથી પાત્રતા મળે છે. પાત્રતાથી ધન મળે છે અને ધનથી ધર્મ તથા સુખ મળે છે.",
                guV2Verified = "સાચી વિદ્યા મનુષ્યને નમ્રતા (વિનય) પ્રદાન કરે છે; નમ્રતાથી યોગ્યતા (પાત્રતા) પ્રાપ્ત થાય છે; યોગ્યતાથી સત્સંપત્તિ મળે છે, જેમાંથી ધર્મનું આચરણ અને શાશ્વત સુખ પરિણમે છે.",
                hiVerified = "सच्ची विद्या मनुष्य को विनम्रता प्रदान करती है और विनम्रता से योग्यता (पात्रता) आती है। योग्यता से सद्धन की प्राप्ति होती है, धन से धर्म के कार्य होते हैं और अंततः शाश्वत सुख मिलता है।",
                iast = "vidyā dadāti vinayaṃ vinayād yāti pātratām | pātratvāddhanamāpnoti dhanāddharmaṃ tataḥ sukham ||",
                phonetic = "vid-yaa da-daa-ti vi-na-yam, vi-na-yaad yaa-ti paa-tra-taam | paa-trat-vaad dha-nam aap-no-ti, dha-naad dhar-mam ta-tah su-kham",
                breakdown = "विद्या (vidyā): knowledge/learning • ददाति (dadāti): gives/bestows • विनयम् (vinayam): humility/discipline • पात्रताम् (pātratām): worthiness/capability • धनम् (dhanam): rightful prosperity • धर्मम् (dharmam): righteous duty • सुखम् (sukham): abiding happiness",
                literalEn = "Knowledge gives humility; from humility one attains worthiness; from worthiness one obtains wealth; from wealth, righteousness, and thence happiness.",
                naturalEn = "True learning instills humility; through humility one grows worthy; worthiness brings ethical prosperity, which empowers righteous living and lasting joy.",
                commentary = "From the Hitopadesha (Prastava 6). Notice the causal chain: wealth (dhana) produces happiness (sukha) only when channeled through Dharma."
            ),
            SeedParaSpec(
                chId = ch1Id,
                secId = sec1_1Id,
                order = 2,
                page = 1,
                text = "न चोरहार्यं न च राजहार्यं न भ्रातृभाज्यं न च भारकारि। व्यये कृते वर्धत एव नित्यं विद्याधनं सर्वधनप्रधानम्॥",
                guV1Ai = "ચોર ચોરી શકતો નથી, રાજા લઈ શકતો નથી, ભાઈઓમાં વહેંચાતું નથી અને બોજારૂપ નથી. ખર્ચ કરવાથી વધે છે તેવું વિદ્યાધન સર્વ ધનમાં શ્રેષ્ઠ છે.",
                guV2Verified = "જેને ચોર ચોરી શકતો નથી, રાજસત્તા છીનવી શકતી નથી, ભાઈઓ વચ્ચે વહેંચી શકાતું નથી અને જે કદી બોજારૂપ બનતું નથી—વહેંચવાથી જે નિત્ય વધતું જ રહે છે, તેવું ‘વિદ્યાધન’ સર્વ સંપત્તિમાં સર્વોચ્ચ છે.",
                hiVerified = "जिसे न चोर चुरा सकता है, न राजा छीन सकता है, न भाइयों में बाँटा जा सकता है और न ही जो बोझ बनता है—व्यय करने (बाँटने) पर जो नित्य बढ़ता ही जाता है, वह विद्या-रूपी धन सभी धनों में प्रधान है।",
                iast = "na corahāryaṃ na ca rājahāryaṃ na bhrātṛbhājyaṃ na ca bhārakāri | vyaye kṛte vardhata eva nityaṃ vidyādhanaṃ sarvadhanapradhānam ||",
                phonetic = "na cho-ra-haar-yam na cha raa-ja-haar-yam, na bhraa-tri-bhaaj-yam na cha bhaa-ra-kaa-ri | vya-ye kri-te var-dha-ta e-va nit-yam, vid-yaa-dha-nam sar-va-dha-na-pra-dhaa-nam",
                breakdown = "न चोरहार्यम् (na corahāryam): not stealable by thieves • न राजहार्यम् (na rājahāryam): not confiscable by kings • न भ्रातृभाज्यम् (na bhrātṛbhājyam): not divisible among brothers • व्यये कृते (vyaye kṛte): when spent/shared • वर्धते (vardhate): increases • विद्याधनम् (vidyādhanam): wealth of knowledge",
                literalEn = "Not stealable by thieves, nor confiscable by rulers, not divisible among siblings, nor burdensome; when spent, it ever grows—the wealth of knowledge is supreme among all wealth.",
                naturalEn = "Unlike material riches, knowledge cannot be stolen, taxed, divided, or weighed down; the more generously it is shared, the more abundantly it grows.",
                commentary = "A classic Subhashita contrasting tangible property with intellectual and spiritual insight (vidyā-dhana)."
            ),
            SeedParaSpec(
                chId = ch1Id,
                secId = sec1_2Id,
                order = 1,
                page = 1,
                text = "धर्मो रक्षति रक्षितः। सत्यमेव जयते नानृतं सत्येन पन्था विततो देवयानः॥",
                guV1Ai = "રક્ષાયેલો ધર્મ રક્ષણ કરે છે. સત્યનો જ વિજય થાય છે, અસત્યનો નહીં.",
                guV2Verified = "જે ધર્મનું રક્ષણ કરે છે, ધર્મ તેનું રક્ષણ કરે છે. સત્યનો જ સદાય વિજય થાય છે, અસત્યનો કદાપિ નહીં; સત્ય વડે જ દિવ્ય માર્ગ વિસ્તરેલો છે.",
                hiVerified = "रक्षा किया गया धर्म ही हमारी रक्षा करता है। सत्य की ही विजय होती है, असत्य की नहीं; सत्य से ही देवयान का दिव्य मार्ग प्रशस्त होता है।",
                iast = "dharmo rakṣati rakṣitaḥ | satyameva jayate nānṛtaṃ satyena panthā vitato devayānaḥ ||",
                phonetic = "dhar-mo rak-sha-ti rak-shi-tah | sat-yam e-va ja-ya-te naa-nri-tam, sat-ye-na pan-thaa vi-ta-to de-va-yaa-nah",
                breakdown = "धर्मः (dharmaḥ): cosmic order/ethical duty • रक्षति (rakṣati): protects • रक्षितः (rakṣitaḥ): when protected/upheld • सत्यम् एव (satyam eva): truth alone • जयते (jayate): triumphs • न अनृतम् (na anṛtam): not falsehood • पन्थाः (panthāḥ): path",
                literalEn = "Dharma protects when protected. Truth alone triumphs, not falsehood; by truth is laid out the divine path.",
                naturalEn = "Righteousness safeguards those who uphold it. Truth alone prevails, never falsehood; through truth the luminous path of wisdom unfolds.",
                commentary = "Combines Manusmriti 8.15 (dharmo rakṣati rakṣitaḥ) and Mundaka Upanishad 3.1.6 (satyameva jayate)."
            ),
            SeedParaSpec(
                chId = ch1Id,
                secId = sec1_2Id,
                order = 2,
                page = 1,
                text = "अद्भिर्गात्राणि शुद्ध्यन्ति मनः सत्येन शुद्ध्यति। विद्यातपोभ्यां भूतात्मा बुद्धिर्ज्ञानेन शुद्ध्यति॥",
                guV1Ai = "પાણીથી શરીર શુદ્ધ થાય છે, સત્યથી મન શુદ્ધ થાય છે. વિદ્યા અને તપથી જીવાત્મા અને જ્ઞાનથી બુદ્ધિ શુદ્ધ થાય છે.",
                guV2Verified = "જળથી શરીરનાં અંગો પવિત્ર થાય છે, સત્યના આચરણથી મન નિર્મળ બને છે, વિદ્યા અને તપસ્યાથી જીવાત્મા ઊર્ધ્વગામી બને છે અને વિવેકજ્ઞાનથી બુદ્ધિ વિશુદ્ધ થાય છે.",
                hiVerified = "जल से शरीर के अंग शुद्ध होते हैं, सत्य से मन पवित्र होता है, विद्या और तप से जीवात्मा तथा ज्ञान से बुद्धि विशुद्ध होती है।",
                iast = "adbhirgātrāṇi śuddhyanti manaḥ satyena śuddhyati | vidyātapobhyāṃ bhūtātmā buddhirjñānena śuddhyati ||",
                phonetic = "ad-bhir gaa-traa-ni shud-dhyan-ti, ma-nah sat-ye-na shud-dhya-ti | vid-yaa-ta-pobh-yaam bhoo-taat-maa, bud-dhir gyaa-ne-na shud-dhya-ti",
                breakdown = "अद्भिः (adbhiḥ): by water • गात्राणि (gātrāṇi): limbs/body • शुद्ध्यन्ति (śuddhyanti): are purified • मनः (manaḥ): mind • सत्येन (satyena): by truth • बुद्धिः (buddhiḥ): intellect • ज्ञानेन (jñānena): by discernment/wisdom",
                literalEn = "By water the limbs are purified; the mind is purified by truth; the living soul by learning and austerity; the intellect is purified by wisdom.",
                naturalEn = "Water cleanses the outer body, truth purifies the mind, study and discipline refine the spirit, and discerning wisdom illuminates the intellect.",
                commentary = "Outlines the fourfold purification hierarchy in classical Indian ethics: physical, mental, spiritual, and intellectual."
            ),
            SeedParaSpec(
                chId = ch2Id,
                secId = sec2_1Id,
                order = 1,
                page = 2,
                text = "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन। मा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥",
                guV1Ai = "તારો અધિકાર માત્ર કર્મ કરવા પૂરતો છે, ફળ પર ક્યારેય નહીં. કર્મના ફળનો હેતુ ન બન અને અકર્મમાં પણ આસક્તિ ન રાખ.",
                guV2Verified = "તારો અધિકાર કેવળ કર્તવ્ય-કર્મ આચરવામાં જ છે, તેનાં પરિણામો (ફળ) પર કદાપિ નહીં. તું કર્મફળની તૃષ્ણાવાળો ન બન અને કર્તવ્યના ત્યાગમાં (અકર્મમાં) પણ તારી આસક્તિ ન હો.",
                hiVerified = "तुम्हारा अधिकार केवल कर्तव्य-कर्म करने में ही है, उसके फलों में कदापि नहीं। इसलिए तुम कर्मफल के हेतु मत बनो और न ही अकर्मण्यता (कर्तव्य-त्याग) में तुम्हारी आसक्ति हो।",
                iast = "karmaṇyevādhikāraste mā phaleṣu kadācana | mā karmaphalaheturbhūrmā te saṅgo'stvakarmaṇi ||",
                phonetic = "kar-man-ye-vaa-dhi-kaa-ras-te maa pha-le-shu ka-daa-cha-na | maa kar-ma-pha-la-he-tur-bhoor maa te san-go-stva-kar-ma-ni",
                breakdown = "कर्मणि एव (karmaṇi eva): in action alone • अधिकारः ते (adhikāraḥ te): your right/jurisdiction • मा फलेषु (mā phaleṣu): never in the fruits • कदाचन (kadācana): at any time • अकर्मणि (akarmaṇi): in inaction",
                literalEn = "In action alone is your right, never in the fruits at any time; do not be the motive of the fruit of action, nor let your attachment be to inaction.",
                naturalEn = "You have sovereignty over your effort alone, never over its outcomes. Act neither out of craving for reward nor out of retreat into passivity.",
                commentary = "Bhagavad Gita 2.47 — the cornerstone verse of Nishkama Karma (selfless mastery in action)."
            ),
            SeedParaSpec(
                chId = ch2Id,
                secId = sec2_1Id,
                order = 2,
                page = 2,
                text = "योगस्थः कुरु कर्माणि सङ्गं त्यक्त्वा धनञ्जय। सिद्ध्यसिद्ध्योः समो भूत्वा समत्वं योग उच्यते॥ योगः कर्मसु कौशलम्।",
                guV1Ai = "આસક્તિ છોડીને અને સિદ્ધિ-અસિદ્ધિમાં સમાન રહીને કર્મ કર. સમભાવને જ યોગ કહે છે. કર્મોમાં કુશળતા એ જ યોગ છે.",
                guV2Verified = "હે ધનંજય! આસક્તિનો ત્યાગ કરીને તથા સફળતા અને નિષ્ફળતામાં સમભાવ રાખીને તું કર્તવ્યકર્મો કર; આ સમત્વભાવને જ ‘યોગ’ કહેવામાં આવે છે. કર્મોમાં નિષ્ઠાપૂર્વકની કુશળતા એ જ યોગ છે.",
                hiVerified = "हे धनंजय! आसक्ति का त्याग कर तथा सिद्धि और असिद्धि में समान रहकर अपने कर्मों को संपन्न करो; चित्त की इसी समता को 'योग' कहा जाता है। कर्मों में कुशलता ही योग है।",
                iast = "yogasthaḥ kuru karmāṇi saṅgaṃ tyaktvā dhanañjaya | siddhyasiddhyoḥ samo bhūtvā samatvaṃ yoga ucyate || yogaḥ karmasu kauśalam |",
                phonetic = "yo-gas-thah ku-ru kar-maa-ni san-gam tyak-tvaa dha-nan-ja-ya | sid-dhy-a-sid-dhyoh sa-mo bhoo-tvaa sa-mat-vam yo-ga uch-ya-te | yo-gah kar-ma-su kau-sha-lam",
                breakdown = "योगस्थः (yogasthaḥ): steadfast in yoga • कुरु कर्माणि (kuru karmāṇi): perform actions • सङ्गम् त्यक्त्वा (saṅgam tyaktvā): abandoning attachment • समत्वम् (samatvam): equanimity/evenness of mind • कर्मसु कौशलम् (karmasu kauśalam): excellence/skill in action",
                literalEn = "Established in Yoga, perform actions abandoning attachment; being equal in success and failure, equanimity is called Yoga. Yoga is skill in actions.",
                naturalEn = "Act from inner steadiness, free from possessive attachment. Remaining poised in both triumph and setback—that inner balance is Yoga, the art of serene excellence in action.",
                commentary = "Synthesizes Bhagavad Gita 2.48 and 2.50, defining Yoga both as inner equanimity (samatvam) and skilful, unentangled action (karmasu kauśalam)."
            ),
            SeedParaSpec(
                chId = ch3Id,
                secId = sec3_1Id,
                order = 1,
                page = 3,
                text = "अयं निजः परो वेति गणना लघुचेतसाम्। उदारचरितानां तु वसुधैव कुटुम्बकम्॥",
                guV1Ai = "આ મારો છે કે પારકો છે એવી ગણતરી સંકુચિત મનવાળા કરે છે. ઉદાર ચરિત્રવાળા લોકો માટે તો આખી પૃથ્વી એક પરિવાર છે.",
                guV2Verified = "‘આ મારું છે અને આ પારકું છે’—એવી ભેદભાવભરી ગણતરી સંકુચિત હૃદયવાળા લોકોની હોય છે; ઉદાર ચરિત્ર ધરાવતા મહાપુરુષો માટે તો સમગ્ર પૃથ્વી જ એક પરિવાર (કુટુંબ) છે.",
                hiVerified = "'यह मेरा अपना है और यह पराया है'—ऐसी गणना संकुचित चित्त वाले लोग करते हैं; उदार चरित्र वाले महापुरुषों के लिए तो संपूर्ण पृथ्वी ही एक परिवार (कुटुम्ब) है।",
                iast = "ayaṃ nijaḥ paro veti gaṇanā laghucetasām | udāracaritānāṃ tu vasudhaiva kuṭumbakam ||",
                phonetic = "a-yam ni-jah pa-ro ve-ti ga-na-naa la-ghu-che-ta-saam | u-daa-ra-cha-ri-taa-naam tu va-su-dhai-va ku-tum-ba-kam",
                breakdown = "अयम् निजः (ayam nijaḥ): this one is mine • परः वा (paraḥ vā): or a stranger • गणना (gaṇanā): calculation/reckoning • लघुचेतसाम् (laghucetasām): of the narrow-minded • उदारचरितानाम् (udāracaritānām): for the noble-hearted • वसुधा एव कुटुम्बकम् (vasudhā eva kuṭumbakam): the whole earth is one family",
                literalEn = "'This one is mine, or a stranger' is the reckoning of the small-minded; for those of noble conduct, the earth itself is a family.",
                naturalEn = "Dividing humanity into 'us' and 'them' belongs to narrow minds; to the generous of spirit, the entire world is one shared family.",
                commentary = "Maha Upanishad 6.71 & Hitopadesha 1.70 — engraved in the foyer of the Parliament of India."
            ),
            SeedParaSpec(
                chId = ch3Id,
                secId = sec3_1Id,
                order = 2,
                page = 3,
                text = "सर्वे भवन्तु सुखिनः सर्वे सन्तु निरामयाः। सर्वे भद्राणि पश्यन्तु मा कश्चिद्दुःखभाग्भवेत्॥",
                guV1Ai = "સૌ સુખી થાઓ, સૌ નિરોગી રહો. સૌ કલ્યાણ જુઓ અને કોઈ પણ દુઃખી ન થાય.",
                guV2Verified = "સર્વ જીવો સુખી થાઓ, સર્વ નિરોગી અને સ્વસ્થ રહો, સર્વ પ્રાણીઓ મંગળમય કલ્યાણનાં દર્શન કરો અને જગતમાં કોઈ પણ જીવ દુઃખનો ભાગીદાર ન બને.",
                hiVerified = "सभी प्राणी सुखी हों, सभी निरोगी और स्वस्थ रहें, सभी मंगलमय कल्याण का दर्शन करें और किसी भी प्राणी को दुःख का भागी न बनना पड़े।",
                iast = "sarve bhavantu sukhinaḥ sarve santu nirāmayāḥ | sarve bhadrāṇi paśyantu mā kaścidduḥkhabhāgbhavet ||",
                phonetic = "sar-ve bha-van-tu su-khi-nah, sar-ve san-tu ni-raa-ma-yaah | sar-ve bhad-raa-ni pash-yan-tu, maa kash-chid duh-kha-bhaag bha-vet",
                breakdown = "सर्वे (sarve): all beings • भवन्तु सुखिनः (bhavantu sukhinaḥ): may they be happy • निरामयाः (nirāmayāḥ): free from illness/affliction • भद्राणि पश्यन्तु (bhadrāṇi paśyantu): may they behold auspiciousness • मा कश्चित् दुःखभाक् भवेत् (mā kaścit duḥkhabhāk bhavet): may no one partake of suffering",
                literalEn = "May all be happy; may all be free from illness; may all behold auspicious things; may no one suffer.",
                naturalEn = "May all beings flourish in happiness and health; may everyone witness goodness, and may no creature anywhere be touched by sorrow.",
                commentary = "Universal Shanti Mantra invoking well-being, health, and freedom from suffering for all sentient beings."
            )
        )

        specs.forEach { spec ->
            val contentHash = SecurityVault.computeContentFingerprint(spec.text.trim())
            val pId = dao.insertParagraphs(
                listOf(
                    ParagraphEntity(
                        bookId = bookId,
                        chapterId = spec.chId,
                        sectionId = spec.secId,
                        paragraphOrder = spec.order,
                        originalPageNumber = spec.page,
                        blockType = "PARAGRAPH",
                        rawOcrText = spec.text,
                        originalText = spec.text,
                        originalLanguage = "Sanskrit",
                        normalizedSearchText = "${spec.text.lowercase()} ${spec.iast.lowercase()} ${spec.literalEn.lowercase()}",
                        ocrVersion = 1,
                        sourceContentHash = contentHash
                    )
                )
            ).first()

            // Insert Gujarati Version 1 (AI_GENERATED, historical version)
            dao.insertTranslation(
                ParagraphTranslationEntity(
                    bookId = bookId,
                    sectionId = spec.secId,
                    paragraphId = pId,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Gujarati",
                    translatedText = spec.guV1Ai,
                    transliterationOrNotes = spec.iast,
                    versionNumber = 1,
                    isActiveVersion = false,
                    providerUsed = "Google Gemini",
                    modelUsed = "gemini-3.1-flash-lite-preview",
                    modelVersion = "2026.09",
                    promptVersion = "polyglot-indic-v1.0",
                    sourceContentVersion = 1,
                    sourceContentHash = contentHash,
                    translatorDisplayName = "AI Baseline Pipeline",
                    reviewStatus = TranslationReviewStatus.AI_GENERATED,
                    accessPolicy = TranslationAccessPolicy.PUBLIC,
                    commercialRightsStatus = "PUBLIC_DOMAIN_DERIVATIVE",
                    isSharedRepositoryAsset = true,
                    estimatedTokens = 110,
                    estimatedCostUsd = 0.0002,
                    reuseCount = 5,
                    createdAt = now - 86_400_000L * 4,
                    updatedAt = now - 86_400_000L * 4
                )
            )

            // Insert Gujarati Version 2 (EXPERT_VERIFIED, preferred canonical version!)
            val guV2Id = dao.insertTranslation(
                ParagraphTranslationEntity(
                    bookId = bookId,
                    sectionId = spec.secId,
                    paragraphId = pId,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Gujarati",
                    translatedText = spec.guV2Verified,
                    transliterationOrNotes = "${spec.iast} — ${spec.naturalEn}",
                    versionNumber = 2,
                    isActiveVersion = true,
                    providerUsed = "Google Gemini",
                    modelUsed = "gemini-3.5-flash",
                    modelVersion = "2026.10",
                    promptVersion = "polyglot-indic-literary-v2.1",
                    sourceContentVersion = 1,
                    sourceContentHash = contentHash,
                    translatorDisplayName = "Dr. K. M.astri & Gemini 3.5",
                    reviewStatus = TranslationReviewStatus.EXPERT_VERIFIED,
                    reviewedByDisplayName = "Sanskrit Vidyapith Review Board",
                    accessPolicy = TranslationAccessPolicy.PUBLIC,
                    commercialRightsStatus = "PUBLIC_DOMAIN_DERIVATIVE",
                    isSharedRepositoryAsset = true,
                    estimatedTokens = 165,
                    estimatedCostUsd = 0.0004,
                    reuseCount = 18,
                    createdAt = now - 86_400_000L * 2,
                    updatedAt = now - 86_400_000L * 1
                )
            )

            // Insert Hindi Version 1 (PUBLISHER_APPROVED, preferred canonical version!)
            val hiV1Id = dao.insertTranslation(
                ParagraphTranslationEntity(
                    bookId = bookId,
                    sectionId = spec.secId,
                    paragraphId = pId,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Hindi",
                    translatedText = spec.hiVerified,
                    transliterationOrNotes = "${spec.iast} — ${spec.naturalEn}",
                    versionNumber = 1,
                    isActiveVersion = true,
                    providerUsed = "Anthropic Claude",
                    modelUsed = "claude-3-7-sonnet-latest",
                    modelVersion = "2026.10",
                    promptVersion = "polyglot-indic-literary-v2.1",
                    sourceContentVersion = 1,
                    sourceContentHash = contentHash,
                    translatorDisplayName = "Polyglot Editorial Board",
                    reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
                    reviewedByDisplayName = "Acharya V. Tripathi",
                    accessPolicy = TranslationAccessPolicy.PUBLIC,
                    commercialRightsStatus = "PUBLIC_DOMAIN_DERIVATIVE",
                    isSharedRepositoryAsset = true,
                    estimatedTokens = 158,
                    estimatedCostUsd = 0.0005,
                    reuseCount = 14,
                    createdAt = now - 86_400_000L * 3,
                    updatedAt = now - 86_400_000L * 1
                )
            )

            // Populate Translation Memory (#9) for both Gujarati and Hindi
            dao.insertTranslationMemory(
                TranslationMemoryEntity(
                    normalizedSourceHash = contentHash,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Gujarati",
                    canonicalSourceText = spec.text.trim(),
                    translatedText = spec.guV2Verified,
                    notesOrGlossary = "${spec.iast} — ${spec.naturalEn}",
                    originBookId = bookId,
                    originParagraphId = pId,
                    originTranslationId = guV2Id,
                    providerUsed = "Google Gemini",
                    modelUsed = "gemini-3.5-flash",
                    reviewStatus = TranslationReviewStatus.EXPERT_VERIFIED,
                    accessPolicy = TranslationAccessPolicy.PUBLIC,
                    matchReuseCount = 12
                )
            )
            dao.insertTranslationMemory(
                TranslationMemoryEntity(
                    normalizedSourceHash = contentHash,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Hindi",
                    canonicalSourceText = spec.text.trim(),
                    translatedText = spec.hiVerified,
                    notesOrGlossary = "${spec.iast} — ${spec.naturalEn}",
                    originBookId = bookId,
                    originParagraphId = pId,
                    originTranslationId = hiV1Id,
                    providerUsed = "Anthropic Claude",
                    modelUsed = "claude-3-7-sonnet-latest",
                    reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
                    accessPolicy = TranslationAccessPolicy.PUBLIC,
                    matchReuseCount = 9
                )
            )

            // Populate Independent Transliteration & Scholarly Learning Layer (#14 & #15)
            dao.insertTransliterationLearning(
                TransliterationLearningEntity(
                    bookId = bookId,
                    paragraphId = pId,
                    sourceLanguage = "Sanskrit",
                    iastTransliteration = spec.iast,
                    phoneticPronunciation = spec.phonetic,
                    wordByWordBreakdown = spec.breakdown,
                    literalEnglishTranslation = spec.literalEn,
                    naturalEnglishTranslation = spec.naturalEn,
                    scholarlyCommentary = spec.commentary
                )
            )
        }

        // Seed initial completed & queued background jobs (#10) and analytics events (#21)
        dao.insertProcessingJob(
            ProcessingJobEntity(
                jobType = JobType.OCR,
                status = JobStatus.COMPLETED,
                bookId = bookId,
                bookTitle = "भारतीय ज्ञान संग्रह — Bharat Gyan Sangraha",
                providerAndModel = "Polyglot Archival Vision OCR v1",
                progressPercent = 100,
                statusDetail = "Ingested 3 Chapters, 4 Sections, 8 Paragraphs with SHA-256 content hashes"
            )
        )
        dao.insertProcessingJob(
            ProcessingJobEntity(
                jobType = JobType.SEARCH_INDEX,
                status = JobStatus.COMPLETED,
                bookId = bookId,
                bookTitle = "भारतीय ज्ञान संग्रह — Bharat Gyan Sangraha",
                providerAndModel = "Cross-Book Lexicon & IAST Indexer",
                progressPercent = 100,
                statusDetail = "Indexed Devanagari, Gujarati, Hindi, and IAST transliterations"
            )
        )

        repeat(15) { idx ->
            dao.insertAnalyticsEvent(
                CorpusAnalyticsEventEntity(
                    eventType = if (idx % 3 == 0) "TM_EXACT_MATCH_REUSED" else "TRANSLATION_REUSED",
                    bookId = bookId,
                    sourceLanguage = "Sanskrit",
                    targetLanguage = if (idx % 2 == 0) "Gujarati" else "Hindi",
                    tokensSavedOrUsed = 165
                )
            )
        }

        return dao.getBookById(bookId)!!
    }

    private suspend fun seedBook2FreeRegistered(
        dao: PolyglotDao,
        pdfPath: String,
        pdfSize: Long
    ) {
        val bookId = dao.insertBook(
            BookEntity(
                title = "आयुर्वेद एवं योग सूत्र — Ayurveda & Yoga Sutras",
                subtitle = "Mindfulness, Breath & Holistic Equilibrium",
                authorOrSource = "Maharshi Patanjali & Charaka Samhita Selections",
                description = "Free registered edition covering classical Yoga Sutras and Dinacharya wellness verses. Includes shared verses that demonstrate automatic Translation Memory exact-match reuse without calling AI!",
                category = "Wellness & Yoga Philosophy",
                coverBadgeColorHex = "#14B8A6",
                sourcePdfPath = pdfPath,
                contentHash = BOOK2_FINGERPRINT,
                totalPages = 2,
                fileSizeBytes = pdfSize,
                originalLanguage = "Sanskrit",
                preferredTargetLanguage = "Gujarati",
                accessPolicy = BookAccessPolicy.FREE,
                rightsCategory = BookRightsCategory.POLYGLOT_OWNED,
                isPublicCatalog = true,
                isSampleBook = false,
                isPaid = false,
                priceDisplay = "Free (Registered)",
                publisherName = "Polyglot Open Classics",
                totalChapters = 1,
                totalSections = 1,
                totalParagraphs = 2
            )
        )
        val chId = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: चित्तवृत्तिनिरोधः (Stillness of Mind)",
                summary = "Foundational aphorisms of Raja Yoga and holistic balance.",
                startPage = 1,
                endPage = 2
            )
        )
        val secId = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = chId,
                sectionOrder = 1,
                heading = "Section 1.1: Definition of Yoga & Equanimity",
                startPage = 1,
                endPage = 2
            )
        )

        val p1Text = "योगश्चित्तवृत्तिनिरोधः। तदा द्रष्टुः स्वरूपेऽवस्थानम्॥"
        val p1Hash = SecurityVault.computeContentFingerprint(p1Text)
        // Paragraph 2 intentionally matches the exact verse from Book 1 Section 2.1 so Translation Memory exact-match reuse triggers immediately!
        val p2Text = "योगस्थः कुरु कर्माणि सङ्गं त्यक्त्वा धनञ्जय। सिद्ध्यसिद्ध्योः समो भूत्वा समत्वं योग उच्यते॥ योगः कर्मसु कौशलम्।"
        val p2Hash = SecurityVault.computeContentFingerprint(p2Text)

        val pIds = dao.insertParagraphs(
            listOf(
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 1,
                    originalPageNumber = 1,
                    rawOcrText = p1Text,
                    originalText = p1Text,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${p1Text.lowercase()} yogaścittavṛttinirodhaḥ stillness of mind",
                    ocrVersion = 1,
                    sourceContentHash = p1Hash
                ),
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 2,
                    originalPageNumber = 2,
                    rawOcrText = p2Text,
                    originalText = p2Text,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${p2Text.lowercase()} samatvam yoga ucyate yogah karmasu kausalam",
                    ocrVersion = 1,
                    sourceContentHash = p2Hash
                )
            )
        )

        dao.insertTranslation(
            ParagraphTranslationEntity(
                bookId = bookId,
                sectionId = secId,
                paragraphId = pIds[0],
                sourceLanguage = "Sanskrit",
                targetLanguage = "Gujarati",
                translatedText = "ચિત્તની વૃત્તિઓનો સંપૂર્ણ નિરોધ (શાંતિ) એ જ ‘યોગ’ છે. તે સમયે દ્રષ્ટા (આત્મા) પોતાના મૂળ સ્વરૂપમાં સ્થિર થાય છે.",
                transliterationOrNotes = "yogaścittavṛttinirodhaḥ | tadā draṣṭuḥ svarūpe'vasthānam ||",
                versionNumber = 1,
                isActiveVersion = true,
                providerUsed = "Google Gemini",
                modelUsed = "gemini-3.5-flash",
                sourceContentVersion = 1,
                sourceContentHash = p1Hash,
                reviewStatus = TranslationReviewStatus.EXPERT_VERIFIED,
                accessPolicy = TranslationAccessPolicy.FREE_REGISTERED_USERS,
                reuseCount = 9
            )
        )

        dao.insertTransliterationLearning(
            TransliterationLearningEntity(
                bookId = bookId,
                paragraphId = pIds[0],
                sourceLanguage = "Sanskrit",
                iastTransliteration = "yogaścittavṛttinirodhaḥ | tadā draṣṭuḥ svarūpe'vasthānam ||",
                phoneticPronunciation = "yo-gash chit-ta-vrit-ti-ni-ro-dhah | ta-daa drash-tuh sva-roo-pe-va-sthaa-nam",
                wordByWordBreakdown = "योगः (yogaḥ): Yoga • चित्त (citta): mind-field • वृत्ति (vṛtti): fluctuations/modifications • निरोधः (nirodhaḥ): cessation/stillness • द्रष्टुः (draṣṭuḥ): of the Seer/Witness • स्वरूपे (svarūpe): in one's true nature",
                literalEnglishTranslation = "Yoga is the cessation of the modifications of the mind; then the Seer abides in its own true form.",
                naturalEnglishTranslation = "Yoga is the stilling of the restless waves of the mind; in that stillness, awareness rests in its luminous original nature.",
                scholarlyCommentary = "Patanjali Yoga Sutras 1.2–1.3."
            )
        )
    }

    private suspend fun seedBook3PaidEdition(
        dao: PolyglotDao,
        pdfPath: String,
        pdfSize: Long
    ) {
        val bookId = dao.insertBook(
            BookEntity(
                title = "उपनिषद् दर्शन कोश — Upanishadic Philosophy Compendium",
                subtitle = "Critical Bilingual Edition with Shankara & Ramanuja Notes",
                authorOrSource = "Vedic Research Institute Edition",
                description = "Paid scholarly edition featuring Isha, Kena, and Katha Upanishad dialogues with publisher-verified Gujarati, Hindi, Marathi, and Tamil commentary.",
                category = "Vedanta & Metaphysics",
                coverBadgeColorHex = "#8B5CF6",
                sourcePdfPath = pdfPath,
                contentHash = BOOK3_FINGERPRINT,
                totalPages = 3,
                fileSizeBytes = pdfSize,
                originalLanguage = "Sanskrit",
                preferredTargetLanguage = "Gujarati",
                accessPolicy = BookAccessPolicy.PAID,
                rightsCategory = BookRightsCategory.PUBLISHER_LICENSED,
                isPublicCatalog = true,
                isSampleBook = false,
                isPaid = true,
                priceDisplay = "₹199 • Paid Edition",
                publisherName = "Bharatiya Vidya Prakashan",
                totalChapters = 1,
                totalSections = 1,
                totalParagraphs = 2
            )
        )
        val chId = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: ईशावास्योपनिषद् (Isha Upanishad)",
                summary = "Invocation of completeness (Purnamadah) and sacred immanence.",
                startPage = 1,
                endPage = 3
            )
        )
        val secId = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = chId,
                sectionOrder = 1,
                heading = "Section 1.1: The Fullness Invocation (पूर्णमदः पूर्णमिदम्)",
                startPage = 1,
                endPage = 3
            )
        )

        val p1Text = "ॐ पूर्णमदः पूर्णमिदं पूर्णात्पूर्णमुदच्यते। पूर्णस्य पूर्णमादाय पूर्णमेवावशिष्यते॥"
        val p1Hash = SecurityVault.computeContentFingerprint(p1Text)
        val p2Text = "ईशावास्यमिदं सर्वं यत्किञ्च जगत्यां जगत्। तेन त्यक्तेन भुञ्जीथा मा गृधः कस्यस्विद्धनम्॥"
        val p2Hash = SecurityVault.computeContentFingerprint(p2Text)

        val pIds = dao.insertParagraphs(
            listOf(
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 1,
                    originalPageNumber = 1,
                    rawOcrText = p1Text,
                    originalText = p1Text,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${p1Text.lowercase()} purnamadah purnamidam fullness invocation",
                    ocrVersion = 1,
                    sourceContentHash = p1Hash
                ),
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 2,
                    originalPageNumber = 2,
                    rawOcrText = p2Text,
                    originalText = p2Text,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${p2Text.lowercase()} isavasya idam sarvam",
                    ocrVersion = 1,
                    sourceContentHash = p2Hash
                )
            )
        )

        dao.insertTranslations(
            listOf(
                ParagraphTranslationEntity(
                    bookId = bookId,
                    sectionId = secId,
                    paragraphId = pIds[0],
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Gujarati",
                    translatedText = "ૐ તે પરબ્રહ્મ પૂર્ણ છે અને આ દૃશ્યમાન જગત પણ પૂર્ણ છે; પૂર્ણમાંથી જ પૂર્ણની ઉત્પત્તિ થાય છે. પૂર્ણમાંથી પૂર્ણ લઈ લેવામાં આવે તો પણ પાછળ પૂર્ણ જ શેષ રહે છે.",
                    transliterationOrNotes = "oṃ pūrṇamadaḥ pūrṇamidaṃ pūrṇātpūrṇamudacyate",
                    versionNumber = 1,
                    isActiveVersion = true,
                    providerUsed = "Publisher Official Corpus",
                    modelUsed = "Human + Claude 3.7 Sonnet",
                    sourceContentVersion = 1,
                    sourceContentHash = p1Hash,
                    reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
                    accessPolicy = TranslationAccessPolicy.PAID_USERS,
                    commercialRightsStatus = "PUBLISHER_RESTRICTED",
                    reuseCount = 11
                ),
                ParagraphTranslationEntity(
                    bookId = bookId,
                    sectionId = secId,
                    paragraphId = pIds[1],
                    sourceLanguage = "Sanskrit",
                    targetLanguage = "Gujarati",
                    translatedText = "આ ગતિશીલ જગતમાં જે કંઈ પણ છે તે સર્વ ઈશ્વરથી વ્યાપ્ત છે. તેથી ત્યાગભાવપૂર્વક તેનો આનંદ માણો; કોઈના પણ ધનની લાલસા ન કરો.",
                    transliterationOrNotes = "īśāvāsyamidaṃ sarvaṃ yatkiñca jagatyāṃ jagat",
                    versionNumber = 1,
                    isActiveVersion = true,
                    providerUsed = "Publisher Official Corpus",
                    modelUsed = "Human + Claude 3.7 Sonnet",
                    sourceContentVersion = 1,
                    sourceContentHash = p2Hash,
                    reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
                    accessPolicy = TranslationAccessPolicy.PAID_USERS,
                    commercialRightsStatus = "PUBLISHER_RESTRICTED",
                    reuseCount = 8
                )
            )
        )
    }

    private suspend fun seedBook4RestrictedResearch(
        dao: PolyglotDao,
        pdfPath: String,
        pdfSize: Long
    ) {
        val bookId = dao.insertBook(
            BookEntity(
                title = "नव्य-न्याय तर्क भाष्य — Navya-Nyaya Logic & Epistemology",
                subtitle = "Institutional & Pro Research Monograph on Pramana Theory",
                authorOrSource = "Gangesha Upadhyaya • Mithila Research Archive",
                description = "Restricted academic edition on Indian formal logic (Pramana, Vyapti, Anumana). Visible & accessible to Pro / Research and Institutional subscribers.",
                category = "Epistemology & Logic (Research)",
                coverBadgeColorHex = "#EC4899",
                sourcePdfPath = pdfPath,
                contentHash = BOOK4_FINGERPRINT,
                totalPages = 2,
                fileSizeBytes = pdfSize,
                originalLanguage = "Sanskrit",
                preferredTargetLanguage = "Gujarati",
                accessPolicy = BookAccessPolicy.RESTRICTED,
                rightsCategory = BookRightsCategory.PUBLISHER_LICENSED,
                isPublicCatalog = false,
                isSampleBook = false,
                isPaid = true,
                priceDisplay = "Pro / Institution Only",
                publisherName = "Mithila University Press",
                totalChapters = 1,
                totalSections = 1,
                totalParagraphs = 1
            )
        )
        val chId = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: प्रत्यक्षखण्डः (Theory of Perception)",
                summary = "Formal analysis of cognition and valid means of knowledge (Pramana).",
                startPage = 1,
                endPage = 2
            )
        )
        val secId = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = chId,
                sectionOrder = 1,
                heading = "Section 1.1: Definition of Vyapti (Invariable Concomitance)",
                startPage = 1,
                endPage = 2
            )
        )
        val pText = "प्रमाकरणं प्रमाणम्। यत्र यत्र धूमस्तत्र तत्र वह्निरिति साहचर्यनियमो व्याप्तिः॥"
        val pHash = SecurityVault.computeContentFingerprint(pText)
        val pIds = dao.insertParagraphs(
            listOf(
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 1,
                    originalPageNumber = 1,
                    rawOcrText = pText,
                    originalText = pText,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${pText.lowercase()} pramana vyapti logic smoke fire inference",
                    ocrVersion = 1,
                    sourceContentHash = pHash
                )
            )
        )
        dao.insertTranslation(
            ParagraphTranslationEntity(
                bookId = bookId,
                sectionId = secId,
                paragraphId = pIds[0],
                sourceLanguage = "Sanskrit",
                targetLanguage = "Gujarati",
                translatedText = "યથાર્થ જ્ઞાન (પ્રમા) ના અસાધારણ કારણને ‘પ્રમાણ’ કહે છે. ‘જ્યાં જ્યાં ધુમાડો છે ત્યાં ત્યાં અગ્નિ છે’—આવા અવિનાભાવી સાહચર્ય નિયમને ન્યાયશાસ્ત્રમાં ‘વ્યાપ્તિ’ કહેવામાં આવે છે.",
                transliterationOrNotes = "pramākaraṇaṃ pramāṇam | yatra yatra dhūmastatra tatra vahniḥ",
                versionNumber = 1,
                isActiveVersion = true,
                providerUsed = "Mithila Faculty Review",
                modelUsed = "Expert Human + GPT-4o",
                sourceContentVersion = 1,
                sourceContentHash = pHash,
                reviewStatus = TranslationReviewStatus.EXPERT_VERIFIED,
                accessPolicy = TranslationAccessPolicy.INSTITUTION_ONLY,
                commercialRightsStatus = "PUBLISHER_RESTRICTED",
                reuseCount = 6
            )
        )
    }

    private suspend fun seedBook5PublisherOnly(
        dao: PolyglotDao,
        pdfPath: String,
        pdfSize: Long
    ) {
        val bookId = dao.insertBook(
            BookEntity(
                title = "संस्कृत काव्यशास्त्र पाण्डुलिपि — Classical Poetics Manuscript",
                subtitle = "Pre-Release Publisher Editorial Edition (Dhvanyaloka)",
                authorOrSource = "Anandavardhana • Publisher Editorial Desk",
                description = "Publisher-controlled manuscript under editorial approval workflow. Accessible only to accounts with Publisher / Admin entitlement.",
                category = "Publisher Editorial Queue",
                coverBadgeColorHex = "#10B981",
                sourcePdfPath = pdfPath,
                contentHash = BOOK5_FINGERPRINT,
                totalPages = 1,
                fileSizeBytes = pdfSize,
                originalLanguage = "Sanskrit",
                preferredTargetLanguage = "Gujarati",
                accessPolicy = BookAccessPolicy.PUBLISHER_ONLY,
                rightsCategory = BookRightsCategory.PUBLISHER_LICENSED,
                isPublicCatalog = false,
                isSampleBook = false,
                isPaid = true,
                priceDisplay = "Publisher Controlled",
                publisherName = "Polyglot Editorial Press",
                totalChapters = 1,
                totalSections = 1,
                totalParagraphs = 1
            )
        )
        val chId = dao.insertChapter(
            ChapterEntity(
                bookId = bookId,
                chapterOrder = 1,
                title = "Chapter 1: ध्वनिस्वरूपम् (The Soul of Poetry)",
                summary = "Aesthetic resonance (Dhvani) and Rasa in Sanskrit poetics.",
                startPage = 1,
                endPage = 1
            )
        )
        val secId = dao.insertSection(
            SectionEntity(
                bookId = bookId,
                chapterId = chId,
                sectionOrder = 1,
                heading = "Section 1.1: Kavyasyatma Dhvanih",
                startPage = 1,
                endPage = 1
            )
        )
        val pText = "काव्यस्यात्मा ध्वनिरिति बुधैर्यः समाम्नातपूर्वः।"
        val pHash = SecurityVault.computeContentFingerprint(pText)
        val pIds = dao.insertParagraphs(
            listOf(
                ParagraphEntity(
                    bookId = bookId,
                    chapterId = chId,
                    sectionId = secId,
                    paragraphOrder = 1,
                    originalPageNumber = 1,
                    rawOcrText = pText,
                    originalText = pText,
                    originalLanguage = "Sanskrit",
                    normalizedSearchText = "${pText.lowercase()} kavyasyatma dhvanih soul of poetry",
                    ocrVersion = 1,
                    sourceContentHash = pHash
                )
            )
        )
        dao.insertTranslation(
            ParagraphTranslationEntity(
                bookId = bookId,
                sectionId = secId,
                paragraphId = pIds[0],
                sourceLanguage = "Sanskrit",
                targetLanguage = "Gujarati",
                translatedText = "કાવ્યનો આત્મા ‘ધ્વનિ’ (વ્યંગ્યાર્થ) છે—એવું વિદ્વાન આચાર્યોએ પૂર્વે જ પ્રતિપાદિત કર્યું છે.",
                transliterationOrNotes = "kāvyasyātmā dhvaniriti budhairyaḥ samāmnātapūrvaḥ",
                versionNumber = 1,
                isActiveVersion = true,
                providerUsed = "Editorial Desk",
                modelUsed = "Publisher Master v1",
                sourceContentVersion = 1,
                sourceContentHash = pHash,
                reviewStatus = TranslationReviewStatus.PUBLISHER_APPROVED,
                accessPolicy = TranslationAccessPolicy.PUBLISHER_CONTROLLED,
                commercialRightsStatus = "PUBLISHER_RESTRICTED",
                reuseCount = 3
            )
        )
    }

    private suspend fun ensurePlatformConfigAndSuperadminSeeded(dao: PolyglotDao) {
        val now = System.currentTimeMillis()

        // 1. Seed Default Commercial Plans if none exist (No hardcoded commercial rules in UI!)
        val existingPlans = dao.getAllCommercialPlans()
        if (existingPlans.isEmpty()) {
            val defaultPlans = listOf(
                CommercialPlanEntity(
                    planCode = SubscriptionTier.FREE,
                    name = "Free Registered Reader",
                    description = "Read public & free books, reuse PUBLIC/FREE shared translations, and bring your own AI API key for P2P extraction.",
                    isEnabled = true,
                    monthlyPriceAmount = 0.0,
                    yearlyPriceAmount = 0.0,
                    currencyCode = "USD",
                    trialDays = 0,
                    displayOrder = 1,
                    isFeatured = false,
                    featuresCsv = "PUBLIC_BOOKS,FREE_BOOKS,BYO_AI_KEY,P2P_EXTRACTION,TOP_3_LANGUAGE_SELECTOR",
                    allowedBookAccessPoliciesCsv = "PUBLIC,FREE",
                    dailyTranslationLimit = 25,
                    monthlyManagedAiTokensLimit = 0,
                    offlineAccessEnabled = false,
                    advancedSearchAndScholarlyEnabled = false,
                    ttsAudioDailyMinutesLimit = 30,
                    createdAt = now,
                    updatedAt = now
                ),
                CommercialPlanEntity(
                    planCode = SubscriptionTier.READER,
                    name = "VidyaKosha Reader Plan",
                    description = "Unlocks PAID_USERS verified translations, offline reading packages, enhanced Indian TTS, and Ask This Book grounded passage citations.",
                    isEnabled = true,
                    monthlyPriceAmount = 12.0,
                    yearlyPriceAmount = 119.0,
                    currencyCode = "USD",
                    trialDays = 7,
                    displayOrder = 2,
                    isFeatured = true,
                    featuresCsv = "PUBLIC_BOOKS,FREE_BOOKS,PAID_TRANSLATIONS,OFFLINE_SYNC,ENHANCED_TTS,ASK_THIS_BOOK",
                    allowedBookAccessPoliciesCsv = "PUBLIC,FREE,PAID",
                    dailyTranslationLimit = 250,
                    monthlyManagedAiTokensLimit = 150000,
                    offlineAccessEnabled = true,
                    advancedSearchAndScholarlyEnabled = false,
                    ttsAudioDailyMinutesLimit = 180,
                    createdAt = now,
                    updatedAt = now
                ),
                CommercialPlanEntity(
                    planCode = SubscriptionTier.PRO_RESEARCH,
                    name = "Pro & Scholarly Research",
                    description = "Full access to RESTRICTED research monographs, PAID collector editions, Cross-Book IAST search, and Word-by-Word Pada commentary.",
                    isEnabled = true,
                    monthlyPriceAmount = 29.0,
                    yearlyPriceAmount = 290.0,
                    currencyCode = "USD",
                    trialDays = 14,
                    displayOrder = 3,
                    isFeatured = false,
                    featuresCsv = "ALL_READER_FEATURES,RESTRICTED_ARCHIVE,PAID_BOOKS,CROSS_BOOK_SEARCH,SCHOLARLY_PADA_MODE,VERSION_COMPARISON",
                    allowedBookAccessPoliciesCsv = "PUBLIC,FREE,PAID,RESTRICTED,SPECIFIC_PLAN_ONLY,INSTITUTION_ONLY",
                    dailyTranslationLimit = -1,
                    monthlyManagedAiTokensLimit = 750000,
                    offlineAccessEnabled = true,
                    advancedSearchAndScholarlyEnabled = true,
                    ttsAudioDailyMinutesLimit = -1,
                    createdAt = now,
                    updatedAt = now
                ),
                CommercialPlanEntity(
                    planCode = SubscriptionTier.PUBLISHER_INSTITUTION,
                    name = "Publisher & University Licence",
                    description = "Institutional seat licensing, Publisher-controlled manuscript access, human editorial verification, and corpus governance.",
                    isEnabled = true,
                    monthlyPriceAmount = 99.0,
                    yearlyPriceAmount = 990.0,
                    currencyCode = "USD",
                    trialDays = 30,
                    displayOrder = 4,
                    isFeatured = false,
                    featuresCsv = "ALL_PRO_FEATURES,PUBLISHER_CMS,HUMAN_EDITORIAL_APPROVAL,INSTITUTIONAL_SEATS,CORPUS_ANALYTICS",
                    allowedBookAccessPoliciesCsv = "PUBLIC,FREE,PAID,RESTRICTED,SPECIFIC_PLAN_ONLY,INDIVIDUAL_PURCHASE,INSTITUTION_ONLY,PUBLISHER_ONLY",
                    dailyTranslationLimit = -1,
                    monthlyManagedAiTokensLimit = 3000000,
                    offlineAccessEnabled = true,
                    advancedSearchAndScholarlyEnabled = true,
                    ttsAudioDailyMinutesLimit = -1,
                    createdAt = now,
                    updatedAt = now
                )
            )
            defaultPlans.forEach { dao.insertCommercialPlan(it) }
        }

        // 2. Seed Reusable Platform Metadata Registry if none exist
        val existingMetadata = dao.getAllPlatformMetadata()
        if (existingMetadata.isEmpty()) {
            val defaultMeta = listOf(
                // Categories
                PlatformMetadataEntity(metadataType = MetadataCategoryType.CATEGORY, keyCode = "CLASSICAL_WISDOM", displayLabel = "Classical Wisdom & Ethics", description = "Subhashitas, Hitopadesha, Niti Shastras", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.CATEGORY, keyCode = "AYURVEDA_YOGA", displayLabel = "Ayurveda & Yoga Philosophy", description = "Patanjali Yoga Sutras, Charaka & Sushruta Samhita", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.CATEGORY, keyCode = "UPANISHADS_VEDANTA", displayLabel = "Vedanta & Upanishads", description = "Principal Upanishads, Brahma Sutras & Commentaries", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.CATEGORY, keyCode = "NYAYA_EPISTEMOLOGY", displayLabel = "Indian Epistemology & Logic", description = "Nyaya, Vaisheshika & Navya-Nyaya treatises", sortOrder = 4),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.CATEGORY, keyCode = "SANSKRIT_POETICS", displayLabel = "Classical Poetics & Alaṅkāra", description = "Dhvanyaloka, Kavyaprakasha & Natyashastra", sortOrder = 5),
                // Genres
                PlatformMetadataEntity(metadataType = MetadataCategoryType.GENRE, keyCode = "PHILOSOPHY", displayLabel = "Darshana (Philosophy)", description = "Classical philosophical treatises", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.GENRE, keyCode = "SCIENTIFIC_TREATISE", displayLabel = "Shastra (Scientific Treatise)", description = "Medicine, grammar, astronomy, logic", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.GENRE, keyCode = "POETRY_DRAMA", displayLabel = "Kavya (Poetry & Aesthetics)", description = "Lyric, epic, and dramatic works", sortOrder = 3),
                // Tags
                PlatformMetadataEntity(metadataType = MetadataCategoryType.TAG, keyCode = "BILINGUAL_EDITION", displayLabel = "Bilingual Critical Edition", description = "Side-by-side original and target script", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.TAG, keyCode = "IAST_BREAKDOWN", displayLabel = "IAST Word-by-Word Pada", description = "Includes transliteration and grammatical gloss", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.TAG, keyCode = "P2P_DERIVATIVE", displayLabel = "Zero-Host P2P Derivative", description = "Converted ephemerally without hosting source binary", sortOrder = 3),
                // Book Statuses
                PlatformMetadataEntity(metadataType = MetadataCategoryType.BOOK_STATUS, keyCode = BookPublicationStatus.PUBLISHED, displayLabel = "Published", description = "Live in eligible catalogue", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.BOOK_STATUS, keyCode = BookPublicationStatus.DRAFT, displayLabel = "Draft", description = "Under editorial preparation; hidden from public", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.BOOK_STATUS, keyCode = BookPublicationStatus.UNPUBLISHED, displayLabel = "Unpublished", description = "Withdrawn from public catalogue; data preserved", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.BOOK_STATUS, keyCode = BookPublicationStatus.ARCHIVED, displayLabel = "Archived", description = "Archived for historical retention; OCR & translations preserved", sortOrder = 4),
                // Review Statuses
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.AI_GENERATED, displayLabel = "AI Generated", description = "Initial machine translation", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.AI_REVIEWED, displayLabel = "AI Reviewed", description = "Secondary AI verification pass", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.HUMAN_REVIEWED, displayLabel = "Human Reviewed", description = "Community or editorial human review", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.EXPERT_VERIFIED, displayLabel = "Expert Verified", description = "Verified by language/domain scholar", sortOrder = 4),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.PUBLISHER_APPROVED, displayLabel = "Publisher Approved", description = "Canonical publisher-approved edition", sortOrder = 5),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.REVIEW_STATUS, keyCode = TranslationReviewStatus.REJECTED, displayLabel = "Rejected", description = "Rejected by admin/editorial review (preserved in history)", sortOrder = 6),
                // Access Policies
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.PUBLIC, displayLabel = "Public", description = "Open to all readers including guests", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.FREE, displayLabel = "Free Registered Users", description = "Free for any signed-in account", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.PAID, displayLabel = "Paid Subscribers", description = "Requires active paid subscription or purchase", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.SPECIFIC_PLAN_ONLY, displayLabel = "Specific Plan Only", description = "Restricted to a designated plan code", sortOrder = 4),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.INDIVIDUAL_PURCHASE, displayLabel = "Individual Book Purchase", description = "Requires a per-title purchase/grant", sortOrder = 5),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.INSTITUTION_ONLY, displayLabel = "Institution Only", description = "Requires university/institutional membership", sortOrder = 6),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.PUBLISHER_ONLY, displayLabel = "Publisher Controlled", description = "Requires publisher or superadmin role", sortOrder = 7),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.ACCESS_POLICY, keyCode = BookAccessPolicy.PRIVATE, displayLabel = "Private / Unpublished", description = "Strictly private to owner or superadmin", sortOrder = 8),
                // Plan Features
                PlatformMetadataEntity(metadataType = MetadataCategoryType.PLAN_FEATURE, keyCode = "OFFLINE_SYNC", displayLabel = "Offline Book Packages", description = "Download structured OCR & translations for offline reading", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.PLAN_FEATURE, keyCode = "ENHANCED_TTS", displayLabel = "Enhanced Multilingual TTS", description = "Continuous section audio & pronunciation", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.PLAN_FEATURE, keyCode = "ASK_THIS_BOOK", displayLabel = "Ask This Book Grounded AI", description = "Passage-cited Q&A grounded in book text", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.PLAN_FEATURE, keyCode = "CROSS_BOOK_SEARCH", displayLabel = "Cross-Book Corpus Search", description = "Search original script, translations & IAST across books", sortOrder = 4),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.PLAN_FEATURE, keyCode = "SCHOLARLY_PADA_MODE", displayLabel = "Word-by-Word Pada Study", description = "IAST transliteration & grammatical breakdown", sortOrder = 5),
                // Featured Languages in Registry
                PlatformMetadataEntity(metadataType = MetadataCategoryType.LANGUAGE, keyCode = "sa", displayLabel = "Sanskrit (संस्कृतम्)", description = "Classical Devanagari & IAST", sortOrder = 1),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.LANGUAGE, keyCode = "gu", displayLabel = "Gujarati (ગુજરાતી)", description = "Primary Indian Translation Target", sortOrder = 2),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.LANGUAGE, keyCode = "hi", displayLabel = "Hindi (हिन्दी)", description = "Primary Devanagari Translation Target", sortOrder = 3),
                PlatformMetadataEntity(metadataType = MetadataCategoryType.LANGUAGE, keyCode = "en", displayLabel = "English", description = "Global Scholarly & Natural Translation Target", sortOrder = 4)
            )
            defaultMeta.forEach { dao.insertPlatformMetadata(it) }
        }

        // 3. Seed Protected Superadmin Account if not present
        val existingSuperadmin = dao.getUserByEmail(DEFAULT_SUPERADMIN_EMAIL)
        if (existingSuperadmin == null) {
            val salt = SecurityVault.generateSalt()
            val hash = SecurityVault.hashPassword(DEFAULT_SUPERADMIN_PASSWORD, salt)
            val superadminId = dao.insertUser(
                UserEntity(
                    email = DEFAULT_SUPERADMIN_EMAIL,
                    displayName = "VidyaKosha Superadmin",
                    passwordHash = hash,
                    passwordSalt = salt,
                    role = UserRole.SUPERADMIN,
                    accountStatus = AccountStatus.ACTIVE,
                    preferredLanguage = "Gujarati",
                    topThreeLanguagesCsv = "Gujarati,Hindi,English",
                    activeTierCode = SubscriptionTier.PUBLISHER_INSTITUTION,
                    institutionCode = "INST_VIDYAKOSHA_FOUNDATION",
                    hasAcceptedContributorLicense = true,
                    createdAt = now - 86_400_000L * 14,
                    lastLoginAt = now
                )
            )
            dao.insertEntitlement(
                UserEntitlementEntity(
                    userId = superadminId,
                    entitlementKind = EntitlementKind.SUBSCRIPTION_TIER,
                    targetRef = SubscriptionTier.PUBLISHER_INSTITUTION,
                    displayTitle = "Superadmin Platform All-Access & Governance",
                    status = "ACTIVE",
                    grantedByAdminId = superadminId,
                    grantedByAdminName = "System Bootstrap",
                    reasonOrNote = "Default founder Superadmin entitlement"
                )
            )
            dao.insertEntitlement(
                UserEntitlementEntity(
                    userId = superadminId,
                    entitlementKind = EntitlementKind.ADMIN_GRANTED_ACCESS,
                    targetRef = "ALL_PLATFORM_BOOKS",
                    displayTitle = "Unrestricted Superadmin Catalogue Access",
                    status = "ACTIVE",
                    grantedByAdminId = superadminId,
                    grantedByAdminName = "System Bootstrap",
                    reasonOrNote = "Superadmin global entitlement"
                )
            )
            dao.insertAdminAuditLog(
                AdminAuditLogEntity(
                    adminUserId = superadminId,
                    adminEmailOrName = DEFAULT_SUPERADMIN_EMAIL,
                    adminRole = UserRole.SUPERADMIN,
                    action = "PLATFORM_SUPERADMIN_INITIALIZED",
                    entityAffected = "SYSTEM_CONFIG",
                    entityId = "VIDYAKOSHA_V6",
                    previousValue = "NONE",
                    newValue = "4 Commercial Plans, 25 Metadata Keys, Superadmin RBAC Active",
                    reasonOrNote = "Automated database bootstrap of commercial plans, metadata registry, and Superadmin RBAC",
                    timestamp = now
                )
            )
        } else if (existingSuperadmin.role != UserRole.SUPERADMIN) {
            dao.updateUser(existingSuperadmin.copy(role = UserRole.SUPERADMIN, accountStatus = AccountStatus.ACTIVE))
        }
    }
}
