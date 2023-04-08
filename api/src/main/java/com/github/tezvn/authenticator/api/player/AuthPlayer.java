package com.github.tezvn.authenticator.api.player;

import org.bukkit.entity.Player;

public interface AuthPlayer {

    Player getPlayer();

    String getPassword2nd();

    void setPassword2nd(String password2nd);

    boolean checkPassword(String password);

}
