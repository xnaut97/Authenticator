package com.github.tezvn.authenticator.api.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface AuthPlayer {

    OfflinePlayer getPlayer();

    String getPassword2nd();

    void setPassword2nd(String password2nd);

    boolean checkPassword(String password);

}
