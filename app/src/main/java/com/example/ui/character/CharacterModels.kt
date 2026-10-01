package com.example.ui.character

import androidx.compose.ui.graphics.Color

/**
 * حالات الشخصية التفاعلية (Character States)
 */
enum class CharacterState {
    IDLE,       // هادئ ومستعد في الحالة الطبيعية
    LISTENING,  // يستمع لصوت أو إدخال المستخدم مع هالة نبضية
    THINKING,   // يفكر ويعالج الأمر المدخل
    SPEAKING,   // يتحدث أو يرد على المستخدم
    HAPPY,      // سعيد ومتفاعل
    SUCCESS,    // نجاح فهم الأمر وتأكيده
    ERROR       // تعبير هادئ عند عدم وضوح الأمر
}

/**
 * أنواع الشخصيات المستقبلية القابلة للتوسع عبر متجر الثيمات
 */
enum class CharacterSpecies(val titleArabic: String) {
    DEFAULT_RAFIQ("رفيق الأصلي"),
    BEAR("الدب"),
    CAT("القط"),
    RABBIT("الأرنب"),
    FOX("الثعلب"),
    CHILD("الطفل"),
    CARTOON_BOY("الفتى الكرتوني"),
    CARTOON_GIRL("الفتاة الكرتونية"),
    ROBOT("الآلي الذكي")
}

/**
 * بنية الثيمات المستقبلية للشخصيات (Future Character Theme Architecture)
 * NOTE: Theme Store will be implemented in a future phase.
 * No store backend, downloads, payments, or accounts are implemented in this phase.
 */
data class CharacterTheme(
    val id: String,
    val name: String,
    val species: CharacterSpecies,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val backgroundStyle: String,
    val voiceStyle: String,
    val isAvailable: Boolean = true
) {
    companion object {
        // الثيم الافتراضي الحالي للمرحلة التجريبية
        val DEFAULT = CharacterTheme(
            id = "theme_default_rafiq",
            name = "رفيق الافتراضي",
            species = CharacterSpecies.DEFAULT_RAFIQ,
            primaryColorHex = "#6B21A8",
            secondaryColorHex = "#C084FC",
            backgroundStyle = "lavender_mist",
            voiceStyle = "calm_friendly",
            isAvailable = true
        )
    }
}
