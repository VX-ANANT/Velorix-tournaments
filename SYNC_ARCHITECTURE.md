# VELORIX ESPORTS ENGINE — TWO-WAY REAL-TIME SYNC ARCHITECTURE (`SYNC_ARCHITECTURE.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=ADMIN%20%E2%86%94%20CLIENT%20SYNC%20ENGINE&fontSize=38&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=CENTRALIZED%20FIREBASE%20REALTIME%20PIPELINE%20%E2%80%A2%20OFFLINE%20ROOM%20CACHE%20%E2%80%A2%20LIFECYCLE%20SCHEDULER&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Sync Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/SYNC_CORE-FIREBASE_REALTIME_DB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase RTDB" />
  <img src="https://img.shields.io/badge/PERSISTENCE-OFFLINE_FIRST_ROOM-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Android Room" />
  <img src="https://img.shields.io/badge/OBSERVER-KOTLIN_FLOW_STATEFLOW-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="StateFlow" />
  <img src="https://img.shields.io/badge/HEALTH-AUTO_HEALING_SCHEDULER-0284C7?style=for-the-badge&logo=prometheus&logoColor=white" alt="Auto Healing" />
</p>

</div>

---

## 1. THE PROBLEM STATEMENT: KEEPING ADMIN & USER IN PERFECT HARMONY

In competitive mobile esports (Free Fire, BGMI), tournament status, prize pools, slot occupancy, room credentials, and emergency system states change dynamically within fractions of a second.

A traditional REST API architecture creates severe synchronization issues:
- **Stale Slots**: A player registers for a slot that an Admin already canceled or filled.
- **Delayed Credentials**: Room ID and Password posted by Admin take too long to reach the user, causing match disqualifications.
- **Desynchronized Maintenance**: Admin enters emergency maintenance, but user clients keep sending registration requests.

To solve this permanently, VeloRix establishes **Firebase Realtime Database as the Single Source of Truth (SSOT)** coupled with an **Offline-First Android Room Cache** and an **Auto-Refreshing Coroutine Lifecycle Scheduler**.

---

## 2. REAL-TIME DATA TOPOLOGY & NODE SCHEMAS

```
                                  FIREBASE REALTIME DATABASE (ROOT)
                                                  │
         ┌──────────────────┬─────────────────────┼────────────────────┬─────────────────┐
         │                  │                     │                    │                 │
         ▼                  ▼                     ▼                    ▼                 ▼
 ┌───────────────┐  ┌───────────────┐     ┌───────────────┐    ┌───────────────┐ ┌───────────────┐
 │global_app_state│  │  tournaments  │     │     users     │    │   deposits    │ │ system_config │
 └───────────────┘  └───────────────┘     └───────────────┘    └───────────────┘ └───────────────┘
```

### 2.1 `/global_app_state` (High-Speed Global Pulse)
A ultra-lightweight node monitored continuously by all connected clients:
```json
{
  "last_updated_timestamp": 1727099400000,
  "active_tournament_count": 8,
  "system_status": "OPERATIONAL",
  "active_announcement_id": "ann_2026_fall_01",
  "min_app_version": "1.4.2",
  "force_client_resync_flag": false
}
```

### 2.2 `/tournaments/{tournament_id}` (Live Match Metadata)
```json
{
  "id": "tourney_ff_101",
  "title": "Free Fire Squad Master Cup",
  "game": "FREE_FIRE",
  "entryFee": 50,
  "prizePool": 10000,
  "perKill": 15,
  "maxSlots": 48,
  "filledSlots": 34,
  "status": "UPCOMING",
  "scheduleTime": "2026-09-24T18:00:00Z",
  "credentialsReleaseTime": "2026-09-24T17:45:00Z",
  "roomId": "8941029",
  "roomPassword": "VELO99",
  "slots": {
    "slot_1": { "ign": "VeloKiller", "charId": "59201948", "team": "TeamAlpha" }
  }
}
```

### 2.3 `/system_config` (Instant Killswitch & Banner Controls)
```json
{
  "isMaintenance": false,
  "maintenanceMessage": "Scheduled server upgrade for high-capacity scrims.",
  "maintenanceUntil": "19:30 IST",
  "allowedAdminUids": ["admin_master_01"],
  "upiId": "velorix.esports@fam",
  "upiMerchantName": "VeloRix Gaming Network"
}
```

---

## 3. THREE-TIER SYNCHRONIZATION PIPELINE

```
Admin Action (Web/App)
       │
       ▼
Firebase Realtime Database (Instant Atomic Write)
       │
       ├──► Tier 1: Realtime WebSocket (Firebase ValueEventListener)
       │       • Pushes JSON delta directly to active clients within 50ms.
       │
       ├──► Tier 2: Lifecycle Resume Interceptor (SyncManager.kt)
       │       • Fires every time app moves from background to foreground.
       │       • Detects state changes while user was away in WhatsApp or Free Fire.
       │
       └──► Tier 3: Coroutine Periodic Poller (SyncManager.kt)
               • Silent background loop runs every 35 seconds.
               • Guarantees data freshness even across flaky 3G/4G cellular links.
```

---

## 4. CODE IMPLEMENTATION & CONCURRENCY CONTROLS

### 4.1 SyncManager Lifecycle Scheduler (`SyncManager.kt`)
The scheduler is initialized once in `MainActivity.kt` and managed with non-blocking Kotlin Coroutines:

```kotlin
class SyncManager private constructor(private val context: Context) {
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var periodicJob: Job? = null
    private var lastSyncTime = 0L
    private val DEBOUNCE_THRESHOLD_MS = 1000L

    fun setupLifecycleScheduler(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                // Instantly sync when user returns to the app
                triggerDataSynchronization("APP_FOREGROUND_RESUME")
            }
            override fun onActivityPaused(activity: Activity) { /* Safe pause */ }
            // ...
        })
        startPeriodicPolling(intervalSeconds = 35)
    }

    fun triggerDataSynchronization(origin: String) {
        val now = System.currentTimeMillis()
        if (now - lastSyncTime < DEBOUNCE_THRESHOLD_MS) return // Spam protection
        lastSyncTime = now

        schedulerScope.launch {
            try {
                PlatformRepository.getInstance(context).syncAllDataFromRemote()
            } catch (e: Exception) {
                Log.w("SyncManager", "Sync cycle failed gracefully: ${e.message}")
            }
        }
    }
}
```

### 4.2 Concurrency & Mutex Locks
To avoid race conditions during tournament enrollment and wallet operations, `PlatformViewModel.kt` coordinates state modifications with dedicated coroutine mutex locks:
- `joinMutex.withLock`: Guarantees only one slot registration executes at any instant per player.
- `depositMutex.withLock`: Prevents duplicate wallet deposit submissions.
- `ongoingRegistrations: ConcurrentHashMap`: In-memory thread-safe tracker preventing double-click submissions.

---

## 5. ADMIN SITUATION TESTER & HEALTH TELEMETRY

Admins and developers can monitor and debug the two-way sync in real-time within the app:
1. Log in with `velorixtest@gmail.com` or launch from the top-bar Admin Gear.
2. Open **Admin Situation Tester Sheet** (`AdminSituationTesterSheet.kt`).
3. View the **SyncManager Scheduler Monitor**:
   - Status: `ACTIVE` / `SYNCING...`
   - Interval: `35s | On App Open & Resume`
   - Last Sync: Calculated dynamically (e.g., `4s ago`).
   - "Test Immediate Auto-Refresh Cycle" button: Triggers on-demand force-refresh with haptic feedback.

---

## 6. EXTERNAL REFERENCES & TECHNICAL DOCUMENTATION

1. **Firebase Realtime Database Structure Guide**: [https://firebase.google.com/docs/database/web/structure-data](https://firebase.google.com/docs/database/web/structure-data)
2. **Android Room Architecture Guide**: [https://developer.android.com/training/data-storage/room](https://developer.android.com/training/data-storage/room)
3. **Kotlin Structured Concurrency & SupervisorJob**: [https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency](https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency)
4. **Android ActivityLifecycleCallbacks**: [https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks](https://developer.android.com/reference/android/app/Application.ActivityLifecycleCallbacks)
5. **StateFlow and SharedFlow**: [https://developer.android.com/kotlin/flow/stateflow-and-sharedflow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

---

<div align="center">
  <b>VeloRix Two-Way Sync System • Realtime Reliability for Competitive Gaming</b>
</div>
