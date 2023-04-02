package com.github.tezvn.authenticator.player;

import com.github.tezvn.authenticator.AuthenticatorPlugin;
import com.github.tezvn.authenticator.utils.MessageUtils;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.Map;
import java.util.UUID;

public class PlayerManager implements Listener {

    private final Map<UUID, AuthPlayer> players = Maps.newHashMap();

    private final AuthenticatorPlugin plugin;

    private final AuthMeApi authMeApi = AuthMeApi.getInstance();

    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    private final Map<UUID, Integer> attempts = Maps.newHashMap();

    private final Map<UUID, PlayerRegisteration> registerInputs = Maps.newHashMap();

    public PlayerManager(AuthenticatorPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public AuthPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public AuthPlayer getPlayer(UUID uuid) {
        return this.players.getOrDefault(uuid, null);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId()))
            return;
        if (!authMeApi.isRegistered(player.getName()))
            openRegisterForm(player, 1);
        else
            openLoginForm(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.registerInputs.remove(event.getPlayer().getUniqueId());
    }

    private void openLoginForm(Player player) {
        if (authMeApi.isAuthenticated(player)) {
            authMeApi.forceLogin(player);
            return;
        }
        runDelayed(1, () -> {
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
        });
    }

    private void openRegisterForm(Player player, int delay) {
        runDelayed(delay, () -> {
            PlayerRegisteration input = this.registerInputs.getOrDefault(player.getUniqueId(), new PlayerRegisteration());
            this.registerInputs.putIfAbsent(player.getUniqueId(), input);
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                    .title("Đăng Ký Tài Khoản")

                    //Main password
                    .input("Mật khẩu", "Nhập mật khẩu...",
                            input.getPassword() == null ? "" : input.getPassword())
                    .optionalLabel(MessageUtils.color(input.getPassword() == null ? "" :
                                    input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                            : input.getPassword().length() > 32 ? "&cMật khẩu không được dài quá 32 ký tự"
                                            : MessageUtils.checkSpecialCharacters(input.getPassword()) ? "&cMật khẩu không được chứa ký tự đặc biệt"
                                            : ""),
                            //Condition
                            input.getPassword() != null && (input.getPassword().isEmpty()
                                    || input.getPassword().length() > 32
                                    || MessageUtils.checkSpecialCharacters(input.getPassword())))

                    //Retype password
                    .input("Xác thực mật khẩu", "Nhập lại mật khẩu...",
                            input.getRetypePassword() == null ? "" : input.getRetypePassword())
                    .optionalLabel(MessageUtils.color(input.getRetypePassword() == null ? "" :
                                    input.getRetypePassword().isEmpty() ? "&cVui lòng nhập lại mật khẩu"
                                            : !input.getPassword().equals(input.getPassword2nd()) ? "&cMật khẩu không trùng khớp"
                                            : ""),
                            //Condition
                            input.getRetypePassword() != null && (input.getRetypePassword().isEmpty()
                                    || !input.getPassword().equals(input.getRetypePassword())))

                    //Password 2nd
                    .input("Mật khẩu cấp 2", "Nhập mật khẩu cấp 2...",
                            input.getPassword2nd() == null ? "" : input.getPassword2nd())
                    .optionalLabel(MessageUtils.color(input.getPassword2nd() == null ? "" :
                                    input.getPassword2nd().isEmpty() ? "&cVui lòng nhập mật khẩu cấp 2"
                                            : input.getPassword2nd().length() > 32 ? "&cMật khẩu không được dài quá 32 ký tự"
                                            : MessageUtils.checkSpecialCharacters(input.getPassword2nd()) ? "&cMật khẩu không được chứa ký tự đặc biệt"
                                            : ""),
                            //Condition
                            input.getPassword2nd() != null && (input.getPassword2nd().isEmpty()
                                    || input.getPassword2nd().length() > 32
                                    || MessageUtils.checkSpecialCharacters(input.getPassword2nd())))
                    .label(MessageUtils.color("&7[&4&l!&7] &r&cMật khẩu cấp 2 dùng để khôi phục " +
                            "mật khẩu chính cho nên bạn bắt buộc phải có."))

                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        String rePassword = getInput(response, 1);
                        String password2nd = getInput(response, 2);
                        input.setPassword(password);
                        input.setRetypePassword(rePassword);
                        input.setPassword2nd(password2nd);
                        //Checking state
                        if (notValid(password)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        if (rePassword == null || rePassword.isEmpty() || !password.equals(rePassword)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        if (notValid(password2nd)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        authMeApi.forceRegister(player, password);
                        player.sendTitle(MessageUtils.color("&a&lĐĂNG KÝ THÀNH CÔNG"), MessageUtils.color("&7CHÚC BẠN CHƠI GAME VUI VẺ"));
                        players.computeIfAbsent(player.getUniqueId(), uuid -> {
                            AuthPlayer authPlayer = new AuthPlayer(player);
                            authPlayer.setPassword2nd(password2nd);
                            return authPlayer;
                        });
                        registerInputs.remove(player.getUniqueId());
                    })
                    .closedOrInvalidResultHandler(response -> {
                        input.setAttempts(input.getAttempts() + 1);
                        if(input.getAttempts() >= 3) {
                            player.kickPlayer(MessageUtils.color("&cĐĂNG KÝ THẤT BẠI, XIN THỬ LẠI"));
                            return;
                        }
                        openRegisterForm(player, 0);
                    })
                    .build());
        });
    }

    private void runDelayed(int delay, Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable != null)
                    runnable.run();
            }
        }.runTaskLater(plugin, delay > 0 ? 20L * delay : 1);
    }

    private void addMetadata(Player player, String key, Object value, long cooldown) {
        if (player.hasMetadata(key))
            return;
        player.setMetadata(key, new FixedMetadataValue(plugin, value));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.hasMetadata(key))
                    player.removeMetadata(key, plugin);
            }
        }.runTaskLater(plugin, 20 * cooldown);
    }

    private boolean notValid(String password) {
        return password == null || password.isEmpty() || password.length() > 32 || MessageUtils.checkSpecialCharacters(password);
    }

    private String getInput(CustomFormResponse response, int index) {
        int count = 0;
        while (response.hasNext()) {
            Object o = response.next();
            if (o instanceof String) {
                if (count == index)
                    return String.valueOf(o);
                count++;
            }
        }
        return null;
    }

}
