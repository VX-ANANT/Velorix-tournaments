# VELORIX ESPORTS ENGINE — DESIGN SYSTEM & SPECIFICATION (`DESIGN.md`)

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=0,2,11,20&height=200&section=header&text=VELORIX%20DESIGN%20SYSTEM&fontSize=42&fontColor=ffffff&fontAlignY=38&animation=twinkling&desc=PURE%20AMOLED%20BLACK%20•%20SUBTLE%20CYBERNETIC%20•%20M3%20COMPOSE%20SYSTEM&descAlignY=62&descAlign=50&descSize=14" width="100%" alt="VeloRix Design System Banner" />

<p align="center">
  <img src="https://img.shields.io/badge/CANVAS_FOUNDATION-AMOLED_TRUE_BLACK_%23000000-000000?style=for-the-badge&logo=android&logoColor=white" alt="AMOLED True Black" />
  <img src="https://img.shields.io/badge/FRAMEWORK-JETPACK_COMPOSE_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose M3" />
  <img src="https://img.shields.io/badge/REFRESH_RATE-60%2F120HZ_ENFORCED-00E676?style=for-the-badge&logo=fastapi&logoColor=black" alt="High Refresh Rate" />
  <img src="https://img.shields.io/badge/AUDIO_CORE-MJ_INSPIRED_STUDIO_SFX-A855F7?style=for-the-badge&logo=spotify&logoColor=white" alt="Audio Engine" />
  <img src="https://img.shields.io/badge/ACCESSIBILITY-WCAG_2.1_AAA-10B981?style=for-the-badge&logo=w3c&logoColor=white" alt="WCAG AAA" />
</p>

</div>

---

## 1. DESIGN PHILOSOPHY & CORE AESTHETIC DIRECTIVES

VeloRix Esports is engineered with an uncompromising **Pure Cybernetic Minimalism** ethos tailored for professional esports competitors. It rejects cheap gimmicks, noisy visual clutter, and oversaturated neon gradients in favor of an **ultra-clean, battery-efficient, AMOLED-optimized experience**.

### Foundational Invariants:
1. **Absolute Zero Background (`#000000`)**:
   - The foundational canvas background of the entire app is strictly pure `#000000` (AMOLED Black).
   - This ensures 0 mA pixel emission on OLED/AMOLED panels, prevents eye fatigue during high-stakes night tournaments, and delivers infinite contrast ratios.
2. **Subtle & Controlled Accent Hierarchy**:
   - No blinding electric greens or radioactive cyan fills.
   - Primary accents use muted emerald greens (`#10B981`, `#059669`), deep sapphire blues (`#0284C7`, `#38BDF8`), cybernetic amethysts (`#A855F7`), and laser rubies (`#EF4444`, `#DC2626`).
3. **Layered Elevation Without Muddy Grays**:
   - Elevation is achieved through subtle border strokes (`1.dp` @ `#262626` / `#1F2937`) and deep obsidian container fills (`#0A0A0A`, `#0D0D0D`, `#121212`) rather than light-gray drop shadows.
4. **Haptic & Auditory Tactility**:
   - Every primary interactive affordance is paired with precision micro-haptic clicks (`HapticFeedbackType.TextHandleMove`) and low-latency acoustic audio feedback.
5. **High Refresh Rate Hardware Enforcement**:
   - On display panels supporting 90Hz, 120Hz, or 144Hz, the engine requests maximum display mode parameters (`preferredDisplayModeId`) via window attributes on activity resume.

---

## 2. COLOR PALETTE & DESIGN TOKENS

The design system is implemented in [Theme.kt](app/src/main/java/com/example/ui/theme/Theme.kt) and adheres to [Material Design 3 Token Guidelines](https://m3.material.io/styles/color/the-color-system/tokens).

### 2.1 Color Matrix

| Token Name | Hex Code | Compose Resource | Usage Specification |
| :--- | :--- | :--- | :--- |
| **Canvas Background** | `#000000` | `Color(0xFF000000)` | Base screen background (Never light gray) |
| **Card Surface (Deep)** | `#0D0D0D` | `Color(0xFF0D0D0D)` | Primary tournament cards, metric surfaces |
| **Elevated Surface** | `#141414` | `Color(0xFF141414)` | Modals, bottom sheets, active input fields |
| **Subtle Stroke/Border**| `#262626` | `Color(0xFF262626)` | 1dp card borders, dividers, chip outlines |
| **Highlight Stroke** | `#404040` | `Color(0xFF404040)` | Active focused borders, selected tabs |
| **Cyber Emerald** | `#10B981` | `Color(0xFF10B981)` | Success states, wallet balance, active slots |
| **Muted Mint** | `#34D399` | `Color(0xFF34D399)` | Labels, secondary success indicators |
| **Sapphire Blue** | `#0284C7` | `Color(0xFF0284C7)` | Primary action buttons, CTA accents |
| **Electric Cyan** | `#38BDF8` | `Color(0xFF38BDF8)` | Information badges, telemetry indicators |
| **Amethyst Royal** | `#A855F7` | `Color(0xFFA855F7)` | VIP founder badges, audio engine icons |
| **Crimson Laser** | `#EF4444` | `Color(0xFFEF4444)` | Danger, penalty notices, banned screens |
| **Amber Scrim** | `#F59E0B` | `Color(0xFFF59E0B)` | Under-maintenance, scheduled alerts, warnings |
| **Text Primary** | `#FFFFFF` | `Color(0xFFFFFFFF)` | Titles, headers, monetary figures |
| **Text Secondary** | `#A1A1AA` | `Color(0xFFA1A1AA)` | Body copy, timestamps, rules text |
| **Text Muted** | `#71717A` | `Color(0xFF71717A)` | Subtle hints, slot counts, footers |

---

## 3. TYPOGRAPHY & FONT HIERARCHY

VeloRix utilizes high-legibility geometric and modern sans-serif typefaces configured via the Android typography toolchain.

### 3.1 Font Families Used:
- **[Plus Jakarta Sans](https://fonts.google.com/specimen/Plus+Jakarta+Sans)**: Primary UI font for buttons, body labels, dialog texts, and metadata chips.
- **[Orbitron](https://fonts.google.com/specimen/Orbitron)**: Stylized cybernetic heading font used for tournament titles, match countdown timers, and victory scores.
- **[Inter](https://fonts.google.com/specimen/Inter)**: High-precision numeric font used for wallet amounts, UTR verification numbers, and slot tables.
- **GFF Devanagari**: Native high-contrast font used for Hindi and regional language support.

### 3.2 Typography Scale

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Orbitron,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = 1.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = Color.White
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = Color(0xFFA1A1AA)
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 4. COMPONENT ARCHITECTURE & CANONICAL LAYOUTS

### 4.1 Tournament Card Component
The tournament card is the primary anchor of the app. It adheres to the **Zero-Glitch AMOLED Specification**:
- **Outer Shell**: `RoundedCornerShape(16.dp)` with `BorderStroke(1.dp, Color(0xFF262626))` on container `#0D0D0D`.
- **Top Row**: Game badge (e.g. Free Fire Battle Royale, BGMI Squad) + Status Chip (e.g. `UPCOMING`, `SLOTS FULL`, `LIVE`).
- **Telemetry Row**: Entry Fee (e.g., `₹50`), Prize Pool (e.g., `₹10,000`), Per Kill (e.g., `₹15`).
- **Slot Capacity Indicator**: Linear progress bar styled with `#10B981` (filling up) and `#262626` (empty track).
- **CTA Button**: Custom full-width action button triggering instant slot reservation modal.

### 4.2 Room Credentials Secret Reveal Card
Designed specifically for T-15 minute credential broadcasts:
- Locked State: Subtle pulsing lock icon with timestamp badge indicating release time.
- Unlocked State: Split dual-card displaying **Room ID** and **Password** with dedicated one-tap copy buttons.
- Auditory confirmation: Plays `sfx_smooth_stab.wav` on reveal and `sfx_bad_snap.wav` on copy.

### 4.3 Adaptive Window Size Support
VeloRix complies with [Android Adaptive Design Guidelines](https://developer.android.com/develop/ui/compose/layouts/adaptive):
- **Compact (`< 600dp`)**: Handheld portrait mode with bottom navigation bar (`NavigationBar`) and single-column LazyColumn feeds.
- **Medium (`600dp – 840dp`)**: Foldables and small tablets with expanded padding and multi-column tournament grids.
- **Expanded (`> 840dp`)**: Large tablets and Samsung DeX with `NavigationRail` and List-Detail canonical splits.

---

## 5. AUDIO DESIGN SYSTEM (MJ-INSPIRED SIGNATURE SFX)

Sound is an active feedback channel in VeloRix. Generated with mathematical studio synthesis and Quincy Jones-era production aesthetics, the app features 5 micro-sound effects (< 1.2s each) operated via [SoundEffectManager.kt](app/src/main/java/com/example/audio/SoundEffectManager.kt).

### 5.1 Sound Catalog & Situation Matrix

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        VELORIX SIGNATURE SFX CATALOG MATRIX                           │
├───────────────────────┬──────────┬──────────────┬──────────────────────────────────────┤
│ Audio Asset           │ Duration │ Frequency/BP │ Situation Trigger & Interaction      │
├───────────────────────┼──────────┼──────────────┼──────────────────────────────────────┤
│ sfx_billie_groove.wav │ 1.05s    │ 116 BPM      │ Tournament Join & Slot Confirmed     │
│                       │          │ 50-210 Hz    │ (Played alongside Confetti burst)    │
├───────────────────────┼──────────┼──────────────┼──────────────────────────────────────┤
│ sfx_smooth_stab.wav   │ 1.15s    │ Am9 Chord    │ Room ID & Pass Unlocked / Revealed   │
│                       │          │ 55 Hz Bass   │ Founder Pass Activation Celebration  │
├───────────────────────┼──────────┼──────────────┼──────────────────────────────────────┤
│ sfx_beatit_power.wav  │ 0.85s    │ E-Power Chor │ Wallet Cash Deposit Confirmed        │
│                       │          │ 48 Hz Drop   │ UPI Transaction Verification Success │
├───────────────────────┼──────────┼──────────────┼──────────────────────────────────────┤
│ sfx_bad_snap.wav      │ 0.45s    │ 2.2 kHz Snap │ One-Tap Copy Room ID / Password      │
│                       │          │ 860 Hz Rim   │ Quick Micro-Interactions & Nav Taps  │
├───────────────────────┼──────────┼──────────────┼──────────────────────────────────────┤
│ sfx_thriller_chime.wav│ 0.95s    │ G#m Shimmer  │ In-App Match Announcements & Alerts  │
│                       │          │ 415-740 Hz   │ Critical Admin Situation Broadcasts  │
└───────────────────────┴──────────┴──────────────┴──────────────────────────────────────┘
```

### 5.2 Audio Pipeline Implementation
- Implemented with low-latency native Android [SoundPool](https://developer.android.com/reference/android/media/SoundPool).
- Configured with `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` and `CONTENT_TYPE_SONIFICATION`.
- Preloads all assets into non-blocking audio memory on initial startup for zero-lag triggering.
- Respects user preferences via persistent shared preferences (`sfx_enabled`).

---

## 6. ACCESSIBILITY & TOUCH STANDARDS

All interactive elements strictly follow [Google Play Policy & Material 3 Accessibility Standards](https://m3.material.io/foundations/accessible-design/overview):
- **48dp × 48dp Minimum Touch Targets**: Verified across all icon buttons, copy triggers, and chips.
- **Color Contrast**: All text elements maintain a minimum contrast ratio of 4.5:1 against `#000000`, with critical headers exceeding 7:1 (WCAG AAA).
- **Screen Reader Semantics**: `contentDescription` provided for all interactive icons and status indicators.
- **Haptic Separation**: Differentiated haptics for warnings vs. successful operations.

---

## 7. EXTERNAL DESIGN LINKS & TOOLING REFERENCES

1. **Material Design 3 (M3) Specification**: [https://m3.material.io](https://m3.material.io)
2. **Jetpack Compose Design Guidelines**: [https://developer.android.com/develop/ui/compose](https://developer.android.com/develop/ui/compose)
3. **Android Adaptive Layouts**: [https://developer.android.com/develop/ui/compose/layouts/adaptive](https://developer.android.com/develop/ui/compose/layouts/adaptive)
4. **Google Fonts (Plus Jakarta Sans)**: [https://fonts.google.com/specimen/Plus+Jakarta+Sans](https://fonts.google.com/specimen/Plus+Jakarta+Sans)
5. **Google Fonts (Orbitron)**: [https://fonts.google.com/specimen/Orbitron](https://fonts.google.com/specimen/Orbitron)
6. **Google Fonts (Inter)**: [https://fonts.google.com/specimen/Inter](https://fonts.google.com/specimen/Inter)
7. **Android SoundPool Documentation**: [https://developer.android.com/reference/android/media/SoundPool](https://developer.android.com/reference/android/media/SoundPool)
8. **WCAG 2.1 Accessibility Standards**: [https://www.w3.org/WAI/standards-guidelines/wcag/](https://www.w3.org/WAI/standards-guidelines/wcag/)
9. **Capsule Render Engine**: [https://github.com/kyechan99/capsule-render](https://github.com/kyechan99/capsule-render)
10. **Shields.io Badges**: [https://shields.io](https://shields.io)

---

<div align="center">
  <b>VeloRix Design System • Engineered for Precision • Pure AMOLED Black Excellence</b>
</div>
