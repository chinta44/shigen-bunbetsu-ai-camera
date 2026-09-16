package com.example.data.model

data class VersionRelease(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val title: String,
    val changes: List<String>
)

object AppVersionManager {
    const val CURRENT_VERSION_NAME = "1.3.0"
    const val CURRENT_VERSION_CODE = 4

    val VERSION_HISTORY: List<VersionRelease> = listOf(
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
