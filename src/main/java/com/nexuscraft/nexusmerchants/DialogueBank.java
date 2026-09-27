package com.nexuscraft.nexusmerchants;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * The "expand it further" piece of this plugin -- trade-scoped flavor lines in chat, floated as an
 * open example by the person who asked for this plugin and deliberately built narrow: every line
 * this class ever produces fires only around an actual real trade-GUI moment (opening a trade
 * window, completing a trade, a villager finishing a restock), never from a generic right-click.
 * That boundary is deliberate, not incidental -- NexusMinds already owns a villager's right-click
 * profession dialogue (its own {@code DialogueBank}, a much bigger system with per-mob
 * personality), and duplicating that here would step on an already-shipped feature rather than add
 * something new. This one only ever speaks in the moment money is actually changing hands, which
 * NexusMinds' dialogue never does. All of it can be turned off independently in config.yml
 * ({@code dialogue.greeting}/{@code completion}/{@code restock-announcement}) or as a whole
 * ({@code dialogue.enabled}), in case it turns out to feel redundant once tried in practice.
 */
final class DialogueBank {

    private static final Map<ReputationTier, List<String>> GREETINGS = Map.of(
            ReputationTier.STRANGER, List.of(
                    "eyes you over before saying anything.",
                    "sizes you up -- a new face."
            ),
            ReputationTier.ACQUAINTANCE, List.of(
                    "gives you a small nod of recognition.",
                    "remembers your last visit."
            ),
            ReputationTier.FRIEND, List.of(
                    "greets you warmly.",
                    "waves you over -- good to see a familiar customer."
            ),
            ReputationTier.TRUSTED, List.of(
                    "brightens up -- one of their better customers.",
                    "already has a good deal in mind for you."
            ),
            ReputationTier.CONFIDANT, List.of(
                    "grins -- an old, trusted friend.",
                    "sets aside their best offer just for you."
            )
    );

    private static final List<String> COMPLETIONS = List.of(
            "Pleasure doing business.",
            "A fine trade, that.",
            "Come back anytime.",
            "Hmph -- fair enough."
    );

    private static final List<String> RESTOCK_ANNOUNCEMENTS = List.of(
            "finishes restocking their wares.",
            "sets out a fresh batch of goods.",
            "has replenished their stock."
    );

    private final Random random;

    DialogueBank(Random random) {
        this.random = random;
    }

    String greeting(AbstractVillager merchant, ReputationTier reputationTier) {
        List<String> lines = GREETINGS.get(reputationTier);
        String line = lines.get(random.nextInt(lines.size()));
        return "§7The " + labelFor(merchant) + " §7" + line;
    }

    String completion() {
        return "§7" + COMPLETIONS.get(random.nextInt(COMPLETIONS.size()));
    }

    String restockAnnouncement(AbstractVillager merchant) {
        String line = RESTOCK_ANNOUNCEMENTS.get(random.nextInt(RESTOCK_ANNOUNCEMENTS.size()));
        return "§7The " + labelFor(merchant) + " §7" + line;
    }

    private static String labelFor(AbstractVillager merchant) {
        if (merchant instanceof WanderingTrader) {
            return "§fWandering Trader";
        }
        if (merchant instanceof Villager villager) {
            // Profession#name() is real, deprecated OldEnum API -- getKey() is the non-deprecated
            // way to read it on a real server (see Villager.java's own stub comment, and
            // MerchantsCommand#kindOf for the same fix).
            String name = villager.getProfession().getKey().getKey().toLowerCase(Locale.ROOT).replace('_', ' ');
            return "§f" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return "§fmerchant";
    }
}
