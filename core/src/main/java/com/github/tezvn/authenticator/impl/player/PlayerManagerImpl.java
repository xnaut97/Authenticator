package com.github.tezvn.authenticator.impl.player;

import com.github.tezvn.authenticator.impl.player.handler.PEPlayerHandlerImpl;
import com.github.tezvn.authenticator.impl.player.handler.DataHandlerImpl;
import com.github.tezvn.authenticator.impl.player.handler.JavaPlayerHandlerImpl;
import com.github.tezvn.authenticator.impl.AuthenticatorPluginImpl;
import com.github.tezvn.authenticator.api.AbstractDatabase.MySQL;
import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.DataHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tezvn.authenticator.api.AbstractDatabase.DatabaseInsertion;

@Getter
public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, AuthPlayer> players = Maps.newHashMap();

    private final AuthenticatorPlugin plugin;

    private final Map<Platform, DataHandler> playerHandlers = Maps.newHashMap();

    public PlayerManagerImpl(AuthenticatorPluginImpl plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null)
            new PEPlayerHandlerImpl(this);
        new JavaPlayerHandlerImpl(this);
        new AuthmeListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public AuthPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    @Override
    public AuthPlayer getPlayer(UUID uuid) {
        return this.players.getOrDefault(uuid, null);
    }

    public void registerHandler(DataHandler playerHandler) {
        this.playerHandlers.putIfAbsent(playerHandler.getPlatform(), playerHandler);
    }

    @SuppressWarnings("unchecked")
    public <T extends DataHandler> T getDataHandler(Platform platform) {
        return (T) this.playerHandlers.get(platform);
    }

    public void save() {
        if (plugin.getDatabase() != null && plugin.getDatabase().isConnected()) saveToDatabase();
        else saveToLocal();
    }

    public void load() {
        if (plugin.getDatabase() == null || !plugin.getDatabase().isConnected())
            loadFromLocal();
        else
            loadFromDatabase();
    }

    public void saveToLocal() {
        if (this.players.isEmpty())
            return;
        this.players.forEach((uuid, authPlayer) -> {
            saveToLocal(authPlayer);
        });
        plugin.getLogger().info("Saved " + players.size() + " players to local.");
    }

    public void saveToLocal(AuthPlayer authPlayer) {
        try {
            File file = new File(plugin.getDataFolder() + "/user-cache.yml");
            if (!file.exists())
                file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set(authPlayer.getName() + ".uuid", authPlayer.getPlayer().getUniqueId().toString());
            config.set(authPlayer.getName() + ".password-2nd", authPlayer.getPassword2nd());
            config.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadFromLocal() {
        File file = new File(plugin.getDataFolder() + "/user-cache.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.getKeys(false).forEach(name -> {
            UUID uuid = UUID.fromString(config.getString(name + ".uuid", ""));
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String password = config.getString(name + ".password-2nd", name + "123456");
            this.players.computeIfAbsent(uuid, u -> {
                AuthPlayer authPlayer = new AuthPlayerImpl(player);
                authPlayer.setPassword2nd(password);
                return authPlayer;
            });
        });
        if (this.players.size() > 0)
            plugin.getLogger().info("Loaded " + players.size() + " players from local!");
    }

    public void saveToDatabase() {
        if (this.players.isEmpty())
            return;
        this.players.forEach((uuid, authPlayer) -> saveToDatabase(authPlayer));
        plugin.getLogger().info("Saved " + players.size() + " players to database.");
    }

    public void saveToDatabase(AuthPlayer authPlayer) {
        MySQL database = plugin.getDatabase();
        try (Connection connection = database.getConnection()) {
            String tableName = plugin.getConfig().getString("database.table-name", null);
            if (tableName == null)
                return;
            database.addOrUpdate(tableName,
                    new DatabaseInsertion("uuid", authPlayer.getPlayer().getUniqueId().toString()),
                    //
                    new DatabaseInsertion("uuid", authPlayer.getPlayer().getUniqueId().toString()),
                    new DatabaseInsertion("player_name", authPlayer.getName()),
                    new DatabaseInsertion("password_2nd", authPlayer.getPassword2nd()));
        } catch (Exception e) {
            saveToLocal(authPlayer);
        }
    }

    public void loadFromDatabase() {
        MySQL database = plugin.getDatabase();
        if (database == null || !database.isConnected())
            return;
        try (Connection connection = database.getConnection()) {
            String tableName = plugin.getConfig().getString("database.table-name", "user");
            ResultSet rs = connection.prepareStatement("SELECT * FROM " + tableName).executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String password = rs.getString("password_2nd");
                this.players.computeIfAbsent(uuid, u -> {
                    AuthPlayer authPlayer = new AuthPlayerImpl(Bukkit.getOfflinePlayer(uuid));
                    authPlayer.setPassword2nd(password);
                    return authPlayer;
                });
            }
            if (this.players.size() > 0)
                plugin.getLogger().info("Loaded " + players.size() + " players from database!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFromDatabase(AuthPlayer player) {
        if (player == null)
            return;
        MySQL database = plugin.getDatabase();
        if (database == null || !database.isConnected())
            return;
        String tableName = plugin.getConfig().getString("database.table-name", "user");
        plugin.getDatabase().remove(tableName, "uuid", player.getUniqueId().toString());
    }

    public void cleanDuplicateAccounts() {
        Iterator<Map.Entry<UUID, AuthPlayer>> iterator = this.players.entrySet().iterator();
        while (iterator.hasNext()) {
            AuthPlayer player = iterator.next().getValue();
            if (player.getName().startsWith(".") && !player.getUniqueId().toString().startsWith("00000000-")) {
                iterator.remove();
                deleteFromDatabase(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isBE = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        Platform playerPlatform = getPlatform(player);
        if (playerPlatform == Platform.JAVA_EDITION) {
            if (player.getName().startsWith(".")) {
                if (!player.hasPermission("authenticator.pc.bypass")) {
                    String type = AuthMeApi.getInstance().isRegistered(player.getName()) ? "ĐĂNG NHẬP" : "ĐĂNG KÍ";
                    MessageUtils.sendKickMessage(player, "pc-player-join-with-pe-name",
                            "@authenticator-type@:" + type);
                    return;
                }
            }
            if (!player.hasPermission("authenticator.java.bypass")) {
                MessageUtils.sendKickMessage(player, "block-java-connection");
                return;
            }
        }

        AtomicBoolean login = new AtomicBoolean(true);
        AuthMeApi.getInstance().getPlayerInfo(player.getName()).ifPresent(playerAuth -> {
            Platform platform = foundDuplicate(player);
            if (platform != null && playerPlatform != platform) {
                String type = platform == Platform.BEDROCK_OR_POCKET_EDITION ? "ĐIỆN THOẠI" : "MÁY TÍNH";
                MessageUtils.sendKickMessage(player, "already-registered", "@platform-type@:" + type);
                login.set(false);
            }
        });

        if (login.get()) {
            DataHandlerImpl handler = (DataHandlerImpl) this.playerHandlers.getOrDefault(
                    isBE ? Platform.BEDROCK_OR_POCKET_EDITION : Platform.JAVA_EDITION, null);
            if (handler != null)
                handler.onExecute(player);
        }

    }

    private Platform foundDuplicate(Player player) {
        AuthPlayer authPlayer = this.players.values().stream()
                .filter(p -> !p.getUniqueId().equals(player.getUniqueId()) && p.getName() != null && p.getName().equals(player.getName()))
                .findAny().orElse(null);
        return authPlayer == null ? null
                : authPlayer.getUniqueId().toString().startsWith("00000000-")
                ? Platform.BEDROCK_OR_POCKET_EDITION : Platform.JAVA_EDITION;
    }

    private Platform getPlatform(Player player) {
        return player.getUniqueId().toString().startsWith("00000000-")
                ? Platform.BEDROCK_OR_POCKET_EDITION : Platform.JAVA_EDITION;
    }

    private PlayerAuth getPlayerAuth(Player player) {
        try {
            Field playerCacheField = AuthMeApi.class.getDeclaredField("playerCache");
            playerCacheField.setAccessible(true);
            PlayerCache playerCache = (PlayerCache) playerCacheField.get(AuthMeApi.getInstance());
            PlayerAuth playerAuth = playerCache.getAuth(player.getName());
            if (playerAuth == null) {
                Field dataSourceField = AuthMeApi.class.getDeclaredField("dataSource");
                dataSourceField.setAccessible(true);
                fr.xephi.authme.datasource.DataSource dataSource = (fr.xephi.authme.datasource.DataSource) dataSourceField.get(AuthMeApi.getInstance());
                playerAuth = dataSource.getAuth(player.getName());
            }
            return playerAuth;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        AuthPlayer authPlayer = getPlayer(event.getPlayer());
//        if (authPlayer == null)
//            return;
//        saveToLocal(authPlayer);
//        saveToDatabase(authPlayer);
//    }

//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//        ItemStack clicked = event.getCurrentItem();
//        ItemStack cursor = event.getCursor();
//        System.out.println("Clicked: " + clicked);
//        System.out.println("Cursor: " + cursor);
//        System.out.println("Clicked slot: " + event.getSlot());
//    }
//
//    @EventHandler
//    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
//
//    }

}
