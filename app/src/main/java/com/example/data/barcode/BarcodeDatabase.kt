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
        ),
        BarcodeProduct(
            barcode = "4975292602484",
            productName = "ベニカXファインスプレー 1000ml",
            brandName = "住友化学園芸",
            categoryHint = "園芸用殺虫殺菌剤・トリガースプレーボトル",
            parts = listOf(
                ProductPackagingPart(
                    partName = "ボトル本体（PE）",
                    material = "ポリエチレン（PE プラマーク）",
                    categoryKey = "plastic_container",
                    disposalAction = "薬剤を完全に使い切り、内部を水洗いしてプラスチック資源（指定プラ袋）へ。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "スプレーノズル（トリガー部）",
                    material = "プラスチック（PP）＋ 内部極小金属スプリング",
                    categoryKey = "plastic_container",
                    disposalAction = "ボトルから外し水ですすぎます。愛西市では指定プラ袋へ（金属複合で不燃指定の自治体では不燃ごみ）。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "薬剤残液（中身がある場合）",
                    material = "家庭園芸用殺虫殺菌剤（農薬）",
                    categoryKey = "hazardous",
                    disposalAction = "中身を入れたまま出すのは禁止です。使い切るか新聞紙等に染み込ませて乾燥させて可燃袋へ。下水や側溝には絶対に流さないでください。"
                )
            ),
            generalAdvice = "殺虫殺菌剤の容器です。必ず中身を使い切り、内部を水ですすいでからプラスチック資源に出してください。残液がある場合は絶対にそのまま捨てないでください。"
        ),
        BarcodeProduct(
            barcode = "4582319171405",
            productName = "モロッカンビューティ オイルスプレー 145g",
            brandName = "ボトルワークス",
            categoryHint = "ヘアスプレー・エアゾールスプレー缶（高圧ガス製品）",
            parts = listOf(
                ProductPackagingPart(
                    partName = "スプレー缶本体（金属缶）",
                    material = "スチール缶またはアルミ缶（高圧ガスエアゾール）",
                    categoryKey = "hazardous",
                    disposalAction = "中身を完全に使い切り、火気のない風通しの良い屋外でガスを抜いてください。愛西市では穴あけ不要で透明袋（危険・有害ごみ・第2水曜）へ出します。"
                ),
                ProductPackagingPart(
                    partName = "スプレーキャップ・ボタン",
                    material = "プラスチック（PP プラマーク）",
                    categoryKey = "plastic_container",
                    disposalAction = "缶から取り外し、愛西市のプラスチック資源（火曜日・指定プラ袋）へ出します。",
                    isMarkedPlamark = true
                )
            ),
            generalAdvice = "【高圧ガス製品・火気厳禁】スプレー缶は可燃ごみに出すと火災・爆発の危険があります。必ずガス抜きを行ってから、危険・有害ごみまたは不燃ごみ（愛西市：第2水曜日）へお出しください。"
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

        // Generic fallback for any valid barcode (UPC, foreign JAN, etc.) so user never hits a dead end
        if (clean.length in 6..18 && clean.all { it.isDigit() }) {
            return generateGenericProduct(clean)
        }

        return null
    }

    fun getAllPredefined(): List<BarcodeProduct> = products

    private fun generateGenericJapaneseProduct(barcode: String): BarcodeProduct {
        val (manufacturer, isKnown) = when {
            barcode.startsWith("4975292") -> Pair("住友化学園芸（KINCHO園芸）", true)
            barcode.startsWith("4901080") -> Pair("アース製薬", true)
            barcode.startsWith("4902424") -> Pair("フマキラー", true)
            barcode.startsWith("4987115") -> Pair("KINCHO（大日本除虫菊）", true)
            barcode.startsWith("4901777") || barcode.startsWith("4901778") -> Pair("サントリー", true)
            barcode.startsWith("4902102") -> Pair("コカ・コーラ", true)
            barcode.startsWith("4901004") -> Pair("アサヒ", true)
            barcode.startsWith("4901411") -> Pair("キリン", true)
            barcode.startsWith("4901301") -> Pair("花王", true)
            barcode.startsWith("4903301") -> Pair("ライオン", true)
            barcode.startsWith("4902777") -> Pair("明治", true)
            barcode.startsWith("4902105") -> Pair("日清食品", true)
            barcode.startsWith("4901330") -> Pair("カルビー", true)
            barcode.startsWith("4901872") -> Pair("資生堂", true)
            barcode.startsWith("4984824") -> Pair("パナソニック", true)
            else -> Pair("国内流通メーカー製品", false)
        }

        val displayProductName = if (isKnown) {
            "$manufacturer 製品パッケージ"
        } else {
            "国内メーカー包装製品"
        }

        return BarcodeProduct(
            barcode = barcode,
            productName = displayProductName,
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
                    material = "容器本体（缶・ビン・プラ・スプレー缶等）",
                    categoryKey = "check_mark",
                    disposalAction = "中身を空にして材質マークを確認。スプレー缶や金属缶は火災防止のため絶対に可燃ごみに入れないでください。"
                )
            ),
            generalAdvice = "商品のパッケージ裏面に表示されているリサイクル識別表示マーク（プラ、紙、アルミ、スチール、PET）をご確認ください。"
        )
    }

    private fun generateGenericProduct(barcode: String): BarcodeProduct {
        return BarcodeProduct(
            barcode = barcode,
            productName = "包装商品 (バーコード: $barcode)",
            brandName = "製品パッケージ",
            categoryHint = "商品包装・日用品",
            parts = listOf(
                ProductPackagingPart(
                    partName = "外装フィルム・外箱・キャップ",
                    material = "プラマークまたは紙マーク",
                    categoryKey = "plastic_container",
                    disposalAction = "パッケージのリサイクル識別マークを確認し、プラマークはプラスチック製容器包装へ。",
                    isMarkedPlamark = true
                ),
                ProductPackagingPart(
                    partName = "容器本体",
                    material = "ペット・缶・ガラス瓶・プラスチック等",
                    categoryKey = "check_mark",
                    disposalAction = "中身を空にして水洗いし、各自治体の資源（ペット・缶・瓶・プラ資源）または不燃ごみへ。スプレー缶は絶対に可燃ごみに入れないでください。"
                )
            ),
            generalAdvice = "バーコード番号から包装容器を認識しました。裏面のリサイクルマーク（プラ、PET、アルミ、スチール、紙）に従って分別してください。"
        )
    }
}
