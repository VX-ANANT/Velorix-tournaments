# VELORIX ESPORTS ENGINE — ANTI-CHEAT, HARDWARE & VPN INTEGRITY POLICY (`ANTI_CHEAT_AND_VPN_POLICY.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=blur&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=ANTI-CHEAT%20%26%20VPN%20POLICY&fontSize=38&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=ZERO%20TOLERANCE%20%E2%80%A2%20VPN%20FILTER%20%E2%80%A2%20HARDWARE%20INTEGRITY%20%E2%80%A2%20NEURAL%20SENTRY&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Anti-Cheat Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/SECURITY-HARDWARE_ISOLATION-FF3366?style=for-the-badge&logo=wireguard&logoColor=white" alt="Hardware Isolation" />
  <img src="https://img.shields.io/badge/SENTRY-GEMINI_3.6_FLASH_OCR-8E75B2?style=for-the-badge&logo=google&logoColor=white" alt="Gemini Neural OCR" />
  <img src="https://img.shields.io/badge/NETWORK-ANTI_VPN_PROXY_SHIELD-0284C7?style=for-the-badge&logo=cloudflare&logoColor=white" alt="Anti VPN" />
  <img src="https://img.shields.io/badge/ENFORCEMENT-PERMANENT_HWID_BAN-E11D48?style=for-the-badge&logo=scylladb&logoColor=white" alt="Permanent HWID Ban" />
</p>

</div>

---

## 1. MISSION & FAIR PLAY MANDATE

The VeloRix platform exists to provide an authentic, meritocratic arena where competitive mobile gamers (Free Fire, BGMI) can demonstrate genuine human skill and tactical coordination. The introduction of synthetic aim assistance, memory modifications, wallhacks, network spoofing, or device emulation into mobile-only divisions undermines the sanctity of competitive esports and is met with swift, automated, irreversible sanctions.

---

## 2. VPN, PROXY & NETWORK SPOOFING POLICY

### 2.1 Explicit Prohibition
The active use of **Virtual Private Networks (VPNs)**, **Datacenter Proxies**, **Tor Exit Nodes**, or **Packet Manipulation Tools** during match registration, custom room play, or score submission is **strictly prohibited**.

### 2.2 Rationale for Prohibition:
1. **Ping Manipulation & Desynchronization**: Players using distant VPN nodes artificially inflate their round-trip latency, causing erratic "rubber-banding" and desynchronized hit-boxes that unfairly disadvantage legitimate players.
2. **Geo-Restriction Evasion**: Evading state compliance policies (e.g. regions with local skill-gaming regulations) via proxy masking violates regulatory compliance.
3. **Sybil & Multi-Account Farming**: Proxies are frequently utilized by botfarms to cycle multiple accounts for referral fraud or automated slot hoarding.

### 2.3 Detection & Enforcement Engine:
- **Network Interface Inspection**: Android client audits active network capabilities (`NetworkCapabilities.TRANSPORT_VPN`).
- **Autonomous IP Quality Score**: Incoming connections with high fraud risk scores or known commercial hosting IP addresses (AWS, DigitalOcean, OVH, NordVPN, ExpressVPN) are automatically denied room credential access.
- **Penalty**: Immediate disqualification from the match without entry fee refund. Repeat violations result in a 7-day account suspension.

---

## 3. EMULATOR VS. MOBILE HARDWARE SEGREGATION

### 3.1 Division Classifications:
- **`MOBILE_ONLY` Divisions**: Reserved exclusively for physical handheld smartphones and tablets (iOS and Android).
- **`OPEN_HARDWARE` Divisions**: Allows PC emulators (BlueStacks, LDPlayer, Gameloop, Nox) to compete against fellow emulator squads.

### 3.2 Mobile-Only Hardware Inspection Heuristics:
The client executes non-intrusive runtime hardware fingerprinting upon launch:
```kotlin
// Multi-factor Emulator Heuristics
val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
    Build.FINGERPRINT.startsWith("unknown") ||
    Build.MODEL.contains("google_sdk") ||
    Build.MODEL.contains("Emulator") ||
    Build.MODEL.contains("Android SDK built for x86") ||
    Build.MANUFACTURER.contains("Genymotion") ||
    (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
    Build.HARDWARE == "goldfish" ||
    Build.HARDWARE == "ranchu" ||
    Build.PRODUCT == "sdk_gphone64_arm64"
```

If a user launches a `MOBILE_ONLY` tournament from an emulator:
1. The app flags the session with error code `EMULATOR_IN_MOBILE_DIVISION`.
2. The user is prevented from clicking "Join Match".
3. An advisory dialog appears directing them to join the designated PC/Emulator bracket.

---

## 4. ROOT, JAILBREAK & DYNAMIC INJECTION INTEGRITY

### 4.1 Prohibited Hooking Frameworks:
- **Frida & Xposed Frameworks**: Dynamic binary instrumentation tools capable of modifying memory at runtime.
- **Magisk / KernelSU Zygisk Modules**: Root hiding modules used to mask unauthorized package alterations.
- **Dual-Space & Virtual App Cloners**: Parallel spaces running modified binaries with virtualized package identities.

### 4.2 Play Integrity Verification:
- Client validates package signature against Google Play distribution signing keys.
- If tampering or root-level memory injection is detected during an active match session, the player’s credentials are invalidated immediately.

---

## 5. NEURAL SENTRY: GEMINI 3.6 FLASH OCR & ANOMALY DETECTION

VeloRix integrates Google's **Gemini 3.6 Flash** multimodal model to perform automated post-match forensic review:

```
Match Victory Screenshot Uploaded
              │
              ▼
Gemini 3.6 Flash Multimodal Analysis:
  ├─ Extract: Player In-Game Names (IGNs)
  ├─ Extract: Game Character UIDs
  ├─ Extract: Rank Placement (#1 Booyah / #1 Chicken Dinner)
  ├─ Extract: Kill Count & Damage Dealt
  └─ Extract: Timestamp & Room Watermark
              │
              ▼
Cross-Reference with Firebase Registered Roster:
  ├─ Does Player UID match the registered slot occupant?
  ├─ Does total kills sum match room limit (max 48 kills for solo lobby)?
  └─ Are kill-per-minute metrics within human physiological threshold?
              │
         ┌────┴──────────────────────────┐
         ▼ Valid                         ▼ Discrepancy Flagged
Score Verified & Stored          Escalated to Human Referees
                                 Player Notified in Dispute Window
```

---

## 6. SANCTIONS & DISCIPLINARY ESCALATION MATRIX

| Violation Category | Detection Method | First Offense | Second Offense | Third Offense |
| :--- | :--- | :--- | :--- | :--- |
| **Active VPN in Match** | Network Interface Audit | Match Disqualification (No Refund) | 3-Day Suspension | 30-Day Suspension |
| **Emulator in Mobile Lobby** | Hardware Fingerprint Heuristic | Match Disqualification | 7-Day Suspension | Permanent Ban from Mobile Brackets |
| **Teaming / Collusion** | In-Game Video Spectator Review | Disqualification + Zero Prize | 30-Day Suspension | Permanent Account Ban |
| **Memory Injection / ESP / Aimbot** | Client Integrity + Anomaly Detection | **Immediate Permanent Ban + Device Fingerprint Blacklist + 100% Balance Forfeiture** | — | — |
| **Fake Screenshot / Score Fraud** | Gemini OCR Hash & Telemetry Check | 7-Day Account Suspension | Permanent Account Ban | — |

---

## 7. APPEALS & ARBITRATION PROCESS

Players who believe an automated penalty was issued in error may file an appeal through the following process:
1. Navigate to **In-App Settings → 24/7 AI Support Desk**.
2. Select **"Dispute Sanction / Ban Appeal"**.
3. Upload unedited, full-screen video recording (screen record of match replay or hand-cam POV).
4. Appeals are arbitrated by the Senior Fair Play Committee within **24 hours**. Decisions following human review are final and binding.

---

<sub>Authored and Maintained for the **VeloRix Esports Engine** by **VX-ANANT**.</sub>
