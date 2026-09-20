package com.example.data.barcode

import com.example.data.model.NextDateInfo
import com.example.data.model.SortingResult

/**
 * Packaging component of a scanned product
 */
data class ProductPackagingPart(
    val partName: String,               // e.g. "ボトル本体", "キャップ", "外装フィルム/ラベル", "外箱"
    val material: String,               // e.g. "PET (ポリエチレンテレフタレート)", "プラ (PP)", "紙", "アルミ"
    val categoryKey: String,            // "pet", "plastic_container", "burnable", "non_burnable", "paper", "can", "glass"
    val disposalAction: String,         // e.g. "水洗いしてラベル・キャップを外す", "プラマーク分別へ"
    val isMarkedPlamark: Boolean = false
)

/**
 * Recognized product from barcode scan
 */
data class BarcodeProduct(
    val barcode: String,
    val productName: String,
    val brandName: String,
    val categoryHint: String,           // e.g. "飲料ペットボトル", "アルミ缶", "レトルトパウチ", "洗剤ボトル"
    val parts: List<ProductPackagingPart>,
    val generalAdvice: String,
    val imageUrl: String? = null
)

/**
 * Result of scanning and resolving a barcode for a specific municipality
 */
data class BarcodeAnalysisResult(
    val product: BarcodeProduct,
    val municipalityName: String,
    val partDisposalList: List<PartDisposalResolution>,
    val primarySortingResult: SortingResult
)

data class PartDisposalResolution(
    val part: ProductPackagingPart,
    val targetCategoryName: String,
    val targetColorHex: Long,
    val nextSchedule: NextDateInfo?,
    val instruction: String
)
