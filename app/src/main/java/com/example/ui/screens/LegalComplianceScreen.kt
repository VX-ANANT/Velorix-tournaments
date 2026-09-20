package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.GffDevanagariFontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.LegalTab
import com.example.ui.components.stretchOverscroll

/**
 * LegalComplianceScreen.kt
 *
 * Minimalist, full-page Statutory & Legal Jurisprudence architecture designed with
 * clean Vercel & Xiaomi HyperOS aesthetic principles:
 * - Pure AMOLED Black (#000000) canvas with subtle obsidian borders (#1F1F1F)
 * - Micro-tabs with subtle border highlighting (no heavy military boxes)
 * - Top-to-bottom clean scannable typographic rhythm with generous negative space
 * - Minimal, ultra-smooth entry animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalComplianceScreen(
    initialTab: LegalTab = LegalTab.TERMS,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabScrollState = rememberScrollState()

    // Pure Minimalist Palette (Vercel / Xiaomi style)
    val bgBlack = Color(0xFF000000)
    val cardBg = Color(0xFF0A0A0A)
    val borderSubtle = Color(0xFF1E1E1E)
    val textPrimary = Color(0xFFEDEDED)
    val textSecondary = Color(0xFF888888)
    val textMuted = Color(0xFF555555)
    val accentVercel = Color(0xFFFFFFFF)

    Scaffold(
        containerColor = bgBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Legal & Compliance",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "PROG Act 2025 • MeitY 2026 • DPDP 2023",
                            fontSize = 11.sp,
                            color = textSecondary,
                            letterSpacing = 0.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgBlack
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tabs - Minimalist Vercel / Xiaomi pill bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LegalTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    val pillBg = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF0D0D0D)
                    val pillTextColor = if (isSelected) Color(0xFF000000) else Color(0xFF888888)
                    val pillBorder = if (isSelected) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = pillBg,
                        border = BorderStroke(1.dp, pillBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                selectedTab = tab
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = pillTextColor,
                                letterSpacing = (-0.2).sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = borderSubtle, thickness = 0.8.dp)

            // Content Body with top-to-bottom structure and smooth animated switch
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                        slideInVertically(animationSpec = tween(220, easing = LinearOutSlowInEasing), initialOffsetY = { 18 }))
                        .togetherWith(
                            fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
                        )
                },
                label = "legal_tab_content",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .stretchOverscroll()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp)
                ) {
                    item {
                        when (targetTab) {
                            LegalTab.TERMS -> TermsPage()
                            LegalTab.PRIVACY -> PrivacyPage()
                            LegalTab.FAIR_PLAY -> FairPlayPage()
                            LegalTab.REFUNDS -> RefundsPage()
                            LegalTab.RESPONSIBLE -> ResponsiblePage()
                            LegalTab.LEGAL_STATUS -> LegalStatusPage()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        GrievanceFooter(
                            onEmailClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:anantisback47@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "VeloRix Legal Inquiry / Grievance")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Contact: anantisback47@gmail.com", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- PAGES ----------------

@Composable
private fun TermsPage() {
    Column {
        PageHeroHeader(
            tag = "TERMS OF SERVICE",
            title = "Covenant of Skill & Participation",
            subtitle = "Binding legal agreement ratified under the Indian Contract Act, 1872 and Article 19(1)(g) of the Constitution of India."
        )

        SectionBlock(
            number = "01",
            title = "Architectural Scope & Safe Harbor",
            content = "VeloRix functions strictly as an electronic sports contest organizer and intermediary infrastructure under Section 79 of the Information Technology Act, 2000. VeloRix provides competitive matchmaking for mobile gaming titles including Garena Free Fire and is not affiliated with, endorsed by, or sponsored by Garena International."
        )

        SectionBlock(
            number = "02",
            title = "18+ Age Majority & Geographical Geofencing",
            bullets = listOf(
                "Mandatory Legal Majority: In strict compliance with the PROG Act 2025 and MeitY PROG Rules 2026, participation in cash prize tournament rooms is strictly restricted to individuals who have achieved 18+ years of age. Minors are restricted solely to non-monetary practice scrims.",
                "Territorial Geofencing: In compliance with state enactments, participation from the states of Assam, Odisha, Telangana, Nagaland, Andhra Pradesh, and Sikkim is barred. VPN or location circumvention triggers permanent account suspension."
            )
        )

        SectionBlock(
            number = "03",
            title = "Escrow Discipline, GST 2025 & TDS",
            bullets = listOf(
                "Entry fees remain secured in deterministic escrow until match conclusion and verification.",
                "Prizes liquidate directly to the combatant's verified UPI VPA handle.",
                "Statutory Tax Deducted at Source (TDS) under Section 194BA of the Indian Income Tax Act (30%) is calculated and remitted in full accordance with Ministry of Finance guidelines."
            )
        )

        SectionBlock(
            number = "04",
            title = "Anti-Sybil & Account Invariants",
            content = "Each player is permitted exactly one verified terminal identity. Multi-accounting, account sharing, or credential leakage to unauthorized third parties results in immediate room disqualification and forfeiture of entry funds."
        )
    }
}

@Composable
private fun PrivacyPage() {
    Column {
        PageHeroHeader(
            tag = "DATA SOVEREIGNTY",
            title = "Privacy Policy & Zero Telemetry Sharing",
            subtitle = "Fully compliant with the Digital Personal Data Protection (DPDP) Act, 2023 and the Information Technology SPDI Rules, 2011."
        )

        SectionBlock(
            number = "01",
            title = "Data Protection Commitment",
            content = "We adhere to strict data minimization principles. Zero marketing data brokers. Zero personal data commercialization. We collect only what is strictly required to verify fair competition and fulfill payout obligations."
        )

        SectionBlock(
            number = "02",
            title = "Data Points We Process",
            bullets = listOf(
                "Account Identity: Verified mobile number and Google OAuth profile for authentication and 18+ age verification.",
                "In-Game Identifiers: In-game nickname and Free Fire UID strictly for room allocation and score calculation.",
                "Payment Telemetry: Verified UPI handle for instant withdrawal liquidation. We never store debit/credit card numbers or banking PINs.",
                "Fair Play Integrity: Hardware hash and network latency variance indicators to detect emulators and ban evaders."
            )
        )

        SectionBlock(
            number = "03",
            title = "Data Retention & Right to Erasure",
            content = "Under Section 12 of the DPDP Act 2023, you hold the right to demand complete erasure of your data. Triggering 'Delete Account' in App Settings queues all personal identifiers for complete deletion within 7 business days."
        )
    }
}

@Composable
private fun FairPlayPage() {
    Column {
        PageHeroHeader(
            tag = "COMPETITIVE INTEGRITY",
            title = "Sentinel Anti-Cheat & Fair Play Protocols",
            subtitle = "Zero tolerance for unfair software manipulation, unauthorized hardware advantage, or collusive behavior."
        )

        SectionBlock(
            number = "01",
            title = "Prohibited Exploits & Hardware Bans",
            bullets = listOf(
                "Aimbots, Silent Aim, ESP, Wallhacks, and Recoil Modifiers.",
                "Memory injection, hex-editing, or modified APK/game clients.",
                "Hardware Blacklist: Detected violations trigger irreversible device ID blacklisting and complete tournament prize forfeiture."
            )
        )

        SectionBlock(
            number = "02",
            title = "Teaming & Cross-Squad Collusion",
            content = "Intentional teaming with opposing squads, match-fixing, or passive non-engagement in solo/duo rooms violates the fundamental integrity of esports competition and carries an immediate 30-day competitive ban."
        )

        SectionBlock(
            number = "03",
            title = "PC Emulator Restrictions",
            content = "Rooms tagged as 'Mobile Only' strictly bar PC emulators (BlueStacks, LDPlayer, Nox, MSI App Player). Real-time telemetry checks remove unauthorized instances prior to match launch."
        )
    }
}

@Composable
private fun RefundsPage() {
    Column {
        PageHeroHeader(
            tag = "FINANCIAL INTEGRITY",
            title = "Escrow & Payout Arbitration",
            subtitle = "Deterministic escrow security and transparent settlement across automated UPI rails."
        )

        SectionBlock(
            number = "01",
            title = "Match Cancellation & Quorum Failure",
            content = "If a match is aborted due to game server downtime, network disruption, or failure to meet minimum room quorum, 100% of the reserved entry fee is automatically refunded back to the player's wallet balance."
        )

        SectionBlock(
            number = "02",
            title = "Player Voluntary Cancellation",
            bullets = listOf(
                "More than 15 minutes before scheduled match start: 100% instant wallet refund upon room exit.",
                "Under 15 minutes (post Room ID & Password release): Non-refundable to prevent unauthorized room credential leakage."
            )
        )

        SectionBlock(
            number = "03",
            title = "UPI Settlement Latency",
            content = "Standard withdrawals process near-instantly. In the rare event of banking switch downtime, transactions settle within 24–48 hours. Retain your 12-digit UPI UTR reference for support inquiries."
        )
    }
}

@Composable
private fun ResponsiblePage() {
    Column {
        PageHeroHeader(
            tag = "PLAYER WELFARE",
            title = "Responsible Gaming & 18+ Safeguards",
            subtitle = "Esports is a competitive discipline of motor skill and strategy. It should never be treated as a livelihood, investment, or speculative instrument."
        )

        SectionBlock(
            number = "01",
            title = "Uncompromising 18+ Age Barrier",
            content = "Participation in entry-fee contests is strictly barred for individuals under 18 years of age. Accounts identified as operated by minors are closed immediately and unutilized balances returned."
        )

        SectionBlock(
            number = "02",
            title = "Player Self-Exclusion & Limits",
            bullets = listOf(
                "Never participate with funds allocated for daily living necessities.",
                "Never borrow money, credit, or take loans to play.",
                "Voluntary Cooling-Off: Players can request voluntary account freeze periods ranging from 7 days to 6 months by reaching out to support."
            )
        )

        SectionBlock(
            number = "03",
            title = "National Support Helplines",
            bullets = listOf(
                "National Mental Health Helpline (KIRAN): 1800-599-0019 (24/7 Toll-Free)",
                "NIMHANS Helpline: 080-46110007"
            )
        )
    }
}

@Composable
private fun LegalStatusPage() {
    Column {
        PageHeroHeader(
            tag = "STATUTORY JURISPRUDENCE",
            title = "PROG Act 2025 & MeitY PROG Rules 2026",
            subtitle = "Statutory compliance architecture establishing VeloRix as a permissible skill-based electronic sports platform."
        )

        SectionBlock(
            number = "01",
            title = "Promotion & Regulation of Online Gaming Act, 2025",
            content = "VeloRix operates in full compliance with the Promotion and Regulation of Online Gaming Act, 2025 (PROG Act 2025) enacted by the Parliament of India, clearly separating permissible skill-based competitive electronic sports from prohibited wagering."
        )

        SectionBlock(
            number = "02",
            title = "MeitY PROG Rules, 2026 & OGAI Directives",
            bullets = listOf(
                "Classification of Esports: Under Sections 4 & 5 of the MeitY PROG Rules 2026, battle royale contests requiring hand-eye coordination, rapid spatial decision-making, and game knowledge are classified as permissible games of skill.",
                "Online Gaming Authority of India (OGAI): VeloRix aligns with the operational directives of the Online Gaming Authority of India (OGAI) established in April 2026, including zero algorithmic manipulation, auditable match histories, and prompt player grievance resolution."
            )
        )

        SectionBlock(
            number = "03",
            title = "Supreme Court Judicial Precedents",
            content = "The Supreme Court of India in State of Bombay v. R.M.D. Chamarbaugwala (1957) and Dr. K.R. Lakshmanan v. State of Tamil Nadu (1996) definitively settled that competitions where success depends substantially on player skill, dexterity, and judgment are protected under Article 19(1)(g) of the Constitution of India. Section 12 of the Public Gambling Act, 1867 specifically exempts games of mere skill."
        )

        SectionBlock(
            number = "04",
            title = "Statutory Safe Harbor (IT Act Section 79)",
            content = "VeloRix functions strictly as an electronic sports intermediary under Section 79 of the Information Technology Act 2000, adhering fully to MeitY Intermediary Rules, DPDP 2023 rules, and CBDT Section 194BA TDS statutory deductions."
        )
    }
}

// ---------------- UI BUILDING BLOCKS (MINIMAL VERCEL STYLE) ----------------

@Composable
private fun PageHeroHeader(
    tag: String,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        // Tag badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = tag,
                fontSize = 10.sp,
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA1A1A6),
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEDEDED),
            letterSpacing = (-0.4).sp,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF888888),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SectionBlock(
    number: String,
    title: String,
    content: String? = null,
    bullets: List<String>? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0A0A0A),
        border = BorderStroke(1.dp, Color(0xFF1C1C1C)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontFamily = GffDevanagariFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF555555)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEDEDED),
                    letterSpacing = (-0.2).sp
                )
            }

            if (content != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF999999),
                    lineHeight = 18.sp
                )
            }

            if (bullets != null) {
                Spacer(modifier = Modifier.height(8.dp))
                bullets.forEach { bullet ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = bullet,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF999999),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GrievanceFooter(
    onEmailClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF080808),
        border = BorderStroke(1.dp, Color(0xFF181818)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "GRIEVANCE REDRESSAL OFFICER",
                fontSize = 10.sp,
                fontFamily = GffDevanagariFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666),
                letterSpacing = 0.6.sp
            )

            Text(
                text = "For statutory inquiries, DPDP data requests, or dispute arbitration:",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF121212),
                border = BorderStroke(1.dp, Color(0xFF222222)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEmailClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✉ anantisback47@gmail.com",
                        fontSize = 12.sp,
                        fontFamily = GffDevanagariFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE5E5EA)
                    )
                }
            }
        }
    }
}
