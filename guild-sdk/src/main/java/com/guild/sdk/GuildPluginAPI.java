package com.guild.sdk;

import com.guild.GuildPlugin;
import com.guild.core.module.hook.GUIExtensionHook;
import com.guild.sdk.command.ModuleCommandHandler;
import com.guild.sdk.data.GuildData;
import com.guild.sdk.data.MemberData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventHandler;
import com.guild.sdk.gui.ModuleGUIFactory;
import com.guild.sdk.http.HttpClientProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 编译期 SDK 门面。
 * 运行时由主插件中的同名类提供真实实现。
 */
public class GuildPluginAPI {
    private final HttpClientProvider httpClient = new HttpClientProvider();

    public GuildPluginAPI() {
    }

    public GuildPluginAPI(GuildPlugin plugin) {
    }

    public CompletableFuture<GuildData> getGuildById(int id) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<GuildData> getGuildByName(String name) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<GuildData> getPlayerGuild(UUID playerUuid) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<List<GuildData>> getAllGuilds() {
        return CompletableFuture.completedFuture(List.of());
    }

    public CompletableFuture<List<MemberData>> getGuildMembers(int guildId) {
        return CompletableFuture.completedFuture(List.of());
    }

    public void registerGUIButton(String guiType, int slot, ItemStack item, String moduleId,
                                  GUIExtensionHook.GUIClickAction handler) {
    }

    public void registerCustomGUI(String guiId, ModuleGUIFactory factory) {
    }

    public void unregisterCustomGUI(String guiId) {
    }

    public void openCustomGUI(String guiId, Player player, Map<String, Object> data) {
    }

    public void openCustomGUI(String guiId, Player player) {
    }

    public void registerSubCommand(String parentCommand, String name, ModuleCommandHandler handler, String permission) {
    }

    public void onGuildCreate(GuildEventHandler handler) {
    }

    public void onGuildDelete(GuildEventHandler handler) {
    }

    public void onMemberJoin(MemberEventHandler handler) {
    }

    public void onMemberLeave(MemberEventHandler handler) {
    }

    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return httpClient.httpGet(url, headers);
    }

    public CompletableFuture<String> httpGet(String url) {
        return httpGet(url, null);
    }

    public CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers) {
        return httpClient.httpPost(url, body, headers);
    }

    public HttpClientProvider getHttpClient() {
        return httpClient;
    }
}
