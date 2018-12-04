# JDACommands
Light command framework for JDA Discord bots using annotation

## How to use

### Creating a command

Create a method and add the annotation `@CommandName`, you can also add the annotation `@CommandAlias` to add alias
```java
@CommandName("test")
@CommandAlias({"testcommand", "commandtest"})
public void testCommand(TextChannel channel) {
    channel.sendMessage("Test command is working").queue();
}
```

If you want to have one command per class you can add the annotations to the class and annotate the method with `@CommandExecutor`
```java
@CommandName("test")
@CommandAlias({"testcommand", "commandtest"})
public class TestCommand {
    
    @CommandExecutor
    public void testCommand(TextChannel channel) {
        channel.sendMessage("Test command is working").queue();
    }
}
```

Arguments can be one of the following types:
* `Message`
* `User`
* `Member`
* `Guild`
* `JDA`
* `String[]` - the command arguments

### Registering commands

Just create a `CommandManager` instance and call `executeCommand` when the bot receive a message that can be a command

```java
    CommandManager commandManager = new CommandManager("!");
    commandManager.register(new TestCommand());

    try {
        new JDABuilder("token").addEventListener(new ListenerAdapter() {

            @Override
               public void onMessageReceived(MessageReceivedEvent event) {
                // Don't execute commands from bots
                if (event.getAuthor().isBot()) {
                    return;
                }

                commandManager.executeCommand(event.getMessage());
            }
        }).build();
    } catch (LoginException e) {
        e.printStackTrace();
    }
```