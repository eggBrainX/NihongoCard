package com.nihongo.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Question(
    val prompt: String, val sub: String, val options: List<String>, val answer: Int, val explain: String = ""
)

private fun opts(correct: String, all: List<String>): Pair<List<String>, Int> {
    val list = (all.filter { it != correct }.distinct().shuffled().take(3) + correct).shuffled()
    return list to list.indexOf(correct)
}

/** dir: 0 karakter->arti, 1 arti->karakter, 2 campur (termasuk bacaan) */
fun makeQuestions(pool: List<Flash>, n: Int, dir: Int): List<Question> = pool.shuffled().take(n).map { f ->
    val readable = f.reading.isNotBlank() && pool.map { it.reading }.distinct().size >= 4
    val kind = when (dir) {
        0 -> 0
        1 -> 1
        else -> if (readable && Random.nextInt(3) == 0) 2 else Random.nextInt(2)
    }
    val info = "${f.front} = ${f.meaning}" + if (f.reading.isNotBlank()) " (${f.reading})" else ""
    when (kind) {
        0 -> { val (o, a) = opts(f.meaning, pool.map { it.meaning }); Question(f.front, "Apa arti / romaji-nya?", o, a, info) }
        1 -> { val (o, a) = opts(f.front, pool.map { it.front }); Question(f.meaning, "Pilih karakter yang tepat", o, a, info) }
        else -> { val (o, a) = opts(f.reading, pool.map { it.reading }.filter { it.isNotBlank() }); Question(f.front, "Bagaimana bacaannya?", o, a, info) }
    }
}

@Composable
fun QuizRunner(qs: List<Question>, timeSec: Int, onExit: () -> Unit) {
    val c = LocalNeo.current
    var idx by remember { mutableIntStateOf(0) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(timeSec) }
    val wrong = remember { mutableStateListOf<Question>() }

    LaunchedEffect(Unit) {
        if (timeSec > 0) {
            while (left > 0 && !done) { delay(1000); left-- }
            if (!done) done = true
        }
    }

    if (done) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val pct = if (qs.isEmpty()) 0 else score * 100 / qs.size
            NeoCard(Modifier.fillMaxWidth(), bg = c.main, center = true) {
                Text("Skor", color = c.onAccent, fontSize = 14.sp)
                Text("$score / ${qs.size}", color = c.onAccent, fontSize = 44.sp, fontWeight = FontWeight.Black)
                Text(if (pct >= 80) "Mantap! 🔥" else if (pct >= 50) "Lumayan, terus latihan 💪" else "Ayo ulangi lagi 📖", color = c.onAccent)
            }
            if (wrong.isNotEmpty()) {
                Label("Perlu diulang")
                wrong.forEach {
                    NeoCard(Modifier.fillMaxWidth()) {
                        Text(it.prompt, color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Jawaban: ${it.options[it.answer]}", color = c.text, fontSize = 13.sp)
                        if (it.explain.isNotBlank()) Text(it.explain, color = c.text.copy(alpha = .7f), fontSize = 12.sp)
                    }
                }
            }
            NeoButton("Selesai", onExit, Modifier.fillMaxWidth())
        }
    } else {
        val q = qs[idx]
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Soal ${idx + 1}/${qs.size}   ✓ $score", color = c.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (timeSec > 0) Text("⏱ %d:%02d".format(left / 60, left % 60), color = c.text, fontWeight = FontWeight.Bold)
            }
            NeoCard(Modifier.fillMaxWidth(), bg = c.main, center = true) {
                Text(
                    q.prompt, color = c.onAccent, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                    fontSize = if (q.prompt.length <= 4) 56.sp else if (q.prompt.length <= 10) 28.sp else 19.sp
                )
                if (q.sub.isNotBlank()) Text(q.sub, color = c.onAccent, fontSize = 12.sp)
            }
            q.options.forEachIndexed { i, o ->
                val bg = when {
                    picked < 0 -> c.surface
                    i == q.answer -> c.green
                    i == picked -> c.red
                    else -> c.surface
                }
                NeoButton(
                    o, { if (picked < 0) { picked = i; if (i == q.answer) score++ else wrong.add(q) } },
                    Modifier.fillMaxWidth(), bg, fontSize = if (o.length <= 3) 24.sp else 15.sp
                )
            }
            if (picked >= 0) {
                if (q.explain.isNotBlank()) NeoCard(Modifier.fillMaxWidth(), bg = c.blue) {
                    Text(q.explain, color = c.onAccent, fontSize = 13.sp)
                }
                NeoButton(if (idx + 1 < qs.size) "Lanjut →" else "Lihat hasil", {
                    if (idx + 1 < qs.size) { idx++; picked = -1 } else done = true
                }, Modifier.fillMaxWidth(), c.pink)
            }
        }
    }
}
