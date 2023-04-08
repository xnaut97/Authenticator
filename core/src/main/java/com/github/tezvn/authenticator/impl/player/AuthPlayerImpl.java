package com.github.tezvn.authenticator.impl.player;

import com.github.tezvn.authenticator.api.player.AuthPlayer;
import org.bukkit.entity.Player;

public final class AuthPlayerImpl implements AuthPlayer {

    private final Player player;

    private String password2nd;

    public AuthPlayerImpl(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public String getPassword2nd() {
        return password2nd;
    }

    @Override
    public void setPassword2nd(String password2nd) {
        this.password2nd = password2nd;
    }

    @Override
    public boolean checkPassword(String password2nd) {
        return password2nd != null && password2nd.equals(this.getPassword2nd());
    }

}
