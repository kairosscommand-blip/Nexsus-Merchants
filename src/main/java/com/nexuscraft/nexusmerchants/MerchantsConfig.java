package com.nexuscraft.nexusmerchants;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/** Parses config.yml's tunables. Same "config drives shared numbers, the catalog/tier enums hold
 *  the fixed game-balance data" split this whole family already uses (compare NexusArcanum's own
 *  {@code ArcanumConfig}). */
final class MerchantsConfig {

    private final Plugin plugin;

    boolean enabled = true;

    // -- Dynamic pricing (PriceMemory) --
    /** How much a trade's live multiplier climbs, per use, toward {@link #maxPriceMultiplier}. */
    double surgePerUse = 0.05;
    /** How much a trade's live multiplier eases back down toward 1.0 per scheduler tick (see
     *  {@link #restockIntervalMinutes}) it goes unused. */
    double decayPerInterval = 0.03;
    double minPriceMultiplier = 0.55;
    double maxPriceMultiplier = 1.85;

    // -- TradeTier roll weights, STANDARD/SKILLED/EXPERT/MASTER/LEGENDARY -- fixed shape (5
    //    values), tunable odds. Cascades the same "roll from the bottom up against a running
    //    total" way OfferPlanner already does for EnchantTier. --
    double[] tierRollWeights = {100.0, 45.0, 18.0, 6.0, 1.5};

    // -- Reputation (ReputationStore) --
    int reputationPerTrade = 1;
    int acquaintanceThreshold = 6;
    int friendThreshold = 16;
    int trustedThreshold = 32;
    int confidantThreshold = 60;

    // -- Restock (RestockScheduler) --
    int restockIntervalMinutes = 15;
    double restockChancePerVillager = 0.4;
    int restockBonusMinUses = 2;
    int restockBonusMaxUses = 5;

    // -- Dialogue (the "expand it further" feature -- see README's own section on why this is
    //    scoped strictly to trade-GUI moments and not a general right-click chat system). --
    boolean dialogueEnabled = true;
    boolean greetingDialogueEnabled = true;
    boolean completionDialogueEnabled = true;
    boolean restockDialogueEnabled = true;

    MerchantsConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(Logger log) {
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("enabled", true);

        ConfigurationSection pricing = c.getConfigurationSection("pricing");
        if (pricing != null) {
            surgePerUse = Math.max(0, pricing.getDouble("surge-per-use", surgePerUse));
            decayPerInterval = Math.max(0, pricing.getDouble("decay-per-interval", decayPerInterval));
            minPriceMultiplier = Math.max(0.05, pricing.getDouble("min-multiplier", minPriceMultiplier));
            maxPriceMultiplier = Math.max(minPriceMultiplier, pricing.getDouble("max-multiplier", maxPriceMultiplier));
        }

        ConfigurationSection tiers = c.getConfigurationSection("tier-roll-weights");
        if (tiers != null) {
            tierRollWeights = new double[]{
                    Math.max(0, tiers.getDouble("standard", tierRollWeights[0])),
                    Math.max(0, tiers.getDouble("skilled", tierRollWeights[1])),
                    Math.max(0, tiers.getDouble("expert", tierRollWeights[2])),
                    Math.max(0, tiers.getDouble("master", tierRollWeights[3])),
                    Math.max(0, tiers.getDouble("legendary", tierRollWeights[4])),
            };
        }

        ConfigurationSection reputation = c.getConfigurationSection("reputation");
        if (reputation != null) {
            reputationPerTrade = Math.max(1, reputation.getInt("per-trade", reputationPerTrade));
            acquaintanceThreshold = Math.max(1, reputation.getInt("acquaintance-threshold", acquaintanceThreshold));
            friendThreshold = Math.max(acquaintanceThreshold, reputation.getInt("friend-threshold", friendThreshold));
            trustedThreshold = Math.max(friendThreshold, reputation.getInt("trusted-threshold", trustedThreshold));
            confidantThreshold = Math.max(trustedThreshold, reputation.getInt("confidant-threshold", confidantThreshold));
        }

        ConfigurationSection restock = c.getConfigurationSection("restock");
        if (restock != null) {
            restockIntervalMinutes = Math.max(1, restock.getInt("interval-minutes", restockIntervalMinutes));
            restockChancePerVillager = clamp01(restock.getDouble("chance", restockChancePerVillager));
            restockBonusMinUses = Math.max(0, restock.getInt("bonus-min-uses", restockBonusMinUses));
            restockBonusMaxUses = Math.max(restockBonusMinUses, restock.getInt("bonus-max-uses", restockBonusMaxUses));
        }

        ConfigurationSection dialogue = c.getConfigurationSection("dialogue");
        if (dialogue != null) {
            dialogueEnabled = dialogue.getBoolean("enabled", dialogueEnabled);
            greetingDialogueEnabled = dialogue.getBoolean("greeting", greetingDialogueEnabled);
            completionDialogueEnabled = dialogue.getBoolean("completion", completionDialogueEnabled);
            restockDialogueEnabled = dialogue.getBoolean("restock-announcement", restockDialogueEnabled);
        }

        if (!enabled) {
            log.info("NexusMerchants is disabled via config.yml.");
        }
    }

    /** Weighted tier roll, cascading from the bottom up against a running total -- the same idiom
     *  NexusArcanum's {@code OfferPlanner} already uses for {@code EnchantTier}. */
    TradeTier rollTier(java.util.Random random) {
        double total = 0;
        for (double weight : tierRollWeights) {
            total += weight;
        }
        double roll = random.nextDouble() * total;
        double running = 0;
        TradeTier[] tiers = TradeTier.values();
        for (int i = 0; i < tiers.length; i++) {
            running += tierRollWeights[i];
            if (roll < running) {
                return tiers[i];
            }
        }
        return TradeTier.STANDARD;
    }

    ReputationTier tierFor(int reputationScore) {
        if (reputationScore >= confidantThreshold) {
            return ReputationTier.CONFIDANT;
        }
        if (reputationScore >= trustedThreshold) {
            return ReputationTier.TRUSTED;
        }
        if (reputationScore >= friendThreshold) {
            return ReputationTier.FRIEND;
        }
        if (reputationScore >= acquaintanceThreshold) {
            return ReputationTier.ACQUAINTANCE;
        }
        return ReputationTier.STRANGER;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
