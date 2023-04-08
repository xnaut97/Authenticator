package com.github.tezvn.authenticator.api.events;

import com.github.tezvn.authenticator.api.player.input.LoginInput;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public final class PlayerSignInEvent extends PlayerInputEvent<LoginInput> implements Cancellable {

    private boolean cancel;

    public PlayerSignInEvent(Player player, LoginInput input) {
        super(player, input);
    }

    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

}
