package com.guild.services.repository;

import com.guild.core.database.DatabaseManager;

import java.util.logging.Logger;

/**
 * 聚合公会相关 Repository，供 {@link com.guild.services.GuildService} 门面委托。
 */
public final class GuildRepositories {

    private final GuildMemberRepository members;
    private final GuildRepository guilds;
    private final GuildRelationRepository relations;
    private final GuildApplicationRepository applications;
    private final GuildInvitationRepository invitations;
    private final GuildLogRepository logs;
    private final GuildEconomyRepository economy;
    private final GuildContributionRepository contributions;

    public GuildRepositories(DatabaseManager databaseManager, Logger logger) {
        this.members = new GuildMemberRepository(databaseManager, logger);
        this.guilds = new GuildRepository(databaseManager, logger);
        this.relations = new GuildRelationRepository(databaseManager, logger);
        this.applications = new GuildApplicationRepository(databaseManager, logger);
        this.invitations = new GuildInvitationRepository(databaseManager, logger);
        this.logs = new GuildLogRepository(databaseManager, logger);
        this.economy = new GuildEconomyRepository(databaseManager, logger);
        this.contributions = new GuildContributionRepository(databaseManager, logger);
    }

    public GuildMemberRepository members() {
        return members;
    }

    public GuildRepository guilds() {
        return guilds;
    }

    public GuildRelationRepository relations() {
        return relations;
    }

    public GuildApplicationRepository applications() {
        return applications;
    }

    public GuildInvitationRepository invitations() {
        return invitations;
    }

    public GuildLogRepository logs() {
        return logs;
    }

    public GuildEconomyRepository economy() {
        return economy;
    }

    public GuildContributionRepository contributions() {
        return contributions;
    }
}
