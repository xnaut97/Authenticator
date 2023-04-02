package com.github.tezvn.authenticator.utils;

import com.github.tezvn.authenticator.utils.time.TimeUtils;
import com.google.common.collect.Maps;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageUtils {

    private static final Map<UUID, Map<String, Long>> cooldowns = Maps.newHashMap();

    public static void sendMessage(Player player, String... msg) {
        sendMessage(player, 0, msg);
    }

    public static void sendMessage(CommandSender sender, String... msg) {
        for (String s : msg) {
            sender.sendMessage(s.replace("&", "§"));
        }
    }

    public static void broadcast(String... messages) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendMessage(player, messages);
        }
    }

    public static void sendMessage(Player player, int cooldown, String... msg) {
        Map<String, Long> map = cooldowns.getOrDefault(player.getUniqueId(), Maps.newHashMap());
        for (String s : msg) {
            long date = map.getOrDefault(s, -1L);
            if (date == -1 || date > System.currentTimeMillis())
                continue;
            player.sendMessage(s.replace("&", "§"));
            map.put(s, TimeUtils.of(System.currentTimeMillis()).add(cooldown).getNewTime());
        }
    }

    public static String color(String str) {
        return str.replace("&", "§");
    }

    public static List<String> color(String... msg) {
        return Arrays.stream(msg).map(MessageUtils::color).collect(Collectors.toList());
    }

    public static List<String> color(List<String> list) {
        return color(list.toArray(new String[0]));
    }

    public static boolean checkSpecialCharacters(String str) {
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }
}
