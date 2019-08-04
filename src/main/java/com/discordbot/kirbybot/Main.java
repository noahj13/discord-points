package com.discordbot.kirbybot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {

        Map<Long,Map<User,Integer>> servers = new HashMap<>();
        Map<String,BufferedImage> images = new HashMap<>();
        // Insert your bot's token here
        Scanner file = new Scanner(System.in);

        try {
            file = new Scanner(new File("../KirbyBotResources/token.cred"));
        }catch (IOException e){
            System.out.println("cannot find token");
            System.exit(1);
        }
        String token = file.next();

        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

        try {
            file = new Scanner(new File("../KirbyBotResources/points.txt"));
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
            e.printStackTrace();
        }

        try{
            File imgDir = new File("../KirbyBotResources/img");
            Arrays.stream(imgDir.listFiles()).forEach(i -> {
                try {
                    images.put(i.getName(), ImageIO.read(i));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }
        MessageBuilder mb = new MessageBuilder();
        api.addMessageCreateListener(event -> {
            String message = event.getMessageContent();
            if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!give")){
                long server = event.getServer().get().getId();
                servers.putIfAbsent(server,new HashMap<>());
                for(User user: event.getMessage().getMentionedUsers()){
                    if(user.equals(event.getMessageAuthor().asUser().get()))
                        continue;
                    if(servers.get(server).get(user) != null)
                        servers.get(server).put(user,servers.get(server).get(user)+1);
                    else
                        servers.get(server).put(user,1);
                    event.getChannel().sendMessage("Gave 1 point to " + user.getNicknameMentionTag());
                    System.out.println(event.getServer().get().getId());
                }
            }
            else if (message.length() >= 5 && message.substring(0,5).equalsIgnoreCase("!take")){
                long server = event.getServer().get().getId();
                servers.putIfAbsent(server,new HashMap<>());
                for(User user: event.getMessage().getMentionedUsers()){
                    if(user.equals(event.getMessageAuthor().asUser().get()))
                        continue;
                    if(servers.get(server).get(user) != null)
                        servers.get(server).put(user,servers.get(server).get(user)-1);
                    else
                        servers.get(server).put(user,-1);
                    event.getChannel().sendMessage("Took 1 point from " + user.getNicknameMentionTag());
                }
            }
            else if (message.length() >= 7 && message.substring(0,7).equalsIgnoreCase("!points")){
                long server = event.getServer().get().getId();
                servers.putIfAbsent(server,new HashMap<>());
                String ret = "";
                if (event.getMessage().getMentionedUsers().size() == 0){
                    ret += event.getMessageAuthor().asUser().get().getNicknameMentionTag();
                    servers.get(server).putIfAbsent(event.getMessageAuthor().asUser().get(),0);
                    ret += ": " + servers.get(server).get(event.getMessageAuthor().asUser().get());
                    event.getChannel().sendMessage(ret);
                }
                else {
                    for (User user : event.getMessage().getMentionedUsers()) {
                        if(user.getName().equalsIgnoreCase("everyone") || user.getName().equalsIgnoreCase("here"))
                            continue;
                        servers.get(server).putIfAbsent(user,0);
                        event.getChannel().sendMessage(user.getNicknameMentionTag() + ": " + servers.get(server).get(user));
                    }
                }

            }
            else if(message.equalsIgnoreCase("!leaderboard")){
                long server = event.getServer().get().getId();
                servers.putIfAbsent(server,new HashMap<>());
                List<Map.Entry<User,Integer>> map = new LinkedList<>(servers.get(server).entrySet());
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
            else if(message.equalsIgnoreCase("!shame")){
                long server = event.getServer().get().getId();
                servers.putIfAbsent(server,new HashMap<>());
                List<Map.Entry<User,Integer>> map = new LinkedList<>(servers.get(server).entrySet());
                Collections.sort(map, new Comparator<Map.Entry<User, Integer>>() {
                    @Override
                    public int compare(Map.Entry<User, Integer> o1, Map.Entry<User, Integer> o2) {
                        return o1.getValue().compareTo(o2.getValue());
                    }
                });
                for (int i = 0; i < 5 && i < map.size() ; i++) {
                    String board = "**" + (i+1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                    event.getChannel().sendMessage(board);
                }

            }
            try {
                if (containsString(message, "oof")) {
                    mb.addAttachment(images.get("Smackkirb.png"), "Smackkirb.png").send(event.getChannel());
                } else if (containsString(message, "sip")) {
                    mb.addAttachment(images.get("kirbsip.png"), "kirbsip.png").send(event.getChannel());
                } else if (containsString(message, "ouch")) {
                    mb.addAttachment(images.get("Sadkirb.png"), "Sadkirb.png").send(event.getChannel());
                } else if (containsString(message, "bedtime")) {
                    mb.addAttachment(images.get("Sleepkirb.png"), "Sleepkirb.png").send(event.getChannel());
                } else if (containsString(message, "dead")) {
                    mb.addAttachment(images.get("Pikadown.png"), "Pikadown.png").send(event.getChannel());
                } else if (message.equalsIgnoreCase("no u")) {
                    mb.addAttachment(images.get("Madkirb.gif"), "Madkirb.gif").send(event.getChannel());
                } else if (containsString(message, "bruh")) {
                    mb.addAttachment(images.get("Bruh.png"), "Bruh.png").send(event.getChannel());
                } else if (containsString(message, "illegal")) {
                    mb.addAttachment(images.get("Policekirb.jpg"), "Policekirb.jpg").send(event.getChannel());
                } else if (containsString(message, "why")) {
                    mb.addAttachment(images.get("disturbedkirb.jpg"), "disturbedkirb.jpg").send(event.getChannel());
                } else if (message.equalsIgnoreCase("oh okay")) {
                    mb.addAttachment(images.get("Deflatedpika.png"), "Deflatedpika.png").send(event.getChannel());
                } else if (!event.getMessageAuthor().isYourself() && containsString(message,"stfu")){
                    List<User> users = event.getMessage().getMentionedUsers();
                    mb.addAttachment(images.get("STFUkirb.png"),"STFUkirb.png").append(
                            users.stream().map(User::getNicknameMentionTag).collect(Collectors.joining(" ","STFU ",""))
                    ).send(event.getChannel());
                }

            }catch (IllegalArgumentException e){
                System.out.println("cannot find image for " + message);
            }

        });

        // Print the invite url of your bot
        System.out.println("You can invite the bot by using the following url: " + api.createBotInvite());
        while (true){
            try{
                System.out.println("updating points");
                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("../KirbyBotResources/points.txt")));
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
    private static boolean containsString(String message,String word){
        return Arrays.stream(message.toLowerCase().split("\\s")).filter(s->(word.toLowerCase().equalsIgnoreCase(s))).count() != 0;
    }

}