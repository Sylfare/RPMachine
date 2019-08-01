package net.zyuiop.rpmachine.shops.types;

import net.zyuiop.rpmachine.RPMachine;
import net.zyuiop.rpmachine.database.PlayerData;
import net.zyuiop.rpmachine.economy.EconomyManager;
import net.zyuiop.rpmachine.economy.Messages;
import net.zyuiop.rpmachine.economy.jobs.JobRestrictions;
import net.zyuiop.rpmachine.entities.RoleToken;
import net.zyuiop.rpmachine.json.JsonExclude;
import net.zyuiop.rpmachine.permissions.ShopPermissions;
import net.zyuiop.rpmachine.shops.ShopBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Map;
import java.util.Optional;

public class EnchantingSign extends AbstractShopSign {
    private String enchantment;
    private int level;
    private int available;

    @JsonExclude
    private Enchantment bukkitEnchantment;

    public EnchantingSign(Location location) {
        super(location);
    }

    public int getAvailable() {
        return available;
    }

    public void display() {
        Block block = location.getLocation().getBlock();
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if (enchantment == null) {
                sign.setLine(0, ChatColor.AQUA + "Enchantements");
                sign.setLine(1, ChatColor.RED + "Non configuré");
                sign.setLine(3, ChatColor.RED + "<CLIC DROIT>");
            } else {
                checkEnchantment();

                sign.setLine(0, owner().shortDisplayable());
                sign.setLine(1, ChatColor.DARK_AQUA + "enchante niv " + level);
                sign.setLine(2, ChatColor.AQUA + enchantment.replaceAll("_", " "));
                sign.setLine(3, ChatColor.BLUE + "Prix : " + price);
            }

            Bukkit.getScheduler().runTask(RPMachine.getInstance(), (Runnable) sign::update);
        } else {
            Bukkit.getLogger().info("Error : sign is not a sign, at " + location.toString());
        }
    }

    private void checkEnchantment() {
        if (enchantment != null && bukkitEnchantment == null) {
            bukkitEnchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantment));
        }
    }

    @Override
    public void breakSign() {
        super.breakSign();

        if (enchantment != null) {
            checkEnchantment();

            if (bukkitEnchantment != null) for (; available > 0; available--) {
                ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stack.getItemMeta();
                meta.addStoredEnchant(bukkitEnchantment, level, true);
                stack.setItemMeta(meta);
                location.getLocation().getWorld().dropItemNaturally(location.getLocation(), stack);
            }
        }
    }

    void clickPrivileged(Player player, RoleToken tt, PlayerInteractEvent event) {
        if (enchantment == null) {
            if (event.getItem() == null)
                return;

            Map<Enchantment, Integer> enchants;
            if (event.getItem().getType() == Material.ENCHANTED_BOOK) {
                enchants = ((EnchantmentStorageMeta) event.getItem().getItemMeta()).getStoredEnchants();
            } else {
                enchants = event.getItem().getEnchantments();
            }

            if (enchants.size() > 1) {
                player.sendMessage(ChatColor.RED + "Pour initialiser le shop, merci d'utiliser un item ne portant qu'un seul enchantement.");
                return;
            } else if (enchants.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Pour initialiser le shop, merci d'utiliser un item enchanté.");
                return;
            }

            Enchantment enchantment = enchants.keySet().iterator().next();
            Integer level = enchants.get(enchantment);

            this.bukkitEnchantment = enchantment;
            this.level = level;
            this.enchantment = bukkitEnchantment.getKey().getKey();

            player.sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.GREEN + "Votre shop est maintenant totalement opérationnel.");

            display();
        } else {
            checkEnchantment();

            if (event.getItem() != null) {
                boolean isBook = event.getItem().getType() == Material.ENCHANTED_BOOK;
                Map<Enchantment, Integer> enchants = isBook ? ((EnchantmentStorageMeta) event.getItem().getItemMeta()).getStoredEnchants() : event.getItem().getEnchantments();

                if (enchants.containsKey(bukkitEnchantment)) {
                    if (enchants.get(bukkitEnchantment) == level) {
                        if (isBook) {
                            if (enchants.size() == 1) {
                                event.getItem().setType(Material.BOOK);
                            } else {
                                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) event.getItem().getItemMeta();
                                meta.removeStoredEnchant(bukkitEnchantment);
                                event.getItem().setItemMeta(meta);
                            }
                        } else {
                            event.getItem().removeEnchantment(bukkitEnchantment);
                        }
                        available++;
                        event.getPlayer().sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.GREEN + "Vous venez d'ajouter " + ChatColor.AQUA + 1 + ChatColor.GREEN + " enchantement à votre shop.");
                    } else {
                        event.getPlayer().sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.RED + "L'enchantement " + bukkitEnchantment.getKey().getKey() + " n'est pas du bon niveau.");
                    }
                } else {
                    event.getPlayer().sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.RED + "L'enchantement " + bukkitEnchantment.getKey().getKey() + " n'a pas été trouvé sur l'item.");
                }
            }

            event.getPlayer().sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.YELLOW + "Il y a actuellement " + ChatColor.GOLD + this.available + ChatColor.YELLOW + " items dans la réserve de ce shop.");
        }
    }

    private Optional<ItemStack> getEligibleItem(ItemStack stack) {
        return Optional.ofNullable(stack)
                .filter(s -> this.bukkitEnchantment.canEnchantItem(s))
                .filter(s ->
                        !s.getEnchantments().keySet().stream().anyMatch(e -> bukkitEnchantment.conflictsWith(e)) // avoid conflicts
                );
    }

    void clickUser(Player player, PlayerInteractEvent event) {
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Le créateur de ce shop n'a pas terminé sa configuration.");
        }

        checkEnchantment();
        if (bukkitEnchantment == null)
            player.sendMessage(ChatColor.RED + "Une erreur s'est produite avec ce shop.");


        // Used item
        Optional<ItemStack> item = getEligibleItem(event.getItem());
        if (!item.isPresent()) {
            player.sendMessage(ChatColor.RED + "Aucun objet dans la main (ou objet non compatible)");
            return;
        }

        RoleToken token = RPMachine.getPlayerRoleToken(player);


        if (!token.checkDelegatedPermission(ShopPermissions.BUY_ENCHANTS))
            return;

        if (available <= 0) {
            player.sendMessage(ChatColor.RED + "Il n'y a plus d'enchantements à vendre.");
            return;
        }

        if (token.getLegalEntity().transfer(price, owner())) {
            available--;
            ItemStack stack = item.get();
            stack.addEnchantment(bukkitEnchantment, level);
            player.sendMessage(Messages.SHOPS_PREFIX.getMessage() + ChatColor.GREEN + "Votre item a été enchanté pour " + price + " " + EconomyManager.getMoneyName());

        } else {
            player.sendMessage(Messages.NOT_ENOUGH_MONEY.getMessage());
        }
    }

    public String getEnchantment() {
        return enchantment;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String describe() {
        String size = (available > 0 ? net.md_5.bungee.api.ChatColor.GREEN : net.md_5.bungee.api.ChatColor.RED) + "" + getAvailable() + " en stock";

        return super.describe() + ChatColor.GREEN + "Vente" + ChatColor.YELLOW + " de l'enchantement " + enchantment + " " + level +
                " pour " + ChatColor.AQUA + price + EconomyManager.getMoneyName() + ChatColor.YELLOW +
                " (" + size + ChatColor.YELLOW + ")";
    }

    public static class Builder extends ShopBuilder<EnchantingSign> {
        @Override
        public void describeFormat(Player player) {
            player.sendMessage(ChatColor.YELLOW + " - ShopEnchant");
            player.sendMessage(ChatColor.AQUA + " - Prix par enchant");
        }

        @Override
        public boolean hasPermission(RoleToken player) {
            return player.getLegalEntity() instanceof PlayerData &&
                    RPMachine.getInstance().getJobsManager().isRestrictionAllowed(player.getPlayer(), JobRestrictions.ENCHANTING);
        }

        public void postCreateInstructions(Player player) {
            player.sendMessage(ChatColor.AQUA + "[" + ChatColor.GREEN + "Shops" + ChatColor.AQUA + "] " + ChatColor.GREEN + "Votre boutique est presque prête ! Cliquez droit avec un item enchanté pour l'initialiser.");
        }

        @Override
        public Optional<EnchantingSign> parseSign(Block block, RoleToken tt, String[] lines) throws SignPermissionError, SignParseError {
            return Optional.of(new EnchantingSign(block.getLocation()))
                    .flatMap(sign -> extractDouble(lines[1]).map(price -> {
                        if (price > 100_000_000_000D)
                            throw new SignParseError("Le prix maximal est dépassé (100 milliards)");
                        sign.price = price;
                        return sign;
                    }))
                    .map(sign -> {
                        sign.setOwner(tt.getTag());
                        return sign;
                    });
        }
    }

    @Override
    public void debug(Player p) {
        p.sendMessage(ChatColor.YELLOW + "-----[ Débug Shop ] -----");
        p.sendMessage(ChatColor.YELLOW + "Price : " + getPrice());
        p.sendMessage(ChatColor.YELLOW + "Owner (Tag/displayable) : " + ownerTag() + " / " + owner().displayable());
        p.sendMessage(ChatColor.YELLOW + "Enchantement : " + enchantment + " " + level);
        p.sendMessage(ChatColor.YELLOW + "Available items : " + getAvailable());
    }
}
