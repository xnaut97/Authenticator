package com.github.tezvn.authenticator.player.input;

import org.bukkit.entity.Player;

public class LoginInput extends AbstractInput{

    private int attempts;

    public LoginInput(Player player) {
        super(player, InputType.LOGIN);
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

}
