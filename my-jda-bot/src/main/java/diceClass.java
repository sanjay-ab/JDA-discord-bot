
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.regex.Pattern;

public class diceClass extends ListenerAdapter {
    private MessageChannel channel = null; //channel of the game
    private boolean game; //if the game is active
    private List<User> players = new ArrayList<User>(); //users in the game
    private List<Integer> numberOfDice = new ArrayList<Integer>(); //number of dice that each person has
    private List<Integer> totalDice = new ArrayList<Integer>(); //number of dice with each value

    public diceClass(MessageChannel newChannel, User author){ //constructor
        channel = newChannel;
        totalDice = Arrays.asList(0,0,0,0,0,0);
        players.add(author);
        numberOfDice.add(6);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        User author = event.getAuthor();

        if (content.equalsIgnoreCase("!rules") && channel.getId().equals(event.getChannel().getId())){ //send rules
            channel.sendMessage("Each player takes turns to make progressively higher bets on the total amount of dice with a given value. For example: player 1 can bet that there are 4 dice with a value of " +
                    "3. Then player 2 must bet either a higher number of dice with any dice value e.g. 5 3s or a greater than or equal to number of dice with a higher value e.g. 4 4s or they bet half the number of 1s or \"aces\"(rounded up) e.g. 2 1s. This is since aces are wild in this variant i.e. they can take any value" +
                    "This continues until one player uses their turn to call out the previous bet for being too high" +
                    " in this case the player who made the final bet types !bet followed by their bet e.g. !bet 8 4s. Then if there are more than this number of dice then the player who made" +
                    " the bet wins and the player who called them out loses and vice versa. The loser loses one dice. The game ends when all but one player have lost all their dice.").queue();
        }

        if (content.equalsIgnoreCase("!join") && !game && channel.getId().equals(event.getChannel().getId())){ //players join game
            if (players.contains(author)) {
                channel.sendMessage("You are already part of the game").queue();
            } else {
                players.add(author);
                numberOfDice.add(6);
                channel.sendMessage("Joined").queue();
            }
        }
        if (content.equalsIgnoreCase("!start") && !game && channel.getId().equals(event.getChannel().getId())){ //starting game
            if (players.size() <= 1){
                channel.sendMessage("Not enough people joined to the game, more people are required to start").queue();
            } else {
                game = true;
                StringBuilder nameList = new StringBuilder();
                for (User x:players) {
                    nameList.append(x.getName()).append(" ");
                }
                channel.sendMessage("Order of play is: " + nameList).queue();
                sendDice();
            }

        }
        if (content.toLowerCase().startsWith("!finalbet")&& game && channel.getId().equals(event.getChannel().getId())){ //player who writes out final bet is the player who made the bet. The proceeding player is the one who called them on it.
            finalBet(content.substring(9).trim(),players.indexOf(event.getAuthor()));
        }
        if (content.toLowerCase().startsWith("!final bet")&& game && channel.getId().equals(event.getChannel().getId())){
            finalBet(content.substring(10).trim(),players.indexOf(event.getAuthor()));

        }
        if (content.toLowerCase().startsWith("!bet")&& game && channel.getId().equals(event.getChannel().getId())){
            finalBet(content.substring(4).trim(),players.indexOf(event.getAuthor()));
        }
        if (content.toLowerCase().startsWith("!end")&& channel.getId().equals(event.getChannel().getId())){ //end game and remove listener object
            channel.sendMessage("Ending game").queue();
            Listener.removeListener(channel);
        }
    }
    public void sendDice(){ //send random values to each person i.e. roll dice
        Random random = new Random();
        totalDice = Arrays.asList(0,0,0,0,0,0);
        for (int x = 0; x < numberOfDice.size(); x++){ //loop through each player

            StringBuilder tempMessage = new StringBuilder("Your dice are: ");
            for (int i = 0; i < numberOfDice.get(x); i++){ //loop through each dice
                int temp = random.nextInt(6) + 1;
                tempMessage.append(String.valueOf(temp)).append(" ");
                totalDice.set(temp-1,totalDice.get(temp-1) + 1);
            }
            final String message = tempMessage.toString();
            players.get(x).openPrivateChannel().queue(pChannel -> { //send direct message to player
                pChannel.sendMessage(message).queue();
            });
        }
    }

    public void finalBet(String bet, int index){ //checks the final bet
        String[] bets = bet.split("\\s");
        Pattern pattern = Pattern.compile("[a-zA-Z]"); //checks to see if any letters in message
        StringBuilder temp = new StringBuilder();
        int num; //amount of dice
        int dice; //value of dice
        for (int i=0; i < bets[0].length();i++){ //message formatted like !bet 5 6s. Loop removes any letters.
            if (!pattern.matcher(bets[0].substring(i,i+1)).matches()){
                temp.append(bets[0].charAt(i));
            }
        }
        num = Integer.parseInt(temp.toString());
        temp = new StringBuilder();
        if (bets[1].equalsIgnoreCase("aces")){
            dice = 1;
        } else {
            for (int i = 0; i < bets[1].length(); i++) {
                if (!pattern.matcher(bets[1].substring(i, i + 1)).matches()) {
                    temp.append(bets[1].charAt(i));
                }
            }
            dice = Integer.parseInt(temp.toString());
        }

        if (dice>6 || dice<1 || num<1){ //check that the bet is valid
            channel.sendMessage("Please enter a valid bet").queue();
            return;
        }
        int total; //total number of dice including aces
        boolean Aces; //see if final bet was for aces
        if (dice == 1){
            total = totalDice.get(0);
            Aces = true;
        } else {
            total = totalDice.get(dice - 1) + totalDice.get(0); //total is number of dice plus number of aces
            Aces = false;
        }
        if(total >= num){ //true if their bet was correct else returns false
            if (Aces){
                channel.sendMessage("You Win! There are in fact: " + String.valueOf(totalDice.get(0)) + " Aces").queue(); //tells the real  number of dice
            } else{
                channel.sendMessage("You Win! There are in fact: " + String.valueOf(totalDice.get(dice - 1)) + " " + String.valueOf(dice) + "s and " + String.valueOf(totalDice.get(0)) + " aces for a total of: " + total).queue();
            }
            if (index == players.size()-1){ //call change dice to change the dice of the loser
                changeDice(0); //if index is at the end of the list then the next person is at the start.
            } else {
                changeDice(index + 1);
            }
        } else { //change index to be the person who made the bet since they are the loser.
            if (Aces){
                channel.sendMessage("You Lose! There are in fact: " + String.valueOf(totalDice.get(0)) + " Aces").queue();
            } else{
                channel.sendMessage("You Lose! There are in fact: " + String.valueOf(totalDice.get(dice - 1)) + " " + String.valueOf(dice) + "s and " + String.valueOf(totalDice.get(0)) + " aces for a total of: " + total).queue();
            }
            changeDice(index);
        }
        sendDice(); //call send dice after bet to start next round
    }

    public void changeDice(int loserIndex){ //removes dice of loser and checks if a player has one
        channel.sendMessage(players.get(loserIndex).getName() + " loses a die").queue();

        if (numberOfDice.get(loserIndex) == 1){ //checks whether player only has one dice left. if they do then they are removed from the game
            channel.sendMessage(players.get(loserIndex).getName() + " has no more dice left and is out of the game :(").queue(); //player is out of the game
            if (numberOfDice.size() == 2){ //if there are only two players in the game then the remaining player wins.
                if (loserIndex == 0){
                    loserIndex = 2;
                }
                channel.sendMessage(players.get(loserIndex-1).getName() + " Wins the game with " + numberOfDice.get(loserIndex-1) + " dice left! :)").queue(); //player wins
                Listener.removeListener(channel); //remove listener from channel since game is over
            } else { //if game is not over then remove player from game
                numberOfDice.remove(loserIndex);
                players.remove(loserIndex);
            }

        } else { //if player not eliminated then reduce their number of dice by one.
            numberOfDice.set(loserIndex, numberOfDice.get(loserIndex) - 1);
        }
    }

}
