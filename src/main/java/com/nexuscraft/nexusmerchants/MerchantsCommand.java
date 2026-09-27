package com.nexuscraft.nexusmerchants;

import org.bukkit.Location;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** {@code /nexusmerchants reload|inspect} -- reload is admin-only (double-checked here, not just
 *  plugin.yml's default); inspect shows the real economic state of whatever villager or wandering
 *  trader the player is looking straight at: profession, quality tier, the looking player's own
 *  reputation with that specific entity, and every trade's current live price-memory multiplier. */
final class MerchantsCommand implements CommandExecutor {

    private static final double INSPECT_RANGE_BLOCKS = 8.0;

    private final Plugin plugin;
    private final MerchantsConfig config;
    private final VillagerTagService tagService;
    private final VillagerTradeBuilder builder;
    private final Runnable reload;

    MerchantsCommand(Plugin plugin, MerchantsConfig config, VillagerTagService tagService,
                      VillagerTradeBuilder builder, Runnable reload) {
        this.plugin = plugin;
        this.config = config;
        this.tagService = tagService;
        this.builder = builder;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "inspect" -> handleInspect(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§7Usage: /nexusmerchants <reload|inspect>");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexusmerchants.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return;
        }
        reload.run();
        sender.sendMessage("§aNexusMerchants reloaded.");
    }

    private void handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly a player can inspect a merchant.");
            return;
        }
        AbstractVillager merchant = resolveLookedAtMerchant(player);
        if (merchant == null) {
            player.sendMessage("§7Look straight at a villager or wandering trader first.");
            return;
        }

        TradeTier tier = tagService.tierOf(merchant);
        Map<UUID, Integer> reputation = ReputationStore.load(merchant, plugin);
        int reputationScore = ReputationStore.get(reputation, player.getUniqueId());
        ReputationTier reputationTier = config.tierFor(reputationScore);

        player.sendMessage("§6=== Merchant Inspection ===");
        player.sendMessage("§7Kind: §f" + kindOf(merchant));
        player.sendMessage("§7Quality tier: §f" + tier.colored());
        player.sendMessage("§7Your reputation: §f" + reputationTier.colored()
                + " §7(" + reputationScore + " points)");

        List<MerchantRecipe> recipes = merchant.getRecipes();
        if (recipes.isEmpty()) {
            player.sendMessage("§7No trades.");
            return;
        }
        player.sendMessage("§7Trades:");
        for (MerchantRecipe recipe : recipes) {
            TradeDefinition def = builder.definitionFor(merchant, recipe);
            double multiplier = def == null ? 1.0
                    : PriceMemory.get(PriceMemory.load(merchant, plugin), VillagerTradeBuilder.tradeKeyFor(def));
            player.sendMessage("§8 - §f" + describeRecipe(recipe)
                    + " §7(uses " + recipe.getUses() + "/" + recipe.getMaxUses()
                    + ", multiplier §f" + String.format(Locale.ROOT, "%.2f", multiplier) + "§7)");
        }
    }

    private String kindOf(AbstractVillager merchant) {
        if (merchant instanceof org.bukkit.entity.Villager villager) {
            return villager.getProfession().name() + " (level " + villager.getVillagerLevel() + ")";
        }
        return "Wandering Trader";
    }

    private String describeRecipe(MerchantRecipe recipe) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            if (i > 0) {
                text.append(" + ");
            }
            var ingredient = recipe.getIngredients().get(i);
            text.append(ingredient.getAmount()).append(' ').append(ingredient.getType());
        }
        text.append(" -> ").append(recipe.getResult().getAmount()).append(' ').append(recipe.getResult().getType());
        return text.toString();
    }

    private AbstractVillager resolveLookedAtMerchant(Player player) {
        Location eye = player.getEyeLocation();
        if (eye == null || eye.getWorld() == null) {
            return null;
        }
        RayTraceResult result = eye.getWorld().rayTraceEntities(eye, eye.getDirection(), INSPECT_RANGE_BLOCKS);
        Entity hit = result == null ? null : result.getHitEntity();
        return hit instanceof AbstractVillager merchant ? merchant : null;
    }
}
