package com.nexuscraft.nexusmerchants;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A specific player's familiarity with a specific villager -- not a profession-wide or
 * server-wide standing, an individual relationship, stored on the villager itself (not the
 * player) since the whole point is that it doesn't transfer to a different villager of the same
 * profession. Same PDC-tagging convention as {@link PriceMemory}, one compact string blob per
 * villager: {@code "uuid1:7;uuid2:41;..."}.
 *
 * <p>This does mean a villager that's played host to hundreds of distinct players over a long
 * server's lifetime accumulates a slowly-growing blob -- an accepted, deliberate trade-off (real
 * per-player relationship data has to live somewhere, and PDC string size limits are generous
 * enough that this was judged not worth the complexity of a separate off-entity store for a
 * plugin at this scope).
 */
final class ReputationStore {

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "reputation");
    }

    static Map<UUID, Integer> load(AbstractVillager villager, Plugin plugin) {
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String blob = pdc.get(key(plugin), PersistentDataType.STRING);
        Map<UUID, Integer> data = new LinkedHashMap<>();
        if (blob == null || blob.isBlank()) {
            return data;
        }
        for (String entry : blob.split(";")) {
            int split = entry.indexOf(':');
            if (split <= 0) {
                continue;
            }
            try {
                data.put(UUID.fromString(entry.substring(0, split)), Integer.parseInt(entry.substring(split + 1)));
            } catch (IllegalArgumentException ignored) {
                // Malformed entry -- dropped, not fatal. Same discipline as PriceMemory#load.
            }
        }
        return data;
    }

    static void save(AbstractVillager villager, Plugin plugin, Map<UUID, Integer> data) {
        StringBuilder blob = new StringBuilder();
        for (Map.Entry<UUID, Integer> entry : data.entrySet()) {
            if (blob.length() > 0) {
                blob.append(';');
            }
            blob.append(entry.getKey()).append(':').append(entry.getValue());
        }
        villager.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, blob.toString());
    }

    static int get(Map<UUID, Integer> data, UUID player) {
        return data.getOrDefault(player, 0);
    }

    static void increment(Map<UUID, Integer> data, UUID player, int amount) {
        data.merge(player, amount, Integer::sum);
    }
}
