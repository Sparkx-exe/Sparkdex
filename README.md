<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
<img src="https://img.shields.io/badge/API-MangaDex-E7484F?style=for-the-badge" />
<img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />

<br /><br />

# ⚡ Sparkdex

### A high-fidelity Android manga reader — fast, offline-first, and beautifully dark.

Powered by the [MangaDex API](https://api.mangadex.org/docs/) · Built with Jetpack Compose · 100% Kotlin

<br />

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| 📖 **Seamless Reader** | Rich chapter reader with instant page loads and pre-fetch support |
| 🔍 **Smart Search & Filter** | Find manga by title, tags, author, and genres |
| 📦 **Offline Library** | Bookmark manga and keep your reading history stored locally |
| 🌑 **Native Dark Mode** | Fully immersive dark theme — easy on the eyes, beautiful by design |
| ⚡ **120Hz Smooth** | Unlocked high-refresh-rate support for buttery transitions |
| 🏛️ **Material 3 UI** | Modern, declarative UI following the latest Material You guidelines |

---

## 🛠️ Tech Stack & Architecture

```
Sparkdex
├── UI Layer          → Jetpack Compose + Material 3
├── Architecture      → MVVM (ViewModel + StateFlow)
├── Networking        → Retrofit + OkHttp + Moshi
├── Local Storage     → Room Database (offline-first)
├── Image Loading     → Coil
├── Async             → Kotlin Coroutines + Flow
└── Navigation        → Navigation Compose (type-safe routing)
```

| Layer | Technology |
|---|---|
| Language | Kotlin 100% |
| UI Toolkit | Jetpack Compose |
| Architecture | MVVM |
| Network | Retrofit + OkHttp |
| JSON Parsing | Moshi |
| Local DB | Room |
| Image Loading | Coil |
| Async / State | Coroutines + Flow |
| Navigation | Navigation Compose |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio **Giraffe** or newer
- Android device or emulator running **API 24+** (Target: API 36)
- A valid [MangaDex API](https://api.mangadex.org/docs/) key (see `.env.example`)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/Sparkx-exe/Sparkdex.git

# 2. Open in Android Studio
# File → Open → select the cloned folder

# 3. Set up your environment
cp .env.example .env
# Fill in your API credentials in .env

# 4. Let Gradle sync dependencies automatically
# (libs.versions.toml manages all versions)

# 5. Run on your device or emulator
# Click ▶ Run or use Shift + F10
```

---

## 📁 Project Structure

```
Sparkdex/
├── app/
│   ├── src/main/
│   │   ├── ui/           # Compose screens & components
│   │   ├── viewmodel/    # MVVM ViewModels
│   │   ├── data/         # Repositories, Room DB, Retrofit
│   │   ├── model/        # Data models
│   │   └── navigation/   # Nav graph & routes
│   └── build.gradle.kts
├── assets/
├── gradle/
├── .env.example
└── README.md
```

---

## 📸 Screenshots

> https://github.com/Sparkx-exe/Sparkdex/tree/dca5bafd73bab95b0b2f84237e71d9469e24d9df/Images

---

## 🌐 API

All manga data and cover images are sourced from the **MangaDex** public API.
This app operates strictly within MangaDex's API usage guidelines and terms of service.

→ [MangaDex API Docs](https://api.mangadex.org/docs/)

---

## 🗺️ Roadmap

- [ ] Download chapters for true offline reading
- [ ] Reading mode customization (webtoon / RTL / LTR)
- [ ] Custom reading themes and font size controls
- [ ] Push notifications for chapter updates
- [ ] MAL / AniList integration

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve Sparkdex:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to your branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [Sparkx-exe](https://github.com/Sparkx-exe)

⭐ If you like Sparkdex, consider giving it a star!

</div>
