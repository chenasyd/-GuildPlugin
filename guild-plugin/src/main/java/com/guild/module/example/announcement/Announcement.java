package com.guild.module.example.announcement;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工会公告数据模型
 * <p>
 * 每条公告包含：ID、所属工会ID、作者信息、标题、内容、创建/更新时间
 */
public class Announcement {

    private String id;
    private int guildId;
    private UUID authorUuid;
    private String authorName;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 无参构造器（JSON反序列化使用） */
    public Announcement() {}

    public Announcement(String id, int guildId, UUID authorUuid, String authorName,
                       String title, String content) {
        this.id = id;
        this.guildId = guildId;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public int getGuildId() { return guildId; }
    public UUID getAuthorUuid() { return authorUuid; }
    public String getAuthorName() { return authorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }
    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** 获取预览文本（内容前50字符） */
    public String getPreview(int maxLength) {
        if (content == null || content.isEmpty()) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
}
