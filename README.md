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

### Android SDK が無い環境での確認

`dl.google.com` に到達できない環境では Gradle ビルドが通らない(Android SDK と Google Maven の両方が
そのホストにあるため)。到達できる環境なら上記コマンドはすべてローカルで実行できる。
到達できない場合でも ktlint / detekt だけは Maven Central から CLI を取得して単体で実行できる。

```bash
curl -L -o /tmp/ktlint.jar https://repo.maven.apache.org/maven2/com/pinterest/ktlint/ktlint-cli/1.5.0/ktlint-cli-1.5.0-all.jar
java -jar /tmp/ktlint.jar "app/src/**/*.kt" "**/*.kts"          # --format で自動修正

curl -L -o /tmp/detekt.jar https://repo.maven.apache.org/maven2/io/gitlab/arturbosch/detekt/detekt-cli/1.23.8/detekt-cli-1.23.8-all.jar
java -jar /tmp/detekt.jar --config config/detekt/detekt.yml --build-upon-default-config \
  --input app/src/main/java,app/src/test/java
```

ネットワークが制限された環境では、コンパイル・Android Lint・ユニットテストの実行は CI 側でのみ検証される。

### データベース

Room のスキーマJSONは `app/schemas/` に出力してコミットする。`EarthStepDatabase.VERSION` を上げたときは
生成された新しいJSONを必ず含めること(`SchemaExportTest` が出し忘れを検知する)。
マイグレーション方針は `EarthStepMigrations` の KDoc を参照。

### 計測エンジン

距離計算は Android に依存しない `core/domain/measurement` に閉じてあり、
`core/data/measurement` の各 DataSource が位置・歩数・活動判定を供給する。
擬似走行ログ(`app/src/test/resources/measurement`)を流す `PseudoTrackDistanceTest` が
直線・カーブ・ジッター・GPSジャンプ・トンネル欠測・モック位置の各ケースを検証する。

## ステータス

P2(計測エンジン)まで実装済み。次は P3(常駐サービス・権限・オンボーディング)。
権限リクエストの導線が未実装のため、この時点では実機での計測は動かない。
