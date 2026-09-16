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
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BarcodeScannerHelper {

    /**
     * Highly resilient barcode decoder supporting EAN-13, EAN-8, UPC, Code 128, etc.
     * Evaluates multiple binarization algorithms (Hybrid & GlobalHistogram),
     * multiple crop regions (full, center box, horizontal band), inverted luminances, and rotations.
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
                        com.google.zxing.BarcodeFormat.ITF,
                        com.google.zxing.BarcodeFormat.QR_CODE
                    )
                )
            )
        }

        // 1. Prepare normalized bitmap (prevent memory exhaustion on 48MP photos, while maintaining sharpness)
        val normalized = prepareNormalizedBitmap(bitmap)

        // 2. Generate candidate crop regions (Center crop and horizontal strip often isolate the barcode from clutter)
        val candidates = mutableListOf<Bitmap>()
        candidates.add(normalized)

        createCenterCrop(normalized, 0.75f, 0.60f)?.let { candidates.add(it) }
        createCenterCrop(normalized, 0.90f, 0.35f)?.let { candidates.add(it) } // Barcode-like wide strip

        for (candidate in candidates) {
            // Natural orientation
            var code = decodeBitmapVariations(reader, candidate)
            if (code != null) return@withContext code

            // 90 degrees rotation
            val rot90 = rotateBitmap(candidate, 90f)
            code = decodeBitmapVariations(reader, rot90)
            if (code != null) return@withContext code

            // 270 degrees rotation
            val rot270 = rotateBitmap(candidate, 270f)
            code = decodeBitmapVariations(reader, rot270)
            if (code != null) return@withContext code

            // 180 degrees (upside down)
            val rot180 = rotateBitmap(candidate, 180f)
            code = decodeBitmapVariations(reader, rot180)
            if (code != null) return@withContext code
        }

        return@withContext null
    }

    private fun decodeBitmapVariations(reader: MultiFormatReader, bmp: Bitmap): String? {
        return try {
            val width = bmp.width
            val height = bmp.height
            val pixels = IntArray(width * height)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)

            // Attempt 1: HybridBinarizer
            try {
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val rawResult = reader.decodeWithState(binaryBitmap)
                if (!rawResult.text.isNullOrBlank()) return rawResult.text
            } catch (_: Exception) {} finally { reader.reset() }

            // Attempt 2: GlobalHistogramBinarizer (superior on shadow-heavy/uneven 1D barcodes)
            try {
                val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                val rawResult = reader.decodeWithState(binaryBitmap)
                if (!rawResult.text.isNullOrBlank()) return rawResult.text
            } catch (_: Exception) {} finally { reader.reset() }

            // Attempt 3: Inverted luminance (white-on-black barcodes)
            try {
                val inverted = source.invert()
                val binaryBitmap = BinaryBitmap(HybridBinarizer(inverted))
                val rawResult = reader.decodeWithState(binaryBitmap)
                if (!rawResult.text.isNullOrBlank()) return rawResult.text
            } catch (_: Exception) {} finally { reader.reset() }

            null
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun prepareNormalizedBitmap(bmp: Bitmap): Bitmap {
        val maxDim = maxOf(bmp.width, bmp.height)
        if (maxDim > 1600) {
            val scale = 1600f / maxDim
            val w = (bmp.width * scale).toInt()
            val h = (bmp.height * scale).toInt()
            return Bitmap.createScaledBitmap(bmp, w, h, true)
        }
        if (maxDim < 400 && maxDim > 0) {
            val scale = 2f
            val w = (bmp.width * scale).toInt()
            val h = (bmp.height * scale).toInt()
            return Bitmap.createScaledBitmap(bmp, w, h, true)
        }
        return bmp
    }

    private fun createCenterCrop(source: Bitmap, widthRatio: Float, heightRatio: Float): Bitmap? {
        return try {
            val cropW = (source.width * widthRatio).toInt().coerceIn(10, source.width)
            val cropH = (source.height * heightRatio).toInt().coerceIn(10, source.height)
            val startX = ((source.width - cropW) / 2).coerceAtLeast(0)
            val startY = ((source.height - cropH) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(source, startX, startY, cropW, cropH)
        } catch (e: Exception) {
            null
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
