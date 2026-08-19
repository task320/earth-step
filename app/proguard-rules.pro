# Hilt / Compose / AndroidX のルールは各ライブラリの consumer rules で足りる。
# Room のエンティティやシリアライズ対象クラスを追加したら、ここに keep ルールを足す (P1 / P7)。

# クラッシュログのスタックトレースを読めるようにする。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
