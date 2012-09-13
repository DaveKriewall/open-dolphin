package com.canoo.dolphin.core.server.comm;

import com.canoo.dolphin.core.comm.Command;
import groovy.lang.Closure;

import java.util.*;

// todo: think about inlining this into receiver and use get/setProperty to ease registration
public class ActionRegistry {
    // todo: think about proper sizing and synchronization needs
    private final Map<String, List<CommandHandler<? super Command>>> ACTIONS = new HashMap<String, List<CommandHandler<? super Command>>>();

    public Map<String, List<CommandHandler<? super Command>>> getActions() {
        return Collections.unmodifiableMap(ACTIONS);
    }

    public void register(String commandId, Closure serverCommand) {
        getActionsFor(commandId).add(new CommandHandlerClosureAdapter(serverCommand));
    }

    public void register(Class commandClass, Closure serverCommand) {
        getActionsFor(Command.idFor(commandClass)).add(new CommandHandlerClosureAdapter(serverCommand));
    }

    public <T extends Command> void register(String commandId, CommandHandler<T> serverCommand) {
        getActionsFor(commandId).add((CommandHandler<? super Command>) serverCommand);
    }

    public <T extends Command>  void register(Class commandClass, CommandHandler<T> serverCommand) {
        getActionsFor(Command.idFor(commandClass)).add((CommandHandler<? super Command>) serverCommand);
    }

    public List<CommandHandler<? super Command>> getAt(String commandId) {
        return getActionsFor(commandId);
    }

    public <T extends Command> void unregister(String commandId, Closure serverCommand) {
        List<CommandHandler<? super Command>> commandList = getActionsFor(commandId);
        commandList.remove(new CommandHandlerClosureAdapter(serverCommand));
    }

    public void unregister(Class commandClass, Closure serverCommand) {
        unregister(Command.idFor(commandClass), new CommandHandlerClosureAdapter(serverCommand));
    }

    public <T extends Command>  void unregister(String commandId, CommandHandler<T> serverCommand) {
        List<CommandHandler<? super Command>> commandList = getActionsFor(commandId);
        commandList.remove(serverCommand);
    }

    public <T extends Command> void unregister(Class commandClass, CommandHandler<T> serverCommand) {
        unregister(Command.idFor(commandClass), serverCommand);
    }

    private List<CommandHandler<? super Command>> getActionsFor(String commandName) {
        List<CommandHandler<? super Command>> actions = ACTIONS.get(commandName);
        if (actions == null) {
            actions = new ArrayList<CommandHandler<? super Command>>();
            ACTIONS.put(commandName, actions);
        }

        return actions;
    }
}
