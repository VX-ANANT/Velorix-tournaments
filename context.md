# Velorix Esports Platform - Complete Project Context & Architecture Reference

> **Document Version:** 2.0.0  
> **Target Audience:** AI Agents, Full-Stack Engineers, Admin Panel Developers, Backend Engineers.  
> **Platform Purpose:** Production-grade competitive Esports tournament management, automated matchmaking, anti-exploit referral commission engine, tactical daily/weekly/monthly missions, and real-time wallet & payout ledger.

---

## Table of Contents
1. [System Overview & Architecture](#1-system-overview--architecture)
2. [Technology Stack](#2-technology-stack)
3. [Currency & Economy Model](#3-currency--economy-model)
4. [Referral & Tiered Deposit Commission Engine](#4-referral--tiered-deposit-commission-engine)
5. [Missions, Streaks & Anti-Exploit Limiter](#5-missions-streaks--anti-exploit-limiter)
6. [Tournament Matchmaking & Anti-Cheat Validation](#6-tournament-matchmaking--anti-cheat-validation)
7. [Database Schemas (Firestore & Realtime DB)](#7-database-schemas-firestore--realtime-db)
8. [Firebase Cloud Functions API Contract](#8-firebase-cloud-functions-api-contract)
9. [Security Rules & Row-Level Security (RLS)](#9-security-rules--row-level-security-rls)
10. [Admin Panel Integration Guide & Control Operations](#10-admin-panel-integration-guide--control-operations)

---

## 1. System Overview & Architecture

Velorix is a high-performance, real-time esports tournament platform for mobile gamers (Free Fire, BGMI, Clash Squad, Lone Wolf). It operates on a **Dual-Database Synchronization Architecture**:

* **Cloud Firestore:** Primary persistence for structured relational data, transactions, legal records, audit logs, and complex queries.
* **Firebase Realtime Database (RTDB):** Low-latency real-time state for live match lobby slots, real-time chat, tournament sync status, and instant balance mirrors.
* **Firebase Cloud Functions (Node/TypeScript):** Authoritative backend server enforcing all financial calculations, referral commissions, mission claims, daily streak validation, and anti-tamper constraints.
* **Client App (Android):** Kotlin + Jetpack Compose (Material Design 3 with Cyber Tactical Liquid Glass styling), Room database for instant offline caching, and strict server-side synchronization.

```
+-------------------------------------------------------------------------+
|                              VELORIX PLATFORM                            |
+-------------------------------------------------------------------------+
|                                                                         |
|   +-------------------+      HTTPS / WSS      +---------------------+   |
|   | Android Client App | <==================> | Firebase Cloud Funcs |   |
|   | (Jetpack Compose) |                      | (Authoritative API) |   |
|   +-------------------+                      +---------------------+   |
|            |                                            |               |
|            | Real-Time Listeners                        | Transactions  |
|            v                                            v               |
|   +-----------------------------------------------------------------+   |
|   |                  FIREBASE CLOUD INFRASTRUCTURE                  |   |
|   |                                                                 |   |
|   |  - Cloud Firestore (Users, Transactions, Audit, Rules RLS)     |   |
|   |  - Realtime Database (Slots, Live Chat, Match Updates)          |   |
|   |  - Firebase Auth (Email/Password, Phone OTP, Google Identity)   |   |
|   +-----------------------------------------------------------------+   |
|                                ^                                        |
|                                | Admin SDK / Elevated Privileges        |
|                                v                                        |
|   +-----------------------------------------------------------------+   |
|   |                  VELORIX ADMIN CONTROL PANEL                    |   |
|   |   (Tournament Manager, User Sanctions, Payout Approvals, Banners)  |   |
+-------------------------------------------------------------------------+
```

---

## 2. Technology Stack

* **Mobile App:** Kotlin 2.0+, Jetpack Compose, Material 3, Android Architecture Components (MVVM), Coroutines & StateFlow, Navigation Compose, Room DB, Coil, Moshi, Retrofit / Ktor.
* **Serverless Backend:** Node.js 18, TypeScript, Firebase Cloud Functions (v4+), Firebase Admin SDK.
* **Cloud Storage & Database:** Google Cloud Firestore (Multi-region), Firebase Realtime Database.
* **Security & Auth:** Firebase Authentication, Cloud Functions RBAC, Firestore Security Rules (RLS), RTDB Rules.

---

## 3. Currency & Economy Model

The Velorix platform employs a two-tier tokenomics system:

### A. Playable Wallet Balance (`VT Tokens`)
* **Peg:** 1 VT Token = ₹1.00 INR (Standard Tournament Value).
* **Usage:** Used to pay tournament entry fees and receive tournament prize pool & kill bounty payouts.
* **Liquidity:** Can be deposited via UPI QR / Payment Gateway and withdrawn via UPI ID / Bank Transfer.

### B. In-Game Combat Tokens (`Tokens`)
* **Source:** Earned for completing Daily, Weekly, and Monthly Missions, maintaining Daily Login Streaks, and community events.
* **Exchange Rate:** **10 Combat Tokens = 1 VT Token** (convertible in Wallet).
* **Daily Mission Reward Cap:** Strictly **100 Combat Tokens / day** per user to prevent infinite farming exploits.

---

## 4. Referral & Tiered Deposit Commission Engine

To completely eliminate self-referral money glitches, multi-account farming, and bot exploitation, referral codes are governed by strict server-side rules:

### A. Registration-Only Linking Rule
* **No In-App Entry:** Users **CANNOT** enter a referral code after account creation. The referral input is strictly confined to the `AuthScreen` registration interface.
* **Anti-Fraud Protections:** 
  * Self-referral is blocked (`referrerUid === newUserId`).
  * Accounts sharing identical phone numbers or email addresses are blocked.
  * Unique referral format: `VRX-<USERNAME>-<RANDOM>`.

### B. Dynamic Tier-Based Deposit Commission
When a referred player deposits real funds into their wallet, the referrer automatically receives lifetime deposit commissions based on their active squad size:

| Referral Tier | Active Squadmates Referred | Commission Rate | Payout Destination |
|:---|:---:|:---:|:---|
| **Tier 1** | 1 Squadmate | **10%** of deposit value | Directly credited as VT Tokens to Referrer Balance |
| **Tier 2** | 2 – 4 Squadmates | **12%** of deposit value | Directly credited as VT Tokens to Referrer Balance |
| **Tier 3 (MAX CAP)** | 5+ Squadmates | **15% STRICT MAX CAP** | Directly credited as VT Tokens to Referrer Balance |

* **Automation:** Handled atomically by Cloud Function `processReferralDepositCommission` and triggered on Firestore `deposit_requests/{reqId}` status update (`status === "SUCCESS"`).
* **Audit Trail:** Creates a permanent transaction record `TX_COMMISSION_<UID>_<TIMESTAMP>` and sends an instant in-app notification.

---

## 5. Missions, Streaks & Anti-Exploit Limiter

### A. Reset Schedule (IST - Asia/Kolkata UTC+5:30)
* **Daily Missions & Streak:** Resets every midnight at **12:00 AM IST**.
* **Weekly Directives:** Resets every **Monday at 12:00 AM IST**.
* **Monthly Directives:** Resets on the **1st of every month at 12:00 AM IST**.

### B. Procedural Multi-Tier Mission Pools
Each cycle presents **5 active missions** in each category:

1. **Daily (5 Active):**
   * *Daily Check-In Streak* (+10 Tokens)
   * *Combat Deployment - 1 Match* (+15 Tokens)
   * *Target Neutralization - 2 Kills* (+15 Tokens)
   * *Top 5 Placement* (+20 Tokens)
   * *Squad Recruiter Share* (+10 Tokens)
2. **Weekly (5 Active):**
   * *Weekly Warrior - 5 Matches* (+30 Tokens)
   * *Sharpshooter Elite - 10 Kills* (+35 Tokens)
   * *Victory Royale - #1 Champion* (+40 Tokens)
   * *Token Liquidity - Convert to VT* (+25 Tokens)
   * *5-Day Loyalty Streak* (+30 Tokens)
3. **Monthly (5 Active):**
   * *Season Veteran - 20 Matches* (+60 Tokens)
   * *Centurion Slayer - 35 Kills* (+75 Tokens)
   * *Podium Dominance - 3 Top 3s* (+80 Tokens)
   * *Leaderboard Contender* (+50 Tokens)
   * *Squad Master - 2 Active Squadmates* (+70 Tokens)

### C. Server-Side Reward Limiter
* Cloud Function `validateMissionClaim` checks `dailyMissionsTokensClaimed`.
* If `dailyMissionsTokensClaimed + reward > 100`, the claim is rejected with `resource-exhausted`.

---

## 6. Tournament Matchmaking & Anti-Cheat Validation

* **Game ID / In-Game Name (IGN) Verification:** Player UIDs for Free Fire and BGMI are checked for 8–12 numeric digit constraints and anti-dummy filters (`onTournamentRegistrationCreate`).
* **Room Credentials Security:** Room ID and Password are protected by timing gates and released only **15 minutes before match start** to registered participants.
* **Slot Locking:** Concurrent slot reservations in RTDB prevent overbooking.

---

## 7. Database Schemas (Firestore & Realtime DB)

### A. Users Collection (`/users/{uid}`)
```json
{
  "id": "USR_99214A",
  "username": "ShadowSniper",
  "phoneOrEmail": "player@velorix.com",
  "balance": 250.0,
  "tokens": 45,
  "loginStreak": 7,
  "lastLoginClaimDate": "2026-09-24",
  "lastMissionClaimDate": "2026-09-24",
  "dailyMissionsTokensClaimed": 35,
  "referralCode": "VRX-SHAD-8812",
  "referredBy": "VRX-ALPHA-1001",
  "referralCount": 3,
  "referralEarnings": 120.0,
  "freeFireId": "829471920",
  "freeFireIgn": "Shadow_FF",
  "bgmiId": "5192847102",
  "bgmiIgn": "Shadow_BGMI",
  "role": "player",
  "isBanned": false,
  "isSuspended": false,
  "updatedAt": "2026-09-24T18:25:00Z"
}
```

### B. Tournaments Collection (`/tournaments/{tournamentId}`)
```json
{
  "id": "TRN_FF_SOLO_101",
  "title": "Bermuda Championship Solo #101",
  "game": "Free Fire",
  "matchCategory": "Battle Royale",
  "format": "SOLO",
  "mapType": "Bermuda",
  "entryFee": 30.0,
  "prizePool": 1200.0,
  "killBounty": 10.0,
  "filledSlots": 38,
  "maxSlots": 48,
  "startTime": "2026-09-24T20:00:00+05:30",
  "status": "OPEN",
  "roomId": "ROOM_78291",
  "roomPassword": "PASS_velorix",
  "roomReleased": true
}
```

### C. Transactions Collection (`/transactions/{txId}`)
```json
{
  "id": "TX_COMMISSION_USR_99214A_1727202300000",
  "userId": "USR_99214A",
  "type": "REFERRAL_COMMISSION",
  "amount": 24.0,
  "detail": "Squad Referral Commission: 12% on ₹200 deposit by Squadmate_42",
  "isPositive": true,
  "status": "SUCCESS",
  "timestamp": 1727202300000
}
```

### D. System App Config (`/system_config/app_config`)
```json
{
  "maintenanceMode": false,
  "maintenanceMessage": "Scheduled tactical server upgrade.",
  "minAppVersion": "2.0.0",
  "forceUpdate": false,
  "showBanners": true,
  "upiId": "velorix.esports@upi",
  "upiMerchantName": "Velorix Esports Platform"
}
```

---

## 8. Firebase Cloud Functions API Contract

| Function Name | Type | Auth Required | Parameters | Description |
|:---|:---:|:---:|:---|:---|
| `validateDailyLogin` | HTTPS Callable | Yes | `{ userId?: string }` | Validates IST midnight streak, awards 10 Tokens, checks daily cap. |
| `validateMissionClaim` | HTTPS Callable | Yes | `{ userId: string, missionId: string, rewardCurrency?: number }` | Verifies completion, enforces 100 token cap, marks mission claimed. |
| `verifyReferralOnRegistration` | HTTPS Callable | No | `{ newUserId: string, referralCode: string, newUsername?: string, newUserEmail?: string, newUserPhone?: string }` | Validates referral code on signup, links referee, blocks self-referral. |
| `processReferralDepositCommission` | HTTPS Callable | Yes (Admin/Internal) | `{ payerUserId: string, depositAmount: number, depositTxId?: string }` | Awards 10%, 12%, or 15% (max cap) commission on deposit to referrer. |
| `onUserDepositWritten` | Firestore Trigger | System | `deposit_requests/{reqId}` | Listens for deposit approvals and automatically invokes commission. |
| `onTournamentRegistrationCreate` | RTDB Trigger | System | `/registrations/{tournamentId}/{uid}` | Enforces 8-12 numeric digit Game ID verification and sanitizes invalid entries. |

---

## 9. Security Rules & Row-Level Security (RLS)

* **Firestore (`firestore.rules`):**
  * `balance`, `tokens`, `referralEarnings`, `referralCount`, `isBanned` are protected against client mutations.
  * Audit logs (`audit_logs/`) are append-only and strictly immutable.
  * Users can only read their own transactions and modify their own non-financial profile fields.
* **Realtime Database (`database.rules.json`):**
  * Data schema validation enforces non-negative numeric balances, valid string formats, and admin privilege gates.

---

## 10. Admin Panel Integration Guide & Control Operations

An AI Agent or engineer building the **Velorix Web Admin Panel** should utilize the Firebase Admin SDK or authenticated Firebase Client with Admin Claims (`request.auth.token.admin == true`).

### Essential Admin Panel Screens & Workflows:

1. **Tournament Operations:**
   * Create, update, cancel, and complete tournament matches.
   * Enter Room ID and Room Password to release credentials to registered players.
   * Enter final match rankings & kill counts -> trigger prize pool distribution.

2. **Deposit & Payout Requests Management:**
   * View live queue of `deposit_requests` and `withdraw_requests`.
   * Approve deposit (sets `status = "SUCCESS"`) -> automatically triggers `onUserDepositWritten` to credit user balance and award referral commission.
   * Process withdrawal via UPI gateway / Bank API and mark `status = "SUCCESS"`.

3. **User Moderation & Sanctions:**
   * Ban/Unban user (`isBanned: true`, record in `banned_users`).
   * Temporary suspension (`isSuspended: true`).
   * View full transaction history and referral network tree.

4. **Dynamic In-App Banners & Announcements:**
   * Publish promotional banners to `/banners/{bannerId}` with target actions (Tournament, Wallet, Support, Leaderboard).
   * Broadcast push notifications to `/notifications`.

5. **Platform Settings & Maintenance:**
   * Toggle `maintenanceMode`, set ETA, and toggle `forceUpdate` in `/system_config/app_config`.

---
*Generated for the Velorix Platform ecosystem. Use this document as the master context for all client, backend, and admin panel implementations.*
