package com.github.tezvn.authenticator.impl.commands;

import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.PEPlayerHandler;
import com.github.tezvn.authenticator.api.player.handler.JavaPlayerHandler;
import com.github.tezvn.authenticator.api.player.input.PlayerInput;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import com.github.tezvn.authenticator.impl.player.input.RegisterInputImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Collections;
import java.util.List;

public class PasswordRecoveryCommand implements CommandExecutor, TabCompleter {

    private final PlayerManager playerManager;

    public PasswordRecoveryCommand(AuthenticatorPlugin plugin) {
//        super(commandManager.getPlugin(), "khoiphucmatkhau",
//                "Khôi phục mật khẩu",
//                "/khoiphucmatkhau",
//                Collections.emptyList());
        this.playerManager = plugin.getPlayerManager();
    }

    public void onPlayerSingleExecute(Player player, String[] args) {
        AuthPlayer authPlayer = playerManager.getPlayer(player);
        if (authPlayer == null) {
            MessageUtils.sendMessage(player, "&cBạn chưa có mật khẩu cấp 2.");
            return;
        }
        boolean isPE = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        if (args.length == 0) {
            if (!isPE) {
                MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu cấp 2");
                return;
            }
        }
        if (isPE) {
            PEPlayerHandler PEPlayerHandler = playerManager.getDataHandler(Platform.BEDROCK_OR_POCKET_EDITION);
            PEPlayerHandler.openPasswordRecoveryGUI(player);
            return;
        }
        String password = args[0];
        if (!authPlayer.checkPassword(password)) {
            MessageUtils.sendMessage(player, "&cMật khẩu cấp 2 không đúng");
            return;
        }
        JavaPlayerHandler javaPlayerHandler = playerManager.getDataHandler(Platform.JAVA_EDITION);
        RegisterInput forgotPasswordInput = new RegisterInputImpl(player);
        forgotPasswordInput.setPassword2nd(password);
        javaPlayerHandler.addInput(forgotPasswordInput);
        MessageUtils.sendMessage(player, "&aMật khẩu cấp 2 hợp lệ!");
        MessageUtils.sendRepeatedMessage(player, 0, 10, true,
                p -> {
                    PlayerInput playerInput = javaPlayerHandler.getInput(player);
                    if (playerInput == null)
                        return true;
                    if (!(playerInput instanceof RegisterInput))
                        return true;
                    RegisterInput forgotPasswordInput1 = (RegisterInput) playerInput;
                    return forgotPasswordInput1.getPassword() != null;
                },
                "&aTạo mật khẩu mới: &6/matkhaumoi [mật khẩu] [nhập lại mật khẩu]");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender)
            return true;
        onPlayerSingleExecute((Player) sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 1) {
                if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                    return Collections.singletonList("Nhập mật khẩu cấp 2");
            }
        }
        return null;
    }
}
