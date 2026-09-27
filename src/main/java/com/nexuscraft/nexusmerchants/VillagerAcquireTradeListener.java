package com.nexuscraft.nexusmerchants;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;

/**
 * Vanilla's own trading-level-up logic occasionally hands a villager a brand-new, randomly-rolled
 * trade outside anything {@link ProfessionTradeCatalog} knows about -- this project's simplified
 * trading-level model already unlocks every one of a profession's catalog slots from the moment
 * they're tagged, so a vanilla-acquired extra trade here would just be an uncontrolled entry with
 * none of this plugin's tiering, dynamic pricing, or reputation discounts applied to it. Cancelling
 * this event keeps {@code ProfessionTradeCatalog} the single source of truth for what a managed
 * villager can ever offer, rather than letting the two systems drift apart trade by trade.
 */
final class VillagerAcquireTradeListener implements Listener {

    private final MerchantsConfig config;

    VillagerAcquireTradeListener(MerchantsConfig config) {
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAcquire(VillagerAcquireTradeEvent event) {
        if (!config.enabled) {
            return;
        }
        event.setCancelled(true);
    }
}
