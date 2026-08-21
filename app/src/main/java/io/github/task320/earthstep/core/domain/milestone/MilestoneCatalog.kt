package io.github.task320.earthstep.core.domain.milestone

import io.github.task320.earthstep.core.domain.progress.Earth

/**
 * 仕様3.2の100マイルストーンの静的マスタ(P1-4)。
 *
 * - 距離はm単位の Long。仕様表のkm値をmへ換算した値をそのまま持つ
 * - 距離の厳密昇順で並ぶ。番号(index)は並び順そのもの
 * - #100 は実在物ではなく、当初からのゴールである地球一周(40,075km)に固定(仕様3.1)
 * - `isMajor` は大台演出の対象。現状は #100 のみ(仕様3.4 / P1-6)。
 *   周内マーカー(25/50/75%)は周回側の概念なのでこの一覧には含めない
 *
 * 一覧の内容を変更したら [MilestoneCatalogTest] が不変条件を検証する。
 */
// 実在物の距離そのものが値であり、定数へ切り出しても意味を持たないためマジックナンバー検査を外す。
@Suppress("MagicNumber")
object MilestoneCatalog {

    /** マイルストーンの総数。 */
    const val SIZE = 100

    val milestones: List<Milestone> = listOf(
        Milestone(1, 300L, "あべのハルカス(日本一高いビル)の高さ"),
        Milestone(2, 333L, "東京タワーの高さ"),
        Milestone(3, 541L, "ワン・ワールド・トレード・センターの高さ"),
        Milestone(4, 553L, "CNタワーの高さ(旧世界一高い自立式建造物)"),
        Milestone(5, 555L, "ロッテワールドタワーの高さ"),
        Milestone(6, 600L, "広州塔の高さ"),
        Milestone(7, 632L, "上海タワーの高さ"),
        Milestone(8, 634L, "東京スカイツリーの高さ(現存する世界一高い塔)"),
        Milestone(9, 679L, "マーデカ118の高さ(世界2位の高さのビル)"),
        Milestone(10, 828L, "ブルジュ・ハリファの高さ(世界一高いビル)"),
        Milestone(11, 2_700L, "ゴールデンゲートブリッジの全長"),
        Milestone(12, 3_776L, "富士山の標高"),
        Milestone(13, 4_200L, "ヴェラザノナローズブリッジの全長"),
        Milestone(14, 8_000L, "マッキノー橋の全長"),
        Milestone(15, 8_848L, "エベレストの標高"),
        Milestone(16, 9_600L, "東京湾アクアトンネル"),
        Milestone(17, 10_920L, "マリアナ海溝の深さ(世界最深部)"),
        Milestone(18, 14_000L, "ジブラルタル海峡の最狭部"),
        Milestone(19, 18_200L, "山手トンネル(世界2位の道路トンネル)"),
        Milestone(20, 24_500L, "ラルダールトンネル(世界最長の道路トンネル)"),
        Milestone(21, 34_000L, "ドーバー海峡の最狭部"),
        Milestone(22, 36_400L, "霊渠(中国、世界最古の運河のひとつ)"),
        Milestone(23, 42_200L, "マラソンの正式距離"),
        Milestone(24, 50_500L, "英仏海峡トンネル"),
        Milestone(25, 53_850L, "青函トンネル"),
        Milestone(26, 57_100L, "ゴッタルドベーストンネル(世界最長の鉄道トンネル)"),
        Milestone(27, 70_000L, "しまなみ海道"),
        Milestone(28, 80_000L, "パナマ運河"),
        Milestone(29, 82_000L, "ベーリング海峡の最狭部"),
        Milestone(30, 98_700L, "キール運河"),
        Milestone(31, 150_000L, "淡路島一周(あわいち)"),
        Milestone(32, 164_000L, "世界最長の橋・丹陽-昆山グランドブリッジ"),
        Milestone(33, 193_000L, "スエズ運河"),
        Milestone(34, 200_000L, "琵琶湖一周(ビワイチ)"),
        Milestone(35, 367_000L, "信濃川(日本最長の川)"),
        Milestone(36, 400_000L, "東京から大阪"),
        Milestone(37, 408_000L, "国際宇宙ステーションの軌道高度"),
        Milestone(38, 636_000L, "バイカル湖の南北の長さ(世界最古・最深の湖)"),
        Milestone(39, 680_000L, "東京から広島"),
        Milestone(40, 780_000L, "サンティアゴ巡礼路(フランス人の道)"),
        Milestone(41, 880_000L, "東京から福岡"),
        Milestone(42, 1_000_000L, "台湾一周(環島)"),
        Milestone(43, 1_030_000L, "カスピ海の南北の長さ(世界最大の湖)"),
        Milestone(44, 1_090_000L, "四国遍路(最短ルート計算)"),
        Milestone(45, 1_200_000L, "アルプス山脈の全長"),
        Milestone(46, 1_400_000L, "本州縦断(大間崎-潮岬)"),
        Milestone(47, 1_550_000L, "東京から沖縄・那覇"),
        Milestone(48, 1_600_000L, "マダガスカル島の南北の長さ"),
        Milestone(49, 1_794_000L, "京杭大運河(世界最長の運河)"),
        Milestone(50, 2_000_000L, "グレートバリアリーフの全長(世界最大のサンゴ礁)"),
        Milestone(51, 2_300_000L, "日本列島(北海道-本州-九州)"),
        Milestone(52, 2_330_000L, "コロラド川(グランドキャニオンを刻んだ川)"),
        Milestone(53, 2_400_000L, "ヒマラヤ山脈の全長(狭義)"),
        Milestone(54, 2_500_000L, "ウラル山脈の全長"),
        Milestone(55, 2_670_000L, "グリーンランドの南北の長さ"),
        Milestone(56, 2_850_000L, "ドナウ川(ヨーロッパで2番目に長い川)"),
        Milestone(57, 2_900_000L, "東京から香港"),
        Milestone(58, 3_141_000L, "メキシコ・アメリカ国境"),
        Milestone(59, 3_300_000L, "日本全体(択捉島-与那国島)"),
        Milestone(60, 3_380_000L, "中印国境(実効支配線)"),
        Milestone(61, 3_400_000L, "ボリビア・ブラジル国境"),
        Milestone(62, 3_485_000L, "モンゴル・ロシア国境"),
        Milestone(63, 3_500_000L, "アパラチアントレイル(米)"),
        Milestone(64, 3_530_000L, "ヴォルガ川(ヨーロッパ最長の川)"),
        Milestone(65, 3_645_000L, "中露国境"),
        Milestone(66, 4_000_000L, "オーストラリアの東西幅"),
        Milestone(67, 4_053_000L, "バングラデシュ・インド国境"),
        Milestone(68, 4_260_000L, "パシフィック・クレスト・トレイル(米)"),
        Milestone(69, 4_300_000L, "チリの南北の長さ(世界一縦長の国)"),
        Milestone(70, 4_667_000L, "コンゴ川"),
        Milestone(71, 4_677_000L, "中国・モンゴル国境"),
        Milestone(72, 4_989_000L, "コンチネンタル・ディバイド・トレイル(米)"),
        Milestone(73, 5_300_000L, "サハラ砂漠の東西幅"),
        Milestone(74, 5_308_000L, "アルゼンチン・チリ国境"),
        Milestone(75, 5_464_000L, "黄河"),
        Milestone(76, 5_500_000L, "カナダの東西幅(大西洋岸-太平洋岸)"),
        Milestone(77, 5_550_000L, "エニセイ川"),
        Milestone(78, 5_568_000L, "オビ川"),
        Milestone(79, 5_969_000L, "ミシシッピ川"),
        Milestone(80, 6_380_000L, "長江(中国最長の川)"),
        Milestone(81, 6_500_000L, "アマゾン川"),
        Milestone(82, 6_670_000L, "ナイル川(世界最長の川)"),
        Milestone(83, 6_846_000L, "カザフスタン・ロシア国境(世界2位の長さの国境)"),
        Milestone(84, 7_500_000L, "アンデス山脈(世界最長の山脈)"),
        Milestone(85, 7_600_000L, "ロッキー山脈の全長(広義)"),
        Milestone(86, 8_000_000L, "アフリカ大陸の南北の長さ"),
        Milestone(87, 8_891_000L, "アメリカ・カナダ国境(世界最長の国境線)"),
        Milestone(88, 9_000_000L, "ユーラシア大陸の最大東西幅"),
        Milestone(89, 9_290_000L, "シベリア鉄道(モスクワ-ウラジオストク)"),
        Milestone(90, 11_000_000L, "シベリアン・ハイウェイ(道路、サンクトペテルブルク-ウラジオストク)"),
        Milestone(91, 12_742_000L, "地球の直径"),
        Milestone(92, 14_500_000L, "オーストラリアのハイウェイ1(1つの国道番号として世界最長)"),
        Milestone(93, 15_332_000L, "世界最長の定期航空路線(シンガポール-ニューヨーク)"),
        Milestone(94, 15_962_000L, "北極線に沿った地球の周長(緯度66.5度)"),
        Milestone(95, 20_241_000L, "ロシアの陸上国境総延長"),
        Milestone(96, 21_196_000L, "万里の長城(全時代の累計、地球半周以上)"),
        Milestone(97, 22_117_000L, "中国の陸上国境総延長(世界最長)"),
        Milestone(98, 35_000_000L, "キョクアジサシの渡り(北極圏→南極圏、片道)"),
        Milestone(99, 35_786_000L, "静止衛星の軌道高度"),
        Milestone(
            index = 100,
            distanceMeters = Earth.CIRCUMFERENCE_METERS,
            name = "地球一周達成 🏁",
            isMajor = true,
            description = "赤道に沿って地球をぐるりと1周踏破!40,075km、あなたの足跡が地球を完全に包み込みました。",
        ),
    )

    private val byIndex: Map<Int, Milestone> = milestones.associateBy(Milestone::index)

    /** 番号(1〜100)で引く。範囲外は null。 */
    fun byIndex(index: Int): Milestone? = byIndex[index]

    /**
     * 累計距離 [meters] で到達済みとなるマイルストーンの件数。
     * 距離が昇順なので、そのまま「最後に達成した番号」でもある。
     */
    fun achievedCount(meters: Long): Int = milestones.count { it.distanceMeters <= meters }

    /** 累計距離 [meters] の次に来る未達成のマイルストーン。全達成済みなら null。 */
    fun nextAfter(meters: Long): Milestone? = milestones.firstOrNull { it.distanceMeters > meters }
}
