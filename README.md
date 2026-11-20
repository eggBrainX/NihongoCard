# Nihongo Card
Flashcard belajar bahasa Jepang (Kotlin + Jetpack Compose, tema neobrutalism).

- Deck: Hiragana, Katakana, Kanji (contoh N5-N1), Kosakata (dari AI)
- Mode: Kartu, Tebak, Tulis (canvas), Cocokkan, Simulasi (timer), Generate kartu & soal via AI
- Level: Mudah (N5-N4) / Sedang (N3) / Sulit (N2-N1) atau N5..N1
- AI: Gemini atau API OpenAI-compatible (OpenRouter, Groq, dll). Isi API key di Pengaturan.

## Build
- Android Studio: Open folder ini, Sync, Run. (Android Studio membuat gradle wrapper otomatis.)
- GitHub Actions: push ke `main`, APK ada di tab Actions > Artifacts.

## Push dari Termux
```
git init && git add . && git commit -m "init"
git branch -M main
git remote add origin https://github.com/USER/NihongoCard.git
git push -u origin main
```
