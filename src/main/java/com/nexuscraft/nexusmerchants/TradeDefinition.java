package com.nexuscraft.nexusmerchants;

import org.bukkit.Material;

/**
 * One fixed trade template out of {@link ProfessionTradeCatalog} -- the base shape a real
 * {@code MerchantRecipe} gets built from before {@link VillagerTradeBuilder} layers this
 * villager's {@link TradeTier}, this player's {@link ReputationTier}, and {@code PriceMemory}'s
 * own live supply/demand multiplier on top. {@code level} is cosmetic here (matches vanilla's
 * own 1-5 Novice-through-Master labeling for flavor in {@code /nexusmerchants inspect} output),
 * not a hard gate -- this project's simplified trading-level model unlocks every one of a
 * profession's catalog slots at real vanilla trading level 1, rather than reproducing vanilla's
 * own per-level unlock schedule exactly.
 *
 * <p>{@code secondaryMaterial}/{@code secondaryCount} of {@code null}/{@code 0} means a
 * single-ingredient trade. Direction is implicit: a "sell" trade has the player's raw good as the
 * (primary) ingredient and {@code Material.EMERALD} as the result; a "buy" trade has emerald(s) as
 * the (primary) ingredient and the villager's good as the result -- exactly mirroring how a real
 * {@code MerchantRecipe}'s ingredients/result already work, just named for readability here.
 */
record TradeDefinition(
        int level,
        Material inputMaterial,
        int inputCount,
        Material secondaryMaterial,
        int secondaryCount,
        Material outputMaterial,
        int outputCount,
        int baseMaxUses,
        int villagerXp
) {
    /** Convenience constructor for the common single-ingredient case. */
    TradeDefinition(int level, Material inputMaterial, int inputCount,
                    Material outputMaterial, int outputCount, int baseMaxUses, int villagerXp) {
        this(level, inputMaterial, inputCount, null, 0, outputMaterial, outputCount, baseMaxUses, villagerXp);
    }

    boolean hasSecondaryIngredient() {
        return secondaryMaterial != null && secondaryCount > 0;
    }
}
