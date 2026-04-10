package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工会经济系统数据模型
 */
public class GuildEconomy {
    
    private int id;
    private int guildId;
    private double balance;
    private int level;
    private double experience;
    private double maxExperience;
    private int maxMembers;
    private LocalDateTime lastUpdated;
    
    public GuildEconomy() {}
    
    public GuildEconomy(int guildId) {
        this.guildId = guildId;
        this.balance = 0.0;
        this.level = 1;
        this.experience = 0.0;
        this.maxExperience = 5000.0;
        this.maxMembers = 6;
        this.lastUpdated = LocalDateTime.now();
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
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public double getExperience() {
        return experience;
    }
    
    public void setExperience(double experience) {
        this.experience = experience;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public double getMaxExperience() {
        return maxExperience;
    }
    
    public void setMaxExperience(double maxExperience) {
        this.maxExperience = maxExperience;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * 添加资金
     */
    public void addBalance(double amount) {
        this.balance += amount;
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 扣除资金
     */
    public boolean deductBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            this.lastUpdated = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否可以升级
     */
    public boolean canLevelUp() {
        return this.balance >= this.maxExperience && this.level < 10;
    }
    
    /**
     * 升级工会
     */
    public boolean levelUp() {
        if (canLevelUp()) {
            this.level++;
            this.balance -= this.maxExperience;
            this.experience = this.balance;
            
            // 计算下一级所需经验
            this.maxExperience = calculateNextLevelExperience();
            this.maxMembers = calculateMaxMembers();
            
            this.lastUpdated = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * 计算下一级所需经验
     */
    private double calculateNextLevelExperience() {
        switch (this.level) {
            case 1: return 5000.0;
            case 2: return 10000.0;
            case 3: return 20000.0;
            case 4: return 40000.0;
            case 5: return 80000.0;
            case 6: return 160000.0;
            case 7: return 320000.0;
            case 8: return 640000.0;
            case 9: return 1280000.0;
            default: return Double.MAX_VALUE;
        }
    }
    
    /**
     * 计算最大成员数
     */
    private int calculateMaxMembers() {
        switch (this.level) {
            case 1: return 6;
            case 2: return 12;
            case 3: return 20;
            case 4: return 30;
            case 5: return 40;
            case 6: return 50;
            case 7: return 60;
            case 8: return 75;
            case 9: return 85;
            case 10: return 100;
            default: return 100;
        }
    }
    
    /**
     * 获取升级进度百分比
     */
    public double getUpgradeProgress() {
        if (this.level >= 10) {
            return 100.0;
        }
        return (this.balance / this.maxExperience) * 100.0;
    }
    
    @Override
    public String toString() {
        return "GuildEconomy{" +
                "id=" + id +
                ", guildId=" + guildId +
                ", balance=" + balance +
                ", level=" + level +
                ", maxMembers=" + maxMembers +
                '}';
    }
}
