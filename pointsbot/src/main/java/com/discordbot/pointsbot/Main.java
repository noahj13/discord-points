package com.discordbot.pointsbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.User;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Map<User,Integer> pointTotals = new HashMap<>();
        // Insert your bot's token here
        Scanner file = new Scanner(System.in);
        try {
            file = new Scanner(new File("pointsbot/Resources/token.cred"));
        }catch (IOException e){
            System.out.println("cannot find token");
            System.exit(1);
        }
        String token = file.next();

        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

        // Add a listener which answers with "Pong!" if someone writes "!ping"
        api.addMessageCreateListener(event -> {
            String message = event.getMessageContent();
            if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!give")){
                for(User user: event.getMessage().getMentionedUsers()){
                    if(pointTotals.get(user) != null)
                        pointTotals.put(user,pointTotals.get(user)+1);
                    else
                        pointTotals.put(user,1);
                }
            }
            else if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!take")){
                for(User user: event.getMessage().getMentionedUsers()){
                    pointTotals.put(user,pointTotals.get(user)+1);
                }
            }
            else if (message.length() >= 7 && message.substring(0,7).equalsIgnoreCase("!points")){
                String ret = "";
                if (event.getMessage().getMentionedUsers() == null){
                    ret += event.getMessageAuthor().getDiscriminatedName();
                    if(pointTotals.get(event.getMessageAuthor().asUser()) == null){
                        ret += ": 0";
                    }
                    else
                        ret += ": " + pointTotals.get(event.getMessageAuthor().asUser());
                    event.getChannel().sendMessage(ret);
                }
                else {
                    for (User user : event.getMessage().getMentionedUsers()) {
                        event.getChannel().sendMessage(user.getNicknameMentionTag() + ": " + pointTotals.get(user));
                    }
                }

            }
        });

        // Print the invite url of your bot
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
    }

}