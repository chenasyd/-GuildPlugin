package com.guild.gui.base;

import com.guild.GuildPlugin;
import com.guild.core.gui.GUIManager;
import com.guild.core.gui.layout.GuiImageLayoutConfig;
import com.guild.core.language.LanguageManager;
import com.guild.models.Guild;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractConfirmGUITest {

    private static final UUID LEADER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private GuildPlugin plugin;
    private Player viewer;
    private GUIManager guiManager;
    private TestConfirmGUI gui;

    @BeforeEach
    void setUp() {
        plugin = mock(GuildPlugin.class);
        viewer = mock(Player.class);
        LanguageManager languageManager = mock(LanguageManager.class);
        guiManager = mock(GUIManager.class);

        when(plugin.getLanguageManager()).thenReturn(languageManager);
        doReturn(guiManager).when(plugin).getGuiManager();
        when(guiManager.isImageLayoutActive(viewer, "TestConfirmGUI")).thenReturn(false);
        when(languageManager.getGuiMessage(eq(viewer), eq("test.title"), eq("Confirm Title")))
                .thenReturn("Confirm Title");

        Guild guild = new Guild("TestGuild", "TG", "desc", LEADER_UUID, "Leader");
        gui = new TestConfirmGUI(plugin, guild, viewer);
    }

    @Test
    void getSize_returnsConfirmLayout() {
        assertEquals(27, gui.getSize());
    }

    @Test
    void getTitle_resolvesLanguageTemplate() {
        assertEquals("Confirm Title", gui.getTitle());
    }

    @Test
    void infoFunctionName_defaultsToInfo() {
        assertEquals(AbstractConfirmGUI.FUNC_INFO, gui.infoFunctionName());
    }

    @Test
    void cancelMaterial_defaultsToEmeraldBlock() {
        assertEquals(Material.EMERALD_BLOCK, gui.cancelMaterial());
    }

    @Test
    void onClick_confirmSlot_invokesOnConfirm() {
        gui.onClick(viewer, 11, new ItemStack(Material.LIME_WOOL), ClickType.LEFT);

        assertTrue(gui.confirmed);
    }

    @Test
    void onClick_cancelSlot_invokesOnCancel() {
        gui.onClick(viewer, 15, new ItemStack(Material.RED_WOOL), ClickType.LEFT);

        assertTrue(gui.cancelled);
    }

    @Test
    void dispatchFunction_routesConfirmAndCancel() {
        gui.dispatchFunction(viewer, AbstractConfirmGUI.FUNC_CONFIRM);
        assertTrue(gui.confirmed);

        gui.dispatchFunction(viewer, AbstractConfirmGUI.FUNC_CANCEL);
        assertTrue(gui.cancelled);
    }

    @Test
    void onClick_imagoLayout_dispatchesConfiguredFunction() {
        GuiImageLayoutConfig layoutConfig = mock(GuiImageLayoutConfig.class);
        when(guiManager.isImageLayoutActive(viewer, "TestConfirmGUI")).thenReturn(true);
        when(guiManager.getImageLayoutConfig()).thenReturn(layoutConfig);
        when(layoutConfig.getFunctionAtSlot("TestConfirmGUI", 11)).thenReturn(AbstractConfirmGUI.FUNC_CONFIRM);

        gui.onClick(viewer, 11, new ItemStack(Material.LIME_WOOL), ClickType.LEFT);

        assertTrue(gui.confirmed);
    }

    static final class TestConfirmGUI extends AbstractConfirmGUI {

        boolean confirmed;
        boolean cancelled;

        TestConfirmGUI(GuildPlugin plugin, Guild guild, Player viewer) {
            super(plugin, guild, viewer, "SourceGUI");
        }

        @Override
        public String getGuiType() {
            return "TestConfirmGUI";
        }

        @Override
        protected String titleKey() {
            return "test.title";
        }

        @Override
        protected String titleDefault() {
            return "Confirm Title";
        }

        @Override
        protected String bedrockTitleKey() {
            return "test.bedrock.title";
        }

        @Override
        protected String bedrockTitleDefault() {
            return "Bedrock";
        }

        @Override
        protected String bedrockContentKey() {
            return "test.bedrock.content";
        }

        @Override
        protected String bedrockContentDefault() {
            return "Content";
        }

        @Override
        protected String bedrockConfirmKey() {
            return "test.bedrock.confirm";
        }

        @Override
        protected String bedrockConfirmDefault() {
            return "Confirm";
        }

        @Override
        protected String bedrockCancelKey() {
            return "test.bedrock.cancel";
        }

        @Override
        protected String bedrockCancelDefault() {
            return "Cancel";
        }

        @Override
        protected Material confirmMaterial() {
            return Material.LIME_WOOL;
        }

        @Override
        protected String confirmButtonKey() {
            return "test.confirm";
        }

        @Override
        protected String confirmButtonDefault() {
            return "Confirm";
        }

        @Override
        protected String confirmLoreKey() {
            return "test.confirm.lore";
        }

        @Override
        protected String confirmLoreDefault() {
            return "Confirm lore";
        }

        @Override
        protected String cancelButtonKey() {
            return "test.cancel";
        }

        @Override
        protected String cancelButtonDefault() {
            return "Cancel";
        }

        @Override
        protected String cancelLoreKey() {
            return "test.cancel.lore";
        }

        @Override
        protected String cancelLoreDefault() {
            return "Cancel lore";
        }

        @Override
        protected ItemStack createInfoItem() {
            return new ItemStack(Material.PAPER);
        }

        @Override
        protected String[] bedrockContentPlaceholders() {
            return new String[0];
        }

        @Override
        protected void onConfirm(Player player) {
            confirmed = true;
        }

        @Override
        protected void onCancel(Player player) {
            cancelled = true;
        }
    }
}
