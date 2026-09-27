package com.nexuscraft.nexusmerchants;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

/**
 * Fires the instant a player opens a real trade window -- the one moment this plugin's two
 * per-viewer effects both apply: {@link VillagerTradeBuilder#applyReputationDiscount} adjusts
 * this specific player's view of this villager's buy-direction trades, and (if dialogue is on) a
 * reputation-flavored greeting plays. Both are read-only with respect to anyone else's view of
 * the same villager -- the shared trade list's own uses/maxUses/base pricing are never touched
 * here, only the live recipe objects' {@code specialPrice}, which real vanilla itself already
 * treats as a per-viewer-adjustable number (its own hero-of-the-village discount works the same
 * way).
 */
final class TradeWindowListener implements Listener {

    private final Plugin plugin;
    private final MerchantsConfig config;
    private final VillagerTradeBuilder builder;
    private final DialogueBank dialogue;

    TradeWindowListener(Plugin plugin, MerchantsConfig config, VillagerTradeBuilder builder, DialogueBank dialogue) {
        this.plugin = plugin;
        this.config = config;
        this.builder = builder;
        this.dialogue = dialogue;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!config.enabled) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof MerchantInventory merchantInventory)) {
            return;
        }
        Merchant merchant = merchantInventory.getMerchant();
        HumanEntity viewer = event.getPlayer();
        if (!(merchant instanceof AbstractVillager villager) || !(viewer instanceof Player player)) {
            return;
        }

        builder.applyReputationDiscount(villager, player.getUniqueId());

        if (config.dialogueEnabled && config.greetingDialogueEnabled) {
            Map<UUID, Integer> reputation = ReputationStore.load(villager, plugin);
            ReputationTier tier = config.tierFor(ReputationStore.get(reputation, player.getUniqueId()));
            player.sendMessage(dialogue.greeting(villager, tier));
        }
    }
}
