package com.nihongo.card

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Ai {
    private fun post(url: String, body: String, headers: Map<String, String>): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 60000
        c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("HTTP $code: ${text.take(300)}")
        return text
    }

    private suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        val key = Prefs.apiKey.trim()
        if (key.isEmpty()) throw Exception("API key belum diisi (buka Pengaturan)")
        if (Prefs.provider == "gemini") {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/${Prefs.model.trim().removePrefix("models/")}:generateContent"
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("responseMimeType", "application/json").apply {
                    val m = Prefs.model
                    if (m.contains("2.5") && m.contains("flash")) put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                    else if (m.contains("gemini-3")) put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                })
            val res = post(url, body.toString(), mapOf("x-goog-api-key" to key))
            JSONObject(res).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } else {
            val url = Prefs.baseUrl.trim().trimEnd('/') + "/chat/completions"
            val body = JSONObject().put("model", Prefs.model.trim())
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            val res = post(url, body.toString(), mapOf("Authorization" to "Bearer $key"))
            JSONObject(res).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun jsonArray(s: String): JSONArray {
        val a = s.indexOf('[')
        val b = s.lastIndexOf(']')
        if (a < 0 || b <= a) throw Exception("Respons AI bukan JSON array")
        return JSONArray(s.substring(a, b + 1))
    }

    suspend fun cards(kind: String, topic: String, n: Int, level: Level): List<Flash> {
        val jenis = if (kind == "KANJI") "kanji (satu karakter per kartu)" else "kosakata"
        val tp = if (topic.isBlank()) "" else " dengan topik \"$topic\""
        val prompt = "Buat $n kartu belajar bahasa Jepang jenis $jenis untuk level JLPT ${level.ai}$tp. " +
            "Balas HANYA JSON array berisi objek: {\"front\":\"tulisan Jepang\",\"reading\":\"hiragana + romaji\"," +
            "\"meaning\":\"arti bahasa Indonesia\",\"example\":\"satu kalimat contoh Jepang beserta terjemahan Indonesia\"," +
            "\"level\":\"N5/N4/N3/N2/N1\"}"
        val def = level.ns.first()
        val arr = jsonArray(ask(prompt))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val lv = o.optString("level").uppercase().let { if (it in Level.ALL.ns) it else def }
            Flash(o.getString("front"), o.optString("meaning"), o.optString("reading"), o.optString("example"), lv, kind)
        }
    }

    suspend fun questions(level: Level, n: Int): List<Question> {
        val prompt = "Buat $n soal pilihan ganda latihan JLPT level ${level.ai} (campur kosakata, kanji, tata bahasa). " +
            "Instruksi dan penjelasan dalam bahasa Indonesia. Balas HANYA JSON array: " +
            "[{\"q\":\"pertanyaan\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":0,\"explain\":\"penjelasan singkat\"}] " +
            "dengan answer = indeks (0-3) jawaban benar."
        val arr = jsonArray(ask(prompt))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val op = o.getJSONArray("options")
            val opts = (0 until op.length()).map { j -> op.getString(j) }
            Question(o.getString("q"), "", opts, o.getInt("answer").coerceIn(0, opts.size - 1), o.optString("explain"))
        }
    }
}
