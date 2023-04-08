package com.github.tezvn.authenticator.impl.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnvilMenu implements InventoryHolder {

    private final EventHandler listener;

    private static Plugin instance;

    private final List<UUID> using = Lists.newArrayList();

    private String title = "Repair & Name";

    private final Map<SlotType, Slot> slots = Maps.newHashMap();

    private boolean closeable;

    private Consumer<Player> closeListener;

    private Consumer<Player> openListener;

    private Function<Completion, Response> completeListener;

    private Consumer<Player> leftInputClickListener;

    private Consumer<Player> rightInputClickListener;

    private final Inventory inventory;

    public AnvilMenu(Plugin plugin) {
        this.listener = new EventHandler();
        instance = plugin;
        initSlot();
        this.inventory = Bukkit.createInventory(null, InventoryType.ANVIL, title);
    }

    private void initSlot() {
        Arrays.stream(SlotType.values()).forEach(type ->
                this.slots.computeIfAbsent(type, slotType -> {
                    Slot slot = new Slot(type);
                    slot.setDrop(type != SlotType.OUTPUT);
                    return slot;
                }));
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCloseable() {
        return closeable;
    }

    public AnvilMenu setTitle(String title) {
        if (title != null) {
            this.title = title;
            this.using.forEach(player -> InventoryUpdate.updateInventory(
                    (JavaPlugin) instance, Bukkit.getPlayer(player), title));
        }
        return this;
    }

    public AnvilMenu setItem(SlotType type, ItemStack item) {
        this.slots.computeIfPresent(type, (slotType, slot) -> {
            slot.setItem(item);
            getInventory().setItem(slot.getValue(), slot.getItem());
            return slot;
        });
        return this;
    }

    public AnvilMenu setInteractable(SlotType type, boolean interactable) {
        this.slots.computeIfPresent(type, (slotType, slot) -> {
            slot.setInteractable(interactable);
            return slot;
        });
        return this;
    }

    public AnvilMenu setCloseable(boolean closeable) {
        this.closeable = closeable;
        return this;
    }

    public AnvilMenu setDrop(SlotType type, boolean drop) {
        this.slots.computeIfPresent(type, (slotType, slot) -> {
            slot.setDrop(drop);
            return slot;
        });
        return this;
    }

    public AnvilMenu onClose(Consumer<Player> closeListener) {
        this.closeListener = closeListener;
        return this;
    }

    public AnvilMenu onOpen(Consumer<Player> openListener) {
        this.openListener = openListener;
        return this;
    }

    public AnvilMenu onComplete(Function<Completion, Response> completeListener) {
        this.completeListener = completeListener;
        return this;
    }

    public AnvilMenu onLeftItemClick(Consumer<Player> leftInputClickListener) {
        this.leftInputClickListener = leftInputClickListener;
        return this;
    }

    public AnvilMenu onRightItemClick(Consumer<Player> rightInputClickListener) {
        this.rightInputClickListener = rightInputClickListener;
        return this;
    }

    public AnvilMenu open(Player player) {
        return open(player, 0);
    }

    public AnvilMenu open(Player player, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().registerEvents(listener, instance);
                player.openInventory(getInventory());
            }
        }.runTaskLater(instance, 20L * delay);
        return this;
    }

    public boolean isOpening(Player player) {
        return this.using.contains(player.getUniqueId());
    }

    private class EventHandler implements Listener {

        @org.bukkit.event.EventHandler
        public void onInventoryOpen(InventoryOpenEvent event) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            if (!inventory.equals(getInventory()))
                return;
            if (!using.contains(player.getUniqueId()))
                using.add(player.getUniqueId());
            if (openListener != null)
                openListener.accept(player);
        }

        @org.bukkit.event.EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            Player player = (Player) event.getPlayer();
            Inventory inventory = event.getInventory();
            if (!inventory.equals(getInventory()))
                return;
            if(!isCloseable()) {
                player.openInventory(getInventory());
                return;
            }
            HandlerList.unregisterAll(this);
            using.remove(player.getUniqueId());
            if (closeListener != null)
                closeListener.accept(player);
        }

        @org.bukkit.event.EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            Player player = (Player) event.getWhoClicked();
            Inventory inventory = event.getClickedInventory();
            if (inventory == null)
                return;
            if (!inventory.equals(getInventory()))
                return;
            AnvilInventory anvilInventory = (AnvilInventory) inventory;
            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
                return;
            }
            String text = anvilInventory.getRenameText();
            ItemStack leftItem = inventory.getItem(0);
            ItemStack rightItem = inventory.getItem(1);
            ItemStack outputItem = inventory.getItem(2);
            int clickedSlot = event.getRawSlot();
            Slot slot = slots.values().stream().filter(s -> s.getValue() == clickedSlot).findAny().orElse(null);
            if(slot != null)
                event.setCancelled(!slot.isInteractable());
            switch (clickedSlot) {
                case 0:
                    if (leftInputClickListener != null)
                        leftInputClickListener.accept(player);
                    break;
                case 1:
                    if (rightInputClickListener != null)
                        rightInputClickListener.accept(player);
                    break;
                case 2:
                    if (completeListener == null)
                        return;
                    Response response = completeListener.apply(new Completion(
                            leftItem, rightItem, outputItem, player, text));
                    if (response != null)
                        response.accept(AnvilMenu.this, player);
                    break;
            }
        }

        @org.bukkit.event.EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory inventory = event.getInventory();
            if(!inventory.equals(getInventory()))
                return;
            for (Integer i : event.getRawSlots()) {
                Slot slot = slots.values().stream().filter(s -> s.getValue() == i)
                        .findAny().orElse(null);
                if(slot == null)
                    continue;
                event.setCancelled(!slot.isInteractable());
            }
        }

        @org.bukkit.event.EventHandler
        public void onQuit(PlayerQuitEvent event) {
            using.remove(event.getPlayer().getUniqueId());
        }

        @org.bukkit.event.EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            using.remove(event.getEntity().getUniqueId());
        }

    }

    public interface Response extends BiConsumer<AnvilMenu, Player> {

        static Response setOutput(ItemStack item) {
            return (menu, player) -> {
                if (item == null)
                    return;
                menu.getInventory().setItem(2, item);
            };
        }

        static Response run(Runnable runnable) {
            return run(runnable, 0);
        }

        static Response run(Runnable runnable, int delay) {
            return (menu, player) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (runnable != null)
                            runnable.run();
                    }
                }.runTaskLater(instance, 20L * delay);
            };
        }

        static Response close() {
            return (menu, player) -> {
                player.closeInventory();
            };
        }

        static Response open(Inventory inventory) {
            return open(inventory, 0);
        }

        static Response open(Inventory inventory, int delay) {
            return (menu, player) -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (inventory != null)
                            player.openInventory(inventory);
                    }
                }.runTaskLater(instance, 20L * delay);
            };
        }

    }

    public static final class Completion {

        private final ItemStack leftItem;

        private final ItemStack rightItem;

        private final ItemStack outputItem;

        private final Player player;

        private final String text;

        public Completion(ItemStack leftItem, ItemStack rightItem, ItemStack outputItem, Player player, String text) {
            this.leftItem = leftItem;
            this.rightItem = rightItem;
            this.outputItem = outputItem;
            this.player = player;
            this.text = text;
        }

        public ItemStack getLeftItem() {
            return leftItem;
        }

        public ItemStack getRightItem() {
            return rightItem;
        }

        public ItemStack getOutputItem() {
            return outputItem;
        }

        public Player getPlayer() {
            return player;
        }

        public String getText() {
            return text;
        }

    }

    public static enum SlotType {
        LEFT,
        RIGHT,
        OUTPUT;

    }

    protected static class Slot {

        private final SlotType type;

        private boolean drop;

        private boolean interactable;

        private ItemStack item;

        protected Slot(SlotType type) {
            this.type = type;
        }

        public int getValue() {
            return getType().ordinal();
        }

        public SlotType getType() {
            return type;
        }

        public boolean isDrop() {
            return drop;
        }

        public void setDrop(boolean drop) {
            this.drop = drop;
        }

        public boolean isInteractable() {
            return interactable;
        }

        public void setInteractable(boolean interactable) {
            this.interactable = interactable;
        }

        public ItemStack getItem() {
            return item;
        }

        public void setItem(ItemStack item) {
            this.item = item == null ? new ItemStack(Material.AIR) : item;
        }

        public static List<Integer> getValues() {
            return Arrays.stream(SlotType.values()).map(Enum::ordinal).collect(Collectors.toList());
        }
    }
}
