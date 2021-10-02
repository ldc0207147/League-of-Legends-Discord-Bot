package codes.bronze.tutorial;

import codes.bronze.tutorial.listeners.HelloListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.sql.Statement;
import java.util.Scanner;


public class ldcbot {

    public static JDABuilder builder;
    public static JDA build;
    public static Statement st;

    public static void main(String[] args) throws LoginException, IOException, InterruptedException {
        String token = "您的Discord Token";
        builder = JDABuilder.createDefault(token);

        builder.enableIntents(GatewayIntent.GUILD_MESSAGES);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);

        builder.setActivity(Activity.streaming("www.ldcbot.cf", "https://www.youtube.com/watch?v=zlFjv1_MfVQ"));

        registerListeners();

        build = builder.build();

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String next = scanner.next();
            switch (next) {
                case "gs":
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                    System.out.println("已加入的群數量: " + build.getGuilds().size());
                    break;
                case "gsn":
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                    System.out.println("-------------------------");
                    System.out.println("所有加入的群的名字:");
                    for (Guild guild : build.getGuilds()) {
                        System.out.println(String.format("%s(%s),", guild.getName(), guild.getId()));
                    }
                    System.out.println("-------------------------");
                    break;
            }
        }
    }

    public static void registerListeners() {
        builder.addEventListeners(new HelloListener());
    }
}
