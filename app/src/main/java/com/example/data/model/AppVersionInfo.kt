package com.example.data.model

data class VersionRelease(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val title: String,
    val changes: List<String>
)

object AppVersionManager {
    const val CURRENT_VERSION_NAME = "1.1.0"
    const val CURRENT_VERSION_CODE = 2

    val VERSION_HISTORY: List<VersionRelease> = listOf(
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
