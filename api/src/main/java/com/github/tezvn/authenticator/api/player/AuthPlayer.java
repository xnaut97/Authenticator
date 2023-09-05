package com.github.tezvn.authenticator.api.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface AuthPlayer {

    UUID getUniqueId();

    OfflinePlayer getPlayer();

    String getName();

    String getPassword2nd();

    void setPassword2nd(String password2nd);

    boolean checkPassword(String password);

}
