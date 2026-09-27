package com.nexuscraft.nexusmerchants;

/**
 * A single player's own familiarity with a single villager -- not a profession-wide or
 * server-wide reputation, an individual relationship with an individual entity, keyed by that
 * villager's own persistent UUID. Trading with the same villager repeatedly climbs this; trading
 * with a different villager of the same profession starts back at {@link #STRANGER}. Thresholds
 * (in {@code ReputationStore} points, gained per completed trade) live in config.yml, same "fixed
 * shape in code, tunable numbers in config" split as {@link TradeTier}.
 */
enum ReputationTier {
    STRANGER(0.0),
    ACQUAINTANCE(0.05),
    FRIEND(0.10),
    TRUSTED(0.18),
    CONFIDANT(0.28),
    ;

    private final double discountFraction;

    ReputationTier(double discountFraction) {
        this.discountFraction = discountFraction;
    }

    /** Stacks multiplicatively with {@link TradeTier#discountFraction()} -- see that method's own
     *  comment for why the combination, not either alone, is where the real payoff shows up. */
    double discountFraction() {
        return discountFraction;
    }

    String displayName() {
        return switch (this) {
            case STRANGER -> "Stranger";
            case ACQUAINTANCE -> "Acquaintance";
            case FRIEND -> "Friend";
            case TRUSTED -> "Trusted";
            case CONFIDANT -> "Confidant";
        };
    }

    String colored() {
        String color = switch (this) {
            case STRANGER -> "§7";
            case ACQUAINTANCE -> "§f";
            case FRIEND -> "§a";
            case TRUSTED -> "§b";
            case CONFIDANT -> "§d";
        };
        return color + displayName();
    }
}
