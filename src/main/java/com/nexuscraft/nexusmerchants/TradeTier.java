package com.nexuscraft.nexusmerchants;

/**
 * An individual villager's own quality roll -- original design, layered on top of (not a
 * replacement for) vanilla's own real 1-5 {@code Villager#getVillagerLevel()} trading level.
 * Vanilla's level gates which trade *slots* are unlocked; this tier decides how good a *specific*
 * villager turns out to be at their job once those slots are unlocked -- a freshly-employed
 * Standard farmer and a freshly-employed Legendary farmer both start at trading level 1 with the
 * same base trades, but the Legendary one already prices and stocks them noticeably better. Rolled
 * once per villager by {@code TradeTierRoller} (weighted, config-driven), re-rolled on profession
 * change, and stored in the villager's own PDC so it survives a server restart.
 *
 * <p>Same "shared numbers in config.yml, this fixed enum shape in code" split NexusArcanum's own
 * {@code EnchantTier} established -- what changes per-tier (discount, stock bonus) is fixed here;
 * how often each tier actually gets rolled is a config knob, not fixed in this enum.
 */
enum TradeTier {
    STANDARD(0.0, 0),
    SKILLED(0.08, 2),
    EXPERT(0.16, 4),
    MASTER(0.27, 7),
    LEGENDARY(0.4, 12),
    ;

    private final double discountFraction;
    private final int extraMaxUses;

    TradeTier(double discountFraction, int extraMaxUses) {
        this.discountFraction = discountFraction;
        this.extraMaxUses = extraMaxUses;
    }

    /** Fraction shaved off a trade's emerald side before reputation's own discount is applied on
     *  top -- the two stack multiplicatively, not additively, so a Legendary villager a player is
     *  also Confidant with is a genuinely rare, dramatically good deal, not just a modest one. */
    double discountFraction() {
        return discountFraction;
    }

    /** Flat bonus added to every trade's {@code maxUses} this villager offers -- a better villager
     *  doesn't just charge less, they can actually sustain more business before needing to
     *  restock. */
    int extraMaxUses() {
        return extraMaxUses;
    }

    String displayName() {
        return switch (this) {
            case STANDARD -> "Standard";
            case SKILLED -> "Skilled";
            case EXPERT -> "Expert";
            case MASTER -> "Master";
            case LEGENDARY -> "Legendary";
        };
    }

    String colored() {
        String color = switch (this) {
            case STANDARD -> "§7";
            case SKILLED -> "§a";
            case EXPERT -> "§b";
            case MASTER -> "§d";
            case LEGENDARY -> "§6";
        };
        return color + displayName();
    }
}
