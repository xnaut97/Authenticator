package com.github.tezvn.authenticator.api;

import com.github.tezvn.authenticator.api.AbstractDatabase.MySQL;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import org.bukkit.plugin.Plugin;

public interface AuthenticatorPlugin extends Plugin {

    PlayerManager getPlayerManager();

    MySQL getDatabase();

}
