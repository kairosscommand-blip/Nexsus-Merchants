package com.nexuscraft.nexusmerchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The visible, predictable restock this whole plugin was built to replace "sleep near a lectern
 * and hope" with: on a fixed real-world interval ({@code restock.interval-minutes} in config.yml),
 * every currently-tagged, currently-loaded villager or wandering trader eases its price memory
 * back toward baseline, and has a fixed, known chance of one of its trades getting a real,
 * visible restock -- a sound, a particle, and (if dialogue is enabled) an announcement, all at the
 * villager's own location, rather than a silent number change a player has no way to notice
 * without opening the trade window and checking. A player who wants to know exactly when their
 * favorite trader might restock can just watch the clock; nothing here is hidden behind vanilla's
 * own sleep-trigger unpredictability.
 *
 * <p>Iterates {@code VillagerTagService}'s own runtime registry rather than {@code
 * World#getEntities()} -- see that class's own comment for why (this sandbox's stub always
 * returns an empty collection from that method, a limitation of the stub, not of a real server;
 * the registry approach works identically either way and doesn't depend on it).
 */
final class RestockScheduler implements Runnable {

    private static final double ANNOUNCE_RADIUS_BLOCKS = 16.0;

    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final VillagerTradeBuilder builder;
    private final DialogueBank dialogue;
    private final Random random;

    RestockScheduler(MerchantsConfig config, VillagerTagService tagService, VillagerTradeBuilder builder,
                      DialogueBank dialogue, Random random) {
        this.config = config;
        this.tagService = tagService;
        this.builder = builder;
        this.dialogue = dialogue;
        this.random = random;
    }

    @Override
    public void run() {
        if (!config.enabled) {
            return;
        }
        for (AbstractVillager villager : tagService.snapshotManaged()) {
            TradeTier tier = tagService.tierOf(villager);
            builder.decayPricing(villager, tier);

            if (random.nextDouble() < config.restockChancePerVillager) {
                restockOneTrade(villager);
                if (config.dialogueEnabled && config.restockDialogueEnabled) {
                    announceRestock(villager, dialogue);
                }
            }
        }
    }

    private void restockOneTrade(AbstractVillager villager) {
        List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
        if (recipes.isEmpty()) {
            return;
        }
        int index = random.nextInt(recipes.size());
        MerchantRecipe recipe = recipes.get(index);
        int bonus = config.restockBonusMinUses
                + (config.restockBonusMaxUses > config.restockBonusMinUses
                        ? random.nextInt(config.restockBonusMaxUses - config.restockBonusMinUses + 1)
                        : 0);
        recipe.setUses(Math.max(0, recipe.getUses() - bonus));
        villager.setRecipes(recipes);
    }

    /** Shared with {@code VillagerReplenishTradeListener} so a vanilla-triggered restock announces
     *  exactly the same way a scheduler-driven one does. */
    static void announceRestock(AbstractVillager villager, DialogueBank dialogue) {
        Location location = villager.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.4f);
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 8, 0.4, 0.4, 0.4);

        String message = dialogue.restockAnnouncement(villager);
        double radiusSquared = ANNOUNCE_RADIUS_BLOCKS * ANNOUNCE_RADIUS_BLOCKS;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLocation = player.getLocation();
            if (playerLocation == null || playerLocation.getWorld() == null) {
                continue;
            }
            if (!playerLocation.getWorld().getName().equals(location.getWorld().getName())) {
                continue;
            }
            if (playerLocation.distanceSquared(location) <= radiusSquared) {
                player.sendMessage(message);
            }
        }
    }
}
