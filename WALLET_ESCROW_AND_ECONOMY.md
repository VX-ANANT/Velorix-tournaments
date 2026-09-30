# VELORIX ESPORTS ENGINE — WALLET ESCROW & ECONOMY SPECIFICATION (`WALLET_ESCROW_AND_ECONOMY.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=blur&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=WALLET%20ESCROW%20%26%20ECONOMY&fontSize=38&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=DOUBLE-ENTRY%20LEDGER%20%E2%80%A2%20ATOMIC%20ESCROW%20%E2%80%A2%20TIERED%20REFERRALS%20%E2%80%A2%20UPI%20SETTLEMENT&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Wallet Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/ACCOUNTING-DOUBLE_ENTRY_LEDGER-22C55E?style=for-the-badge&logo=cashapp&logoColor=white" alt="Double Entry" />
  <img src="https://img.shields.io/badge/SETTLEMENT-UPI_INSTANT_INTENT-00E676?style=for-the-badge&logo=googlepay&logoColor=black" alt="UPI Instant" />
  <img src="https://img.shields.io/badge/SECURITY-ANTI_EXPLOIT_LOCK-FFCA28?style=for-the-badge&logo=securityscorecard&logoColor=black" alt="Anti Exploit" />
  <img src="https://img.shields.io/badge/TOKENOMICS-DUAL_CURRENCY_PEGGED-7F52FF?style=for-the-badge&logo=solana&logoColor=white" alt="Dual Currency" />
</p>

</div>

---

## 1. DUAL-TOKEN CURRENCY SPECIFICATION

The VeloRix gaming ecosystem operates on a transparent, two-tier economy designed to reward skill, consistency, and referral engagement without currency inflation:

```
┌──────────────────────────────────────┐        ┌──────────────────────────────────────┐
│       VT TOKENS (PLAYABLE / CASH)    │        │       COMBAT TOKENS (IN-GAME)        │
├──────────────────────────────────────┤        ├──────────────────────────────────────┤
│ • Hard Currency pegged 1:1 with INR. │        │ • Soft Loyalty Currency.             │
│ • 1 VT Token = ₹1.00 INR.            │        │ • Earned via Daily/Weekly Missions.  │
│ • Fully withdrawable via UPI/Bank.   │        │ • Earned via Daily Login Streaks.    │
│ • Used for tournament entry fees     │        │ • Convertible: 10 Tokens = 1 VT.     │
│   and prize pool distributions.      │        │ • Daily Mission Cap: 100 Tokens/day. │
└──────────────────────────────────────┘        └──────────────────────────────────────┘
```

---

## 2. ATOMIC WALLET ESCROW ENGINE

### 2.1 The Escrow Invariant
Whenever a player joins a tournament, their entry fee is **never** transferred directly to general revenue or unallocated pools. It is transferred into a **Dedicated Match Escrow Vault** associated with the specific tournament ID.

### 2.2 Escrow State Machine
```
[User Wallet: ₹150]
        │
        ▼ User registers for ₹30 Match
[Atomic Escrow Deduction]
        ├──► User Playable Balance: ₹120
        └──► Escrow Vault: +₹30 (Status: LOCKED_IN_ESCROW)
                    │
        ┌───────────┴───────────────────────────────┐
        ▼ Match Concluded Authoritatively            ▼ Match Cancelled / Quorum Failed
[Prize Distribution Execution]              [Automated Escrow Refund]
        ├──► Winners' Balances Credited             └──► 100% Refunded to User Wallet
        └──► Escrow Vault: Depleted to 0                 (User Playable Balance: ₹150)
```

### 2.3 Strict Ledger Schema
Every transaction generates an immutable double-entry ledger event under `/users/{uid}/transactions/{txn_id}`:
```json
{
  "id": "TXN_ESCROW_984102_A",
  "userId": "usr_vx_88912",
  "type": "TOURNAMENT_ESCROW_LOCK",
  "amount": 30.00,
  "currency": "VT",
  "tournamentId": "tourney_ff_101",
  "slotNumber": 14,
  "balanceBefore": 150.00,
  "balanceAfter": 120.00,
  "timestamp": 1727099400000,
  "status": "LOCKED",
  "idempotencyKey": "IDEM_JOIN_FF101_USR88912_S14"
}
```

---

## 3. DEPOSIT WORKFLOW & UTR RATE LIMITING

### 3.1 Flow Architecture
```
Player Selects Deposit (₹50, ₹100, ₹500)
             │
             ▼
Dynamic UPI Intent Generated (`upi://pay?pa=veloxyra.anant@fam&am=...`)
             │
             ▼
User Completes Payment in UPI App (GPay / PhonePe / Paytm / BHIM)
             │
             ▼
User Returns to VeloRix & Enters 12-Digit Banking UTR Number
             │
             ▼
[UserRateLimiter] Verification:
  • Checks if user submitted another UTR within 30 seconds (Anti-Spam)
  • Checks if UTR string matches `^[0-9]{12}$` format
  • Checks if UTR has already been submitted in `/deposits` (Anti-Replay)
             │
             ▼
Accepted: Pushed to `/deposits/{deposit_id}` with status "PENDING"
             │
             ▼
Admin / Payment Webhook Confirms Settlement ──► Wallet Incremented + Haptic SFX
```

### 3.2 Anti-Replay Guard:
If any user attempts to submit a UTR that has previously been recorded or processed anywhere in the system, the deposit is rejected immediately with code `DUPLICATE_UTR_REPLAY_ATTEMPT`.

---

## 4. TIERED REFERRAL & LIFETIME DEPOSIT COMMISSION ENGINE

To encourage community squad building without vulnerability to multi-account farming, referral commissions are tied strictly to **downline deposit activity**:

### 4.1 Registration-Only Referral Linking
- Referral codes can **only** be entered during initial account registration on the `AuthScreen`.
- In-app entry of referral codes after registration is disabled to prevent retroactive kickback exploitation.
- Self-referral detection checks: `referrerUid != newUid`, device hardware hash mismatch, IP subnet clustering filters.

### 4.2 Commission Tiers

| Tier Name | Active Downline Players | Lifetime Deposit Commission Rate |
| :--- | :--- | :--- |
| **Recruit Squad** | 1 – 4 Active Players | **5% of all deposited funds** |
| **Tactical Veteran** | 5 – 14 Active Players | **8% of all deposited funds** |
| **Elite Commander** | 15 – 49 Active Players | **12% of all deposited funds** |
| **Apex Syndicate** | 50+ Active Players | **15% of all deposited funds** |

*Note: Commissions are credited instantly as withdrawable VT Tokens upon successful deposit verification of the referred player.*

---

## 5. WITHDRAWAL & PAYOUT POLICY

1. **Minimum Withdrawal Limit**: **₹50.00 VT Tokens**.
2. **Maximum Withdrawal Limit (Unverified Tier)**: ₹2,000.00 per 24-hour cycle.
3. **Maximum Withdrawal Limit (KYC Verified Tier)**: ₹10,000.00 per 24-hour cycle.
4. **Valid Payout Channels**:
   - UPI ID (e.g. `username@okhdfcbank`, `mobile@paytm`, `handle@fam`).
   - Direct Bank IMPS/NEFT (Account Number + IFSC Code).
5. **Settlement SLA**:
   - Automated Instant Disbursal: 95% of withdrawals under ₹500 processed within 5 minutes.
   - Manual Security Review: Withdrawals above ₹1,000 or accounts flagged by the Sentinel Anti-Cheat engine undergo manual review within 2 to 4 hours.
6. **Zero Gateway Deductions**: 100% of the requested amount is dispatched; no hidden service fees are taken from tournament winnings.

---

<sub>Authored and Maintained for the **VeloRix Esports Engine** by **VX-ANANT**.</sub>
