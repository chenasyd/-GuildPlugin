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
     * 更新指定目标的进度
     *
     * @param index 目标索引（从0开始）
     * @param delta 增加的进度量（必须为正数）
     * @return 是否成功更新
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
     * 标记任务为已完成（由外部调用，不在getState中自动触发）
     * 应该在检测到进度满足条件后显式调用
     */
    public void markAsCompleted() { 
        synchronized (stateLock) {
            if (this.completedTime == 0) {
                this.completedTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * 检查是否已标记完成
     */
    public boolean isCompletedMarked() { 
        synchronized (stateLock) {
            return completedTime > 0;
        }
    }

    /**
     * 标记奖励已领取
     */
    public void setClaimed() { 
        synchronized (stateLock) {
            this.claimed = true;
            this.claimedTime = System.currentTimeMillis();
        }
    }

    /**
     * 获取当前状态（纯查询方法，不产生副作用）
     * 
     * @param definition 任务定义（用于判断目标是否完成）
     * @return 当前状态枚举
     */
    public ProgressState getState(QuestDefinition definition) { 
        synchronized (stateLock) {
            if (claimed) return ProgressState.CLAIMED;
            
            // 如果已标记完成，直接返回完成状态
            if (completedTime > 0) return ProgressState.COMPLETED;
            
            // 检查进度是否满足完成条件（但不自动标记）
            if (definition != null && definition.isCompleted(objectiveProgress)) {
                return ProgressState.COMPLETED;  // 进度已完成但尚未标记
            }
            
            return ProgressState.IN_PROGRESS;
        }
    }

    /**
     * 检查进度是否已满足完成条件（不修改任何状态）
     * 
     * @param definition 任务定义
     * @return 是否所有目标都已完成
     */
    public boolean isObjectivesCompleted(QuestDefinition definition) { 
        synchronized (stateLock) {
            return definition != null && definition.isCompleted(objectiveProgress);
        }
    }

    /**
     * 获取进度完成的百分比
     * 
     * @param definition 任务定义
     * @return 完成百分比（0.0 - 100.0）
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
