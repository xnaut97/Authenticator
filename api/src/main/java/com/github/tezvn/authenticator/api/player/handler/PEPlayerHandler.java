package com.github.tezvn.authenticator.api.player.handler;

import org.bukkit.entity.Player;

public interface PEPlayerHandler extends DataHandler {

    /**
     * Open Password Recovery for bedrock/pocket player
     * @param player Player to open
     */
    void openPasswordRecoveryGUI(Player player);

}
