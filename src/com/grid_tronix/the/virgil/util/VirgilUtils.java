package com.grid_tronix.the.virgil.util;

import com.grid_tronix.the.virgil.VirgilMain;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class VirgilUtils
{
    private static Permission permission;

    public static boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }

    public static boolean hasPermission(final Player player, final String permission)
    {
        if (VirgilUtils.permission == null)
            return player.isOp();

        return VirgilUtils.permission.playerHas(player, permission);
    }

    public static String convertColorCodes(final String toConvert)
    {
        return ChatColor.translateAlternateColorCodes('&', toConvert);
    }

    public static String getPrefix()
    {
        return VirgilUtils.convertColorCodes(VirgilMain.plugin.getConfig().getString("chat-prefix")) + " ";
    }

    public static void log(String message)
    {
        File logfile = new File(VirgilMain.plugin.getDataFolder().getPath() + "error.log");
        PrintWriter logOut;
        try
        {
            logOut = new PrintWriter(logfile);
            logOut.append(message);
            logOut.flush();
            logOut.close();
        }
        catch (IOException e)
        {
            System.err.println(getPrefix() + "IOException thrown while trying to log an error: Could not open file for writing.");
        }
    }
}
