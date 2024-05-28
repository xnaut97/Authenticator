package com.github.tezvn.authenticator.impl.player;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.PlayerManager;
import com.github.tezvn.authenticator.api.player.handler.DataHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.api.player.input.EmailInput;
import com.github.tezvn.authenticator.api.player.input.InputType;
import com.github.tezvn.authenticator.api.player.input.RegisterInput;
import com.github.tezvn.authenticator.impl.player.handler.DataHandlerImpl;
import com.github.tezvn.authenticator.impl.player.input.AbstractInput;
import com.github.tezvn.authenticator.impl.player.input.LoginInputImpl;
import com.github.tezvn.authenticator.impl.player.input.PasswordUpdateInputImpl;
import com.github.tezvn.authenticator.impl.player.input.RegisterInputImpl;
import com.github.tezvn.authenticator.impl.utils.AuthMeUtils;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import com.github.tezvn.authenticator.impl.utils.item.ItemCreator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.api.v3.AuthMePlayer;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.events.EmailChangedEvent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AuthmeListener extends DataHandlerImpl {

    public static Map<Player, AnvilGUI> anvilGuis = Maps.newHashMap();

    private final AuthMeApi authMeApi = AuthMeApi.getInstance();
    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    public AuthmeListener(PlayerManager playerManager) {
        super(playerManager, Platform.NONE);
    }

    @Override
    public void onExecute(Player player) {

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEmailChanged(EmailChangedEvent event) {
        Player player = event.getPlayer();
        String newEmail = event.getNewEmail();

        EmailInput input = getOrCreate(player, InputType.EMAIL);
        input.setEmail(newEmail);
        event.setCancelled(true);

        Platform platform = getPlatform(player);
        switch (platform) {
            case JAVA_EDITION -> openPassword2ndCheckingGUI(player);
            case BEDROCK_OR_POCKET_EDITION -> openPassword2ndCheckingForm(player);
        }
    }

    private void openPassword2ndCheckingGUI(Player player) {
        AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
        EmailInput input = getOrCreate(player, InputType.EMAIL);
        ItemCreator creator = new ItemCreator(XMaterial.SUNFLOWER.parseItem())
                .setDisplayName(" ")
                .addLore("&bNhập mật khẩu cấp 2 ở trên",
                        "&bSau đó bấm xác nhận ở đây",
                        "&7Số lần thử còn lại: " + (5 - input.getAttempts()));

        AnvilGUI.Builder gui = new AnvilGUI.Builder()
                .allowConcurrentClickHandlerExecution()
                .plugin(getPlugin())
                .title("§lXÁC MINH TÀI KHOẢN")
                .itemLeft(new ItemCreator(XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE.parseItem())
                        .setDisplayName(" ")
                        .build())
                .itemRight(creator.build())
                .itemOutput(new ItemCreator(XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE.parseItem())
                        .setDisplayName("dasdasd")
                        .build());

        gui.onClick((slot, stateSnapshot) -> {
            if(slot != AnvilGUI.Slot.INPUT_RIGHT)
                return Collections.emptyList();

            String text = stateSnapshot.getText();
            // Check if password match
            if (!authPlayer.checkPassword(text)) {
                XSound.ENTITY_BLAZE_HURT.play(player, .75f, -1);
                return input.getAttempts() >= 5
                        // Reach out of attempts, close GUI
                        ? Lists.newArrayList(
                        AnvilGUI.ResponseAction.close(),
                        AnvilGUI.ResponseAction.run(() -> {
                            removeInput(player);
                            anvilGuis.remove(player);
                            MessageUtils.sendMessage(player, "&cĐã vượt quá số lần nhập cho phép, vui lòng thử lại!");
                        }))
                        // Try again
                        : Lists.newArrayList(AnvilGUI.ResponseAction.run(() -> {
                    input.setAttempts(input.getAttempts() + 1);
                    gui.itemRight(creator.setLore(2, "&7Số lần thử còn lại: " + (5 - input.getAttempts())).build()).open(player);
                }));
            }
            return Lists.newArrayList(
                    AnvilGUI.ResponseAction.close(),
                    AnvilGUI.ResponseAction.run(() -> {
                        anvilGuis.remove(player);
                        removeInput(player);
                        PlayerAuth playerAuth = getPlayerAuth(player);
                        if (playerAuth == null) {
                            MessageUtils.sendMessage(player, "&cĐã xảy ra lỗi trong quá trình thay đổi email, vui lòng liên hệ quản trị viên!");
                            return;
                        }
                        playerAuth.setEmail(input.getEmail());
                        boolean update = Objects.requireNonNull(AuthMeUtils.getDataSource()).updateEmail(playerAuth);
                        getPlugin().getLogger().warning("Update email result for player " + player.getName() + ": " + (update ? "success" : "failed"));
                        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                        MessageUtils.sendMessage(player, "&aCập nhật email thành công!");
                    }));
        });

        Bukkit.getScheduler().runTask(getPlugin(), () -> anvilGuis.put(player, gui.open(player)));
    }

    private void openPassword2ndCheckingForm(Player player) {
        AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
        EmailInput input = getOrCreate(player, InputType.EMAIL);
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId()))
            return;
        floodgateApi.getPlayer(player.getUniqueId()).sendForm(CustomForm.builder()
                .title("XÁC MINH EMAIL")
                .label(MessageUtils.color("&7[&4&l!&7] &cDùng mật khẩu cấp 2 để xác minh"))
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
                            return;
                        }
                        input.setAttempts(input.getAttempts() + 1);
                        openPassword2ndCheckingForm(player);
                        XSound.ENTITY_BLAZE_HURT.play(player, .75f, -1);
                        return;
                    }
                    removeInput(player);
                    PlayerAuth playerAuth = getPlayerAuth(player);
                    if (playerAuth == null) {
                        MessageUtils.sendMessage(player, "&cĐã xảy ra lỗi trong quá trình thay đổi email, vui lòng liên hệ quản trị viên!");
                        return;
                    }
                    playerAuth.setEmail(input.getEmail());
                    boolean update = Objects.requireNonNull(AuthMeUtils.getDataSource()).updateEmail(playerAuth);
                    getPlugin().getLogger().warning("Update email result for player " + player.getName() + ": " + (update ? "success" : "failed"));
                    XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
                    MessageUtils.sendMessage(player, "&aCập nhật email thành công!");
                })
                .closedOrInvalidResultHandler(() -> {
                    removeInput(player);
                })
                .build());
    }

    private PlayerAuth getPlayerAuth(Player player) {
        try {
            Field playerCacheField = AuthMeApi.class.getDeclaredField("playerCache");
            playerCacheField.setAccessible(true);
            PlayerCache playerCache = (PlayerCache) playerCacheField.get(AuthMeApi.getInstance());
            PlayerAuth playerAuth = playerCache.getAuth(player.getName());
            if (playerAuth == null) {
                Field dataSourceField = AuthMeApi.class.getDeclaredField("dataSource");
                dataSourceField.setAccessible(true);
                fr.xephi.authme.datasource.DataSource dataSource = (fr.xephi.authme.datasource.DataSource) dataSourceField.get(AuthMeApi.getInstance());
                playerAuth = dataSource.getAuth(player.getName());
            }
            return playerAuth;
        } catch (Exception e) {
            return null;
        }
    }

    private Platform getPlatform(Player player) {
        return player.getUniqueId().toString().startsWith("00000000-")
                ? Platform.BEDROCK_OR_POCKET_EDITION : Platform.JAVA_EDITION;
    }

}
