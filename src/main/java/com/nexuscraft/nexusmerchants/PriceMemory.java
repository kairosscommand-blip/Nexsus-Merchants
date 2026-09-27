package com.nexuscraft.nexusmerchants;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A real, persistent, slowly-moving supply-and-demand multiplier per specific trade on a specific
 * villager -- the actual "prices move over time based on use" promise this plugin was built
 * around, and something genuinely separate from vanilla's own {@code MerchantRecipe#getDemand()}.
 * Vanilla's demand number resets to near-nothing the moment a trade restocks and only ever nudges
 * a single trade's cost up mid-session; this multiplier survives restocks, survives a server
 * restart (stored in the villager's own real PDC, exactly like {@code ChiseledBookshelf}'s
 * inventory or NexusMinds' mob personality tags), and only eases back down slowly, one
 * {@code RestockScheduler} tick at a time -- so a trade a whole server has been leaning on stays
 * expensive for a while, and a genuinely idle trade drifts back to baseline instead of
 * front-loading its whole cost onto the very next restock.
 *
 * <p>Stored as one compact string blob (one PDC entry per villager, not one per trade) --
 * {@code "key1:1.230;key2:0.910;..."} -- same "a single delimited string is simplest and plenty"
 * choice this whole plugin family already makes for anything PDC-backed that isn't a single
 * primitive. A key with no entry is assumed to be at baseline (1.0) and never actually written
 * until its first bump, keeping a freshly-tagged villager's blob empty.
 */
final class PriceMemory {

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "price-memory");
    }

    static Map<String, Double> load(AbstractVillager villager, Plugin plugin) {
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String blob = pdc.get(key(plugin), PersistentDataType.STRING);
        Map<String, Double> data = new LinkedHashMap<>();
        if (blob == null || blob.isBlank()) {
            return data;
        }
        for (String entry : blob.split(";")) {
            int split = entry.indexOf(':');
            if (split <= 0) {
                continue;
            }
            try {
                data.put(entry.substring(0, split), Double.parseDouble(entry.substring(split + 1)));
            } catch (NumberFormatException ignored) {
                // A malformed entry (hand-edited world data, a future version's differently-shaped
                // blob) is simply dropped rather than crashing the whole load -- same "never crash,
                // fail open" discipline this plugin family uses everywhere.
            }
        }
        return data;
    }

    static void save(AbstractVillager villager, Plugin plugin, Map<String, Double> data) {
        StringBuilder blob = new StringBuilder();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            if (blob.length() > 0) {
                blob.append(';');
            }
            blob.append(entry.getKey()).append(':').append(String.format(java.util.Locale.ROOT, "%.3f", entry.getValue()));
        }
        villager.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, blob.toString());
    }

    static double get(Map<String, Double> data, String tradeKey) {
        return data.getOrDefault(tradeKey, 1.0);
    }

    /** Called once per completed trade -- always moves the multiplier in the direction that makes
     *  this specific trade a worse deal next time (higher cost to buy, smaller bonus payout to
     *  sell), same as any real market getting used hard in one direction. */
    static void bump(Map<String, Double> data, String tradeKey, double surgePerUse, double max) {
        double current = get(data, tradeKey);
        double next = Math.min(max, current + surgePerUse);
        data.put(tradeKey, next);
    }

    /** Called by {@code RestockScheduler} on every tick for every key this villager has drifted
     *  away from baseline -- eases each one back toward 1.0 by a fixed step, removing any entry
     *  that's close enough to baseline to just drop (keeps a long-idle villager's blob shrinking
     *  back to empty instead of accumulating forever). */
    static void decayAll(Map<String, Double> data, double decayPerInterval) {
        data.replaceAll((tradeKey, multiplier) -> {
            if (multiplier > 1.0) {
                return Math.max(1.0, multiplier - decayPerInterval);
            }
            if (multiplier < 1.0) {
                return Math.min(1.0, multiplier + decayPerInterval);
            }
            return multiplier;
        });
        data.values().removeIf(multiplier -> Math.abs(multiplier - 1.0) < 0.001);
    }
}
