package com.nexuscraft.nexusmerchants;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * NexusMerchants -- a massive overhaul of villager (and wandering trader) trading, the fifth
 * entry in the ongoing engine-overhaul series (after NexusIntegrity's structural collapse,
 * NexusHydro's real fluid volume, NexusMinds' mob AI + speech, and NexusArcanum's enchanting
 * table). Every trade this plugin ever shows a player is a real, ordinary {@code
 * org.bukkit.inventory.MerchantRecipe} in the real vanilla trading GUI -- nothing here is a
 * custom shop screen standing in for it.
 *
 * <p>Three systems layer on top of that real trade list: a persistent, per-villager {@code
 * PriceMemory} that makes a specific trade genuinely more expensive to buy (or less profitable to
 * sell) the more it's actually used, easing back down slowly over real time instead of resetting
 * the moment a restock happens; a {@code TradeTier} quality roll that makes an individual
 * villager's own trades noticeably better or worse than another villager of the same profession,
 * independent of vanilla's own 1-5 trading level; and a per-player, per-villager {@code
 * ReputationTier} that rewards actually trading with the *same* villager repeatedly, not just the
 * same profession. On top of all three sits an intentionally narrow trade-scoped dialogue layer
 * (greetings, completion lines, restock announcements) -- see README.md's own section on why that
 * stayed narrow rather than becoming a second right-click chat system, which NexusMinds already
 * owns.
 *
 * <p>Deliberately scoped away from both siblings it could be confused with: NexusEconomy is a
 * player-to-player market and bank, nothing to do with what a villager itself offers; NexusMinds
 * owns a villager's personality, right-click dialogue, and combat/social AI, but never touches
 * trade generation or pricing at all. A standalone plugin, no shared compiled dependency with
 * either.
 */
public final class NexusMerchantsPlugin extends JavaPlugin {

    private MerchantsConfig config;
    private VillagerTagService tagService;
    private VillagerTradeBuilder builder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new MerchantsConfig(this);
        config.load(getLogger());

        this.builder = new VillagerTradeBuilder(this, config);
        this.tagService = new VillagerTagService(this, config, builder);

        Random random = new Random();
        DialogueBank dialogue = new DialogueBank(random);

        getServer().getPluginManager().registerEvents(new VillagerSpawnListener(config, tagService, random), this);
        getServer().getPluginManager().registerEvents(new VillagerCareerChangeListener(config, tagService, random), this);
        getServer().getPluginManager().registerEvents(new VillagerAcquireTradeListener(config), this);
        getServer().getPluginManager().registerEvents(
                new VillagerReplenishTradeListener(config, tagService, builder, dialogue), this);
        getServer().getPluginManager().registerEvents(
                new PlayerTradeListener(this, config, tagService, builder, dialogue), this);
        getServer().getPluginManager().registerEvents(new TradeWindowListener(this, config, builder, dialogue), this);

        RestockScheduler scheduler = new RestockScheduler(config, tagService, builder, dialogue, random);
        long periodTicks = 20L * 60L * config.restockIntervalMinutes;
        getServer().getScheduler().runTaskTimer(this, scheduler, periodTicks, periodTicks);

        var command = getCommand("nexusmerchants");
        if (command != null) {
            command.setExecutor(new MerchantsCommand(this, config, tagService, builder, this::reload));
        }

        getLogger().info("NexusMerchants enabled -- villager trading has been overhauled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexusMerchants disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getLogger());
    }
}
