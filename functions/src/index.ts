import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const rtdb = admin.database();

/** Anti-Exploit Constants */
const DAILY_MISSION_REWARD_CAP_TOKENS = 100;
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

interface DailyLoginRequest {
  userId?: string;
}

interface MissionClaimRequest {
  userId?: string;
  missionId?: string;
  rewardCurrency?: number;
  reward?: number;
}

interface ReferralRegistrationRequest {
  newUserId?: string;
  newUsername?: string;
  newUserEmail?: string;
  newUserPhone?: string;
  referralCode?: string;
}

interface DepositCommissionRequest {
  payerUserId?: string;
  depositAmount?: number;
  depositTxId?: string;
}

/**
 * Returns the current date in IST format (YYYY-MM-DD) based on server clock.
 */
function getTodayIstDate(timestampMs: number = Date.now()): string {
  const d = new Date(timestampMs + IST_OFFSET_MS);
  return d.toISOString().split("T")[0];
}

/**
 * Returns yesterday's date in IST format (YYYY-MM-DD).
 */
function getYesterdayIstDate(timestampMs: number = Date.now()): string {
  const ONE_DAY_MS = 24 * 60 * 60 * 1000;
  return getTodayIstDate(timestampMs - ONE_DAY_MS);
}

// ============================================================================
// 1. SERVER-SIDE DAILY LOGIN & STREAK VALIDATION
// ============================================================================
export const validateDailyLogin = functions.https.onCall(
  async (data: DailyLoginRequest, context: functions.https.CallableContext) => {
    const uid = context.auth ? context.auth.uid : data?.userId;
    if (!uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to claim daily login rewards."
      );
    }

    const userRef = db.collection("users").document(uid);
    const nowServerMs = Date.now();
    const todayIst = getTodayIstDate(nowServerMs);
    const yesterdayIst = getYesterdayIstDate(nowServerMs);

    return await db.runTransaction(async (transaction) => {
      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "User record not found on server."
        );
      }

      const userData = userDoc.data() || {};
      const lastClaimDate = userData.lastLoginClaimDate || "";
      const currentStreak = userData.loginStreak || 0;
      const currentTokens = userData.tokens || 0;
      const lastMissionDate = userData.lastMissionClaimDate || "";
      const claimedToday =
        lastMissionDate === todayIst ? userData.dailyMissionsTokensClaimed || 0 : 0;

      // Strict anti-exploit check: already claimed today in server IST date
      if (lastClaimDate === todayIst) {
        throw new functions.https.HttpsError(
          "already-exists",
          "Daily check-in reward already claimed for today (Resets at 12:00 AM IST)."
        );
      }

      // Daily Cap Check
      const rewardTokens = 10;
      if (claimedToday + rewardTokens > DAILY_MISSION_REWARD_CAP_TOKENS) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          `Daily mission reward limit reached (${claimedToday}/${DAILY_MISSION_REWARD_CAP_TOKENS} Tokens today). Resets at 12:00 AM IST.`
        );
      }

      const isConsecutive = lastClaimDate === yesterdayIst;
      const newStreak = isConsecutive ? currentStreak + 1 : 1;
      const newTokens = currentTokens + rewardTokens;
      const newClaimedToday = claimedToday + rewardTokens;

      const txId = `TX_DAILY_${uid}_${nowServerMs}`;
      const txRef = db.collection("transactions").document(txId);
      const txData = {
        id: txId,
        userId: uid,
        type: "MISSION_REWARD",
        amount: rewardTokens,
        detail: `Daily Check-In (Day ${newStreak} Streak - Server Verified): +${rewardTokens} Tokens`,
        isPositive: true,
        timestamp: nowServerMs,
        serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
        status: "SUCCESS",
      };

      // Update User Document
      transaction.update(userRef, {
        tokens: newTokens,
        tokenBalance: newTokens,
        tokensBalance: newTokens,
        rewardTokens: newTokens,
        loginStreak: newStreak,
        lastLoginClaimDate: todayIst,
        lastMissionClaimDate: todayIst,
        dailyMissionsTokensClaimed: newClaimedToday,
        lastDailyClaimTimestamp: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Record Transaction
      transaction.set(txRef, txData);

      // Update User Mission Document for daily checkin
      const missionRef = userRef.collection("missions").document("m_daily_checkin");
      transaction.set(
        missionRef,
        {
          missionId: "m_daily_checkin",
          progress: 1,
          target: 1,
          isCompleted: true,
          isClaimed: true,
          claimedDate: todayIst,
          claimedAt: admin.firestore.FieldValue.serverTimestamp(),
          rewardCurrency: rewardTokens,
          serverVerified: true,
        },
        { merge: true }
      );

      // Mirror to Realtime Database
      try {
        await rtdb.ref(`users/${uid}`).update({
          tokens: newTokens,
          tokenBalance: newTokens,
          tokensBalance: newTokens,
          rewardTokens: newTokens,
          loginStreak: newStreak,
          lastLoginClaimDate: todayIst,
          lastMissionClaimDate: todayIst,
          dailyMissionsTokensClaimed: newClaimedToday,
          lastLoginAt: nowServerMs,
        });
        await rtdb.ref(`transactions/${txId}`).set(txData);
        await rtdb.ref(`user_missions/${uid}/m_daily_checkin`).set({
          missionId: "m_daily_checkin",
          progress: 1,
          isCompleted: true,
          isClaimed: true,
          claimedDate: todayIst,
          serverVerified: true,
        });
      } catch (rtdbErr: any) {
        console.warn("RTDB mirror failed for daily login:", rtdbErr.message);
      }

      return {
        success: true,
        rewardTokens: rewardTokens,
        newStreak: newStreak,
        todayIst: todayIst,
        dailyClaimedToday: newClaimedToday,
        dailyCap: DAILY_MISSION_REWARD_CAP_TOKENS,
        message: `🎉 Server Verified: Claimed +${rewardTokens} Tokens! Streak: ${newStreak} Days 🔥 (Daily Cap: ${newClaimedToday}/${DAILY_MISSION_REWARD_CAP_TOKENS})`,
      };
    });
  }
);

// ============================================================================
// 2. SERVER-SIDE MISSION REWARD CLAIM WITH STRICT ANTI-EXPLOIT LIMITER
// ============================================================================
export const validateMissionClaim = functions.https.onCall(
  async (data: MissionClaimRequest, context: functions.https.CallableContext) => {
    const uid = context.auth ? context.auth.uid : data?.userId;
    const missionId = data?.missionId;
    const requestedReward = Number(data?.rewardCurrency || data?.reward || 15);

    if (!uid || !missionId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing userId or missionId."
      );
    }

    if (missionId === "m_daily_checkin" || missionId.startsWith("m_daily_login_")) {
      return await validateDailyLogin.run(data, context);
    }

    const nowServerMs = Date.now();
    const todayIst = getTodayIstDate(nowServerMs);
    const userRef = db.collection("users").document(uid);
    const missionRef = userRef.collection("missions").document(missionId);
    const globalMissionRef = db.collection("missions").document(missionId);

    return await db.runTransaction(async (transaction) => {
      const userDoc = await transaction.get(userRef);
      if (!userDoc.exists) {
        throw new functions.https.HttpsError(
          "not-found",
          "User record not found on server."
        );
      }

      const userData = userDoc.data() || {};
      const lastMissionDate = userData.lastMissionClaimDate || "";
      const claimedToday =
        lastMissionDate === todayIst ? userData.dailyMissionsTokensClaimed || 0 : 0;

      // Fetch Mission Metadata from global catalog if present
      const globalMissionDoc = await transaction.get(globalMissionRef);
      let rewardTokens = Math.min(requestedReward, 80); // Strict upper bounds per single mission
      let missionTitle = missionId;
      let missionCategory = "DAILY";

      if (globalMissionDoc.exists) {
        const gData = globalMissionDoc.data() || {};
        rewardTokens = Number(gData.rewardCurrency || gData.reward || rewardTokens);
        missionTitle = gData.title || missionTitle;
        missionCategory = gData.category || missionCategory;
      }

      // Check Daily Cap Limiter (Applies to daily missions)
      if (missionCategory === "DAILY") {
        if (claimedToday + rewardTokens > DAILY_MISSION_REWARD_CAP_TOKENS) {
          throw new functions.https.HttpsError(
            "resource-exhausted",
            `Daily mission reward cap reached (${claimedToday}/${DAILY_MISSION_REWARD_CAP_TOKENS} Tokens). Limit resets at 12:00 AM IST.`
          );
        }
      }

      const userMissionDoc = await transaction.get(missionRef);
      const userMissionData = userMissionDoc.exists ? userMissionDoc.data() || {} : {};

      if (userMissionData.isClaimed) {
        throw new functions.https.HttpsError(
          "already-exists",
          "This mission reward has already been claimed on the server."
        );
      }

      const currentTokens = userData.tokens || 0;
      const newTokens = currentTokens + rewardTokens;
      const newClaimedToday =
        missionCategory === "DAILY" ? claimedToday + rewardTokens : claimedToday;

      const txId = `TX_MISSION_${missionId}_${uid}_${nowServerMs}`;
      const txRef = db.collection("transactions").document(txId);
      const txData = {
        id: txId,
        userId: uid,
        type: "MISSION_REWARD",
        amount: rewardTokens,
        detail: `Claimed ${missionCategory} Mission: ${missionTitle} (+${rewardTokens} Tokens - Server Verified)`,
        isPositive: true,
        timestamp: nowServerMs,
        serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
        status: "SUCCESS",
      };

      // Update User Balances
      transaction.update(userRef, {
        tokens: newTokens,
        tokenBalance: newTokens,
        tokensBalance: newTokens,
        rewardTokens: newTokens,
        lastMissionClaimDate: todayIst,
        dailyMissionsTokensClaimed: newClaimedToday,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Mark Mission Claimed
      transaction.set(
        missionRef,
        {
          missionId: missionId,
          title: missionTitle,
          category: missionCategory,
          isCompleted: true,
          isClaimed: true,
          claimedAt: admin.firestore.FieldValue.serverTimestamp(),
          claimedDate: todayIst,
          serverVerified: true,
        },
        { merge: true }
      );

      // Record Transaction
      transaction.set(txRef, txData);

      // Mirror to Realtime Database
      try {
        await rtdb.ref(`users/${uid}`).update({
          tokens: newTokens,
          tokenBalance: newTokens,
          tokensBalance: newTokens,
          rewardTokens: newTokens,
          lastMissionClaimDate: todayIst,
          dailyMissionsTokensClaimed: newClaimedToday,
        });
        await rtdb.ref(`transactions/${txId}`).set(txData);
        await rtdb.ref(`user_missions/${uid}/${missionId}`).update({
          isCompleted: true,
          isClaimed: true,
          claimedDate: todayIst,
          serverVerified: true,
        });
      } catch (rtdbErr: any) {
        console.warn("RTDB mission claim mirror warning:", rtdbErr.message);
      }

      return {
        success: true,
        rewardTokens: rewardTokens,
        missionId: missionId,
        dailyClaimedToday: newClaimedToday,
        dailyCap: DAILY_MISSION_REWARD_CAP_TOKENS,
        message: `🎉 Server Verified: Claimed +${rewardTokens} Tokens for ${missionTitle}!`,
      };
    });
  }
);

// ============================================================================
// 3. SERVER-SIDE REGISTRATION REFERRAL LINKING & VALIDATION (ANTI-EXPLOIT)
// ============================================================================
export const verifyReferralOnRegistration = functions.https.onCall(
  async (
    data: ReferralRegistrationRequest,
    _context: functions.https.CallableContext
  ) => {
    const { newUserId, newUsername, newUserEmail, newUserPhone, referralCode } =
      data || {};
    if (!newUserId || !referralCode) {
      return { valid: false, message: "Missing registration or referral parameters." };
    }

    const cleanCode = referralCode.trim().toUpperCase();

    // 1. Look up referrer in Firestore referral_codes collection
    let referrerUid: string | null = null;
    let referrerUsername = "Squadmate";
    let referrerCount = 0;
    let referrerEmail = "";
    let referrerPhone = "";

    const codeDoc = await db.collection("referral_codes").document(cleanCode).get();
    if (codeDoc.exists) {
      referrerUid = codeDoc.data()?.userId || null;
      referrerUsername = codeDoc.data()?.username || "Squadmate";
    }

    if (!referrerUid) {
      const usersSnapshot = await db
        .collection("users")
        .where("referralCode", "==", cleanCode)
        .limit(1)
        .get();
      if (!usersSnapshot.empty) {
        const doc = usersSnapshot.docs[0];
        referrerUid = doc.id;
        const d = doc.data();
        referrerUsername = d.username || "Squadmate";
        referrerCount = d.referralCount || 0;
        referrerEmail = d.phoneOrEmail || d.email || "";
        referrerPhone = d.phoneNumber || "";
      }
    }

    // 2. Fallback RTDB lookup
    if (!referrerUid) {
      const rtdbSnap = await rtdb.ref(`referral_codes/${cleanCode}`).get();
      if (rtdbSnap.exists()) {
        referrerUid = rtdbSnap.child("userId").val();
        referrerUsername = rtdbSnap.child("username").val() || "Squadmate";
      }
    }

    if (!referrerUid) {
      return { valid: false, message: "Referral code not found on server." };
    }

    // Anti-fraud: Prevent self-referral
    if (referrerUid === newUserId) {
      return {
        valid: false,
        message: "Anti-Fraud: You cannot redeem your own referral code.",
      };
    }

    // Anti-fraud: Prevent duplicate identity registration (matching email / phone)
    if (
      newUserEmail &&
      referrerEmail &&
      newUserEmail.toLowerCase() === referrerEmail.toLowerCase()
    ) {
      return {
        valid: false,
        message: "Anti-Fraud: Cannot use referral between identical registered accounts.",
      };
    }
    if (newUserPhone && referrerPhone && newUserPhone === referrerPhone) {
      return {
        valid: false,
        message: "Anti-Fraud: Cannot use referral between identical registered accounts.",
      };
    }

    // Fetch updated referrer count
    const refDoc = await db.collection("users").document(referrerUid).get();
    if (refDoc.exists) {
      referrerCount = refDoc.data()?.referralCount || 0;
    }

    const newReferrerCount = referrerCount + 1;
    const currentCommissionTier =
      newReferrerCount <= 1 ? 10 : newReferrerCount <= 4 ? 12 : 15;

    const batch = db.batch();
    const referrerRef = db.collection("users").document(referrerUid);

    // Update Referrer Stats on Registration
    batch.update(referrerRef, {
      referralCount: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create In-App Notification for Referrer
    const notifId = `NOTIF_REF_${Date.now()}`;
    const notifRef = referrerRef.collection("items").document(notifId);
    batch.set(notifRef, {
      id: notifId,
      userId: referrerUid,
      title: "🎉 New Squadmate Joined!",
      message: `${
        newUsername || "A new player"
      } registered using your referral code! You will earn ${currentCommissionTier}% commission on all their wallet deposits.`,
      type: "REFERRAL",
      timestamp: Date.now(),
      isRead: false,
      serverVerified: true,
    });

    await batch.commit();

    // Sync to RTDB
    try {
      await rtdb.ref(`users/${referrerUid}`).update({
        referralCount: newReferrerCount,
      });
      await rtdb.ref(`notifications/${referrerUid}/${notifId}`).set({
        id: notifId,
        userId: referrerUid,
        title: "🎉 New Squadmate Joined!",
        message: `${
          newUsername || "A new player"
        } registered using your referral code! You will earn ${currentCommissionTier}% commission on all their wallet deposits.`,
        type: "REFERRAL",
        timestamp: Date.now(),
        read: false,
      });
    } catch (rtdbErr: any) {
      console.warn("RTDB referral link mirror warning:", rtdbErr.message);
    }

    return {
      valid: true,
      referrerUid: referrerUid,
      referrerUsername: referrerUsername,
      commissionTier: currentCommissionTier,
      message: `Referral code linked successfully! Referrer will earn ${currentCommissionTier}% commission on deposits.`,
    };
  }
);

/**
 * Pre-registration instant referral code validator.
 * Validates existence and active status without modifying state, returning referrer IGN.
 */
export const validateReferralCode = functions.https.onCall(
  async (
    data: { referralCode?: string },
    _context: functions.https.CallableContext
  ) => {
    const rawCode = (data?.referralCode || "").trim().toUpperCase();
    if (!rawCode || rawCode.length < 4) {
      return { valid: false, message: "Code must be at least 4 characters." };
    }

    let referrerUid: string | null = null;
    let referrerUsername = "Squadmate";

    const codeDoc = await db.collection("referral_codes").document(rawCode).get();
    if (codeDoc.exists) {
      referrerUid = codeDoc.data()?.userId || null;
      referrerUsername = codeDoc.data()?.username || "Squadmate";
    }

    if (!referrerUid) {
      const usersSnapshot = await db
        .collection("users")
        .where("referralCode", "==", rawCode)
        .limit(1)
        .get();
      if (!usersSnapshot.empty) {
        referrerUid = usersSnapshot.docs[0].id;
        referrerUsername = usersSnapshot.docs[0].data()?.username || "Squadmate";
      }
    }

    if (!referrerUid) {
      const rtdbSnap = await rtdb.ref(`referral_codes/${rawCode}`).get();
      if (rtdbSnap.exists()) {
        referrerUid = rtdbSnap.child("userId").val();
        referrerUsername = rtdbSnap.child("username").val() || "Squadmate";
      }
    }

    if (!referrerUid) {
      return { valid: false, message: "Referral code not found on server." };
    }

    return {
      valid: true,
      referrerUid: referrerUid,
      referrerUsername: referrerUsername,
      message: `Verified Squadmate: ${referrerUsername} (10-15% Tier Linked)`
    };
  }
);

// ============================================================================
// 4. SERVER-SIDE DEPOSIT COMMISSION ENGINE (10%, 12%, 15% MAX CAP)
// ============================================================================
export const processReferralDepositCommission = functions.https.onCall(
  async (
    data: DepositCommissionRequest,
    _context: functions.https.CallableContext
  ) => {
    const { payerUserId, depositAmount, depositTxId: _depositTxId } = data || {};
    const amt = Number(depositAmount || 0);

    if (!payerUserId || amt <= 0) {
      return { success: false, message: "Invalid commission parameters." };
    }

    const payerDoc = await db.collection("users").document(payerUserId).get();
    if (!payerDoc.exists) {
      return { success: false, message: "Payer user not found on server." };
    }

    const payerData = payerDoc.data() || {};
    const referredByCode = (payerData.referredBy || "").trim().toUpperCase();

    if (!referredByCode) {
      return { success: false, message: "Payer was not referred by any code." };
    }

    // Look up referrer UID
    let referrerUid: string | null = null;
    let referrerUsername = "Squadmate";

    const codeDoc = await db.collection("referral_codes").document(referredByCode).get();
    if (codeDoc.exists) {
      referrerUid = codeDoc.data()?.userId || null;
      referrerUsername = codeDoc.data()?.username || "Squadmate";
    }

    if (!referrerUid) {
      const usersSnapshot = await db
        .collection("users")
        .where("referralCode", "==", referredByCode)
        .limit(1)
        .get();
      if (!usersSnapshot.empty) {
        referrerUid = usersSnapshot.docs[0].id;
        referrerUsername = usersSnapshot.docs[0].data().username || "Squadmate";
      }
    }

    if (!referrerUid || referrerUid === payerUserId) {
      return { success: false, message: "Invalid or self-referrer." };
    }

    // Calculate Tiered Commission
    const referrerDoc = await db.collection("users").document(referrerUid).get();
    if (!referrerDoc.exists) {
      return { success: false, message: "Referrer record not found." };
    }

    const refData = referrerDoc.data() || {};
    const refCount = Number(refData.referralCount || 1);

    // Commission Tiers:
    // 1 Referral -> 10% (0.10)
    // 2-4 Referrals -> 12% (0.12)
    // 5+ Referrals -> 15% STRICT MAX CAP (0.15)
    const commissionRate = refCount <= 1 ? 0.10 : refCount <= 4 ? 0.12 : 0.15;
    const commissionAmount = Math.round(amt * commissionRate * 100) / 100; // 2 decimal precision
    const percentageLabel = Math.round(commissionRate * 100);

    if (commissionAmount <= 0) {
      return { success: false, message: "Commission amount is zero." };
    }

    const nowServerMs = Date.now();
    const txId = `TX_COMMISSION_${referrerUid}_${nowServerMs}`;
    const payerName = payerData.username || "Squadmate";

    const batch = db.batch();
    const referrerRef = db.collection("users").document(referrerUid);
    const txRef = db.collection("transactions").document(txId);

    // Credit Referrer Balance & Referral Earnings
    batch.update(referrerRef, {
      balance: admin.firestore.FieldValue.increment(commissionAmount),
      referralEarnings: admin.firestore.FieldValue.increment(commissionAmount),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create Commission Transaction Record
    const txData = {
      id: txId,
      userId: referrerUid,
      type: "REFERRAL_COMMISSION",
      amount: commissionAmount,
      detail: `Squad Referral Commission: ${percentageLabel}% on ₹${amt} deposit by ${payerName}`,
      isPositive: true,
      timestamp: nowServerMs,
      serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
      status: "SUCCESS",
    };
    batch.set(txRef, txData);

    // Send Notification to Referrer
    const notifId = `NOTIF_COMMISSION_${nowServerMs}`;
    const notifRef = referrerRef.collection("items").document(notifId);
    batch.set(notifRef, {
      id: notifId,
      userId: referrerUid,
      title: "💰 Referral Commission Credited!",
      message: `You earned +₹${commissionAmount} VT Tokens (${percentageLabel}% Tier) from ${payerName}'s deposit of ₹${amt}.`,
      type: "WALLET",
      timestamp: nowServerMs,
      isRead: false,
      serverVerified: true,
    });

    await batch.commit();

    // Sync to RTDB
    try {
      const currentRefBalance = Number(refData.balance || 0) + commissionAmount;
      const currentEarnings = Number(refData.referralEarnings || 0) + commissionAmount;

      await rtdb.ref(`users/${referrerUid}`).update({
        balance: currentRefBalance,
        referralEarnings: currentEarnings,
      });
      await rtdb.ref(`transactions/${txId}`).set(txData);
      await rtdb.ref(`notifications/${referrerUid}/${notifId}`).set({
        id: notifId,
        userId: referrerUid,
        title: "💰 Referral Commission Credited!",
        message: `You earned +₹${commissionAmount} VT Tokens (${percentageLabel}% Tier) from ${payerName}'s deposit of ₹${amt}.`,
        type: "WALLET",
        timestamp: nowServerMs,
        read: false,
      });
    } catch (rtdbErr: any) {
      console.warn("RTDB commission mirror warning:", rtdbErr.message);
    }

    return {
      success: true,
      referrerUid: referrerUid,
      commissionAmount: commissionAmount,
      commissionRate: percentageLabel,
      message: `Successfully processed ${percentageLabel}% deposit commission (+₹${commissionAmount} VT) for referrer ${referrerUid}.`,
    };
  }
);

// ============================================================================
// 5. AUTOMATIC FIRESTORE BACKGROUND TRIGGER FOR DEPOSIT COMMISSIONS
// ============================================================================
export const onUserDepositWritten = functions.firestore
  .document("deposit_requests/{reqId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;

    const data = change.after.data() || {};
    const beforeData = change.before.exists ? change.before.data() || {} : {};

    const isApprovedNow = data.status === "SUCCESS" || data.status === "APPROVED";
    const wasApprovedBefore =
      beforeData.status === "SUCCESS" || beforeData.status === "APPROVED";

    if (isApprovedNow && !wasApprovedBefore && !data.referralCommissionProcessed) {
      const payerUid = data.userId;
      const amount = Number(data.amount || 0);
      const reqId = context.params.reqId;

      if (payerUid && amount > 0) {
        console.log(
          `[TRIGGER] Processing deposit commission for Deposit Req: ${reqId}, Payer: ${payerUid}, Amount: ${amount}`
        );
        await processReferralDepositCommission.run(
          { payerUserId: payerUid, depositAmount: amount, depositTxId: reqId },
          { auth: null } as any
        );

        // Mark processed to prevent duplicate payouts
        await change.after.ref.set(
          { referralCommissionProcessed: true },
          { merge: true }
        );
      }
    }
    return null;
  });

// ============================================================================
// 6. TOURNAMENT REGISTRATION VALIDATION & SANITIZATION (GAME ID SECURITY)
// ============================================================================
export const onTournamentRegistrationCreate = functions.database
  .ref("/registrations/{tournamentId}/{uid}")
  .onCreate(async (snapshot, context) => {
    const regData = snapshot.val() || {};
    const { tournamentId, uid } = context.params;
    const gameId = String(
      regData.gameId || regData.characterId || regData.freeFireId || ""
    ).trim();
    const timestamp = Date.now();

    const formatRegex = /^[0-9]{8,12}$/;
    const isValidFormat = formatRegex.test(gameId);
    const uniqueChars = new Set(gameId.split(""));
    const isSuspicious =
      !isValidFormat ||
      uniqueChars.size <= 1 ||
      gameId === "12345678" ||
      gameId === "123456789";

    const logId = `audit_reg_${tournamentId}_${uid}_${timestamp}`;
    const auditPayload = {
      id: logId,
      action: "TOURNAMENT_REGISTRATION_ATTEMPT",
      performedBy: uid,
      userId: uid,
      targetUid: uid,
      gameId: gameId,
      tournamentId: tournamentId,
      status: !isSuspicious && isValidFormat ? "VALIDATED" : "FLAGGED_INVALID",
      details:
        !isSuspicious && isValidFormat
          ? `Valid registration attempt logged for tournament ${tournamentId} with Game ID: ${gameId}`
          : `Invalid/Suspicious Game ID '${gameId}' detected on registration. Automatic sanitization triggered.`,
      timestamp: timestamp,
    };

    try {
      await rtdb.ref(`audit_logs/${logId}`).set(auditPayload);
    } catch (auditErr: any) {
      console.error("Failed to write to audit_logs:", auditErr.message);
    }

    if (!isValidFormat || isSuspicious) {
      console.warn(
        `[SECURITY] Malformed gameId '${gameId}' detected in onCreate for User ${uid}, Tournament ${tournamentId}. Triggering auto-deletion.`
      );

      await snapshot.ref.remove();

      const slotNum = regData.slotNumber;
      if (slotNum) {
        await rtdb
          .ref(`tournaments/${tournamentId}/slots/${slotNum}`)
          .remove()
          .catch(() => {});
      }
      await rtdb
        .ref(`tournaments/${tournamentId}/participants/${uid}`)
        .remove()
        .catch(() => {});

      await rtdb
        .ref(`suspicious_registrations/${uid}/${logId}`)
        .set({
          gameId: gameId,
          tournamentId: tournamentId,
          reason: "Invalid ID Format (must be 8-12 digits)",
          timestamp: timestamp,
        })
        .catch(() => {});

      return { success: false, action: "DELETED_MALFORMED_REGISTRATION", gameId: gameId };
    }

    return { success: true, action: "ACCEPTED", gameId: gameId };
  });
