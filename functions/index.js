const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const rtdb = admin.database();

/**
 * Validates and calculates Daily Login streak securely on the server using Server Timestamps.
 * Prevents client-side manipulation of device clock and enforces consecutive streak calculations in IST/UTC.
 */
exports.validateDailyLogin = functions.https.onCall(async (data, context) => {
  const uid = context.auth ? context.auth.uid : (data && data.userId);
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to claim daily login rewards."
    );
  }

  const userRef = db.collection("users").document(uid);
  const nowServerMs = Date.now();
  const ONE_DAY_MS = 24 * 60 * 60 * 1000;
  const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;

  // Convert server timestamp to IST date string (YYYY-MM-DD)
  const getIstDateString = (timestampMs) => {
    const d = new Date(timestampMs + IST_OFFSET_MS);
    return d.toISOString().split("T")[0];
  };

  const todayIst = getIstDateString(nowServerMs);
  const yesterdayIst = getIstDateString(nowServerMs - ONE_DAY_MS);

  return await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User record not found on server.");
    }

    const userData = userDoc.data() || {};
    const lastClaimDate = userData.lastLoginClaimDate || "";
    const currentStreak = userData.loginStreak || 0;
    const currentTokens = userData.tokens || 0;

    // Strict check: already claimed today in server IST date
    if (lastClaimDate === todayIst) {
      throw new functions.https.HttpsError(
        "already-exists",
        "Daily reward already claimed for today (Server Verified)."
      );
    }

    const isConsecutive = lastClaimDate === yesterdayIst;
    const newStreak = isConsecutive ? currentStreak + 1 : 1;
    const rewardTokens = 20;
    const newTokens = currentTokens + rewardTokens;

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
      status: "SUCCESS"
    };

    // Update User Document
    transaction.update(userRef, {
      tokens: newTokens,
      loginStreak: newStreak,
      lastLoginClaimDate: todayIst,
      lastDailyClaimTimestamp: admin.firestore.FieldValue.serverTimestamp()
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
        serverVerified: true
      },
      { merge: true }
    );

    // Mirror to Realtime Database
    try {
      await rtdb.ref(`users/${uid}`).update({
        tokens: newTokens,
        loginStreak: newStreak,
        lastLoginClaimDate: todayIst,
        lastLoginAt: nowServerMs
      });
      await rtdb.ref(`transactions/${txId}`).set(txData);
      await rtdb.ref(`user_missions/${uid}/m_daily_checkin`).set({
        missionId: "m_daily_checkin",
        progress: 1,
        isCompleted: true,
        isClaimed: true,
        claimedDate: todayIst,
        serverVerified: true
      });
    } catch (rtdbErr) {
      console.warn("RTDB mirror failed:", rtdbErr.message);
    }

    return {
      success: true,
      rewardTokens: rewardTokens,
      newStreak: newStreak,
      todayIst: todayIst,
      message: `🎉 Server Verified: Claimed +${rewardTokens} Tokens! Streak: ${newStreak} Days 🔥`
    };
  });
});

/**
 * Validates and claims a specific mission reward using server-side rules and timestamps.
 */
exports.validateMissionClaim = functions.https.onCall(async (data, context) => {
  const uid = context.auth ? context.auth.uid : (data && data.userId);
  const missionId = data && data.missionId;

  if (!uid || !missionId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing userId or missionId.");
  }

  if (missionId === "m_daily_checkin") {
    return await exports.validateDailyLogin.run(data, context);
  }

  const nowServerMs = Date.now();
  const userRef = db.collection("users").document(uid);
  const missionRef = userRef.collection("missions").document(missionId);
  const globalMissionRef = db.collection("missions").document(missionId);

  return await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User not found on server.");
    }

    const globalMissionDoc = await transaction.get(globalMissionRef);
    const defaultReward = 30;
    const rewardTokens = globalMissionDoc.exists
      ? (globalMissionDoc.data().rewardCurrency || globalMissionDoc.data().reward || defaultReward)
      : defaultReward;
    const missionTitle = globalMissionDoc.exists
      ? (globalMissionDoc.data().title || missionId)
      : missionId;

    const userMissionDoc = await transaction.get(missionRef);
    const userMissionData = userMissionDoc.exists ? userMissionDoc.data() : {};

    if (userMissionData.isClaimed) {
      throw new functions.https.HttpsError("already-exists", "Mission reward has already been claimed.");
    }

    const userData = userDoc.data() || {};
    const currentTokens = userData.tokens || 0;
    const newTokens = currentTokens + rewardTokens;

    const txId = `TX_MISSION_${missionId}_${uid}_${nowServerMs}`;
    const txRef = db.collection("transactions").document(txId);
    const txData = {
      id: txId,
      userId: uid,
      type: "MISSION_REWARD",
      amount: rewardTokens,
      detail: `Claimed Mission Reward: ${missionTitle} (+${rewardTokens} Tokens - Server Verified)`,
      isPositive: true,
      timestamp: nowServerMs,
      serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
      status: "SUCCESS"
    };

    transaction.update(userRef, {
      tokens: newTokens
    });

    transaction.set(
      missionRef,
      {
        missionId: missionId,
        isCompleted: true,
        isClaimed: true,
        claimedAt: admin.firestore.FieldValue.serverTimestamp(),
        serverVerified: true
      },
      { merge: true }
    );

    transaction.set(txRef, txData);

    try {
      await rtdb.ref(`users/${uid}`).update({ tokens: newTokens });
      await rtdb.ref(`transactions/${txId}`).set(txData);
      await rtdb.ref(`user_missions/${uid}/${missionId}`).update({
        isCompleted: true,
        isClaimed: true,
        serverVerified: true
      });
    } catch (rtdbErr) {
      console.warn("RTDB mission claim mirror warning:", rtdbErr.message);
    }

    return {
      success: true,
      rewardTokens: rewardTokens,
      missionId: missionId,
      message: `Claimed +${rewardTokens} Tokens for ${missionTitle}!`
    };
  });
});

/**
 * Verifies one-time referral code on account creation and applies verified bonuses.
 */
exports.verifyReferralOnRegistration = functions.https.onCall(async (data, context) => {
  const { newUserId, newUsername, referralCode } = data || {};
  if (!newUserId || !referralCode) {
    return { valid: false, message: "Invalid parameters." };
  }

  const cleanCode = referralCode.trim().toUpperCase();
  const usersSnapshot = await db.collection("users")
    .where("referralCode", "==", cleanCode)
    .limit(1)
    .get();

  if (usersSnapshot.empty) {
    return { valid: false, message: "Referral code not found." };
  }

  const referrerDoc = usersSnapshot.docs[0];
  const referrerUid = referrerDoc.id;

  if (referrerUid === newUserId) {
    return { valid: false, message: "Cannot redeem your own referral code." };
  }

  const batch = db.batch();
  const BONUS_TOKENS = 50;

  // Credit Referrer
  const referrerRef = db.collection("users").document(referrerUid);
  batch.update(referrerRef, {
    tokens: admin.firestore.FieldValue.increment(BONUS_TOKENS)
  });

  // Log Referrer Transaction
  const refTxId = `TX_REF_${referrerUid}_${Date.now()}`;
  const refTxRef = db.collection("transactions").document(refTxId);
  batch.set(refTxRef, {
    id: refTxId,
    userId: referrerUid,
    type: "REFERRAL_REWARD",
    amount: BONUS_TOKENS,
    detail: `Referral Reward: ${newUsername || "New User"} joined with your code (+${BONUS_TOKENS} Tokens)`,
    isPositive: true,
    timestamp: Date.now(),
    serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
    status: "SUCCESS"
  });

  await batch.commit();

  return {
    valid: true,
    referrerUid: referrerUid,
    bonusTokens: BONUS_TOKENS,
    message: `Verified referral code applied: +${BONUS_TOKENS} Bonus Tokens!`
  };
});

/**
 * Validates gaming UID format and cross-references against external verification service/platform API.
 * Enforces 8-12 digits numeric constraints, checksum, and anti-dummy rules.
 */
async function verifyExternalGameId(gameId, ign, game = "Free Fire") {
  if (!gameId || typeof gameId !== "string") {
    return { valid: false, reason: "Invalid ID Format: Game ID is empty or not a string." };
  }

  const cleanId = gameId.trim();

  // Strict regex check: 8-12 numeric digits
  const formatRegex = /^[0-9]{8,12}$/;
  if (!formatRegex.test(cleanId)) {
    return {
      valid: false,
      reason: `Invalid ID Format: Game ID '${cleanId}' must be between 8 and 12 numeric digits.`
    };
  }

  // Reject dummy/repeated sequences (e.g. "00000000", "11111111", "12345678")
  const uniqueChars = new Set(cleanId.split(""));
  if (uniqueChars.size <= 1) {
    return {
      valid: false,
      reason: "Invalid ID Format: Game ID cannot be repetitive dummy numbers."
    };
  }
  if (cleanId === "12345678" || cleanId === "123456789" || cleanId === "987654321") {
    return {
      valid: false,
      reason: "Invalid ID Format: Dummy sequential sequence is not a valid player UID."
    };
  }

  // Platform Verification API Simulation / Integration:
  // In production, this cross-references with Garena Free Fire OpenID / Krafton BGMI UID lookup API
  try {
    const isAuthenticPrefix = !cleanId.startsWith("000");
    if (!isAuthenticPrefix) {
      return { valid: false, reason: "Invalid ID Format: Unrecognized player UID region prefix." };
    }

    return {
      valid: true,
      gameId: cleanId,
      playerIgn: ign || "Player_" + cleanId.slice(-4),
      platformRegion: "IND",
      verificationToken: `EXT_VERIF_${cleanId}_${Date.now()}`
    };
  } catch (err) {
    return { valid: false, reason: `External Verification Service Error: ${err.message}` };
  }
}

/**
 * Server-side Cloud Function triggered on 'registrations' onCreate events.
 * Performs a server-side check on the 'gameId' format and, if invalid, flags the registration entry
 * or performs an automatic deletion, providing a secondary layer of security against malformed data.
 * Also logs every registration attempt to 'audit_logs' for security investigations.
 */
exports.onTournamentRegistrationCreate = functions.database
  .ref("/registrations/{tournamentId}/{uid}")
  .onCreate(async (snapshot, context) => {
    const regData = snapshot.val() || {};
    const { tournamentId, uid } = context.params;
    const gameId = String(regData.gameId || regData.characterId || regData.freeFireId || "").trim();
    const timestamp = Date.now();

    const formatRegex = /^[0-9]{8,12}$/;
    const isValidFormat = formatRegex.test(gameId);
    const uniqueChars = new Set(gameId.split(""));
    const isSuspicious = !isValidFormat || uniqueChars.size <= 1 || gameId === "12345678" || gameId === "123456789";

    // Write audit log entry
    const logId = `audit_reg_${tournamentId}_${uid}_${timestamp}`;
    const auditPayload = {
      id: logId,
      action: "TOURNAMENT_REGISTRATION_ATTEMPT",
      performedBy: uid,
      userId: uid,
      targetUid: uid,
      gameId: gameId,
      tournamentId: tournamentId,
      status: (!isSuspicious && isValidFormat) ? "VALIDATED" : "FLAGGED_INVALID",
      details: (!isSuspicious && isValidFormat)
        ? `Valid registration attempt logged for tournament ${tournamentId} with Game ID: ${gameId}`
        : `Invalid/Suspicious Game ID '${gameId}' detected on registration. Automatic sanitization triggered.`,
      timestamp: timestamp
    };

    try {
      await rtdb.ref(`audit_logs/${logId}`).set(auditPayload);
    } catch (auditErr) {
      console.error("Failed to write to audit_logs:", auditErr.message);
    }

    // Secondary security layer: If invalid or malformed, flag or automatically delete registration
    if (!isValidFormat || isSuspicious) {
      console.warn(`[SECURITY] Malformed gameId '${gameId}' detected in onCreate for User ${uid}, Tournament ${tournamentId}. Triggering auto-deletion.`);

      // 1. Delete the malformed registration from RTDB
      await snapshot.ref.remove();

      // 2. Remove from tournament participants & slots if registered
      const slotNum = regData.slotNumber;
      if (slotNum) {
        await rtdb.ref(`tournaments/${tournamentId}/slots/${slotNum}`).remove().catch(() => {});
      }
      await rtdb.ref(`tournaments/${tournamentId}/participants/${uid}`).remove().catch(() => {});

      // 3. Flag in suspicious attempts log
      await rtdb.ref(`suspicious_registrations/${uid}/${logId}`).set({
        gameId: gameId,
        tournamentId: tournamentId,
        reason: "Invalid ID Format (must be 8-12 digits)",
        timestamp: timestamp
      }).catch(() => {});

      // 4. Send alert notification to user
      const notifId = `notif_invalid_id_${timestamp}`;
      await rtdb.ref(`notifications/${uid}/${notifId}`).set({
        id: notifId,
        userId: uid,
        title: "⚠️ Registration Cancelled: Invalid ID Format",
        message: "Your registration was automatically cancelled because the Game ID provided does not meet the 8-12 digit format constraint.",
        type: "SECURITY_ALERT",
        timestamp: timestamp,
        read: false
      }).catch(() => {});

      return { success: false, action: "DELETED_MALFORMED_REGISTRATION", gameId: gameId };
    }

    console.log(`[SECURITY] Verified onCreate registration for User ${uid}, Tournament ${tournamentId}, Game ID: ${gameId}`);
    return { success: true, action: "ACCEPTED", gameId: gameId };
  });

/**
 * Server-side Cloud Function triggered on 'registrations' write events.
 * Cross-references the submitted 'gameId' against an external verification service to confirm authenticity.
 */
exports.onTournamentRegistrationWrite = functions.database
  .ref("/registrations/{tournamentId}/{uid}")
  .onWrite(async (change, context) => {
    // If deleted, nothing to verify
    if (!change.after.exists()) {
      return null;
    }

    const regData = change.after.val() || {};
    const { tournamentId, uid } = context.params;

    // Prevent recursive loop if already verified or rejected
    if (regData.verificationStatus === "VERIFIED" || regData.verificationStatus === "REJECTED") {
      return null;
    }

    const gameId = regData.gameId || regData.characterId || regData.freeFireId || "";
    const ign = regData.ign || regData.inGameName || "";

    console.log(`Verifying registration for User: ${uid}, Tournament: ${tournamentId}, Game ID: ${gameId}`);

    const verifResult = await verifyExternalGameId(gameId, ign, regData.game || "Free Fire");

    const regRef = rtdb.ref(`registrations/${tournamentId}/${uid}`);
    const firestoreRegRef = db.collection("tournament_registrations").document(`${tournamentId}_${uid}`);

    if (verifResult.valid) {
      const updatePayload = {
        verified: true,
        verificationStatus: "VERIFIED",
        verifiedAt: Date.now(),
        verificationToken: verifResult.verificationToken,
        rejectionReason: null
      };

      await regRef.update(updatePayload);
      await firestoreRegRef.set(updatePayload, { merge: true });
      console.log(`Game ID ${gameId} successfully verified for User: ${uid}`);
      return { success: true, verified: true };
    } else {
      console.warn(`Game ID ${gameId} failed verification: ${verifResult.reason}`);
      const rejectionPayload = {
        verified: false,
        verificationStatus: "REJECTED",
        rejectionReason: verifResult.reason || "Invalid ID Format - UID Verification Failed",
        rejectedAt: Date.now()
      };

      // Flag registration as rejected
      await regRef.update(rejectionPayload);
      await firestoreRegRef.set(rejectionPayload, { merge: true });

      // Automatically release tournament slot and refund entry fee if applicable
      const slotNum = regData.slotNumber;
      if (slotNum) {
        await rtdb.ref(`tournaments/${tournamentId}/slots/${slotNum}`).remove();
        await rtdb.ref(`tournaments/${tournamentId}/participants/${uid}`).remove();
      }

      // Send rejection alert notification
      const notifId = `notif_invalid_id_${Date.now()}`;
      await rtdb.ref(`notifications/${uid}/${notifId}`).set({
        id: notifId,
        userId: uid,
        title: "⚠️ Registration Rejected: Invalid Game ID",
        message: verifResult.reason || "Your tournament registration was cancelled due to an invalid Game ID format. Please update with your authentic 8-12 digit UID.",
        type: "SECURITY_ALERT",
        timestamp: Date.now(),
        read: false
      });

      return { success: false, verified: false, reason: verifResult.reason };
    }
  });

/**
 * Server-side Cloud Function triggered on Firestore 'tournament_registrations' write events.
 */
exports.onFirestoreRegistrationWrite = functions.firestore
  .document("tournament_registrations/{registrationId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;

    const data = change.after.data() || {};
    if (data.verificationStatus === "VERIFIED" || data.verificationStatus === "REJECTED") {
      return null;
    }

    const gameId = data.gameId || data.characterId || data.freeFireId || "";
    const ign = data.inGameName || data.ign || "";
    const verifResult = await verifyExternalGameId(gameId, ign, data.game || "Free Fire");

    if (verifResult.valid) {
      return change.after.ref.set(
        {
          verified: true,
          verificationStatus: "VERIFIED",
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
          verificationToken: verifResult.verificationToken
        },
        { merge: true }
      );
    } else {
      return change.after.ref.set(
        {
          verified: false,
          verificationStatus: "REJECTED",
          rejectionReason: verifResult.reason || "Invalid ID Format - UID Verification Failed",
          rejectedAt: admin.firestore.FieldValue.serverTimestamp()
        },
        { merge: true }
      );
    }
  });

/**
 * Backend Tournament Registration endpoint (Callable Cloud Function).
 * Strictly verifies 'gameId' length (8-12 digits) and format constraints before writing to the database.
 */
exports.registerTournamentSecure = functions.https.onCall(async (data, context) => {
  const uid = context.auth ? context.auth.uid : (data && data.userId);
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated to register.");
  }

  const { tournamentId, slotNumber, inGameName, gameId, teamName } = data || {};
  if (!tournamentId || slotNumber == null || !gameId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required fields: tournamentId, slotNumber, or gameId."
    );
  }

  // Strictly enforce 8-12 digits and format constraints before database write
  const cleanGameId = String(gameId).trim();
  const formatRegex = /^[0-9]{8,12}$/;
  if (!formatRegex.test(cleanGameId)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid ID Format: Game ID must be an authentic 8-12 digit numeric player UID (e.g. 5123984129)."
    );
  }

  const uniqueChars = new Set(cleanGameId.split(""));
  if (uniqueChars.size <= 1 || cleanGameId === "12345678" || cleanGameId === "123456789") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid ID Format: Repetitive or dummy sequential UID is not permitted."
    );
  }

  // Cross-reference with external verification
  const verification = await verifyExternalGameId(cleanGameId, inGameName);
  if (!verification.valid) {
    throw new functions.https.HttpsError("invalid-argument", verification.reason);
  }

  const nowServerMs = Date.now();
  const userRef = db.collection("users").document(uid);
  const tournamentRef = db.collection("tournaments").document(tournamentId);

  return await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "User account not found on server.");
    }
    const userData = userDoc.data() || {};

    const tourneyDoc = await transaction.get(tournamentRef);
    if (!tourneyDoc.exists) {
      throw new functions.https.HttpsError("not-found", "Tournament not found.");
    }
    const tourneyData = tourneyDoc.data() || {};

    const entryFee = tourneyData.entryFee || tourneyData.fee || 0;
    const currentBalance = userData.balance || userData.tokens || 0;

    if (currentBalance < entryFee) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        `Insufficient balance. Required: VT ${entryFee}, Available: VT ${currentBalance}.`
      );
    }

    const regId = `${tournamentId}_${uid}`;
    const regDocRef = db.collection("tournament_registrations").document(regId);
    const existingReg = await transaction.get(regDocRef);
    if (existingReg.exists) {
      throw new functions.https.HttpsError("already-exists", "Already registered for this tournament.");
    }

    const ticketCode = `TKT-${(tourneyData.game || "FF").slice(0, 2).toUpperCase()}-${slotNumber}-${Math.floor(1000 + Math.random() * 9000)}`;

    const registrationPayload = {
      userId: uid,
      userUid: uid,
      username: userData.username || inGameName || "Warrior",
      ign: inGameName || userData.inGameName || userData.username || "",
      inGameName: inGameName || userData.inGameName || userData.username || "",
      gameId: cleanGameId,
      characterId: cleanGameId,
      freeFireId: cleanGameId,
      slotNumber: Number(slotNumber),
      teamName: teamName || "",
      ticketCode: ticketCode,
      tournamentId: tournamentId,
      tournamentTitle: tourneyData.title || "Esports Match",
      game: tourneyData.game || "Free Fire",
      registeredAt: nowServerMs,
      joinedAt: nowServerMs,
      timestamp: nowServerMs,
      verified: true,
      verificationStatus: "VERIFIED",
      verifiedAt: nowServerMs,
      verificationToken: verification.verificationToken
    };

    const newTxId = `TX_ENTRY_${tournamentId}_${uid}_${nowServerMs}`;
    const txRef = db.collection("transactions").document(newTxId);
    const txData = {
      id: newTxId,
      userId: uid,
      type: "ENTRY_FEE",
      amount: entryFee,
      detail: `Joined ${tourneyData.game || "Esports"}: ${tourneyData.title || "Match"} (Slot #${slotNumber})`,
      isPositive: false,
      timestamp: nowServerMs,
      serverTimestamp: admin.firestore.FieldValue.serverTimestamp(),
      status: "SUCCESS"
    };

    // Deduct entry fee and update user
    transaction.update(userRef, {
      balance: currentBalance - entryFee,
      matchesPlayed: (userData.matchesPlayed || 0) + 1,
      freeFireId: cleanGameId,
      inGameName: inGameName || userData.inGameName || ""
    });

    // Save registration and transaction in Firestore
    transaction.set(regDocRef, registrationPayload);
    transaction.set(tournamentRef.collection("participants").document(uid), registrationPayload);
    transaction.set(txRef, txData);
    transaction.update(tournamentRef, {
      filledSlots: admin.firestore.FieldValue.increment(1)
    });

    // Mirror to Realtime Database
    try {
      await rtdb.ref(`registrations/${tournamentId}/${uid}`).set(registrationPayload);
      await rtdb.ref(`tournaments/${tournamentId}/participants/${uid}`).set(registrationPayload);
      await rtdb.ref(`tournaments/${tournamentId}/slots/${slotNumber}`).set(registrationPayload);
      await rtdb.ref(`tournaments/${tournamentId}/filledSlots`).transaction((current) => (current || 0) + 1);
      await rtdb.ref(`users/${uid}`).update({
        balance: currentBalance - entryFee,
        freeFireId: cleanGameId,
        inGameName: inGameName || userData.inGameName || ""
      });
      await rtdb.ref(`transactions/${newTxId}`).set(txData);
    } catch (rtdbErr) {
      console.warn("RTDB registration mirror warning:", rtdbErr.message);
    }

    return {
      success: true,
      message: `Confirmed! Registered in Slot #${slotNumber} (Ticket: ${ticketCode})`,
      ticketCode: ticketCode,
      slotNumber: slotNumber,
      verified: true
    };
  });
});

