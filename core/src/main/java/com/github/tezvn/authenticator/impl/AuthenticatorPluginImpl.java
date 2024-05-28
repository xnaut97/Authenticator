package com.github.tezvn.authenticator.impl;

import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.impl.commands.CleanCommand;
import com.github.tezvn.authenticator.impl.commands.NewPasswordCommand;
import com.github.tezvn.authenticator.impl.commands.PasswordCreateCommand;
import com.github.tezvn.authenticator.impl.commands.PasswordRecoveryCommand;
import com.github.tezvn.authenticator.impl.player.AuthmeListener;
import com.github.tezvn.authenticator.impl.player.PlayerManagerImpl;
import com.github.tezvn.authenticator.api.AbstractDatabase.MySQL;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;

import static com.github.tezvn.authenticator.api.AbstractDatabase.*;

public final class AuthenticatorPluginImpl extends JavaPlugin implements AuthenticatorPlugin {

    private MySQL database;

    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("Require dependency 'AuthMe' to run.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        this.playerManager = new PlayerManagerImpl(this);
        setupCommands();
        getLogger().info("Hooked into AuthMe!");
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            setupDatabase();
            this.playerManager.load();
        });
    }

    @Override
    public void onDisable() {
        AuthmeListener.anvilGuis.forEach((player, gui) -> gui.closeInventory());
        AuthmeListener.anvilGuis.clear();
    }

    @Override
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MySQL getDatabase() {
        return database;
    }

    private void setupDatabase() {
        boolean toggle = getConfig().getBoolean("database.toggle", true);
        if(!toggle)
            return;
        String username = getConfig().getString("database.username", "root");
        String password = getConfig().getString("database.password", "password");
        String name = getConfig().getString("database.name", "authenticator");
        String host = getConfig().getString("database.host", "localhost");
        String port = getConfig().getString("database.port", "3306");
        String tableName = getConfig().getString("database.table-name", "user");
        int poolSize = getConfig().getInt("database.pool.max-pool-size", 10);
        int timeout = getConfig().getInt("database.pool.timeout", 5000);
        int idleTimeout = getConfig().getInt("database.pool.idle-timeout", 600000);
        int lifeTime = getConfig().getInt("database.pool.max-life-time", 1800000);
        this.database = new MySQL(this, username, password, name, host, port, poolSize, timeout, idleTimeout, lifeTime);
        if(!this.database.isConnected()) {
            getLogger().info("Use local cache instead.");
            return;
        }
        boolean createResult = this.database.createTable(tableName,
                new DatabaseElement("uuid", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("player_name", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("password_2nd", DatabaseElement.Type.VAR_CHAR));
        if (createResult)
            getLogger().info("Created table '" + tableName + "' success!");
    }

    private void setupCommands() {
        PluginCommand command = getCommand("taomatkhaucap2");
        if (command != null) {
            command.setExecutor(new PasswordCreateCommand(this));
            command.setTabCompleter(new PasswordCreateCommand(this));
        }
        PluginCommand command2 = getCommand("khoiphucmatkhau");
        if (command2 != null) {
            command2.setExecutor(new PasswordRecoveryCommand(this));
            command2.setTabCompleter(new PasswordRecoveryCommand(this));
        }
        PluginCommand command3 = getCommand("matkhaumoi");
        if (command3 != null) {
            command3.setExecutor(new NewPasswordCommand(this));
            command3.setTabCompleter(new NewPasswordCommand(this));
        }
        PluginCommand command4 = getCommand("cleanduplicate");
        if (command3 != null) {
            command4.setExecutor(new CleanCommand(this));
        }
    }

}
