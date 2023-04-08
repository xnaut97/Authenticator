package com.github.tezvn.authenticator.api.player.handler;

import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import org.bukkit.entity.Player;

public interface DataHandler {

    Platform getPlatform();

    <T extends PlayerInput> T getInput(Player player);

    void addInput(PlayerInput input);

    void removeInput(Player player);

}
