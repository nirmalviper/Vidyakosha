package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Extensible Language Catalog Registry supporting all major Indian languages
 * and global languages, with automatic TTS locale and script metadata.
 */
@Serializable
data class LanguageDescriptor(
    val code: String,
    val name: String,
    val nativeName: String,
    val scriptName: String,
    val regionGroup: String,
    val flagOrSymbol: String,
    val ttsLocaleTag: String,
    val fallbackTtsTag: String = "hi-IN"
)

object LanguageRegistry {
    const val MAX_USER_ACTIVE_LANGUAGES = 3
    const val DEFAULT_TOP_THREE_CSV = "Gujarati,Hindi,English"

    val INDIAN_LANGUAGES: List<LanguageDescriptor> = listOf(
        // 22 Scheduled Languages of India
        LanguageDescriptor("gu", "Gujarati", "ગુજરાતી", "Gujarati", "Indian Languages", "🇮🇳 ગ", "gu-IN"),
        LanguageDescriptor("hi", "Hindi", "हिन्दी", "Devanagari", "Indian Languages", "🇮🇳 अ", "hi-IN"),
        LanguageDescriptor("sa", "Sanskrit", "संस्कृतम्", "Devanagari", "Indian Languages", "🕉️ सं", "sa-IN", "hi-IN"),
        LanguageDescriptor("mr", "Marathi", "मराठी", "Devanagari", "Indian Languages", "🇮🇳 म", "mr-IN"),
        LanguageDescriptor("bn", "Bengali", "বাংলা", "Bengali", "Indian Languages", "🇮🇳 অ", "bn-IN"),
        LanguageDescriptor("ta", "Tamil", "தமிழ்", "Tamil", "Indian Languages", "🇮🇳 அ", "ta-IN"),
        LanguageDescriptor("te", "Telugu", "తెలుగు", "Telugu", "Indian Languages", "🇮🇳 అ", "te-IN"),
        LanguageDescriptor("kn", "Kannada", "ಕನ್ನಡ", "Kannada", "Indian Languages", "🇮🇳 ಅ", "kn-IN"),
        LanguageDescriptor("ml", "Malayalam", "മലയാളം", "Malayalam", "Indian Languages", "🇮🇳 അ", "ml-IN"),
        LanguageDescriptor("pa", "Punjabi", "ਪੰਜਾਬੀ", "Gurmukhi", "Indian Languages", "🇮🇳 ਅ", "pa-IN"),
        LanguageDescriptor("or", "Odia", "ଓଡ଼ିଆ", "Odia", "Indian Languages", "🇮🇳 ଅ", "or-IN", "bn-IN"),
        LanguageDescriptor("as", "Assamese", "অসমীয়া", "Assamese", "Indian Languages", "🇮🇳 অ", "as-IN", "bn-IN"),
        LanguageDescriptor("ur", "Urdu", "اردو", "Perso-Arabic", "Indian Languages", "🇮🇳 ا", "ur-IN", "hi-IN"),
        LanguageDescriptor("ne", "Nepali", "नेपाली", "Devanagari", "Indian Languages", "🇳🇵 ने", "ne-NP", "hi-IN"),
        LanguageDescriptor("kok", "Konkani", "कोंकणी", "Devanagari", "Indian Languages", "🇮🇳 कों", "kok-IN", "mr-IN"),
        LanguageDescriptor("mai", "Maithili", "मैथिली", "Devanagari", "Indian Languages", "🇮🇳 मै", "mai-IN", "hi-IN"),
        LanguageDescriptor("sd", "Sindhi", "सिन्धी / سنڌي", "Devanagari", "Indian Languages", "🇮🇳 सि", "sd-IN", "hi-IN"),
        LanguageDescriptor("ks", "Kashmiri", "कॉशुर / کٲشُر", "Devanagari", "Indian Languages", "🇮🇳 कॉ", "ks-IN", "hi-IN"),
        LanguageDescriptor("mni", "Manipuri (Meitei)", "মৈতৈলোন্", "Meitei Mayek", "Indian Languages", "🇮🇳 মৈ", "mni-IN", "bn-IN"),
        LanguageDescriptor("doi", "Dogri", "डोगरी", "Devanagari", "Indian Languages", "🇮🇳 डो", "doi-IN", "hi-IN"),
        LanguageDescriptor("sat", "Santali", "ᱥᱟᱱᱛᱟᱲᱤ", "Ol Chiki", "Indian Languages", "🇮🇳 ᱥ", "sat-IN", "hi-IN"),
        LanguageDescriptor("brx", "Bodo", "बर'", "Devanagari", "Indian Languages", "🇮🇳 ब", "brx-IN", "hi-IN"),
        // Classical, Regional & Liturgical Subcontinental Languages
        LanguageDescriptor("pi", "Pali", "पालि", "Devanagari/Brahmi", "Indian Languages", "☸️ पा", "sa-IN", "hi-IN"),
        LanguageDescriptor("pra", "Prakrit (Ardhamagadhi)", "प्राकृतम्", "Devanagari", "Indian Languages", "🕉️ प्रा", "sa-IN", "hi-IN"),
        LanguageDescriptor("awa", "Awadhi", "अवधी", "Devanagari", "Indian Languages", "🇮🇳 अव", "hi-IN"),
        LanguageDescriptor("bho", "Bhojpuri", "भोजपुरी", "Devanagari", "Indian Languages", "🇮🇳 भो", "hi-IN"),
        LanguageDescriptor("mag", "Magahi", "मगही", "Devanagari", "Indian Languages", "🇮🇳 मग", "hi-IN"),
        LanguageDescriptor("bra", "Braj Bhasha", "ब्रजभाषा", "Devanagari", "Indian Languages", "🇮🇳 ब्र", "hi-IN"),
        LanguageDescriptor("raj", "Rajasthani", "राजस्थानी", "Devanagari", "Indian Languages", "🇮🇳 रा", "hi-IN"),
        LanguageDescriptor("mwr", "Marwari", "मारवाड़ी", "Devanagari", "Indian Languages", "🇮🇳 मा", "hi-IN"),
        LanguageDescriptor("bgc", "Haryanvi", "हरियाणवी", "Devanagari", "Indian Languages", "🇮🇳 ह", "hi-IN"),
        LanguageDescriptor("hne", "Chhattisgarhi", "छत्तीसगढ़ी", "Devanagari", "Indian Languages", "🇮🇳 छ", "hi-IN"),
        LanguageDescriptor("gbm", "Garhwali", "गढ़वळि", "Devanagari", "Indian Languages", "🇮🇳 ग", "hi-IN"),
        LanguageDescriptor("kfy", "Kumaoni", "कुमाऊँनी", "Devanagari", "Indian Languages", "🇮🇳 कु", "hi-IN"),
        LanguageDescriptor("tcy", "Tulu", "ತುಳು", "Kannada/Tigalari", "Indian Languages", "🇮🇳 ತು", "kn-IN"),
        LanguageDescriptor("kfa", "Kodava", "ಕೊಡವ", "Kannada", "Indian Languages", "🇮🇳 ಕೊ", "kn-IN"),
        LanguageDescriptor("kfr", "Kutchi", "કચ્છી", "Gujarati", "Indian Languages", "🇮🇳 ક", "gu-IN"),
        LanguageDescriptor("saz", "Saurashtra", "ꢱꣃꢬꢵꢰ꣄ꢜ꣄ꢬ", "Saurashtra/Tamil", "Indian Languages", "🇮🇳 সৌ", "ta-IN"),
        LanguageDescriptor("lus", "Mizo", "Mizo ṭawng", "Latin", "Indian Languages", "🇮🇳 MZ", "en-IN"),
        LanguageDescriptor("kha", "Khasi", "Ka Ktien Khasi", "Latin", "Indian Languages", "🇮🇳 KH", "en-IN"),
        LanguageDescriptor("grt", "Garo", "A·chikku", "Latin", "Indian Languages", "🇮🇳 GR", "en-IN"),
        LanguageDescriptor("trp", "Kokborok", "কোকবরক", "Bengali/Latin", "Indian Languages", "🇮🇳 কো", "bn-IN"),
        LanguageDescriptor("lbj", "Ladakhi", "ལ་དྭགས་སྐད་", "Tibetan", "Indian Languages", "🇮🇳 ལ", "hi-IN"),
        LanguageDescriptor("si", "Sinhala", "සිංහල", "Sinhala", "Indian Languages", "🇱🇰 සි", "si-LK", "ta-IN"),
        LanguageDescriptor("bo", "Tibetan", "བོད་ཡིག", "Tibetan", "Indian Languages", "🏔️ བོ", "hi-IN")
    )

    val GLOBAL_LANGUAGES: List<LanguageDescriptor> = listOf(
        // Major World & European Languages
        LanguageDescriptor("en", "English", "English", "Latin", "Global Languages", "🇬🇧 EN", "en-IN", "en-US"),
        LanguageDescriptor("es", "Spanish", "Español", "Latin", "Global Languages", "🇪🇸 ES", "es-ES"),
        LanguageDescriptor("fr", "French", "Français", "Latin", "Global Languages", "🇫🇷 FR", "fr-FR"),
        LanguageDescriptor("de", "German", "Deutsch", "Latin", "Global Languages", "🇩🇪 DE", "de-DE"),
        LanguageDescriptor("it", "Italian", "Italiano", "Latin", "Global Languages", "🇮🇹 IT", "it-IT"),
        LanguageDescriptor("pt", "Portuguese", "Português", "Latin", "Global Languages", "🇵🇹 PT", "pt-BR"),
        LanguageDescriptor("nl", "Dutch", "Nederlands", "Latin", "Global Languages", "🇳🇱 NL", "nl-NL"),
        LanguageDescriptor("ru", "Russian", "Русский", "Cyrillic", "Global Languages", "🇷🇺 RU", "ru-RU"),
        LanguageDescriptor("uk", "Ukrainian", "Українська", "Cyrillic", "Global Languages", "🇺🇦 UK", "uk-UA"),
        LanguageDescriptor("pl", "Polish", "Polski", "Latin", "Global Languages", "🇵🇱 PL", "pl-PL"),
        LanguageDescriptor("cs", "Czech", "Čeština", "Latin", "Global Languages", "🇨🇿 CS", "cs-CZ"),
        LanguageDescriptor("sk", "Slovak", "Slovenčina", "Latin", "Global Languages", "🇸🇰 SK", "sk-SK"),
        LanguageDescriptor("hu", "Hungarian", "Magyar", "Latin", "Global Languages", "🇭🇺 HU", "hu-HU"),
        LanguageDescriptor("ro", "Romanian", "Română", "Latin", "Global Languages", "🇷🇴 RO", "ro-RO"),
        LanguageDescriptor("bg", "Bulgarian", "Български", "Cyrillic", "Global Languages", "🇧🇬 BG", "bg-BG"),
        LanguageDescriptor("sr", "Serbian", "Српски", "Cyrillic/Latin", "Global Languages", "🇷🇸 SR", "sr-RS"),
        LanguageDescriptor("hr", "Croatian", "Hrvatski", "Latin", "Global Languages", "🇭🇷 HR", "hr-HR"),
        LanguageDescriptor("el", "Greek", "Ελληνικά", "Greek", "Global Languages", "🇬🇷 EL", "el-GR"),
        LanguageDescriptor("sv", "Swedish", "Svenska", "Latin", "Global Languages", "🇸🇪 SV", "sv-SE"),
        LanguageDescriptor("no", "Norwegian", "Norsk", "Latin", "Global Languages", "🇳🇴 NO", "nb-NO"),
        LanguageDescriptor("da", "Danish", "Dansk", "Latin", "Global Languages", "🇩🇰 DA", "da-DK"),
        LanguageDescriptor("fi", "Finnish", "Suomi", "Latin", "Global Languages", "🇫🇮 FI", "fi-FI"),
        LanguageDescriptor("is", "Icelandic", "Íslenska", "Latin", "Global Languages", "🇮🇸 IS", "is-IS"),
        LanguageDescriptor("lt", "Lithuanian", "Lietuvių", "Latin", "Global Languages", "🇱🇹 LT", "lt-LT"),
        LanguageDescriptor("lv", "Latvian", "Latviešu", "Latin", "Global Languages", "🇱🇻 LV", "lv-LV"),
        LanguageDescriptor("et", "Estonian", "Eesti", "Latin", "Global Languages", "🇪🇪 ET", "et-EE"),
        LanguageDescriptor("ga", "Irish", "Gaeilge", "Latin", "Global Languages", "🇮🇪 GA", "en-IE"),
        LanguageDescriptor("cy", "Welsh", "Cymraeg", "Latin", "Global Languages", "🏴󠁧󠁢󠁷󠁬󠁳󠁿 CY", "cy-GB"),
        LanguageDescriptor("ca", "Catalan", "Català", "Latin", "Global Languages", "🇪🇸 CA", "ca-ES"),
        // East & Southeast Asian Languages
        LanguageDescriptor("ja", "Japanese", "日本語", "Kanji/Kana", "Global Languages", "🇯🇵 JA", "ja-JP"),
        LanguageDescriptor("zh", "Mandarin Chinese", "中文 (简体)", "Simplified Hanzi", "Global Languages", "🇨🇳 ZH", "zh-CN"),
        LanguageDescriptor("zh-TW", "Traditional Chinese", "繁體中文", "Traditional Hanzi", "Global Languages", "🇹🇼 ZH", "zh-TW"),
        LanguageDescriptor("ko", "Korean", "한국어", "Hangul", "Global Languages", "🇰🇷 KO", "ko-KR"),
        LanguageDescriptor("vi", "Vietnamese", "Tiếng Việt", "Latin", "Global Languages", "🇻🇳 VI", "vi-VN"),
        LanguageDescriptor("th", "Thai", "ไทย", "Thai", "Global Languages", "🇹🇭 TH", "th-TH"),
        LanguageDescriptor("id", "Indonesian", "Bahasa Indonesia", "Latin", "Global Languages", "🇮🇩 ID", "id-ID"),
        LanguageDescriptor("ms", "Malay", "Bahasa Melayu", "Latin", "Global Languages", "🇲🇾 MS", "ms-MY"),
        LanguageDescriptor("tl", "Tagalog (Filipino)", "Filipino", "Latin", "Global Languages", "🇵🇭 TL", "fil-PH"),
        LanguageDescriptor("my", "Burmese (Myanmar)", "မြန်မာဘာသာ", "Burmese", "Global Languages", "🇲🇲 MY", "my-MM"),
        LanguageDescriptor("km", "Khmer (Cambodian)", "ភាសាខ្មែរ", "Khmer", "Global Languages", "🇰🇭 KM", "km-KH"),
        LanguageDescriptor("lo", "Lao", "ພາສາລາວ", "Lao", "Global Languages", "🇱🇦 LO", "lo-LA"),
        LanguageDescriptor("mn", "Mongolian", "Монгол хэл", "Cyrillic", "Global Languages", "🇲🇳 MN", "mn-MN"),
        // Middle Eastern, Central Asian & Caucasian Languages
        LanguageDescriptor("ar", "Arabic", "العربية", "Arabic", "Global Languages", "🇸🇦 AR", "ar-SA"),
        LanguageDescriptor("fa", "Persian (Farsi)", "فارسی", "Persian", "Global Languages", "🇮🇷 FA", "fa-IR"),
        LanguageDescriptor("he", "Hebrew", "עברית", "Hebrew", "Global Languages", "🇮🇱 HE", "he-IL"),
        LanguageDescriptor("tr", "Turkish", "Türkçe", "Latin", "Global Languages", "🇹🇷 TR", "tr-TR"),
        LanguageDescriptor("ku", "Kurdish", "Kurdî / کوردی", "Latin/Arabic", "Global Languages", "🌍 KU", "tr-TR"),
        LanguageDescriptor("ps", "Pashto", "پښتو", "Pashto", "Global Languages", "🌍 PS", "ur-IN"),
        LanguageDescriptor("kk", "Kazakh", "Қазақ тілі", "Cyrillic", "Global Languages", "🇰🇿 KK", "ru-RU"),
        LanguageDescriptor("uz", "Uzbek", "Oʻzbekcha", "Latin", "Global Languages", "🇺🇿 UZ", "tr-TR"),
        LanguageDescriptor("ka", "Georgian", "ქართული", "Georgian", "Global Languages", "🇬🇪 KA", "en-US"),
        LanguageDescriptor("hy", "Armenian", "Հայերեն", "Armenian", "Global Languages", "🇦🇲 HY", "en-US"),
        // African & Classical Global Languages
        LanguageDescriptor("sw", "Swahili", "Kiswahili", "Latin", "Global Languages", "🇰🇪 SW", "sw-KE"),
        LanguageDescriptor("am", "Amharic", "አማርኛ", "Ge'ez", "Global Languages", "🇪🇹 AM", "en-US"),
        LanguageDescriptor("ha", "Hausa", "Harshen Hausa", "Latin", "Global Languages", "🇳🇬 HA", "en-NG"),
        LanguageDescriptor("yo", "Yoruba", "Èdè Yorùbá", "Latin", "Global Languages", "🇳🇬 YO", "en-NG"),
        LanguageDescriptor("ig", "Igbo", "Asụsụ Igbo", "Latin", "Global Languages", "🇳🇬 IG", "en-NG"),
        LanguageDescriptor("zu", "Zulu", "isiZulu", "Latin", "Global Languages", "🇿🇦 ZU", "zu-ZA"),
        LanguageDescriptor("af", "Afrikaans", "Afrikaans", "Latin", "Global Languages", "🇿🇦 AF", "af-ZA"),
        LanguageDescriptor("la", "Latin (Classical)", "Lingua Latina", "Latin", "Global Languages", "🏛️ LA", "it-IT"),
        LanguageDescriptor("eo", "Esperanto", "Esperanto", "Latin", "Global Languages", "🌍 EO", "es-ES")
    )

    val ALL_LANGUAGES: List<LanguageDescriptor> = INDIAN_LANGUAGES + GLOBAL_LANGUAGES

    fun findByNameOrCode(query: String): LanguageDescriptor {
        val trimmed = query.trim()
        return ALL_LANGUAGES.find {
            it.name.equals(trimmed, ignoreCase = true) ||
                it.code.equals(trimmed, ignoreCase = true) ||
                it.nativeName.equals(trimmed, ignoreCase = true)
        } ?: INDIAN_LANGUAGES.first { it.name == "Gujarati" }
    }

    /**
     * Parses a user's comma-separated Top 3 language preference string into at most 3 distinct
     * LanguageDescriptors. Restricting active UI and server queries to the user's Top 3 languages
     * keeps the reading interface uncluttered and minimizes server load.
     */
    fun parseTopThreeLanguages(csv: String?, fallbackPrimary: String? = null): List<LanguageDescriptor> {
        val rawTokens = csv.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val resolved = mutableListOf<LanguageDescriptor>()
        if (!fallbackPrimary.isNullOrBlank() && rawTokens.isEmpty()) {
            resolved.add(findByNameOrCode(fallbackPrimary))
        }
        rawTokens.forEach { token ->
            val desc = findByNameOrCode(token)
            if (resolved.none { it.code == desc.code }) {
                resolved.add(desc)
            }
        }
        val defaults = listOf("Gujarati", "Hindi", "English", "Sanskrit")
        for (defName in defaults) {
            if (resolved.size >= MAX_USER_ACTIVE_LANGUAGES) break
            val defDesc = findByNameOrCode(defName)
            if (resolved.none { it.code == defDesc.code }) {
                resolved.add(defDesc)
            }
        }
        return resolved.take(MAX_USER_ACTIVE_LANGUAGES)
    }

    fun formatTopThreeCsv(languages: List<LanguageDescriptor>): String {
        val cleaned = languages.distinctBy { it.code }.take(MAX_USER_ACTIVE_LANGUAGES)
        if (cleaned.isEmpty()) return DEFAULT_TOP_THREE_CSV
        return cleaned.joinToString(",") { it.name }
    }
}

// =====================================================================
// ACCESS POLICIES, LICENSING, QUALITY LIFECYCLE & ENTITLEMENT CONSTANTS
// =====================================================================

/**
 * Requirement #4: Book Access Catalogue Policies
 */
object BookAccessPolicy {
    const val PUBLIC = "PUBLIC"                           // Discoverable & readable by everyone (including logged-out guests)
    const val FREE = "FREE"                               // Discoverable by all; free for registered users
    const val PAID = "PAID"                               // Paid subscribers or individual book purchase
    const val SPECIFIC_PLAN_ONLY = "SPECIFIC_PLAN_ONLY"   // Requires a specific commercial plan (e.g. PRO_RESEARCH)
    const val INDIVIDUAL_PURCHASE = "INDIVIDUAL_PURCHASE" // Requires individual book purchase / access grant
    const val INSTITUTION_ONLY = "INSTITUTION_ONLY"       // Requires institutional/organisation entitlement
    const val RESTRICTED = "RESTRICTED"                   // Restricted research archive (Pro/Research or Institution)
    const val PUBLISHER_ONLY = "PUBLISHER_ONLY"           // Controlled by Publisher/Admin entitlement only
    const val PRIVATE = "PRIVATE"                         // Private/unpublished or personal upload; strictly visible to owner/superadmin

    val ALL = listOf(
        PUBLIC,
        FREE,
        PAID,
        SPECIFIC_PLAN_ONLY,
        INDIVIDUAL_PURCHASE,
        INSTITUTION_ONLY,
        RESTRICTED,
        PUBLISHER_ONLY,
        PRIVATE
    )
}

object BookPublicationStatus {
    const val PUBLISHED = "PUBLISHED"
    const val DRAFT = "DRAFT"
    const val UNPUBLISHED = "UNPUBLISHED"
    const val ARCHIVED = "ARCHIVED"

    val ALL = listOf(PUBLISHED, DRAFT, UNPUBLISHED, ARCHIVED)
}

object UserRole {
    const val USER = "user"
    const val CONTRIBUTOR = "contributor"
    const val PUBLISHER = "publisher"
    const val ADMIN = "admin"
    const val SUPERADMIN = "superadmin"

    val ALL = listOf(USER, CONTRIBUTOR, PUBLISHER, ADMIN, SUPERADMIN)
}

object AccountStatus {
    const val ACTIVE = "ACTIVE"
    const val SUSPENDED = "SUSPENDED"

    val ALL = listOf(ACTIVE, SUSPENDED)
}

/**
 * Requirement #3: Book Rights & Commercial Redistribution Classification
 */
object BookRightsCategory {
    const val PUBLIC_DOMAIN = "PUBLIC_DOMAIN"
    const val POLYGLOT_OWNED = "POLYGLOT_OWNED"
    const val PUBLISHER_LICENSED = "PUBLISHER_LICENSED"
    const val P2P_DERIVATIVE_OWNED = "P2P_DERIVATIVE_OWNED" // Zero-host P2P: Original file purged immediately; VidyaKosha owns OCRed + translated derivative
    const val USER_PRIVATE_UPLOAD = "USER_PRIVATE_UPLOAD"
    const val COPYRIGHTED_NO_REDISTRIBUTION = "COPYRIGHTED_NO_REDISTRIBUTION"

    fun allowsSharedRepositoryContribution(rightsCategory: String): Boolean {
        return rightsCategory == PUBLIC_DOMAIN ||
            rightsCategory == POLYGLOT_OWNED ||
            rightsCategory == PUBLISHER_LICENSED ||
            rightsCategory == P2P_DERIVATIVE_OWNED
    }
}

/**
 * Requirement #2: Translation Access & Licensing Policy
 */
object TranslationAccessPolicy {
    const val PUBLIC = "PUBLIC"
    const val FREE_REGISTERED_USERS = "FREE_REGISTERED_USERS"
    const val PAID_USERS = "PAID_USERS"
    const val CONTRIBUTOR_ONLY = "CONTRIBUTOR_ONLY"
    const val PRIVATE = "PRIVATE"
    const val INSTITUTION_ONLY = "INSTITUTION_ONLY"
    const val PUBLISHER_CONTROLLED = "PUBLISHER_CONTROLLED"

    val ALL = listOf(
        PUBLIC,
        FREE_REGISTERED_USERS,
        PAID_USERS,
        CONTRIBUTOR_ONLY,
        PRIVATE,
        INSTITUTION_ONLY,
        PUBLISHER_CONTROLLED
    )
}

/**
 * Requirement #7: Translation Quality & Review Lifecycle
 */
object TranslationReviewStatus {
    const val AI_GENERATED = "AI_GENERATED"
    const val AI_REVIEWED = "AI_REVIEWED"
    const val HUMAN_REVIEWED = "HUMAN_REVIEWED"
    const val EXPERT_VERIFIED = "EXPERT_VERIFIED"
    const val PUBLISHER_APPROVED = "PUBLISHER_APPROVED"
    const val REJECTED = "REJECTED"

    val ORDERED_STAGES = listOf(
        AI_GENERATED,
        AI_REVIEWED,
        HUMAN_REVIEWED,
        EXPERT_VERIFIED,
        PUBLISHER_APPROVED
    )

    val ALL_STATUSES = ORDERED_STAGES + REJECTED
}

/**
 * Requirement #10: Persistent Background Job Types & Statuses
 */
object JobType {
    const val OCR = "OCR"
    const val TRANSLATION = "TRANSLATION"
    const val RETRANSLATION = "RETRANSLATION"
    const val TTS = "TTS"
    const val SEARCH_INDEX = "SEARCH_INDEX"
    const val EMBEDDING = "EMBEDDING"
}

object JobStatus {
    const val PENDING = "PENDING"
    const val PROCESSING = "PROCESSING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val RETRYING = "RETRYING"
}

/**
 * Requirement #18 & #19: Subscription Tiers & Granular Entitlement Types
 */
object SubscriptionTier {
    const val FREE = "FREE"                               // $0 — Free/public books, eligible translations, basic reader, BYO AI
    const val READER = "READER"                           // $12/mo — Offline reading, enhanced TTS, notes, Ask This Book, advanced search
    const val PRO_RESEARCH = "PRO_RESEARCH"               // $29/mo — Cross-book research, scholarly mode, version comparison, restricted catalog
    const val PUBLISHER_INSTITUTION = "PUBLISHER_INSTITUTION" // Custom — Book & rights CMS, official translations, human review, analytics
}

object EntitlementKind {
    const val FREE_ACCOUNT = "FREE_ACCOUNT"                     // Free registered account grant
    const val SUBSCRIPTION_TIER = "SUBSCRIPTION_TIER"           // Subscription plan grant (e.g. READER, PRO_RESEARCH)
    const val BOOK_PURCHASE = "BOOK_PURCHASE"                   // Individual book purchase/access (targetRef = bookId.toString())
    const val PROMOTIONAL_ACCESS = "PROMOTIONAL_ACCESS"         // Time-bound promotional campaign grant
    const val CONTRIBUTOR_ACCESS = "CONTRIBUTOR_ACCESS"         // Scholarly / translator contributor access grant
    const val INSTITUTION_MEMBERSHIP = "INSTITUTION_MEMBERSHIP" // Institution / university access grant
    const val PUBLISHER_ROLE = "PUBLISHER_ROLE"                 // Publisher-controlled access grant
    const val ADMIN_GRANTED_ACCESS = "ADMIN_GRANTED_ACCESS"     // Direct Superadmin override / custom access grant

    val ALL = listOf(
        FREE_ACCOUNT,
        SUBSCRIPTION_TIER,
        BOOK_PURCHASE,
        PROMOTIONAL_ACCESS,
        CONTRIBUTOR_ACCESS,
        INSTITUTION_MEMBERSHIP,
        PUBLISHER_ROLE,
        ADMIN_GRANTED_ACCESS
    )
}

// =====================================================================
// EXTENSIBLE AI PROVIDER CATALOG
// =====================================================================

@Serializable
data class AiProviderDescriptor(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultBaseUrl: String,
    val availableModels: List<String>,
    val supportsVisionOcr: Boolean,
    val keyPlaceholder: String
)

object AiProviderRegistry {
    val PROVIDERS: List<AiProviderDescriptor> = listOf(
        AiProviderDescriptor(
            id = "GEMINI",
            displayName = "Google Gemini",
            description = "Native multimodal OCR & fast Indian language translation via Google AI Studio keys.",
            defaultBaseUrl = "https://generativelanguage.googleapis.com/",
            availableModels = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite-preview"
            ),
            supportsVisionOcr = true,
            keyPlaceholder = "AIzaSy..."
        ),
        AiProviderDescriptor(
            id = "OPENAI",
            displayName = "OpenAI",
            description = "GPT-4o and GPT-4.1 models for literary translation, OCR, and summarization.",
            defaultBaseUrl = "https://api.openai.com/",
            availableModels = listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4.1",
                "gpt-4.1-mini"
            ),
            supportsVisionOcr = true,
            keyPlaceholder = "sk-proj-..."
        ),
        AiProviderDescriptor(
            id = "ANTHROPIC",
            displayName = "Anthropic Claude",
            description = "Claude Sonnet & Haiku models with nuanced literary and classical translation.",
            defaultBaseUrl = "https://api.anthropic.com/",
            availableModels = listOf(
                "claude-3-7-sonnet-latest",
                "claude-3-5-sonnet-latest",
                "claude-3-5-haiku-latest"
            ),
            supportsVisionOcr = true,
            keyPlaceholder = "sk-ant-..."
        ),
        AiProviderDescriptor(
            id = "OPENROUTER",
            displayName = "OpenRouter (Unified Gateway)",
            description = "Access 200+ models (DeepSeek, Llama, Qwen, Claude, Gemini, Mistral) with one API key.",
            defaultBaseUrl = "https://openrouter.ai/api/",
            availableModels = listOf(
                "google/gemini-2.5-flash",
                "anthropic/claude-3.7-sonnet",
                "openai/gpt-4o-mini",
                "meta-llama/llama-3.3-70b-instruct",
                "qwen/qwen-2.5-72b-instruct"
            ),
            supportsVisionOcr = true,
            keyPlaceholder = "sk-or-v1-..."
        ),
        AiProviderDescriptor(
            id = "CUSTOM_OPENAI",
            displayName = "Custom OpenAI-Compatible API",
            description = "Connect Groq, Together AI, Fireworks, DeepSeek, or your own self-hosted vLLM/Ollama endpoint.",
            defaultBaseUrl = "https://api.groq.com/openai/",
            availableModels = listOf(
                "llama-3.3-70b-versatile",
                "deepseek-r1-distill-llama-70b",
                "custom-model"
            ),
            supportsVisionOcr = false,
            keyPlaceholder = "Bearer token / API key..."
        )
    )

    fun findById(providerId: String): AiProviderDescriptor {
        return PROVIDERS.find { it.id.equals(providerId, ignoreCase = true) } ?: PROVIDERS.first()
    }
}

// =====================================================================
// 1. MULTI-USER AUTHENTICATION, ENTITLEMENTS & CONTRIBUTOR AGREEMENTS
// =====================================================================

@Serializable
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String = UserRole.USER,
    val accountStatus: String = AccountStatus.ACTIVE,
    val preferredLanguage: String = "Gujarati",
    val topThreeLanguagesCsv: String = "Gujarati,Hindi,English",
    val activeTierCode: String = SubscriptionTier.FREE,
    val institutionCode: String = "",
    val hasAcceptedContributorLicense: Boolean = false,
    val contributorLicenseVersion: String = "v1.0-2026",
    val autoPreTranslateEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "user_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["token"], unique = true), Index(value = ["userId"])]
)
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30L * 24 * 3600 * 1000
)

/**
 * Requirement #18 & #19: Granular Entitlement Architecture
 * User -> Entitlements (Reader subscription, Pro Research, Book A, Book B, Institution X)
 */
@Serializable
@Entity(
    tableName = "user_entitlements",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "entitlementKind", "targetRef"])]
)
data class UserEntitlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val entitlementKind: String,  // FREE_ACCOUNT, SUBSCRIPTION_TIER, BOOK_PURCHASE, PROMOTIONAL_ACCESS, CONTRIBUTOR_ACCESS, INSTITUTION_MEMBERSHIP, PUBLISHER_ROLE, ADMIN_GRANTED_ACCESS
    val targetRef: String,        // e.g. "READER", "PRO_RESEARCH", bookId, "INST_BHARAT_VIDYA"
    val displayTitle: String,
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, REVOKED, REFUNDED
    val regionCode: String = "IN-GLOBAL",
    val grantedByAdminId: Long? = null,
    val grantedByAdminName: String = "System",
    val reasonOrNote: String = "",
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 365L * 24 * 3600 * 1000,
    val revokedAt: Long? = null
)

/**
 * Requirement #3: Contributor Agreement / Rights Audit Record
 */
@Serializable
@Entity(
    tableName = "contributor_consents",
    indices = [Index(value = ["userId", "bookId"])]
)
data class ContributorConsentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String,
    val bookRightsCategory: String,
    val resultingTranslationAccessPolicy: String,
    val agreementTextShown: String,
    val licenseVersion: String = "Polyglot-Contrib-License-1.0",
    val acceptedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "user_ai_configs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "providerId"])]
)
data class UserAiConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val providerId: String,
    val providerDisplayName: String,
    val modelName: String,
    val customBaseUrl: String = "",
    val encryptedApiKey: String,
    val maskedKeyPreview: String,
    val isDefaultForOcr: Boolean = true,
    val isDefaultForTranslation: Boolean = true,
    val isDefaultForSummaryAndChat: Boolean = true,
    val lastTestedStatus: String = "Untested",
    val updatedAt: Long = System.currentTimeMillis()
)

// =====================================================================
// 2. BOOKS, ACCESS CONTROL CATALOGUE & VERSIONED OCR CONTENT
// =====================================================================

@Serializable
@Entity(
    tableName = "books",
    indices = [Index(value = ["contentHash"]), Index(value = ["accessPolicy"]), Index(value = ["workId"])]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val authorOrSource: String = "Unknown Author",
    val contributors: String = "",
    val description: String = "",
    val category: String = "Classical Wisdom",
    val genresCsv: String = "Philosophy, Classical Literature",
    val tagsCsv: String = "Sanskrit, Bilingual, Canonical",
    val coverBadgeColorHex: String = "#F59E0B",
    val coverImageUrl: String = "",
    // Work vs. Edition / Source Document Distinction
    val workId: String = "WORK-DEFAULT",
    val editionLabel: String = "1st Digital Critical Edition",
    val isbnOrIdentifier: String = "",
    val publicationYear: String = "2026",
    val publicationInfo: String = "VidyaKosha Digital Archival Press",
    val customMetadataJson: String = "{}",
    val publicationStatus: String = BookPublicationStatus.PUBLISHED, // PUBLISHED, DRAFT, UNPUBLISHED, ARCHIVED
    val searchIndexStatus: String = "INDEXED",                       // INDEXED, PENDING_REINDEX, EXCLUDED
    val sourcePdfPath: String,
    val contentHash: String = "",
    val totalPages: Int,
    val fileSizeBytes: Long,
    val originalLanguage: String = "Sanskrit",
    val preferredTargetLanguage: String = "Gujarati",
    // Requirement #4 & #6: Proper Book Rights & Access Catalogue
    val accessPolicy: String = BookAccessPolicy.PUBLIC, // PUBLIC, FREE, PAID, SPECIFIC_PLAN_ONLY, INDIVIDUAL_PURCHASE, INSTITUTION_ONLY, RESTRICTED, PUBLISHER_ONLY, PRIVATE
    val requiredPlanCode: String = "",                  // Used when accessPolicy == SPECIFIC_PLAN_ONLY
    val rightsCategory: String = BookRightsCategory.PUBLIC_DOMAIN,
    val isPublicCatalog: Boolean = true,
    val isSampleBook: Boolean = false,
    val isPaid: Boolean = false,
    val priceDisplay: String = "Free",
    val publisherName: String = "Polyglot Open Classics",
    val uploadedByUserId: Long? = null,
    val currentOcrVersion: Int = 1,
    val ocrStatus: String = "COMPLETED",
    val ocrProviderUsed: String = "Pre-Ingested Structured OCR",
    val ocrProgressPages: Int = 0,
    val totalChapters: Int = 0,
    val totalSections: Int = 0,
    val totalParagraphs: Int = 0,
    val readCountAnalytics: Int = 1,
    val listenCountAnalytics: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "user_book_access",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "bookId"], unique = true)]
)
data class UserBookAccessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val accessType: String = "FREE_CLAIMED", // OWNER, PURCHASED, FREE_CLAIMED, INSTITUTION_GRANT
    val grantedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "book_pages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId", "pageNumber"], unique = true)]
)
data class BookPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val rawOcrText: String,
    val detectedSourceLanguage: String,
    val ocrConfidenceNote: String = "Normalized Structured OCR",
    val ingestedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId", "chapterOrder"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterOrder: Int,
    val title: String,
    val summary: String = "",
    val startPage: Int,
    val endPage: Int
)

@Serializable
@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId", "sectionOrder"])
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterId: Long,
    val sectionOrder: Int,
    val heading: String,
    val startPage: Int,
    val endPage: Int
)

/**
 * Requirement #8 & #16: Versioned Source Content & Immutable Raw OCR Preservation
 * Never overwrites `rawOcrText`. Tracks `ocrVersion` and `sourceContentHash` so any
 * translation linked to an older hash is automatically flagged for review!
 */
@Serializable
@Entity(
    tableName = "paragraphs",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["chapterId"]),
        Index(value = ["sectionId", "paragraphOrder"]),
        Index(value = ["sourceContentHash"])
    ]
)
data class ParagraphEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterId: Long,
    val sectionId: Long,
    val paragraphOrder: Int,
    val originalPageNumber: Int,
    val blockType: String = "PARAGRAPH",
    val rawOcrText: String = "",         // Immutable OCR v1 output — NEVER overwritten!
    val originalText: String,            // Active (latest corrected/normalized) source text
    val originalLanguage: String,
    val normalizedSearchText: String,
    val ocrVersion: Int = 1,             // Increments when OCR is corrected (v1 -> v2 -> v3)
    val sourceContentHash: String = "",  // Immutable SHA-256 hash of `originalText`
    val lastCorrectedBy: String = "",
    val lastCorrectedAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #16: OCR Correction & Audit Trail Entity
 */
@Serializable
@Entity(
    tableName = "ocr_correction_audits",
    foreignKeys = [
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["paragraphId"]), Index(value = ["bookId"])]
)
data class OcrCorrectionAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val paragraphId: Long,
    val previousOcrVersion: Int,
    val newOcrVersion: Int,
    val rawOcrText: String,
    val previousText: String,
    val correctedText: String,
    val previousContentHash: String,
    val newContentHash: String,
    val correctionAuthorId: Long,
    val correctionAuthorName: String,
    val correctionReason: String = "OCR character & diacritic refinement",
    val correctedAt: Long = System.currentTimeMillis()
)

// =====================================================================
// 3. PERMANENT SHARED TRANSLATION REPOSITORY, MEMORY & TRANSLITERATION
// =====================================================================

/**
 * Requirement #1, #2, #6, #7, #8:
 * Permanent Shared Translation Repository Entity (NEVER deleted, NEVER treated as a temporary cache).
 * Records full provenance, source content version/hash, review lifecycle status, licensing policy,
 * token/cost metadata, and reuse metrics.
 */
@Serializable
@Entity(
    tableName = "paragraph_translations",
    foreignKeys = [
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paragraphId", "targetLanguage", "versionNumber"], unique = true),
        Index(value = ["bookId", "targetLanguage"]),
        Index(value = ["sectionId", "targetLanguage"]),
        Index(value = ["sourceContentHash", "targetLanguage"])
    ]
)
data class ParagraphTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val sectionId: Long,
    val paragraphId: Long,
    val sourceLanguage: String = "Sanskrit",
    val targetLanguage: String,
    val translatedText: String,
    val transliterationOrNotes: String = "",
    val versionNumber: Int = 1,
    val isActiveVersion: Boolean = true, // Marks the preferred version (`isPreferred`) without deleting history
    // Full Provenance Metadata (#1)
    val providerUsed: String = "Polyglot Shared Corpus",
    val modelUsed: String = "Indic Scholar v2",
    val modelVersion: String = "2026.10",
    val promptVersion: String = "polyglot-literary-v2.1",
    // Source Content Versioning (#8)
    val sourceContentVersion: Int = 1,
    val sourceContentHash: String = "",
    // Contributor / Translator Provenance (#1, #3, #6)
    val createdByUserId: Long? = null,
    val translatorDisplayName: String = "Polyglot Editorial & Community",
    // Quality & Review Lifecycle (#7)
    val reviewStatus: String = TranslationReviewStatus.AI_GENERATED,
    val reviewedByDisplayName: String = "",
    // Access & Licensing Policy (#2)
    val accessPolicy: String = TranslationAccessPolicy.PUBLIC,
    val commercialRightsStatus: String = "POLYGLOT_CONTRIBUTOR_LICENSE",
    val isSharedRepositoryAsset: Boolean = true,
    // Token, Cost & Reuse Analytics (#1, #21)
    val estimatedTokens: Int = 140,
    val estimatedCostUsd: Double = 0.0004,
    val reuseCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #9: Reusable Segment-Level Translation Memory
 * Matches normalized source passages across ANY book in the library so identical verses,
 * sutras, headings, or recurring passages are translated once and reused everywhere.
 */
@Serializable
@Entity(
    tableName = "translation_memory",
    indices = [
        Index(value = ["normalizedSourceHash", "sourceLanguage", "targetLanguage"], unique = true),
        Index(value = ["sourceLanguage", "targetLanguage"])
    ]
)
data class TranslationMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedSourceHash: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val canonicalSourceText: String,
    val translatedText: String,
    val notesOrGlossary: String = "",
    val originBookId: Long,
    val originParagraphId: Long,
    val originTranslationId: Long,
    val providerUsed: String,
    val modelUsed: String,
    val reviewStatus: String = TranslationReviewStatus.EXPERT_VERIFIED,
    val accessPolicy: String = TranslationAccessPolicy.PUBLIC,
    val matchReuseCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #7: Translation Issue Reports & Quality Review Queue
 */
@Serializable
@Entity(
    tableName = "translation_issue_reports",
    indices = [Index(value = ["bookId"]), Index(value = ["translationId"])]
)
data class TranslationIssueReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val paragraphId: Long,
    val translationId: Long,
    val targetLanguage: String,
    val reportedByUserId: Long,
    val reporterName: String,
    val issueCategory: String, // "TERMINOLOGY", "SANSKRIT_SANDHI", "GRAMMAR", "OCR_MISMATCH", "CULTURAL_NUANCE"
    val issueDescription: String,
    val suggestedCorrection: String = "",
    val resolutionStatus: String = "OPEN", // "OPEN", "RESOLVED_NEW_VERSION", "APPROVED_AS_IS"
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #14 & #15: Independent Transliteration & Scholarly Learning Layer
 * Stores IAST transliteration, phonetic pronunciation, word-by-word breakdown,
 * literal translation, natural translation, and scholarly commentary independently.
 */
@Serializable
@Entity(
    tableName = "transliteration_learning",
    foreignKeys = [
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["paragraphId"], unique = true), Index(value = ["bookId"])]
)
data class TransliterationLearningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val paragraphId: Long,
    val sourceLanguage: String = "Sanskrit",
    val iastTransliteration: String,          // e.g. "dharmo rakṣati rakṣitaḥ"
    val phoneticPronunciation: String,        // e.g. "dhar-mo rak-sha-ti rak-shi-tah"
    val wordByWordBreakdown: String,          // e.g. "धर्मः (dharmaḥ): cosmic order/duty • रक्षति (rakṣati): protects • रक्षितः (rakṣitaḥ): when protected"
    val literalEnglishTranslation: String,
    val naturalEnglishTranslation: String,
    val scholarlyCommentary: String,
    val updatedAt: Long = System.currentTimeMillis()
)

// =====================================================================
// 4. PERSISTENT PROCESSING JOB QUEUE, OFFLINE PACKAGES & ANALYTICS
// =====================================================================

/**
 * Requirement #10: Persistent Background Processing Job Architecture
 * Supports OCR, TRANSLATION, RETRANSLATION, TTS, SEARCH_INDEX, EMBEDDING
 */
@Serializable
@Entity(
    tableName = "processing_jobs",
    indices = [Index(value = ["status", "createdAt"]), Index(value = ["bookId"])]
)
data class ProcessingJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobType: String,        // OCR, TRANSLATION, RETRANSLATION, TTS, SEARCH_INDEX, EMBEDDING
    val status: String,         // PENDING, PROCESSING, COMPLETED, FAILED, RETRYING
    val bookId: Long,
    val bookTitle: String,
    val sectionId: Long? = null,
    val targetLanguage: String = "",
    val requestedByUserId: Long = 0L,
    val providerAndModel: String = "Shared Repository / Queue",
    val progressPercent: Int = 0,
    val statusDetail: String = "",
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #17: Offline Reading Package for Reader / Pro Users
 */
@Serializable
@Entity(
    tableName = "offline_book_packages",
    indices = [Index(value = ["userId", "bookId"], unique = true)]
)
data class OfflineBookPackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String,
    val includedLanguagesCsv: String,
    val paragraphsCount: Int,
    val translationsCount: Int,
    val bookmarksCount: Int,
    val audioMetadataCached: Boolean = true,
    val packageSizeKb: Int = 420,
    val syncStatus: String = "SYNCED_OFFLINE_READY",
    val downloadedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

/**
 * Requirement #21: Corpus & Translation Reuse Analytics Counter
 */
@Serializable
@Entity(tableName = "corpus_analytics_events")
data class CorpusAnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // "TRANSLATION_REUSED", "TM_EXACT_MATCH_REUSED", "NEW_AI_TRANSLATION_CONTRIBUTED", "BOOK_READ", "TTS_PLAYED"
    val bookId: Long,
    val sourceLanguage: String,
    val targetLanguage: String,
    val tokensSavedOrUsed: Int = 140,
    val userId: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

// =====================================================================
// 5. PER-USER ISOLATED READING PROGRESS, BOOKMARKS, HISTORY & CHAT
// =====================================================================

@Serializable
@Entity(
    tableName = "bookmarks_highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "bookId"]),
        Index(value = ["userId", "paragraphId"])
    ]
)
data class BookmarkHighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String = "",
    val chapterId: Long,
    val sectionId: Long,
    val sectionHeading: String = "",
    val paragraphId: Long,
    val selectedSnippet: String,
    val translatedSnippet: String = "",
    val userNote: String = "",
    val highlightColorHex: String = "#F59E0B",
    val isBookmark: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "bookId"], unique = true)]
)
data class ReadingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String = "",
    val currentChapterId: Long,
    val currentChapterTitle: String = "",
    val currentSectionId: Long,
    val currentSectionHeading: String = "",
    val currentParagraphId: Long,
    val preferredLanguage: String = "Gujarati",
    val completedSectionsCount: Int = 1,
    val totalSectionsCount: Int = 1,
    val progressPercent: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "reading_history",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "timestamp"])]
)
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val bookTitle: String,
    val chapterTitle: String,
    val sectionId: Long,
    val sectionHeading: String,
    val languageRead: String,
    val actionSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Requirement #12: "Ask This Book" Grounded Research Message with Citations & Open Passage targets
 */
@Serializable
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "bookId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val chapterId: Long? = null,
    val sectionId: Long? = null,
    val citedParagraphId: Long? = null,
    val citationLabel: String = "", // e.g., "Chapter 1, Section 2, Paragraph 1 (Page 1)"
    val role: String,
    val content: String,
    val targetLanguage: String,
    val providerAndModelUsed: String = "",
    val referencedPage: Int? = null,
    val actionType: String = "ASK_THIS_BOOK",
    val timestamp: Long = System.currentTimeMillis()
)

// =====================================================================
// COMPOSITE DOMAIN MODELS FOR READER, DISCOVERY, SEARCH & ANALYTICS
// =====================================================================

data class BilingualParagraphItem(
    val paragraph: ParagraphEntity,
    val activeTranslation: ParagraphTranslationEntity?,
    val allVersions: List<ParagraphTranslationEntity> = emptyList(),
    val allVersionsCount: Int = 0,
    val requiresSourceReview: Boolean = false, // True when paragraph.sourceContentHash != activeTranslation.sourceContentHash
    val transliterationLearning: TransliterationLearningEntity? = null,
    val isBookmarked: Boolean = false,
    val highlightSnippet: String? = null
)

data class ContinuousSectionBlock(
    val chapter: ChapterEntity,
    val isFirstSectionOfChapter: Boolean,
    val section: SectionEntity,
    val globalSectionIndex: Int,
    val totalBookSections: Int,
    val paragraphs: List<BilingualParagraphItem>,
    val isFullyTranslated: Boolean
)

/**
 * Requirement #5: Translation Discovery Summary for the selected language
 */
data class TranslationDiscoveryStatus(
    val targetLanguage: LanguageDescriptor,
    val totalParagraphsInBook: Int,
    val translatedParagraphsCount: Int,
    val coveragePercent: Int,
    val isAvailable: Boolean,
    val primaryProvider: String,
    val primaryModel: String,
    val latestVersionNumber: Int,
    val latestReviewStatus: String,
    val accessPolicy: String,
    val lastGeneratedTimestamp: Long,
    val staleSourceCount: Int
)

/**
 * Requirement #13: Cross-Book Search Result Item
 */
data class CrossBookSearchResultItem(
    val book: BookEntity,
    val chapter: ChapterEntity,
    val section: SectionEntity,
    val paragraph: ParagraphEntity,
    val matchedTranslation: ParagraphTranslationEntity?,
    val matchedTransliteration: TransliterationLearningEntity?,
    val matchSourceType: String // "ORIGINAL_TEXT", "TRANSLATION", "TRANSLITERATION", "METADATA"
)

/**
 * Requirement #21: Corpus Analytics Summary
 */
data class CorpusAnalyticsSummary(
    val totalBooksInCatalog: Int = 0,
    val totalParagraphsStored: Int = 0,
    val totalCanonicalTranslations: Int = 0,
    val totalTranslationMemorySegments: Int = 0,
    val aiCallsAvoidedByReuse: Int = 0,
    val estimatedTokensSaved: Int = 0,
    val expertVerifiedTranslationsCount: Int = 0,
    val openQualityReportsCount: Int = 0,
    val activeOrCompletedJobsCount: Int = 0,
    val languageCoverageBreakdown: Map<String, Int> = emptyMap()
)

// =====================================================================
// 6. SUPERADMIN BACKEND ENTITIES: COMMERCIAL PLANS, METADATA & AUDIT LOGS
// =====================================================================

/**
 * Database-backed Commercial Plan Configuration (No hard-coded prices or permissions!).
 */
@Serializable
@Entity(
    tableName = "commercial_plans",
    indices = [Index(value = ["planCode"], unique = true), Index(value = ["displayOrder"])]
)
data class CommercialPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planCode: String,                        // e.g. FREE, READER, PRO_RESEARCH, PUBLISHER_INSTITUTION, or custom
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val monthlyPriceAmount: Double = 0.0,
    val yearlyPriceAmount: Double = 0.0,
    val currencyCode: String = "USD",            // USD, INR, EUR, GBP
    val trialDays: Int = 0,
    val displayOrder: Int = 1,
    val isFeatured: Boolean = false,
    val featuresCsv: String = "",
    val allowedBookAccessPoliciesCsv: String = "PUBLIC,FREE",
    val dailyTranslationLimit: Int = 25,         // -1 = unlimited
    val monthlyManagedAiTokensLimit: Int = 0,    // 0 = BYO AI only; >0 = managed AI token allowance
    val offlineAccessEnabled: Boolean = false,
    val advancedSearchAndScholarlyEnabled: Boolean = false,
    val ttsAudioDailyMinutesLimit: Int = 30,     // -1 = unlimited
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun formatPriceSummary(): String {
        if (monthlyPriceAmount <= 0.0 && yearlyPriceAmount <= 0.0) {
            return "Free ($currencyCode 0)"
        }
        val symbol = when (currencyCode.uppercase()) {
            "INR" -> "₹"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$"
        }
        val monthlyText = if (monthlyPriceAmount == monthlyPriceAmount.toLong().toDouble()) {
            "$symbol${monthlyPriceAmount.toLong()}/mo"
        } else {
            "$symbol$monthlyPriceAmount/mo"
        }
        val yearlyText = if (yearlyPriceAmount > 0.0) {
            val yr = if (yearlyPriceAmount == yearlyPriceAmount.toLong().toDouble()) {
                "$symbol${yearlyPriceAmount.toLong()}/yr"
            } else {
                "$symbol$yearlyPriceAmount/yr"
            }
            " ($yr)"
        } else ""
        return "$monthlyText$yearlyText"
    }
}

object MetadataCategoryType {
    const val LANGUAGE = "LANGUAGE"
    const val CATEGORY = "CATEGORY"
    const val GENRE = "GENRE"
    const val TAG = "TAG"
    const val BOOK_STATUS = "BOOK_STATUS"
    const val TRANSLATION_STATUS = "TRANSLATION_STATUS"
    const val REVIEW_STATUS = "REVIEW_STATUS"
    const val ACCESS_POLICY = "ACCESS_POLICY"
    const val PLAN_FEATURE = "PLAN_FEATURE"

    val ALL = listOf(
        LANGUAGE,
        CATEGORY,
        GENRE,
        TAG,
        BOOK_STATUS,
        TRANSLATION_STATUS,
        REVIEW_STATUS,
        ACCESS_POLICY,
        PLAN_FEATURE
    )
}

/**
 * Reusable Platform Metadata Registry (Supports adding new enumerations without DB restructuring).
 */
@Serializable
@Entity(
    tableName = "platform_metadata",
    indices = [Index(value = ["metadataType", "keyCode"], unique = true)]
)
data class PlatformMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metadataType: String,   // LANGUAGE, CATEGORY, GENRE, TAG, BOOK_STATUS, TRANSLATION_STATUS, REVIEW_STATUS, ACCESS_POLICY, PLAN_FEATURE
    val keyCode: String,        // Canonical identifier
    val displayLabel: String,   // Human-readable label
    val description: String = "",
    val sortOrder: Int = 1,
    val isActive: Boolean = true,
    val extraAttributesJson: String = "{}",
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable Administrative Audit Trail (`admin_audit_logs`).
 * Records every administrative action with previous/new values, actor, timestamp, and reason.
 */
@Serializable
@Entity(
    tableName = "admin_audit_logs",
    indices = [Index(value = ["timestamp"]), Index(value = ["entityAffected", "entityId"]), Index(value = ["adminUserId"])]
)
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adminUserId: Long,
    val adminEmailOrName: String,
    val adminRole: String,
    val action: String,
    val entityAffected: String,
    val entityId: String,
    val previousValue: String = "",
    val newValue: String = "",
    val reasonOrNote: String = "",
    val deviceOrContextInfo: String = "Android Client • Verified Session",
    val timestamp: Long = System.currentTimeMillis()
)

data class SuperadminDashboardOverview(
    val totalUsers: Int = 0,
    val activeSubscriptionsCount: Int = 0,
    val freeUsersCount: Int = 0,
    val paidUsersCount: Int = 0,
    val suspendedUsersCount: Int = 0,
    val totalBooksCount: Int = 0,
    val publishedBooksCount: Int = 0,
    val draftOrUnpublishedBooksCount: Int = 0,
    val archivedBooksCount: Int = 0,
    val booksByOriginalLanguage: Map<String, Int> = emptyMap(),
    val totalTranslationsCount: Int = 0,
    val translationsByLanguage: Map<String, Int> = emptyMap(),
    val recentRegistrations: List<UserEntity> = emptyList(),
    val recentTranslations: List<ParagraphTranslationEntity> = emptyList(),
    val recentAuditLogs: List<AdminAuditLogEntity> = emptyList()
)
