package com.nihongo.card

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

data class Flash(
    val front: String, val meaning: String, val reading: String = "",
    val example: String = "", val level: String = "N5", val type: String = "HIRA"
)

enum class Deck(val label: String) {
    HIRA("あ Hiragana"), KATA("ア Katakana"), KANJI("漢 Kanji"), VOCAB("語 Kosakata")
}

enum class Level(val label: String, val ns: List<String>, val ai: String) {
    ALL("Semua", listOf("N5", "N4", "N3", "N2", "N1"), "N5 sampai N1 (campur)"),
    EASY("Mudah", listOf("N5", "N4"), "N5-N4"),
    MID("Sedang", listOf("N3"), "N3"),
    HARD("Sulit", listOf("N2", "N1"), "N2-N1"),
    N5("N5", listOf("N5"), "N5"), N4("N4", listOf("N4"), "N4"), N3("N3", listOf("N3"), "N3"),
    N2("N2", listOf("N2"), "N2"), N1("N1", listOf("N1"), "N1")
}

private const val KANA = "あ a い i う u え e お o か ka き ki く ku け ke こ ko さ sa し shi す su せ se そ so " +
    "た ta ち chi つ tsu て te と to な na に ni ぬ nu ね ne の no は ha ひ hi ふ fu へ he ほ ho " +
    "ま ma み mi む mu め me も mo や ya ゆ yu よ yo ら ra り ri る ru れ re ろ ro わ wa を wo ん n " +
    "が ga ぎ gi ぐ gu げ ge ご go ざ za じ ji ず zu ぜ ze ぞ zo だ da で de ど do " +
    "ば ba び bi ぶ bu べ be ぼ bo ぱ pa ぴ pi ぷ pu ぺ pe ぽ po"

private fun kana(kata: Boolean): List<Flash> = KANA.split(" ").chunked(2).map { (k, r) ->
    val ch = if (kata) (k[0].code + 0x60).toChar().toString() else k
    Flash(ch, r, type = if (kata) "KATA" else "HIRA")
}

private val HIRA_CARDS by lazy { kana(false) }
private val KATA_CARDS by lazy { kana(true) }

// format: kanji|arti|bacaan|level
private val KANJI_RAW = """
日|hari, matahari|ニチ・ひ|N5
月|bulan|ゲツ・つき|N5
火|api|カ・ひ|N5
水|air|スイ・みず|N5
木|pohon, kayu|モク・き|N5
金|emas, uang|キン・かね|N5
土|tanah|ド・つち|N5
人|orang|ジン・ひと|N5
山|gunung|サン・やま|N5
川|sungai|セン・かわ|N5
大|besar|ダイ・おおきい|N5
小|kecil|ショウ・ちいさい|N5
中|tengah, dalam|チュウ・なか|N5
上|atas|ジョウ・うえ|N5
下|bawah|カ・した|N5
学|belajar|ガク・まなぶ|N5
先|dahulu, sebelumnya|セン・さき|N5
生|hidup, lahir|セイ・いきる|N5
食|makan|ショク・たべる|N5
見|melihat|ケン・みる|N5
行|pergi|コウ・いく|N5
本|buku, asal|ホン・もと|N5
車|mobil, kendaraan|シャ・くるま|N5
休|istirahat|キュウ・やすむ|N5
会|bertemu|カイ・あう|N4
家|rumah, keluarga|カ・いえ|N4
病|sakit|ビョウ・やまい|N4
運|mengangkut, nasib|ウン・はこぶ|N4
悪|buruk, jahat|アク・わるい|N4
医|kedokteran|イ|N4
理|logika, alasan|リ|N4
政|politik|セイ|N3
経|melewati, ekonomi|ケイ・へる|N3
議|diskusi|ギ|N3
協|kerja sama|キョウ|N3
憲|konstitusi|ケン|N2
維|serat, mempertahankan|イ|N2
憂|sedih, cemas|ユウ・うれい|N1
顕|tampak jelas|ケン|N1
""".trimIndent().lines().filter { it.isNotBlank() }.map {
    val p = it.split("|")
    Flash(p[0], p[1], p[2], level = p[3], type = "KANJI")
}

object AppState {
    var deck by mutableStateOf(Deck.HIRA)
    var level by mutableStateOf(Level.ALL)

    fun pool(): List<Flash> = when (deck) {
        Deck.HIRA -> HIRA_CARDS
        Deck.KATA -> KATA_CARDS
        Deck.KANJI -> (KANJI_RAW + Prefs.cards.filter { it.type == "KANJI" }).filter { it.level in level.ns }
        Deck.VOCAB -> Prefs.cards.filter { it.type == "VOCAB" && it.level in level.ns }
    }
}

object Prefs {
    private lateinit var sp: SharedPreferences
    var provider by mutableStateOf("gemini")
    var apiKey by mutableStateOf("")
    var model by mutableStateOf("gemini-2.5-flash")
    var baseUrl by mutableStateOf("https://openrouter.ai/api/v1")
    var theme by mutableIntStateOf(0) // 0 sistem, 1 terang, 2 gelap
    val cards = mutableStateListOf<Flash>()

    fun init(c: Context) {
        sp = c.getSharedPreferences("nihongo", Context.MODE_PRIVATE)
        provider = sp.getString("provider", provider) ?: provider
        apiKey = sp.getString("apiKey", "") ?: ""
        model = sp.getString("model", model) ?: model
        baseUrl = sp.getString("baseUrl", baseUrl) ?: baseUrl
        theme = sp.getInt("theme", 0)
        cards.clear()
        runCatching {
            val a = JSONArray(sp.getString("cards", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                cards.add(Flash(o.getString("front"), o.getString("meaning"), o.optString("reading"),
                    o.optString("example"), o.optString("level", "N5"), o.optString("type", "VOCAB")))
            }
        }
    }

    fun save() {
        val a = JSONArray()
        cards.forEach {
            a.put(JSONObject().put("front", it.front).put("meaning", it.meaning).put("reading", it.reading)
                .put("example", it.example).put("level", it.level).put("type", it.type))
        }
        sp.edit().putString("provider", provider).putString("apiKey", apiKey).putString("model", model)
            .putString("baseUrl", baseUrl).putInt("theme", theme).putString("cards", a.toString()).apply()
    }
}
