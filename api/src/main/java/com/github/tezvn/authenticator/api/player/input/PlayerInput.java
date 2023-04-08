package com.github.tezvn.authenticator.api.player.input;

import org.bukkit.entity.Player;

public interface PlayerInput {

    Player getPlayer();

    InputType getType();

    String getPassword();

    void setPassword(String password);

}
