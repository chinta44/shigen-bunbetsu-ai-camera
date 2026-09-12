package com.example.data.barcode

import android.graphics.Bitmap
import android.graphics.Matrix
import com.example.data.model.CollectionSchedule
import com.example.data.model.Municipality
import com.example.data.model.NextDateInfo
import com.example.data.model.SortingResult
import com.example.data.repository.MunicipalityData
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BarcodeScannerHelper {

    /**
     * Attempts to decode a barcode (EAN-13, EAN-8, UPC, QR code, Code 128, etc.) from a Bitmap.
     * Tries 0°, 90°, 180°, 270° orientations if needed.
     */
    suspend fun decodeBarcodeFromBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        com.google.zxing.BarcodeFormat.EAN_13,
                        com.google.zxing.BarcodeFormat.EAN_8,
                        com.google.zxing.BarcodeFormat.UPC_A,
                        com.google.zxing.BarcodeFormat.UPC_E,
                        com.google.zxing.BarcodeFormat.CODE_128,
                        com.google.zxing.BarcodeFormat.CODE_39,
                        com.google.zxing.BarcodeFormat.QR_CODE
                    )
                )
            )
        }

        // Try natural orientation
        var result = decodeSingleBitmap(reader, bitmap)
        if (result != null) return@withContext result

        // Try 90 degree rotation
        val rotated90 = rotateBitmap(bitmap, 90f)
        result = decodeSingleBitmap(reader, rotated90)
        if (result != null) return@withContext result

        // Try 270 degree rotation
        val rotated270 = rotateBitmap(bitmap, 270f)
        result = decodeSingleBitmap(reader, rotated270)
        return@withContext result
    }

    private fun decodeSingleBitmap(reader: MultiFormatReader, bmp: Bitmap): String? {
        return try {
            val width = bmp.width
            val height = bmp.height
            val pixels = IntArray(width * height)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val rawResult = reader.decodeWithState(binaryBitmap)
            rawResult.text
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Evaluates a recognized barcode product against the current municipality's sorting regulations
     */
    fun resolveProductForMunicipality(
        product: BarcodeProduct,
        municipality: Municipality
    ): BarcodeAnalysisResult {
        val partResolutions = product.parts.map { part ->
            resolvePartForMunicipality(part, municipality)
        }

        // Select the primary part (usually the body of the container or highest volume)
        val primaryResolution = partResolutions.firstOrNull() ?: PartDisposalResolution(
            part = ProductPackagingPart(product.productName, "複合", "burnable", product.generalAdvice),
            targetCategoryName = "可燃ごみ",
            targetColorHex = 0xFFFF7043,
            nextSchedule = municipality.schedules.firstOrNull()?.getNextCollectionDate(),
            instruction = product.generalAdvice
        )

        val nextDateStr = primaryResolution.nextSchedule?.let {
            "${it.dateText} ${it.dayOfWeekText}"
        } ?: "自治体カレンダー参照"

        val daysRemStr = primaryResolution.nextSchedule?.daysRemainingText ?: ""

        val detailedAdvice = buildString {
            appendLine("【バーコード判定】${product.productName}（${product.brandName}）")
            appendLine()
            appendLine("■ 部位別の分別ルール（${municipality.name}）:")
            partResolutions.forEach { res ->
                val schedText = res.nextSchedule?.let { " (${it.daysRemainingText} ${it.dateText})" } ?: ""
                appendLine("・${res.part.partName}: 【${res.targetCategoryName}】$schedText")
                appendLine("  ${res.instruction}")
            }
            appendLine()
            appendLine("■ 注意事項: ${product.generalAdvice}")
        }

        val primarySortingResult = SortingResult(
            itemName = "${product.productName} [バーコード: ${product.barcode}]",
            categoryName = primaryResolution.targetCategoryName,
            categoryId = primaryResolution.part.categoryKey,
            colorHex = primaryResolution.targetColorHex,
            municipalityName = municipality.name,
            nextDateText = nextDateStr,
            daysRemainingText = daysRemStr,
            disposalAdvice = detailedAdvice,
            sizeMaterialNotes = "部位ごとに材質が異なります（${product.parts.joinToString(" / ") { "${it.partName}: ${it.material}" }}）。",
            requiresReservation = false,
            isConfidenceHigh = true
        )

        return BarcodeAnalysisResult(
            product = product,
            municipalityName = municipality.name,
            partDisposalList = partResolutions,
            primarySortingResult = primarySortingResult
        )
    }

    private fun resolvePartForMunicipality(
        part: ProductPackagingPart,
        municipality: Municipality
    ): PartDisposalResolution {
        val targetCatId = when (part.categoryKey) {
            "pet" -> {
                if (municipality.categories.any { it.id == "resources" }) "resources"
                else if (municipality.categories.any { it.id == "plastic" }) "plastic"
                else municipality.categories.first().id
            }
            "plastic_container" -> {
                if (municipality.categories.any { it.id == "plastic" }) "plastic"
                else if (municipality.categories.any { it.id == "burnable" }) "burnable"
                else municipality.categories.first().id
            }
            "can" -> {
                if (municipality.categories.any { it.id == "resources" }) "resources"
                else if (municipality.categories.any { it.id == "metal" }) "metal"
                else "non_burnable"
            }
            "paper" -> {
                if (municipality.categories.any { it.id == "paper" }) "paper"
                else if (municipality.categories.any { it.id == "resources" }) "resources"
                else "burnable"
            }
            "hazardous" -> "hazardous"
            "non_burnable" -> "non_burnable"
            else -> "burnable"
        }

        val cat = municipality.categories.firstOrNull { it.id == targetCatId }
            ?: municipality.categories.firstOrNull()
            ?: MunicipalityData.CAT_BURNABLE

        val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
        val nextInfo = sched?.getNextCollectionDate()

        val fullInstruction = "${part.disposalAction} (${cat.containerType})"

        return PartDisposalResolution(
            part = part,
            targetCategoryName = cat.name,
            targetColorHex = cat.colorHex,
            nextSchedule = nextInfo,
            instruction = fullInstruction
        )
    }
}
