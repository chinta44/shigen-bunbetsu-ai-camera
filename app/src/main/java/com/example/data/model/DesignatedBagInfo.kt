package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

data class DesignatedBagInfo(
    val categoryId: String,
    val title: String,
    val bagTypeDescription: String,
    val appearanceNote: String,
    val purchaseGuide: String,
    val disposalTip: String,
    @DrawableRes val drawableRes: Int
)

object DesignatedBagProvider {

    fun getBagInfo(categoryId: String, municipalityName: String): DesignatedBagInfo? {
        val isAisai = municipalityName.contains("愛西")

        return when (categoryId.lowercase()) {
            "burnable" -> DesignatedBagInfo(
                categoryId = "burnable",
                title = if (isAisai) "愛西市 指定 可燃物ごみ専用袋" else "$municipalityName 指定 可燃ごみ専用袋",
                bagTypeDescription = "黄色半透明の有料指定袋（大・中・小・特小）",
                appearanceNote = if (isAisai) "愛西市の可燃ごみは『黄色半透明（赤文字印字）』の指定袋を使用します。" else "自治体指定の可燃ごみ専用袋をお使いください。",
                purchaseGuide = "地域のスーパー、コンビニエンスストア、ドラッグストア等で取扱中",
                disposalTip = "生ごみは十分に水切りをし、袋の口をしっかり縛って収集日の朝8時までに出してください。段ボール箱等に入れて出すことはできません。",
                drawableRes = R.drawable.img_bag_burnable
            )

            "plastic" -> DesignatedBagInfo(
                categoryId = "plastic",
                title = if (isAisai) "愛西市 指定 プラスチック類ごみ専用袋" else "$municipalityName 指定 プラマーク・プラスチック専用袋",
                bagTypeDescription = "透明・青色印字の有料指定袋",
                appearanceNote = if (isAisai) "愛西市では『プラスチック類ごみ専用袋』を使用します。お菓子袋、レジ袋、洗剤ボトル、トレイ等が対象です。" else "プラマークが付いた容器包装プラスチック用の指定袋です。",
                purchaseGuide = "地域のスーパー、コンビニエンスストア等で購入できます",
                disposalTip = "中身を必ず空にし、汚れは軽く水ですすいで乾かしてから入れてください。※白色発泡スチロールトレイは資源ごみへ。",
                drawableRes = R.drawable.img_bag_plastic
            )

            "non_burnable" -> DesignatedBagInfo(
                categoryId = "non_burnable",
                title = if (isAisai) "愛西市 指定 不燃物ごみ専用袋" else "$municipalityName 指定 不燃ごみ専用袋",
                bagTypeDescription = "透明（緑・青文字印字）の有料指定袋",
                appearanceNote = "陶磁器類、ガラスくず、小型の金属類など、燃えないごみ専用の指定袋です。",
                purchaseGuide = "地域の指定ごみ袋取扱店（コンビニ・スーパー等）",
                disposalTip = "割れたガラスや刃物、針などは厚紙や新聞紙に包み、袋の外側に『キケン』と表示してください。",
                drawableRes = R.drawable.img_bag_nonburnable
            )

            "oversized" -> DesignatedBagInfo(
                categoryId = "oversized",
                title = if (isAisai) "愛西市 粗大ごみ処理シール（納付券）" else "$municipalityName 粗大ごみ処理シール・納付券",
                bagTypeDescription = if (isAisai) "集積場所用(200円) / 戸別回収用(500円)" else "粗大ごみ処理手数料納付券",
                appearanceNote = if (isAisai) "袋には入れず、購入したシールに受付番号・氏名を記入して品物の目立つ位置に貼り付けます。" else "予約受付後にコンビニ等で納付券シールを購入し貼り付けます。",
                purchaseGuide = if (isAisai) "愛西市役所、支所、市内コンビニ、取扱店" else "各自治体の指定納付券取扱店（コンビニ等）",
                disposalTip = if (isAisai) "集積場所は第1水曜(200円券)、戸別回収は事前電話予約(0567-22-2480)の上で第3水曜(500円券)に出してください。" else "事前に粗大ごみ受付センターへ予約の上、指定日時に貼り付けて出してください。",
                drawableRes = R.drawable.img_tag_oversized
            )

            "bottle_can" -> DesignatedBagInfo(
                categoryId = "bottle_can",
                title = "$municipalityName 資源ごみ（ビン・缶・ペットボトル）",
                bagTypeDescription = "透明袋 または 集積所の専用回収コンテナ",
                appearanceNote = "中身がはっきり確認できる透明のポリ袋、または集積所に設置される黄色・青色の折りたたみコンテナに入れます。",
                purchaseGuide = "市販の透明ポリ袋、または集積所の回収カゴ",
                disposalTip = "キャップとラベルを外して軽く水洗いし、缶はつぶさずにそのまま出してください（ペットボトルはつぶす自治体もあります）。",
                drawableRes = R.drawable.img_bag_plastic
            )

            else -> null
        }
    }
}
