package com.example.domain.parser

import com.example.model.ActionType
import com.example.model.RepeatFrequency
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockCommandParserTest {

    private lateinit var parser: MockCommandParser

    @Before
    fun setUp() {
        parser = MockCommandParser()
    }

    @Test
    fun parse_reminderTomorrow_returnsCreateReminder() = runTest {
        val input = "بكرة الساعة 8 ذكرني أذاكر الرياضيات"
        val result = parser.parse(input)

        assertEquals(ActionType.CREATE_REMINDER, result.intent)
        assertTrue(result.title.contains("مذاكرة") || result.title.contains("أذاكر"))
        assertEquals("غداً", result.scheduleRule.targetDateArabic)
        assertEquals(RepeatFrequency.NONE, result.scheduleRule.frequency)
    }

    @Test
    fun parse_grammarReminder_returnsCreateReminder() = runTest {
        val input = "غدًا الساعة 9 ذكرني بمراجعة درس النحو"
        val result = parser.parse(input)

        assertEquals(ActionType.CREATE_REMINDER, result.intent)
        assertTrue(result.title.contains("النحو") || result.title.contains("مراجعة"))
        assertEquals("غداً", result.scheduleRule.targetDateArabic)
    }

    @Test
    fun parse_dailyQuran_returnsOpenQuranDaily() = runTest {
        val input = "كل يوم الساعة 10 افتح تطبيق القرآن"
        val result = parser.parse(input)

        assertEquals(ActionType.OPEN_QURAN, result.intent)
        assertEquals(RepeatFrequency.DAILY, result.scheduleRule.frequency)
        assertNotNull(result.action)
    }

    @Test
    fun parse_openFacebook_returnsOpenApp() = runTest {
        val input = "الساعة 8 مساءً افتح Facebook"
        val result = parser.parse(input)

        assertEquals(ActionType.OPEN_APP, result.intent)
        assertEquals("Facebook", result.action?.target)
    }

    @Test
    fun parse_openQuranSurah_returnsOpenQuranWithSurah() = runTest {
        val input = "الساعة 10 افتح القرآن على سورة آل عمران آية 15"
        val result = parser.parse(input)

        assertEquals(ActionType.OPEN_QURAN, result.intent)
        assertTrue(result.action?.target?.contains("آل عمران") == true)
    }

    @Test
    fun parse_prayerReminder_returnsPrayerReference() = runTest {
        val input = "بعد صلاة الفجر ذكرني بقراءة صفحتين من القرآن"
        val result = parser.parse(input)

        assertEquals(ActionType.CREATE_REMINDER, result.intent)
        assertEquals("صلاة الفجر", result.scheduleRule.prayerReference)
    }

    @Test
    fun parse_replanDay_returnsReplanIntent() = runTest {
        val input = "أعد ترتيب باقي اليوم لأنني تأخرت ساعتين"
        val result = parser.parse(input)

        assertEquals(ActionType.REPLAN_DAY, result.intent)
        assertEquals("2", result.action?.payload?.get("delay_hours"))
    }

    @Test
    fun parse_focusSession_returnsFocusIntent() = runTest {
        val input = "أريد جلسة تركيز لمدة 50 دقيقة"
        val result = parser.parse(input)

        assertEquals(ActionType.START_FOCUS_SESSION, result.intent)
        assertEquals("50", result.action?.payload?.get("duration_minutes"))
    }

    @Test
    fun parse_youtube_returnsSearchYoutube() = runTest {
        val input = "ابحث في يوتيوب عن تفسير سورة الكهف"
        val result = parser.parse(input)

        assertEquals(ActionType.SEARCH_YOUTUBE, result.intent)
        assertTrue(result.action?.target?.contains("الكهف") == true)
    }
}
