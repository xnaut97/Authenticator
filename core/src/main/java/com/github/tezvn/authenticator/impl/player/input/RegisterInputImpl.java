package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class RegisterInputImpl extends PasswordInputImpl implements RegisterInput {

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

    public void setRetypePassword(String retypePassword) {
        this.retypePassword = retypePassword == null ? "" : retypePassword;
    }

    public void setPassword2nd(String password2nd) {
        this.password2nd = password2nd == null ? "" : password2nd;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isPasswordMatch() {
        return getPassword().equals(this.getRetypePassword());
    }

}
