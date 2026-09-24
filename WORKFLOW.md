# VELORIX ESPORTS ENGINE — END-TO-END WORKFLOW ARCHITECTURE (`WORKFLOW.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=blur&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=VELORIX%20WORKFLOW%20SPEC&fontSize=40&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=COMPLETE%20OPERATIONAL%20PIPELINE%20%E2%80%A2%20ADMIN-TO-CLIENT%20LIFECYCLE%20%E2%80%A2%20REAL-TIME%20ENGINE&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Workflow Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/SYNC_PIPELINE-FIREBASE_REALTIME_DB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase Realtime DB" />
  <img src="https://img.shields.io/badge/LOCAL_CACHE-ANDROID_ROOM_2.6-4285F4?style=for-the-badge&logo=sqlite&logoColor=white" alt="Android Room" />
  <img src="https://img.shields.io/badge/COROUTINES-STRUCTURED_CONCURRENCY-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Coroutines" />
  <img src="https://img.shields.io/badge/PAYMENTS-UPI_INSTANT_INTENT-22C55E?style=for-the-badge&logo=googlepay&logoColor=white" alt="UPI Instant" />
</p>

</div>

---

## 1. EXECUTIVE SUMMARY & LIFECYCLE OVERVIEW

VeloRix Esports Engine operates as an event-driven, real-time distributed platform designed to coordinate thousands of mobile esports competitors (Free Fire, BGMI, etc.) with millisecond-grade tournament synchronization. 

This document breaks down the full operational lifecycle across four primary domains:
1. **Player Onboarding & Identity Lifecycle** (Auth, Profile, Verification, Anti-Cheat Integrity).
2. **Financial Lifecycle** (Deposit, Wallet Accounting, Automatic Escrow, Instant Withdrawal).
3. **Tournament & Match Lifecycle** (Discovery, Slot Locking, T-15 Credential Broadcast, Live Scrims, Result Validation).
4. **Admin & Client Synchronization Workflow** (Firebase Realtime Single Source of Truth, Background Lifecycle Scheduler).

---

## 2. SYSTEM ARCHITECTURE & DATA FLOW

```
                        ┌─────────────────────────────────────────┐
                        │           ADMIN DASHBOARD / WEB         │
                        │    (Tournament Admin, Finance Head)     │
                        └───────────────────┬─────────────────────┘
                                            │ Writes/Updates
                                            ▼
                    ┌─────────────────────────────────────────────────┐
                    │       FIREBASE REALTIME DATABASE (SSOT)         │
                    │                                                 │
                    │  ├─ /global_app_state                           │
                    │  ├─ /tournaments/{tournament_id}                │
                    │  ├─ /users/{uid}                                │
                    │  ├─ /deposits/{deposit_id}                      │
                    │  └─ /system_config                              │
                    └───────────────────────┬─────────────────────────┘
                                            │
               ┌────────────────────────────┴────────────────────────────┐
               │ Realtime ValueEventListener & Periodic Poll Scheduler   │
               ▼                                                         ▼
┌──────────────────────────────────────────────┐        ┌──────────────────────────────────┐
│             ANDROID USER CLIENT              │        │        AUTOMATED SERVICES        │
│                                              │        │                                  │
│  ├─ SyncManager.kt (Lifecycle Engine)        │        │  ├─ Cloud Functions (Webhooks)   │
│  ├─ PlatformRepository.kt (Network/DB)       │        │  ├─ FCM Push Notifications       │
│  ├─ PlatformViewModel.kt (StateFlows)        │        │  └─ Watchdog Anti-Cheat Bot      │
│  ├─ Room Database (Offline Cache)            │        └──────────────────────────────────┘
│  ├─ SoundEffectManager.kt (MJ SFX Engine)    │
│  └─ Jetpack Compose UI Screens               │
└──────────────────────────────────────────────┘
```

---

## 3. STAGE-BY-STAGE OPERATIONAL WORKFLOWS

### Stage 1: Player Onboarding & Anti-Cheat Handshake
```
User Opens App ──► Splash Initialization ──► Device Check (Emulator / Root)
       │
       ├─── Device is Banned/Tampered ──► Redirect to BannedScreen (Reason displayed)
       │
       └─── Device Clean ──► Auth Verification (Google Sign-In / Token Auth)
                                  │
                                  ├──► New User: Profile Setup (IGN, Character ID, Game)
                                  └──► Existing User: Hydrate Room DB Cache & Navigate to HomeScreen
```
1. **Device Verification**: Handled via [PlatformRepository.kt](app/src/main/java/com/example/data/repository/PlatformRepository.kt). Detects prohibited virtual environments, hook frameworks, and unverified package signatures.
2. **Authentication Flow**: Supports Google Identity Services and email authentication.
3. **Local Database Hydration**: Android [Room Database](https://developer.android.com/training/data-storage/room) loads cached tournaments and user wallet balances for instant zero-latency UI rendering before network resolution.

---

### Stage 2: Wallet Deposit & Automated Escrow Workflow
```
Player Taps 'Add Cash' ──► Selects Amount (₹50, ₹100, ₹500)
       │
       ▼
UpiPaymentQrDialog Opens (Dynamic UPI Intent String Generated)
       │
       ├──► Instant UPI App Launch (GPay, PhonePe, Paytm, BHIM)
       │
       ▼
Payment Completed in Banking App ──► UTR / Transaction Reference Captured
       │
       ▼
User Enters 12-Digit UTR ──► [UserRateLimiter] Checks Spam Guard
       │
       ▼
PlatformViewModel.addWalletFunds() ──► Firebase /deposits/{id}
       │
       ├──► Pending Verification Status
       │
       ▼
Admin/Automated Webhook Confirms UTR ──► User Balance Incremented
       │
       ▼
[SoundEffectManager.playBeatItPower()] Fires ──► Confetti & Success Toast
```
- **Spam Guard Protection**: Rate limiter rejects duplicate submissions within 30 seconds to prevent UTR brute-forcing.
- **Audio Feedback**: The 0.85s `Beat It` power synth punch triggers immediately upon confirmed balance credit.

---

### Stage 3: Tournament Registration & Slot Reservation
```
Browse Tournaments ──► Filter by Game / Mode (Solo, Duo, Squad)
       │
       ▼
Open TournamentDetailsScreen (Checks Entry Fee vs Wallet Balance)
       │
       ├──► Insufficient Funds: Opens Wallet Deposit Dialog
       │
       ▼
Sufficient Balance: Tap 'Join Match' ──► Slot Matrix Dialog Opens
       │
       ▼
Player Selects Specific Slot # (e.g. Slot 12, Team Slot 4)
       │
       ▼
Input In-Game Name (IGN) & Game Character ID
       │
       ▼
PlatformViewModel.joinTournamentWithSlot()
       │
       ├──► JoinMutex Acquires Thread Lock (Prevents Race Conditions)
       ├──► Firebase Atomic Transaction Reserves Slot
       ├──► Local Room Database Updates Tournament Entity
       │
       ▼
Success Response ──► [SoundEffectManager.playBillieGroove()]
       │
       ▼
3-Second Victory Confetti Burst + Registration Confirmation Pass
```
- **Race Condition Prevention**: Employs `joinMutex.withLock` and Firebase atomic increments so two players clicking the same slot at the exact same millisecond never collide.
- **Iconic Feedback**: `playBillieGroove()` executes the 1.05s 116 BPM Billie Jean groove while celebratory confetti renders on screen.

---

### Stage 4: T-15 Room Credentials Reveal & Copy Flow
```
Countdown Reaches T-15 Minutes
       │
       ▼
Admin Dispatches Room ID & Password to Firebase /tournaments/{id}/roomId
       │
       ▼
SyncManager Receives Real-time Node Delta
       │
       ├──► Triggers FCM Notification ("Room Details Released!")
       ├──► Updates MatchPassDialog & TournamentDetailsScreen
       │
       ▼
User Opens Tournament Card
       │
       ▼
[SoundEffectManager.playSmoothStab()] Plays (Smooth Criminal Brass Horn Stab)
       │
       ▼
Dual Credentials Card Revealed:
   [ROOM ID: 8941029]   [PASSWORD: VELO99]
          │                     │
   Tap "Copy ID"         Tap "Copy Pass"
          │                     │
          ▼                     ▼
[SoundEffectManager.playBadSnap()] Plays (Crisp 0.45s Funky Finger Snap)
          │
          ▼
Copied to System Clipboard + Haptic Vibration
```

---

### Stage 5: Live Scrims, Result Calculation & Prize Payout
```
Match Concludes in In-Game Custom Room
       │
       ▼
Admin Uploads Match Screenshot / Kills Leaderboard via Admin Panel
       │
       ▼
Automated Score Calculation:
   Score = (Rank Points) + (Kills × PerKill Bounty)
       │
       ▼
Firebase /tournaments/{id}/status Set to "COMPLETED"
       │
       ▼
Winners' Wallets Credited in Single Atomic Batch
       │
       ▼
SyncManager Pushes Notification & Updates In-App Podium & LeaderboardScreen
```

---

## 4. ADMIN & CLIENT TWO-WAY SYNCHRONIZATION PIPELINE

To prevent the User and Admin apps from falling out of sync, VeloRix employs a **Triple-Synchronized Protocol**:

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                      TRIPLE-SYNC PROTOCOL MATRIX                       │
 ├──────────────────┬─────────────────────────────────────────────────────┤
 │ Protocol Layer   │ Function & Execution Path                           │
 ├──────────────────┼─────────────────────────────────────────────────────┤
 │ 1. Realtime Push │ Firebase WebSocket Listener (ValueEventListener)    │
 │                  │ Immediate 0ms propagation when admin edits data.    │
 ├──────────────────┼─────────────────────────────────────────────────────┤
 │ 2. Lifecycle Sync│ ActivityLifecycleCallbacks in SyncManager.kt        │
 │                  │ Auto-sync triggers every time app enters foreground │
 │                  │ or user switches back from banking / game apps.     │
 ├──────────────────┼─────────────────────────────────────────────────────┤
 │ 3. Periodic Poll │ Coroutine Background Loop (Dispatchers.IO)          │
 │                  │ Hard polling every 35s to guarantee fresh state.    │
 └──────────────────┴─────────────────────────────────────────────────────┘
```

### SyncManager Implementation Details:
- **Location**: [SyncManager.kt](app/src/main/java/com/example/data/sync/SyncManager.kt)
- **Debounce Guard**: 1000ms cooldown prevents spamming the network when activities rapidly pause/resume.
- **Tester Sheet**: Admins can inspect live sync health, last sync timestamps, and manually force sync cycles via the in-app **Admin Situation Tester Sheet**.

---

## 5. REPOSITORY & VIEWMODEL DATA ARCHITECTURE

```
PlatformViewModel (UI State Producer)
    │
    ├── tournaments: StateFlow<List<Tournament>>
    ├── user: StateFlow<User?>
    ├── banners: StateFlow<List<Banner>>
    ├── syncHealth: StateFlow<SyncHealthStatus>
    └── sfxEngine: SoundEffectManager
          ▲
          │
PlatformRepository (Single Source of Truth Coordinator)
    │
    ├── Local: Room AppDatabase (UserDao, TournamentDao, MatchDao)
    └── Remote: Firebase Realtime Database + FCM REST API
```

---

## 6. EXTERNAL TECHNOLOGY & TOOLING LINKS

| Technology / Library | Documentation Link | Usage in VeloRix |
| :--- | :--- | :--- |
| **Kotlin Coroutines** | [kotlinlang.org/docs/coroutines-overview.html](https://kotlinlang.org/docs/coroutines-overview.html) | Asynchronous task execution & background schedules |
| **Kotlin StateFlow** | [kotlinlang.org/api/kotlinx.coroutines/.../state-flow.html](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/) | Reactive UI state binding in ViewModels |
| **Android Room DB** | [developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room) | Local offline caching and rapid start hydration |
| **Firebase Realtime DB** | [firebase.google.com/docs/database](https://firebase.google.com/docs/database) | Centralized live database and single source of truth |
| **Firebase Cloud Messaging** | [firebase.google.com/docs/cloud-messaging](https://firebase.google.com/docs/cloud-messaging) | Push alerts for match rooms and prize drops |
| **Jetpack Compose** | [developer.android.com/jetpack/compose](https://developer.android.com/jetpack/compose) | Declarative UI toolkit for all mobile screens |
| **Android SoundPool** | [developer.android.com/reference/android/media/SoundPool](https://developer.android.com/reference/android/media/SoundPool) | Zero-latency game sound effects engine |
| **NPCI UPI Specification** | [npci.org.in/what-we-do/upi/product-overview](https://www.npci.org.in/what-we-do/upi/product-overview) | Peer-to-peer and merchant payment standard |
| **Coil Image Loader** | [coil-kt.github.io/coil/](https://coil-kt.github.io/coil/) | High-performance asynchronous image rendering |

---

<div align="center">
  <b>VeloRix Operational Architecture • High Precision Esports Management</b>
</div>
