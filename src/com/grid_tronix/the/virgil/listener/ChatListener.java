package com.grid_tronix.the.virgil.listener;

import chatterbotapi.ChatterBot;
import chatterbotapi.ChatterBotFactory;
import chatterbotapi.ChatterBotSession;
import com.grid_tronix.the.virgil.VirgilMain;
import com.grid_tronix.the.virgil.util.VirgilUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener
{
    private VirgilMain plugin;
    private ChatterBotFactory chatterBotFactory;
    private ChatterBot cleverbot;
    private ChatterBotSession cleverbotSession;

    private boolean debug;
    private static int messageID = 0;

    public ChatListener(final VirgilMain plugin, ChatterBotFactory chatterBotFactory, ChatterBot cleverbot, ChatterBotSession cleverbotSession)
    {
        this.plugin = plugin;
        this.chatterBotFactory = chatterBotFactory;
        this.cleverbot = cleverbot;
        this.cleverbotSession = cleverbotSession;
    }

    @EventHandler (priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(final AsyncPlayerChatEvent event)
    {
        boolean debug = this.plugin.getConfig().getBoolean("debug", false);
        this.debug = debug;
        if (!VirgilUtils.hasPermission(event.getPlayer(), "virgil.trigger"))
            return;

        String message = event.getMessage().toLowerCase();
        ArrayList<String> matchingKeywords = new ArrayList<>();

        if (debug)
        {
            messageID++;
            System.out.println("Message " + messageID + ": \"" + message + "\"");
        }

        // For each set of keywords
        for (ArrayList<String> keywords : this.plugin.getKeywords())
        {
            boolean matched = true;
            // If any of the words in the set aren't present, skip this set
            if (keywords.size() == 1)
            {
                if (debug)
                    System.out.println("One keyword in this set");
                if (!message.contains(keywords.get(0).toLowerCase()))
                {
                    matched = false;
                    if (debug)
                        System.out.println("Reject: " + keywords);
                }
            }
            else
            {
                for (String keyword : keywords)
                {
                    // Matcher1 checks the message for the keyword preceded by a space
                    // Matcher2 checks the message for the keyword followed by a space
                    // If neither finds something, then the keyword is not present and we skip to the next set
                    //Matcher matcher1 = Pattern.compile("[\\s\\p{Punct}]" + keyword.toLowerCase()).matcher(message);
                    //Matcher matcher2 = Pattern.compile(keyword.toLowerCase() + "[\\p{Punct}\\s]").matcher(message);
                    Matcher matcher = Pattern.compile("\\b" + keyword.toLowerCase() + "\\b").matcher(message);
                    if (!matcher.find())
                    {
                        matched = false;
                        if (debug)
                            System.out.println("Reject: " + keywords);
                        break;
                    }
                }
            }
            // If all the words in the set are present, add them to the list of matching keyword sets
            if (matched)
            {
                StringBuilder stringBuilder = new StringBuilder();
                for (String s : keywords)
                    stringBuilder.append(s).append(","); // Save a hidden StringBuilder object by not using concatenation
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                matchingKeywords.add(stringBuilder.toString());
            }
        }

        if (debug)
        {
            System.out.println("Matches:");
            matchingKeywords.forEach(System.out::println);
        }

        // Don't look for the best match if there aren't any matches
        if (matchingKeywords.size() > 0)
        {

            // Find the keyword set with the most characters
            String longestMatch = "";
            for (final String s : matchingKeywords)
                if (s.length() > longestMatch.length())
                    longestMatch = s;

            if (debug)
                System.out.println("Selected: " + longestMatch);

            if (playerHasPermissionForKeyword(event.getPlayer(), longestMatch))
            {

                // Cancel the chat message if the response is not global
                if (!respondGlobally(longestMatch))
                    event.setCancelled(true);

                List<String> responses = getResponses(longestMatch);
                for (String response : responses)
                {
                    if (debug)
                        System.out.println("Response: " + response);
                    response = response.replace("%name%", event.getPlayer().getName());
                    if (debug)
                        System.out.println("Replaced %name%: " + response);
                    if (response.startsWith("/"))
                    {
                        if (response.contains("%player%"))
                        {
                            String[] words = message.split(" ");
                            for (int i = words.length; i > 0; i--)
                            {
                                Player player = this.plugin.getServer().getPlayer(words[i - 1]);
                                if (player != null)
                                {
                                    response = response.replace("%player%", player.getName());
                                    break;
                                }
                            }
                        }
                        if (debug)
                            System.out.println("Executing command: " + response);
                        dispatchCommand(event.getPlayer(), response.replace("/", ""));
                    }
                    else
                    {
                        if (respondGlobally(longestMatch))
                        {
                            if (debug)
                                System.out.println("Broadcasting: " + response);
                            sendBroadcast(VirgilUtils.getPrefix() + VirgilUtils.convertColorCodes(response));
                        }
                        else
                        {
                            if (debug)
                                System.out.println("Messaging: " + response);
                            sendMessage(event.getPlayer(), VirgilUtils.getPrefix() + VirgilUtils.convertColorCodes(response));
                        }
                    }
                }
            }
            else // If player doesn't have the specified permission
            {
                if (debug)
                    System.out.println("Player doesn't have permission. Selecting secondary response.");
                // Cancel the chat message if the response is not global
                if (!respondGloballyNoPerms(longestMatch))
                    event.setCancelled(true);

                List<String> responses = getResponsesNoPerms(longestMatch);
                for (String response : responses)
                {
                    if (debug)
                        System.out.println("Response: " + response);
                    response = response.replace("%name%", event.getPlayer().getName());
                    if (debug)
                        System.out.println("Replaced %name%: " + response);
                    if (response.startsWith("/"))
                    {
                        if (response.contains("%player%"))
                        {
                            String[] words = message.split(" ");
                            for (int i = words.length; i > 0; i--)
                            {
                                Player player = this.plugin.getServer().getPlayer(words[i - 1]);
                                if (player != null)
                                {
                                    response = response.replace("%player%", player.getName());
                                    break;
                                }
                            }
                        }
                        if (debug)
                            System.out.println("Executing command: " + response);
                        dispatchCommand(event.getPlayer(), response.replace("/", ""));
                    }
                    else
                    {
                        if (respondGloballyNoPerms(longestMatch))
                        {
                            if (debug)
                                System.out.println("Broadcasting: " + response);
                            sendBroadcast(VirgilUtils.getPrefix() + VirgilUtils.convertColorCodes(response));
                        }
                        else
                        {
                            if (debug)
                                System.out.println("Messaging: " + response);
                            sendMessage(event.getPlayer(), VirgilUtils.getPrefix() + VirgilUtils.convertColorCodes(response));
                        }
                    }
                }
            }
        }
        else // No matching keyword sets
        {
            // TODO: Maybe have anything from the player for the next <configurable> seconds be sent to the bot so conversations don't need to always include the trigger word?
            String botword = this.plugin.getConfig().getString("bot-keyword");
            Matcher matcher1 = Pattern.compile("\\b" + botword.toLowerCase() + "\\b").matcher(message);
            if (matcher1.find() && VirgilUtils.hasPermission(event.getPlayer(), "virgil.trigger.bot"))
            {
                String toCleverbot = message.replace(botword, "").trim();
                System.out.println("Sending to Cleverbot: " + toCleverbot);
                this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new DelayedThought(toCleverbot, cleverbotSession));
            }
        }
    }

    // Returns true if the response is set to global, returns false otherwise
    private boolean respondGlobally(String keyword)
    {
        return this.plugin.getConfig().getBoolean("keywords." + keyword + ".global", false);
    }

    private List<String> getResponses(String keyword)
    {
        return this.plugin.getConfig().getStringList("keywords." + keyword + ".response");
    }

    private boolean respondGloballyNoPerms(String keyword)
    {
        return this.plugin.getConfig().getBoolean("keywords." + keyword + ".permissions.global");
    }

    private List<String> getResponsesNoPerms(String keyword)
    {
        return this.plugin.getConfig().getStringList("keywords." + keyword + ".permissions.response");
    }

    // Returns false if the keyword has a permission defined and the player does not have that permission node
    // Returns true if there is no permission defined, or if the player does have the permission node
    private boolean playerHasPermissionForKeyword(Player player, String keyword)
    {
        String permission = this.plugin.getConfig().getString("keywords." + keyword + ".permissions.permission", null);
        if (permission == null)
            return true;
        return VirgilUtils.hasPermission(player, permission);
    }

    private void sendMessage(Player player, String message)
    {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new DelayedMessage(player, message), 10L);
    }

    private void sendBroadcast(String broadcast)
    {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new DelayedBroadcast(broadcast), 10L);
    }

    private void dispatchCommand(Player player, String command)
    {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new DelayedCommand(player, command), 10L);
    }

    class DelayedMessage implements Runnable
    {
        Player player;
        String message;

        DelayedMessage(Player player, String message)
        {
            this.player = player;
            this.message = message;
        }

        public void run()
        {
            player.sendMessage(message);
        }
    }

    class DelayedBroadcast implements Runnable
    {
        String broadcast;

        DelayedBroadcast(String broadcast)
        {
            this.broadcast = broadcast;
        }

        public void run()
        {
            Bukkit.broadcastMessage(broadcast);
        }
    }

    class DelayedThought implements Runnable
    {
        String toBot;
        String fromBot;
        ChatterBotSession session;

        DelayedThought(String thought, ChatterBotSession session)
        {
            this.session = session;
            this.toBot = thought;
        }

        public void run()
        {
            try
            {
                fromBot = session.think(toBot);
                if (debug)
                    System.out.println("Received from Cleverbot: " + fromBot);
                sendBroadcast(VirgilUtils.getPrefix() + fromBot);
            }
            catch (Exception e)
            {
                VirgilUtils.log("[" + new Timestamp(new Date().getTime()) + "] " + e);
            }
        }
    }

    class DelayedCommand implements Runnable
    {
        Player player;
        String command;

        DelayedCommand(Player player, String command)
        {
            this.player = player;
            this.command = command;
        }

        public void run()
        {
            Bukkit.dispatchCommand(player, command);
        }
    }
}
