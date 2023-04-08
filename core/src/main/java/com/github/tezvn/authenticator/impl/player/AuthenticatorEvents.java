package com.github.tezvn.authenticator.impl.player;

import com.github.tezvn.authenticator.api.events.PlayerPasswordCreateEvent;
import com.github.tezvn.authenticator.api.events.PlayerPasswordUpdateEvent;
import com.github.tezvn.authenticator.api.events.PlayerSignUpEvent;
import com.github.tezvn.authenticator.api.player.input.PasswordUpdateInput;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import com.github.tezvn.authenticator.impl.AuthenticatorPluginImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AuthenticatorEvents {

    public static void handlePasswordUpdateEvent(Player player, PasswordUpdateInput input) {
        handleEvent(new PlayerPasswordUpdateEvent(player, input));
    }

    public static void handleRegisterEvent(Player player, RegisterInput input) {
        handleEvent(new PlayerSignUpEvent(player, input));
    }

    public static void handlePasswordCreateEvent(Player player, String password) {
        handleEvent(new PlayerPasswordCreateEvent(player, password));
    }

    private static void handleEvent(Event event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(event);
            }
        }.runTask(JavaPlugin.getPlugin(AuthenticatorPluginImpl.class));
    }
}
