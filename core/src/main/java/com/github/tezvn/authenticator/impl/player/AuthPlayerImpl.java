package com.github.tezvn.authenticator.impl.player;

import com.github.tezvn.authenticator.api.player.AuthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class AuthPlayerImpl implements AuthPlayer {

    private final UUID uniqueId;

    private final String name;

    private String password2nd;

    public AuthPlayerImpl(OfflinePlayer player) {
        this.uniqueId = player.getUniqueId();
        this.name = player.getName();
    }

    @Override
    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(this.uniqueId);
    }

    @Override
    public String getName() {
        return name;
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
