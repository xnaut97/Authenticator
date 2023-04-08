package com.github.tezvn.authenticator.api.player.input;

public interface PasswordUpdateInput extends PlayerInput {

    String getRetypePassword();

    void setRetypePassword(String retypePassword);

    boolean isPasswordMatch();

}
