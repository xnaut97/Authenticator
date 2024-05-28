package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.PasswordInput;
import org.bukkit.entity.Player;

public abstract class PasswordInputImpl extends AbstractInput implements PasswordInput {

    private String password;

    public PasswordInputImpl(Player player, InputType type) {
        super(player, type);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

}
