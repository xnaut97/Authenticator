package com.github.tezvn.authenticator.player;

import org.bukkit.entity.Player;

public class AuthPlayer {

    private final Player player;

    private String password2nd;

    public AuthPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPassword2nd() {
        return password2nd;
    }

    public void setPassword2nd(String password2nd) {
        this.password2nd = password2nd;
    }

    public boolean checkPassword(String password2nd) {
        return password2nd != null && password2nd.equals(this.getPassword2nd());
    }

}
