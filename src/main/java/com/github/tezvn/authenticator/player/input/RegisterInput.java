package com.github.tezvn.authenticator.player.input;

import org.bukkit.entity.Player;

public class RegisterInput extends AbstractInput {

    private String retypePassword;

    private String password2nd;

    private int attempts;

    public RegisterInput(Player player) {
        super(player, InputType.REGISTER);
    }

    public RegisterInput(Player player, String password, String retypePassword, String password2nd) {
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
