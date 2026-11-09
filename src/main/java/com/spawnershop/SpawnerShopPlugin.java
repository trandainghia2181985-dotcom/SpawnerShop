package com.spawnershop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpawnerShopPlugin extends JavaPlugin implements Listener, CommandExecutor {

    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int SIZE = 36;
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private final List<Entry> entries = new ArrayList<>();
    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private Economy economy;

    private record Entry(String name, String type, double price, Material icon) {
    }

    private static final class ShopHolder implements InventoryHolder {
        private final Map<Integer, Entry> bySlot = new HashMap<>();
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadEntries();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = getCommand("spawnershop");
        if (command != null) {
            command.setExecutor(this);
        }
        getLogger().info("SpawnerShop da bat. So spawner: " + entries.size());
    }

    // ------------------------------------------------------------------ config

    private void loadEntries() {
        entries.clear();
        List<Map<?, ?>> list = getConfig().getMapList("spawners");
        for (Map<?, ?> map : list) {
            Object name = map.get("name");
            Object type = map.get("type");
            Object price = map.get("price");
            Object icon = map.get("icon");
            if (name == null || type == null || !(price instanceof Number number)) {
                getLogger().warning("Bo qua mot muc spawner thieu name/type/price trong config.");
                continue;
            }
            String typeName = String.valueOf(type).toLowerCase(Locale.ROOT);
            Material material = null;
            if (icon != null) {
                material = Material.matchMaterial(String.valueOf(icon));
            }
            if (material == null) {
                material = Material.matchMaterial(typeName.toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
            }
            if (material == null) {
                material = Material.SPAWNER;
            }
            entries.add(new Entry(String.valueOf(name), typeName, number.doubleValue(), material));
        }
        if (entries.size() > SLOTS.length) {
            getLogger().warning("Chi hien duoc " + SLOTS.length + " spawner, phan du bi bo qua.");
        }
    }

    private String text(String key, String... pairs) {
        String value = getConfig().getString("messages." + key, "");
        value = value.replace("%prefix%", getConfig().getString("messages.prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            value = value.replace(pairs[i], pairs[i + 1]);
        }
        return value;
    }

    private Component color(String value) {
        return LEGACY.deserialize(value);
    }

    private Component plain(String value) {
        return color(value).decoration(TextDecoration.ITALIC, false);
    }

    private String formatPrice(double price) {
        return priceFormat.format(price);
    }

    private void playSound(Player player, String configKey) {
        String name = getConfig().getString("sounds." + configKey, "");
        if (name.isEmpty()) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT).replace('_', '.');
        player.playSound(player.getLocation(), key, 0.5f, 1.0f);
    }

    private Economy economy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> provider =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (provider != null) {
                economy = provider.getProvider();
            }
        }
        return economy;
    }

    // ------------------------------------------------------------------ gui

    private void openShop(Player player) {
        ShopHolder holder = new ShopHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, color(getConfig().getString("title", "Spawner Shop")));
        holder.inventory = inventory;

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.text(" "));
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        int shift = getConfig().getInt("amount-shift-click", 10);
        for (int i = 0; i < entries.size() && i < SLOTS.length; i++) {
            Entry entry = entries.get(i);
            ItemStack item = new ItemStack(entry.icon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(plain("&c" + entry.name()));

            List<Component> lore = new ArrayList<>();
            for (String line : getConfig().getStringList("lore")) {
                lore.add(plain(line
                        .replace("%price%", formatPrice(entry.price()))
                        .replace("%shiftamount%", String.valueOf(shift))));
            }
            meta.lore(lore);
            item.setItemMeta(meta);

            inventory.setItem(SLOTS[i], item);
            holder.bySlot.put(SLOTS[i], entry);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Entry entry = holder.bySlot.get(event.getSlot());
        if (entry == null) {
            return;
        }
        int amount = event.isShiftClick()
                ? Math.max(1, getConfig().getInt("amount-shift-click", 10))
                : Math.max(1, getConfig().getInt("amount-click", 1));
        purchase(player, entry, amount);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------ purchase

    private void purchase(Player player, Entry entry, int amount) {
        double total = entry.price() * amount;
        String price = formatPrice(total);

        Economy eco = economy();
        if (eco == null) {
            player.sendMessage(color(text("no-economy")));
            playSound(player, "fail");
            return;
        }
        if (!eco.has(player, total)) {
            player.sendMessage(color(text("not-enough-money", "%price%", price)));
            playSound(player, "fail");
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(color(text("inventory-full")));
            playSound(player, "fail");
            return;
        }

        EconomyResponse response = eco.withdrawPlayer(player, total);
        if (!response.transactionSuccess()) {
            player.sendMessage(color(text("not-enough-money", "%price%", price)));
            playSound(player, "fail");
            return;
        }

        String command = getConfig().getString("give-command", "ss give spawner %player% %type% %amount%")
                .replace("%player%", player.getName())
                .replace("%type%", entry.type())
                .replace("%amount%", String.valueOf(amount));
        boolean ok;
        try {
            ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception exception) {
            getLogger().warning("Loi khi chay lenh '" + command + "': " + exception.getMessage());
            ok = false;
        }

        if (!ok) {
            eco.depositPlayer(player, total);
            player.sendMessage(color(text("give-failed")));
            playSound(player, "fail");
            getLogger().warning("Lenh give that bai, da hoan tien: " + command);
            return;
        }

        player.sendMessage(color(text("success",
                "%amount%", String.valueOf(amount),
                "%name%", entry.name(),
                "%price%", price)));
        playSound(player, "success");
    }

    // ------------------------------------------------------------------ command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("spawnershop.admin")) {
                sender.sendMessage(color(text("no-permission")));
                return true;
            }
            reloadConfig();
            loadEntries();
            sender.sendMessage(color(text("reloaded")));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(getConfig().getString("messages.players-only", "Players only.")));
            return true;
        }
        if (!player.hasPermission("spawnershop.use")) {
            player.sendMessage(color(text("no-permission")));
            return true;
        }
        openShop(player);
        return true;
    }
}
