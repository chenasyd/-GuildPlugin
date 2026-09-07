package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractPagedPlayerGUITest {

    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private GuildPlugin plugin;
    private Player viewer;
    private Player target;
    private TestPlayerGUI gui;

    private GUIManager guiManager;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        viewer = mock(Player.class);
        target = mock(Player.class);
        when(target.getName()).thenReturn("Target");

        LanguageManager languageManager = mock(LanguageManager.class);
        guiManager = mock(GUIManager.class);
        when(plugin.getLanguageManager()).thenReturn(languageManager);
        doReturn(guiManager).when(plugin).getGuiManager();
        when(guiManager.isImageLayoutActive(viewer, "TestPlayerGUI")).thenReturn(false);
        when(languageManager.getGuiMessage(eq(viewer), eq("test.title"), eq("Title"),
                eq("{page}"), eq("1"), eq("{guild}"), eq("TestGuild")))
                .thenReturn("Invite Title");

        Guild guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        gui = new TestPlayerGUI(plugin, guild, viewer, List.of(target));
    }

    @Test
    void getTitle_resolvesLanguageTemplate() {
        assertEquals("Invite Title", gui.getTitle());
    }

    @Test
    void onClick_selectsPlayerEntry() {
        gui.onClick(viewer, GuiLayoutUtils.slotForPageIndex(0), new ItemStack(Material.PLAYER_HEAD), ClickType.LEFT);

        assertTrue(gui.playerSelected);
        assertEquals(target, gui.lastSelectedPlayer);
    }

    @Test
    void loadEntries_refreshesWhenReloaded() {
        gui.players = List.of();

        assertTrue(gui.loadEntries().isEmpty());
    }

    static final class TestPlayerGUI extends AbstractPagedPlayerGUI {

        List<Player> players;
        boolean playerSelected;
        Player lastSelectedPlayer;

        TestPlayerGUI(GuildPlugin plugin, Guild guild, Player viewer, List<Player> players) {
            super(plugin, guild, viewer);
            this.players = players;
            setEntries(players);
        }

        @Override
        public String getGuiType() {
            return "TestPlayerGUI";
        }

        @Override
        protected List<Player> loadEntries() {
            return players;
        }

        @Override
        protected String titleKey() {
            return "test.title";
        }

        @Override
        protected String titleDefault() {
            return "Title";
        }

        @Override
        protected String bedrockTitleKey() {
            return "test.bedrock";
        }

        @Override
        protected String bedrockTitleDefault() {
            return "Bedrock";
        }

        @Override
        protected String bedrockTitlePageKey() {
            return "test.page";
        }

        @Override
        protected String bedrockTitlePageDefault() {
            return "Page";
        }

        @Override
        protected String bedrockContentKey() {
            return "test.content";
        }

        @Override
        protected String bedrockContentDefault() {
            return "Content";
        }

        @Override
        protected String bedrockEmptyKey() {
            return "test.empty";
        }

        @Override
        protected String bedrockEmptyDefault() {
            return "Empty";
        }

        @Override
        protected String bedrockMemberButtonPrefix() {
            return "§a";
        }

        @Override
        protected ItemStack createEntryItem(Player targetPlayer) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        @Override
        protected void onEntrySelected(Player viewerPlayer, Player targetPlayer) {
            playerSelected = true;
            lastSelectedPlayer = targetPlayer;
        }

        @Override
        protected void openBackGui(Player player) {
        }
    }
}
