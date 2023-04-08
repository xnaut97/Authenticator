package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import org.bukkit.entity.Player;

public class RegisterInputImpl extends AbstractInput implements RegisterInput {

    private String retypePassword;

    private String password2nd;

    private int attempts;

    public RegisterInputImpl(Player player) {
        super(player, InputType.REGISTER);
    }

    public RegisterInputImpl(Player player, String password, String retypePassword, String password2nd) {
        super(player, InputType.REGISTER);
        setPassword(password);
        this.retypePassword = retypePassword;
        this.password2nd = password2nd;
    }

    public String getRetypePassword() {
        return retypePassword;
    }

    public void setRetypePassword(String retypePassword) {
        this.retypePassword = retypePassword == null ? "" : retypePassword;
    }

    public String getPassword2nd() {
        return password2nd;
    }

    public void setPassword2nd(String password2nd) {
        this.password2nd = password2nd == null ? "" : password2nd;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isPasswordMatch() {
        return getPassword().equals(this.getRetypePassword());
    }

}
