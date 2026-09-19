package com.example.util

import com.example.data.model.Tournament
import com.example.data.model.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ComplianceEngine.kt
 *
 * Statutory Jurisprudence & Fair Play Enforcement Engine for the VeloRix Citadel.
 * Translates documented legal declarations into hardcoded, uncompromised algorithmic gatekeepers:
 *
 * 1. Promotion and Regulation of Online Gaming Act, 2025 (PROG Act, 2025)
 * 2. Promotion and Regulation of Online Gaming Rules, 2026 (MeitY Notified April 22, 2026, Effective May 1, 2026)
 * 3. Online Gaming Authority of India (OGAI) Classification: Permissible Skill-Based E-Sports Platform
 * 4. Republic of India Age Majority (18+ Mandatory for Real-Money Staking under PROG Rules 2026)
 * 5. Jurisdictional Exclusions (State-Specific Gaming Acts: Assam, Odisha, Telangana, AP, Nagaland, Sikkim)
 * 6. Sentinel Anti-Cheat & Hardware Integrity (OGAI Parity Directives: Desktop Emulator Interception)
 * 7. Operative Welfare Protocol (Daily Match Frequency Throttle & Voluntary Cooling-Off under Rule 7)
 * 8. Fiscal & TDS Compliance (Section 194BA IT Act 1961, 2025 GST 40% Framework & NPCI UPI Sanitization)
 */
object ComplianceEngine {

    const val STATUTORY_FRAMEWORK_NAME = "PROG Act 2025 & MeitY Rules 2026"
    const val PROG_ACT_2025 = "Promotion and Regulation of Online Gaming Act, 2025"
    const val PROG_RULES_2026 = "Promotion and Regulation of Online Gaming Rules, 2026"
    const val OGAI_REGULATORY_BODY = "Online Gaming Authority of India (OGAI - MeitY)"
    const val ESPORTS_STATUS = "Permissible Pure Skill-Based E-Sports Tournament Platform (Section 4 & 5 PROG Rules 2026)"

    val RESTRICTED_INDIAN_STATES = setOf(
        "ASSAM",
        "ODISHA",
        "ORISSA",
        "TELANGANA",
        "ANDHRA PRADESH",
        "NAGALAND",
        "SIKKIM"
    )

    private val STATE_CITATIONS = mapOf(
        "ASSAM" to "The Assam Game and Betting Act, 1970",
        "ODISHA" to "The Orissa Prevention of Gambling Act, 1955",
        "ORISSA" to "The Orissa Prevention of Gambling Act, 1955",
        "TELANGANA" to "The Telangana Gaming (Amendment) Act, 2017",
        "ANDHRA PRADESH" to "The Andhra Pradesh Gaming (Amendment) Act, 2020",
        "NAGALAND" to "Nagaland Prohibition of Gambling and Promotion of Online Games of Skill Act, 2016",
        "SIKKIM" to "The Sikkim Online Gaming (Regulation) Act, 2008"
    )

    const val MAX_DAILY_TOURNAMENTS = 10
    const val TDS_RATE_PERCENT = 30.0

    sealed class ValidationResult {
        object Passed : ValidationResult()
        data class Denied(
            val reason: String,
            val statutoryCitation: String,
            val auditTag: String
        ) : ValidationResult()
    }

    /**
     * Calculates chronological age in completed years from multiple common date formats.
     */
    fun calculateAge(dobString: String): Int {
        if (dobString.isBlank()) return 0
        val trimmed = dobString.trim()
        val patterns = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "yyyy/MM/dd",
            "d/M/yyyy",
            "d-M-yyyy"
        )

        var parsedDate: Date? = null
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
                parsedDate = sdf.parse(trimmed)
                if (parsedDate != null) break
            } catch (_: Exception) {
                // Try next pattern
            }
        }

        if (parsedDate == null) {
            // Attempt manual numeric extraction if format has 4-digit year
            val yearMatch = Regex("""\b(19\d{2}|20\d{2})\b""").find(trimmed)
            if (yearMatch != null) {
                val birthYear = yearMatch.value.toInt()
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                return (currentYear - birthYear).coerceAtLeast(0)
            }
            return 0
        }

        val dobCal = Calendar.getInstance().apply { time = parsedDate }
        val todayCal = Calendar.getInstance()

        var age = todayCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (todayCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age.coerceAtLeast(0)
    }

    /**
     * Checks whether an operative is 18 years or older.
     */
    fun is18Plus(dobString: String): Boolean {
        return calculateAge(dobString) >= 18
    }

    /**
     * Identifies if a territory is prohibited from real-money skill gaming contests.
     */
    fun isRestrictedTerritory(stateName: String?): Boolean {
        if (stateName.isNullOrBlank()) return false
        val normalized = stateName.trim().uppercase(Locale.ENGLISH)
        return RESTRICTED_INDIAN_STATES.any { restricted ->
            normalized == restricted || normalized.contains(restricted)
        }
    }

    fun getStatutoryCitation(stateName: String?): String {
        if (stateName.isNullOrBlank()) return "State Skill Gaming Prohibitory Statutes"
        val normalized = stateName.trim().uppercase(Locale.ENGLISH)
        for ((state, citation) in STATE_CITATIONS) {
            if (normalized == state || normalized.contains(state)) {
                return citation
            }
        }
        return "State Statutory Gaming Legislation"
    }

    /**
     * Comprehensive multi-tiered tournament enrollment gatekeeper.
     */
    fun validateTournamentEnrollment(
        user: User,
        match: Tournament,
        todayMatchesCount: Int,
        isEmulator: Boolean,
        isAdminBypass: Boolean = false
    ): ValidationResult {
        // 1. Account Sanctions / Banned Status
        if (user.isBanned) {
            return ValidationResult.Denied(
                reason = "Account Disqualified: Active sanction in effect (${user.banReason.ifBlank { "Violation of Fair Play Code" }}).",
                statutoryCitation = "VeloRix Sentinel Anti-Cheat & Conduct Codex",
                auditTag = "REJECTED_ACCOUNT_BANNED"
            )
        }
        if (user.isSuspended && user.suspensionExpiresAt > System.currentTimeMillis()) {
            return ValidationResult.Denied(
                reason = "Account Suspended: Operational pause in effect until ${Date(user.suspensionExpiresAt)}.",
                statutoryCitation = "VeloRix Conduct Enforcement",
                auditTag = "REJECTED_ACCOUNT_SUSPENDED"
            )
        }

        // 2. Operative Welfare - Voluntary Cooling-off Protocol (PROG Rules 2026 Rule 7)
        if (user.coolingOffUntil > System.currentTimeMillis()) {
            return ValidationResult.Denied(
                reason = "Cognitive Cooling-Off Active: You have voluntarily self-excluded until ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(Date(user.coolingOffUntil))}. Access is locked for operative welfare under PROG Rules 2026.",
                statutoryCitation = "$PROG_RULES_2026 (Rule 7 Self-Exclusion Mandate)",
                auditTag = "REJECTED_COOLING_OFF_ACTIVE"
            )
        }

        // 3. Operative Welfare - Daily Participation Cap
        if (todayMatchesCount >= MAX_DAILY_TOURNAMENTS && !isAdminBypass) {
            return ValidationResult.Denied(
                reason = "Daily Participation Limit Reached: You have entered $todayMatchesCount tournaments in the past 24 hours. Daily maximum is $MAX_DAILY_TOURNAMENTS under OGAI Responsible Gaming directives.",
                statutoryCitation = "$OGAI_REGULATORY_BODY (Responsible Gaming Directive 2026)",
                auditTag = "REJECTED_DAILY_FREQUENCY_CAP"
            )
        }

        // 4. Sentinel Hardware Integrity - Desktop Emulator Gate
        if (isEmulator && !isAdminBypass) {
            return ValidationResult.Denied(
                reason = "Hardware Parity Sentry: Desktop emulator hardware signature detected (BlueStacks/LDPlayer/Nox/VM). Competitive mobile brackets require authentic mobile hardware under OGAI Esports Fair-Play rules.",
                statutoryCitation = "OGAI Electronic Sports Fair Play Codex § 4 (Anti-Emulator Parity)",
                auditTag = "REJECTED_EMULATOR_HARDWARE"
            )
        }

        // 5. Paid Tournament Statutory Majority (18+ Requirement under PROG Act 2025 & PROG Rules 2026)
        if (match.entryFee > 0.0) {
            if (user.dob.isBlank()) {
                return ValidationResult.Denied(
                    reason = "Statutory Date of Birth Required: Paid real-money tournament entry mandates a verified Date of Birth under the Promotion and Regulation of Online Gaming Act, 2025.",
                    statutoryCitation = "$PROG_ACT_2025 & $PROG_RULES_2026",
                    auditTag = "REJECTED_DOB_MISSING"
                )
            }
            val calculatedAge = calculateAge(user.dob)
            if (calculatedAge < 18) {
                return ValidationResult.Denied(
                    reason = "Statutory Age Gate (Under 18): Operative age is $calculatedAge years. Under the Promotion and Regulation of Online Gaming Act 2025 & MeitY PROG Rules 2026, monetary stake contests are strictly limited to adults 18+. Minors are restricted to Free Practice Scrims.",
                    statutoryCitation = "$PROG_ACT_2025 (Section 5 Statutory Age Safeguard) & $PROG_RULES_2026",
                    auditTag = "REJECTED_UNDERAGE_STATUTORY"
                )
            }

            // 6. Jurisdictional Exclusions (Restricted Indian States)
            if (isRestrictedTerritory(user.state)) {
                val citation = getStatutoryCitation(user.state)
                return ValidationResult.Denied(
                    reason = "Territorial Legal Exclusion: Real-money esports tournaments are statutorily barred in ${user.state}. Free scrimmage matches remain unrestricted.",
                    statutoryCitation = "$citation & $PROG_ACT_2025 Territorial Ingress Rules",
                    auditTag = "REJECTED_JURISDICTION_RESTRICTED_STATE"
                )
            }
        }

        return ValidationResult.Passed
    }

    /**
     * Financial Liquidation & TDS Gatekeeper.
     */
    fun validateWithdrawalRequest(
        user: User,
        amount: Double,
        upiId: String,
        availableWinnings: Double
    ): ValidationResult {
        if (amount <= 0.0) {
            return ValidationResult.Denied(
                reason = "Invalid Amount: Liquidation value must be strictly positive.",
                statutoryCitation = "Financial Liquidation Rules",
                auditTag = "REJECTED_WITHDRAWAL_NON_POSITIVE"
            )
        }
        if (amount < 50.0) {
            return ValidationResult.Denied(
                reason = "Minimum Threshold: Minimum statutory liquidation threshold is VT 50.",
                statutoryCitation = "NPCI & Liquidation Guidelines",
                auditTag = "REJECTED_WITHDRAWAL_BELOW_MIN"
            )
        }
        if (amount > 10000.0) {
            return ValidationResult.Denied(
                reason = "Threshold Cap: Maximum single liquidation batch is capped at VT 10,000 for AML risk mitigation.",
                statutoryCitation = "Anti-Money Laundering Framework",
                auditTag = "REJECTED_WITHDRAWAL_EXCEEDS_MAX"
            )
        }

        // 18+ Age Verification for Financial Settlements
        val operativeAge = calculateAge(user.dob)
        if (user.dob.isNotBlank() && operativeAge < 18) {
            return ValidationResult.Denied(
                reason = "Statutory Financial Incapacity: Individuals under 18 years cannot execute direct banking settlements under Section 11 Indian Contract Act & PROG Act 2025.",
                statutoryCitation = "$PROG_ACT_2025 & RBI KYC Directions",
                auditTag = "REJECTED_UNDERAGE_LIQUIDATION"
            )
        }

        // Escrow Segregation Check
        if (amount > availableWinnings) {
            return ValidationResult.Denied(
                reason = "Escrow Violation: Only earned tournament prize winnings (Available: VT ${availableWinnings.toInt()}) are eligible for liquidation. Deposited tokens are strictly held in escrow for match entry fees under PROG Rules 2026.",
                statutoryCitation = "$PROG_RULES_2026 § Escrow & Prize Pool Liquidation Policy",
                auditTag = "REJECTED_WITHDRAWAL_DEPOSIT_DRAIN"
            )
        }

        // Strict NPCI UPI VPA Regex Validation
        val cleanUpi = upiId.trim()
        val upiRegex = Regex("""^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}$""")
        if (!cleanUpi.matches(upiRegex)) {
            return ValidationResult.Denied(
                reason = "Malformed UPI VPA: Provide a verified NPCI UPI Virtual Payment Address (e.g., handle@oksbi, name@paytm).",
                statutoryCitation = "NPCI UPI Operating Specifications",
                auditTag = "REJECTED_MALFORMED_UPI_VPA"
            )
        }

        return ValidationResult.Passed
    }

    /**
     * Compute statutory TDS under Section 194BA of the Income Tax Act, 1961.
     */
    fun computeTaxDeduction(amount: Double): Double {
        return (amount * (TDS_RATE_PERCENT / 100.0))
    }
}
