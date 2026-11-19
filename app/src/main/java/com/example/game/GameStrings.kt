package com.example.game

object GameStrings {
    fun appTitle(lang: AppLanguage) = if (lang == AppLanguage.BN) "ফাইট সিমুলেটর" else "Fight Simulator"
    fun subtitle(lang: AppLanguage) = if (lang == AppLanguage.BN)
        "লবি, ক্লাশ স্কোয়াড 4v4, বড় ঘরের ম্যাপ, গ্রাউন্ড গান লুট, সার্কুলার জোন ও কাস্টম সেনসিভিটি"
    else
        "Tactical Lobby, 4v4 Clash Squad, Realistic Buildings, Ground Gun Loot, Safe Zone & Custom Sensitivity"

    fun startMatch(lang: AppLanguage) = if (lang == AppLanguage.BN) "ম্যাচ স্টার্ট" else "START MATCH"
    fun clashSquadMode(lang: AppLanguage) = if (lang == AppLanguage.BN) "ক্লাশ স্কোয়াড (4v4)" else "Clash Squad (4v4)"
    fun openWorldMode(lang: AppLanguage) = if (lang == AppLanguage.BN) "ওপেন ওয়ার্ল্ড" else "Open World"

    fun startAdventure(lang: AppLanguage, hasProgress: Boolean): String = when {
        lang == AppLanguage.BN && hasProgress -> "ম্যাচ স্টার্ট"
        lang == AppLanguage.BN -> "ম্যাচ স্টার্ট"
        hasProgress -> "Start Match"
        else -> "Start Match"
    }

    fun customizeHero(lang: AppLanguage) = if (lang == AppLanguage.BN) "ক্যারেক্টার ও গান" else "Character & Armory"
    fun settingsTitle(lang: AppLanguage) = if (lang == AppLanguage.BN) "সেটিংস ও সেনসিভিটি" else "Settings & Sensitivity"
    fun backToLobby(lang: AppLanguage) = if (lang == AppLanguage.BN) "লবিতে ফিরুন" else "Return to Lobby"

    fun missions(lang: AppLanguage) = if (lang == AppLanguage.BN) "ট্যাকটিক্যাল মিশন" else "Tactical Missions"
    fun worldMap(lang: AppLanguage) = if (lang == AppLanguage.BN) "ওপেন ওয়ার্ল্ড ম্যাপ" else "Tactical Map"
    fun tapToFastTravel(lang: AppLanguage) = if (lang == AppLanguage.BN)
        "আনলক করা হেলিপ্যাডে ট্যাপ করে দ্রুত যান"
    else
        "Tap any unlocked Extraction Pad to fast travel"

    fun biomeName(biome: BiomeType, lang: AppLanguage): String = when (biome) {
        BiomeType.CLOCKTOWER_VALLEY -> if (lang == AppLanguage.BN) "ক্লকটাওয়ার এরেনা" else "Clocktower Arena"
        BiomeType.PINE_FOREST -> if (lang == AppLanguage.BN) "পাইন জঙ্গল" else "Pine Forest"
        BiomeType.DESERT_OUTPOST -> if (lang == AppLanguage.BN) "ডেজার্ট আউটপোস্ট" else "Desert Outpost"
        BiomeType.MILITARY_PEAK -> if (lang == AppLanguage.BN) "মিলিটারি পিক" else "Military Peak"
    }

    fun fire(lang: AppLanguage) = if (lang == AppLanguage.BN) "ফায়ার" else "FIRE"
    fun jump(lang: AppLanguage) = if (lang == AppLanguage.BN) "জাম্প" else "JUMP"
    fun run(lang: AppLanguage) = if (lang == AppLanguage.BN) "রান" else "RUN"
    fun scope(lang: AppLanguage) = if (lang == AppLanguage.BN) "স্কোপ" else "SCOPE"
    fun reload(lang: AppLanguage) = if (lang == AppLanguage.BN) "রিলোড" else "RELOAD"
    fun medkit(lang: AppLanguage) = if (lang == AppLanguage.BN) "মেডকিট" else "MEDKIT"
    fun loot(lang: AppLanguage) = if (lang == AppLanguage.BN) "লুট" else "LOOT"
    fun reloading(lang: AppLanguage) = if (lang == AppLanguage.BN) "রিলোড হচ্ছে..." else "RELOADING..."

    fun barrierCountdown(sec: Int, lang: AppLanguage) = if (lang == AppLanguage.BN)
        "৪ দিক ঘেরা ব্যারিয়ার খুলতে বাকি: ${sec}s"
    else
        "SPAWN BARRIER DROPS IN ${sec}s"

    fun outsideZoneWarning(lang: AppLanguage) = if (lang == AppLanguage.BN)
        "সতর্কতা! জোনের বাইরে — দ্রুত সেফ জোনে ঢুকুন!"
    else
        "OUTSIDE SAFE ZONE! Taking Zone Damage!"

    fun generalSensitivityLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "জেনারেল এইম সেনসিভিটি (360°)" else "General Aim Sensitivity (360°)"
    fun scopeSensitivityLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "স্কোপ এইম সেনসিভিটি" else "Scope Aim Sensitivity"
    fun autoLootLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "অটো গান/অ্যামো লুট" else "Auto Pickup Ammo & Medkits"
    fun soundEffectsLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "সাউন্ড ইফেক্টস (গুলি, ফুটস্টেপ ও জোন অ্যালার্ট)" else "Sound Effects (Fire, Footsteps & Zone Alert)"
    fun soundVolumeLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "সাউন্ড ভলিউম" else "Sound Volume"
    fun sensitivityHint(lang: AppLanguage) = if (lang == AppLanguage.BN)
        "সেনসিভিটি বাড়ালে ডান দিকে আঙুল ঘোরালেই চারপাশে ৩৬০° দ্রুত এইম ঘুরবে।"
    else
        "Higher sensitivity rotates your 360° camera aim faster as you drag."

    fun customizeTitle(lang: AppLanguage) = if (lang == AppLanguage.BN) "ক্যারেক্টার ও HD গ্রাফিক্স" else "Character & HD Graphics"
    fun tabAppearance(lang: AppLanguage) = if (lang == AppLanguage.BN) "ক্যারেক্টার ও গান" else "Character"
    fun tabUpgrades(lang: AppLanguage) = if (lang == AppLanguage.BN) "আর্মর ও স্কিল" else "Upgrades"
    fun tabGraphics(lang: AppLanguage) = if (lang == AppLanguage.BN) "HD গ্রাফিক্স" else "Graphics"

    fun cloakColor(lang: AppLanguage) = if (lang == AppLanguage.BN) "কম্ব্যাট আউটফিট" else "Combat Outfit"
    fun headgear(lang: AppLanguage) = if (lang == AppLanguage.BN) "হেলমেট ও মাস্ক" else "Helmet & Mask"
    fun weaponStyle(lang: AppLanguage) = if (lang == AppLanguage.BN) "প্রাইমারি অস্ত্র" else "Primary Weapon"
    fun elementalAura(lang: AppLanguage) = if (lang == AppLanguage.BN) "ট্যাকটিক্যাল ব্যাকপ্যাক" else "Tactical Backpack"

    fun statVitality(lang: AppLanguage) = if (lang == AppLanguage.BN) "হেলথ ও বডি আর্মর" else "Vitality & Armor"
    fun statMight(lang: AppLanguage) = if (lang == AppLanguage.BN) "ওয়েপন ড্যামেজ" else "Firepower"
    fun statAgility(lang: AppLanguage) = if (lang == AppLanguage.BN) "স্প্রিন্ট স্পিড" else "Sprint Speed"
    fun statEnergy(lang: AppLanguage) = if (lang == AppLanguage.BN) "রিলোড স্পিড" else "Reload Mastery"
    fun upgrade(lang: AppLanguage) = if (lang == AppLanguage.BN) "আপগ্রেড" else "Upgrade"
    fun maxed(lang: AppLanguage) = if (lang == AppLanguage.BN) "সর্বোচ্চ" else "MAXED"

    fun graphicsQualityLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "HD গ্রাফিক্স কোয়ালিটি" else "HD Graphics Preset"
    fun qualityName(q: GraphicsQuality, lang: AppLanguage): String = when (q) {
        GraphicsQuality.STANDARD -> if (lang == AppLanguage.BN) "HD স্মুথ" else "HD Smooth"
        GraphicsQuality.HIGH -> if (lang == AppLanguage.BN) "ফুল HD (Shadows)" else "Full HD (Shadows)"
        GraphicsQuality.ULTRA -> if (lang == AppLanguage.BN) "আল্ট্রা HD + ফগ" else "Ultra HD + Fog"
    }

    fun timeModeLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "আবহাওয়া ও আলো" else "Lighting & Time"
    fun timeModeName(mode: TimeOfDayMode, lang: AppLanguage): String = when (mode) {
        TimeOfDayMode.DYNAMIC -> if (lang == AppLanguage.BN) "ডায়নামিক আকাশ" else "Dynamic Sky"
        TimeOfDayMode.DAWN -> if (lang == AppLanguage.BN) "গোল্ডেন আওয়ার" else "Golden Hour"
        TimeOfDayMode.NIGHT -> if (lang == AppLanguage.BN) "নাইট অপস" else "Night Ops"
    }

    fun languageLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "ভাষা (Language)" else "Language"
    fun resetWorld(lang: AppLanguage) = if (lang == AppLanguage.BN) "নতুন ম্যাচ শুরু করুন" else "Reset Match"

    fun trackMission(lang: AppLanguage) = if (lang == AppLanguage.BN) "ট্র্যাক" else "Track"
    fun tracked(lang: AppLanguage) = if (lang == AppLanguage.BN) "ট্র্যাকড" else "Tracked"
    fun claimReward(lang: AppLanguage) = if (lang == AppLanguage.BN) "রিওয়ার্ড নিন" else "Claim"
    fun claimed(lang: AppLanguage) = if (lang == AppLanguage.BN) "সংগৃহীত" else "Claimed"
    fun levelLabel(level: Int, lang: AppLanguage) = if (lang == AppLanguage.BN) "র‍্যাংক $level" else "LVL $level"
    fun headshotLabel(lang: AppLanguage) = if (lang == AppLanguage.BN) "হেডশট!" else "HEADSHOT!"
    fun killFeedTitle(lang: AppLanguage) = if (lang == AppLanguage.BN) "কিল ফিড" else "KILL FEED"
    fun killFeedEmpty(lang: AppLanguage) = if (lang == AppLanguage.BN) "ফাইট শুরু হলে এলিমিনেশন এখানে দেখাবে" else "Eliminations appear here"
    fun safeZoneName(lang: AppLanguage) = if (lang == AppLanguage.BN) "সেফ জোন" else "Safe Zone"
    fun dualHandTip(lang: AppLanguage) = if (lang == AppLanguage.BN)
        "লবি থেকে ম্যাচ স্টার্ট দিন • ১৫ সেকেন্ড পর ব্যারিয়ার খুলবে • 4v4 ফাইট ও গান লুট!"
    else
        "Start from Lobby • 15s Spawn Barrier • 4v4 Squad Combat, Big Houses & Gun Loot!"
}
