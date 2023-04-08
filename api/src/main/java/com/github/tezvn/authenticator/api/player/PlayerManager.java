package com.github.tezvn.authenticator.api.player;

import com.github.tezvn.authenticator.api.player.handler.DataHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface PlayerManager {

    Map<UUID, AuthPlayer> getPlayers();

    AuthPlayer getPlayer(Player player);

    AuthPlayer getPlayer(UUID uuid);

    void save();

    void load();

    void saveToDatabase();

    void loadFromDatabase();

    <T extends DataHandler> T getDataHandler(Platform platform);

}
