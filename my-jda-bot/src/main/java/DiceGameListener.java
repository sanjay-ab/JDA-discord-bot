
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A listener object that is created when a user starts a game of liars dice. It listens for messages
 * and interacts with the users to run the game.
 */
public class DiceGameListener extends ListenerAdapter {
    private MessageChannel channel = null;  // channel of the game
    private boolean gameIsActive;  // if the game is active
    private List<User> players = new ArrayList<User>();  // users in the game
    private List<Integer> numDicePerPerson = new ArrayList<Integer>();  // number of dice that each person has
    private List<Integer> numDiceWithEachVal = new ArrayList<Integer>();  // number of dice with each value - e.g. num of 1s, 2s, etc.

    public DiceGameListener(MessageChannel newChannel, User author) {
        channel = newChannel;
        numDiceWithEachVal = Arrays.asList(0,0,0,0,0,0);
        players.add(author);
        numDicePerPerson.add(6);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw();
        User author = event.getAuthor();

        // send rules
        if (content.equalsIgnoreCase("!rules") && channel.getId().equals(event.getChannel().getId())) {
            channel.sendMessage("Each player starts with 6 dice, these dice are all randomised (i.e., rolled) " +
            "at the start of each round. Each player then takes turns to make progressively higher bets on the " +
            "total amount of dice in the game with a specific value. For example: player 1 can bet that there are " +
            "4 dice with a value of 3 ('4 3s') between all the players. Then player 2 must bet either a higher " + 
            "number of dice with any dice value e.g. 5 2s, 5 3s, or a number of dice greater than or equal to the " +
            "previous number, with a higher value e.g. 4 4s, 4 5s, or they can bet half the number of 1s ('aces') " +
            "(rounded up) e.g. 2 1s. Aces are wild, so they can take any value. So, when betting a number of dice " +
            "with some value (e.g. 3 - i.e., 4 3s), you include the number of dice with that value, plus all the " +
            "aces (1s). The round continues until one player uses their turn to call out the previous bet for being " +
            "too high in this case the player who made the final bet types !bet followed by their bet e.g. !bet 8 4s. " +
            "Then, if there are more than this number of dice, the player who made the bet wins and the player who " +
            "called them out loses and vice versa. The loser loses one dice. The game ends when all but one player " +
            "have lost all their dice.").queue();
        }

        // join game
        if (content.equalsIgnoreCase("!join") && !gameIsActive && channel.getId().equals(event.getChannel().getId())) { 
            if (players.contains(author)) {
                channel.sendMessage("You are already part of the game").queue();
            } else {
                players.add(author);
                numDicePerPerson.add(6);
                channel.sendMessage("Joined").queue();
            }
        }

        // start game
        if (content.equalsIgnoreCase("!start") && channel.getId().equals(event.getChannel().getId())) {
            if (gameIsActive) {
                channel.sendMessage("Game already started").queue();
            } else { 
                if (players.size() <= 1) {
                    channel.sendMessage("Not enough people joined to the game, more people are required to start").queue();
                } else {
                    gameIsActive = true;
                    StringBuilder nameList = new StringBuilder();
                    for (User x:players) {
                        nameList.append(x.getName()).append(" ");
                    }
                    channel.sendMessage("Order of play is: " + nameList).queue();
                    sendDice();  // start round
                }
            }
        }

        if (content.toLowerCase().startsWith("!bet") && gameIsActive && channel.getId().equals(event.getChannel().getId())) {
            finalBet(content.substring(4).trim(), author);
        }

        // end game and remove listener object
        if (content.toLowerCase().startsWith("!end") && channel.getId().equals(event.getChannel().getId())){ 
            channel.sendMessage("Ending game").queue();
            ListenerManager.removeListener(channel);
        }
    }

    /**
     * Sends a random dice roll to each player in the game.
     */
    public void sendDice() { 
        Random random = new Random();
        numDiceWithEachVal = Arrays.asList(0,0,0,0,0,0);
        for (int x = 0; x < numDicePerPerson.size(); x++) {
            // loop through players
            StringBuilder tempMessage = new StringBuilder("Your dice are: ");

            for (int i = 0; i < numDicePerPerson.get(x); i++) {
                // loop through dice
                int temp = random.nextInt(6) + 1;
                tempMessage.append(String.valueOf(temp)).append(" ");
                numDiceWithEachVal.set(temp-1, numDiceWithEachVal.get(temp-1) + 1);
            }
            final String message = tempMessage.toString();
            players.get(x).openPrivateChannel().queue(pChannel -> { 
                // send direct message to player
                pChannel.sendMessage(message).queue();
            });
        }
    }

    /**
     * Checks the final bet and determines the winner and loser of the round.
     * @param bet the final bet, formatted as a string
     * @param player the player who made the final bet
     */
    public void finalBet(String bet, User player) {
        int indexOfPlayer = players.indexOf(player);
        String[] bets = bet.split("\\s");
        int numOfDice; 
        int valOfDice;

        // remove any non-numeric characters from the bet
        StringBuilder temp = new StringBuilder();
        Pattern pattern = Pattern.compile("[a-zA-Z]"); 
        for (int i = 0; i < bets[0].length(); i++) { 
            if (!pattern.matcher(bets[0].substring(i,i+1)).matches()) {
                temp.append(bets[0].charAt(i));
            }
        }

        numOfDice = Integer.parseInt(temp.toString());
        temp = new StringBuilder();
        if (bets[1].equalsIgnoreCase("aces")) {
            valOfDice = 1;
        } else {
            for (int i = 0; i < bets[1].length(); i++) {
                if (!pattern.matcher(bets[1].substring(i, i + 1)).matches()) {
                    temp.append(bets[1].charAt(i));
                }
            }
            valOfDice = Integer.parseInt(temp.toString());
        }

        // ensure bet is valid
        if (valOfDice>6 || valOfDice<1 || numOfDice<1){
            channel.sendMessage("Please enter a valid bet").queue();
            return;
        }

        int numDiceIncludingAces;
        boolean isBetWithAces;
        if (valOfDice == 1) {
            numDiceIncludingAces = numDiceWithEachVal.get(0);
            isBetWithAces = true;
        } else {
            numDiceIncludingAces = numDiceWithEachVal.get(valOfDice - 1) + numDiceWithEachVal.get(0);
            isBetWithAces = false;
        }

        if (numDiceIncludingAces >= numOfDice) {
            // bet was correct
            if (isBetWithAces) {
                channel.sendMessage("You Win! There are in fact: " + String.valueOf(numDiceWithEachVal.get(0)) + " Aces").queue(); 
            } else{
                channel.sendMessage("You Win! There are in fact: " + String.valueOf(numDiceWithEachVal.get(valOfDice - 1)) + " " +
                String.valueOf(valOfDice) + "s and " + String.valueOf(numDiceWithEachVal.get(0)) + 
                " aces for a total of: " + numDiceIncludingAces).queue();
            }

            // remove one die from the loser - player who called out the bet
            if (indexOfPlayer == players.size()-1) { 
                // if betting player is at the end, then the following player is at the start.
                changeDice(0); 
            } else {
                changeDice(indexOfPlayer + 1);
            }

        } else { 
            // bet was incorrect
            if (isBetWithAces){
                channel.sendMessage("You Lose! There are in fact: " + String.valueOf(numDiceWithEachVal.get(0)) + " Aces").queue();
            } else{
                channel.sendMessage("You Lose! There are in fact: " + String.valueOf(numDiceWithEachVal.get(valOfDice - 1)) +
                " " + String.valueOf(valOfDice) + "s and " + String.valueOf(numDiceWithEachVal.get(0)) +
                " aces for a total of: " + numDiceIncludingAces).queue();
            }

            // remove one die from the loser - player who made the bet
            changeDice(indexOfPlayer);
        }

        sendDice();  // start next round
    }

    /**
     * Removes one die from a player and checks if they are eliminated from the game.
     * @param loserIndex index of the player in the players list variable who lost
     */
    public void changeDice(int loserIndex) {
        channel.sendMessage(players.get(loserIndex).getName() + " loses a die").queue();

        if (numDicePerPerson.get(loserIndex) == 1) {
            // player is out of the game
            channel.sendMessage(players.get(loserIndex).getName() + " has no more dice left and is out of the game :(").queue();
            if (numDicePerPerson.size() == 2){ 
                // if there are only two players left in the game then the remaining player wins.
                int winnerIndex = 0;
                if (loserIndex == 0){
                    winnerIndex = 1;
                } 
                channel.sendMessage(players.get(winnerIndex).getName() + " Wins the game with " + numDicePerPerson.get(winnerIndex) +
                " dice left! :)").queue(); 
                
                // end game and remove listener object
                ListenerManager.removeListener(channel); 

            } else {
                // remove player from the game
                numDicePerPerson.remove(loserIndex);
                players.remove(loserIndex);
            }

        } else { 
            // remove one die from the player
            numDicePerPerson.set(loserIndex, numDicePerPerson.get(loserIndex) - 1);
        }
    }
}
