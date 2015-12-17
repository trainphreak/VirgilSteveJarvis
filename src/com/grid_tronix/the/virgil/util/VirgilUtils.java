package com.grid_tronix.the.virgil.util;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

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
}
