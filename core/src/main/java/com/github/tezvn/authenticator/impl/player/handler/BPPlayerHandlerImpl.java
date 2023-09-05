package com.github.tezvn.authenticator.impl.player.handler;

import com.cryptomorin.xseries.XSound;
import com.github.tezvn.authenticator.api.events.PlayerSignInEvent;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.handler.BPPlayerHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.api.player.input.*;
import com.github.tezvn.authenticator.impl.player.AuthPlayerImpl;
import com.github.tezvn.authenticator.impl.player.AuthenticatorEvents;
import com.github.tezvn.authenticator.impl.player.PlayerManagerImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

public class BPPlayerHandlerImpl extends DataHandlerImpl implements BPPlayerHandler {

    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    public BPPlayerHandlerImpl(PlayerManagerImpl playerManager) {
        super(playerManager, Platform.BEDROCK_OR_POCKET_EDITION);
        playerManager.registerHandler(this);
    }

    @Override
    public void onExecute(Player player) {
        if (getAuthMeApi().isRegistered(player.getName()))
            openLoginSelectionForm(player, 1);
        else
            openRegisterForm(player, 1);
    }

    @Override
    public void openPasswordRecoveryGUI(Player player) {
        if (floodgateApi.isFloodgatePlayer(player.getUniqueId()))
            openPassword2ndCheckingForm(player);
    }

    private void openLoginSelectionForm(Player player, int delay) {
        runDelayed(delay, () -> {
            if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                return;
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                    .button("ĐĂNG NHẬP")
                    .optionalButton("KHÔI PHỤC MẬT KHẨU", getPlayerManager().getPlayer(player) != null)
                    .button("THOÁT GAME")
                    .validResultHandler(response -> {
                        int id = response.clickedButtonId();
                        switch (id) {
                            case 0:
                                if (getAuthMeApi().isAuthenticated(player)) {
                                    getAuthMeApi().forceLogin(player);
                                    return;
                                }
                                openLoginForm(player);
                                break;
                            case 1:
                                openPassword2ndCheckingForm(player);
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

    private void openPassword2ndSetupForm(Player player) {
        RegisterInput input = getOrCreate(player, InputType.REGISTER);
        if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                .title("THIẾT LẬP MẬT KHẨU CẤP 2")
                .label(MessageUtils.color("&cVì lý do bảo mật nên mỗi người chơi đều phải" +
                        " có mật khẩu cấp 2 để khôi phục lại mật khẩu chính, nhận thấy bạn chưa có" +
                        " mật khẩu cấp 2 nên yêu cầu bạn cần thiết lập ngay."))
                //Main password
                .input("Mật khẩu cấp 2", "Nhập mật khẩu cấp 2...",
                        input.getPassword() == null ? "" : input.getPassword())
                .optionalLabel(MessageUtils.color(input.getPassword() == null ? ""
                                : input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                : input.getPassword().length() < getMinPasswordLength()
                                ? "&c✘ Mật khẩu không được ngắn hơn &6" + getMinPasswordLength() + " &cký tự"
                                : input.getPassword().length() > getMaxPasswordLength()
                                ? "&c✘ Mật khẩu không được dài quá &6" + getMaxPasswordLength() + " &cký tự"
                                : MessageUtils.checkSpecialCharacters(input.getPassword())
                                ? "&c✘ Mật khẩu không được chứa ký tự đặc biệt"
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

                .validResultHandler(response -> {
                    String password = getInput(response, 0);
                    String rePassword = getInput(response, 1);
                    input.setPassword(password);
                    input.setRetypePassword(rePassword);
                    if (notValid(password) || !input.isPasswordMatch()) {
                        openPassword2ndSetupForm(player);
                        return;
                    }
                    XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
//                            openLoginSelectionForm(player, 0);
                    MessageUtils.sendTitle(player, "&a&lTHIẾT LẬP THÀNH CÔNG");
                    removeInput(player);
                    getPlayerManager().getPlayers().computeIfAbsent(player.getUniqueId(), uuid -> {
                        AuthPlayerImpl authPlayer = new AuthPlayerImpl(player);
                        authPlayer.setPassword2nd(password);
                        ((PlayerManagerImpl) getPlayerManager()).saveToDatabase(authPlayer);
                        return authPlayer;
                    });
                })
                .closedOrInvalidResultHandler(() -> {
                    openPassword2ndSetupForm(player);
                })
                .build());
    }

    private void openLoginForm(Player player) {
        LoginInput input = getOrCreate(player, InputType.LOGIN);
        if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                .title("ĐĂNG NHẬP")
                .input("Mật khẩu", "Nhập mật khẩu...",
                        input.getPassword() == null ? "" : input.getPassword())
                .optionalLabel(MessageUtils.color(input.getPassword() == null ? "" :
                                input.getPassword().isEmpty() ? "&cVui lòng nhập mật khẩu"
                                        : !getAuthMeApi().checkPassword(player.getName(), input.getPassword())
                                        ? "&cSai mật khẩu, bạn còn &6" + (5 - input.getAttempts()) + " &clần thử lại."
                                        : ""),
                        //Condition
                        input.getPassword() != null && (!getAuthMeApi().checkPassword(player.getName(), input.getPassword())))
                .validResultHandler(response -> {
                    String password = getInput(response, 0);
                    input.setPassword(password);
                    PlayerSignInEvent event = handleLoginEvent(player, input);
                    if(event.isCancelled()) {
                        openLoginForm(player);
                        return;
                    }
                    if (!getAuthMeApi().checkPassword(player.getName(), password)) {
                        int attempts = input.getAttempts();
                        if (attempts >= 5) {
                            removeInput(player);
                            openAskingForm(player);
                            return;
                        }
                        input.setAttempts(input.getAttempts() + 1);
                        openLoginForm(player);
                        XSound.ENTITY_BLAZE_DEATH.play(player, .75f, -1);
                        return;
                    }
                    getAuthMeApi().forceLogin(player);
                    player.sendTitle(MessageUtils.color("ĐĂNG NHẬP THÀNH CÔNG"), "");
                    MessageUtils.sendTitle(player,  "&a&lĐĂNG NHẬP THÀNH CÔNG");
                    XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                    removeInput(player);
                    AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
                    if (authPlayer == null)
                        openPassword2ndSetupForm(player);
                })
                .closedOrInvalidResultHandler(response -> {
                    removeInput(player);
                    openLoginSelectionForm(player, 0);
                })
                .build());
    }

    private void openAskingForm(Player player) {
        if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                .title("ĐĂNG NHẬP")
                .content("Bạn đã nhập sai mật khẩu quá nhiều lần, có muốn khôi phục lại mật khẩu không?")
                .button(MessageUtils.color("&cĐỒNG Ý"))
                .button(MessageUtils.color("&cKHÔNG, TÔI MUỐN THỬ LẠI"))
                .validResultHandler(response -> {
                    int id = response.clickedButtonId();
                    switch (id) {
                        case 0 -> openPassword2ndCheckingForm(player);
                        case 1 -> openLoginForm(player);
                    }
                })
                .closedOrInvalidResultHandler(() -> openLoginForm(player))
                .build());
    }

    private void openPassword2ndCheckingForm(Player player) {
        AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
        RegisterInput input = getOrCreate(player, InputType.REGISTER);
        if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;
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
                        openPassword2ndCheckingForm(player);
                        XSound.ENTITY_BLAZE_DEATH.play(player, .75f, -1);
                        return;
                    }
                    removeInput(player);
                    openResetPasswordForm(player);
                })
                .closedOrInvalidResultHandler(() -> {
                    removeInput(player);
                    if (!getAuthMeApi().isAuthenticated(player))
                        openLoginForm(player);
                })
                .build());
    }

    private void openResetPasswordForm(Player player) {
        runDelayed(0, () -> {
            PasswordUpdateInput input = getOrCreate(player, InputType.PASSWORD_UPDATE);
            if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                return;
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
                                            : !input.getPassword().equals(input.getRetypePassword()) ? "&cMật khẩu không trùng khớp"
                                            : ""),
                            //Condition
                            input.getRetypePassword() != null && (input.getRetypePassword().isEmpty()
                                    || !input.isPasswordMatch()))
                    .validResultHandler(response -> {
                        String password = getInput(response, 0);
                        String rePassword = getInput(response, 1);
                        input.setPassword(password);
                        input.setRetypePassword(rePassword);
                        AuthenticatorEvents.handlePasswordUpdateEvent(player, input);
                        if (notValid(password)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        if (rePassword == null || rePassword.isEmpty() || !password.equals(rePassword)) {
                            openRegisterForm(player, 0);
                            return;
                        }
                        getAuthMeApi().changePassword(player.getName(), password);
                        removeInput(player);
                        player.kickPlayer(MessageUtils.color("&a&lKHÔI PHỤC MẬT KHẨU THÀNH CÔNG, VUI LÒNG ĐĂNG NHẬP LẠI."));
                    })
                    .closedOrInvalidResultHandler(response -> {
                        if (!getAuthMeApi().isAuthenticated(player))
                            openResetPasswordForm(player);
                    })
                    .build());
        });
    }

    private void openResetPasswordResult(Player player) {
        runDelayed(0, () -> {
            if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                return;
            floodgateApi.getPlayer(player.getUniqueId()).sendForm(SimpleForm.builder()
                    .title("KHÔI PHỤC MẬT KHẨU")
                    .content(MessageUtils.color("&aĐANG XỬ LÝ YÊU CẦU CỦA BẠN . . ."))

                    .build());

            runDelayed(3, () -> {
                player.kickPlayer("&aKHÔI PHỤC MẬT KHẨU THÀNH CÔNG, VUI LÒNG ĐĂNG NHẬP LẠI");
            });
        });
    }

    private void openRegisterForm(Player player, int delay) {
        runDelayed(delay, () -> {
            RegisterInput input = getOrCreate(player, InputType.REGISTER);
            if(!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
                return;
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
                                    : MessageUtils.checkSpecialCharacters(input.getPassword2nd()) ?
                                    "&c✘ Mật khẩu không được chứa ký tự đặc biệt"
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
                        input.setPassword(password);
                        input.setRetypePassword(rePassword);
                        input.setPassword2nd(password2nd);
                        //Checking state
                        AuthenticatorEvents.handleRegisterEvent(player, input);
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
                        getAuthMeApi().registerPlayer(player.getName(), password);
                        player.sendTitle(MessageUtils.color("&a&lĐĂNG KÝ THÀNH CÔNG"), MessageUtils.color("&7CHÚC BẠN CHƠI GAME VUI VẺ"));
                        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                        getPlayerManager().getPlayers().computeIfAbsent(player.getUniqueId(), uuid -> {
                            AuthPlayer authPlayer = new AuthPlayerImpl(player);
                            authPlayer.setPassword2nd(password2nd);
                            ((PlayerManagerImpl) getPlayerManager()).saveToDatabase(authPlayer);
                            return authPlayer;
                        });
                        removeInput(player);
                    })
                    .closedOrInvalidResultHandler(response -> {
                        removeInput(player);
                        player.kickPlayer(MessageUtils.color("&cĐĂNG KÝ THẤT BẠI, XIN THỬ LẠI"));
                    })
                    .build());
        });
    }

    private PlayerSignInEvent handleLoginEvent(Player player, LoginInput input) {
        PlayerSignInEvent event = new PlayerSignInEvent(player, input);
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(event);
            }
        }.runTask(((PlayerManagerImpl) getPlayerManager()).getPlugin());
        return event;
    }

}
