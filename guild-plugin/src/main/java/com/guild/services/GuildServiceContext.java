package com.guild.services;

import com.guild.GuildPlugin;
import com.guild.core.database.DatabaseManager;
import com.guild.services.repository.GuildRepositories;

import java.util.logging.Logger;

/**
 * Shared context for guild domain services; sub-services wired after construction.
 */
public final class GuildServiceContext {
    public final GuildPlugin plugin;
    public final DatabaseManager databaseManager;
    public final GuildRepositories repos;
    public final Logger logger;

    public GuildQueryService queries;
    public GuildLogService logs;
    public GuildRelationService relations;
    public GuildHomeService home;
    public GuildEconomyService economy;
    public GuildMemberService members;
    public GuildLifecycleService lifecycle;

    /** Set after {@link GuildService} construction; enables cross-service calls through the facade (spy-friendly). */
    GuildService serviceRef;

    public GuildServiceContext(GuildPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
        this.repos = new GuildRepositories(databaseManager, logger);
    }
}
