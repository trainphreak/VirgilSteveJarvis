package com.grid_tronix.the.virgil;

import chatterbotapi.ChatterBot;
import chatterbotapi.ChatterBotFactory;
import chatterbotapi.ChatterBotSession;
import chatterbotapi.ChatterBotType;
import com.grid_tronix.the.virgil.listener.ChatListener;
import com.grid_tronix.the.virgil.util.VirgilUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class VirgilMain extends JavaPlugin
{
    public static VirgilMain plugin;
    private ChatListener chatListener;
    private ChatterBotFactory chatterBotFactory;
    private ChatterBot cleverbot;
    private ChatterBotSession cleverbotSession;

    public void onEnable()
    {
        VirgilMain.plugin = this;
        this.saveDefaultConfig();
        chatterBotFactory = new ChatterBotFactory();
        cleverbot = chatterBotFactory.create(ChatterBotType.CLEVERBOT);
        cleverbotSession = cleverbot.createSession();
        VirgilUtils.setupPermissions();
        this.chatListener = new ChatListener(this, chatterBotFactory, cleverbot, cleverbotSession);
        this.getServer().getPluginManager().registerEvents(chatListener, this);
    }

    public void onDisable()
    {
        HandlerList.unregisterAll(chatListener);
        cleverbotSession = null;
        cleverbot = null;
        chatterBotFactory = null;
    }

    public ArrayList<ArrayList<String>> getKeywords()
    {
        ArrayList<ArrayList<String>> keywords = new ArrayList<>();

        Set<String> configKeywords = this.getConfig().getConfigurationSection("keywords").getKeys(false);
        if (configKeywords == null || configKeywords.size() < 1)
            return keywords;

        for (String s : configKeywords)
        {
            ArrayList<String> list = new ArrayList<>();
            Collections.addAll(list, s.split(","));
            keywords.add(list);
        }

        return keywords;
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage(VirgilUtils.getPrefix() + "-" + this.getDescription().getName() + " v" + this.getDescription().getVersion() + " by " + this.getDescription().getAuthors());
            return true;
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            if (!(sender instanceof Player) || VirgilUtils.hasPermission((Player) sender, "virgil.reload"))
            {
                this.reloadConfig();
                sender.sendMessage(VirgilUtils.getPrefix() + "Config reloaded.");
                return true;
            }
        }
        else if (args[0].equalsIgnoreCase("shutup"))
        {
            if (!(sender instanceof Player) || VirgilUtils.hasPermission((Player) sender, "virgil.shutup"))
            {
                int minutes = 60;
                if (args.length > 1)
                {
                    try
                    {
                        minutes = Integer.parseInt(args[1]);
                    }
                    catch (NumberFormatException e)
                    {
                        sender.sendMessage(VirgilUtils.getPrefix() + "\"" + args[1] +
                                VirgilUtils.convertColorCodes("\" is not a number. Try again."));
                        return true;
                    }
                    if (minutes > (60 * 24))
                    {
                        minutes = 60 * 24;
                        sender.sendMessage(VirgilUtils.getPrefix() + "Maximum shutoff time is " + (60 * 24) + " minutes (24 hours).");
                    }
                }
                this.chatListener.shutup(minutes);
                sender.sendMessage(VirgilUtils.getPrefix() + "Shutting off Cleverbot replies for " + minutes + " minutes.");
                return true;
            }
        }
        else if (args[0].equalsIgnoreCase("speak"))
        {
            if (!(sender instanceof Player) || VirgilUtils.hasPermission((Player) sender, "virgil.shutup"))
            {
                this.chatListener.shutup(0); // zero minutes means speak now
                sender.sendMessage(VirgilUtils.getPrefix() + "Cleverbot replies enabled.");
                return true;
            }
        }
        else if (args[0].equalsIgnoreCase("disable"))
        {
            if (!(sender instanceof Player) || VirgilUtils.hasPermission((Player) sender, "virgil.shutup"))
            {
                this.chatListener.shutup(Integer.MAX_VALUE); // Integer.MAX_VALUE*60*1000 is less than Long.MAX_VALUE by a factor of over 65k, so this really shouldn't overflow
                sender.sendMessage(VirgilUtils.getPrefix() + "Cleverbot replies disabled until someone turns them back on.");
                return true;
            }
        }
        return false;
    }
}