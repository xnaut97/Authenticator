package com.github.tezvn.authenticator.impl.player.handler;

import com.github.tezvn.authenticator.impl.player.input.AbstractInput;
import com.github.tezvn.authenticator.impl.AuthenticatorPluginImpl;
import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.DataHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import com.github.tezvn.authenticator.impl.player.input.LoginInputImpl;
import com.github.tezvn.authenticator.impl.player.input.PasswordUpdateInputImpl;
import com.github.tezvn.authenticator.impl.player.input.RegisterInputImpl;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.response.CustomFormResponse;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DataHandlerImpl implements DataHandler, Listener {

    private final PlayerManager playerManager;

    private final Platform platform;

    private final AuthMeApi authMeApi = AuthMeApi.getInstance();

    private final Map<UUID, ? super PlayerInput> inputs = Maps.newHashMap();
    
    private final AuthenticatorPlugin plugin;

    public DataHandlerImpl(PlayerManager playerManager, Platform platform) {
        this.playerManager = playerManager;
        this.platform = platform;
        this.plugin = JavaPlugin.getPlugin(AuthenticatorPluginImpl.class);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeInput(event.getPlayer());
    }

    public Platform getPlatform() {
        return platform;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PlayerInput> T getInput(Player player) {
        return (T) this.inputs.getOrDefault(player.getUniqueId(), null);
    }

    public AuthMeApi getAuthMeApi() {
        return authMeApi;
    }

    public abstract void onExecute(Player player);

    protected void runDelayed(int delay, Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable != null)
                    runnable.run();
            }
        }.runTaskLater(plugin, 20L * delay);
    }

    protected String getInput(CustomFormResponse response, int index) {
        int stringCount = 0;
        String value = null;
        while (response.hasNext()) {
            Object o = response.next();
            if (o == null)
                continue;
            if (o instanceof String) {
                if (stringCount == index)
                    value = String.valueOf(o);
                stringCount++;
            }
        }
        response.reset();
        return value;
    }

    protected int getMinPasswordLength() {
        return getAuthMeConfig() == null ? 5 : getAuthMeConfig().getInt("settings.security.minPasswordLength", 5);
    }

    protected int getMaxPasswordLength() {
        return getAuthMeConfig() == null ? 30 : getAuthMeConfig().getInt("settings.security.passwordMaxLength", 30);
    }

    protected String getRegex() {
        return getAuthMeConfig() == null ? "" : getAuthMeConfig().getString("settings.restrictions.allowedPasswordCharacters");
    }

    protected FileConfiguration getAuthMeConfig() {
        File file = new File("plugins/AuthMe/config.yml");
        if (!file.exists())
            return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    protected boolean notValid(String password) {
        return password == null || password.isEmpty()
                || password.length() < getMinPasswordLength()
                || password.length() > getMaxPasswordLength()
                || !allowPasswordCharacters(password);
    }

    protected boolean allowPasswordCharacters(String str) {
        Pattern pattern = Pattern.compile(getRegex());
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    protected void addMetadata(Player player, String key, int cooldown) {
        if(player.hasMetadata(key))
            return;
        player.setMetadata(key, new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                if(player.hasMetadata(key))
                    player.removeMetadata(key, plugin);
            }
        }.runTaskLater(plugin, cooldown > 0 ? 20L*cooldown : 1);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractInput> T getOrCreate(Player player, InputType type) {
        T input = getInput(player);
        if (input == null) {
            input = switch (type) {
                case LOGIN -> (T) new LoginInputImpl(player);
                case REGISTER -> (T) new RegisterInputImpl(player);
                case PASSWORD_UPDATE -> (T) new PasswordUpdateInputImpl(player);
            };
            addInput(input);
        }
        return input;
    }

    @Override
    public void addInput(PlayerInput input) {
        this.inputs.putIfAbsent(input.getPlayer().getUniqueId(), input);
    }

    @Override
    public void removeInput(Player player) {
        this.inputs.remove(player.getUniqueId());
    }

}
