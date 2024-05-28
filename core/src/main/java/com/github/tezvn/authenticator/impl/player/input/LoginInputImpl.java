package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.LoginInput;
import com.github.tezvn.authenticator.api.player.input.PasswordInput;
import org.bukkit.entity.Player;

public class LoginInputImpl extends PasswordInputImpl implements LoginInput {

    private int attempts;

    public LoginInputImpl(Player player) {
        super(player, InputType.LOGIN);
    }

    @Override
    public int getAttempts() {
        return attempts;
    }

    @Override
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

}
