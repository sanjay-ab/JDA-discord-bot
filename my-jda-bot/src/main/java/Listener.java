import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;

public class Listener extends ListenerAdapter {
    public static Dictionary<String, diceClass> diceListeners = new Hashtable<String, diceClass>();
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.getAuthor().isBot()) return; // so we dont respond to other bots

        Message message = event.getMessage();
        String content = message.getContentRaw();
        String authorId = message.getAuthor().getId();
        MessageChannel channel = event.getChannel();
        JDA api = event.getJDA(); //get bot object

        if (content.toLowerCase().startsWith("!dice")){ //if keyword written then create a new listener for the channel and add it to the bot
            if (diceListeners.get(channel.getId()) == null){ //if channel does not already have a listener then add one
                diceListeners.put(channel.getId(),new diceClass(channel,message.getAuthor()));
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

    public static void removeListener(MessageChannel channel){ //method to remove listeners from channels when they are finished.
        JDA api = channel.getJDA();
        api.removeEventListener(diceListeners.get(channel.getId()));
        diceListeners.remove(channel.getId());
    }



}


