package com.nexuscraft.nexusmerchants;

import org.bukkit.Material;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns {@link ProfessionTradeCatalog}'s fixed data into the real {@link MerchantRecipe}s a
 * villager actually offers -- the one place {@link TradeTier} (this villager's own quality) and
 * {@link PriceMemory} (this villager's own live supply/demand state) both apply. Both of those are
 * properties of the *villager*, shared across whoever opens their trade window, which is exactly
 * how real Bukkit's trade list works (one shared {@code List<MerchantRecipe>} per merchant, not
 * one per viewer).
 *
 * <p>{@link ReputationTier} is deliberately NOT applied here -- a specific player's familiarity
 * with this villager can't be baked into a recipe list every other player sees too. That's applied
 * separately, per viewer, at the moment their trade window opens -- see this class's own {@link
 * #applyReputationDiscount}, called from {@code TradeWindowListener}, for where and, more
 * importantly, why: real
 * vanilla's own per-player-adjustable lever on a trade is {@code MerchantRecipe#setSpecialPrice},
 * which only discounts what a purchase *costs* -- it has no equivalent for boosting what a sale
 * *pays out*. So reputation's discount only ever applies to this villager's buy-direction trades
 * (the ones priced in emeralds); a sell-direction trade's payout still benefits from this
 * villager's own {@link TradeTier} and current {@link PriceMemory} state, just not from any one
 * specific player's relationship with them. Documented here plainly rather than faked, since
 * faking a per-player sell bonus would mean either mutating a supposedly-immutable {@code
 * MerchantRecipe#getResult()} across every viewer or maintaining a second, needlessly complex
 * per-player recipe list this project judged not worth it for what it would add.
 */
final class VillagerTradeBuilder {

    private final Plugin plugin;
    private final MerchantsConfig config;

    VillagerTradeBuilder(Plugin plugin, MerchantsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Rebuilds this villager's entire trade list from scratch -- used at first tagging and after
     *  a profession change, when every old trade is meaningless anyway. */
    List<MerchantRecipe> buildTrades(AbstractVillager villager, TradeTier tier) {
        List<TradeDefinition> defs = tradeDefinitionsFor(villager);
        Map<String, Double> priceMemory = PriceMemory.load(villager, plugin);

        List<MerchantRecipe> recipes = new ArrayList<>();
        for (TradeDefinition def : defs) {
            recipes.add(buildRecipe(def, tier, PriceMemory.get(priceMemory, tradeKeyFor(def))));
        }
        return recipes;
    }

    /** Rebuilds just the recipes whose live price memory actually changed since the villager's
     *  trades were last built -- called right after a completed trade so the very next open shows
     *  the fresh multiplier, without discarding {@code uses}/{@code maxUses} on every other trade
     *  the way a full {@link #buildTrades} rebuild would. */
    void reapplyPricing(AbstractVillager villager, TradeTier tier) {
        List<TradeDefinition> defs = tradeDefinitionsFor(villager);
        Map<String, Double> priceMemory = PriceMemory.load(villager, plugin);
        List<MerchantRecipe> current = new ArrayList<>(villager.getRecipes());

        for (int i = 0; i < current.size() && i < defs.size(); i++) {
            TradeDefinition def = defs.get(i);
            MerchantRecipe existing = current.get(i);
            MerchantRecipe rebuilt = buildRecipe(def, tier, PriceMemory.get(priceMemory, tradeKeyFor(def)));
            // Preserve the live trade's own uses/maxUses -- only the price side (ingredient
            // count / result count) should move here; a restock is a separate, deliberate act
            // owned by RestockScheduler, not something a routine price refresh should undo.
            rebuilt.setUses(existing.getUses());
            rebuilt.setMaxUses(existing.getMaxUses());
            current.set(i, rebuilt);
        }
        villager.setRecipes(current);
    }

    List<TradeDefinition> tradeDefinitionsFor(AbstractVillager villager) {
        if (villager instanceof Villager v) {
            return ProfessionTradeCatalog.tradesFor(v.getProfession());
        }
        return ProfessionTradeCatalog.wanderingTraderTrades();
    }

    /** Finds which fixed {@link TradeDefinition} a live, already-adjusted {@link MerchantRecipe}
     *  was built from -- matched by ingredient/result material pair, which is unique within any
     *  one profession's own catalog list (see this class's own catalog for why: a profession never
     *  repeats the same input/output material pair across its own trades, even though two
     *  *different* trades can share just one side of the pair, like Cleric's two emerald-output
     *  sell trades). Returns {@code null} if nothing matches (a trade from an older catalog
     *  version, or a hand-edited recipe) -- callers treat that as "nothing to bump", not an error.
     */
    TradeDefinition definitionFor(AbstractVillager villager, MerchantRecipe recipe) {
        if (recipe.getIngredients().isEmpty()) {
            return null;
        }
        Material inputType = recipe.getIngredients().get(0).getType();
        Material outputType = recipe.getResult() == null ? null : recipe.getResult().getType();
        for (TradeDefinition def : tradeDefinitionsFor(villager)) {
            if (def.inputMaterial() == inputType && def.outputMaterial() == outputType) {
                return def;
            }
        }
        return null;
    }

    static String tradeKeyFor(TradeDefinition def) {
        return def.level() + ":" + def.outputMaterial().name();
    }

    private MerchantRecipe buildRecipe(TradeDefinition def, TradeTier tier, double multiplier) {
        boolean isBuy = def.inputMaterial() == Material.EMERALD;
        int maxUses = def.baseMaxUses() + tier.extraMaxUses();

        ItemStack primaryIngredient;
        ItemStack result;

        if (isBuy) {
            double adjusted = def.inputCount() * multiplier * (1 - tier.discountFraction());
            int effectiveCost = Math.max(1, (int) Math.round(adjusted));
            primaryIngredient = new ItemStack(Material.EMERALD, effectiveCost);
            result = new ItemStack(def.outputMaterial(), def.outputCount());
        } else {
            primaryIngredient = new ItemStack(def.inputMaterial(), def.inputCount());
            double bonusFraction = tier.discountFraction() / Math.max(0.05, multiplier);
            int bonusEmeralds = (int) Math.floor(def.outputCount() * bonusFraction);
            result = new ItemStack(Material.EMERALD, def.outputCount() + Math.max(0, bonusEmeralds));
        }

        MerchantRecipe recipe = new MerchantRecipe(result, 0, maxUses, true, def.villagerXp(), 0.0f);
        recipe.addIngredient(primaryIngredient);
        if (def.hasSecondaryIngredient()) {
            recipe.addIngredient(new ItemStack(def.secondaryMaterial(), def.secondaryCount()));
        }
        return recipe;
    }

    /** Called by {@code PlayerTradeListener} right after a completed trade -- bumps this one
     *  trade's price memory, then immediately calls {@link #reapplyPricing} so the surge is
     *  visible the very next time this villager's window opens, not just after the next scheduled
     *  restock. */
    void recordCompletedTrade(AbstractVillager villager, TradeDefinition def, TradeTier tier) {
        Map<String, Double> priceMemory = PriceMemory.load(villager, plugin);
        PriceMemory.bump(priceMemory, tradeKeyFor(def), config.surgePerUse, config.maxPriceMultiplier);
        PriceMemory.save(villager, plugin, priceMemory);
        reapplyPricing(villager, tier);
    }

    /** Called by {@code RestockScheduler} on every tick -- eases every trade's price memory back
     *  toward baseline and refreshes the live recipes to reflect it. */
    void decayPricing(AbstractVillager villager, TradeTier tier) {
        Map<String, Double> priceMemory = PriceMemory.load(villager, plugin);
        if (priceMemory.isEmpty()) {
            return;
        }
        PriceMemory.decayAll(priceMemory, config.decayPerInterval);
        PriceMemory.save(villager, plugin, priceMemory);
        reapplyPricing(villager, tier);
    }

    /** Reputation discount, applied only to a viewer's own view of this villager's buy-direction
     *  trades, right as their trade window opens -- see this class's own top comment for why sell
     *  trades are deliberately excluded. Mutates the live {@link MerchantRecipe} objects in place
     *  via {@code setSpecialPrice}, matching the real, established plugin idiom for a per-viewer
     *  price adjustment (the same lever vanilla's own hero-of-the-village discount uses).
     */
    void applyReputationDiscount(AbstractVillager villager, UUID player) {
        Map<UUID, Integer> reputation = ReputationStore.load(villager, plugin);
        ReputationTier tier = config.tierFor(ReputationStore.get(reputation, player));
        for (MerchantRecipe recipe : villager.getRecipes()) {
            if (recipe.getIngredients().isEmpty()) {
                continue;
            }
            ItemStack firstIngredient = recipe.getIngredients().get(0);
            if (firstIngredient.getType() != Material.EMERALD) {
                recipe.setSpecialPrice(0);
                continue;
            }
            int discount = (int) Math.floor(firstIngredient.getAmount() * tier.discountFraction());
            recipe.setSpecialPrice(-Math.min(firstIngredient.getAmount() - 1, discount));
        }
    }
}
