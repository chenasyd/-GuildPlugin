package com.guild.module.example.quest;

import com.guild.module.example.quest.tree.GuildTreeService;
import com.guild.sdk.GuildPluginAPI;
import com.guild.sdk.event.EconomyEventData;
import com.guild.sdk.event.EconomyEventHandler;
import com.guild.sdk.event.GuildEventData;
import com.guild.sdk.event.GuildEventHandler;
import com.guild.sdk.event.MemberEventData;
import com.guild.sdk.event.MemberEventHandler;

/** SDK 事件订阅：公会删除、成员离开、经济存款 → 任务进度。 */
final class QuestEventDispatcher {

    private QuestEventDispatcher() {
    }

    static void register(GuildPluginAPI api, GuildQuestModule module,
                         QuestManager questManager, QuestTracker questTracker,
                         GuildTreeService treeService) {
        api.onGuildDelete(new GuildEventHandler() {
            @Override
            public void onEvent(GuildEventData data) {
                questManager.saveAll();
                if (treeService != null) {
                    treeService.invalidate(data.getGuildId());
                }
            }

            @Override
            public Object getModuleInstance() {
                return module;
            }
        });

        api.onMemberLeave(new MemberEventHandler() {
            @Override
            public void onEvent(MemberEventData data) {
                questManager.clearPlayerProgress(data.getGuildId(), data.getPlayerUuid());
            }

            @Override
            public Object getModuleInstance() {
                return module;
            }
        });

        api.onEconomyDeposit(new EconomyEventHandler() {
            @Override
            public void onEvent(EconomyEventData data) {
                questTracker.onPlayerDepositMoney(data.getPlayerUuid(), data.getAmount());
            }

            @Override
            public Object getModuleInstance() {
                return module;
            }
        });
    }
}
