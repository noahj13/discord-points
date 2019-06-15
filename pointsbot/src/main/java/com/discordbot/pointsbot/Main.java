package com.discordbot.pointsbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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

        try {
            file = new Scanner(new File("pointsbot/Resources/points.txt"));
            while(file.hasNext()) {
                try {
                    User user = api.getUserById(file.next()).get();
                    int points = file.nextInt();
                    pointTotals.put(user, points);
                } catch (Exception e) {
                    System.out.println("cant get user");
                }
            }
        }catch (IOException e){

        }

        api.addMessageCreateListener(event -> {
            String message = event.getMessageContent();
            if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!give")){
                for(User user: event.getMessage().getMentionedUsers()){
                    if(pointTotals.get(user) != null)
                        pointTotals.put(user,pointTotals.get(user)+1);
                    else
                        pointTotals.put(user,1);
                    event.getChannel().sendMessage("Gave 1 point to " + user.getNicknameMentionTag());

                }
            }
            else if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!take")){
                for(User user: event.getMessage().getMentionedUsers()){
                    if(pointTotals.get(user) != null)
                        pointTotals.put(user,pointTotals.get(user)-1);
                    else
                        pointTotals.put(user,-1);
                    event.getChannel().sendMessage("Took 1 point from " + user.getNicknameMentionTag());
                }
            }
            else if (message.length() >= 7 && message.substring(0,7).equalsIgnoreCase("!points")){
                String ret = "";
                if (event.getMessage().getMentionedUsers().size() == 0){
                    ret += event.getMessageAuthor().asUser().get().getNicknameMentionTag();
                    if(pointTotals.get(event.getMessageAuthor().asUser().get()) == null)
                        pointTotals.put(event.getMessageAuthor().asUser().get(),0);
                    ret += ": " + pointTotals.get(event.getMessageAuthor().asUser().get());
                    event.getChannel().sendMessage(ret);
                }
                else {
                    for (User user : event.getMessage().getMentionedUsers()) {
                        if(user.getName().equalsIgnoreCase("everyone") || user.getName().equalsIgnoreCase("here"))
                            continue;
                        if(pointTotals.get(user) == null)
                            pointTotals.put(user,0);
                        event.getChannel().sendMessage(user.getNicknameMentionTag() + ": " + pointTotals.get(user));
                    }
                }

            }
            else if(message.equalsIgnoreCase("!leaderboard")){
                List<Map.Entry<User,Integer>> map = new LinkedList<>(pointTotals.entrySet());
                Collections.sort(map, new Comparator<Map.Entry<User, Integer>>() {
                    @Override
                    public int compare(Map.Entry<User, Integer> o1, Map.Entry<User, Integer> o2) {
                        return o2.getValue().compareTo(o1.getValue());
                    }
                });
                for (int i = 0; i < 5 && i < map.size() ; i++) {
                    String board = "**" + (i+1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                    event.getChannel().sendMessage(board);
                }

            }

        });

        // Print the invite url of your bot
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
        while (true){
            try{
                System.out.println("updating points");
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("pointsbot/Resources/points.txt")));
                for (Map.Entry<User,Integer> entry:pointTotals.entrySet()){
                    writer.write(entry.getKey().getIdAsString() + " " + entry.getValue() + "\n");
                }
                writer.flush();
                writer.close();
            }catch(IOException e){}
            try {
                Thread.sleep(600000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}