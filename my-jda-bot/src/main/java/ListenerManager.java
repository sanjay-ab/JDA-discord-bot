import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;

/**
 * ListenerManager class that listens for messages and creates new DiceGameListener objects
 */
public class ListenerManager extends ListenerAdapter {
    /**
     * Dictionary that maps channel IDs to DiceGameListener objects, a channel can 
     * only have one DiceGameListener object at a time.
    */
    public static Dictionary<String, DiceGameListener> diceListeners = new Hashtable<String, DiceGameListener>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.getAuthor().isBot()) return; // so we dont respond to other bots

        Message message = event.getMessage();
        String content = message.getContentRaw();
        String authorId = message.getAuthor().getId();
        MessageChannel channel = event.getChannel();
        JDA api = event.getJDA();  // get bot object

        if (content.toLowerCase().startsWith("!dice")) { 
            // create a new listener for the channel and add it to the bot
            if (diceListeners.get(channel.getId()) == null) {  
                // if channel does not already have a listener then add one
                diceListeners.put(channel.getId(), new DiceGameListener(channel, message.getAuthor()));
                api.addEventListener(diceListeners.get(channel.getId()));
                channel.sendMessage("Starting Liars Dice game. Type !join to join and !start to start.").queue();
            } else {
                channel.sendMessage("Game already started").queue();
            }

        }

    }

    public void onReady(ReadyEvent event){
        System.out.println("The bot is ready!");
    }

    /**
     * Removes the DiceGameListener from the channel
     * @param channel the channel to remove the listener from
     */
    public static void removeListener(MessageChannel channel){
        JDA api = channel.getJDA();
        api.removeEventListener(diceListeners.get(channel.getId()));
        diceListeners.remove(channel.getId());
    }
}
