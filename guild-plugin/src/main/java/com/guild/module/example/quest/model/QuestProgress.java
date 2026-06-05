package com.guild.module.example.quest.model;

import java.util.UUID;

public class QuestProgress {
    private final String questId;
    private final UUID playerUuid;
    private final String playerName;
    private final int guildId;
    private int[] objectiveProgress;
    private long acceptedTime;
    private long completedTime;
    private boolean claimed;
    private long claimedTime;
    private final Object stateLock = new Object();

    public enum ProgressState {
        AVAILABLE,
        IN_PROGRESS,
        COMPLETED,
        CLAIMED,
        EXPIRED
    }

    public QuestProgress(String questId, UUID playerUuid, String playerName, int guildId,
                         int objectiveCount) {
        this.questId = questId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.guildId = guildId;
        this.objectiveProgress = new int[objectiveCount];
        this.acceptedTime = System.currentTimeMillis();
        this.completedTime = 0;
        this.claimed = false;
        this.claimedTime = 0;
    }

    public String getQuestId() { return questId; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public int getGuildId() { return guildId; }
    public int[] getObjectiveProgress() { return objectiveProgress; }
    public long getAcceptedTime() { return acceptedTime; }
    public long getCompletedTime() { return completedTime; }
    public boolean isClaimed() { return claimed; }
    public long getClaimedTime() { return claimedTime; }

    public void setAcceptedTime(long acceptedTime) { 
        synchronized (stateLock) {
            this.acceptedTime = acceptedTime; 
        }
    }
    public void setCompletedTime(long completedTime) { 
        synchronized (stateLock) {
            this.completedTime = completedTime; 
        }
    }
    public void setClaimed(boolean claimed) { 
        synchronized (stateLock) {
            this.claimed = claimed; 
        }
    }
    public void setClaimedTime(long claimedTime) { 
        synchronized (stateLock) {
            this.claimedTime = claimedTime; 
        }
    }

    public void setObjectiveProgress(int[] progress) { 
        synchronized (stateLock) {
            if (progress != null && progress.length == this.objectiveProgress.length) {
                System.arraycopy(progress, 0, this.objectiveProgress, 0, progress.length);
            }
        }
    }

    /**
     * Update progress for a specific objective
     *
     * @param index Objective index (0-based)
     * @param delta Progress increment (must be positive)
     * @return Whether the update was successful
     */
    public boolean updateProgress(int index, int delta) { 
        synchronized (stateLock) {
            if (index >= 0 && index < objectiveProgress.length && delta > 0) {
                objectiveProgress[index] += delta;
                return true;
            }
            return false;
        }
    }

    /**
     * Mark quest as completed (called externally, not auto-triggered in getState).
     * Should be explicitly called after detecting progress meets completion criteria.
     */
    public void markAsCompleted() { 
        synchronized (stateLock) {
            if (this.completedTime == 0) {
                this.completedTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Check if completion is already marked
     */
    public boolean isCompletedMarked() { 
        synchronized (stateLock) {
            return completedTime > 0;
        }
    }

    /**
     * Mark reward as claimed
     */
    public void setClaimed() { 
        synchronized (stateLock) {
            this.claimed = true;
            this.claimedTime = System.currentTimeMillis();
        }
    }

    /**
     * Get the current state (pure query method, no side effects)
     * 
     * @param definition Quest definition (used to check if objectives are complete)
     * @return Current progress state
     */
    public ProgressState getState(QuestDefinition definition) { 
        synchronized (stateLock) {
            if (claimed) return ProgressState.CLAIMED;
            
            // If already marked complete, return directly
            if (completedTime > 0) return ProgressState.COMPLETED;
            
            // Check if progress meets completion criteria (without auto-marking)
            if (definition != null && definition.isCompleted(objectiveProgress)) {
                return ProgressState.COMPLETED;  // Progress completed but not yet marked
            }
            
            return ProgressState.IN_PROGRESS;
        }
    }

    /**
     * Check if progress meets completion criteria (without modifying any state)
     * 
     * @param definition Quest definition
     * @return Whether all objectives are complete
     */
    public boolean isObjectivesCompleted(QuestDefinition definition) { 
        synchronized (stateLock) {
            return definition != null && definition.isCompleted(objectiveProgress);
        }
    }

    /**
     * Get completion percentage
     * 
     * @param definition Quest definition
     * @return Completion percentage (0.0 - 100.0)
     */
    public double getCompletionPercent(QuestDefinition definition) { 
        synchronized (stateLock) {
            if (definition == null || definition.getObjectives().isEmpty()) return 100.0;
            double total = 0;
            for (int i = 0; i < definition.getObjectives().size(); i++) {
                var obj = definition.getObjectives().get(i);
                total += Math.min(1.0, (double) objectiveProgress[i] / obj.getTarget());
            }
            return (total / definition.getObjectives().size()) * 100.0;
        }
    }
}
