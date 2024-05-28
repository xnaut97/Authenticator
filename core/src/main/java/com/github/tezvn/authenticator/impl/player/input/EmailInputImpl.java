package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.EmailInput;
import com.github.tezvn.authenticator.api.player.input.InputType;
import org.bukkit.entity.Player;

public class EmailInputImpl extends AbstractInput implements EmailInput {

    private String email;

    private String password2nd;

    private int attempts = 1;

    public EmailInputImpl(Player player) {
        super(player, InputType.EMAIL);
    }

    @Override
    public int getAttempts() {
        return this.attempts;
    }

    @Override
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword2nd() {
        return this.password2nd;
    }

    @Override
    public void setPassword2nd(String password2nd) {
        this.password2nd = password2nd;
    }
}
