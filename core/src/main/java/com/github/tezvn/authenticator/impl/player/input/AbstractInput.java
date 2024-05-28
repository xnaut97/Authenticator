package com.github.tezvn.authenticator.impl.player.input;

import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import org.bukkit.entity.Player;

public abstract class AbstractInput implements PlayerInput {

    private final Player player;

    private final InputType type;

    public AbstractInput(Player player, InputType type) {
        this.player = player;
        this.type = type;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public InputType getType() {
        return type;
    }


}
