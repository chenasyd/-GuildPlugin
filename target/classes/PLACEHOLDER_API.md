# Guild插件 PlaceholderAPI 变量使用说明

## 概述

Guild插件本次更新提供了完整的 PlaceholderAPI 支持，允许其他插件和聊天栏显示工会相关的动态信息。所有变量都支持颜色代码和中文显示。

## 基础工会信息变量

### 工会基本信息
- `%guild_name%` - 工会名称
- `%guild_tag%` - 工会标签
- `%guild_membercount%` - 工会成员数量
- `%guild_maxmembers%` - 工会最大成员数
- `%guild_level%` - 工会等级
- `%guild_balance%` - 工会余额（保留2位小数）
- `%guild_frozen%` - 工会状态（正常/已冻结/无工会）

### 玩家在工会中的信息
- `%guild_role%` - 玩家在工会中的角色（会长/官员/成员）
- `%guild_joined%` - 玩家加入工会的时间
- `%guild_contribution%` - 玩家对工会的贡献度

## 工会状态检查变量

### 玩家状态
- `%guild_hasguild%` - 玩家是否有工会（是/否）
- `%guild_isleader%` - 玩家是否是会长（是/否）
- `%guild_isofficer%` - 玩家是否是官员（是/否）
- `%guild_ismember%` - 玩家是否是工会成员（是/否）

## 工会权限检查变量

### 权限状态
- `%guild_caninvite%` - 是否可以邀请玩家（是/否）
- `%guild_cankick%` - 是否可以踢出成员（是/否）
- `%guild_canpromote%` - 是否可以提升成员（是/否）
- `%guild_candemote%` - 是否可以降级成员（是/否）
- `%guild_cansethome%` - 是否可以设置工会传送点（是/否）
- `%guild_canmanageeconomy%` - 是否可以管理工会经济（是/否）
