import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class mainBotClass {
    public static void main(String[] args) throws Exception {
        String token = "/* Token */"; //bot token, replace with your own token.
        JDA api = JDABuilder.createDefault(token).build(); //create bot object and login
        api.addEventListener(new Listener()); //add an event listener
    }
}

