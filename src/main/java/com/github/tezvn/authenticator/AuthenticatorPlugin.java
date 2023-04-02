package com.github.tezvn.authenticator;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuthenticatorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        if(Bukkit.getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("Require dependency 'AuthMe' to run.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Hooked into AuthMe!");
        new PlayerListener(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
