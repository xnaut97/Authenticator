package com.github.tezvn.authenticator.player.input;

import org.bukkit.entity.Player;

public abstract class AbstractInput {

    private final Player player;

    private final InputType type;

    private String password;

    public AbstractInput(Player player, InputType type) {
        this.player = player;
        this.type = type;
    }

    public Player getPlayer() {
        return player;
    }

    public InputType getType() {
        return type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }


}
