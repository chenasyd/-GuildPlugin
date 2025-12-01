package com.guild.models;

import java.time.LocalDateTime;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Model danych gildii
 */
public class Guild {
    
    private int id;
    private String name;
    private String tag;
    private String description;
    private UUID leaderUuid;
    private String leaderName;
    private String homeWorld;
    private double homeX;
    private double homeY;
    private double homeZ;
    private float homeYaw;
    private float homePitch;
    private double balance;
    private int level;
    private int maxMembers;
    private boolean frozen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Guild() {}
    
    public Guild(String name, String tag, String description, UUID leaderUuid, String leaderName) {
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.leaderUuid = leaderUuid;
        this.leaderName = leaderName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public UUID getLeaderUuid() {
        return leaderUuid;
    }
    
    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }
    
    public String getLeaderName() {
        return leaderName;
    }
    
    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Metody związane z lokalizacją domu
    public String getHomeWorld() {
        return homeWorld;
    }
    
    public void setHomeWorld(String homeWorld) {
        this.homeWorld = homeWorld;
    }
    
    public double getHomeX() {
        return homeX;
    }
    
    public void setHomeX(double homeX) {
        this.homeX = homeX;
    }
    
    public double getHomeY() {
        return homeY;
    }
    
    public void setHomeY(double homeY) {
        this.homeY = homeY;
    }
    
    public double getHomeZ() {
        return homeZ;
    }
    
    public void setHomeZ(double homeZ) {
        this.homeZ = homeZ;
    }
    
    public float getHomeYaw() {
        return homeYaw;
    }
    
    public void setHomeYaw(float homeYaw) {
        this.homeYaw = homeYaw;
    }
    
    public float getHomePitch() {
        return homePitch;
    }
    
    public void setHomePitch(float homePitch) {
        this.homePitch = homePitch;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public boolean isFrozen() {
        return frozen;
    }
    
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
    
    /**
     * Sprawdź, czy gildia ma ustawiony dom
     */
    public boolean hasHome() {
        return homeWorld != null && !homeWorld.isEmpty();
    }
    
    /**
     * Ustaw lokalizację domu
     */
    public void setHome(Location location) {
        this.homeWorld = location.getWorld().getName();
        this.homeX = location.getX();
        this.homeY = location.getY();
        this.homeZ = location.getZ();
        this.homeYaw = location.getYaw();
        this.homePitch = location.getPitch();
    }
    
    /**
     * Pobierz lokalizację domu (wymaga obiektu świata)
     */
    public Location getHomeLocation(org.bukkit.World world) {
        if (!hasHome() || world == null) {
            return null;
        }
        return new Location(world, homeX, homeY, homeZ, homeYaw, homePitch);
    }
    
    @Override
    public String toString() {
        return "Guild{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", description='" + description + '\'' +
                ", leaderUuid=" + leaderUuid +
                ", leaderName='" + leaderName + '\'' +
                ", hasHome=" + hasHome() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
