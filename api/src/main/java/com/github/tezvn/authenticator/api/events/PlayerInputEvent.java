package com.github.tezvn.authenticator.api.events;

import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public abstract class PlayerInputEvent<T> extends AuthenticatorEvent {

    private final Player player;

    private final T input;

    private boolean cancel;

    public PlayerInputEvent(Player player, T input) {
        this.player = player;
        this.input = input;
    }

    public Player getPlayer() {
        return player;
    }

    public T getInput() {
        return input;
    }


}
