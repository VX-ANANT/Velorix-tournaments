# 🤖 VeloRix 24/7 Autonomous AI Watchdog & Operations Bot

Yeh ek **Standalone AI Assistant & Moderator Bot** hai jo bina aapke app ko chhede, 24/7 background me aapke Firebase Realtime Database aur Firestore par nazar rakhta hai. 

Jab bhi app me koi dikkat, pending withdrawal, ya match delay aayega — **yeh turant aapke personal Telegram par aake alert bhejega aur 1-tap action buttons dega!**

---

## 🌟 Yeh Bot Kya-Kya Sambhalta Hai?

1. 🚨 **Room Credentials Reminder (Automatic)**:
   - Match shuru hone se **30 minute aur 15 minute pehle** check karega ki Room ID & Password set hua hai ya nahi.
   - Agar Room ID empty hai, toh turant alert bhejega: *"Bhai, Match #12 ka time ho gaya par Room ID update nahi hui!"*

2. 💰 **One-Tap Withdrawal Approvals**:
   - Jab bhi koi user cashout / UPI payout request lagayega, aapko Telegram par notification aayega: User ID, Amount, aur UPI ID ke sath.
   - Aap Telegram se hi **[✅ Approve]** ya **[❌ Reject]** button daba sakte ho.

3. 🛡️ **Hacker & Toxicity Report AI Analysis (Gemini)**:
   - Jab koi player kisi dusre player ko report karega, Google Gemini AI use assess karega aur aapko batayega ki allegation kitna serious hai aur kya action lena chahiye.

4. 📊 **Instant App Health Check**:
   - Kisi bhi waqt Telegram par `/status` dabayein aur live registered players, total matches, aur pending money ka snapshot paayein.

5. 💬 **Hinglish AI Ops Chat**:
   - Aap is bot se seedhe chat kar sakte ho (e.g. *"Bhai aaj koi glitch aaya kya?"*), aur Gemini AI aapko real-time platform operations ke context me jawab dega.

---

## 🚀 5-Minute Setup Guide (Step-by-Step)

### Step 1: Telegram Bot Token Leina (1 Minute)
1. Apne phone me **Telegram** open karein aur search karein: `@BotFather`.
2. `@BotFather` ko message bhejein: `/newbot`.
3. Bot ka koi bhi naam de dein (e.g., `VeloRix Watchdog`).
4. Ek unique username de dein (e.g., `velorix_my_watchdog_bot`).
5. `@BotFather` aapko ek **HTTP API Token** dega (jaise `7123456789:AAFo_xxxxxx...`). Ise copy kar lein.
6. Apne naye bot ko Telegram par search karke **"Start"** dabayein.

### Step 2: Apna Telegram Chat ID Leina (30 Seconds)
1. Telegram par search karein: `@userinfobot`.
2. Start dabayein, wo aapko aapka numeric **Id** (jaise `123456789`) bhej dega.

### Step 3: Firebase Service Account Key Download Karna (1 Minute)
1. **[Firebase Console](https://console.firebase.google.com/)** open karein.
2. Apna project select karein: `velorix-tournaments`.
3. Upar gear icon ⚙️ par click karein -> **Project settings**.
4. **Service accounts** tab me jayein.
5. **"Generate new private key"** button par click karein. Ek `.json` file download ho jayegi.
6. Is downloaded file ka naam rename karke `serviceAccountKey.json` rakh dein aur is `velorix-watchdog-bot` folder ke andar daal dein.

### Step 4: `.env` File Setup
1. Is folder me `.env.example` ko copy karke `.env` banayein:
   ```bash
   cp .env.example .env
   ```
2. `.env` file ko open karein aur apni details daal dein:
   ```ini
   TELEGRAM_BOT_TOKEN="7123456789:AAFo_xxxxxx..."
   ADMIN_TELEGRAM_CHAT_ID="123456789"
   FIREBASE_DATABASE_URL="https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app"
   GEMINI_API_KEY="YOUR_GEMINI_API_KEY" # Optional: AI insights ke liye
   ```
   *(Gemini API key bilkul free milti hai [Google AI Studio](https://aistudio.google.com/app/apikey) se)*

### Step 5: Run Karke Test Karein
```bash
npm install
npm start
```
Aapke terminal me print ho jayega:
`🤖 VeloRix AI Watchdog Bot is online and listening on Telegram!`

Ab Telegram par apne bot ko `/start` bhejein! Aapka personal AI assistant live chalne lagega!

---

## ☁️ 24/7 Free Me Kaise Chalayein (Bina Computer On Rakhe)

Agar aap chahte ho ki aapka computer band ho tab bhi yeh bot 24 ghante background me chalta rahe, toh aap ise kisi bhi free cloud hosting par deploy kar sakte ho:

### Option A: Render.com (100% Free)
1. **[Render.com](https://render.com/)** par free account banayein.
2. **"New +"** -> **"Background Worker"** (ya Web Service).
3. Apna GitHub repo connect karein aur root directory me `velorix-watchdog-bot` select karein.
4. **Environment Variables** me `.env` wali keys paste kar dein.
5. **Deploy** dabayein! Bot cloud par 24/7 chalne lagega.

### Option B: Railway.app / Koyeb
- Similar steps: Repository connect karein, Environment variables add karein, aur deploy karein.
