package com.example.data.barcode

object BarcodeDatabase {

    private val products = listOf(
        BarcodeProduct(
            barcode = "4902102072618",
            productName = "コカ・コーラ 500ml PET",
            brandName = "日本コカ・コーラ",
            categoryHint = "清涼飲料水・PETボトル",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル本体",
                    material = "ポリエチレンテレフタレート (PET 1)",
                    categoryKey = "pet",
                    disposalAction = "中身を空にして軽く水洗いし、潰して出します。"
                ),
                ProductPackagingPart(
                    partName = "キャップ",
                    material = "ポリプロピレン (PP プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "本体から外し、プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "外装ラベルフィルム",
                    material = "ポリスチレン (PS プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "ミシン目から剥がしてプラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "キャップとラベルを必ず外して分別してください。自治体の資源回収またはスーパーの回収ボックスも利用できます。"
        ),
        BarcodeProduct(
            barcode = "4901777018686",
            productName = "サントリー 天然水 2L PET",
            brandName = "サントリー",
            categoryHint = "ミネラルウォーター・PETボトル",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル本体",
                    material = "PET (リサイクル素材)",
                    categoryKey = "pet",
                    disposalAction = "中身を飲みきり軽く水洗いして小さく畳みます。"
                ),
                ProductPackagingPart(
                    partName = "キャップ",
                    material = "プラスチック (PE/PP)",
                    categoryKey = "plastic_container",
                    disposalAction = "外してプラ容器包装へ。ワクチン支援キャップ回収も活用可。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "ロールラベル",
                    material = "プラスチックフィルム",
                    categoryKey = "plastic_container",
                    disposalAction = "剥がしてプラスチック容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "サントリーの薄肉PETは簡単に小さく折りたためます。資源ごみの減容にご協力ください。"
        ),
        BarcodeProduct(
            barcode = "4901004006714",
            productName = "アサヒ スーパードライ 350ml 缶",
            brandName = "アサヒビール",
            categoryHint = "ビール・アルミ缶",
            parts = listOf(
                ProductPackagingPart(
                    partName = "缶本体（プルタブ含む）",
                    material = "アルミニウム (アルミマーク)",
                    categoryKey = "can",
                    disposalAction = "軽く水洗いして水を切ります。プルタブは外さず中に押し込みます。"
                )
            ),
            generalAdvice = "タバコの吸い殻などの異物は絶対に入れないでください。アルミ缶は貴重な再資源化素材です。"
        ),
        BarcodeProduct(
            barcode = "4902105016084",
            productName = "日清 カップヌードル レギュラー 78g",
            brandName = "日清食品",
            categoryHint = "即席カップめん・複合紙容器",
            parts = listOf(
                ProductPackagingPart(
                    partName = "カップ本体",
                    material = "紙・バイオマスエコカップ",
                    categoryKey = "burnable",
                    disposalAction = "残り汁を拭き取り、可燃ごみ（燃やすごみ）へ。"
                ),
                ProductPackagingPart(
                    partName = "フタ (外フタ)",
                    material = "紙・アルミ蒸着複合材",
                    categoryKey = "burnable",
                    disposalAction = "汚れを落として可燃ごみへ。"
                ),
                ProductPackagingPart(
                    partName = "外装シュリンクフィルム",
                    material = "プラスチック (プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "スープなどの汁は排水口に流さず紙に吸わせるか新聞紙等に包んで可燃ごみに出してください。"
        ),
        BarcodeProduct(
            barcode = "4902777000100",
            productName = "明治 おいしい牛乳 900ml",
            brandName = "明治",
            categoryHint = "紙パック・牛乳パック",
            parts = listOf(
                ProductPackagingPart(
                    partName = "紙パック本体",
                    material = "紙（内側白色・ポリエチレンラミネート）",
                    categoryKey = "paper",
                    disposalAction = "水洗いして切り開き、よく乾かして紙パック資源またはスーパー店頭回収へ。"
                ),
                ProductPackagingPart(
                    partName = "注ぎ口・プラスチックキャップ",
                    material = "プラスチック (PE)",
                    categoryKey = "plastic_container",
                    disposalAction = "パックからハサミ等で切り離し、プラ容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "内側が白い紙パックは貴重なバージンパルプです。切り開いて洗って乾かすとトイレットペーパー等に再生されます。"
        ),
        BarcodeProduct(
            barcode = "4901301288622",
            productName = "花王 アタック抗菌EX 洗剤本体 880g",
            brandName = "花王",
            categoryHint = "洗濯用洗剤ボトル・プラスチック",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル容器本体",
                    material = "ポリエチレン (PE プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "使い切って軽くすすぎ、プラスチック製容器包装へ。"
                ),
                ProductPackagingPart(
                    partName = "計量キャップ・ノズル",
                    material = "ポリプロピレン (PP プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "水ですすいでプラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "中身が残っている場合は水で薄めて流すか新聞紙に吸わせて可燃ごみへ。容器はプラ回収へ。"
        ),
        BarcodeProduct(
            barcode = "4901872445479",
            productName = "資生堂 TSUBAKI プレミアムモイスト シャンプー 490ml",
            brandName = "資生堂",
            categoryHint = "ヘアケアボトル・ポンプ容器",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル本体",
                    material = "プラスチック (PET/PE プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "中を軽く水洗いしてプラ容器包装へ。"
                ),
                ProductPackagingPart(
                    partName = "ディスペンサーポンプ部",
                    material = "複合素材（プラ＋内部金属バネ）",
                    categoryKey = "burnable", // 自治体によっては不燃
                    disposalAction = "金属バネが内蔵されているため、分解できない場合は可燃ごみ（または不燃ごみ）へ。"
                )
            ),
            generalAdvice = "ポンプ内部には小さな金属スプリングが入っているため、自治体により可燃または不燃に分かれます。"
        ),
        BarcodeProduct(
            barcode = "4901330502881",
            productName = "カルビー ポテトチップス うすしお味 60g",
            brandName = "カルビー",
            categoryHint = "スナック菓子袋・アルミ蒸着プラ",
            parts = listOf(
                ProductPackagingPart(
                    partName = "外袋",
                    material = "プラ（内側アルミ蒸着フィルム・プラマーク）",
                    categoryKey = "plastic_container",
                    disposalAction = "中の油やカスを払い、プラ容器包装（または汚れが取れない場合は可燃ごみ）へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "油汚れがひどい場合は可燃ごみ扱いとなる自治体が多いです。軽くティッシュ等で拭き取ってください。"
        ),
        BarcodeProduct(
            barcode = "4902011711400",
            productName = "エリエール ティシュー 180組5個パック",
            brandName = "大王製紙",
            categoryHint = "ティッシュペーパー箱・古紙",
            parts = listOf(
                ProductPackagingPart(
                    partName = "紙箱カートン",
                    material = "板紙・雑がみ",
                    categoryKey = "paper",
                    disposalAction = "折りたたんで雑がみ・古紙資源へ。"
                ),
                ProductPackagingPart(
                    partName = "取り出し口フィルム",
                    material = "ポリエチレンフィルム (プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "箱から指で剥がし、プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "外装大袋",
                    material = "ポリエチレン (プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "ティッシュ箱の取り出し口の透明フィルムは、指をひっかけると簡単に剥がせます。箱は雑紙として資源回収へ！"
        ),
        BarcodeProduct(
            barcode = "4901990363341",
            productName = "東洋水産 マルちゃん 赤いきつねうどん 96g",
            brandName = "東洋水産",
            categoryHint = "即席めん・発泡スチロール丼容器",
            parts = listOf(
                ProductPackagingPart(
                    partName = "どんぶり容器本体",
                    material = "発泡ポリスチレン (PSP プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "スープ汚れを軽く水ですすぎ、プラ容器包装へ（落ちない場合は可燃ごみ）。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "フタ",
                    material = "アルミ蒸着紙フタ",
                    categoryKey = "burnable",
                    disposalAction = "可燃ごみへ。"
                ),
                ProductPackagingPart(
                    partName = "かやく・スープ小袋",
                    material = "プラマーク小袋",
                    categoryKey = "plastic_container",
                    disposalAction = "中身を使い切り、プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "白く清潔にすすげればプラスチック製容器包装、カレーなど油汚れが落ちない場合は可燃ごみに出してください。"
        ),
        BarcodeProduct(
            barcode = "4901085089312",
            productName = "伊藤園 お〜いお茶 緑茶 525ml PET",
            brandName = "伊藤園",
            categoryHint = "緑茶飲料・PETボトル",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル本体",
                    material = "PETボトル",
                    categoryKey = "pet",
                    disposalAction = "中をすすいで潰します。"
                ),
                ProductPackagingPart(
                    partName = "キャップ・ラベル",
                    material = "プラスチック (プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "外してプラ容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "キャップ・ラベルはプラスチック製容器包装、本体はペットボトル資源です。"
        ),
        BarcodeProduct(
            barcode = "4984824700000",
            productName = "パナソニック アルカリ乾電池 単3形 4本パック",
            brandName = "パナソニック",
            categoryHint = "乾電池・有害・危険ごみ",
            parts = listOf(
                ProductPackagingPart(
                    partName = "乾電池本体（使用済）",
                    material = "アルカリ乾電池",
                    categoryKey = "hazardous",
                    disposalAction = "両極（プラス・マイナス）にセロハンテープ等を貼り絶縁し、発火防止して有害ごみ・電池回収へ。"
                ),
                ProductPackagingPart(
                    partName = "パッケージ台紙",
                    material = "紙（雑がみ）",
                    categoryKey = "paper",
                    disposalAction = "資源古紙（雑がみ）へ。"
                ),
                ProductPackagingPart(
                    partName = "ブリスター透明カバー",
                    material = "プラスチック (PET プラマーク)",
                    categoryKey = "plastic_container",
                    disposalAction = "プラスチック製容器包装へ。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "乾電池は電極をテープで絶縁してから有害危険ごみや家電量販店・区役所の回収ボックスへ出してください。"
        )
    )

    fun findByBarcode(barcode: String): BarcodeProduct? {
        val clean = barcode.trim()
        val exact = products.firstOrNull { it.barcode == clean }
        if (exact != null) return exact

        // Prefix / pattern recognition for unknown Japanese JAN codes
        if (clean.length in 8..14 && (clean.startsWith("45") || clean.startsWith("49"))) {
            return generateGenericJapaneseProduct(clean)
        }

        return null
    }

    fun getAllPredefined(): List<BarcodeProduct> = products

    private fun generateGenericJapaneseProduct(barcode: String): BarcodeProduct {
        val manufacturer = when {
            barcode.startsWith("4901777") || barcode.startsWith("4901778") -> "サントリー"
            barcode.startsWith("4902102") -> "コカ・コーラ"
            barcode.startsWith("4901004") -> "アサヒ"
            barcode.startsWith("4901411") -> "キリン"
            barcode.startsWith("4901301") -> "花王"
            barcode.startsWith("4903301") -> "ライオン"
            barcode.startsWith("4902777") -> "明治"
            barcode.startsWith("4902105") -> "日清食品"
            barcode.startsWith("4901330") -> "カルビー"
            barcode.startsWith("4901872") -> "資生堂"
            barcode.startsWith("4984824") -> "パナソニック"
            else -> "国内メーカー製品 (JAN: $barcode)"
        }

        return BarcodeProduct(
            barcode = barcode,
            productName = "$manufacturer 包装製品 (バーコード: $barcode)",
            brandName = manufacturer,
            categoryHint = "日用品・食品包装容器",
            parts = listOf(
                ProductPackagingPart(
                    partName = "外装フィルム・キャップ",
                    material = "プラスチック製容器包装（プラマーク付）",
                    categoryKey = "plastic_container",
                    disposalAction = "プラマークがある包装はプラスチック資源へ。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "本体容器",
                    material = "ボトルまたは容器本体",
                    categoryKey = "burnable",
                    disposalAction = "材質（PET、缶、紙、プラ、瓶）を確認し、汚れを落として分別してください。"
                )
            ),
            generalAdvice = "商品のパッケージ裏面に表示されているリサイクル識別表示マーク（プラ、紙、アルミ、スチール、PET）をご確認ください。"
        )
    }
}
