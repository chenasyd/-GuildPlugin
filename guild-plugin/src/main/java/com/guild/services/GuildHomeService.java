package com.guild.services;

import com.guild.core.utils.QuietLog;
import com.guild.models.GuildMember;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class GuildHomeService extends GuildServiceSupport {

    GuildHomeService(GuildServiceContext ctx) {
        super(ctx);
    }

     /**
      * 设置公会家 (异步)
      */
     public CompletableFuture<Boolean> setGuildHomeAsync(int guildId, org.bukkit.Location location, UUID requesterUuid) {
         return guardServiceFutureBoolean("setGuildHome", ctx.serviceRef.getGuildByIdAsync(guildId).thenCompose(guild -> {
             if (guild == null) {
                 return CompletableFuture.completedFuture(false);
             }
             
             return ctx.serviceRef.getGuildMemberAsync(requesterUuid).thenCompose(member -> {
                 // 检查权限 - 只有会长可以设置家
                 if (member == null || member.getGuildId() != guildId || member.getRole() != GuildMember.Role.LEADER) {
                     return CompletableFuture.completedFuture(false);
                 }
                 
                 return CompletableFuture.supplyAsync(() -> {
                     if (ctx.repos.guilds().updateHome(guildId, location.getWorld().getName(),
                             location.getX(), location.getY(), location.getZ(),
                             location.getYaw(), location.getPitch(), nowString())) {
                         QuietLog.system("Guild home set successfully: " + guild.getName() + " (ID: " + guildId + ")");
                         return true;
                     }
                     return false;
                 });
             });
         }));
     }
     
     /**
      * 设置公会家 (同步包装器)
      */
     public boolean setGuildHome(int guildId, org.bukkit.Location location, UUID requesterUuid) {
         try {
             return setGuildHomeAsync(guildId, location, requesterUuid).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception setting guild home: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * 获取公会家位置 (异步)
      */
     public CompletableFuture<org.bukkit.Location> getGuildHomeAsync(int guildId) {
         return guardServiceFutureNullable("getGuildHome", ctx.serviceRef.getGuildByIdAsync(guildId).thenApply(guild -> {
             if (guild == null || !guild.hasHome()) {
                 return null;
             }
             
             org.bukkit.World world = ctx.plugin.getServer().getWorld(guild.getHomeWorld());
             if (world == null) {
                 ctx.logger.warning("Guild home world does not exist: " + guild.getHomeWorld());
                 return null;
             }
             
             return guild.getHomeLocation(world);
         }));
     }
     
     /**
      * 获取公会家位置 (同步包装器)
      */
     public org.bukkit.Location getGuildHome(int guildId) {
         try {
             return getGuildHomeAsync(guildId).get();
         } catch (Exception e) {
             ctx.logger.severe("Exception fetching guild home: " + e.getMessage());
             return null;
         }
     }

}
