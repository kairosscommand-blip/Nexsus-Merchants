package com.nexuscraft.nexusmerchants;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;

import java.util.Random;

/** A villager's old trades stop meaning anything the moment their profession changes -- taking a
 *  job, losing one, or a cured zombie villager settling into a fresh trade. Forces a full re-roll
 *  ({@link VillagerTagService#reroll}) rather than trying to carry anything forward from the old
 *  profession's catalog, which shares nothing with the new one's. */
final class VillagerCareerChangeListener implements Listener {

    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final Random random;

    VillagerCareerChangeListener(MerchantsConfig config, VillagerTagService tagService, Random random) {
        this.config = config;
        this.tagService = tagService;
        this.random = random;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCareerChange(VillagerCareerChangeEvent event) {
        if (!config.enabled) {
            return;
        }
        Villager villager = event.getEntity();
        if (villager != null) {
            tagService.reroll(villager, random);
        }
    }
}
