package com.discordbot.pointsbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.event.ListenerManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        Map<Long,Map<User,Integer>> servers = new HashMap<>();
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
                    long server= file.nextLong();
                    User user = api.getUserById(file.next()).get();
                    int points = file.nextInt();
                    servers.putIfAbsent(server,new HashMap<>());
                    Map<User,Integer> pointTotals = servers.get(server);
                    pointTotals.put(user, points);
                    servers.put(server,pointTotals);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("cant get user");
                }
            }
        }catch (IOException e){

        }

        api.addMessageCreateListener(event -> {
            if(!event.getMessageAuthor().asUser().get().isYourself()) {
                String message = event.getMessageContent();
                long server = event.getServer().get().getId();
                if (message.length() >= 7 && message.substring(0, 7).equalsIgnoreCase("!points")) {
                    servers.putIfAbsent(server, new HashMap<>());
                    String ret = "";
                    if (event.getMessage().getMentionedUsers().size() == 0) {
                        ret += event.getMessageAuthor().asUser().get().getNicknameMentionTag();
                        servers.get(server).putIfAbsent(event.getMessageAuthor().asUser().get(), 0);
                        ret += ": " + servers.get(server).get(event.getMessageAuthor().asUser().get());
                        event.getChannel().sendMessage(ret);
                    } else {
                        for (User user : event.getMessage().getMentionedUsers()) {
                            if (user.getName().equalsIgnoreCase("everyone") || user.getName().equalsIgnoreCase("here"))
                                continue;
                            servers.get(server).putIfAbsent(user, 0);
                            event.getChannel().sendMessage(user.getNicknameMentionTag() + ": " + servers.get(server).get(user));
                        }
                    }

                } else if (message.equalsIgnoreCase("!leaderboard")) {
                    servers.putIfAbsent(server, new HashMap<>());
                    List<Map.Entry<User, Integer>> map = new LinkedList<>(servers.get(server).entrySet());
                    Collections.sort(map, new Comparator<Map.Entry<User, Integer>>() {
                        @Override
                        public int compare(Map.Entry<User, Integer> o1, Map.Entry<User, Integer> o2) {
                            return o2.getValue().compareTo(o1.getValue());
                        }
                    });
                    for (int i = 0; i < 5 && i < map.size(); i++) {
                        String board = "**" + (i + 1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                        event.getChannel().sendMessage(board);
                    }

                } else if (message.equalsIgnoreCase("!shame")) {
                    servers.putIfAbsent(server, new HashMap<>());
                    List<Map.Entry<User, Integer>> map = new LinkedList<>(servers.get(server).entrySet());
                    Collections.sort(map, new Comparator<Map.Entry<User, Integer>>() {
                        @Override
                        public int compare(Map.Entry<User, Integer> o1, Map.Entry<User, Integer> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    });
                    for (int i = 0; i < 5 && i < map.size(); i++) {
                        String board = "**" + (i + 1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                        event.getChannel().sendMessage(board);
                    }

                }
                else{
                    event.getMessage().addReactionAddListener(m -> {
                        if (event.getMessage().getReactions().stream().filter(i -> {
                            try {
                                return i.getUsers().get().stream().filter(User::isYourself).collect(Collectors.toList()).size() != 0;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            return false;
                        }).collect(Collectors.toList()).size() == 0) {
                            if (m.getEmoji().equalsEmoji("👍")) {
                                User user = event.getMessageAuthor().asUser().get();
                                if (!user.equals(event.getMessageAuthor().asUser().get())) {
                                    if (servers.get(server).get(user) != null)
                                        servers.get(server).put(user, servers.get(server).get(user) + 1);
                                    else
                                        servers.get(server).put(user, 1);
                                    event.getChannel().sendMessage("Gave 1 point to " + user.getNicknameMentionTag());
                                    event.getMessage().addReaction("✅");
                                }
                            } else if (m.getEmoji().equalsEmoji("👎")) {
                                User user = event.getMessageAuthor().asUser().get();
                                if (!user.equals(event.getMessageAuthor().asUser().get())) {
                                    if (servers.get(server).get(user) != null)
                                        servers.get(server).put(user, servers.get(server).get(user) - 1);
                                    else
                                        servers.get(server).put(user, -1);
                                    event.getMessage().addReaction("✅");
                                    event.getChannel().sendMessage("Took 1 point from " + user.getNicknameMentionTag());
                                }
                            }
                        }
                    }).removeAfter(5, TimeUnit.MINUTES);
                }
            }
        });

        // Print the invite url of your bot
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
        while (true){
            try{
                System.out.println("updating points");
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("pointsbot/Resources/points.txt")));
                for (Map.Entry<Long,Map<User,Integer>> entry:servers.entrySet()){
                    long server = entry.getKey();
                    for(Map.Entry<User,Integer> subEntry:servers.get(server).entrySet())
                    writer.write(entry.getKey()+" "+subEntry.getKey().getIdAsString() + " " + subEntry.getValue() + "\n");
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