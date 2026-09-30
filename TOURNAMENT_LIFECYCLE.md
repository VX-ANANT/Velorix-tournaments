# VELORIX ESPORTS ENGINE — TOURNAMENT LIFECYCLE SPECIFICATION (`TOURNAMENT_LIFECYCLE.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=blur&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=TOURNAMENT%20LIFECYCLE%20SPEC&fontSize=38&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=DETERMINISTIC%20STATE%20MACHINE%20%E2%80%A2%20SLOT%20ALLOCATION%20%E2%80%A2%20T-15%20GATE%20%E2%80%A2%20ESCROW%20SETTLEMENT&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Tournament Lifecycle Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/STATE_MACHINE-FINITE_DETERMINISTIC-00E676?style=for-the-badge&logo=diagramsdotnet&logoColor=black" alt="Finite State Machine" />
  <img src="https://img.shields.io/badge/CONCURRENCY-ATOMIC_MUTEX_LOCK-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Atomic Mutex" />
  <img src="https://img.shields.io/badge/TRANSPORT-FIREBASE_REALTIME_DB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase RTDB" />
  <img src="https://img.shields.io/badge/LOCAL_CACHE-OFFLINE_FIRST_ROOM-7F52FF?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room DB" />
</p>

</div>

---

## 1. EXECUTIVE SUMMARY & LIFECYCLE OVERVIEW

In competitive mobile esports (Free Fire, Battlegrounds Mobile India), tournament lobbies operate under extreme real-time constraints. Hundreds of concurrent players compete for limited room slots (Solo 48, Duo 24 teams, Squad 12 teams). The tournament lifecycle must maintain strict mathematical invariants:
1. **Zero Over-Subscription**: No two players can claim the same slot, even if clicking at the exact same millisecond.
2. **Deterministic State Transitions**: State progression is linear and guarded; an ongoing match cannot accept new registrations, and a completed match cannot be joined or refunded twice.
3. **Escrow Safety**: Funds are locked into an automated escrow upon registration and released only when authoritative match results are confirmed or if the tournament is cancelled.
4. **Time-Gated Credential Revelation**: Custom room IDs and passwords are cryptographically shielded until exactly **T-15 minutes** before start time.

---

## 2. FINITE STATE MACHINE (FSM)

The tournament entity traverses six immutable states:

```
 ┌─────────────────┐
 │      DRAFT      │ Admin prepares match details, map, rules & prize pool
 └────────┬────────┘
          │ (Admin Publishes)
          ▼
 ┌─────────────────┐
 │    UPCOMING     │ Visible in Upcoming tab; countdown active; registrations open
 └────────┬────────┘
          │ (T - 15 Minutes Reached / Slot Cap Reached)
          ▼
 ┌─────────────────┐
 │  LOBBY_LOCKED   │ Slot allocations finalized; Room ID & Password broadcasted
 └────────┬────────┘
          │ (Scheduled Match Start Time)
          ▼
 ┌─────────────────┐
 │   LIVE_SCRIM    │ In-game custom room underway; spectator referee monitoring
 └────────┬────────┘
          │ (Match Concludes, Scoreboard Uploaded)
          ▼
 ┌─────────────────┐
 │  VERIFICATION   │ 15-minute dispute window; Gemini OCR + Admin telemetry audit
 └────────┬────────┘
          │
     ┌────┴──────────────────────────┐
     ▼                               ▼
┌───────────┐                 ┌─────────────┐
│ COMPLETED │                 │  CANCELLED  │
│ Payouts   │                 │ 100% Escrow │
│ Credited  │                 │ Refunded    │
└───────────┘                 └─────────────┘
```

### State Definitions & Trigger Invariants

| State | Allowed Player Actions | Allowed Admin Actions | Backend Automation Trigger |
| :--- | :--- | :--- | :--- |
| **`UPCOMING`** | Browse details, select slot, join match (escrow deduction), unregister (if allowed >2h before). | Edit match title, map, schedule, prize distribution. | Continuous countdown ticker; FCM reminders at T-60m and T-30m. |
| **`LOBBY_LOCKED`** | View registered slot, copy Room ID & Password, open game client. | Inject/modify Room ID & Password, ban malicious slots. | Automatic credential release at **T-15 minutes** via Firebase delta. |
| **`LIVE_SCRIM`** | Read-only match spectator guidelines, view registered roster. | Live refereeing, stream broadcasting. | Match timer expires; game custom room starts. |
| **`VERIFICATION`** | Submit kill proof/screenshot dispute via 24/7 AI desk. | Upload end-of-game victory scoreboard screenshot. | Gemini 3.6 Flash neural OCR parses UID, placement & kill tally. |
| **`COMPLETED`** | View final podium, kill bounties, and credit in wallet history. | Export match audit report and ledger settlement log. | Automated atomic wallet credit batch executed for winners. |
| **`CANCELLED`** | Receive 100% entry fee refund in wallet balance + push alert. | Trigger emergency cancellation with mandatory reason. | Escrow reversal atomic batch returns all funds to user balances. |

---

## 3. SLOT RESERVATION & CONCURRENCY MUTEX

### 3.1 The Race Condition Threat
During popular high-prize tournaments, 50+ players frequently press **"Confirm Slot 12"** simultaneously. In an un-isolated system, multiple players would receive slot 12, causing room entry disputes in the in-game lobby.

### 3.2 Dual-Tier Locking Architecture
VeloRix eliminates concurrency conflicts using a two-tier locking pipeline:

```
[User Taps Join] 
       │
       ▼
1. Android Client Mutex (`joinMutex.withLock`)
   • Locks local UI state machine to prevent double-tap submissions
   • Verifies local Room DB wallet balance >= entry fee
       │
       ▼
2. Firebase Realtime Database Atomic Transaction
   • Transaction reads `/tournaments/{id}/slots/{slot_number}`
   • If slot.occupied == false:
         sets slot.occupied = true
         sets slot.player_uid = user.uid
         sets slot.in_game_name = user.ign
         decrements `/tournaments/{id}/available_slots`
         commits atomically
   • If slot.occupied == true:
         aborts transaction; returns SLOT_ALREADY_TAKEN error
       │
       ▼
3. Wallet Escrow Mutex
   • Atomically deducts entry fee from `/users/{uid}/wallet/playable_balance`
   • Records pending escrow ledger item `/wallets/escrow/{tournament_id}_{uid}`
```

---

## 4. T-15 CREDENTIAL GATING & SECURE BROADCAST

```
┌────────────────────────────────────────────────────────────────────────┐
│                        T-15 COUNTDOWN PIPELINE                         │
├─────────────────┬──────────────────────────────────────────────────────┤
│ Time Remaining  │ Action & Cryptographic State                         │
├─────────────────┼──────────────────────────────────────────────────────┤
│ T > 15 Minutes  │ Room ID & Password remain null or server-encrypted.  │
│                 │ Client displays tactical animated countdown timer.   │
├─────────────────┼──────────────────────────────────────────────────────┤
│ T = 15 Minutes  │ Admin pushes credentials to `/tournaments/{id}/room`.│
│                 │ SyncManager receives delta via WebSocket listener.   │
│                 │ FCM High-Priority Push: "Room ID & Pass Available!"  │
│                 │ SoundEffectManager plays Brass Horn Stab SFX.        │
├─────────────────┼──────────────────────────────────────────────────────┤
│ T < 15 Minutes  │ Credential card becomes interactive:                 │
│                 │ • One-tap "Copy Room ID" (Clipboard + Haptic Snap)   │
│                 │ • One-tap "Copy Password" (Clipboard + Haptic Snap)  │
│                 │ • Direct deep-link button: "Launch Game Client"      │
└─────────────────┴──────────────────────────────────────────────────────┘
```

---

## 5. MINIMUM PARTICIPANT QUORUM & AUTO-CANCELLATION

To safeguard competitive integrity and prize pool economics, each tournament defines a **Minimum Quorum Threshold**:
- **Solo Matches (48 Slots)**: Minimum 24 players (50% threshold).
- **Duo Matches (24 Teams)**: Minimum 12 teams (50% threshold).
- **Squad Matches (12 Teams)**: Minimum 8 teams (66% threshold).

### Auto-Cancellation Trigger Rule:
If the participant threshold is not met at **T-10 minutes**:
1. The backend automatically marks the tournament as `CANCELLED`.
2. The reason is logged as: `FAILED_QUORUM_UNDER_50_PERCENT`.
3. All registered users receive an immediate **100% Escrow Refund** credited directly to their `playable_balance`.
4. An automated push notification is dispatched: *"Match [Name] was cancelled due to insufficient participants. Your entry fee of ₹X has been fully refunded to your wallet."*

---

## 6. POST-MATCH VERIFICATION & DISPUTE WINDOW

1. **Match End Detection**: The match concludes in the custom game room.
2. **Screenshot Ingestion**: Tournament referee or admin uploads the official match result screenshot.
3. **Gemini 3.6 Flash Neural Analysis**:
   - The multimodal model extracts: `Rank`, `Player Name`, `UID`, `Total Kills`, `Damage`.
   - Cross-references parsed entries with registered tournament roster.
4. **15-Minute Dispute Window**:
   - Scores are published in a preliminary state.
   - Players have 15 minutes to flag discrepancies via the in-app AI dispute desk by providing screen recording or proof.
5. **Final Settlement**: Once the window closes with zero unresolved disputes, the prize pool engine executes atomic balance credits to winners.

---

<sub>Authored and Maintained for the **VeloRix Esports Engine** by **VX-ANANT**.</sub>
