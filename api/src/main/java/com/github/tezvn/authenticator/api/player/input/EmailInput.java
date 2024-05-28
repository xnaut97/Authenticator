package com.github.tezvn.authenticator.api.player.input;

public interface EmailInput extends PlayerInput, Limitation {

    String getEmail();

    void setEmail(String email);

    String getPassword2nd();

    void setPassword2nd(String password2nd);
}
