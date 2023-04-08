package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.PasswordUpdateInput;
import org.bukkit.entity.Player;

public class PasswordUpdateInputImpl extends AbstractInput implements PasswordUpdateInput {

    private String retypePassword;

    public PasswordUpdateInputImpl(Player player) {
        super(player, InputType.PASSWORD_UPDATE);
    }

    @Override
    public String getRetypePassword() {
        return this.retypePassword;
    }

    public void setRetypePassword(String retypePassword) {
        this.retypePassword = retypePassword == null ? "" : retypePassword;
    }

    @Override
    public boolean isPasswordMatch() {
        return getPassword().equals(this.getRetypePassword());
    }
}
