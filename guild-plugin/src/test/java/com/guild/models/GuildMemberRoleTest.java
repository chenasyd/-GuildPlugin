package com.guild.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuildMemberRoleTest {

    @Test
    void leaderHasFullManagementPermissions() {
        GuildMember.Role leader = GuildMember.Role.LEADER;
        assertTrue(leader.canInvite());
        assertTrue(leader.canKick());
        assertTrue(leader.canPromote());
        assertTrue(leader.canDemote());
        assertTrue(leader.canDeleteGuild());
    }

    @Test
    void officerCanInviteAndKickOnly() {
        GuildMember.Role officer = GuildMember.Role.OFFICER;
        assertTrue(officer.canInvite());
        assertTrue(officer.canKick());
        assertFalse(officer.canPromote());
        assertFalse(officer.canDemote());
        assertFalse(officer.canDeleteGuild());
    }

    @Test
    void memberHasNoManagementPermissions() {
        GuildMember.Role member = GuildMember.Role.MEMBER;
        assertFalse(member.canInvite());
        assertFalse(member.canKick());
        assertFalse(member.canPromote());
        assertFalse(member.canDemote());
        assertFalse(member.canDeleteGuild());
    }
}
