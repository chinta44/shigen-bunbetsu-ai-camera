package com.example.data.repository

import com.example.data.model.CollectionSchedule
import com.example.data.model.Municipality
import com.example.data.model.OversizedFeeItem
import com.example.data.model.OversizedOfficialInfo
import com.example.data.model.ScheduleRecurrence
import com.example.data.model.WasteCategory
import java.time.DayOfWeek

object MunicipalityData {

    // Common Categories
    val CAT_BURNABLE = WasteCategory(
        id = "burnable",
        name = "可燃ごみ（燃やすごみ）",
        shortName = "可燃ごみ",
        icon = "burnable",
        colorHex = 0xFFD32F2FL, // Red / Orange
        containerType = "指定収集袋（黄色または半透明）",
        generalRules = "生ごみはしっかり水切りしてください。紙くず、革製品、ゴム、衛生用品など。"
    )

    val CAT_PLASTIC = WasteCategory(
        id = "plastic",
        name = "プラスチック資源（製品・容器包装）",
        shortName = "プラ資源",
        icon = "plastic",
        colorHex = 0xFF1976D2L, // Blue
        containerType = "透明または中身の見える半透明袋",
        generalRules = "中身を軽く水ですすいで汚れを落としてください。汚れの取れないものは可燃ごみへ。"
    )

    val CAT_BOTTLE_CAN = WasteCategory(
        id = "bottle_can",
        name = "空き缶・空き瓶・ペットボトル",
        shortName = "缶・瓶・PET",
        icon = "bottle",
        colorHex = 0xFF00796BL, // Teal
        containerType = "資源用コンテナまたは透明袋",
        generalRules = "キャップとラベルは外し、さっと水洗いしてください。つぶさずに出す地域もあります。"
    )

    val CAT_PAPER = WasteCategory(
        id = "paper",
        name = "資源古紙（段ボール・雑誌・新聞）",
        shortName = "資源古紙",
        icon = "paper",
        colorHex = 0xFFF57C00L, // Orange
        containerType = "ひもで十字に結束",
        generalRules = "品目別に紙ひも等で十字に束ねてください。雨の日は濡れないよう袋に入れるか次回へ。"
    )

    val CAT_NON_BURNABLE = WasteCategory(
        id = "non_burnable",
        name = "不燃ごみ（燃えないごみ・小型金属）",
        shortName = "不燃ごみ",
        icon = "metal",
        colorHex = 0xFF5D4037L, // Brown
        containerType = "透明・半透明袋",
        generalRules = "ガラス・陶磁器、小型家電、刃物（紙や布で包んで『キケン』と表示）。"
    )

    val CAT_OVERSIZED = WasteCategory(
        id = "oversized",
        name = "粗大ごみ（大型ごみ）",
        shortName = "粗大ごみ",
        icon = "oversized",
        colorHex = 0xFF7B1FA2L, // Purple
        containerType = "納付券（処理シール）貼付・事前予約制",
        generalRules = "電話やWEB、自治体LINE等で事前申し込みが必要です。指定された手数料シールを購入して貼り、収集日の朝に出します。"
    )

    val CAT_HAZARDOUS = WasteCategory(
        id = "hazardous",
        name = "有害・危険ごみ（電池・蛍光灯・スプレー缶）",
        shortName = "有害危険物",
        icon = "danger",
        colorHex = 0xFFC2185BL, // Magenta
        containerType = "透明袋（品目別）または拠点回収BOX",
        generalRules = "スプレー缶は中身を使い切り火気のないところで穴あけ不要で透明袋へ。モバイルバッテリーは家電量販店回収BOXへ。"
    )

    val CAT_SMALL_APPLIANCE = WasteCategory(
        id = "small_appliance",
        name = "小型家電・拠点回収（ボックス回収）",
        shortName = "小型家電",
        icon = "devices",
        colorHex = 0xFF1976D2L, // Blue
        containerType = "回収ボックス投入（袋不要・無料）",
        generalRules = "スマートフォン、携帯電話、タブレット、小型家電、充電器等。個人情報を消去し、市役所・支所・公民館・量販店等の専用回収ボックスへ投入してください。リチウムイオン電池内蔵のため集積所ごみ出しは不可です。"
    )

    val AISAI_OVERSIZED = OversizedOfficialInfo(
        reservationWebUrl = "https://www.city.aisai.lg.jp/contents_detail.php?co=cat&frmId=442&secId=10",
        phoneReservationNumber = "0567243435",
        phoneReservationDisplay = "0567-24-3435（戸別収集受付）",
        receptionHours = "平日 8:30〜17:15（土日祝・年末年始を除く）",
        feeRulesSummary = "集積所収集: 第1水曜日（200円券/品）\n戸別収集: 第3水曜日（予約制・500円券/品・1回5点まで）",
        stickerName = "愛西市 粗大ごみ処理シール（200円券・500円券）",
        purchasePlaces = "愛西市役所、各支所・出張所、市内の各取扱コンビニエンスストア・スーパー等",
        feeGuideItems = listOf(
            OversizedFeeItem("自転車（大人・子供用）", 500, "500円券 × 1枚"),
            OversizedFeeItem("布団・毛布（3枚まで1束）", 200, "200円券 × 1枚"),
            OversizedFeeItem("タンス・本棚（高さ1m以上）", 500, "500円券 × 1枚"),
            OversizedFeeItem("ソファ（1人〜2人掛け）", 500, "500円券 × 1枚"),
            OversizedFeeItem("テーブル・机・こたつ", 500, "500円券 × 1枚"),
            OversizedFeeItem("扇風機・暖房器具・電子レンジ", 200, "200円券 × 1枚"),
            OversizedFeeItem("マットレス（スプリングなし）", 500, "500円券 × 1枚"),
            OversizedFeeItem("物干し竿・じゅうたん・カーペット", 200, "200円券 × 1枚")
        )
    )

    val NAGOYA_OVERSIZED = OversizedOfficialInfo(
        reservationWebUrl = "https://www.city.nagoya.jp/kurashi/category/5-3-2-2-0-0-0-0-0-0.html",
        phoneReservationNumber = "0120758530",
        phoneReservationDisplay = "0120-758-530（携帯からは 052-950-2581）",
        receptionHours = "月〜金 9:00〜17:00（祝日も受付・年末年始除く）",
        feeRulesSummary = "月1回の収集日の7日前までに電話またはインターネットで事前予約。品目ごとに250円/500円/1,000円/1,500円の手数料納付券を購入して貼付。",
        stickerName = "名古屋市 粗大ごみ手数料納付券（250円・500円・1,000円・1,500円）",
        purchasePlaces = "市内のコンビニエンスストア（セブン・ローソン・ファミマ等）、環境事業所、スーパー取扱店",
        feeGuideItems = listOf(
            OversizedFeeItem("自転車・三輪車", 500, "500円券 × 1枚"),
            OversizedFeeItem("布団・マットレス（スプリングなし）", 250, "250円券 × 1枚"),
            OversizedFeeItem("スプリング入りマットレス", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("2人掛け以上ソファ", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("タンス・食器棚・本棚", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("机・学習机・ダイニングテーブル", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("ガスコンロ・ガステーブル", 500, "500円券 × 1枚"),
            OversizedFeeItem("電子レンジ・照明器具・扇風機", 500, "500円券 × 1枚"),
            OversizedFeeItem("じゅうたん・カーペット（畳6畳まで）", 500, "500円券 × 1枚")
        )
    )

    val YOKOHAMA_OVERSIZED = OversizedOfficialInfo(
        reservationWebUrl = "https://www.city.yokohama.lg.jp/kurashi/sumai-kurashi/gomi-recycle/gomi/shushu/sodai/",
        phoneReservationNumber = "0570200530",
        phoneReservationDisplay = "0570-200-530（一般電話 045-330-3953）",
        receptionHours = "月〜土 8:30〜17:00（祝日含む・日曜日除く）",
        feeRulesSummary = "金属製品で50cm以上、その他プラスチック・木製品等は30cm以上が対象。事前にインターネットまたは電話で予約し、指定収集シールを貼付。",
        stickerName = "横浜市 粗大ごみ処理シール（200円券・500円券）",
        purchasePlaces = "市内のコンビニエンスストア、郵便局、各区資源循環局事務所",
        feeGuideItems = listOf(
            OversizedFeeItem("自転車（大人・子供用）", 500, "500円券 × 1枚"),
            OversizedFeeItem("布団・毛布", 200, "200円券 × 1枚"),
            OversizedFeeItem("ソファ（2人掛け以上）", 1000, "500円券 × 2枚"),
            OversizedFeeItem("ダイニングテーブル・机", 500, "500円券 × 1枚"),
            OversizedFeeItem("タンス（高さ・幅合計200cm未満）", 500, "500円券 × 1枚"),
            OversizedFeeItem("ガスレンジ・ガステーブル", 500, "500円券 × 1枚"),
            OversizedFeeItem("扇風機・掃除機・プリンター", 200, "200円券 × 1枚")
        )
    )

    val OSAKA_OVERSIZED = OversizedOfficialInfo(
        reservationWebUrl = "https://www.city.osaka.lg.jp/kankyo/page/0000009054.html",
        phoneReservationNumber = "0120790053",
        phoneReservationDisplay = "0120-79-0053（携帯・一部IP電話 0570-07-0053）",
        receptionHours = "月〜土 9:00〜17:00（祝日含む・年末年始除く）",
        feeRulesSummary = "最大の辺または径が30cmを超えるもの、棒状で1m超のものが対象。粗大ごみ収集受付センターへ電話・Webで申込後、手数料券を購入。",
        stickerName = "大阪市 粗大ごみ処理手数料券（200円・400円・700円・1,000円）",
        purchasePlaces = "市内のコンビニエンスストア、環境事業センター、郵便局等",
        feeGuideItems = listOf(
            OversizedFeeItem("自転車（防犯登録抹消要）", 700, "700円券 × 1枚"),
            OversizedFeeItem("布団（2枚まで1束）", 400, "400円券 × 1枚"),
            OversizedFeeItem("2人掛け以上ソファ", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("タンス・食器棚（3辺計2.5m以上）", 1000, "1,000円券 × 1枚"),
            OversizedFeeItem("テーブル・学習机", 700, "700円券 × 1枚"),
            OversizedFeeItem("扇風機・電気ストーブ・電子レンジ", 400, "400円券 × 1枚")
        )
    )

    val NAGOYA = Municipality(
        id = "nagoya",
        name = "名古屋市",
        prefecture = "愛知県",
        district = "中区・栄・市内全域",
        oversizedThresholdCm = 30,
        plasticRuleNotes = "100%プラスチック素材の製品（タッパー・おもちゃ・バケツなど）も『プラスチック資源』に出せます。ただし30cm角を超えるものは粗大ごみです。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 木曜日"),
            CollectionSchedule("paper", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 水曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1)), "第1 木曜日（月1回）"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前電話・ネット予約制（月1回定期収集地区別）"),
            CollectionSchedule("hazardous", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 木曜日（不燃・拠点回収）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・環境事業所等の専用回収ボックス（開庁時間中随時）")
        ),
        oversizedOfficialInfo = NAGOYA_OVERSIZED,
        officialGuidePdfUrl = "https://www.city.nagoya.jp/kankyo/page/0000008544.html",
        officialGuidePdfTitle = "名古屋市 家庭ごみ・資源の分け方・出し方パンフレット（公式PDF）",
        officialWebUrl = "https://www.city.nagoya.jp/kurashi/category/5-3-0-0-0-0-0-0-0-0.html"
    )

    val YOKOHAMA = Municipality(
        id = "yokohama",
        name = "横浜市",
        prefecture = "神奈川県",
        district = "西区・中区・全域",
        oversizedThresholdCm = 30,
        plasticRuleNotes = "プラマークのある『プラスチック製容器包装』が対象です。商品そのものの硬質プラスチックは30cm未満なら『燃やすごみ』になります。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_NON_BURNABLE,
            CAT_PAPER,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.MONDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 木曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 水曜日"),
            CollectionSchedule("paper", DayOfWeek.SATURDAY, ScheduleRecurrence.Weekly, "毎週 土曜日（集団回収）"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前申込制（粗大ごみ受付センター）"),
            CollectionSchedule("hazardous", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 水曜日（小さな金属類・乾電池）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・地区センター等の小型家電回収BOX")
        ),
        oversizedOfficialInfo = YOKOHAMA_OVERSIZED,
        officialGuidePdfUrl = "https://www.city.yokohama.lg.jp/kurashi/sumai-kurashi/gomi-recycle/gomi/shushu/dashikata/pamphlet.html",
        officialGuidePdfTitle = "横浜市 ごみと資源の分け方・出し方ハンドブック（公式PDF）",
        officialWebUrl = "https://www.city.yokohama.lg.jp/kurashi/sumai-kurashi/gomi-recycle/gomi/"
    )

    val SHIBUYA = Municipality(
        id = "shibuya",
        name = "東京都 渋谷区",
        prefecture = "東京都",
        district = "渋谷・原宿・恵比寿",
        oversizedThresholdCm = 30,
        plasticRuleNotes = "汚れのないプラスチック容器包装・製品プラスチックは回収対象。一辺30cm以上は粗大ごみになります。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 木曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("paper", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 木曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "粗大ごみ受付センター予約制"),
            CollectionSchedule("hazardous", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 木曜日（蛍光管・電池）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・出張所等回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.shibuya.tokyo.jp/kurashi/gomi/gomi/gomi_wakekata.html",
        officialGuidePdfTitle = "渋谷区 資源とごみの分け方・出し方ガイド（公式PDF）",
        officialWebUrl = "https://www.city.shibuya.tokyo.jp/kurashi/gomi/"
    )

    val OSAKA = Municipality(
        id = "osaka",
        name = "大阪市",
        prefecture = "大阪府",
        district = "北区・中央区・全域",
        oversizedThresholdCm = 30,
        plasticRuleNotes = "容器包装プラスチック（プラマークあり）が対象。汚れを落として透明袋へ。最大の辺または径が30cm超は粗大ごみです。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.MONDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
            CollectionSchedule("burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日"),
            CollectionSchedule("paper", DayOfWeek.FRIDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 金曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 金曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "粗大ごみ収集受付センター予約制"),
            CollectionSchedule("hazardous", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "資源ごみと同日回収"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・環境事業センター等回収BOX")
        ),
        oversizedOfficialInfo = OSAKA_OVERSIZED,
        officialGuidePdfUrl = "https://www.city.osaka.lg.jp/kankyo/page/0000369344.html",
        officialGuidePdfTitle = "大阪市 ごみのマナーブック・分別早見表（公式PDF）",
        officialWebUrl = "https://www.city.osaka.lg.jp/kankyo/page/0000009054.html"
    )

    val SAPPORO = Municipality(
        id = "sapporo",
        name = "札幌市",
        prefecture = "北海道",
        district = "中央区・市内全域",
        oversizedThresholdCm = 50,
        plasticRuleNotes = "容器包装プラスチックは週1回無料回収。指定袋に入らないものは大型ごみ（有料）となります。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 木曜日"),
            CollectionSchedule("paper", DayOfWeek.MONDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 月曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 木曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "大型ごみ受付センター予約制"),
            CollectionSchedule("hazardous", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 木曜日（有害ごみ）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・地区リサイクルセンター回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.sapporo.jp/seiso/gomi/wakekata/guidebook.html",
        officialGuidePdfTitle = "札幌市 ごみ分けガイド・早見表（公式PDF）",
        officialWebUrl = "https://www.city.sapporo.jp/seiso/gomi/"
    )

    val FUKUOKA = Municipality(
        id = "fukuoka",
        name = "福岡市",
        prefecture = "福岡県",
        district = "博多区・中央区・市内全域",
        oversizedThresholdCm = 45,
        plasticRuleNotes = "福岡市は夜間収集が特徴です。プラスチック製容器包装は燃えるごみまたは資源回収へ。指定袋に入らないものは粗大ごみ。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.SUNDAY, ScheduleRecurrence.Weekly, "毎週 日曜日・水曜日（夜間収集）"),
            CollectionSchedule("burnable", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 日曜日・水曜日（夜間収集）"),
            CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1)), "第1 木曜日（月1回）"),
            CollectionSchedule("paper", DayOfWeek.SATURDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 土曜日"),
            CollectionSchedule("non_burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(3)), "第3 木曜日（月1回）"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "粗大ごみ受付センター（LINE・電話予約）"),
            CollectionSchedule("hazardous", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(3)), "不燃ごみと同日回収"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "区役所・市民センター等回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.fukuoka.lg.jp/kankyo/jigyokei/life/kateigomi-bunbetsu/rulebook.html",
        officialGuidePdfTitle = "福岡市 家庭ごみルールブック・分別表（公式PDF）",
        officialWebUrl = "https://www.city.fukuoka.lg.jp/kankyo/jigyokei/life/kateigomi-bunbetsu/"
    )

    val AISAI = Municipality(
        id = "aisai",
        name = "愛西市",
        prefecture = "愛知県",
        district = "市内全域（佐屋・立田・八開・佐織地区）",
        oversizedThresholdCm = 45,
        plasticRuleNotes = "市指定プラスチック類ごみ専用袋を使用。菓子袋、レジ袋、洗剤・シャンプー容器、プラ製トレイ、合成樹脂等。白トレイは資源ごみ。指定袋に入らないものは粗大ごみです。",
        categories = listOf(
            CAT_BURNABLE,
            CAT_PLASTIC,
            CAT_BOTTLE_CAN,
            CAT_PAPER,
            CAT_NON_BURNABLE,
            CAT_OVERSIZED,
            CAT_HAZARDOUS,
            CAT_SMALL_APPLIANCE
        ),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.MONDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日（指定可燃物袋）"),
            CollectionSchedule("burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日（指定可燃物袋）"),
            CollectionSchedule("plastic", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日（指定プラ袋）"),
            CollectionSchedule("bottle_can", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 水曜日（資源ごみ）"),
            CollectionSchedule("paper", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 水曜日（資源古紙・段ボール）"),
            CollectionSchedule("non_burnable", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2)), "第2 水曜日（指定不燃物袋）"),
            CollectionSchedule("oversized", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "集積所:第1水曜(200円券) / 戸別回収:第3水曜(予約制・500円券)"),
            CollectionSchedule("hazardous", DayOfWeek.WEDNESDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2)), "第2 水曜日（不燃・危険物・電池類）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "市役所・支所・公民館等の専用回収ボックス（開館中随時）")
        ),
        oversizedOfficialInfo = AISAI_OVERSIZED,
        officialGuidePdfUrl = "https://www.city.aisai.lg.jp/contents_detail.php?frmId=442",
        officialGuidePdfTitle = "愛西市 家庭ごみ分別早見表（公式PDF・詳細ガイド）",
        officialWebUrl = "https://www.city.aisai.lg.jp/contents_detail.php?frmId=442"
    )

    val TSUSHIMA = Municipality(
        id = "tsushima",
        name = "津島市",
        prefecture = "愛知県",
        district = "市内全域",
        oversizedThresholdCm = 50,
        plasticRuleNotes = "プラスチック製容器包装（プラマーク付）は指定収集。指定袋に入らないものは粗大ごみ。",
        categories = listOf(CAT_BURNABLE, CAT_PLASTIC, CAT_BOTTLE_CAN, CAT_PAPER, CAT_NON_BURNABLE, CAT_OVERSIZED, CAT_HAZARDOUS, CAT_SMALL_APPLIANCE),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 木曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前予約制（電話申込）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "市役所・公共施設等の回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.tsushima.lg.jp/kurashi/gomi/gomidashikata/index.html",
        officialGuidePdfTitle = "津島市 ごみの出し方・分別ガイド（公式）",
        officialWebUrl = "https://www.city.tsushima.lg.jp/kurashi/gomi/"
    )

    val YATOMI = Municipality(
        id = "yatomi",
        name = "弥富市",
        prefecture = "愛知県",
        district = "市内全域",
        oversizedThresholdCm = 50,
        plasticRuleNotes = "プラスチック製容器包装は分別回収。八穂クリーンセンター利用。",
        categories = listOf(CAT_BURNABLE, CAT_PLASTIC, CAT_BOTTLE_CAN, CAT_PAPER, CAT_NON_BURNABLE, CAT_OVERSIZED, CAT_HAZARDOUS, CAT_SMALL_APPLIANCE),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.MONDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
            CollectionSchedule("burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前予約制または八穂持込"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "総合社会教育センター等回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.yatomi.lg.jp/kurashi/kankyo/1000780/index.html",
        officialGuidePdfTitle = "弥富市 ごみの出し方・分別ハンドブック（公式）",
        officialWebUrl = "https://www.city.yatomi.lg.jp/kurashi/kankyo/"
    )

    val ICHINOMIYA = Municipality(
        id = "ichinomiya",
        name = "一宮市",
        prefecture = "愛知県",
        district = "市内全域",
        oversizedThresholdCm = 45,
        plasticRuleNotes = "プラスチック製容器包装と製品プラスチックの資源回収。",
        categories = listOf(CAT_BURNABLE, CAT_PLASTIC, CAT_BOTTLE_CAN, CAT_PAPER, CAT_NON_BURNABLE, CAT_OVERSIZED, CAT_HAZARDOUS, CAT_SMALL_APPLIANCE),
        schedules = listOf(
            CollectionSchedule("burnable", DayOfWeek.TUESDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("burnable", DayOfWeek.FRIDAY, ScheduleRecurrence.Weekly, "毎週 火曜日・金曜日"),
            CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
            CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前予約制（粗大ごみ受付）"),
            CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "市役所・出張所等回収BOX")
        ),
        officialGuidePdfUrl = "https://www.city.ichinomiya.aichi.jp/shisei/shisetsu/kankyo/1000062.html",
        officialGuidePdfTitle = "一宮市 家庭ごみの分け方・出し方（公式PDF）",
        officialWebUrl = "https://www.city.ichinomiya.aichi.jp/shisei/shisetsu/kankyo/"
    )

    val ALL_MUNICIPALITIES: List<Municipality> = listOf(
        AISAI,
        NAGOYA,
        TSUSHIMA,
        YATOMI,
        ICHINOMIYA,
        YOKOHAMA,
        SHIBUYA,
        OSAKA,
        SAPPORO,
        FUKUOKA
    )

    fun createCustomMunicipality(name: String, prefecture: String = ""): Municipality {
        val cleanName = name.trim()
        val pref = if (prefecture.isNotBlank()) prefecture else if (cleanName.contains("市") || cleanName.contains("町") || cleanName.contains("村")) "" else "全国"
        return Municipality(
            id = "custom_${cleanName.hashCode().toString().replace("-", "n")}",
            name = cleanName,
            prefecture = pref,
            district = "管内全域",
            oversizedThresholdCm = 45,
            plasticRuleNotes = "自治体指定袋および分別ルール（容器包装プラ・硬質プラ）に沿って分別してください。袋に入らないものは粗大ごみです。",
            categories = listOf(
                CAT_BURNABLE,
                CAT_PLASTIC,
                CAT_BOTTLE_CAN,
                CAT_PAPER,
                CAT_NON_BURNABLE,
                CAT_OVERSIZED,
                CAT_HAZARDOUS,
                CAT_SMALL_APPLIANCE
            ),
            schedules = listOf(
                CollectionSchedule("burnable", DayOfWeek.MONDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
                CollectionSchedule("burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.Weekly, "毎週 月曜日・木曜日"),
                CollectionSchedule("plastic", DayOfWeek.WEDNESDAY, ScheduleRecurrence.Weekly, "毎週 水曜日"),
                CollectionSchedule("bottle_can", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 木曜日"),
                CollectionSchedule("paper", DayOfWeek.SATURDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 土曜日"),
                CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前予約制（各自治体窓口・クリーンセンター）"),
                CollectionSchedule("small_appliance", null, ScheduleRecurrence.OnDemandReservation, "市役所・量販店等の回収BOX（開館中随時）")
            ),
            oversizedOfficialInfo = OversizedOfficialInfo(
                reservationWebUrl = "https://www.google.com/search?q=${cleanName}+粗大ごみ+予約",
                phoneReservationNumber = "",
                phoneReservationDisplay = "${cleanName}役所・粗大ごみ受付窓口",
                receptionHours = "平日 8:30〜17:00（各自治体にお問い合わせください）",
                feeRulesSummary = "各自治体の指定粗大ごみ処理シールまたは納付券をご購入の上、指定の排出場所へお出しください。",
                stickerName = "${cleanName} 粗大ごみ処理券",
                purchasePlaces = "市区町村役所窓口、取扱コンビニエンスストア等",
                feeGuideItems = listOf(
                    OversizedFeeItem("大型家具・ベッド・ソファ", 1000, "自治体指定納付券"),
                    OversizedFeeItem("自転車・小型家具・机", 500, "自治体指定納付券"),
                    OversizedFeeItem("布団・毛布・カーペット", 300, "自治体指定納付券")
                )
            )
        )
    }

    fun findById(id: String): Municipality {
        if (id.startsWith("custom_")) {
            val decodedName = id.removePrefix("custom_")
            return createCustomMunicipality(decodedName)
        }
        return ALL_MUNICIPALITIES.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AISAI
    }

    fun findByNameOrKeyword(query: String): Municipality {
        val q = query.trim()
        if (q.isBlank()) return AISAI

        // 1. Exact or contains in municipality name
        val nameMatch = ALL_MUNICIPALITIES.firstOrNull {
            it.name.equals(q, ignoreCase = true) ||
                    q.contains(it.name, ignoreCase = true) ||
                    it.name.contains(q, ignoreCase = true)
        }
        if (nameMatch != null) return nameMatch

        // 2. Match district
        val districtMatch = ALL_MUNICIPALITIES.firstOrNull {
            it.district.contains(q, ignoreCase = true)
        }
        if (districtMatch != null) return districtMatch

        // 3. If query itself looks like a city/town name, dynamically create custom municipality
        if (q.length >= 2 && (q.endsWith("市") || q.endsWith("区") || q.endsWith("町") || q.endsWith("村"))) {
            return createCustomMunicipality(q)
        }

        // Fallback
        return AISAI
    }
}
