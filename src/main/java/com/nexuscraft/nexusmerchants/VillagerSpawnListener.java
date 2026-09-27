package com.nexuscraft.nexusmerchants;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Random;

/** Tags every freshly-spawned villager or wandering trader exactly once -- see {@code
 *  VillagerTagService#tagIfNeeded}'s own comment for why an already-tagged entity (this same
 *  event somehow re-firing, or a plugin reload) is left with its existing tier/trades intact. */
final class VillagerSpawnListener implements Listener {

    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final Random random;

    VillagerSpawnListener(MerchantsConfig config, VillagerTagService tagService, Random random) {
        this.config = config;
        this.tagService = tagService;
        this.random = random;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!config.enabled) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity instanceof AbstractVillager merchant) {
            tagService.tagIfNeeded(merchant, random);
        }
    }
}
