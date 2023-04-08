package com.github.tezvn.authenticator.api.events;

import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import org.bukkit.entity.Player;

public final class PlayerSignUpEvent extends PlayerInputEvent<RegisterInput> {

    public PlayerSignUpEvent(Player player, RegisterInput input) {
        super(player, input);
    }

}
