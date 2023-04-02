package com.github.tezvn.authenticator;

import com.github.tezvn.authenticator.utils.MessageUtils;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerListener implements Listener {

    private final AuthMeApi authMeApi = AuthMeApi.getInstance();

    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    private final Map<UUID, Integer> attempts = Maps.newHashMap();

    public PlayerListener(AuthenticatorPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId()))
            return;
        if (authMeApi.isRegistered(player.getName()))
            openRegisterForm(player);
        else
            openLoginForm(player);
    }

    private void openRegisterForm(Player player) {

    }

    private void openLoginForm(Player player) {
        if (authMeApi.isAuthenticated(player)) {
            authMeApi.forceLogin(player);
            return;
        }
        SimpleForm.builder()
                .title("")
                .build();
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                .title("Đăng Nhập")
                .input("Mật khẩu", "Nhập mật khẩu...")
                .toggle("Tiến Hành Đăng Nhập")
                .validResultHandler(response -> {
                    String password = response.asInput();
                    if (password == null || password.isEmpty()) {
                        MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu.");
                        return;
                    }
                    if (!authMeApi.checkPassword(player.getName(), password)) {
                        int attempts = this.attempts.getOrDefault(player.getUniqueId(), 3);
                        if (attempts == 0) {
                            player.kickPlayer(MessageUtils.color("&cĐăng Nhập Không Thành Công, Xin Thử Lại"));
                            this.attempts.remove(player.getUniqueId());
                            return;
                        }
                        MessageUtils.sendMessage(player, "&cSai mật khẩu, bạn còn &6" + attempts + " &clần thử lại.");
                        this.attempts.put(player.getUniqueId(), attempts - 1);
                        return;
                    }
                    authMeApi.forceLogin(player);
                    MessageUtils.sendMessage(player, "&c");
                })
                .build());
    }

    private void getRegisterForm(Player player) {
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                .title("Đăng Ký Tài Khoản")
                .input("Mật khẩu", "Nhập mật khẩu...")
                .input("Nhập lại mật khẩu", "Nhập mật khẩu...")
                .toggle("Tiến Hành Tạo Tài Khoản")
                .validResultHandler(response -> {
                    String password = response.asInput(0);
                    String rePassword = response.asInput(1);
                    if (password == null || password.isEmpty()) {
                        MessageUtils.sendMessage(player, "&cVui lòng nhập mật khẩu.");
                        return;
                    }
                    if (rePassword == null || rePassword.isEmpty()) {
                        MessageUtils.sendMessage(player, "&cVui lòng nhập lại mật khẩu.");
                        return;
                    }
                    if (!password.equals(rePassword)) {
                        MessageUtils.sendMessage(player, "&cMật khẩu không trùng khớp.");
                        return;
                    }
                    authMeApi.forceRegister(player, password);
                    MessageUtils.sendMessage(player, "&cĐăng ký thành công.");
                })
                .closedOrInvalidResultHandler(response -> {
                    player.kickPlayer(MessageUtils.color("&cVui lòng nhập mật khẩu."));
                })
                .build());
    }

}
