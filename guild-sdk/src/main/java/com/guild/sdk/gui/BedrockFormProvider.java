package com.guild.sdk.gui;

import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Bedrock Edition form provider for module GUIs.
 * <p>
 * When a Bedrock player opens the registered GUI, GUIManager invokes this
 * interface to send a Cumulus form instead of translating the Java Inventory.
 * <p>
 * Implementation notes:
 * <ul>
 *   <li>The module is responsible for its own Cumulus dependency ({@code provided} scope).</li>
 *   <li>Form callbacks execute on Netty threads — wrap all Bukkit API calls with
 *       {@code ModuleContext.runSync(Entity, Runnable)}.</li>
 *   <li>This interface does NOT reference any Cumulus types; modules use Cumulus
 *       internally within their implementation.</li>
 * </ul>
 */
@FunctionalInterface
public interface BedrockFormProvider {

    /**
     * Send a Cumulus form to a Bedrock Edition player.
     *
     * @param player the Bedrock player
     * @param data   context data passed when opening the GUI
     *               (same as {@link ModuleGUIFactory#create(Player, Map)} data)
     */
    void sendForm(Player player, Map<String, Object> data);
}
