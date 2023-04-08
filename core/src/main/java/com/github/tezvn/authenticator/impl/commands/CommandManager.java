package com.github.tezvn.authenticator.impl.commands;

import com.github.tezvn.authenticator.api.AuthenticatorPlugin;
import com.github.tezvn.authenticator.impl.AuthenticatorPluginImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public class CommandManager {

    private final Map<String, AbstractCommand> commands = Maps.newHashMap();
    private final AuthenticatorPlugin plugin;

    public CommandManager(AuthenticatorPluginImpl plugin) {
        this.plugin = plugin;
        registerCommands();
    }

    public void register(AbstractCommand command) {
        this.commands.put(command.getName(), command);
    }

    public AuthenticatorPlugin getPlugin() {
        return plugin;
    }

    public void registerCommands() {
        this.commands.values().forEach(AbstractCommand::register);
    }

    public void unregisterCommands() {
        this.commands.values().forEach(AbstractCommand::unregister);
    }

}
