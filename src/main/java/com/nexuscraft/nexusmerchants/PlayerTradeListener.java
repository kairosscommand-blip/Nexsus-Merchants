package com.nexuscraft.nexusmerchants;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/**
 * The heart of the whole plugin: fires once per completed real trade, in the real trading GUI,
 * and is where every promise this plugin makes actually gets kept -- the used trade's price
 * memory moves against the player a little (via {@link VillagerTradeBuilder#recordCompletedTrade}),
 * this specific player's familiarity with this specific villager climbs a notch (via {@link
 * ReputationStore}), and a completion flavor line plays if dialogue is on. Never cancels the
 * trade -- this only ever reacts to one that already happened.
 *
 * <p>{@code PlayerTradeEvent} turned out to live in {@code io.papermc.paper.event.player}, not
 * {@code org.bukkit.event.entity} where this project first guessed -- the exact same class of
 * mistake as NexusArcanum's own {@code PrepareItemEnchantEvent} package-location fix, caught the
 * same way: a real build. Its real accessors are {@code getVillager()} (not {@code getTarget()}),
 * {@code getTrade()}/{@code setTrade()} (not {@code getRecipe()}/{@code setRecipe()}), and
 * {@code isRewardingExp()}/{@code willIncreaseTradeUses()} (not the longer names this project
 * first guessed) -- it also extends a real {@code PlayerPurchaseEvent} this project doesn't model
 * separately, since nothing here needs to address that intermediate type on its own. See
 * CHANGES.md's v0.1.1 entry.
 */
final class PlayerTradeListener implements Listener {

    private final Plugin plugin;
    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final VillagerTradeBuilder builder;
    private final DialogueBank dialogue;

    PlayerTradeListener(Plugin plugin, MerchantsConfig config, VillagerTagService tagService,
                         VillagerTradeBuilder builder, DialogueBank dialogue) {
        this.plugin = plugin;
        this.config = config;
        this.tagService = tagService;
        this.builder = builder;
        this.dialogue = dialogue;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        if (!config.enabled) {
            return;
        }
        Player player = event.getPlayer();
        AbstractVillager merchant = event.getVillager();
        MerchantRecipe recipe = event.getTrade();
        if (player == null || merchant == null || recipe == null) {
            return;
        }

        TradeDefinition def = builder.definitionFor(merchant, recipe);
        if (def != null) {
            TradeTier tier = tagService.tierOf(merchant);
            builder.recordCompletedTrade(merchant, def, tier);
        }

        bumpReputation(merchant, player.getUniqueId());

        if (config.dialogueEnabled && config.completionDialogueEnabled) {
            player.sendMessage(dialogue.completion());
        }
    }

    private void bumpReputation(AbstractVillager merchant, UUID playerId) {
        Map<UUID, Integer> reputation = ReputationStore.load(merchant, plugin);
        ReputationStore.increment(reputation, playerId, config.reputationPerTrade);
        ReputationStore.save(merchant, plugin, reputation);
    }
}
