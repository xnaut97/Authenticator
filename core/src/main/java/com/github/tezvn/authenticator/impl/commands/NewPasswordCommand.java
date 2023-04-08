package com.github.tezvn.authenticator.impl.commands;

import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.JavaPlayerHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.api.player.input.PasswordUpdateInput;
import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import com.github.tezvn.authenticator.impl.player.AuthenticatorEvents;
import com.github.tezvn.authenticator.impl.player.input.PasswordUpdateInputImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class NewPasswordCommand implements CommandExecutor, TabCompleter {

    private final PlayerManager playerManager;

    public NewPasswordCommand(AuthenticatorPlugin plugin) {
//        super(commandManager.getPlugin(), "matkhaumoi",
//                "Tạo mật khẩu mới",
//                "/matkhaumoi",
//                Collections.emptyList());
        this.playerManager = plugin.getPlayerManager();
    }

    public void onPlayerSingleExecute(Player player, String[] args) {
        AuthPlayer authPlayer = playerManager.getPlayer(player);
        if(authPlayer == null)
            return;
        JavaPlayerHandler javaPlayerHandler = playerManager.getDataHandler(Platform.JAVA_EDITION);
        PlayerInput playerInput = javaPlayerHandler.getInput(player);
        if(playerInput == null)
            return;
        if(!(playerInput instanceof RegisterInput))
            return;
        RegisterInput input = (RegisterInput) playerInput;
        if(args.length == 0) {
            MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu!");
            return;
        }
        String password = args[0];
        if(password.length() < getMinPasswordLength()) {
            MessageUtils.sendMessage(player, "&cMật khẩu không được ngắn hơn &6"
                    + getMinPasswordLength() + " &cký tự");
            return;
        }
        if(password.length() > getMaxPasswordLength()) {
            MessageUtils.sendMessage(player, "&cMật khẩu không được dài quá &6"
                    + getMaxPasswordLength() + " &cký tự");
            return;
        }
        if(MessageUtils.checkSpecialCharacters(password)) {
            MessageUtils.sendMessage(player, "&cMật khẩu không được chứa ký tự đặc biệt");
            return;
        }
        if(args.length == 1) {
            MessageUtils.sendTitle(player, "&cVui lòng nhập lại mật khẩu");
            return;
        }
        String retypePassword = args[1];
        if(!password.equals(retypePassword)) {
            MessageUtils.sendMessage(player, "&cMật khẩu nhập lại không trùng khớp, vui lòng thử lại.");
            return;
        }
        PasswordUpdateInput updateInput = new PasswordUpdateInputImpl(player);
        updateInput.setPassword(password);
        updateInput.setRetypePassword(retypePassword);
        AuthenticatorEvents.handlePasswordUpdateEvent(player, updateInput);
        AuthMeApi.getInstance().changePassword(player.getName(), updateInput.getPassword());
        player.kickPlayer(MessageUtils.color("&a&lKHÔI PHỤC MẬT KHẨU THÀNH CÔNG, VUI LÒNG ĐĂNG NHẬP LẠI."));
        javaPlayerHandler.removeInput(player);
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
