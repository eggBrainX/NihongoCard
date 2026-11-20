package com.nihongo.card

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun rememberSpeak(): (String) -> Unit {
    val ctx = LocalContext.current
    val holder = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(ctx) { s -> if (s == TextToSpeech.SUCCESS) holder[0]?.language = Locale.JAPANESE }
        holder[0] = t
        onDispose { t.shutdown() }
    }
    return { text -> holder[0]?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nc") ; Unit }
}

private class Tile(val icon: String, val title: String, val sub: String, val route: String)

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val c = LocalNeo.current
    val pool = AppState.pool()
    val colors = listOf(c.main, c.pink, c.blue, c.green, c.pink, c.main)
    val tiles = listOf(
        Tile("🃏", "Kartu", "Balik & hafalkan", "study"),
        Tile("🎯", "Tebak", "Pilihan ganda", "quiz"),
        Tile("✍️", "Tulis", "Latihan menulis", "write"),
        Tile("🧩", "Cocokkan", "Game pasangan", "match"),
        Tile("📝", "Simulasi", "Ujian + timer", "sim"),
        Tile("✨", "Generate AI", "Kartu & soal baru", "ai")
    )
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("日本語カード", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = c.text)
                Text("Belajar Jepang, ringkas & seru", fontSize = 14.sp, color = c.text.copy(alpha = .7f))
            }
            NeoButton("⚙", { go("settings") }, bg = c.blue)
        }
        Label("Level")
        Chips { Level.values().forEach { NeoChip(it.label, AppState.level == it) { AppState.level = it } } }
        Label("Deck")
        Chips { Deck.values().forEach { NeoChip(it.label, AppState.deck == it) { AppState.deck = it } } }
        NeoCard(Modifier.fillMaxWidth(), bg = c.green) {
            val lv = if (AppState.deck == Deck.HIRA || AppState.deck == Deck.KATA) "" else " • ${AppState.level.label}"
            Text("${pool.size} kartu siap • ${AppState.deck.label}$lv", color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        tiles.chunked(2).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { k, t ->
                    NeoCard(Modifier.weight(1f).fillMaxHeight(), bg = colors[r * 2 + k], onClick = { go(t.route) }, center = true) {
                        Text(t.icon, fontSize = 34.sp)
                        Text(t.title, color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(t.sub, color = c.onAccent, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyScreen(back: () -> Unit) {
    val c = LocalNeo.current
    val speak = rememberSpeak()
    val queue = remember { mutableStateListOf<Flash>().apply { addAll(AppState.pool().shuffled()) } }
    val total = remember { queue.size }
    var flipped by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar("Kartu Belajar", back)
        if (total == 0) EmptyCard()
        else if (queue.isEmpty()) {
            NeoCard(Modifier.fillMaxWidth(), bg = c.green, center = true) {
                Text("🎉 Semua sudah hafal!", color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            NeoButton("Ulangi dari awal", { queue.addAll(AppState.pool().shuffled()) }, Modifier.fillMaxWidth())
        } else {
            val f = queue[0]
            Text("Hafal ${total - queue.size}/$total", color = c.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            NeoCard(Modifier.fillMaxWidth().weight(1f), bg = if (flipped) c.pink else c.main, onClick = { flipped = !flipped }, center = true) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val d = LocalDensity.current
                    val w = maxWidth
                    val h = maxHeight
                    fun fit(t: String, maxF: Float) =
                        with(d) { maxOf(minOf(h * maxF, w * 0.9f / t.length.coerceAtLeast(1)), 20.dp).toSp() }
                    if (!flipped) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(f.front, color = c.onAccent, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center, fontSize = fit(f.front, 0.5f))
                            Text("ketuk untuk membalik", color = c.onAccent, fontSize = 16.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(f.meaning, color = c.onAccent, fontWeight = jpWeight(f.meaning, FontWeight.Normal, FontWeight.Black),
                                fontSize = fit(f.meaning, 0.22f), textAlign = TextAlign.Center)
                            if (f.reading.isNotBlank()) Text(f.reading, color = c.onAccent, fontWeight = FontWeight.Normal,
                                fontSize = fit(f.reading, 0.12f), textAlign = TextAlign.Center)
                            if (f.example.isNotBlank()) Text(f.example, color = c.onAccent, fontSize = 18.sp, textAlign = TextAlign.Center)
                            Text(f.level, color = c.onAccent, fontSize = 16.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoButton("🔁 Belum", { queue.removeAt(0); queue.add(f); flipped = false }, Modifier.weight(1f), c.red)
                NeoButton("🔊", { speak(f.front) }, bg = c.blue)
                NeoButton("✓ Hafal", { queue.removeAt(0); flipped = false }, Modifier.weight(1f), c.green)
            }
        }
    }
}

@Composable
fun QuizScreen(sim: Boolean, back: () -> Unit) {
    val c = LocalNeo.current
    val scope = rememberCoroutineScope()
    var qs by remember { mutableStateOf<List<Question>?>(null) }
    var dir by remember { mutableIntStateOf(2) }
    var count by remember { mutableIntStateOf(10) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    val pool = AppState.pool()
    val cur = qs
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar(if (sim) "Simulasi Latihan" else "Tebak", if (cur != null) ({ qs = null }) else back)
        if (cur != null) {
            QuizRunner(cur, if (sim) cur.size * 20 else 0) { qs = null }
        } else Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Label("Jumlah soal")
            Chips { (if (sim) listOf(10, 20, 30) else listOf(5, 10, 20)).forEach { NeoChip("$it", count == it) { count = it } } }
            if (!sim) {
                Label("Arah")
                Chips {
                    listOf("Karakter→Arti", "Arti→Karakter", "Campur").forEachIndexed { i, s -> NeoChip(s, dir == i) { dir = i } }
                }
            } else Text("Waktu: 20 detik per soal. Soal campur dari kartu terpilih.", color = c.text, fontSize = 14.sp)
            if (pool.size < 4) EmptyCard()
            NeoButton("▶ Mulai dari kartu (${AppState.deck.label})", {
                qs = makeQuestions(pool, count, if (sim) 2 else dir)
            }, Modifier.fillMaxWidth().weight(1f).heightIn(max = 180.dp), c.green, enabled = pool.size >= 4, fontSize = 20.sp)
            NeoButton(if (loading) "⏳ Membuat soal… (ketuk untuk batal)" else "✨ Contoh soal AI (${AppState.level.label})", {
                if (loading) { job?.cancel(); loading = false }
                else {
                    loading = true; err = ""
                    job = scope.launch {
                        try { qs = Ai.questions(AppState.level, count) }
                        catch (e: CancellationException) { throw e }
                        catch (e: Throwable) { err = e.message ?: "Gagal" }
                        loading = false
                    }
                }
            }, Modifier.fillMaxWidth().weight(1f).heightIn(max = 180.dp), c.pink, fontSize = 20.sp)
            if (err.isNotBlank()) NeoCard(Modifier.fillMaxWidth(), bg = c.red) { Text(err, color = c.onAccent, fontSize = 14.sp) }
        }
    }
}

@Composable
fun WriteScreen(back: () -> Unit) {
    val c = LocalNeo.current
    val pool = remember { AppState.pool().shuffled() }
    var i by remember { mutableIntStateOf(0) }
    var guide by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar("Latihan Menulis", back)
        if (pool.isEmpty()) { EmptyCard(); return@Column }
        val f = pool[i % pool.size]
        NeoCard(Modifier.fillMaxWidth(), bg = c.main, center = true) {
            Text("Tulis: ${f.meaning}", color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
            if (f.reading.isNotBlank()) Text(f.reading, color = c.onAccent, fontSize = 14.sp)
            if (reveal) Text(f.front, color = c.onAccent, fontWeight = FontWeight.Normal, fontSize = 64.sp)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).neo(c.surface)
                .pointerInput(i) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { ch, _ -> ch.consume(); current = current + ch.position },
                        onDragEnd = { strokes.add(current); current = emptyList() },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            if (guide) BoxWithConstraints(Modifier.fillMaxSize()) {
                val gs = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.8f / f.front.length.coerceAtLeast(1)).toSp() }
                Text(f.front, fontSize = gs, fontWeight = FontWeight.Normal, color = c.text.copy(alpha = .15f), modifier = Modifier.align(Alignment.Center))
            }
            Canvas(Modifier.fillMaxSize()) {
                (strokes.toList() + listOf(current)).forEach { pts ->
                    if (pts.size > 1) {
                        val p = Path()
                        p.moveTo(pts[0].x, pts[0].y)
                        pts.drop(1).forEach { p.lineTo(it.x, it.y) }
                        drawPath(p, c.text, style = Stroke(10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoButton("🗑", { strokes.clear(); current = emptyList() }, bg = c.red)
            NeoButton(if (guide) "Panduan ✓" else "Panduan", { guide = !guide }, Modifier.weight(1f), c.blue)
            NeoButton("Jawaban", { reveal = true }, Modifier.weight(1f), c.pink)
            NeoButton("Lanjut ▶", { i++; reveal = false; strokes.clear(); current = emptyList() }, Modifier.weight(1f), c.green)
        }
    }
}

@Composable
fun MatchScreen(back: () -> Unit) {
    val c = LocalNeo.current
    var round by remember { mutableIntStateOf(0) }
    val cards = remember(round) { AppState.pool().shuffled().take(6) }
    val left = remember(round) { cards.shuffled() }
    val right = remember(round) { cards.shuffled() }
    val done = remember(round) { mutableStateListOf<Flash>() }
    var selL by remember(round) { mutableStateOf<Flash?>(null) }
    var selR by remember(round) { mutableStateOf<Flash?>(null) }
    var miss by remember(round) { mutableIntStateOf(0) }
    LaunchedEffect(selL, selR) {
        val l = selL
        val r = selR
        if (l != null && r != null) {
            if (l == r) done.add(l) else { miss++; delay(250) }
            selL = null; selR = null
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar("Cocokkan", back)
        if (cards.size < 2) EmptyCard()
        else {
            Text("Pasangkan karakter dengan artinya • salah: $miss", color = c.text, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    left.forEach { f ->
                        NeoButton(f.front, { if (f !in done) selL = f }, Modifier.fillMaxWidth().weight(1f),
                            when { f in done -> c.green; f == selL -> c.main; else -> c.surface },
                            fontSize = 20.sp, fit = f.front.length <= 4)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    right.forEach { f ->
                        NeoButton(f.meaning, { if (f !in done) selR = f }, Modifier.fillMaxWidth().weight(1f),
                            when { f in done -> c.green; f == selR -> c.main; else -> c.surface },
                            fontSize = 17.sp, fit = f.meaning.length <= 6)
                    }
                }
            }
            if (done.size == cards.size) {
                NeoCard(Modifier.fillMaxWidth(), bg = c.green, center = true) {
                    Text("🎉 Selesai! Salah $miss kali", color = c.onAccent, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                NeoButton("Main lagi", { round++ }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun AiScreen(back: () -> Unit) {
    val c = LocalNeo.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("VOCAB") }
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(10) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    var result by remember { mutableStateOf<List<Flash>>(emptyList()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar("Generate AI", back)
        if (Prefs.apiKey.isBlank()) NeoCard(Modifier.fillMaxWidth(), bg = c.red) {
            Text("API key belum diisi. Buka ⚙ Pengaturan di Home.", color = c.onAccent, fontSize = 13.sp)
        }
        Label("Jenis kartu")
        Chips { NeoChip("Kosakata", kind == "VOCAB") { kind = "VOCAB" }; NeoChip("Kanji", kind == "KANJI") { kind = "KANJI" } }
        Label("Level: ${AppState.level.label} (ubah di Home)")
        Label("Topik (opsional)")
        NeoField(topic, { topic = it }, "mis. makanan, sekolah, kerja", Modifier.fillMaxWidth())
        Label("Jumlah")
        Chips { listOf(5, 10, 15).forEach { NeoChip("$it", count == it) { count = it } } }
        NeoButton(if (loading) "⏳ Memproses… (ketuk untuk batal)" else "✨ Generate", {
            if (loading) { job?.cancel(); loading = false }
            else {
                loading = true; err = ""
                job = scope.launch {
                    try { result = Ai.cards(kind, topic, count, AppState.level) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Throwable) { err = e.message ?: "Gagal" }
                    loading = false
                }
            }
        }, Modifier.fillMaxWidth())
        if (err.isNotBlank()) NeoCard(Modifier.fillMaxWidth(), bg = c.red) { Text(err, color = c.onAccent, fontSize = 14.sp) }
        if (result.isNotEmpty()) {
            result.forEach {
                NeoCard(Modifier.fillMaxWidth()) {
                    Text("${it.front}  ${it.reading}", color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${it.meaning} • ${it.level}", color = c.text, fontSize = 13.sp)
                    if (it.example.isNotBlank()) Text(it.example, color = c.text.copy(alpha = .7f), fontSize = 14.sp)
                }
            }
            NeoButton("💾 Simpan ${result.size} kartu", {
                Prefs.cards.addAll(result); Prefs.save(); result = emptyList()
            }, Modifier.fillMaxWidth(), c.green)
        }
        Text("Tersimpan: ${Prefs.cards.size} kartu AI (pilih deck Kanji / Kosakata di Home)", color = c.text, fontSize = 14.sp)
        if (Prefs.cards.isNotEmpty()) NeoButton("Hapus semua kartu AI", { Prefs.cards.clear(); Prefs.save() }, Modifier.fillMaxWidth(), c.red)
    }
}

@Composable
fun SettingsScreen(back: () -> Unit) {
    val c = LocalNeo.current
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    var testMsg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TopBar("Pengaturan", back)
        Label("Penyedia AI")
        Chips {
            NeoChip("Gemini", Prefs.provider == "gemini") {
                Prefs.provider = "gemini"
                if (!Prefs.model.startsWith("gemini")) Prefs.model = "gemini-3.8-flash"
                Prefs.save()
            }
            NeoChip("OpenAI-compatible", Prefs.provider == "openai") {
                Prefs.provider = "openai"
                if (Prefs.model.startsWith("gemini")) Prefs.model = "gpt-4o-mini"
                Prefs.save()
            }
        }
        Label("API key")
        NeoField(Prefs.apiKey, { Prefs.apiKey = it; Prefs.save() }, "tempel API key", Modifier.fillMaxWidth(), password = true)
        Label("Model")
        NeoField(Prefs.model, { Prefs.model = it; Prefs.save() }, "nama model", Modifier.fillMaxWidth())
        if (Prefs.provider == "openai") {
            Label("Base URL")
            NeoField(Prefs.baseUrl, { Prefs.baseUrl = it; Prefs.save() }, "https://.../v1", Modifier.fillMaxWidth())
            Text("Contoh: OpenRouter https://openrouter.ai/api/v1 • Groq https://api.groq.com/openai/v1 • OpenAI https://api.openai.com/v1",
                color = c.text.copy(alpha = .7f), fontSize = 14.sp)
        } else Text("API key Gemini gratis: aistudio.google.com", color = c.text.copy(alpha = .7f), fontSize = 14.sp)
        NeoButton(if (testing) "Menguji…" else "🔌 Tes koneksi API", {
            if (!testing) {
                testing = true; testMsg = ""
                scope.launch {
                    testMsg = try { "✓ Berhasil: " + Ai.ping() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Throwable) { "✗ " + (e.message ?: "Gagal") }
                    testing = false
                }
            }
        }, Modifier.fillMaxWidth(), c.green, enabled = !testing)
        if (testMsg.isNotBlank()) NeoCard(Modifier.fillMaxWidth(), bg = if (testMsg.startsWith("✓")) c.green else c.red) {
            Text(testMsg, color = c.onAccent, fontSize = 14.sp)
        }
        Label("Tema")
        Chips { listOf("Sistem", "Terang", "Gelap").forEachIndexed { i, s -> NeoChip(s, Prefs.theme == i) { Prefs.theme = i; Prefs.save() } } }
        Text("API key disimpan lokal di perangkat.", color = c.text.copy(alpha = .7f), fontSize = 14.sp)
    }
}
