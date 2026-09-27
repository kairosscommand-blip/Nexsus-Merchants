package com.nexuscraft.nexusmerchants;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * PDC-tags a villager or wandering trader as one this plugin manages, keeping a runtime registry
 * of every tagged, currently-loaded one so {@code RestockScheduler} has something to iterate
 * without depending on this stub tree's always-empty {@code World#getEntities()} (a limitation of
 * this sandbox, not of a real server -- see this project's README for that standing note). The PDC
 * tag itself is what actually matters for correctness across a restart: a villager that already
 * has a stored {@link TradeTier} keeps it (and its trades are left alone) rather than being
 * re-rolled, so a server owner's already-Legendary farmer doesn't quietly reset just because the
 * server restarted and this registry started out empty again.
 */
final class VillagerTagService {

    private final Plugin plugin;
    private final MerchantsConfig config;
    private final VillagerTradeBuilder builder;
    private final Map<UUID, AbstractVillager> managed = new LinkedHashMap<>();

    VillagerTagService(Plugin plugin, MerchantsConfig config, VillagerTradeBuilder builder) {
        this.plugin = plugin;
        this.config = config;
        this.builder = builder;
    }

    private NamespacedKey taggedKey() {
        return new NamespacedKey(plugin, "tagged");
    }

    private NamespacedKey tierKey() {
        return new NamespacedKey(plugin, "tier");
    }

    boolean isTagged(AbstractVillager villager) {
        return villager.getPersistentDataContainer().has(taggedKey(), PersistentDataType.BOOLEAN);
    }

    TradeTier tierOf(AbstractVillager villager) {
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String stored = pdc.get(tierKey(), PersistentDataType.STRING);
        if (stored == null) {
            return TradeTier.STANDARD;
        }
        try {
            return TradeTier.valueOf(stored);
        } catch (IllegalArgumentException e) {
            // An older/newer version's differently-named tier -- fail open to STANDARD rather
            // than crash, same discipline as PriceMemory/ReputationStore's own malformed-entry
            // handling.
            return TradeTier.STANDARD;
        }
    }

    private void storeTier(AbstractVillager villager, TradeTier tier) {
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(taggedKey(), PersistentDataType.BOOLEAN, true);
        pdc.set(tierKey(), PersistentDataType.STRING, tier.name());
    }

    /** First-time tagging -- a freshly spawned villager or wandering trader. Rolls a tier exactly
     *  once and builds its initial trade list; if this entity is somehow already tagged (a plugin
     *  reload re-firing a spawn-adjacent event, for instance) this only re-registers it and leaves
     *  its existing tier/trades untouched. */
    TradeTier tagIfNeeded(AbstractVillager villager, Random random) {
        register(villager);
        if (isTagged(villager)) {
            return tierOf(villager);
        }
        TradeTier tier = config.rollTier(random);
        storeTier(villager, tier);
        villager.setRecipes(builder.buildTrades(villager, tier));
        return tier;
    }

    /** Forces a fresh tier roll and a full trade rebuild -- used only when the old trades are
     *  genuinely meaningless, i.e. a villager's profession just changed. */
    TradeTier reroll(Villager villager, Random random) {
        register(villager);
        TradeTier tier = config.rollTier(random);
        storeTier(villager, tier);
        villager.setRecipes(builder.buildTrades(villager, tier));
        return tier;
    }

    void register(AbstractVillager villager) {
        managed.put(villager.getUniqueId(), villager);
    }

    void unregister(UUID id) {
        managed.remove(id);
    }

    /** A snapshot, not a live view -- safe for {@code RestockScheduler} to iterate and mutate the
     *  registry (via {@link #unregister}) mid-iteration for anything it finds invalid. */
    java.util.List<AbstractVillager> snapshotManaged() {
        managed.values().removeIf(v -> !v.isValid());
        return new java.util.ArrayList<>(managed.values());
    }
}
