package com.nexuscraft.nexusmerchants;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;

/**
 * Vanilla's own restock trigger (a player sleeping near a bed within trading range at night) is
 * exactly the "sleep near a lectern and hope" unpredictability this whole plugin was built to
 * replace with {@code RestockScheduler}'s visible, timer-driven restocks -- but this project
 * doesn't cancel or disable vanilla's own trigger, since a player who still does the vanilla thing
 * shouldn't be denied a normal-feeling restock just because a scheduler is also running. This
 * listener just makes sure a vanilla-triggered restock gets the same treatment a scheduler-driven
 * one does: this trade's price memory eases and the moment gets the same feedback, rather than
 * only ever reacting to its own timer.
 */
final class VillagerReplenishTradeListener implements Listener {

    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final VillagerTradeBuilder builder;
    private final DialogueBank dialogue;

    VillagerReplenishTradeListener(MerchantsConfig config, VillagerTagService tagService,
                                    VillagerTradeBuilder builder, DialogueBank dialogue) {
        this.config = config;
        this.tagService = tagService;
        this.builder = builder;
        this.dialogue = dialogue;
    }

    @EventHandler(ignoreCancelled = true)
    public void onReplenish(VillagerReplenishTradeEvent event) {
        if (!config.enabled) {
            return;
        }
        Villager villager = event.getEntity();
        if (villager == null) {
            return;
        }
        builder.decayPricing(villager, tagService.tierOf(villager));

        if (config.dialogueEnabled && config.restockDialogueEnabled) {
            RestockScheduler.announceRestock(villager, dialogue);
        }
    }
}
