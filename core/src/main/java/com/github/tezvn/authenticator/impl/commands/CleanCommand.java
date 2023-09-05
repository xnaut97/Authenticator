package com.github.tezvn.authenticator.impl.commands;

import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.impl.player.PlayerManagerImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

public class CleanCommand implements CommandExecutor {

    private final PlayerManager playerManager;

    public CleanCommand(AuthenticatorPlugin plugin) {
        this.playerManager = plugin.getPlayerManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        onExecute(sender, args);
        return true;
    }

    private void onExecute(CommandSender sender, String[] args) {
        if(sender.hasPermission("authenticator.command.cleanduplicate")) {
            if(args.length == 0) {
                int size = playerManager.getPlayers().size();
                ((PlayerManagerImpl) playerManager).cleanDuplicateAccounts();
                int cleaned = playerManager.getPlayers().size();
                MessageUtils.sendMessage(sender, "&6Đã xóa &a" + (size - cleaned) + " &6tài khoản bị trùng khỏi hệ thống!");
                return;
            }
            UUID uuid = UUID.fromString(args[0]);
            Map<UUID, AuthPlayer> players = playerManager.getPlayers();
            if(!players.containsKey(uuid)) {
                MessageUtils.sendMessage(sender, "&cNgười chơi với uuid này không có trong hệ thống!");
                return;
            }
            ((PlayerManagerImpl) playerManager).deleteFromDatabase(players.remove(uuid));
            MessageUtils.sendMessage(sender, "&aĐã xóa " + uuid + " khỏi hệ thống");
        }
    }
}
