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
        assertEquals("1.2.0", AppVersionManager.CURRENT_VERSION_NAME)
        assertEquals(3, AppVersionManager.CURRENT_VERSION_CODE)
        assertTrue(AppVersionManager.VERSION_HISTORY.isNotEmpty())
        assertEquals("1.2.0", AppVersionManager.VERSION_HISTORY.first().versionName)
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
}
