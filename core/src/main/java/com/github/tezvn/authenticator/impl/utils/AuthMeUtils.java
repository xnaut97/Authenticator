package com.github.tezvn.authenticator.impl.utils;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class AuthMeUtils {

    public static PlayerAuth getPlayerAuth(Player player) {
        try {
            Field playerCacheField = AuthMeApi.class.getDeclaredField("playerCache");
            playerCacheField.setAccessible(true);
            PlayerCache playerCache = (PlayerCache) playerCacheField.get(AuthMeApi.getInstance());
            PlayerAuth playerAuth = playerCache.getAuth(player.getName());
            if (playerAuth == null) {
                Field dataSourceField = AuthMeApi.class.getDeclaredField("dataSource");
                dataSourceField.setAccessible(true);
                DataSource dataSource = (DataSource) dataSourceField.get(AuthMeApi.getInstance());
                playerAuth = dataSource.getAuth(player.getName());
            }
            return playerAuth;
        } catch (Exception e) {
            return null;
        }
    }

    public static DataSource getDataSource() {
        try {
            Field dataSourceField = AuthMeApi.class.getDeclaredField("dataSource");
            dataSourceField.setAccessible(true);
            return (DataSource) dataSourceField.get(AuthMeApi.getInstance());
        }catch (Exception e){
            return null;
        }
    }
}
