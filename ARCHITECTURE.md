# VELORIX KERNEL TOPOLOGY & CYBERNETIC ESPORTS ARCHITECTURE

> **Platform:** Native Android 14 / 15 (API 34–35)  
> **Core Language:** Kotlin 2.0  
> **UI Paradigm:** Declarative Jetpack Compose (Material Design 3, Pure AMOLED Dark)  
> **Architecture Pattern:** Clean Architecture + Unidirectional Data Flow (MVI / Reactive MVVM)  
> **Persistence & Sync:** Room SQLite (Compile-Time Verified) + Firebase Realtime Database  
> **Autonomous Sentinel:** Node.js Heuristic Watchdog Bot powered by Google Gemini 3.6 Flash  
> **Engineering Principal:** [VX-ANANT](https://github.com/VX-ANANT/Velorix-tournaments)

---

## 1. High-Level Architectural Topology

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                    PRESENTATION TIER                                   │
│                        120 FPS Declarative Composables • AMOLED Dark                   │
│  [AuthScreen]      [TournamentHub]     [MatchDetailsScreen]    [LeaderboardViewport]   │
│  [WalletDashboard] [ProfileScreen]     [NotificationCenter]    [AboutScreen]           │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Unidirectional Immutable UI States / Events
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                                     VIEWMODEL LAYER                                    │
│                 StateFlow Pipeline • Coroutine Dispatchers (Main/IO/Default)           │
│  • PlatformViewModel (Consolidated Single Source of Truth for Realtime Ingestion)       │
│  • Mutation Mutexes • Rate-Limiting Semaphores • Offline Optimistic Updates            │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Domain Contracts & Repository Interfaces
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                                   DOMAIN & BUSINESS LOGIC                              │
│  • AtomicSlotReservationMutex            • TimeGatedCredentialRevelationEngine         │
│  • StandingsReductionAlgorithm           • DuplicateRegistrationConstraintValidator    │
│  • Anti-Tamper State Verification        • UPI / Wallet Transaction Mutex Engine        │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Data Sources & Network Boundaries
┌───────────────────────────────────────────▼────────────────────────────────────────────┐
│                                PERSISTENCE & CLOUD SERVICES                            │
│  ┌─────────────────────────┐  ┌──────────────────────────┐  ┌───────────────────────┐  │
│  │     Local Room SQLite   │  │   Firebase Realtime DB   │  │ Firebase Auth / Phone │  │
│  │  • Offline Cache        │  │  • Sub-15ms WebSocket    │  │  • OAuth 2.0 Creds    │  │
│  │  • User Profile & Wallet│  │  • Live Slot Synchronizer│  │  • Identity Assertion │  │
│  └─────────────────────────┘  └──────────────────────────┘  └───────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │            Autonomous Watchdog Subsystem (Node.js + Gemini 3.6 Flash)            │  │
│  │  • Screenshot OCR & Standings Verification • Realtime Telemetry Monitoring       │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technical Stack Breakdown

| Subsystem | Technology | Architectural Justification |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.x | Modern functional paradigms, strict null-safety, and inline value classes. |
| **UI Framework** | Jetpack Compose + M3 | Zero-Jank declarative reactive canvas; eliminates fragile legacy XML view hierarchies. |
| **Asynchronous Engine** | Coroutines & Flow | Structured concurrency (`StateFlow`, `SharedFlow`, non-blocking I/O dispatchers). |
| **Local Persistence** | AndroidX Room DB | Embedded ACID-compliant SQLite with compile-time query verification and offline-first cache. |
| **Realtime Sync** | Firebase Realtime Database | Sub-15ms WebSocket state fan-out across concurrent esports tournament lobbies. |
| **Identity & Security** | Google Identity & Firebase Auth | OAuth 2.0 token assertion, cryptographic session management, and zero credential leakage. |
| **Push Infrastructure** | Firebase Cloud Messaging (FCM) | Time-critical push notification delivery for lobby IDs, match passwords, and results. |
| **Neural Anomaly Sentry**| Gemini 3.6 Flash API | Autonomous heuristic analysis of match leaderboards, anti-fraud checks, and admin alerts. |

---

## 3. Core Architectural Principles

### 3.1 Unidirectional Data Flow (UDF)
- Every Composable consumes an **immutable UI state** emitted by the `PlatformViewModel`.
- User interactions dispatch strongly typed **Events/Actions** upward to the ViewModel.
- Coroutines mutate internal state via `MutableStateFlow` and expose read-only `StateFlow` to the UI, guaranteeing deterministic state replication without race conditions.

### 3.2 Atomic Slot Reservation & Concurrency Mutex
- Esports tournament slot reservation enforces transactional consistency.
- Realtime slot counter checks and wallet balance deductions run atomically to prevent over-subscription or double-booking during high-traffic lobby drops.

### 3.3 Time-Gated Room Credential Distribution
- Match Room ID and Passwords remain encrypted on the backend until **T-15 minutes** before match commencement.
- The client-side decodes and reveals credentials dynamically with local copy actions and automated push notifications.

### 3.4 Multi-Tiered Offline Resilience
- Even under severe cellular network packet loss, Room DB provides immediate access to registered tournaments, user profile statistics, and wallet history.
- State resynchronizes automatically upon network reconnection.

---

## 4. Subsystem Deep-Dive

### 4.1 Android Presentation Layer
- **Pure AMOLED Dark Theme (`#000000`)**: Engineered for OLED power conservation and high-contrast tournament visibility.
- **Custom Vector Graphics**: Tailored SVG/Vector drawables (including custom outline cursor icons, precision crosshairs, and tournament trophy vectors).
- **Haptic Feedback**: Android system haptic integration on all mission-critical buttons and statistics boxes.

### 4.2 Watchdog Bot & Gemini Heuristic Engine
- Located in `/velorix-watchdog-bot`.
- Built with Node.js to bridge Telegram operations and administrative controls.
- Leverages Google Gemini 3.6 Flash for automated leaderboard dispute reviews and anti-cheat anomaly flagging.

---

## 5. Security & Play Policy Invariants
- **Zero Insecure Dynamic Code Loading**: Compliant with Google Play Store system integrity policies.
- **Hardware-Isolated Credential Storage**: Sensitive keys are loaded exclusively through secure `.env` and `BuildConfig` injection, never hardcoded in source trees.
- **Minimal Permissions**: Operates on least-privilege principle (Internet, Network State, Vibration, and Post Notifications only).

---

<sub>Documented with mathematical rigor by **VX-ANANT** for the VeloRix Esports Platform.</sub>
