package com.github.tezvn.authenticator.impl.commands;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.events.PlayerPasswordCreateEvent;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.impl.player.AuthPlayerImpl;
import com.github.tezvn.authenticator.impl.player.AuthenticatorEvents;
import com.github.tezvn.authenticator.impl.player.PlayerManagerImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.github.tezvn.authenticator.api.events.PlayerPasswordCreateEvent.*;

public class PasswordCreateCommand implements CommandExecutor, TabCompleter {

    private final PlayerManager playerManager;

    public PasswordCreateCommand(AuthenticatorPlugin plugin) {
//        super(commandManager.getPlugin(), "taomatkhaucap2",
//                "Tạo mật khẩu cấp 2",
//                "/taomatkhaucap2 [mật khẩu] [nhập lại mật khẩu]", Collections.emptyList());
        this.playerManager = plugin.getPlayerManager();
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public void onPlayerSingleExecute(Player player, String[] args) {
        AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
        if(authPlayer != null) {
            MessageUtils.sendMessage(player, "&cBạn đã có mật khẩu cấp 2 rồi!");
            return;
        }
        if(args.length == 0) {
            MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu!");
            return;
        }
        String password = args[0];
        PlayerPasswordCreateEvent event = new PlayerPasswordCreateEvent(player, password);
        Bukkit.getPluginManager().callEvent(event);
        if(event.getRestrictions().size() > 0) {
            if(event.hasRestriction(RestrictionType.MIN_LENGTH)) {
                if (password.length() < getMinPasswordLength()) {
                    MessageUtils.sendMessage(player, "&cMật khẩu không được ngắn hơn &6"
                            + getMinPasswordLength() + " &cký tự");
                    return;
                }
            }
            if(event.hasRestriction(RestrictionType.MAX_LENGTH)) {
                if (password.length() > getMaxPasswordLength()) {
                    MessageUtils.sendMessage(player, "&cMật khẩu không được dài quá &6"
                            + getMaxPasswordLength() + " &cký tự");
                    return;
                }
            }
            if(event.hasRestriction(RestrictionType.SPECIAL_CHARACTER)) {
                if (MessageUtils.checkSpecialCharacters(password)) {
                    MessageUtils.sendMessage(player, "&cMật khẩu không được chứa ký tự đặc biệt");
                    return;
                }
            }
        }
        if(args.length == 1) {
            MessageUtils.sendTitle(player, "&cVui lòng nhập lại mật khẩu");
            return;
        }
        String retypePassword = args[1];
        if(!event.getPassword().equals(retypePassword)) {
            MessageUtils.sendMessage(player, "&cMật khẩu nhập lại không trùng khớp, vui lòng thử lại.");
            return;
        }
        MessageUtils.sendTitle(player, "&a&lTHIẾT LẬP THÀNH CÔNG");
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        getPlayerManager().getPlayers().computeIfAbsent(player.getUniqueId(), uuid -> {
            AuthPlayerImpl newData = new AuthPlayerImpl(player);
            newData.setPassword2nd(event.getPassword());
            ((PlayerManagerImpl) getPlayerManager()).saveToDatabase(newData);
            return newData;
        });
    }

    protected int getMinPasswordLength() {
        return getAuthMeConfig() == null ? 5 : getAuthMeConfig().getInt("settings.security.minPasswordLength", 5);
    }

    protected int getMaxPasswordLength() {
        return getAuthMeConfig() == null ? 30 : getAuthMeConfig().getInt("settings.security.passwordMaxLength", 30);
    }

    protected FileConfiguration getAuthMeConfig() {
        File file = new File("plugins/AuthMe/config.yml");
        if (!file.exists())
            return null;
        return YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof ConsoleCommandSender)
            return true;
        onPlayerSingleExecute((Player) sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("Mật khẩu");
        else if(args.length == 2)
            return Collections.singletonList("Nhập lại mật khẩu");
        return null;
    }
}
