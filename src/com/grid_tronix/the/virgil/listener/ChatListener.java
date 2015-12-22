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

public class ChatListener implements Listener
{
    private VirgilMain plugin;
    private ChatterBotFactory chatterBotFactory;
    private ChatterBot cleverbot;
    private ChatterBotSession cleverbotSession;

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
        if (VirgilUtils.hasPermission(event.getPlayer(), "virgil.trigger"))
            return;

        String message = event.getMessage().toLowerCase();
        ArrayList<String> matchingKeywords = new ArrayList<>();

        // For each set of keywords
        for (ArrayList<String> keywords : this.plugin.getKeywords())
        {
            boolean matched = true;
            // If any of the words in the set aren't present, skip this set
            for (String keyword : keywords)
            {
                if (!message.contains(keyword.toLowerCase()))
                {
                    matched = false;
                    break;
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

        // If the player doesn't have the permission to trigger this response, remove it from the list of matches
        // This line says "for each String in the ArrayList, if the player doesn't have permission, remove it from the ArrayList"
        // The Stream API was introduced in Java 7 and is more efficient than a basic foreach, which is important in a chat event
        matchingKeywords.stream().filter(keyword -> !playerHasPermission(event.getPlayer(), this.plugin.getConfig().getString("keywords." + keyword + ".permission"))).forEach(matchingKeywords::remove);

        // Don't look for the best match if there aren't any matches
        if (matchingKeywords.size() > 0)
        {

            // Find the keyword set with the most characters
            String longestMatch = "";
            for (final String s : matchingKeywords)
                if (s.length() > longestMatch.length())
                    longestMatch = s;

            // Cancel the chat message if the response is not global
            if (!respondGlobally(longestMatch))
                event.setCancelled(true);

            List<String> responses = getResponse(longestMatch);
            for (String response : responses)
            {
                response = response.replace("%name%", event.getPlayer().getName());
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
                    this.plugin.getServer().dispatchCommand(event.getPlayer(), response.replace("/", ""));
                }
                else
                {
                    if (respondGlobally(response))
                        sendBroadcast(VirgilUtils.convertColorCodes(response));
                    else
                        sendMessage(event.getPlayer(), VirgilUtils.convertColorCodes(response));
                }
            }
        }
        else // No matching keyword sets
        {
            if (VirgilUtils.hasPermission(event.getPlayer(), "virgil.trigger.bot"))
            {
                String toCleverbot = message.substring(this.plugin.getConfig().getString("bot-keyword").length());
                String fromCleverbot;
                try
                {
                    fromCleverbot = cleverbotSession.think(toCleverbot);
                    sendBroadcast(VirgilUtils.getPrefix() + fromCleverbot);
                }
                catch (Exception e)
                {
                    VirgilUtils.log("[" + new Timestamp(new Date().getTime()) + "] " + e);
                }
            }
        }
    }

    // Returns true if the response is set to global, returns false otherwise
    private boolean respondGlobally(String keyword)
    {
        return this.plugin.getConfig().getBoolean("keywords." + keyword + ".global", false);
    }

    private List<String> getResponse(String keyword)
    {
        return this.plugin.getConfig().getStringList("keywords." + keyword + ".response");
    }

    // Returns false if the keyword has a permission defined and the player does not have that permission node
    // Returns true if there is no permission defined, or if the player does have the permission node
    private boolean playerHasPermission(Player player, String keyword)
    {
        String permission = this.plugin.getConfig().getString("keywords." + keyword + ".permission", null);
        if (permission == null)
            return true;
        return VirgilUtils.hasPermission(player, permission);
    }

    private void sendMessage(Player player, String message)
    {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new DelayedMessage(player, message));
    }

    private void sendBroadcast(String broadcast)
    {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new DelayedBroadcast(broadcast));
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
}
