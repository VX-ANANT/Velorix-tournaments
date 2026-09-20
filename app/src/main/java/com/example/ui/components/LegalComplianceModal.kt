package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.GffDevanagariFontFamily
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

/**
 * LegalComplianceModal.kt
 *
 * Refined modal design with clean minimalist Vercel & Xiaomi aesthetic:
 * - Obsidian black palette (#000000 / #0A0A0A)
 * - Minimalist pill navigation tabs
 * - Clean typographic hierarchy, generous negative space, no cheesy/cluttered borders
 */
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
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF000000),
            border = BorderStroke(1.dp, Color(0xFF1E1E1E)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header Bar - Clean & Professional
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Legal & Compliance",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEDEDED),
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "PROG Act 2025 • MeitY 2026 • DPDP 2023",
                            fontSize = 11.sp,
                            color = Color(0xFF888888)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141414))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFEDEDED),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Horizontal Tabs - Clean Vercel / Xiaomi style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState)
                        .padding(horizontal = 16.dp),
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
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = pillTextColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFF1A1A1A),
                    thickness = 0.8.dp
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
                            .padding(20.dp)
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

                // Bottom Affirmation Footer - Minimalist
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF070707),
                    border = BorderStroke(1.dp, Color(0xFF1A1A1A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JURISDICTION: REPUBLIC OF INDIA",
                                fontSize = 10.sp,
                                fontFamily = GffDevanagariFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF737373)
                            )
                            Text(
                                text = "Grievance: anantisback47@gmail.com",
                                fontSize = 11.sp,
                                color = Color(0xFF999999)
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEDEDED),
                                contentColor = Color(0xFF000000)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Dismiss",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
            title = "COVENANT OF SKILL & PARTICIPATION",
            desc = "By participating in VeloRix custom rooms, you enter into a legally binding covenant under the Indian Contract Act 1872 and Article 19(1)(g) of the Constitution of India."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. ARCHITECTURAL SCOPE & SAFE HARBOR")
        LegalParagraph("VeloRix functions strictly as an electronic sports contest organizer and intermediary infrastructure under Section 79 of the Information Technology Act, 2000. VeloRix provides competitive matchmaking for mobile gaming titles including Garena Free Fire and is not affiliated with or endorsed by Garena International.")

        LegalSectionTitle("2. STRICT 18+ AGE GATING & TERRITORIAL CITADEL")
        LegalParagraph("• Mandatory 18+ Majority under PROG Act 2025 & MeitY Rules 2026: Operatives must certify attainment of 18+ legal majority to engage in entry-fee combat rooms. Minors are restricted solely to Free Practice Scrims.\n• Geofenced Enactments: In absolute adherence to respective state statutes, cash competitions are strictly prohibited to residents within Assam, Odisha, Telangana, Nagaland, Andhra Pradesh, and Sikkim. VPN or geolocation spoofing triggers autonomous hardware invalidation.")

        LegalSectionTitle("3. ESCROW DISCIPLINE, GST 2025 & PRIZE LIQUIDATION")
        LegalParagraph("• Virtual wallet balances serve exclusively as tournament entry escrow.\n• Prize winnings liquidate solely into verified UPI VPA handles following automated match audit validation.\n• Statutory Tax Withholding (TDS) under Section 194BA of the Indian Income Tax Act (30%) is deducted automatically upon net winnings liquidation in alignment with 2025 GST face-value directives.")

        LegalSectionTitle("4. IDENTITY INVARIANTS & ANTI-SYBIL MANDATE")
        LegalParagraph("Each combatant is permitted exactly one verified terminal identity. Tampering with in-game Free Fire UIDs, credentials spoofing, or unauthorized room vector leakage executes immediate room ejection with total escrow forfeiture.")
    }
}

@Composable
private fun PrivacyContent() {
    Column {
        LegalHighlightBadge(
            title = "DATA SOVEREIGNTY (DPDP ACT 2023)",
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
            title = "SENTINEL ANTI-CHEAT: ZERO-TOLERANCE POLICY",
            desc = "Powered by automated match telemetries and scoreboard audits, match telemetries undergo autonomous verification."
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
            title = "DETERMINISTIC ESCROW & RECONCILIATION",
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
            title = "OPERATIVE WELFARE & RESPONSIBLE GAMING",
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
            title = "PROG ACT 2025 & MEITY RULES 2026",
            desc = "VeloRix operates under the Promotion and Regulation of Online Gaming Act, 2025 and PROG Rules, 2026 (MeitY) as a Permissible Skill-Based Electronic Sports Platform, distinct from prohibited online wagering."
        )

        Spacer(modifier = Modifier.height(14.dp))
        LegalSectionTitle("1. STATUTORY ESPORTS CLASSIFICATION (PROG RULES 2026)")
        LegalParagraph("Under Section 4 & 5 of the Promotion and Regulation of Online Gaming Rules, 2026 notified by MeitY, competitive battle royale matches (Free Fire) with predetermined deterministic rules, motor dexterity requirements, and verified match credentials qualify as permissible skill-based e-sports contests.")

        LegalSectionTitle("2. ONLINE GAMING AUTHORITY OF INDIA (OGAI) SENTINEL")
        LegalParagraph("Adheres to the regulatory directives issued by the Online Gaming Authority of India (OGAI), established in April 2026: strict zero algorithmic tampering, mobile hardware parity (anti-emulator enforcement), auditable tournament ledger, and prompt institutional grievance redressal.")

        LegalSectionTitle("3. CONSTITUTIONAL PRECEDENT & SECTION 12 EXCLUSION")
        LegalParagraph("The Supreme Court of India in State of Bombay v. R.M.D. Chamarbaugwala (1957) and Dr. K.R. Lakshmanan (1996) established that pure skill competitions are constitutionally protected under Article 19(1)(g). Section 12 of the Public Gambling Act, 1867 definitively excludes games of mere skill.")

        LegalSectionTitle("4. INTERMEDIARY SAFE HARBOR CITADEL (SECTION 79)")
        LegalParagraph("VeloRix functions strictly as an electronic sports intermediary under Section 79 of the Information Technology Act 2000, adhering fully to MeitY Intermediary Rules, DPDP 2023 rules, and CBDT Section 194BA TDS statutory deductions.")
    }
}

@Composable
private fun LegalHighlightBadge(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F0F0F),
        border = BorderStroke(1.dp, Color(0xFF222222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GffDevanagariFontFamily,
                letterSpacing = 0.5.sp,
                color = Color(0xFFEDEDED)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun LegalSectionTitle(title: String) {
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = GffDevanagariFontFamily,
        color = Color(0xFFEDEDED),
        letterSpacing = 0.5.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFFA1A1A6),
        lineHeight = 18.sp
    )
}
