package com.discordbot.pointsbot;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.user.User;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    private static Classifier classifier = null;

    private static synchronized String classifyMessage(String message){
        ArrayList<Attribute> atts = new ArrayList<Attribute>(2);
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("good");
        classVal.add("bad");
        classVal.add("neutral");
        atts.add(new Attribute("message",(ArrayList<String>)null));
        atts.add(new Attribute("class",classVal));

        Instances messages = new Instances("messages",atts,1);
        messages.setClassIndex(1);
        double[] instanceValue = new double[messages.numAttributes()];

        instanceValue[0] = messages.attribute(0).addStringValue(message);
        messages.add(new DenseInstance(1.0, instanceValue));
        try {
            messages.firstInstance().setClassValue(classifier.classifyInstance(messages.firstInstance()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages.firstInstance().stringValue(1);


    }
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
        try {
            classifier = (Classifier)weka.core.SerializationHelper.read("pointsbot/Resources/messagesClassifier.weka");
        }catch (Exception e){
            e.printStackTrace();
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
                    Collections.sort(map,(o1, o2)->o2.getValue().compareTo(o1.getValue()));
                    for (int i = 0; i < 5 && i < map.size(); i++) {
                        String board = "**" + (i + 1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                        event.getChannel().sendMessage(board);
                    }

                } else if (message.equalsIgnoreCase("!shame")) {
                    servers.putIfAbsent(server, new HashMap<>());
                    List<Map.Entry<User, Integer>> map = new LinkedList<>(servers.get(server).entrySet());
                    Collections.sort(map,Comparator.comparing(Map.Entry::getValue));
                    for (int i = 0; i < 5 && i < map.size(); i++) {
                        String board = "**" + (i + 1) + ".** " + map.get(i).getKey().getNicknameMentionTag() + ": " + map.get(i).getValue();
                        event.getChannel().sendMessage(board);
                    }

                }
                else{
                    event.getMessage().addReactionAddListener(m -> {
                        User user = event.getMessageAuthor().asUser().get();
                        User reactionUser = m.getUser();
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
                                if (!user.equals(reactionUser)) {
                                    if (servers.get(server).get(user) != null)
                                        servers.get(server).put(user, servers.get(server).get(user) + 1);
                                    else
                                        servers.get(server).put(user, 1);
                                    event.getChannel().sendMessage("+1 " + user.getNicknameMentionTag() + ": \""+message+"\"");
                                    event.getMessage().addReaction("✅");
                                }
                            } else if (m.getEmoji().equalsEmoji("👎")) {
                                if (!user.equals(reactionUser)) {
                                    if (servers.get(server).get(user) != null)
                                        servers.get(server).put(user, servers.get(server).get(user) - 1);
                                    else
                                        servers.get(server).put(user, -1);
                                    event.getMessage().addReaction("✅");
                                    event.getChannel().sendMessage("-1 " + user.getNicknameMentionTag() + ": \""+message+"\"");
                                }
                            }
                        }
                    }).removeAfter(5, TimeUnit.MINUTES);
                }
                if(!message.startsWith("!")) {
                    System.out.println(classifyMessage(message.toLowerCase()));
                }
            }
            else{
                event.getMessage().addReactionAddListener(m->{}).removeAfter(10,TimeUnit.MINUTES).addRemoveHandler(()->{
                    int thumbsup = 0;
                    int thumbsdown = 0;
                    if(event.getMessage().getReactionByEmoji("👍").isPresent())
                        thumbsup = event.getMessage().getReactionByEmoji("👍").get().getCount();
                    if(event.getMessage().getReactionByEmoji("👎").isPresent())
                        thumbsdown = event.getMessage().getReactionByEmoji("👎").get().getCount();
                    String message = event.getMessageContent();
                    String outMessage = message.substring(message.indexOf("\"")+1,message.length()-1);
                    if(message.startsWith("-")) {
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter("pointsbot/Resources/messages.ARFF", true));
                            if (thumbsup > thumbsdown) {
                                writer.write("\""+outMessage+"\""+",bad");
                                writer.newLine();
                            }
                            else if (thumbsup < thumbsdown) {
                                writer.write("\""+outMessage+"\""+",neutral");
                                writer.newLine();
                            }
                            writer.close();
                        }
                        catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    else if(message.startsWith("+")) {
                        try {
                            BufferedWriter writer = new BufferedWriter(new FileWriter("pointsbot/Resources/messages.ARFF", true));
                            if (thumbsup > thumbsdown) {
                                writer.write("\""+outMessage+"\""+",good");
                                writer.newLine();
                            }
                            else if (thumbsup < thumbsdown) {
                                writer.write("\""+outMessage+"\""+",neutral");
                                writer.newLine();
                            }
                            writer.close();
                        }
                        catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    event.getMessage().addReaction("🚫");
                });
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