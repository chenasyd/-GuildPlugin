package com.guild.sdk.data;

import java.util.List;
import java.util.UUID;

/**
 * 工会数据传输对象（只读）
 * <p>
 * 模块通过此对象读取工会信息，无法直接修改核心数据。
 * 所有字段在构造时由核心服务填充，之后不可变更。
 */
public class GuildData {

    private final int id;
    private final String name;
    private final UUID masterUuid;
    private final String masterName;
    private final int level;
    private final long experience;
    private final double balance;
    private final int memberCount;
    private final int maxMembers;
    private final String motto;
    private final long createTime;
    private final List<MemberData> members;

    public GuildData(int id, String name, UUID masterUuid, String masterName,
                    int level, long experience, double balance,
                    int memberCount, int maxMembers, String motto,
                    long createTime, List<MemberData> members) {
        this.id = id;
        this.name = name;
        this.masterUuid = masterUuid;
        this.masterName = masterName;
        this.level = level;
        this.experience = experience;
        this.balance = balance;
        this.memberCount = memberCount;
        this.maxMembers = maxMembers;
        this.motto = motto;
        this.createTime = createTime;
        this.members = members != null ? List.copyOf(members) : List.of();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public UUID getMasterUuid() { return masterUuid; }
    public String getMasterName() { return masterName; }
    public int getLevel() { return level; }
    public long getExperience() { return experience; }
    public double getBalance() { return balance; }
    public int getMemberCount() { return memberCount; }
    public int getMaxMembers() { return maxMembers; }
    public String getMotto() { return motto; }
    public long getCreateTime() { return createTime; }
    public List<MemberData> getMembers() { return members; }

    @Override
    public String toString() {
        return "GuildData{id=" + id + ", name='" + name + "', level=" + level +
                ", members=" + memberCount + "/" + maxMembers + "}";
    }
}
