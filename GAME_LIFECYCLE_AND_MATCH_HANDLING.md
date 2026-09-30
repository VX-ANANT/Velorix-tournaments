# VELORIX ESPORTS ENGINE — MATCH HANDLING & GAME LIFECYCLE (`GAME_LIFECYCLE_AND_MATCH_HANDLING.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=blur&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=MATCH%20HANDLING%20%26%20LIFECYCLE&fontSize=38&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=UPCOMING%20%E2%80%A2%20LIVE%20SCRIMS%20%E2%80%A2%20COMPLETED%20PODIUM%20%E2%80%A2%20ROOM%20DB%20CACHING&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Match Lifecycle Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/MATCH_TABS-UPCOMING%20%7C%20LIVE%20%7C%20COMPLETED-00E676?style=for-the-badge&logo=target&logoColor=black" alt="Match Tabs" />
  <img src="https://img.shields.io/badge/UI_CANVAS-JETPACK_COMPOSE_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose UI" />
  <img src="https://img.shields.io/badge/OFFLINE_CACHE-ROOM_MATCH_DAO-7F52FF?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room Cache" />
  <img src="https://img.shields.io/badge/DATA_STREAM-STATEFLOW_PIPELINE-FFCA28?style=for-the-badge&logo=kotlin&logoColor=black" alt="StateFlow" />
</p>

</div>

---

## 1. THREE-PHASE TOURNAMENT STATE TOPOLOGY

The VeloRix tournament hub groups matches into three intuitive, distinct operational phases within the UI (`MatchesScreen.kt` and `HomeScreen.kt`):

```
┌────────────────────────────────────────────────────────────────────────┐
│                        TOURNAMENT SEGREGATION TABS                     │
├─────────────────────┬────────────────────┬─────────────────────────────┤
│ 1. UPCOMING         │ 2. LIVE / ONGOING  │ 3. COMPLETED                │
├─────────────────────┼────────────────────┼─────────────────────────────┤
│ • Registration Open │ • Match In-Flight  │ • Results Settled           │
│ • Real-time Ticker  │ • Room Details Live│ • Winner Podium (1st,2nd,3rd│
│ • Slot Selection    │ • Spectator Mode   │ • Kill Bounty Breakdown     │
│ • Escrow Staging    │ • Active Sentry    │ • Historical Room Cache     │
└─────────────────────┴────────────────────┴─────────────────────────────┘
```

---

## 2. PHASE 1: UPCOMING GAMES (REGISTRATION & STAGING)

### 2.1 UI Presentation & Interaction
- **Tournament Card**: Displays Game Title (Free Fire / BGMI), Match Type (Solo/Duo/Squad), Map (Bermuda, Purgatory, Erangel), Entry Fee, Prize Pool, and Total Slots Occupied (e.g. `34/48 Filled`).
- **Countdown Ticker**: Real-time ticker counting down to scheduled start time (`02h : 14m : 30s`).
- **One-Tap Slot Matrix**:
  - Displays visual grid of room slots (Slot 1 to 48).
  - Green indicator = Available; Red avatar = Occupied.
  - Selecting an open slot locks it with `joinMutex` and deducts the entry fee into escrow.

### 2.2 Local Caching & Synchronization:
- Upcoming matches are streamed continuously from Firebase RTDB `/tournaments` node with `status == "UPCOMING"`.
- Inserted into Room SQLite table `tournaments` via `TournamentDao.upsertTournaments()`.
- Users can view registered matches even when completely offline or in flight mode.

---

## 3. PHASE 2: LIVE / ONGOING GAMES (CUSTOM ROOM & SCRIMS)

### 3.1 Transition to Live
- When start time arrives or Admin flips tournament status to `LIVE_SCRIM` or `ONGOING`:
  - Match card shifts seamlessly from the **Upcoming** tab to the **Live** tab.
  - Status badge pulses with tactical neon red indicator: `● LIVE SCRIMS IN PROGRESS`.

### 3.2 Participant Experience vs Spectator Experience:
```
                                [USER OPENS LIVE MATCH CARD]
                                              │
                    ┌─────────────────────────┴─────────────────────────┐
                    ▼ Registered Participant                            ▼ Spectator / Non-Participant
      ┌──────────────────────────────────┐                ┌──────────────────────────────────┐
      │  • Revealed Room ID & Password   │                │  • Watch Live Stream (YouTube)   │
      │  • One-Tap Copy with Haptic Snap │                │  • View Registered Rosters       │
      │  • Deep-Link: "Launch Game App"  │                │  • View Prize Pool Distribution  │
      │  • Emergency Admin Live Chat     │                │  • Registration Disabled Notice  │
      └──────────────────────────────────┘                └──────────────────────────────────┘
```

### 3.3 Dynamic Credential Security:
- Non-registered players **cannot** view Room ID and Passwords. Security rules and client composables mask credentials for unauthorized UIDs to prevent uninvited room crashing.

---

## 4. PHASE 3: COMPLETED GAMES (PODIUM & PAYOUT HISTORY)

### 4.1 Transition & Result Archival
- Upon match conclusion, Admin or the Neural Sentry uploads the final scoreboard.
- Tournament status transitions to `COMPLETED`.
- Match card moves to the **Completed** tab.

### 4.2 Cybernetic Podium & Standings View:
- **Top 3 Podium**:
  - **1st Place (Gold)**: Trophy icon, Winner IGN, Kills, Prize Won (e.g. ₹250).
  - **2nd Place (Silver)**: Silver badge, Runner-up IGN, Prize Won (e.g. ₹150).
  - **3rd Place (Bronze)**: Bronze badge, 3rd place IGN, Prize Won (e.g. ₹100).
- **Kill Bounty Leaders**: Lists players with the highest individual kill count and their bounty credits.
- **Full Match Roster Table**: Expandable list displaying every slot, player IGN, placement rank, kills, and earnings.

### 4.3 Automated Wallet Reconciliation:
- Simultaneously, all winners have their `VT Tokens` credited atomically.
- Notification appears in their **In-App Notification Center**: *"Victory! You won ₹X in [Tournament Title]!"*
- Confetti celebration burst triggers when the user navigates to their profile or wallet dashboard.

---

## 5. ROOM DATABASE & FIREBASE SYNC LIFECYCLE

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA PERSISTENCE STRATEGY                       │
├──────────────────┬──────────────────┬──────────────────────────────────┤
│ Match Category   │ Room DB Lifespan │ Firebase RTDB Listener Policy    │
├──────────────────┼──────────────────┼──────────────────────────────────┤
│ **Upcoming**     │ Persisted & fresh│ Real-time ValueEventListener.    │
│                  │ on every delta   │ Active connection for live slots.│
├──────────────────┼──────────────────┼──────────────────────────────────┤
│ **Live / Scrim** │ Cached in memory │ Sub-15ms WebSocket listener for  │
│                  │ and SQLite       │ room ID and emergency alerts.    │
├──────────────────┼──────────────────┼──────────────────────────────────┤
│ **Completed**    │ Retained in Room │ Listener detached after settle.  │
│                  │ for offline read │ Loaded on-demand via paginated   │
│                  │ & history stats  │ REST snapshot to conserve data.  │
└──────────────────┴──────────────────┴──────────────────────────────────┘
```

---

<sub>Authored and Maintained for the **VeloRix Esports Engine** by **VX-ANANT**.</sub>
