package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import com.guild.models.GuildMember;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractPagedMemberGUITest {

    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MEMBER_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private GuildPlugin plugin;
    private Player viewer;
    private Guild guild;
    private TestMemberGUI gui;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        viewer = mock(Player.class);
        LanguageManager languageManager = mock(LanguageManager.class);
        when(plugin.getLanguageManager()).thenReturn(languageManager);
        when(languageManager.getGuiMessage(eq(viewer), eq("test.title"), eq("Title"),
                eq("{page}"), eq("3"))).thenReturn("Page Title");

        guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        guild.setId(1);
        gui = new TestMemberGUI(plugin, guild, viewer);
    }

    @Test
    void filterMembers_excludesLeader() {
        GuildMember leader = new GuildMember(1, LEADER_UUID, "Leader", GuildMember.Role.LEADER);
        GuildMember member = new GuildMember(1, MEMBER_UUID, "Member", GuildMember.Role.MEMBER);

        List<GuildMember> filtered = gui.filterMembers(List.of(leader, member));

        assertEquals(1, filtered.size());
        assertEquals(MEMBER_UUID, filtered.get(0).getPlayerUuid());
    }

    @Test
    void getTitle_includesPageNumber() {
        gui.currentPage = 2;

        assertEquals("Page Title", gui.getTitle());
    }

    @Test
    void onEntrySelected_delegatesToMemberHandler() {
        GuildMember member = new GuildMember(1, MEMBER_UUID, "Member", GuildMember.Role.MEMBER);

        gui.onEntrySelected(viewer, member);

        assertTrue(gui.memberSelected);
        assertEquals(member, gui.lastSelectedMember);
    }

    static final class TestMemberGUI extends AbstractPagedMemberGUI {

        boolean memberSelected;
        GuildMember lastSelectedMember;

        TestMemberGUI(GuildPlugin plugin, Guild guild, Player viewer) {
            super(plugin, guild, viewer);
        }

        @Override
        protected void loadMembers() {
            // 单测不走异步 GuildService
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
        protected String bedrockNoMembersKey() {
            return "test.empty";
        }

        @Override
        protected String bedrockNoMembersDefault() {
            return "Empty";
        }

        @Override
        protected String memberDisplayNamePrefix() {
            return "&e";
        }

        @Override
        protected String bedrockMemberButtonPrefix() {
            return "§e";
        }

        @Override
        protected String memberPositionLoreKey() {
            return "test.position";
        }

        @Override
        protected String memberPositionLoreDefault() {
            return "Position";
        }

        @Override
        protected String memberClickLoreColorPrefix() {
            return "&a";
        }

        @Override
        protected String memberClickLoreKey() {
            return "test.click";
        }

        @Override
        protected String memberClickLoreDefault() {
            return "Click";
        }

        @Override
        protected void onMemberSelected(org.bukkit.entity.Player player, GuildMember member) {
            memberSelected = true;
            lastSelectedMember = member;
        }

        @Override
        protected void openBackGui(org.bukkit.entity.Player player) {
        }
    }
}
