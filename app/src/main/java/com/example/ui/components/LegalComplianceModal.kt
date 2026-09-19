package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

enum class LegalTab(val title: String, val tag: String, val iconRes: Int) {
    TERMS("Terms of Service", "ART. 19(1)(g)", R.drawable.ic_legal_gavel),
    PRIVACY("Data Sovereignty", "DPDP ACT 2023", R.drawable.ic_untitledui_shield_tick),
    FAIR_PLAY("Sentinel Anti-Cheat", "ZERO TOLERANCE", R.drawable.ic_fair_play_swords),
    REFUNDS("Escrow & Liquidation", "INSTANT UPI", R.drawable.ic_refund_receipt),
    RESPONSIBLE("Operative Welfare", "18+ GATED", R.drawable.ic_age_18_badge),
    LEGAL_STATUS("Skill Jurisprudence", "SUPREME COURT", R.drawable.ic_untitledui_shield)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalComplianceModal(
    initialTab: LegalTab = LegalTab.TERMS,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF08090C),
            border = BorderStroke(1.5.dp, Color(0xFF1E2433)),
            tonalElevation = 16.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Cyber Scanline Laser Overlay
                CyberScanlineOverlay(
                    modifier = Modifier.fillMaxSize(),
                    lineColor = Color(0xFF38BDF8).copy(alpha = 0.04f),
                    glowColor = Color(0xFF00E5FF).copy(alpha = 0.08f)
                )

                // Corner Reticle Targeting Brackets
                TacticalReticleFrame(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    cornerLength = 18.dp,
                    strokeWidth = 2.dp,
                    accentColor = Color(0xFF00E5FF).copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        // Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        )
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_untitledui_shield_tick),
                                        contentDescription = "Verified Sentry",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "VELORIX CITADEL JURISDICTION",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "SOVEREIGN ESPORTS KERNEL // DPDP & STATUTORY SENTRY",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF161A24))
                                    .border(1.dp, Color(0xFF262D3D), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Telemetry Ribbon
                        TacticalTelemetryRibbon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            systemTag = "STATUTORY-LOCK",
                            protocolCode = "ARTICLE-19-1-G",
                            statusText = "CRYPTOGRAPHIC QUORUM",
                            accentColor = Color(0xFF10B981)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactive Horizontal Tabs with Tactical Cyber Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tabScrollState)
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LegalTab.values().forEach { tab ->
                                val isSelected = tab == selectedTab
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F121A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2433)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            selectedTab = tab
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = tab.iconRes),
                                                contentDescription = tab.title,
                                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                            )
                                        }
                                        Text(
                                            text = tab.tag,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF475569),
                                            modifier = Modifier.padding(start = 21.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(top = 12.dp),
                            color = Color(0xFF1E2433),
                            thickness = 1.dp
                        )

                        // Tab Content Body
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            val contentScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(contentScrollState)
                                    .padding(16.dp)
                            ) {
                                when (selectedTab) {
                                    LegalTab.TERMS -> TermsContent()
                                    LegalTab.PRIVACY -> PrivacyContent()
                                    LegalTab.FAIR_PLAY -> FairPlayContent()
                                    LegalTab.REFUNDS -> RefundsContent()
                                    LegalTab.RESPONSIBLE -> ResponsibleContent()
                                    LegalTab.LEGAL_STATUS -> LegalStatusContent()
                                }
                            }
                        }

                        // Bottom Tactical Affirmation Footer
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF0B0D13),
                            tonalElevation = 8.dp,
                            border = BorderStroke(1.dp, Color(0xFF1E2433))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SOVEREIGN REALM: REPUBLIC OF INDIA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "GRIEVANCE ARBITER: anantisback47@gmail.com",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        onDismissRequest()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "RATIFY & PROCEED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermsContent() {
    Column {
        LegalHighlightBadge(
            title = "INDOMITABLE SKILL CITADEL & BINDING JURISPRUDENCE",
            desc = "By securing tournament telemetry or accessing VeloRix custom rooms, you enter into an unalterable, legally binding covenant ratified under the Indian Contract Act 1872 and Article 19(1)(g) of the Constitution of India."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. ARCHITECTURAL SCOPE & NEUTRAL ARBITRATION")
        LegalParagraph("VeloRix functions as an independent, high-velocity electronic sports infrastructure facilitating pure skill competitions for mobile combat games including Garena Free Fire. VeloRix operates under statutory safe harbor and is not affiliated with or endorsed by Garena International.")

        LegalSectionTitle("2. STRICT 18+ AGE GATING & TERRITORIAL CITADEL")
        LegalParagraph("• Operatives must certify attainment of 18+ legal majority to engage in entry-fee combat rooms.\n• Geofenced Enactments: In absolute adherence to respective state statutes, cash competitions are strictly prohibited to residents within the state boundaries of Assam, Odisha, Telangana, Nagaland, Andhra Pradesh, and Sikkim. VPN or geolocation spoofing triggers autonomous hardware invalidation.")

        LegalSectionTitle("3. ESCROW DISCIPLINE & PRIZE LIQUIDATION")
        LegalParagraph("• Virtual wallet balances serve exclusively as tournament entry escrow.\n• Prize winnings liquidate solely into verified UPI VPA handles following automated match audit validation.\n• Statutory Tax Withholding (TDS) under Section 194BA of the Indian Income Tax Act is deducted automatically upon net winnings threshold fulfillment.")

        LegalSectionTitle("4. IDENTITY INVARIANTS & ANTI-SYBIL MANDATE")
        LegalParagraph("Each combatant is permitted exactly one verified terminal identity. Tampering with in-game Free Fire UIDs, credentials spoofing, or unauthorized room vector leakage executes immediate room ejection with total escrow forfeiture.")
    }
}

@Composable
private fun PrivacyContent() {
    Column {
        LegalHighlightBadge(
            title = "DEFENSE-GRADE CIPHER SOVEREIGNTY (DPDP ACT 2023)",
            desc = "Zero marketing telemetry brokers. Zero external data commercialization. Telemetry acquisition is restricted strictly to competitive integrity audit trails."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. SOVEREIGN STATUTORY MANDATE")
        LegalParagraph("Data architecture complies with the Digital Personal Data Protection (DPDP) Act 2023, the Information Technology Act 2000 (Sections 43A and 72A), and the IT SPDI Rules 2011.")

        LegalSectionTitle("2. CRYPTOGRAPHIC DATA POINTS COLLECTED")
        LegalParagraph("• Terminal Account Identity: Verified Mobile Number, Google OAuth token, and Date of Birth for 18+ majority validation.\n• Tactical In-Game Telemetry: IGN and Free Fire UID for cryptographic lobby slot allocation.\n• Payout Liquidity Rails: Verified UPI handle for instant liquidation. Zero credit/debit card numbers or ATM PINs are ever ingested.\n• Anti-Cheat Telemetry: Cryptographic hardware device hash and network latency variance indicators to terminate multi-accounting and ban evasion.")

        LegalSectionTitle("3. IRREVERSIBLE DATA PURGE RIGHTS")
        LegalParagraph("Combatants possess complete sovereignty to demand irreversible data scrubbing. Triggering 'Delete Account' in Terminal Settings executes total cryptographic scrubbing within 7 business days.")
    }
}

@Composable
private fun FairPlayContent() {
    Column {
        LegalHighlightBadge(
            title = "AUTONOMOUS SENTINEL ENGINE: ZERO-TOLERANCE EXECUTIONS",
            desc = "Powered by the VeloRix Sentinel Daemon v4.2 and Google Gemini 3.6 Flash neural forensics, match telemetries and scoreboard matrices undergo autonomous sub-atomic verification."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. EXPLOITS TRIGGERING HARDWARE TERMINATION")
        LegalParagraph("Deployment of Aimbots, Silent Aim, Wallhacks/ESP, Speed Hacks, Memory Invalidation Injectors, Modified Obfuscation Configs, or altered APK clients triggers IMMEDIATE PERMANENT HARDWARE BLACKLISTING and total forfeiture of accumulated vault liquidity.")

        LegalSectionTitle("2. SYNTHETIC COLLUSION & CROSS-TEAMING")
        LegalParagraph("Deliberate teaming with opposing squads, match-fixing, or passive griefing in Solo/Duo lobbies violates competitive parity, invoking immediate 30-day suspension and match prize nullification.")

        LegalSectionTitle("3. PC EMULATOR EXCLUSION CITADEL")
        LegalParagraph("Lobbies designated as 'Mobile Only' strictly prohibit PC emulators (BlueStacks, LDPlayer, Nox). Automated telemetry detection triggers instantaneous room kick.")
    }
}

@Composable
private fun RefundsContent() {
    Column {
        LegalHighlightBadge(
            title = "DETERMINISTIC ESCROW & INSTANT RECONCILIATION",
            desc = "100% automated refund liquidation upon room cancellation. Non-repudiation and transparent fiscal settlement across UPI rails."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. INFRASTRUCTURE NULLIFICATION PROTOCOL")
        LegalParagraph("If a tournament room is aborted due to game server maintenance, network downtime, or failure to achieve quorum, 100% of reserved entry fees are immediately returned to the combatant's wallet.")

        LegalSectionTitle("2. COMBATANT VOLUNTARY CANCELLATION")
        LegalParagraph("• More than 15 minutes prior to match schedule: 100% instant wallet refund.\n• Under 15 minutes (post Room ID/Pass revelation): Non-refundable to prevent unauthorized credential leakage into external networks.")

        LegalSectionTitle("3. UPI RECONCILIATION LATENCY")
        LegalParagraph("Banking rail switches settle interrupted transactions within 24-48 hours. Providing your 12-digit UTR identifier accelerates manual verification through Command Dispatch.")
    }
}

@Composable
private fun ResponsibleContent() {
    Column {
        LegalHighlightBadge(
            title = "OPERATIVE COGNITIVE EQUILIBRIUM PROTOCOL",
            desc = "Esports is a high-intensity kinetic and tactical discipline. It is strictly not a primary livelihood, financial derivative, or speculative instrument."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. UNCOMPROMISING 18+ AGE GATE")
        LegalParagraph("Minors under the age of 18 are prohibited from participating in cash entry rooms. Unauthorized accounts operated by minors undergo immediate termination with funds returned to parent guardians.")

        LegalSectionTitle("2. COGNITIVE & FISCAL DISCIPLINE")
        LegalParagraph("• Never allocate funds designated for sovereign life necessities.\n• Never incur debt, credit leverage, or loans for tournament participation.\n• Maintain emotional equilibrium and eliminate emotional loss-chasing.")

        LegalSectionTitle("3. CIRCUIT-BREAKER SELF-EXCLUSION & HELPLINES")
        LegalParagraph("Contact Command Support to activate voluntary account cooling-off lockouts.\n• National Mental Health Helpline (KIRAN): 1800-599-0019\n• NIMHANS 24/7 Helpline: 080-46110007")
    }
}

@Composable
private fun LegalStatusContent() {
    Column {
        LegalHighlightBadge(
            title = "CONSTITUTIONAL PRECEDENT: ABSOLUTE DOCTRINE OF SKILL",
            desc = "Competitive esports tournaments constitute protected trade liberties under Article 19(1)(g) of the Constitution of India, definitively segregated from betting or gambling."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. APEX BENCH LANDMARK JURISPRUDENCE")
        LegalParagraph("The Supreme Court of India in State of Bombay v. R.M.D. Chamarbaugwala (1957) and Dr. K.R. Lakshmanan v. State of Tamil Nadu (1996) cemented that activities where victory is governed predominantly by superior strategic mastery, hand-eye adroitness, and mental acumen constitute Games of Skill protected under fundamental rights.")

        LegalSectionTitle("2. PUBLIC GAMBLING ACT 1867 EXCLUSION")
        LegalParagraph("Section 12 of the Public Gambling Act explicitly and unequivocally exempts games of mere skill from all penal provisions associated with games of chance.")

        LegalSectionTitle("3. INTERMEDIARY SAFE HARBOR CITADEL")
        LegalParagraph("VeloRix functions strictly as an electronic sports intermediary under Section 79 of the Information Technology Act 2000, adhering fully to MeitY Online Gaming guidelines and statutory directives.")
    }
}

@Composable
private fun LegalHighlightBadge(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun LegalSectionTitle(title: String) {
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFF38BDF8),
        letterSpacing = 0.8.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFFE2E8F0),
        lineHeight = 17.sp
    )
}
