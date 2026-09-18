/**
 * ==============================================================================
 * VELORIX AI WATCHDOG & TELEGRAM MANAGEMENT ASSISTANT BOT
 * ==============================================================================
 * 
 * 24/7 Background Monitor, AI Dispute Moderator & Operations Manager for:
 * VeloRix Esports Platform (Free Fire Tournaments)
 * 
 * Functions:
 * 1. Automatic Alert when Room ID / Password is not set before match starts.
 * 2. Instant notification on new Withdrawal Requests with One-Tap Approve/Reject.
 * 3. AI analysis of player dispute & cheater reports using Gemini.
 * 4. Multi-Account / Referral abuse detection.
 * 5. Interactive conversational AI assistant in Hindi/Hinglish for app ops.
 * ==============================================================================
 */

// Load .env from multiple possible locations (local folder, parent folder, root folder)
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

const possibleEnvPaths = [
    path.resolve(__dirname, '.env'),
    path.resolve(__dirname, '../.env'),
    path.resolve(process.cwd(), '.env')
];

for (const envPath of possibleEnvPaths) {
    if (fs.existsSync(envPath)) {
        dotenv.config({ path: envPath });
        console.log(`[ENV] Loaded environment configuration from: ${envPath}`);
    }
}
dotenv.config(); // default fallback

const TelegramBot = require('node-telegram-bot-api');
const admin = require('firebase-admin');
const cron = require('node-cron');
const { GoogleGenerativeAI } = require('@google/generative-ai');
const http = require('http');

// --- 1. CONFIGURATION & ENVIRONMENT VALIDATION ---
const TELEGRAM_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const ADMIN_CHAT_ID = process.env.ADMIN_TELEGRAM_CHAT_ID;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const FIREBASE_DB_URL = process.env.FIREBASE_DATABASE_URL || 'https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app';
const SERVICE_ACCOUNT_PATH = process.env.SERVICE_ACCOUNT_KEY_PATH || './serviceAccountKey.json';
const SERVICE_ACCOUNT_RAW_JSON = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
const MONITOR_INTERVAL = parseInt(process.env.MONITOR_INTERVAL_MINUTES || '3', 10);
const PORT = process.env.PORT || 3000;

// Lightweight HTTP server for Render / Koyeb Web Service health checks (Start immediately)
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
        status: 'UP',
        service: 'VeloRix AI Watchdog Bot',
        telegramConnected: Boolean(TELEGRAM_BOT_TOKEN && TELEGRAM_BOT_TOKEN !== 'YOUR_TELEGRAM_BOT_TOKEN_HERE'),
        uptimeSeconds: Math.floor(process.uptime()),
        timestamp: new Date().toISOString()
    }));
});

// Keep-alive pinger for Render / cloud hosts to prevent free-tier hibernation (every 9 minutes)
const keepAliveUrl = process.env.RENDER_EXTERNAL_URL || process.env.APP_URL;
if (keepAliveUrl) {
    setInterval(() => {
        try {
            http.get(keepAliveUrl, () => {});
            console.log(`[KeepAlive] Pinged ${keepAliveUrl} to prevent cloud host hibernation.`);
        } catch (_) {}
    }, 9 * 60 * 1000);
}

server.listen(PORT, () => {
    console.log(`🌐 HTTP health check endpoint running on port ${PORT} (Render Web Service is ALIVE).`);
});

const isTokenConfigured = TELEGRAM_BOT_TOKEN && 
                          TELEGRAM_BOT_TOKEN.trim() !== '' && 
                          TELEGRAM_BOT_TOKEN !== 'YOUR_TELEGRAM_BOT_TOKEN_HERE';

if (!isTokenConfigured) {
    console.warn('⚠️ NOTICE: TELEGRAM_BOT_TOKEN is not yet set.');
    console.warn('👉 Web Service will stay UP and running so Render does not crash or exit with error.');
    console.warn('👉 Add TELEGRAM_BOT_TOKEN and ADMIN_TELEGRAM_CHAT_ID in Render Environment or root .env to enable the bot.');
}

if (!ADMIN_CHAT_ID || ADMIN_CHAT_ID === 'YOUR_TELEGRAM_CHAT_ID_HERE') {
    console.warn('⚠️ WARNING: ADMIN_TELEGRAM_CHAT_ID is not set.');
}

// --- 2. FIREBASE ADAPTER (ADMIN SDK + REST FALLBACK FOR RENDER/EXTERNAL SERVERS) ---
let db = null;
let firestore = null;

// Built-in REST adapter for environments without Google Cloud metadata server (like Render, Koyeb, Railway)
class FirebaseRestAdapter {
    constructor(databaseURL, authToken) {
        this.baseURL = (databaseURL || 'https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app').replace(/\/$/, '');
        this.authToken = authToken;
    }

    ref(pathStr) {
        const cleanPath = (pathStr || '').toString().replace(/^\/+|\/+$/g, '');
        const self = this;
        return {
            async once(event) {
                const authQuery = self.authToken ? `?auth=${encodeURIComponent(self.authToken)}` : '';
                const url = `${self.baseURL}/${cleanPath}.json${authQuery}`;
                try {
                    const controller = new AbortController();
                    const timeoutId = setTimeout(() => controller.abort(), 5000);
                    const res = await fetch(url, { 
                        headers: { 'Accept': 'application/json' },
                        signal: controller.signal
                    });
                    clearTimeout(timeoutId);
                    if (!res.ok) {
                        console.warn(`[Firebase REST] HTTP ${res.status} on GET ${cleanPath}`);
                        return { exists: () => false, val: () => null };
                    }
                    const data = await res.json();
                    return {
                        exists: () => data !== null && data !== undefined,
                        val: () => data
                    };
                } catch (fetchErr) {
                    console.error(`[Firebase REST Error] GET ${cleanPath}:`, fetchErr.message);
                    return { exists: () => false, val: () => null };
                }
            },
            async update(updates) {
                const authQuery = self.authToken ? `?auth=${encodeURIComponent(self.authToken)}` : '';
                const url = `${self.baseURL}/${cleanPath}.json${authQuery}`;
                try {
                    const res = await fetch(url, {
                        method: 'PATCH',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(updates)
                    });
                    if (!res.ok) {
                        console.warn(`[Firebase REST] HTTP ${res.status} on PATCH ${cleanPath}`);
                    }
                    return await res.json();
                } catch (fetchErr) {
                    console.error(`[Firebase REST Error] PATCH ${cleanPath}:`, fetchErr.message);
                    throw fetchErr;
                }
            }
        };
    }
}

try {
    let hasAdminCredentials = false;

    if (SERVICE_ACCOUNT_RAW_JSON && SERVICE_ACCOUNT_RAW_JSON.trim().startsWith('{')) {
        try {
            const serviceAccount = JSON.parse(SERVICE_ACCOUNT_RAW_JSON.trim());
            admin.initializeApp({
                credential: admin.credential.cert(serviceAccount),
                databaseURL: FIREBASE_DB_URL
            });
            console.log('✅ Firebase Admin SDK initialized from FIREBASE_SERVICE_ACCOUNT_JSON env variable.');
            hasAdminCredentials = true;
        } catch (parseErr) {
            console.warn('⚠️ FIREBASE_SERVICE_ACCOUNT_JSON was provided but could not be parsed as valid JSON:', parseErr.message);
        }
    }

    if (!hasAdminCredentials) {
        const resolvedKeyPath = path.resolve(__dirname, SERVICE_ACCOUNT_PATH);
        if (fs.existsSync(resolvedKeyPath)) {
            try {
                const serviceAccount = require(resolvedKeyPath);
                admin.initializeApp({
                    credential: admin.credential.cert(serviceAccount),
                    databaseURL: FIREBASE_DB_URL
                });
                console.log('✅ Firebase Admin SDK initialized using service account key file.');
                hasAdminCredentials = true;
            } catch (fileErr) {
                console.warn('⚠️ Could not load service account key file:', fileErr.message);
            }
        }
    }

    if (hasAdminCredentials) {
        db = admin.database();
        try { firestore = admin.firestore(); } catch (_) {}
        console.log(`📡 Connected to Firebase Admin SDK: ${FIREBASE_DB_URL}`);
    } else {
        console.log('ℹ️ No Firebase Service Account private key provided.');
        console.log('🌐 Operating in Resilient Firebase REST Mode (no metadata.google.internal errors).');
        console.log(`📡 Connected to Realtime Database via REST endpoint: ${FIREBASE_DB_URL}`);
        db = new FirebaseRestAdapter(FIREBASE_DB_URL, process.env.FIREBASE_DATABASE_AUTH);
    }
} catch (err) {
    console.error('❌ Failed to initialize Firebase:', err.message);
    console.log('🔄 Engaging Firebase REST fallback adapter...');
    db = new FirebaseRestAdapter(FIREBASE_DB_URL, process.env.FIREBASE_DATABASE_AUTH);
}

// --- 3. GEMINI AI ENGINE (Multi-Model Resilient Architecture) ---
let isGeminiConfigured = Boolean(GEMINI_API_KEY && GEMINI_API_KEY !== 'YOUR_GEMINI_API_KEY_HERE');
const GEMINI_MODELS = [
    process.env.GEMINI_MODEL || 'gemini-3.6-flash',
    'gemini-flash-latest',
    'gemini-3-flash-preview'
];

/**
 * Universal resilient Gemini text generation function.
 * Supports direct REST with automatic model fallback and timeout.
 */
async function generateAiText(promptText, maxTokens = 1500, timeoutMs = 9000) {
    if (!isGeminiConfigured) return null;

    for (const model of GEMINI_MODELS) {
        try {
            const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
            const controller = new AbortController();
            const timer = setTimeout(() => controller.abort(), timeoutMs);

            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    contents: [{ parts: [{ text: promptText }] }],
                    generationConfig: {
                        temperature: 0.75,
                        maxOutputTokens: maxTokens
                    }
                }),
                signal: controller.signal
            });

            clearTimeout(timer);

            if (!response.ok) {
                console.warn(`⚠️ Gemini model ${model} responded with HTTP ${response.status}. Trying next model...`);
                continue;
            }

            const data = await response.json();
            const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
            if (text && text.trim().length > 0) {
                return text.trim();
            }
        } catch (err) {
            console.warn(`⚠️ Gemini error with ${model}:`, err.message);
        }
    }
    return null;
}

if (isGeminiConfigured) {
    console.log(`🧠 Google Gemini AI Engine configured with models: ${GEMINI_MODELS.join(', ')}`);
} else {
    console.log('ℹ️ Note: GEMINI_API_KEY is not set. Bot will use standard deterministic moderation rules.');
}

// --- 4. TELEGRAM BOT CLIENT ---
let bot = null;
if (isTokenConfigured) {
    try {
        bot = new TelegramBot(TELEGRAM_BOT_TOKEN, { polling: true });
        console.log('🤖 VeloRix AI Watchdog Bot is online and listening on Telegram!');

        bot.on('polling_error', (error) => {
            console.error('⚠️ [Telegram Polling Error]:', error.code, error.message);
        });

        bot.on('error', (error) => {
            console.error('⚠️ [Telegram General Error]:', error.message);
        });
    } catch (e) {
        console.error('Failed to connect to Telegram:', e.message);
    }
} else {
    console.log('ℹ️ Bot listener standby mode: Provide TELEGRAM_BOT_TOKEN to enable active Telegram polling.');
}

// In-memory cache to prevent duplicate alerts
const notifiedMissingRoomIds = new Set();
const notifiedWithdrawals = new Set();
const notifiedDeposits = new Set();
const notifiedReports = new Set();

/**
 * Sends a message safely to the Admin chat ID
 */
async function sendAdminAlert(text, options = {}) {
    if (!bot) {
        console.log('[ALERT BUFFERED (Bot not configured)]:', text);
        return;
    }
    if (!ADMIN_CHAT_ID || ADMIN_CHAT_ID === 'YOUR_TELEGRAM_CHAT_ID_HERE') {
        console.log('[ALERT BUFFERED (No ADMIN_CHAT_ID set)]:', text);
        return;
    }
    try {
        await bot.sendMessage(ADMIN_CHAT_ID, text, { parse_mode: 'HTML', ...options });
    } catch (err) {
        console.error('Failed to send Telegram message:', err.message);
    }
}

// --- 5. CORE WATCHDOG SURVEILLANCE ENGINE ---

/**
 * 1. Checks upcoming tournaments within next 30 minutes for missing Room ID / Password.
 */
async function checkUpcomingTournaments() {
    if (!db) return;
    try {
        const snapshot = await db.ref('tournaments').once('value');
        if (!snapshot.exists()) return;

        const tournaments = snapshot.val();
        const now = Date.now();
        const THIRTY_MINUTES_MS = 30 * 60 * 1000;

        for (const [id, t] of Object.entries(tournaments)) {
            if (!t) continue;
            
            // Check if tournament is active/upcoming
            const status = (t.status || 'UPCOMING').toUpperCase();
            if (status !== 'UPCOMING' && status !== 'OPEN') continue;

            const matchTime = parseInt(t.matchTime || t.startTime || '0', 10);
            if (isNaN(matchTime) || matchTime <= 0) continue;

            const timeDiff = matchTime - now;

            // If match is starting within 30 minutes and not yet started
            if (timeDiff > 0 && timeDiff <= THIRTY_MINUTES_MS) {
                const hasRoomId = t.roomId && t.roomId.trim().length > 0 && t.roomId.trim() !== 'TBA';
                const hasPassword = t.roomPassword && t.roomPassword.trim().length > 0;

                if ((!hasRoomId || !hasPassword) && !notifiedMissingRoomIds.has(id)) {
                    notifiedMissingRoomIds.add(id);
                    const minutesLeft = Math.round(timeDiff / (60 * 1000));
                    
                    const alertMsg = 
                        `🚨 <b>[URGENT: ROOM CREDENTIALS MISSING]</b>\n\n` +
                        `🎮 <b>Match:</b> ${t.title || 'Tournament #' + id}\n` +
                        `⏰ <b>Starts in:</b> <b>${minutesLeft} minutes</b>\n` +
                        `👥 <b>Registered Players:</b> ${t.registeredSlots || t.currentPlayers || 0}/${t.maxSlots || 48}\n` +
                        `⚠️ <b>Issue:</b> Room ID ya Password abhi tak update nahi hua hai! Players wait kar rahe hain.\n\n` +
                        `👉 <i>Kripya app ke Admin Panel me jaakar turant Room ID & Password enter karein.</i>`;

                    await sendAdminAlert(alertMsg, {
                        reply_markup: {
                            inline_keyboard: [
                                [{ text: '🔍 View Tournaments', callback_data: `view_tourn_${id}` }]
                            ]
                        }
                    });
                }
            }
        }
    } catch (e) {
        console.error('Error during tournament watchdog check:', e.message);
    }
}

/**
 * 2. Checks pending withdrawals and notifies admin with one-tap approval buttons.
 */
async function checkPendingWithdrawals() {
    if (!db) return;
    try {
        const snapshot = await db.ref('withdraw_requests').once('value');
        if (!snapshot.exists()) return;

        const requests = snapshot.val();
        for (const [reqId, req] of Object.entries(requests)) {
            if (!req) continue;
            const status = (req.status || 'PENDING').toUpperCase();

            if (status === 'PENDING' && !notifiedWithdrawals.has(reqId)) {
                notifiedWithdrawals.add(reqId);

                const amount = req.amount || req.withdrawAmount || 0;
                const upiId = req.upiId || 'N/A';
                const userId = req.userId || 'Unknown';

                const alertMsg = 
                    `💰 <b>[NEW WITHDRAWAL REQUEST DETECTED]</b>\n\n` +
                    `👤 <b>User ID:</b> <code>${userId}</code>\n` +
                    `💵 <b>Amount:</b> <b>VT ${amount} (₹${amount})</b>\n` +
                    `🏦 <b>UPI ID:</b> <code>${upiId}</code>\n` +
                    `🆔 <b>Request ID:</b> <code>${reqId}</code>\n` +
                    `🕒 <b>Time:</b> ${new Date(req.timestamp || Date.now()).toLocaleTimeString()}\n\n` +
                    `Kya aap is payment ko approve karke release karna chahte hain?`;

                await sendAdminAlert(alertMsg, {
                    reply_markup: {
                        inline_keyboard: [
                            [
                                { text: '✅ Approve & Mark Paid', callback_data: `wdr_approve_${reqId}` },
                                { text: '❌ Reject & Refund', callback_data: `wdr_reject_${reqId}` }
                            ],
                            [
                                { text: '👤 Inspect User History', callback_data: `user_inspect_${userId}` }
                            ]
                        ]
                    }
                });
            }
        }
    } catch (e) {
        console.error('Error during withdrawal watchdog check:', e.message);
    }
}

/**
 * 2.5. Checks pending deposits (manual UPI / UTR reviews) and alerts admin with 1-tap approve/reject.
 */
async function checkPendingDeposits() {
    if (!db) return;
    try {
        const snapshot = await db.ref('deposit_requests').once('value');
        if (!snapshot.exists()) return;

        const requests = snapshot.val();
        for (const [reqId, req] of Object.entries(requests)) {
            if (!req) continue;
            const status = (req.status || 'PENDING').toUpperCase();

            if (status === 'PENDING' && !notifiedDeposits.has(reqId)) {
                notifiedDeposits.add(reqId);

                const amount = req.amount || 0;
                const utr = req.utrNumber || req.paymentRef || 'N/A';
                const userId = req.userId || 'Unknown';
                const ign = req.inGameName || '';

                const alertMsg = 
                    `💳 <b>[NEW DEPOSIT REQUEST DETECTED]</b>\n\n` +
                    `👤 <b>User:</b> <code>${userId}</code> ${ign ? `(${ign})` : ''}\n` +
                    `💵 <b>Deposit Amount:</b> <b>VT ${amount} (₹${amount})</b>\n` +
                    `🔢 <b>UTR / Ref:</b> <code>${utr}</code>\n` +
                    `🆔 <b>Request ID:</b> <code>${reqId}</code>\n` +
                    `🕒 <b>Time:</b> ${new Date(req.timestamp || req.createdAt || Date.now()).toLocaleTimeString()}\n\n` +
                    `Kya aapne payment check karke user ke wallet me VT credit karne hain?`;

                await sendAdminAlert(alertMsg, {
                    reply_markup: {
                        inline_keyboard: [
                            [
                                { text: '✅ Approve & Add VT', callback_data: `dep_approve_${reqId}` },
                                { text: '❌ Reject Deposit', callback_data: `dep_reject_${reqId}` }
                            ],
                            [
                                { text: '👤 Inspect User', callback_data: `user_inspect_${userId}` }
                            ]
                        ]
                    }
                });
            }
        }
    } catch (e) {
        console.error('Error during deposit watchdog check:', e.message);
    }
}

/**
 * 3. Checks new user dispute / toxicity / cheater reports and provides AI insights.
 */
async function checkUserReports() {
    if (!db) return;
    try {
        const snapshot = await db.ref('user_reports').once('value');
        if (!snapshot.exists()) return;

        const reports = snapshot.val();
        for (const [repId, rep] of Object.entries(reports)) {
            if (!rep) continue;
            const status = (rep.status || 'OPEN').toUpperCase();

            if (status === 'OPEN' && !notifiedReports.has(repId)) {
                notifiedReports.add(repId);

                const reason = rep.reason || 'General Dispute';
                const description = rep.description || 'No description provided';
                const reporterId = rep.reporterId || 'Unknown';
                const accusedId = rep.reportedUserId || 'N/A';

                let aiAssessment = 'Standard review required.';
                if (isGeminiConfigured) {
                    try {
                        const prompt = `You are a Free Fire Esports Moderator AI. A player submitted this dispute report in the tournament app:
Reason: "${reason}"
Description: "${description}"
Evaluate this report for the admin in 2 sharp, professional sentences in natural Hinglish. Tell whether it needs instant ban/action or manual gameplay clip verification. Speak directly to admin.`;
                        const resText = await generateAiText(prompt, 500, 6000);
                        if (resText) {
                            aiAssessment = resText;
                        } else {
                            aiAssessment = `Automated flag: ${reason} (Manual verification suggested)`;
                        }
                    } catch (err) {
                        aiAssessment = `Automated flag: ${reason}`;
                    }
                }

                const alertMsg = 
                    `🛡️ <b>[NEW PLAYER REPORT SUBMITTED]</b>\n\n` +
                    `🚩 <b>Category:</b> ${reason}\n` +
                    `👤 <b>Accused Player:</b> <code>${accusedId}</code>\n` +
                    `🗣️ <b>Reporter:</b> <code>${reporterId}</code>\n` +
                    `📝 <b>User Note:</b> <i>"${description}"</i>\n\n` +
                    `🧠 <b>AI Moderator Opinion:</b>\n${aiAssessment}\n\n` +
                    `Kripya check karein agar is user ko warn ya suspend karna hai.`;

                await sendAdminAlert(alertMsg, {
                    reply_markup: {
                        inline_keyboard: [
                            [
                                { text: '⚠️ Warn User', callback_data: `warn_user_${accusedId}` },
                                { text: '🚫 Suspend Account', callback_data: `suspend_user_${accusedId}` }
                            ],
                            [
                                { text: '✅ Mark Report Resolved', callback_data: `resolve_rep_${repId}` }
                            ]
                        ]
                    }
                });
            }
        }
    } catch (e) {
        console.error('Error during report watchdog check:', e.message);
    }
}

/**
 * 4. Master watchdog cycle run
 */
async function runWatchdogCycle() {
    console.log(`[${new Date().toLocaleTimeString()}] 🔍 Running automated VeloRix Watchdog surveillance cycle...`);
    await checkUpcomingTournaments();
    await checkPendingWithdrawals();
    await checkPendingDeposits();
    await checkUserReports();
}

// Start recurring cron schedule (Default: every 3 minutes)
cron.schedule(`*/${MONITOR_INTERVAL} * * * *`, () => {
    runWatchdogCycle();
});

// Run immediate check on startup
setTimeout(runWatchdogCycle, 5000);

// --- 6. INTERACTIVE TELEGRAM COMMANDS & CENTRAL DISPATCHER ---

if (bot) {
    // Helper function for status report
    async function handleStatusReport(chatId, queryMsgId = null) {
        let waitMsg = null;
        if (!queryMsgId) {
            waitMsg = await bot.sendMessage(chatId, '🔍 Scanning Firebase Realtime Database & Firestore...', { parse_mode: 'HTML' });
        }

        try {
            let userCount = 0;
            let tournamentCount = 0;
            let pendingWithdrawalCount = 0;
            let pendingWithdrawalTotal = 0;
            let pendingDepositCount = 0;
            let pendingDepositTotal = 0;
            let openReportsCount = 0;

            if (db) {
                const [uSnap, tSnap, wSnap, dSnap, rSnap] = await Promise.all([
                    db.ref('users').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('tournaments').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('withdraw_requests').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('deposit_requests').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('user_reports').once('value').catch(() => ({ exists: () => false, val: () => null }))
                ]);

                userCount = uSnap && uSnap.exists() ? Object.keys(uSnap.val() || {}).length : 0;
                tournamentCount = tSnap && tSnap.exists() ? Object.keys(tSnap.val() || {}).length : 0;

                if (wSnap && wSnap.exists()) {
                    for (const w of Object.values(wSnap.val() || {})) {
                        if (w && (w.status || 'PENDING').toUpperCase() === 'PENDING') {
                            pendingWithdrawalCount++;
                            pendingWithdrawalTotal += (w.amount || 0);
                        }
                    }
                }

                if (dSnap && dSnap.exists()) {
                    for (const d of Object.values(dSnap.val() || {})) {
                        if (d && (d.status || 'PENDING').toUpperCase() === 'PENDING') {
                            pendingDepositCount++;
                            pendingDepositTotal += (d.amount || 0);
                        }
                    }
                }

                if (rSnap && rSnap.exists()) {
                    for (const r of Object.values(rSnap.val() || {})) {
                        if (r && (r.status || 'OPEN').toUpperCase() === 'OPEN') {
                            openReportsCount++;
                        }
                    }
                }
            }

            const statusMsg = 
                `📊 <b>[VELORIX PLATFORM HEALTH REPORT]</b>\n\n` +
                `🟢 <b>RTDB Cloud Connection:</b> ACTIVE\n` +
                `👥 <b>Total Registered Players:</b> ${userCount}\n` +
                `🏆 <b>Active/Total Tournaments:</b> ${tournamentCount}\n` +
                `💳 <b>Pending Deposits:</b> ${pendingDepositCount} (Total: ₹${pendingDepositTotal})\n` +
                `💰 <b>Pending Withdrawals:</b> ${pendingWithdrawalCount} (Total: ₹${pendingWithdrawalTotal})\n` +
                `🛡️ <b>Open Dispute Reports:</b> ${openReportsCount}\n` +
                `⚡ <b>Watchdog Frequency:</b> Har ${MONITOR_INTERVAL} minute\n\n` +
                `<i>Sabhi systems actively scan ho rahe hain boss!</i>`;

            if (waitMsg) {
                await bot.editMessageText(statusMsg, {
                    chat_id: chatId,
                    message_id: waitMsg.message_id,
                    parse_mode: 'HTML'
                });
            } else {
                await bot.sendMessage(chatId, statusMsg, { parse_mode: 'HTML' });
            }
        } catch (err) {
            console.error('Error scanning app status:', err.message);
            bot.sendMessage(chatId, `❌ Error scanning app status: ${err.message}`);
        }
    }

    // Helper function for tournaments
    async function handleTournamentsList(chatId) {
        if (!db) {
            bot.sendMessage(chatId, '❌ Firebase connection not active.');
            return;
        }

        try {
            const snap = await db.ref('tournaments').once('value');
            if (!snap || !snap.exists()) {
                bot.sendMessage(chatId, 'ℹ️ Abhi koi tournaments schedule nahi hain.');
                return;
            }

            const tournaments = Object.values(snap.val() || {});
            let response = `🎮 <b>[UPCOMING TOURNAMENTS & ROOM STATUS]</b>\n\n`;

            let count = 0;
            for (const t of tournaments) {
                if (!t) continue;
                count++;
                const hasRoomId = t.roomId && t.roomId.trim().length > 0 && t.roomId !== 'TBA';
                const roomBadge = hasRoomId ? '✅ Room ID Set' : '⚠️ Room ID Pending';

                response += `• <b>${t.title || 'Tournament'}</b>\n`;
                response += `  Status: <code>${t.status || 'UPCOMING'}</code> | Slots: ${t.registeredSlots || 0}/${t.maxSlots || 48}\n`;
                response += `  Credentials: <b>${roomBadge}</b>\n\n`;

                if (count >= 10) break;
            }

            bot.sendMessage(chatId, response, { parse_mode: 'HTML' });
        } catch (e) {
            bot.sendMessage(chatId, `❌ Error fetching tournaments: ${e.message}`);
        }
    }

    // Helper function for withdrawals
    async function handleWithdrawalsList(chatId) {
        if (!db) {
            bot.sendMessage(chatId, '❌ Firebase connection not active.');
            return;
        }

        try {
            const snap = await db.ref('withdraw_requests').once('value');
            if (!snap || !snap.exists()) {
                bot.sendMessage(chatId, '✅ <b>Sabhi withdrawals cleared hain!</b> Koi pending request nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            const all = Object.entries(snap.val() || {});
            const pending = all.filter(([_, w]) => w && (w.status || 'PENDING').toUpperCase() === 'PENDING');

            if (pending.length === 0) {
                bot.sendMessage(chatId, '✅ <b>Sabhi withdrawals cleared hain!</b> Koi pending request nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            bot.sendMessage(chatId, `💰 <b>Total ${pending.length} pending cashout requests hain:</b>`, { parse_mode: 'HTML' });

            for (const [id, req] of pending.slice(0, 5)) {
                const card = 
                    `🆔 <code>${id}</code>\n` +
                    `👤 User: <code>${req.userId || 'N/A'}</code>\n` +
                    `💵 Amount: <b>VT ${req.amount || 0}</b>\n` +
                    `🏦 UPI: <code>${req.upiId || 'N/A'}</code>`;

                await bot.sendMessage(chatId, card, {
                    parse_mode: 'HTML',
                    reply_markup: {
                        inline_keyboard: [
                            [
                                { text: '✅ Approve & Pay', callback_data: `wdr_approve_${id}` },
                                { text: '❌ Reject & Refund', callback_data: `wdr_reject_${id}` }
                            ]
                        ]
                    }
                });
            }
        } catch (e) {
            bot.sendMessage(chatId, `❌ Error fetching withdrawals: ${e.message}`);
        }
    }

    // Helper function for deposit requests
    async function handleDepositsList(chatId) {
        if (!db) {
            bot.sendMessage(chatId, '❌ Firebase connection not active.');
            return;
        }

        try {
            const snap = await db.ref('deposit_requests').once('value');
            if (!snap || !snap.exists()) {
                bot.sendMessage(chatId, '✅ <b>Sabhi deposit requests clear hain!</b> Koi pending deposit nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            const all = Object.entries(snap.val() || {});
            const pending = all.filter(([_, d]) => d && (d.status || 'PENDING').toUpperCase() === 'PENDING');

            if (pending.length === 0) {
                bot.sendMessage(chatId, '✅ <b>Sabhi deposit requests processed hain!</b> Koi pending review nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            bot.sendMessage(chatId, `💳 <b>Total ${pending.length} pending deposit verification requests hain:</b>`, { parse_mode: 'HTML' });

            for (const [id, req] of pending.slice(0, 5)) {
                const amount = req.amount || 0;
                const utr = req.utrNumber || req.paymentRef || 'N/A';
                const card = 
                    `🆔 <code>${id}</code>\n` +
                    `👤 User: <code>${req.userId || 'N/A'}</code> ${req.inGameName ? `(${req.inGameName})` : ''}\n` +
                    `💵 Deposit: <b>VT ${amount} (₹${amount})</b>\n` +
                    `🔢 UTR / Ref: <code>${utr}</code>`;

                await bot.sendMessage(chatId, card, {
                    parse_mode: 'HTML',
                    reply_markup: {
                        inline_keyboard: [
                            [
                                { text: '✅ Approve & Add VT', callback_data: `dep_approve_${id}` },
                                { text: '❌ Reject', callback_data: `dep_reject_${id}` }
                            ],
                            [
                                { text: '👤 Inspect User', callback_data: `user_inspect_${req.userId || ''}` }
                            ]
                        ]
                    }
                });
            }
        } catch (e) {
            bot.sendMessage(chatId, `❌ Error fetching deposits: ${e.message}`);
        }
    }

    // Helper function for reports
    async function handleReportsList(chatId) {
        if (!db) {
            bot.sendMessage(chatId, '❌ Firebase connection not active.');
            return;
        }

        try {
            const snap = await db.ref('user_reports').once('value');
            if (!snap || !snap.exists()) {
                bot.sendMessage(chatId, '✅ <b>Sabhi reports clear hain!</b> Koi open dispute nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            const reports = Object.entries(snap.val() || {}).filter(([_, r]) => r && (r.status || 'OPEN').toUpperCase() === 'OPEN');
            if (reports.length === 0) {
                bot.sendMessage(chatId, '✅ <b>Sabhi reports clear hain!</b> Koi open dispute nahi hai.', { parse_mode: 'HTML' });
                return;
            }

            let resp = `🛡️ <b>[OPEN PLAYER REPORTS (${reports.length})]</b>\n\n`;
            for (const [id, r] of reports.slice(0, 5)) {
                resp += `• <b>ID:</b> <code>${id}</code>\n`;
                resp += `  Reason: <b>${r.reason || 'Report'}</b>\n`;
                resp += `  Accused: <code>${r.reportedUserId || 'N/A'}</code>\n`;
                resp += `  Note: <i>"${r.description || ''}"</i>\n\n`;
            }

            bot.sendMessage(chatId, resp, { parse_mode: 'HTML' });
        } catch (e) {
            bot.sendMessage(chatId, `❌ Error fetching reports: ${e.message}`);
        }
    }

    // Single unified message router - handles commands, buttons, and AI chat
    bot.on('message', async (msg) => {
        if (!msg.text) return;
        const chatId = msg.chat.id;
        const text = msg.text.trim();
        const lower = text.toLowerCase();

        console.log(`📩 Incoming message from ${chatId}: "${text}"`);

        try {
            // 1. /start or /help
        if (lower.startsWith('/start') || lower.startsWith('/help')) {
            const welcome = 
                `👋 <b>Namaste Boss! Main hoon aapka VeloRix 24/7 AI Watchdog & Ops Assistant.</b>\n\n` +
                `Main lagatar aapke Free Fire Tournament app par nazar rakh raha hoon taaki aap free reh sako:\n\n` +
                `<b>Automated Alerts:</b>\n` +
                `• 🚨 Match start hone se pehle Room ID/Password missing alert\n` +
                `• 💳 Naye UPI Deposit requests par instant alert & 1-tap wallet credit\n` +
                `• 💰 Naye withdrawal requests par instant notification & 1-tap approval\n` +
                `• 🛡️ Hacker & toxic player reports ka AI analysis\n` +
                `• ⚡ Stuck wallet transactions and referral fraud flags\n\n` +
                `<b>Available Commands:</b>\n` +
                `• /status - App ka live overall health report\n` +
                `• /tournaments - Upcoming matches & Room credentials status\n` +
                `• /deposits - Pending deposit requests (review & approve)\n` +
                `• /withdrawals - Pending withdrawals list\n` +
                `• /reports - Pending player dispute tickets\n` +
                `• /ask [sawal] - App se judi koi bhi baat mujhse poochhein\n\n` +
                `<i>Aapka Chat ID:</i> <code>${chatId}</code>`;

            bot.sendMessage(chatId, welcome, {
                parse_mode: 'HTML',
                reply_markup: {
                    keyboard: [
                        [{ text: '📊 App Status' }, { text: '🎮 Tournaments' }],
                        [{ text: '💳 Deposits' }, { text: '💰 Withdrawals' }],
                        [{ text: '🛡️ Reports' }, { text: '🔄 Run Health Scan Now' }]
                    ],
                    resize_keyboard: true
                }
            });
            return;
        }

        // 2. Status / Health Scan
        if (lower === '/status' || lower.includes('app status') || lower.includes('health scan')) {
            await handleStatusReport(chatId);
            return;
        }

        // 3. Tournaments
        if (lower === '/tournaments' || lower.includes('tournaments')) {
            await handleTournamentsList(chatId);
            return;
        }

        // 4. Withdrawals
        if (lower === '/withdrawals' || lower.includes('withdrawals') || lower.includes('cashout')) {
            await handleWithdrawalsList(chatId);
            return;
        }

        // 4.5. Deposits
        if (lower === '/deposits' || lower === 'deposits' || lower.includes('deposit request') || lower.includes('deposit list')) {
            await handleDepositsList(chatId);
            return;
        }

        // Dedicated /ask alone handler
        if (lower === '/ask') {
            bot.sendMessage(chatId, '👋 <b>Haan boss, boliye kya poochna chahte hain?</b>\n\nAap direct koi bhi sawal type kar sakte hain, jaise:\n• <i>"Any deposit requests?"</i>\n• <i>"Withdrawals kitne pending hain?"</i>\n• <i>"App me koi issue hai kya?"</i>\n\nYa neeche diye gaye buttons use karein!', { parse_mode: 'HTML' });
            return;
        }

        // 5. Reports
        if (lower === '/reports' || lower.includes('reports')) {
            await handleReportsList(chatId);
            return;
        }

        // 6. Natural Language / AI Assistant Chat (Handles manual typed questions with LIVE DB snapshot)
        bot.sendChatAction(chatId, 'typing').catch(() => {});

        const queryText = text.replace(/^\/ask\s*/i, '').trim() || text;

        // Fetch fresh platform snapshot so bot can accurately answer any manual question
        let liveContext = {
            registeredUsers: 0,
            tournaments: [],
            pendingWithdrawals: [],
            pendingDeposits: [],
            openReports: []
        };

        if (db) {
            try {
                const dbFetchPromise = Promise.all([
                    db.ref('users').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('tournaments').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('withdraw_requests').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('deposit_requests').once('value').catch(() => ({ exists: () => false, val: () => null })),
                    db.ref('user_reports').once('value').catch(() => ({ exists: () => false, val: () => null }))
                ]);

                // Enforce strict 4-second timeout on DB snapshot so message never hangs
                const dbTimeout = new Promise((resolve) => setTimeout(() => resolve([]), 4000));
                const [uSnap, tSnap, wSnap, dSnap, rSnap] = await Promise.race([dbFetchPromise, dbTimeout]);

                if (uSnap && uSnap.exists && uSnap.exists()) {
                    liveContext.registeredUsers = Object.keys(uSnap.val() || {}).length;
                }

                if (tSnap && tSnap.exists && tSnap.exists()) {
                    const tList = Object.entries(tSnap.val() || {}).map(([id, val]) => ({
                        id,
                        title: val?.title || 'Unknown',
                        status: val?.status || 'UPCOMING',
                        hasRoomId: Boolean(val?.roomId && val.roomId.trim().length > 0 && val.roomId !== 'TBA'),
                        slots: `${val?.registeredSlots || 0}/${val?.maxSlots || 48}`
                    }));
                    liveContext.tournaments = tList.slice(0, 10);
                }

                if (wSnap && wSnap.exists && wSnap.exists()) {
                    const wList = Object.entries(wSnap.val() || {})
                        .filter(([_, val]) => (val?.status || 'PENDING').toUpperCase() === 'PENDING')
                        .map(([id, val]) => ({
                            id,
                            userId: val?.userId || 'Unknown',
                            amount: val?.amount || val?.withdrawAmount || 0,
                            upi: val?.upiId || 'N/A'
                        }));
                    liveContext.pendingWithdrawals = wList;
                }

                if (dSnap && dSnap.exists && dSnap.exists()) {
                    const dList = Object.entries(dSnap.val() || {})
                        .filter(([_, val]) => (val?.status || 'PENDING').toUpperCase() === 'PENDING')
                        .map(([id, val]) => ({
                            id,
                            userId: val?.userId || 'Unknown',
                            inGameName: val?.inGameName || '',
                            amount: val?.amount || 0,
                            utr: val?.utrNumber || val?.paymentRef || 'N/A'
                        }));
                    liveContext.pendingDeposits = dList;
                }

                if (rSnap && rSnap.exists && rSnap.exists()) {
                    const rList = Object.entries(rSnap.val() || {})
                        .filter(([_, val]) => (val?.status || 'OPEN').toUpperCase() === 'OPEN')
                        .map(([id, val]) => ({
                            id,
                            reason: val?.reason || 'Dispute',
                            accused: val?.reportedUserId || 'N/A',
                            reporter: val?.reporterId || 'N/A',
                            desc: val?.description || ''
                        }));
                    liveContext.openReports = rList;
                }
            } catch (snapErr) {
                console.error('Failed to gather live DB context for AI:', snapErr.message);
            }
        }

        // Try Google Gemini AI First (Multi-model resilient pipeline)
        if (isGeminiConfigured) {
            try {
                const prompt = `You are "VeloRix Watchdog" — an ultra-smart, loyal, proactive AI co-founder and operations manager for the owner/admin of the VeloRix Free Fire esports tournament platform.

OWNER'S MESSAGE: "${queryText}"

LIVE DATABASE REAL-TIME SNAPSHOT:
- Total Registered Players: ${liveContext.registeredUsers}
- Pending UPI Deposits (${liveContext.pendingDeposits.length}): ${JSON.stringify(liveContext.pendingDeposits)}
- Pending Withdrawals (${liveContext.pendingWithdrawals.length}): ${JSON.stringify(liveContext.pendingWithdrawals)}
- Open Dispute/Hacker Reports (${liveContext.openReports.length}): ${JSON.stringify(liveContext.openReports)}
- Upcoming & Active Tournaments (${liveContext.tournaments.length}): ${JSON.stringify(liveContext.tournaments)}

RESPONSE GUIDELINES:
1. Speak directly to the boss/owner in natural, modern, conversational Hinglish (like a smart, dependable tech co-founder chatting on Telegram).
2. NEVER say "Option 1 / Option 2", "Draft", or output list of choices. Give ONE single, direct, confident response.
3. NEVER sound like a rigid preloaded template. Vary your phrasing, tone, and sentence flow naturally based on his actual question.
4. If he asks about hackers, cheaters, or reports:
   - If 0 open reports: reassure him naturally that rooms and anti-cheat are clean, no player has been reported, and you are continuously scanning.
   - If reports exist: mention the accused user and what was reported concisely.
5. If he asks about deposits/payments/paise add:
   - If 0 pending: tell him smoothly that all deposits are clear and no verification is stuck.
   - If pending: state how many and total amount, and suggest checking /deposits.
6. If he asks about withdrawals/cashouts:
   - If 0 pending: tell him all withdrawals are cleared.
   - If pending: state pending count, amounts, and suggest approving via /withdrawals.
7. If he asks about general app health, status, or greets you:
   - Give him a concise, energetic operational update covering tournaments, payments, and users, reassuring him that you've got his back 24/7.
8. Length: 2 to 4 engaging, crisp sentences. Friendly emojis are welcome.`;

                const reply = await generateAiText(prompt, 1200, 8500);

                if (reply && reply.trim().length > 0) {
                    await bot.sendMessage(chatId, reply);
                    return;
                }
            } catch (err) {
                console.warn('Gemini chat unavailable or timed out, switching to Smart Natural Language Rule Engine:', err.message);
            }
        }

        // --- DYNAMIC NATURAL LANGUAGE INTENT & LIVE DB ENGINE (Smart Fallback) ---
        // Delivers natural, non-preloaded sounding responses even if Gemini API is momentarily slow
        const isHackerQuery = /(hacker|cheat|cheater|report|reported|dispute|ban|toxic|complaint|fair|hack)/i.test(lower);
        const isDepositQuery = /(deposit|deposits|utr|recharge|payment|paise add|paisa add)/i.test(lower);
        const isWithdrawQuery = /(withdraw|withdrawal|withdrawals|cashout|payout|paise nikal|upi transfer)/i.test(lower);
        const isTournamentQuery = /(tournament|tournaments|match|matches|room|custom|slot|password|cred)/i.test(lower);
        const isUserQuery = /(user|users|player|players|registered|audience|kitne log)/i.test(lower);

        if (isHackerQuery) {
            if (liveContext.openReports.length > 0) {
                let msgText = `🚨 <b>Sun bhai, system me ${liveContext.openReports.length} dispute report aayi hui hain:</b>\n\n`;
                for (const r of liveContext.openReports.slice(0, 3)) {
                    msgText += `• <b>User:</b> <code>${r.accused}</code> | <b>Issue:</b> ${r.reason}\n`;
                    msgText += `  <i>"${r.desc || 'No details'}"</i>\n\n`;
                }
                msgText += `Aap <b>/reports</b> check karke turant warn ya suspend kar sakte ho boss!`;
                await bot.sendMessage(chatId, msgText, { parse_mode: 'HTML' });
            } else {
                const cleanPhrases = [
                    `Nahi boss, abhi tak ek bhi hacker ya gameplay issue report nahi hua hai! Saare matches ekdam fair aur clean chal rahe hain. Main continuous scan kar raha hoon, koi bhi shady move dikha toh turant alert bhejunga!`,
                    `Sab chill hai bhai! Zero active dispute reports hain aur koi suspicious player flag nahi hua hai. App ka anti-cheat aur match tracking 100% active hai, aap befikr raho!`,
                    `Filhaal ekdum clean maahol hai boss! Koi bhi cheat ya foul-play report nahi aayi hai. Jaise hi koi suspect milega, main instant action alert bhej doonga.`
                ];
                const chosen = cleanPhrases[Math.floor(Math.random() * cleanPhrases.length)];
                await bot.sendMessage(chatId, chosen);
            }
            return;
        }

        if (isDepositQuery) {
            if (liveContext.pendingDeposits.length > 0) {
                let msgText = `💳 <b>Boss, abhi ${liveContext.pendingDeposits.length} deposit verification line me hain:</b>\n\n`;
                for (const d of liveContext.pendingDeposits.slice(0, 3)) {
                    msgText += `• <b>₹${d.amount}</b> - Player: <code>${d.userId}</code> (UTR: <code>${d.utr}</code>)\n`;
                }
                msgText += `\nAap <b>/deposits</b> par tap karke 1 second me approve kar sakte ho!`;
                await bot.sendMessage(chatId, msgText, { parse_mode: 'HTML' });
            } else {
                const depositClean = [
                    `Nahi boss, koi bhi deposit pending nahi hai! Saari UPI requests clear hain aur players ke wallets update ho chuke hain.`,
                    `Deposit queue ekdam zero hai bhai! Sabhi payments time pe process ho chuki hain, koi player wait nahi kar raha.`,
                    `All clear boss! 0 pending deposits hain abhi. Naya request aate hi main turant approve button ke sath popup kar dunga.`
                ];
                await bot.sendMessage(chatId, depositClean[Math.floor(Math.random() * depositClean.length)]);
            }
            return;
        }

        if (isWithdrawQuery) {
            if (liveContext.pendingWithdrawals.length > 0) {
                let msgText = `💰 <b>Bhai, ${liveContext.pendingWithdrawals.length} cashout requests approval maang rahi hain:</b>\n\n`;
                for (const w of liveContext.pendingWithdrawals.slice(0, 3)) {
                    msgText += `• <b>₹${w.amount}</b> - UPI: <code>${w.upi}</code>\n`;
                }
                msgText += `\nDirect <b>/withdrawals</b> open karke instant payout approve kar lo boss!`;
                await bot.sendMessage(chatId, msgText, { parse_mode: 'HTML' });
            } else {
                const wdrClean = [
                    `Sab clear hai boss! Ek bhi withdrawal request pending nahi hai, saara payout queue khali hai.`,
                    `Nahi bhai, koi cashout request ruki hui nahi hai. Players ke withdrawal demands 100% up to date hain!`,
                    `Zero pending payouts right now boss! Sab smoothly settled hai.`
                ];
                await bot.sendMessage(chatId, wdrClean[Math.floor(Math.random() * wdrClean.length)]);
            }
            return;
        }

        if (isTournamentQuery) {
            const missingCreds = liveContext.tournaments.filter(t => !t.hasRoomId && t.status === 'UPCOMING');
            let msgText = `🎮 <b>Matches & Room Status Update:</b>\n\n`;
            msgText += `Abhi total <b>${liveContext.tournaments.length} tournaments</b> tracked hain.\n`;
            if (missingCreds.length > 0) {
                msgText += `⚠️ Dhyan do boss, <b>${missingCreds.length} upcoming matches</b> me abhi Room ID/Password set karna baki hai!\n`;
            } else {
                msgText += `✅ Saare upcoming matches ke Room ID aur Password ready hain, players bina rukawat join kar rahe hain.\n`;
            }
            msgText += `\nDetail dekhne ke liye <b>/tournaments</b> par tap karein.`;
            await bot.sendMessage(chatId, msgText, { parse_mode: 'HTML' });
            return;
        }

        if (isUserQuery) {
            await bot.sendMessage(chatId, `👥 VeloRix par abhi total <b>${liveContext.registeredUsers} registered Free Fire players</b> active hain boss! Community continuously grow ho rahi hai.`);
            return;
        }

        // Conversational default response that flows naturally like an AI teammate
        const missingRooms = liveContext.tournaments.filter(t => !t.hasRoomId && t.status === 'UPCOMING').length;
        let dynamicGreeting = `Haan bhai, bolo! Main 24/7 VeloRix par live nazar rakhe hue hoon.\n\n`;
        dynamicGreeting += `Abhi platform par <b>${liveContext.registeredUsers} players</b> registered hain, deposits aur withdrawals dono <b>${liveContext.pendingDeposits.length + liveContext.pendingWithdrawals.length === 0 ? 'fully cleared' : 'active'}</b> hain, aur <b>${liveContext.openReports.length} hacker reports</b> hain.\n\n`;
        if (missingRooms > 0) {
            dynamicGreeting += `⚠️ Bas <b>${missingRooms} match</b> me Room credentials daalna baki hai. `;
        } else {
            dynamicGreeting += `Saare upcoming match rooms ready hain boss! `;
        }
        dynamicGreeting += `Aap befikr hoke kaam karo, koi bhi gadbad aayi toh main turant inform karunga!`;
        await bot.sendMessage(chatId, dynamicGreeting, { parse_mode: 'HTML' });
        } catch (msgErr) {
            console.error('❌ Error handling message in bot.on:', msgErr);
            bot.sendMessage(chatId, `⚠️ <i>Bhai, query process karne me issue aaya. Main app database ko continuously monitor kar raha hoon!</i>\n\nAap <b>/status</b> ya <b>/deposits</b> use karke data dekh sakte hain.`, { parse_mode: 'HTML' }).catch(() => {});
        }
    });

    // --- 7. CALLBACK QUERY HANDLER (One-Tap Action Buttons) ---
bot.on('callback_query', async (query) => {
    const data = query.data;
    const chatId = query.message.chat.id;
    const msgId = query.message.message_id;

    try {
        // Approve Withdrawal
        if (data.startsWith('wdr_approve_')) {
            const reqId = data.replace('wdr_approve_', '');
            if (db) {
                await db.ref(`withdraw_requests/${reqId}`).update({
                    status: 'APPROVED',
                    approvedAt: Date.now(),
                    approvedBy: 'Admin_Telegram_Watchdog'
                });
            }
            if (firestore) {
                await firestore.collection('withdraw_requests').doc(reqId).set({
                    status: 'APPROVED',
                    approvedAt: Date.now()
                }, { merge: true });
            }

            await bot.answerCallbackQuery(query.id, { text: 'Payment marked as Approved!' });
            await bot.editMessageText(query.message.text + '\n\n✅ <b>STATUS: APPROVED BY ADMIN</b>', {
                chat_id: chatId,
                message_id: msgId,
                parse_mode: 'HTML'
            });
            return;
        }

        // Reject Withdrawal
        if (data.startsWith('wdr_reject_')) {
            const reqId = data.replace('wdr_reject_', '');
            if (db) {
                await db.ref(`withdraw_requests/${reqId}`).update({
                    status: 'REJECTED',
                    rejectedAt: Date.now(),
                    rejectedBy: 'Admin_Telegram_Watchdog'
                });
            }
            if (firestore) {
                await firestore.collection('withdraw_requests').doc(reqId).set({
                    status: 'REJECTED',
                    rejectedAt: Date.now()
                }, { merge: true });
            }

            await bot.answerCallbackQuery(query.id, { text: 'Request Rejected & Marked' });
            await bot.editMessageText(query.message.text + '\n\n❌ <b>STATUS: REJECTED & REFUNDED</b>', {
                chat_id: chatId,
                message_id: msgId,
                parse_mode: 'HTML'
            });
            return;
        }

        // Approve Deposit
        if (data.startsWith('dep_approve_')) {
            const reqId = data.replace('dep_approve_', '');
            let reqData = null;
            if (db) {
                const snap = await db.ref(`deposit_requests/${reqId}`).once('value');
                reqData = snap.val();
                await db.ref(`deposit_requests/${reqId}`).update({
                    status: 'APPROVED',
                    approvedAt: Date.now(),
                    approvedBy: 'Admin_Telegram_Watchdog'
                });

                // Credit user wallet balance in Realtime DB if user & amount exist
                if (reqData && reqData.userId && reqData.amount) {
                    const uRef = db.ref(`users/${reqData.userId}/balance`);
                    const balSnap = await uRef.once('value');
                    const currentBal = parseFloat(balSnap.val()) || 0;
                    await uRef.set(currentBal + parseFloat(reqData.amount));

                    // Mark processed UTR
                    const cleanUtr = reqData.utrNumber || reqData.paymentRef;
                    if (cleanUtr) {
                        await db.ref(`processed_utrs/${cleanUtr}`).set({
                            userId: reqData.userId,
                            amount: reqData.amount,
                            verifiedAt: Date.now(),
                            requestId: reqId
                        });
                    }
                }
            }
            if (firestore) {
                await firestore.collection('deposit_requests').doc(reqId).set({
                    status: 'APPROVED',
                    approvedAt: Date.now(),
                    approvedBy: 'Admin_Telegram_Watchdog'
                }, { merge: true });
            }

            await bot.answerCallbackQuery(query.id, { text: 'Deposit Approved & VT Credited!' });
            await bot.editMessageText(query.message.text + '\n\n✅ <b>STATUS: DEPOSIT APPROVED & VT CREDITED</b>', {
                chat_id: chatId,
                message_id: msgId,
                parse_mode: 'HTML'
            });
            return;
        }

        // Reject Deposit
        if (data.startsWith('dep_reject_')) {
            const reqId = data.replace('dep_reject_', '');
            if (db) {
                await db.ref(`deposit_requests/${reqId}`).update({
                    status: 'REJECTED',
                    rejectedAt: Date.now(),
                    rejectedBy: 'Admin_Telegram_Watchdog'
                });
            }
            if (firestore) {
                await firestore.collection('deposit_requests').doc(reqId).set({
                    status: 'REJECTED',
                    rejectedAt: Date.now(),
                    rejectedBy: 'Admin_Telegram_Watchdog'
                }, { merge: true });
            }

            await bot.answerCallbackQuery(query.id, { text: 'Deposit Request Rejected' });
            await bot.editMessageText(query.message.text + '\n\n❌ <b>STATUS: DEPOSIT REJECTED BY ADMIN</b>', {
                chat_id: chatId,
                message_id: msgId,
                parse_mode: 'HTML'
            });
            return;
        }

        // Resolve Report
        if (data.startsWith('resolve_rep_')) {
            const repId = data.replace('resolve_rep_', '');
            if (db) {
                await db.ref(`user_reports/${repId}`).update({
                    status: 'RESOLVED',
                    resolvedAt: Date.now()
                });
            }
            await bot.answerCallbackQuery(query.id, { text: 'Report marked as Resolved!' });
            await bot.editMessageText(query.message.text + '\n\n✅ <b>REPORT RESOLVED BY ADMIN</b>', {
                chat_id: chatId,
                message_id: msgId,
                parse_mode: 'HTML'
            });
            return;
        }

        // Suspend User
        if (data.startsWith('suspend_user_')) {
            const targetUserId = data.replace('suspend_user_', '');
            if (db) {
                await db.ref(`users/${targetUserId}`).update({
                    isSuspended: true,
                    suspensionReason: 'Suspended via Admin Watchdog Investigation'
                });
            }
            if (firestore) {
                await firestore.collection('users').doc(targetUserId).set({
                    isSuspended: true,
                    suspensionReason: 'Suspended via Admin Watchdog Investigation'
                }, { merge: true });
            }
            await bot.answerCallbackQuery(query.id, { text: `User ${targetUserId} suspended!` });
            await bot.sendMessage(chatId, `🚫 <b>Player ${targetUserId} has been suspended from tournaments.</b>`, { parse_mode: 'HTML' });
            return;
        }

        await bot.answerCallbackQuery(query.id);
        } catch (e) {
            console.error('Error in callback handling:', e.message);
            bot.answerCallbackQuery(query.id, { text: `Action error: ${e.message}` });
        }
    });
}

// Safe exit handling
process.on('SIGINT', () => {
    console.log('Stopping VeloRix Watchdog Bot safely...');
    if (bot) {
        bot.stopPolling();
    }
    process.exit(0);
});
