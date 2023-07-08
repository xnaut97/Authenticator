package com.github.tezvn.authenticator.impl.player;

import com.github.tezvn.authenticator.api.AbstractDatabase.MySQL;
import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.DataHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.impl.AuthenticatorPluginImpl;
import com.github.tezvn.authenticator.impl.player.handler.BPPlayerHandlerImpl;
import com.github.tezvn.authenticator.impl.player.handler.DataHandlerImpl;
import com.github.tezvn.authenticator.impl.player.handler.JavaPlayerHandlerImpl;
import com.google.common.collect.Maps;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;

import static com.github.tezvn.authenticator.api.AbstractDatabase.DatabaseInsertion;

public class PlayerManagerImpl implements PlayerManager, Listener {

    private final Map<UUID, AuthPlayer> players = Maps.newHashMap();

    private final AuthenticatorPlugin plugin;

    private final Map<Platform, DataHandler> playerHandlers = Maps.newHashMap();

    public PlayerManagerImpl(AuthenticatorPluginImpl plugin) {
        this.plugin = plugin;
        new BPPlayerHandlerImpl(this);
        new JavaPlayerHandlerImpl(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Map<UUID, AuthPlayer> getPlayers() {
        return players;
    }

    public AuthenticatorPlugin getPlugin() {
        return plugin;
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
        saveToLocal();
        saveToDatabase();
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
        if (database == null || !database.isConnected())
            return;
        String tableName = plugin.getConfig().getString("database.table-name", null);
        if (tableName == null)
            return;
        database.addOrUpdate(tableName,
                new DatabaseInsertion("uuid", authPlayer.getPlayer().getUniqueId().toString()),
                //
                new DatabaseInsertion("uuid", authPlayer.getPlayer().getUniqueId().toString()),
                new DatabaseInsertion("player_name", authPlayer.getName()),
                new DatabaseInsertion("password_2nd", authPlayer.getPassword2nd()));
    }

    public void loadFromDatabase() {
        MySQL database = plugin.getDatabase();
        if (database == null || !database.isConnected())
            return;
        String tableName = plugin.getConfig().getString("database.table-name", "user");
        try (Connection connection = plugin.getDatabase().getConnection()){
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + tableName);
            ResultSet rs = statement.executeQuery();
            if (rs == null)
                return;
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DataHandlerImpl handler = (DataHandlerImpl) this.playerHandlers.getOrDefault(
                FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())
                        ? Platform.BEDROCK_OR_POCKET_EDITION : Platform.JAVA_EDITION, null);
        if (handler != null)
            handler.onExecute(player);
    }

//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        AuthPlayer authPlayer = getPlayer(event.getPlayer());
//        if (authPlayer == null)
//            return;
//        saveToLocal(authPlayer);
//        saveToDatabase(authPlayer);
//    }

}
