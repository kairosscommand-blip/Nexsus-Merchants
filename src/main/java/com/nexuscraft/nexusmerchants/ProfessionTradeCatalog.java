package com.nexuscraft.nexusmerchants;

import org.bukkit.Material;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fixed base trade data for every real vanilla profession, three trades apiece -- what
 * {@link VillagerTradeBuilder} starts from before layering tier/reputation/dynamic pricing on
 * top. Same "game-balance data fixed in code, shared numbers in config.yml" split
 * {@code EnchantCatalog} established for NexusArcanum.
 *
 * <p>This is an original, simplified catalog inspired by real vanilla's own profession trades,
 * not a reproduction of vanilla's exact trade tables (which this project has no way to verify
 * against a real jar anyway -- see {@code EnchantCatalog}'s own disclaimer for the same standing
 * caveat). A handful of trades deliberately repeat an ingredient across professions (iron ingot
 * at Armorer/Toolsmith/Weaponsmith's own first trade, coal at Butcher/Toolsmith) because real
 * vanilla does exactly the same thing -- several professions really do share a level-1 buy.
 *
 * <p>Keyed by a plain {@code HashMap<Villager.Profession, ...>}, not an {@code EnumMap} --
 * confirmed the hard way by a real build: real Bukkit's {@code Villager.Profession} is NOT
 * actually a Java enum (it implements the deprecated {@code OldEnum} interface instead, plus
 * {@code Keyed}, precisely so datapacks/plugins can add new professions the JVM's enum machinery
 * could never support -- see {@code Villager.java}'s own stub comment for the full explanation,
 * first written up after NexusMinds hit the same fact). {@code EnumMap<K, V>} requires {@code K}
 * to literally extend {@code java.lang.Enum<K>}, so {@code new EnumMap<>(Villager.Profession.class)}
 * fails to compile against a real server jar even though it compiles fine against this project's
 * own sandbox stub (which, for simplicity, still models {@code Profession} as a plain enum) --
 * see CHANGES.md's v0.1.1 entry for this exact real-build fix. {@code NONE} and {@code NITWIT} are
 * deliberately left with an empty trade list, matching real vanilla (an unemployed villager and a
 * nitwit both genuinely have no trades at all).
 */
final class ProfessionTradeCatalog {

    private static final Map<Villager.Profession, List<TradeDefinition>> BY_PROFESSION = new HashMap<>();

    private static final List<TradeDefinition> WANDERING_TRADER_TRADES = List.of(
            new TradeDefinition(1, Material.EMERALD, 5, Material.NAME_TAG, 1, 6, 5),
            new TradeDefinition(1, Material.EMERALD, 8, Material.COMPASS, 1, 6, 8),
            new TradeDefinition(1, Material.EMERALD, 4, Material.PUMPKIN_PIE, 3, 10, 5)
    );

    static {
        put(Villager.Profession.NONE, List.of());
        put(Villager.Profession.NITWIT, List.of());

        put(Villager.Profession.ARMORER, List.of(
                new TradeDefinition(1, Material.IRON_INGOT, 8, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(3, Material.EMERALD, 7, Material.IRON_BOOTS, 1, 8, 10),
                new TradeDefinition(5, Material.EMERALD, 20, Material.DIAMOND_CHESTPLATE, 1, 3, 30)
        ));

        put(Villager.Profession.BUTCHER, List.of(
                new TradeDefinition(1, Material.COAL, 15, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 3, Material.COOKED_BEEF, 6, 12, 8),
                new TradeDefinition(4, Material.EMERALD, 8, Material.GOLDEN_CARROT, 1, 8, 20)
        ));

        put(Villager.Profession.CARTOGRAPHER, List.of(
                new TradeDefinition(1, Material.PAPER, 24, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 7, Material.COMPASS, 1, Material.FILLED_MAP, 1, 12, 10),
                new TradeDefinition(4, Material.EMERALD, 20, Material.NAME_TAG, 1, 4, 30)
        ));

        put(Villager.Profession.CLERIC, List.of(
                new TradeDefinition(1, Material.REDSTONE, 36, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.LAPIS_LAZULI, 10, Material.EMERALD, 1, 12, 8),
                new TradeDefinition(5, Material.EMERALD, 3, Material.EXPERIENCE_BOTTLE, 1, 8, 25)
        ));

        put(Villager.Profession.FARMER, List.of(
                new TradeDefinition(1, Material.WHEAT, 20, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 1, Material.BREAD, 6, 16, 5),
                new TradeDefinition(4, Material.EMERALD, 4, Material.PUMPKIN_PIE, 4, 8, 20)
        ));

        put(Villager.Profession.FISHERMAN, List.of(
                new TradeDefinition(1, Material.COD, 15, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.STRING, 20, Material.EMERALD, 1, 12, 8),
                new TradeDefinition(4, Material.EMERALD, 6, Material.FISHING_ROD, 1, 4, 20)
        ));

        put(Villager.Profession.FLETCHER, List.of(
                new TradeDefinition(1, Material.GUNPOWDER, 10, Material.EMERALD, 1, 12, 8),
                new TradeDefinition(2, Material.EMERALD, 2, Material.ARROW, 16, 12, 5),
                new TradeDefinition(4, Material.EMERALD, 7, Material.BOW, 1, 8, 15)
        ));

        put(Villager.Profession.LEATHERWORKER, List.of(
                new TradeDefinition(1, Material.LEATHER, 9, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 2, Material.LEATHER_LEGGINGS, 1, 10, 8),
                new TradeDefinition(4, Material.EMERALD, 9, Material.LEATHER_CHESTPLATE, 1, 6, 20)
        ));

        put(Villager.Profession.LIBRARIAN, List.of(
                new TradeDefinition(1, Material.PAPER, 24, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 9, Material.BOOKSHELF, 1, 12, 10),
                new TradeDefinition(4, Material.EMERALD, 5, Material.ENCHANTED_BOOK, 1, 4, 20)
        ));

        put(Villager.Profession.MASON, List.of(
                new TradeDefinition(1, Material.BRICKS, 10, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 1, Material.STONE_BRICKS, 4, 16, 5),
                new TradeDefinition(4, Material.EMERALD, 3, Material.TERRACOTTA, 4, 8, 15)
        ));

        put(Villager.Profession.SHEPHERD, List.of(
                new TradeDefinition(1, Material.WHITE_WOOL, 18, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 1, Material.LIME_WOOL, 1, 16, 5),
                new TradeDefinition(4, Material.EMERALD, 3, Material.SHEARS, 1, 6, 15)
        ));

        put(Villager.Profession.TOOLSMITH, List.of(
                new TradeDefinition(1, Material.COAL, 15, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 5, Material.IRON_PICKAXE, 1, 8, 10),
                new TradeDefinition(4, Material.EMERALD, 13, Material.DIAMOND_PICKAXE, 1, 3, 25)
        ));

        put(Villager.Profession.WEAPONSMITH, List.of(
                new TradeDefinition(1, Material.IRON_INGOT, 8, Material.EMERALD, 1, 12, 5),
                new TradeDefinition(2, Material.EMERALD, 7, Material.IRON_SWORD, 1, 8, 10),
                new TradeDefinition(4, Material.EMERALD, 8, Material.DIAMOND_SWORD, 1, 4, 20)
        ));
    }

    private ProfessionTradeCatalog() {
    }

    private static void put(Villager.Profession profession, List<TradeDefinition> trades) {
        BY_PROFESSION.put(profession, trades);
    }

    static List<TradeDefinition> tradesFor(Villager.Profession profession) {
        return BY_PROFESSION.getOrDefault(profession, List.of());
    }

    static List<TradeDefinition> wanderingTraderTrades() {
        return WANDERING_TRADER_TRADES;
    }
}
