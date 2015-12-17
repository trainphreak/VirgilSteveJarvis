package com.grid_tronix.the.virgil.listener;

import com.grid_tronix.the.virgil.VirgilMain;
import com.grid_tronix.the.virgil.util.VirgilUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;

public class ChatListener implements Listener
{
    private VirgilMain plugin;

    public ChatListener(final VirgilMain plugin)
    {
        this.plugin = plugin;
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

        // Don't look for the best match if there aren't any matches
        if (matchingKeywords.size() <= 0)
            return;


    }
}
