package com.github.tezvn.authenticator.impl.player.handler;

import com.github.tezvn.authenticator.api.player.AuthPlayer;
import com.github.tezvn.authenticator.api.player.handler.JavaPlayerHandler;
import com.github.tezvn.authenticator.api.player.handler.Platform;
import com.github.tezvn.authenticator.impl.player.PlayerManagerImpl;
import com.github.tezvn.authenticator.impl.utils.MessageUtils;
import fr.xephi.authme.events.AuthMeAsyncPreRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.geysermc.floodgate.api.FloodgateApi;

public class JavaPlayerHandlerImpl extends DataHandlerImpl implements JavaPlayerHandler {

    public JavaPlayerHandlerImpl(PlayerManagerImpl playerManager) {
        super(playerManager, Platform.JAVA_EDITION);
        playerManager.registerHandler(this);
    }

    @Override
    public void onExecute(Player player) {
    }

    @EventHandler
    public void onLoginSuccess(LoginEvent event) {
        Player player = event.getPlayer();
        AuthPlayer authPlayer = getPlayerManager().getPlayer(player);
        if (authPlayer == null) {
            if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                MessageUtils.sendDelayMessage(player, 1, "&cVì lý do bảo mật nên mỗi người chơi đều phải" +
                                " có mật khẩu cấp 2 để khôi phục lại mật khẩu chính, nhận thấy bạn chưa có" +
                                " mật khẩu cấp 2 nên yêu cầu bạn cần thiết lập ngay.",
                        "&7[&4&l!&7] &cMật khẩu cấp 2 chỉ tạo một lần, không thể thay đổi, xin hãy ghi nhớ thật kỹ",
                        "&cCú pháp lệnh: &6/taomatkhaucap2 [mật khẩu] [nhập lại mật khẩu]");
                MessageUtils.sendRepeatedMessage(player, 1, 15, true,
                        p -> getPlayerManager().getPlayers().containsKey(player.getUniqueId()),
                        "&cLệnh tạo mật khẩu cấp 2: &6/taomatkhaucap2 [mật khẩu] [nhập lại mật khẩu]");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (getInput(event.getPlayer()) != null)
            removeInput(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        event.setCancelled(shouldCancelEvent(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(shouldCancelEvent(event.getPlayer()));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player)
            event.setCancelled(shouldCancelEvent((Player) event.getEntity()));
    }

    @EventHandler
    public void onBlockDamage(EntityDamageByBlockEvent event) {
        if (event.getEntity() instanceof Player)
            event.setCancelled(shouldCancelEvent((Player) event.getEntity()));
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        event.setCancelled(shouldCancelEvent(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        event.setCancelled(shouldCancelEvent((Player) event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        event.setCancelled(shouldCancelEvent(event.getPlayer()));
    }

    @EventHandler
    public void onCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().startsWith("/taomatkhaucap2"))
            event.setCancelled(shouldCancelEvent(event.getPlayer()));
    }

    private boolean shouldCancelEvent(Player player) {
        return !FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())
                && getPlayerManager().getPlayer(player) == null;
    }

//    private void openPassword2ndSetup(Player player, String input, int error) {
//        AnvilMenu builder = new AnvilMenu(getPlayerManager().getPlugin())
//                .setCloseable(false)
//                .onClose(whoClose -> openPassword2ndSetup(player, input, error))
//                .setTitle("THIẾT LẬP MẬT KHẨU CẤP 2");
//
//        if (error != -1 && input != null) {
//            ItemCreator creator = new ItemCreator(Objects.requireNonNull(XMaterial.BARRIER.parseItem()));
//            switch (error) {
//                case 0:
//                    creator.setDisplayName("&c&lCHƯA NHẬP THÔNG TIN")
//                            .addLore("&7Vui lòng nhập mật khẩu");
//                    break;
//                case 1:
//                    creator.setDisplayName("&c&lMẬT KHẨU QUÁ NGẮN")
//                            .addLore("&7Hiện tại: b" + input.length(),
//                                    "&7Tối thiểu: &6" + getMinPasswordLength());
//                    break;
//                case 2:
//                    creator.setDisplayName("&c&lMẬT KHẨU QUÁ DÀI")
//                            .addLore("&7Hiện tại: b" + input.length(),
//                                    "&7Tối đa: &6" + getMaxPasswordLength());
//                    break;
//                case 3:
//                    creator.setDisplayName("&c&lMẬT KHẨU KHÔNG HỢP LỆ")
//                            .addLore("&7Mật khẩu không được chứa",
//                                    "các ký tự đặc biệt");
//                    break;
//            }
//            builder.setItem(AnvilMenu.SlotType.OUTPUT, creator.build())
//                    .onComplete(null);
//        }
//        runDelayed(error == -1 ? 0 : 5, () -> {
//            builder.setItem(AnvilMenu.SlotType.OUTPUT, new ItemCreator(Objects.requireNonNull(XMaterial.PAPER.parseItem()))
//                            .setDisplayName("&a&lHOÀN TẤT")
//                            .addLore("&7Nhấn để thiết lập mật khẩu")
//                            .build())
//                    .onComplete(completion -> {
//                        String text = completion.getText();
//                        return AnvilMenu.Response.run(() -> {
//                            if (notValid(text)) {
//                                XSound.ENTITY_BLAZE_DEATH.play(player, .75f, -1f);
//                                if (text == null || text.isEmpty()) {
//                                    openPassword2ndSetup(player, "", 0);
//                                    return;
//                                }
//                                if (text.length() < getMinPasswordLength()) {
//                                    openPassword2ndSetup(player, input, 1);
//                                    return;
//                                }
//                                if (text.length() > getMaxPasswordLength()) {
//                                    openPassword2ndSetup(player, input, 2);
//                                    return;
//                                }
//                                if (MessageUtils.checkSpecialCharacters(text)) {
//                                    openPassword2ndSetup(player, input, 3);
//                                    return;
//                                }
//                            }
//                            player.closeInventory();
//                            new VersionMatcher().match().setActiveContainerDefault(player);
//                            XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
//                            MessageUtils.sendTitle(player, "&a&lTHIẾT LẬP THÀNH CÔNG",
//                                    "&7Bạn có thể đăng nhập vào game");
//                            getPlayerManager().getPlayers().computeIfAbsent(player.getUniqueId(), uuid -> {
//                                AuthPlayer authPlayer = new AuthPlayer(player);
//                                authPlayer.setPassword2nd(text);
//                                return authPlayer;
//                            });
//                            openLoginGUI(player, new LoginInput(player));
//                        });
//                    });
//            openPassword2ndSetup(player, input, -1);
//        });
//        runDelayed(0, () -> builder.open(player));
//    }
//
//    private void openLoginGUI(Player player, LoginInput input) {
//        
//        AnvilMenu builder = new AnvilMenu(getPlayerManager().getPlugin())
//                .onClose(p -> player.kickPlayer(MessageUtils.color("&c&lBẠN ĐÃ ĐĂNG XUẤT")))
//                .setTitle("ĐĂNG NHẬP" + (4 - input.getAttempts() > 3 ? "" : " (" + (4 - input.getAttempts()) + ")"))
//                .setItem(AnvilMenu.SlotType.LEFT, new ItemCreator(Objects.requireNonNull(XMaterial.OAK_DOOR.parseItem()))
//                        .setDisplayName("&c&lTHOÁT GAME")
//                        .addLore("&7Nhấn vào để thoát game",
//                                "&7Hoặc bấm nút '&6Esc&7'")
//                        .build())
//                .setItem(AnvilMenu.SlotType.RIGHT, new ItemCreator(Objects.requireNonNull(XMaterial.PLAYER_HEAD.parseItem()))
//                        .setDisplayName("&6&lQUÊN MẬT KHẨU?")
//                        .addLore("&7Nhấn vào để khôi phục mật khẩu.")
//                        .build())
//                .onLeftItemClick(p -> {
//
//                });
//        if(input.getAttempts() > 0) {
//            builder.setItem(AnvilMenu.SlotType.OUTPUT, new ItemCreator(Objects.requireNonNull(XMaterial.BARRIER.parseItem()))
//                            .setDisplayName("&c&lSAI MẬT KHẨU").build())
//                    .onComplete(null);
//        }
//        runDelayed(input.getAttempts() > 0 ? 5 : 0, () -> {
//            builder.setItem(AnvilMenu.SlotType.OUTPUT, new ItemCreator(Objects.requireNonNull(XMaterial.PAPER.parseItem()))
//                            .setDisplayName("&a&lĐĂNG NHẬP")
//                            .addLore("&7Nhấn để vào game")
//                            .build())
//                    .onComplete(completion -> {
//                        String password = completion.getText();
//                        input.setPassword(password);
//                        return AnvilMenu.Response.run(() -> {
//                            if (!getAuthMeApi().checkPassword(player.getName(), password)) {
//                                if (input.getAttempts() >= 3) {
//                                    player.kickPlayer(MessageUtils.color("&c&lĐĂNG NHẬP KHÔNG THÀNH CÔNG"));
//                                    return;
//                                }
//                                input.setAttempts(input.getAttempts() + 1);
//                                openLoginGUI(player, input);
//                                return;
//                            }
//                            player.closeInventory();
//                            new VersionMatcher().match().setActiveContainerDefault(player);
//                            MessageUtils.sendTitle(player, "&a&lĐĂNG NHẬP THÀNH CÔNG");
//                            removeInput(player);
//                        });
//                    });
//            openLoginGUI(player, input);
//        });
//
//        builder.open(player);
//    }
//
//    private void openPassword2ndChecking(Player player, ForgotPasswordInput input) {
//        AnvilGUI.Builder builder = new AnvilGUI.Builder()
//                .preventClose()
//                .plugin(getPlayerManager().getPlugin())
//                .title("KHÔI PHỤC MẬT KHẨU");
//
//        builder.open(player);
//    }

}
