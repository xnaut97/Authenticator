package com.github.tezvn.authenticator.api.events;

import com.github.tezvn.authenticator.api.player.input.PasswordUpdateInput;
import org.bukkit.entity.Player;

public final class PlayerPasswordUpdateEvent extends PlayerInputEvent<PasswordUpdateInput> {

    public PlayerPasswordUpdateEvent(Player player, PasswordUpdateInput input) {
        super(player, input);
    }

}
