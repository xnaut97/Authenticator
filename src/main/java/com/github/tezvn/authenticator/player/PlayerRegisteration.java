package com.github.tezvn.authenticator.player;

public class PlayerRegisteration {

    private String password;

    private String retypePassword;

    private String password2nd;

    private int attempts;

    public PlayerRegisteration() {
    }

    public PlayerRegisteration(String password, String retypePassword, String password2nd) {
        this.password = password;
        this.retypePassword = retypePassword;
        this.password2nd = password2nd;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
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
}
