package com.example.data.model

data class VersionRelease(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val title: String,
    val changes: List<String>
)

object AppVersionManager {
    const val CURRENT_VERSION_NAME = "1.3.2"
    const val CURRENT_VERSION_CODE = 6

    val VERSION_HISTORY: List<VersionRelease> = listOf(
        VersionRelease(
            versionName = "1.3.2",
            versionCode = 6,
            releaseDate = "2026-09-16",
            title = "小型家電・電化製品・扇風機・危険物の分別ルーティング根本改修",
            changes = listOf(
                "小型扇風機・サーキュレーター・小型家電が質問フロー後にプラスチック資源へ誤分類される構造的不具合を解消",
                "小型家電回収ボックス（市役所・支所）および愛西市指定不燃物専用袋（第2水曜）への適正案内を追加",
                "充電式・ハンディファン等のリチウムイオン電池内蔵機器に対する火災防止・発火注意ガイダンスを強化",
                "質問回答処理（resolveClarification）に電化製品・危険物・衣類・陶器ガラスの包括的セーフティルーティングを実装"
            )
        ),
        VersionRelease(
            versionName = "1.3.1",
            versionCode = 5,
            releaseDate = "2026-09-16",
            title = "ステンレス水筒・複合材質の判定精度大幅向上＆金属品目セーフガード",
            changes = listOf(
                "ステンレス水筒・魔法瓶・金属ボトルの分別判定を強化（可燃ごみやプラ単体判定への誤分類を防止）",
                "本体（金属・不燃ごみ）とフタ・パッキン（プラ・可燃）の分解・分別ガイダンスの詳細化",
                "AIプロンプトの複合材質（金属＋プラスチック部品）解釈ルール最適化",
                "鍋・フライパン・傘・刃物などの金属製品が誤って可燃ごみにならないセーフガード機構の追加",
                "追加質問（材質確認）における「金属混在」選択時の適切な不燃ごみルーティング対応"
            )
        ),
        VersionRelease(
            versionName = "1.3.0",
            versionCode = 4,
            releaseDate = "2026-09-16",
            title = "機能強化・自治体モデル連携強化＆安定性向上",
            changes = listOf(
                "自治体データとモデル連携の修正およびビルド安定性の向上",
                "判定機能・手動訂正・回収拠点マップの安定稼働とパフォーマンス改善",
                "バージョン情報管理の刷新"
            )
        ),
        VersionRelease(
            versionName = "1.2.0",
            versionCode = 3,
            releaseDate = "2026-09-13",
            title = "判定結果の訂正機能・対象物指定・撮影＆判定フィードバック・全国自治体AI自動生成・拠点マップ",
            changes = listOf(
                "判定結果の手動訂正機能の追加（誤判定品名の修正や分別区分の手動直接指定）",
                "画面内に複数物が映っている場合の「判定対象物の指定」機能（例: 長財布、メガネ、缶等を指定可能）",
                "撮影直後および判定結果通知時の振動（バイブレーション）＆効果音機能（設定で個別にON/OFF切替可能）",
                "第１計画: 自治体名入力によるAI分別ルール＆カレンダー自動生成機能・手動微調整機能",
                "第２計画: 乾電池・蛍光管・小型家電・古紙等のリサイクル拠点・回収ボックス案内マップ機能"
            )
        ),
        VersionRelease(
            versionName = "1.1.0",
            versionCode = 2,
            releaseDate = "2026-09-12",
            title = "愛西市対応・ユーザー独自APIキー・指定ごみ袋見本表示",
            changes = listOf(
                "愛知県愛西市の分別ルールおよび収集カレンダー（可燃・プラ・粗大ごみ等）の正式登録",
                "GPS現在地取得時の市区町村判定アルゴリズムの最適化（誤判定の解消）",
                "カメラ起動時の実行時権限チェックと安全保護（クラッシュ防止・アルバムフォールバック）",
                "利用者が自身のGemini APIキーを取得・登録できる設定機能の追加",
                "分別結果画面に「指定ごみ袋・シール」の実物写真見本と出し方ガイドを追加",
                "バージョン情報・変更履歴画面の追加"
            )
        ),
        VersionRelease(
            versionName = "1.0.0",
            versionCode = 1,
            releaseDate = "2026-09-10",
            title = "初回リリース",
            changes = listOf(
                "AI画像認識によるゴミ分別アシスタント機能",
                "材質やサイズに応じた追加質問・分岐機能",
                "商品バーコード検索機能",
                "主要自治体の収集カレンダーおよび次回収集日カウントダウン表示"
            )
        )
    )
}
