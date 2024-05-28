package com.github.tezvn.authenticator.api.player.input;

public interface RegisterInput extends PasswordInput, Limitation {

    String getRetypePassword();

    String getPassword2nd();

    void setRetypePassword(String retypePassword);

    void setPassword2nd(String password2nd);

    boolean isPasswordMatch();

}
