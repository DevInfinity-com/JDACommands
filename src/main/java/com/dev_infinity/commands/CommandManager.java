package com.dev_infinity.commands;

import com.dev_infinity.commands.annotations.CommandAlias;
import com.dev_infinity.commands.annotations.CommandExecutor;
import com.dev_infinity.commands.annotations.CommandName;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.Checks;
import net.dv8tion.jda.core.utils.JDALogger;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

/**
 * @author MrMicky
 */
public class CommandManager {

    private static final Logger LOGGER = JDALogger.getLog("JDACommands");
    private static final Map<Class<?>, Function<Message, ?>> ARGUMENTS_TYPE = new HashMap<>();

    private Map<String, Command> commands = new HashMap<>();
    private Map<String, Command> commandAlias = new HashMap<>();

    private String commandPrefix;

    static {
        registerArgumentType(Message.class, m -> m);
        registerArgumentType(User.class, Message::getAuthor);
        registerArgumentType(Member.class, Message::getMember);
        registerArgumentType(TextChannel.class, Message::getTextChannel);
        registerArgumentType(Guild.class, Message::getGuild);
        registerArgumentType(JDA.class, Message::getJDA);
        registerArgumentType(String[].class, m -> new String[0]);
    }

    public CommandManager(String commandTag) {
        this.commandPrefix = commandTag;
    }

    public void registerAll(Object... objects) {
        for (Object o : objects) {
            register(o);
        }
    }

    public void register(Object o) {
        Class<?> clazz = o.getClass();

        if (clazz.isAnnotationPresent(CommandName.class)) {
            String name = clazz.getAnnotation(CommandName.class).value();

            Method[] methods = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.isAnnotationPresent(CommandExecutor.class))
                    .toArray(Method[]::new);

            Checks.check(methods.length > 0, "No CommandExecutor found for command " + name);
            Checks.check(methods.length < 2, "More than one CommandExecutor found for command " + name);

            String[] alias = new String[0];

            if (clazz.isAnnotationPresent(CommandAlias.class)) {
                alias = clazz.getAnnotation(CommandAlias.class).value();
            }

            checkArguments(methods[0]);
            register(new Command(name, alias, methods[0], o));
        } else {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(CommandName.class)) {
                    String name = method.getAnnotation(CommandName.class).value();

                    String[] alias = new String[0];

                    if (method.isAnnotationPresent(CommandAlias.class)) {
                        alias = method.getAnnotation(CommandAlias.class).value();
                    }

                    checkArguments(method);
                    register(new Command(name, alias, method, o));
                }
            }
        }
    }

    private void checkArguments(Method method) {
        for (Parameter parameter : method.getParameters()) {
            Class<?> type = parameter.getType();

            Checks.check(ARGUMENTS_TYPE.get(type) != null, "%s is not a valid argument type", type.getSimpleName());
        }
    }

    private void register(Command command) {
        commands.put(command.getName().toLowerCase(), command);

        for (String alias : command.getAlias()) {
            commandAlias.put(alias.toLowerCase(), command);
        }
    }

    public void unregister(String commandName) {
        Command command = commands.get(commandName);

        if (command != null) {
            unregister(command);
        }
    }

    public void unregister(Command command) {
        Checks.notNull(command, "command");

        commands.remove(command.getName());

        for (String alias : command.getAlias()) {
            String s = alias.toLowerCase();

            while (commands.get(s) != null) {
                commands.remove(s);
            }
        }
    }

    public boolean executeCommand(Message message) {
        Checks.notNull(message, "message");

        if (!message.getContentRaw().startsWith(commandPrefix)) {
            return false;
        }

        String content = message.getContentRaw().substring(commandPrefix.length());
        String[] split = content.split(" ");

        if (content.isEmpty() || split.length == 0 || split[0].isEmpty()) {
            return false;
        }

        String commandName = split[0].toLowerCase();
        Command command = commands.get(commandName);

        if (command == null) {
            command = commandAlias.get(commandName);

            if (command == null) {
                return false;
            }
        }

        LOGGER.info(message.getAuthor().getName() + " executed command " + message.getContentRaw());

        String[] args = Arrays.copyOfRange(split, 1, split.length);

        Object[] parameters = Arrays.stream(command.getMethod().getParameters())
                .map(p -> p.getType() == String[].class ? args : ARGUMENTS_TYPE.get(p.getType()).apply(message))
                .toArray(Object[]::new);

        try {
            command.getMethod().invoke(command.getObject(), parameters);
        } catch (Exception e) {
            LOGGER.error("Error while dispatching command " + command.getName(), e);
        }

        return true;
    }

    public Collection<Command> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    private static <T> void registerArgumentType(Class<T> clazz, Function<Message, T> function) {
        ARGUMENTS_TYPE.put(clazz, function);
    }
}
