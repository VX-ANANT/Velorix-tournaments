-- ==========================================
-- VELORIX ESPORTS - SUPABASE SCHEMA & RLS
-- ==========================================
-- This file contains the complete PostgreSQL schema, Row Level Security (RLS) policies,
-- and rate-limiting triggers for your production Free Fire tournament backend.
-- Execute this directly in your Supabase SQL Editor.
-- ==========================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. TABLES

-- Users / Player Profiles
CREATE TABLE IF NOT EXISTS public.users (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    in_game_name TEXT,
    free_fire_id TEXT UNIQUE,
    email TEXT,
    phone TEXT,
    wallet_balance NUMERIC(10, 2) DEFAULT 0.00,
    matches_played INTEGER DEFAULT 0,
    total_kills INTEGER DEFAULT 0,
    total_wins INTEGER DEFAULT 0,
    avatar_url TEXT,
    is_verified BOOLEAN DEFAULT false,
    date_of_joining TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tournaments
CREATE TABLE IF NOT EXISTS public.tournaments (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    title TEXT NOT NULL,
    game_name TEXT DEFAULT 'Free Fire',
    mode TEXT NOT NULL, -- e.g., 'Squad', 'Solo'
    map TEXT NOT NULL,  -- e.g., 'Bermuda'
    entry_fee NUMERIC(10, 2) DEFAULT 0.00,
    prize_pool NUMERIC(10, 2) NOT NULL,
    total_slots INTEGER NOT NULL,
    registered_count INTEGER DEFAULT 0,
    match_schedule TIMESTAMPTZ NOT NULL,
    status TEXT DEFAULT 'UPCOMING', -- 'UPCOMING', 'LIVE', 'COMPLETED', 'CANCELLED'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Tournament Registrations
CREATE TABLE IF NOT EXISTS public.tournament_registrations (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    tournament_id UUID REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(tournament_id, user_id)
);

-- Match Statistics (Leaderboard / Results per match)
CREATE TABLE IF NOT EXISTS public.match_stats (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    tournament_id UUID REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    match_no TEXT NOT NULL,
    position INTEGER,
    kills INTEGER DEFAULT 0,
    winnings NUMERIC(10, 2) DEFAULT 0.00,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- Wallet Transactions (History, deposits, withdrawals)
CREATE TABLE IF NOT EXISTS public.transactions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    transaction_type TEXT NOT NULL, -- 'DEPOSIT', 'WITHDRAWAL', 'ENTRY_FEE', 'WINNINGS', 'REFUND'
    status TEXT DEFAULT 'COMPLETED', -- 'PENDING', 'COMPLETED', 'FAILED'
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. ROW LEVEL SECURITY (RLS) POLICIES

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.match_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Users Policy: Can read all profiles, but only update their own.
CREATE POLICY "Users can read all profiles" ON public.users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON public.users FOR UPDATE USING (auth.uid() = id);

-- Tournaments Policy: Anyone can read, only admins (service role) can create/update.
CREATE POLICY "Anyone can read tournaments" ON public.tournaments FOR SELECT USING (true);

-- Registrations Policy: Users can see all registrations, but only register themselves.
CREATE POLICY "Anyone can read registrations" ON public.tournament_registrations FOR SELECT USING (true);
CREATE POLICY "Users can register themselves" ON public.tournament_registrations FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Match Stats Policy: Anyone can read leaderboard/stats. Only backend updates this.
CREATE POLICY "Anyone can read match stats" ON public.match_stats FOR SELECT USING (true);

-- Transactions Policy: Users can only see their own transactions.
CREATE POLICY "Users can view own transactions" ON public.transactions FOR SELECT USING (auth.uid() = user_id);

-- 4. FUNCTIONS & TRIGGERS (RATE LIMITING & AUTO-UPDATES)

-- Function: Auto-update user timestamp
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_modtime
BEFORE UPDATE ON public.users
FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- Function: Rate limit registrations (Prevent spamming join API)
-- Allows max 1 registration attempt per 5 seconds per user globally
CREATE OR REPLACE FUNCTION check_registration_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    last_reg TIMESTAMPTZ;
BEGIN
    SELECT registered_at INTO last_reg 
    FROM public.tournament_registrations 
    WHERE user_id = NEW.user_id 
    ORDER BY registered_at DESC LIMIT 1;

    IF last_reg IS NOT NULL AND (NOW() - last_reg) < interval '5 seconds' THEN
        RAISE EXCEPTION 'Rate limit exceeded: Please wait before registering again.';
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER rate_limit_registrations
BEFORE INSERT ON public.tournament_registrations
FOR EACH ROW EXECUTE PROCEDURE check_registration_rate_limit();

-- Function: Auto-increment registered count
CREATE OR REPLACE FUNCTION increment_tournament_slots()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.tournaments 
    SET registered_count = registered_count + 1
    WHERE id = NEW.tournament_id;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER on_registration_increment
AFTER INSERT ON public.tournament_registrations
FOR EACH ROW EXECUTE PROCEDURE increment_tournament_slots();
