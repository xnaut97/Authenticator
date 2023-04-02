package com.github.tezvn.authenticator;

import com.github.tezvn.authenticator.player.AuthPlayer;
import com.github.tezvn.authenticator.player.PlayerManager;
import com.github.tezvn.authenticator.utils.AbstractDatabase;
import com.github.tezvn.authenticator.utils.AbstractDatabase.MySQL;
import com.github.tezvn.authenticator.utils.MessageUtils;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Objects;

import static com.github.tezvn.authenticator.utils.AbstractDatabase.*;

public final class AuthenticatorPlugin extends JavaPlugin implements CommandExecutor {

    private MySQL database;

    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        if(Bukkit.getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("Require dependency 'AuthMe' to run.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Objects.requireNonNull(Bukkit.getPluginCommand("test")).setExecutor(this);
        setupDatabase();
        getLogger().info("Hooked into AuthMe!");
        this.playerManager = new PlayerManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MySQL getDatabase() {
        return database;
    }

    private void setupDatabase() {
        boolean toggle = getConfig().getBoolean("database.toggle", true);
        if(!toggle)
            return;
        String username = getConfig().getString("database.username", "root");
        String password = getConfig().getString("database.password", "password");
        String name = getConfig().getString("database.name", "authenticator");
        String host = getConfig().getString("database.password", "localhost");
        String port = getConfig().getString("database.password", "default");
        String tableName = getConfig().getString("database.table-name", "user");
        this.database = new MySQL(this, username, password, name, host, port);
        if(!this.database.isConnected()) {
            getLogger().info("Use local cache provider.");
            return;
        }
        boolean createResult = this.database.createTable(tableName,
                new DatabaseElement("uuid", DatabaseElement.Type.VAR_CHAR),
                new DatabaseElement("password_2nd", DatabaseElement.Type.VAR_CHAR));
        if(createResult)
            getLogger().info("Created table '" + tableName + "' success!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if(command.getName().equalsIgnoreCase("test")) {
            FloodgateApi floodgateApi = FloodgateApi.getInstance();
            AuthMeApi authMeApi = AuthMeApi.getInstance();
            FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
            System.out.println(floodgatePlayer);
            boolean success = floodgatePlayer.sendForm(CustomForm.builder()
                    .title("Đăng Ký Tài Khoản")
                    .input("Mật khẩu", "Nhập mật khẩu...")
                    .input("Xác thực mật khẩu", "Nhập lại mật khẩu...")
                    .optionalLabel(MessageUtils.color("&cMật khẩu không trùng khớp"),
                            player.hasMetadata("retype-password-not-match"))
                    .input("Mật khẩu cấp 2", "Nhập mật khẩu cấp 2...")
                    .label(MessageUtils.color("&c&l[!] &cMật khẩu cấp 2 dùng để khôi phục mật khẩu chính."))
                    .validResultHandler(response -> {
                        String password = response.asInput(0);
                        String rePassword = response.asInput(1);
                        String password2nd = response.asInput(2);
                        if (password == null || password.isEmpty()) {
                            MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu.");
                            return;
                        }
                        if (rePassword == null || rePassword.isEmpty()) {
                            MessageUtils.sendMessage(player, "&cVui lòng nhập lại mật khẩu.");
//                            addMetadata(player, "retype-password-not-match", true, 2);
                            return;
                        }
                        if (password2nd == null || password2nd.isEmpty()) {
                            MessageUtils.sendMessage(player, "&cVui lòng nhập lại mật khẩu cấp 2.");
                            return;
                        }
                        if (!password.equals(rePassword)) {
                            MessageUtils.sendMessage(player, "&cMật khẩu không trùng khớp.");
                            return;
                        }
                        authMeApi.forceRegister(player, password);
                        MessageUtils.sendMessage(player, "&cĐăng ký thành công.");
//                        this.players.computeIfAbsent(player.getUniqueId(), uuid -> {
//                            AuthPlayer authPlayer = new AuthPlayer(player);
//                            authPlayer.setPassword2nd(password2nd);
//                            return authPlayer;
//                        });
                    })
                    .closedOrInvalidResultHandler(response -> {
                        player.kickPlayer(MessageUtils.color("&cĐăng Ký Thất Bại, Xin Thử Lại"));
                    })
                    .build());
            System.out.println("Sent result: " + success);
        }
        return true;
    }
}
