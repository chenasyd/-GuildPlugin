package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model danych ekonomii gildii
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
     * Dodaj fundusze
     */
    public void addBalance(double amount) {
        this.balance += amount;
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Potrąć fundusze
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
     * Sprawdź, czy można awansować
     */
    public boolean canLevelUp() {
        return this.balance >= this.maxExperience && this.level < 10;
    }

    /**
     * Awansuj gildię
     */
    public boolean levelUp() {
        if (canLevelUp()) {
            this.level++;
            this.balance -= this.maxExperience;
            this.experience = this.balance;

            // Oblicz wymagane doświadczenie dla następnego poziomu
            this.maxExperience = calculateNextLevelExperience();
            this.maxMembers = calculateMaxMembers();

            this.lastUpdated = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Oblicz wymagane doświadczenie dla następnego poziomu
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
     * Oblicz maksymalną liczbę członków
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
     * Pobierz procent postępu ulepszania
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
