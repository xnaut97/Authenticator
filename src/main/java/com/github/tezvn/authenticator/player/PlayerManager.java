package com.github.tezvn.authenticator.player;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.authenticator.AuthenticatorPlugin;
import com.github.tezvn.authenticator.player.input.*;
import com.github.tezvn.authenticator.utils.MessageUtils;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public class PlayerManager implements Listener {

    private final Map<UUID, AuthPlayer> players = Maps.newHashMap();

    private final AuthenticatorPlugin plugin;

    private final AuthMeApi authMeApi = AuthMeApi.getInstance();

    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    private final Map<UUID, ? super AbstractInput> inputs = Maps.newHashMap();

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

    @SuppressWarnings("unchecked")
    private <T extends AbstractInput> T getInput(Player player, InputType type) {
        T input = (T) this.inputs.getOrDefault(player.getUniqueId(), null);
        if (input == null)
            switch (type) {
                case LOGIN:
                    input = (T) new LoginInput(player);
                    break;
                case REGISTER:
                    input = (T) new RegisterInput(player);
                    break;
                case FORGOT_PASSWORD:
                    input = (T) new ForgotPasswordInput(player);
                    break;
            }
        return input;
    }

    private void addInput(AbstractInput input) {
        this.inputs.putIfAbsent(input.getPlayer().getUniqueId(), input);
    }

    private void removeInput(Player player) {
        this.inputs.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId()))
            return;
        if (!authMeApi.isRegistered(player.getName()))
            openRegisterForm(player, 1);
        else
            openLoginSelectionForm(player, 1);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeInput(event.getPlayer());
    }

    private void openLoginSelectionForm(Player player, int delay) {
        runDelayed(delay, () -> {
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                    .button("ĐĂNG NHẬP")
                    .button("LẤY LẠI MẬT KHẨU")
                    .button("THOÁT GAME")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        switch (id) {
                            case 0:
                                if (authMeApi.isAuthenticated(player)) {
                                    authMeApi.forceLogin(player);
                                    return;
                                }
                                openLoginForm(player, 1);
                                break;
                            case 1:
                                openPassword2ndCheckingForm(player, 1);
                                break;
                            case 2:
                                player.kickPlayer("BẠN ĐÃ ĐĂNG XUẤT");
                                break;
                        }
                    })
                    .closedOrInvalidResultHandler(() -> openLoginSelectionForm(player, 0))
                    .build());
        });
    }

    private void openLoginForm(Player player, int delay) {
        runDelayed(delay, () -> {
            LoginInput input = getInput(player, InputType.LOGIN);
            addInput(input);
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                    .title("ĐĂNG NHẬP")
                    .input("Mật khẩu", "Nhập mật khẩu...",
                            input.getPassword() == null ? "" : input.getPassword())
                    .optionalLabel(MessageUtils.color(input.getPassword() == null ? "" :
                                    input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                            : !authMeApi.checkPassword(player.getName(), input.getPassword())
                                            ? "&cSai mật khẩu, bạn còn &6" + (5 - input.getAttempts()) + " &clần thử lại."
                                            : ""),
                            //Condition
                            input.getPassword() != null && (!authMeApi.checkPassword(player.getName(), input.getPassword())))
                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        if (!authMeApi.checkPassword(player.getName(), password)) {
                            int attempts = input.getAttempts();
                            if (attempts >= 5) {
                                removeInput(player);
                                openAskingForm(player);
                                return;
                            }
                            input.setAttempts(input.getAttempts() + 1);
                            openLoginForm(player, 0);
                            XSound.ENTITY_BLAZE_DEATH.play(player, .75f, -1);
                            return;
                        }
                        authMeApi.forceLogin(player);
                        player.sendTitle(MessageUtils.color("ĐĂNG NHẬP THÀNH CÔNG"), "");
                        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                        removeInput(player);
                    })
                    .build());
        });
    }

    private void openAskingForm(Player player) {
        runDelayed(0, () -> {
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                    .title("ĐĂNG NHẬP")
                    .content("Bạn đã nhập sai mật khẩu quá nhiều lần, có muốn khôi phục lại mật khẩu không?")
                    .button(MessageUtils.color("&cĐỒNG Ý"))
                    .button(MessageUtils.color("&cKHÔNG, TÔI MUỐN THỬ LẠI"))
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        switch (id) {
                            case 0:
                                openPassword2ndCheckingForm(player, 0);
                                break;
                            case 1:
                                openLoginForm(player, 0);
                                break;
                        }
                    })
                    .closedOrInvalidResultHandler(() -> openLoginForm(player, 0))
                    .build());
        });
    }

    private void openPassword2ndCheckingForm(Player player, int delay) {
        runDelayed(delay, () -> {
            AuthPlayer authPlayer = getPlayer(player);
            ForgotPasswordInput input = getInput(player, InputType.FORGOT_PASSWORD);
            addInput(input);
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                    .title("KHÔI PHUC MẬT KHẨU")
                    .label(MessageUtils.color("&7[&4&l!&7] &cDùng mật khẩu cấp 2 để khôi phục"))
                    .input("Mật khẩu cấp 2", "Nhập mật khẩu cấp 2...",
                            input.getPassword2nd() == null ? "" : input.getPassword2nd())
                    .optionalLabel(MessageUtils.color(input.getPassword2nd() == null ? "" :
                                    input.getPassword2nd().isEmpty() ? "&cVui lòng nhập mật khẩu cấp 2"
                                            : !authPlayer.checkPassword(input.getPassword2nd())
                                            ? "&cMật khẩu cấp 2 không trùng khớp với tài khoản, " +
                                            "bạn còn &6" + (5 - input.getAttempts()) + " &clần thử lại."
                                            : ""),
                            input.getPassword2nd() != null && !authPlayer.checkPassword(input.getPassword2nd()))
                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        input.setPassword2nd(password);
                        if (!authPlayer.checkPassword(input.getPassword2nd())) {
                            int attempts = input.getAttempts();
                            if (attempts >= 5) {
                                removeInput(player);
                                openLoginSelectionForm(player, 0);
                                return;
                            }
                            input.setAttempts(input.getAttempts() + 1);
                            openPassword2ndCheckingForm(player, 0);
                            XSound.ENTITY_BLAZE_DEATH.play(player, .75f, -1);
                            return;
                        }
                        openResetPasswordForm(player);
                    })
                    .closedOrInvalidResultHandler(() -> {
                        removeInput(player);
                        openLoginForm(player, 0);
                    })
                    .build());
        });
    }

    private void openResetPasswordForm(Player player) {
        runDelayed(0, () -> {
            ForgotPasswordInput input = getInput(player, InputType.FORGOT_PASSWORD);
            addInput(input);
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                    .title("KHÔI PHỤC MẬT KHẨU")

                    //Main password
                    .input("Mật khẩu", "Nhập mật khẩu...",
                            input.getPassword() == null ? "" : input.getPassword())
                    .optionalLabel(MessageUtils.color(input.getPassword() == null ? "" :
                                    input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                            : input.getPassword().length() < getMinPasswordLength()
                                            ? "&cMật khẩu không được ngắn hơn &6" + getMinPasswordLength() + " &cký tự"
                                            : input.getPassword().length() > getMaxPasswordLength()
                                            ? "&cMật khẩu không được dài quá &6" + getMaxPasswordLength() + " &cký tự"
                                            : MessageUtils.checkSpecialCharacters(input.getPassword()) ? "&cMật khẩu không được chứa ký tự đặc biệt"
                                            : ""),
                            //Condition
                            input.getPassword() != null && (input.getPassword().isEmpty()
                                    || input.getPassword().length() < getMinPasswordLength()
                                    || input.getPassword().length() > getMaxPasswordLength()
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
                                    || !input.isPasswordMatch()))
                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        String rePassword = getInput(response, 1);
                        input.setPassword(password);
                        input.setRetypePassword(rePassword);
                        if (notValid(password)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        if (rePassword == null || rePassword.isEmpty() || !password.equals(rePassword)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        openResetPasswordResult(player);
                    })
                    .closedOrInvalidResultHandler(response -> openResetPasswordForm(player))
                    .build());
        });
    }

    private void openResetPasswordResult(Player player) {
        runDelayed(0, () -> {
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                    .title("KHÔI PHỤC MẬT KHẨU")
                    .content(MessageUtils.color("&aKHÔI PHỤC MẬT KHẨU THÀNH CÔNG, ĐANG ĐIỀU HƯỚNG BẠN VỀ TRANG ĐĂNG NHẬP..."))
                    .build());

            runDelayed(3, () -> {
                openLoginForm(player, 0);
            });
        });
    }

    private void openRegisterForm(Player player, int delay) {
        runDelayed(delay, () -> {
            RegisterInput input = getInput(player, InputType.REGISTER);
            addInput(input);
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                    .title("ĐĂNG KÝ TÀI KHOẢN")
                    //Main password
                    .input("Mật khẩu", "Nhập mật khẩu...",
                            input.getPassword() == null ? "" : input.getPassword())
                    .optionalLabel(MessageUtils.color(input.getPassword() == null ? ""
                                    : input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                    : input.getPassword().length() < getMinPasswordLength()
                                    ? "&c✘ Mật khẩu không được ngắn hơn &6" + getMinPasswordLength() + " &cký tự"
                                    : input.getPassword().length() > getMaxPasswordLength()
                                    ? "&c✘ Mật khẩu không được dài quá &6" + getMaxPasswordLength() + " &cký tự"
                                    : MessageUtils.checkSpecialCharacters(input.getPassword()) ? "&cMật khẩu không được chứa ký tự đặc biệt"
                                    : "&a✔ Mật khẩu hợp lệ"),
                            //Condition
                            input.getPassword() != null &&
                                    (input.getPassword().isEmpty()
                                            || input.getPassword().length() < getMinPasswordLength()
                                            || input.getPassword().length() > getMaxPasswordLength()
                                            || MessageUtils.checkSpecialCharacters(input.getPassword())))

                    //Retype password
                    .input("Xác thực mật khẩu", "Nhập lại mật khẩu...",
                            input.getRetypePassword() == null ? "" : input.getRetypePassword())
                    .optionalLabel(MessageUtils.color(input.getRetypePassword() == null ? ""
                                    : input.getRetypePassword().isEmpty() ? "&cVui lòng nhập lại mật khẩu"
                                    : !input.isPasswordMatch() ? "&c✘ Mật khẩu không trùng khớp"
                                    : "&c✔ Mật khẩu trùng khớp"),
                            //Condition
                            input.getRetypePassword() != null
                                    && (input.getRetypePassword().isEmpty()
                                    || !input.isPasswordMatch()))

                    //Password 2nd
                    .input("Mật khẩu cấp 2", "Nhập mật khẩu cấp 2...",
                            input.getPassword2nd() == null ? "" : input.getPassword2nd())
                    .optionalLabel(MessageUtils.color(input.getPassword2nd() == null ? ""
                                    : input.getPassword2nd().isEmpty() ? "&cVui lòng nhập mật khẩu cấp 2"
                                    : input.getPassword2nd().length() < getMinPasswordLength()
                                    ? "&cMật khẩu không được ngắn hơn &6" + getMinPasswordLength() + " &cký tự"
                                    : input.getPassword2nd().length() > getMaxPasswordLength()
                                    ? "&cMật khẩu không được dài quá &6" + getMaxPasswordLength() + " &cký tự"
                                    : MessageUtils.checkSpecialCharacters(input.getPassword2nd()) ? "&cMật khẩu không được chứa ký tự đặc biệt"
                                    : "&a✔ Mật khẩu hợp lệ"),
                            //Condition
                            input.getPassword2nd() != null
                                    && (input.getPassword2nd().isEmpty()
                                    || input.getPassword2nd().length() < getMinPasswordLength()
                                    || input.getPassword2nd().length() > getMaxPasswordLength()
                                    || MessageUtils.checkSpecialCharacters(input.getPassword2nd())))
                    .label(MessageUtils.color("&7[&4&l!&7] &r&cMật khẩu cấp 2 dùng để khôi phục " +
                            "mật khẩu chính cho nên bạn bắt buộc phải có."))

                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        String rePassword = getInput(response, 1);
                        String password2nd = getInput(response, 2);
                        System.out.println("password: " + password);
                        System.out.println("re-password: " + rePassword);
                        System.out.println("password 2nd: " + password2nd);
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
                        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                        players.computeIfAbsent(player.getUniqueId(), uuid -> {
                            AuthPlayer authPlayer = new AuthPlayer(player);
                            authPlayer.setPassword2nd(password2nd);
                            return authPlayer;
                        });
                        removeInput(player);
                    })
                    .closedOrInvalidResultHandler(response -> {
                        player.kickPlayer(MessageUtils.color("&cĐĂNG KÝ THẤT BẠI, XIN THỬ LẠI"));
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
        return password == null
                || password.length() < getMinPasswordLength()
                || password.length() > getMaxPasswordLength()
                || MessageUtils.checkSpecialCharacters(password);
    }

    private String getInput(CustomFormResponse response, int index) {
        int stringCount = 0;
        String value = null;
        while (response.hasNext()) {
            Object o = response.next();
            if (o == null)
                continue;
            if (o instanceof String) {
                if (stringCount == index)
                    value = String.valueOf(o);
                stringCount++;
            }
        }
        response.reset();
        return value;
    }

    private int getMinPasswordLength() {
        return getAuthmeConfig() == null ? 5 : getAuthmeConfig().getInt("settings.security.minPasswordLength", 5);
    }

    public int getMaxPasswordLength() {
        return getAuthmeConfig() == null ? 30 : getAuthmeConfig().getInt("settings.security.passwordMaxLength", 30);
    }

    private FileConfiguration getAuthmeConfig() {
        File file = new File("plugins/AuthMe/config.yml");
        if (!file.exists())
            return null;
        return YamlConfiguration.loadConfiguration(file);
    }

}
