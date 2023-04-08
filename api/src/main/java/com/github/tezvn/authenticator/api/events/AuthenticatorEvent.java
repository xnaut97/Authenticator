package com.github.tezvn.authenticator.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuthenticatorEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
