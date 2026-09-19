package com.example

import com.example.data.ai.WasteClassifierEngine
import com.example.data.model.AppVersionManager
import com.example.data.repository.MunicipalityData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVersionBumping() {
        assertEquals("1.3.8", AppVersionManager.CURRENT_VERSION_NAME)
        assertEquals(10, AppVersionManager.CURRENT_VERSION_CODE)
        assertTrue(AppVersionManager.VERSION_HISTORY.isNotEmpty())
        assertEquals("1.3.8", AppVersionManager.VERSION_HISTORY.first().versionName)
    }

    @Test
    fun testMunicipalityPdfUrlAvailable() {
        val aisai = MunicipalityData.AISAI
        assertNotNull("AISAI should have official guide PDF URL", aisai.officialGuidePdfUrl)
        assertTrue("URL should be HTTPS", aisai.officialGuidePdfUrl!!.startsWith("https://"))
        assertTrue("Aisai PDF should point to direct PDF", aisai.officialGuidePdfUrl!!.endsWith(".pdf"))
        assertNotNull("AISAI should have official guide title", aisai.officialGuidePdfTitle)

        val tsushima = MunicipalityData.TSUSHIMA
        assertNotNull("TSUSHIMA should have official guide PDF URL", tsushima.officialGuidePdfUrl)
        assertTrue("TSUSHIMA PDF should point to direct PDF", tsushima.officialGuidePdfUrl!!.endsWith(".pdf"))

        val yatomi = MunicipalityData.YATOMI
        assertNotNull("YATOMI should have official guide PDF URL", yatomi.officialGuidePdfUrl)
        assertTrue("YATOMI PDF should point to direct PDF", yatomi.officialGuidePdfUrl!!.endsWith(".pdf"))

        val ichinomiya = MunicipalityData.ICHINOMIYA
        assertNotNull("ICHINOMIYA should have official guide PDF URL", ichinomiya.officialGuidePdfUrl)
        assertTrue("ICHINOMIYA PDF should point to direct PDF", ichinomiya.officialGuidePdfUrl!!.endsWith(".pdf"))
    }

    @Test
    fun testMedicineBottleClassification() = runBlocking {
        val engine = WasteClassifierEngine()
        val municipality = MunicipalityData.AISAI
        val output = engine.analyzeImage(
            bitmap = null,
            municipality = municipality,
            fallbackItemKeyword = "虫さされ・かゆみ止め液ボトル"
        )

        assertTrue("Expected Resolved output for medicine bottle", output is WasteClassifierEngine.AnalysisOutput.Resolved)
        val result = (output as WasteClassifierEngine.AnalysisOutput.Resolved).result
        assertEquals("plastic", result.categoryId)
        assertTrue("Category should contain プラスチック: ${result.categoryName}", result.categoryName.contains("プラスチック"))
        assertTrue("Parts breakdown should not be empty", result.partsBreakdown.isNotEmpty())
    }

    @Test
    fun testLeatherWalletClassification() = runBlocking {
        val engine = WasteClassifierEngine()
        val municipality = MunicipalityData.NAGOYA
        val output = engine.analyzeImage(
            bitmap = null,
            municipality = municipality,
            fallbackItemKeyword = "革製の長財布"
        )

        assertTrue("Expected Resolved output for leather wallet", output is WasteClassifierEngine.AnalysisOutput.Resolved)
        val result = (output as WasteClassifierEngine.AnalysisOutput.Resolved).result
        assertEquals("burnable", result.categoryId)
        assertTrue("Category should contain 可燃: ${result.categoryName}", result.categoryName.contains("可燃"))
        assertTrue("Item name should contain 財布: ${result.itemName}", result.itemName.contains("財布"))
    }

    @Test
    fun testManualCorrection() {
        val engine = WasteClassifierEngine()
        val municipality = MunicipalityData.NAGOYA
        val correctedResult = engine.createCustomSortingResult(
            itemName = "革製の長財布",
            categoryId = "burnable",
            municipality = municipality
        )

        assertEquals("革製の長財布", correctedResult.itemName)
        assertEquals("burnable", correctedResult.categoryId)
        assertTrue(correctedResult.categoryName.contains("可燃"))
    }

    @Test
    fun testVersionComparison() {
        val manager = com.example.data.update.AppUpdateManager
        // Remote is newer
        assertTrue(manager.isVersionNewer("1.3.7", "1.3.6"))
        assertTrue(manager.isVersionNewer("1.4.0", "1.3.6"))
        assertTrue(manager.isVersionNewer("2.0.0", "1.3.6"))
        assertTrue(manager.isVersionNewer("v1.3.7", "v1.3.6"))

        // Remote is same or older
        assertFalse(manager.isVersionNewer("1.3.6", "1.3.6"))
        assertFalse(manager.isVersionNewer("1.3.5", "1.3.6"))
        assertFalse(manager.isVersionNewer("1.2.0", "1.3.6"))
    }
}
