# EarthStep

徒歩・走行(人力移動)の距離で地球一周(40,075km)を目指すAndroidゲーム。

- 1m = 1XP。地球一周は 40,075,000XP
- 実在する建造物・地形・記録になぞらえた100マイルストーンを、距離が短い順に達成していく
- ビジュアルは全編ドット絵(ピクセルアート)

## ドキュメント

- [仕様書](docs/spec.md)
- [開発タスク一覧](docs/TASKS.md)

## 開発

| 項目 | 値 |
|---|---|
| 言語 / UI | Kotlin / Jetpack Compose |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| JDK | 17 |
| DI | Hilt |

```bash
./gradlew ktlintCheck      # フォーマットチェック
./gradlew ktlintFormat     # 自動整形
./gradlew detekt           # 静的解析
./gradlew lintDebug        # Android Lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

CI (GitHub Actions) は push ごとに上記すべてを実行する。

## ステータス

P0(プロジェクト基盤)まで実装済み。次は P1(データ層)。
