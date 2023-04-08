package com.github.tezvn.authenticator.api.events;

import com.google.common.collect.Sets;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;

public class PlayerPasswordCreateEvent extends AuthenticatorEvent {

    private final Player player;

    private String password;

    private final Set<RestrictionType> restrictions = Sets.newHashSet(
            RestrictionType.MIN_LENGTH,
            RestrictionType.MAX_LENGTH,
            RestrictionType.SPECIAL_CHARACTER);

    public PlayerPasswordCreateEvent(Player player, String password) {
        this.player = player;
        this.password = password;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<RestrictionType> getRestrictions() {
        return Collections.unmodifiableSet(this.restrictions);
    }

    public boolean hasRestriction(RestrictionType type) {
        return this.restrictions.stream().anyMatch(r -> r == type);
    }

    public void addRestriction(RestrictionType type) {
        this.restrictions.add(type);
    }

    public void removeRestriction(RestrictionType type) {
        this.restrictions.removeIf(r -> r == type);
    }

    public void clearRestrictions() {
        this.restrictions.clear();
    }

    public enum RestrictionType {
        MIN_LENGTH,
        MAX_LENGTH,
        SPECIAL_CHARACTER,

    }

}
