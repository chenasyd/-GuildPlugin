package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;
import com.guild.core.language.LanguageManager;
import com.guild.GuildPlugin;

/**
 * 工会贡献记录数据模型
 */
public class GuildContribution {
    
    private int id;
    private int guildId;
    private UUID playerUuid;
    private String playerName;
    private double amount;
    private ContributionType type;
    private String description;
    private LocalDateTime createdAt;
    
    public enum ContributionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        CREATION,
        UPGRADE,
        ADMIN;

        /**
         * 获取贡献类型显示名称（多语言支持）
         * @param lang 语言代码（如 "zh", "en", "pl"）
         * @return 本地化的显示名称
         */
        public String getDisplayName(String lang) {
            String key = "contribution.type." + name().toLowerCase();
            LanguageManager languageManager = GuildPlugin.getInstance().getLanguageManager();

            switch (this) {
                case DEPOSIT:
                    return languageManager.getMessage(lang, key, "Deposit");
                case WITHDRAW:
                    return languageManager.getMessage(lang, key, "Withdraw");
                case TRANSFER:
                    return languageManager.getMessage(lang, key, "Transfer");
                case CREATION:
                    return languageManager.getMessage(lang, key, "Guild Creation");
                case UPGRADE:
                    return languageManager.getMessage(lang, key, "Guild Upgrade");
                case ADMIN:
                    return languageManager.getMessage(lang, key, "Admin Operation");
                default:
                    return name();
            }
        }

        /**
         * 获取贡献类型显示名称（使用默认语言）
         * @return 本地化的显示名称
         */
        public String getDisplayName() {
            return getDisplayName("en");
        }
    }
    
    public GuildContribution() {}
    
    public GuildContribution(int guildId, UUID playerUuid, String playerName, 
                           double amount, ContributionType type, String description) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getGuildId() {
        return guildId;
    }
    
    public void setGuildId(int guildId) {
        this.guildId = guildId;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public ContributionType getType() {
        return type;
    }
    
    public void setType(ContributionType type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 获取总贡献（存款 - 取款）
     */
    public double getNetContribution() {
        if (type == ContributionType.WITHDRAW) {
            return -amount;
        }
        return amount;
    }
    
    @Override
    public String toString() {
        return "GuildContribution{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", playerName='" + playerName + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }
}
