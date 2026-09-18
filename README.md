<div align="center">

# ⚡ VELORIX TOURNAMENTS
### *Next-Generation Esports Engine & Competitive Gaming Platform for Android*

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2012%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Realtime-Firebase%20RTDB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase" />
  <img src="https://img.shields.io/badge/AI%20Ops-Google%20Gemini-8E75B2?style=for-the-badge&logo=google&logoColor=white" alt="Gemini AI" />
</p>

<p align="center">
  <b>Dynamic Battle Royale & Clash Squad Matchmaking • Real-Time Slot Reservation • Anti-Cheat AI Watchdog</b>
</p>

---

</div>

## 📌 Executive Summary

**VeloRix Tournaments** is a high-performance native Android esports platform engineered for mobile gamers across titles like **Free Fire** and **BGMI**. Built with strict adherence to **Material Design 3 (M3)**, modern **Clean Architecture**, and **reactive state streaming (Coroutines + Flow)**, VeloRix provides sub-second tournament slot booking, automated room credential reveals, live leaderboard tracking, and continuous 24/7 backend operations powered by an autonomous **Gemini AI Watchdog Bot**.

---

## 🏛️ System Architecture

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                            │
│           Jetpack Compose 60fps UI • Material 3 Theming • MVI/MVVM      │
│  [HomeScreen]    [TournamentDetail]    [Leaderboard]    [GamerProfile]  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ (StateFlow / UI Events)
┌────────────────────────────────────▼────────────────────────────────────┐
│                              DOMAIN LAYER                               │
│      Use Cases • Business Rules • Rate Limiters • Concurrency Mutex     │
│   • MatchSlotReservationUseCase        • RoomCredentialRevealUseCase    │
│   • LeaderboardRankCalculationUseCase  • FairPlayReportUseCase          │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ (Repository Interfaces)
┌────────────────────────────────────▼────────────────────────────────────┐
│                               DATA LAYER                                │
│   • Firebase Realtime Database (Sub-second socket sync)                 │
│   • Firebase Authentication (Google One-Tap + Phone OTP)                │
│   • Local Room Database Cache (Offline persistence fallback)            │
│   • 24/7 Autonomous Node.js Watchdog Daemon (Gemini 3.6 Flash)          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✨ Core Pillars & Capabilities

### 🎮 1. Real-Time Tournament Matchmaking
- **Multiple Game Modes:** Battle Royale (Solo, Duo, Squad) & Clash Squad (4v4, 6v6).
- **Concurrency-Safe Slot Booking:** Atomic reservation prevents race conditions when hundreds of players join the same lobby simultaneously.
- **Dynamic Credentials Vault:** Room ID & Password stay securely locked until T-15 minutes before match kickoff, unlocking only for verified registered players.

### 🏆 2. Live Competitive Leaderboards & Ranks
- **Sub-Second Socket Updates:** Real-time point calculations across K/D ratios, match placements, and tournament streaks.
- **Dynamic Tiering:** Progression engine from *Bronze* to *Esports Elite* with custom vector badge assets.

### 🛡️ 3. Fair-Play & Automated AI Watchdog
- **In-App Dispute Reporting:** Players can report malicious behavior, suspected emulator abuse, or cheating with contextual match logs.
- **24/7 Telegram Operations Bot:** Powered by **Google Gemini 3.6 Flash**, continuously monitoring lobby health, unassigned room IDs, and live player disputes.

---

## 🛠️ Engineering Stack

| Component | Technical Selection | Rationale |
|---|---|---|
| **Language** | Kotlin 2.0+ | Strict type safety, structured concurrency, and modern DSL features. |
| **UI Toolkit** | Jetpack Compose (M3) | Declarative UI, zero XML overhead, dynamic edge-to-edge rendering. |
| **Asynchronous Engine** | Coroutines & StateFlow | Reactive, leak-free data streaming from backend to Composable tree. |
| **Backend & Sync** | Firebase Realtime Database | Persistent low-latency WebSocket connection for instantaneous updates. |
| **Identity & Auth** | Google Identity + Phone OTP | Frictionless one-tap onboarding for mobile gaming demographics. |
| **AI Moderation** | Google Gemini 3.6 Flash | Natural language dispute assessment and real-time operational telemetry. |
| **Build System** | Gradle Kotlin DSL (`.gradle.kts`) | Centralized Version Catalog (`libs.versions.toml`) with strict dependency pinning. |

---

## 📁 Repository Structure

```text
velorix-tournaments/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/             # Repositories, Firebase sockets & local Room DB
│   │   ├── domain/           # Models, business invariants, and validators
│   │   ├── ui/               # Jetpack Compose screens, components & viewmodels
│   │   │   ├── screens/      # HomeScreen, TournamentScreen, LeaderboardScreen, etc.
│   │   │   ├── components/   # Modular UI widgets, dialogs & bottom bars
│   │   │   └── theme/        # Material 3 ColorScheme, Typography & Shapes
│   │   └── util/             # Rate limiters, haptic engines & network observers
│   └── google-services.json  # Firebase configuration
├── velorix-watchdog-bot/      # 24/7 Node.js + Gemini AI Telegram Ops Daemon
├── database.rules.json       # Firebase Realtime Database security rules
└── README.md                 # Project technical documentation
```

---

## 🚀 Development & Build Instructions

### Prerequisites
- **Android Studio** Ladybug (2024.2.1+) or Meerkat
- **JDK 17** (LTS)
- **Android SDK** API Level 35 (`compileSdk = 35`, `minSdk = 26`)

### Setup & Compilation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/VX-ANANT/velorix-tournaments.git
   cd velorix-tournaments
   ```

2. **Configure Firebase:**
   Place your project's `google-services.json` inside the `/app` directory.

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Launch AI Watchdog Bot (Optional):**
   ```bash
   cd velorix-watchdog-bot
   cp .env.example .env
   # Populate TELEGRAM_BOT_TOKEN and GEMINI_API_KEY
   npm install
   npm start
   ```

---

## 🔒 Security & Fair-Play Protocols

- **Row-Level RTDB Rules:** Users can only modify their own profile data; tournament slots and match points require authorized service writes.
- **Client-Side Anti-Spam:** Action cooldowns and request mutexes prevent double-submission during network spikes.
- **Environment Isolation:** Sensitive credentials and API tokens are decoupled from source control via secret management pipelines.

---

<div align="center">

<sub>Engineered with precision for the mobile esports ecosystem.</sub><br>
<sub>Maintained by <b><a href="https://github.com/VX-ANANT">VX-ANANT</a></b></sub>

</div>
