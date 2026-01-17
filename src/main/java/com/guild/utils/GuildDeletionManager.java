package com.guild.utils;

import com.guild.GuildPlugin;
import com.guild.models.Guild;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * 强制删除工会的辅助类：
 * - 先尝试常规删除方法（异步优先）
 * - 若失败则级联删除工会关系、成员，再尝试删除
 * - 所有异常使用 logger.log(Level.SEVERE, ..., ex) 打印堆栈，方便后台排查
 */
public final class GuildDeletionManager {
	private GuildDeletionManager() {}

	// 新增：删除结果封装（用于返回失败原因与异常）
	public static final class DeletionResult {
		private final boolean success;
		private final String reason;
		private final Throwable exception;
		private DeletionResult(boolean success, String reason, Throwable exception) { this.success = success; this.reason = reason; this.exception = exception; }
		public boolean isSuccess() { return success; }
		public String getReason() { return reason; }
		public Throwable getException() { return exception; }
		public static DeletionResult success() { return new DeletionResult(true, null, null); }
		public static DeletionResult failure(String reason) { return new DeletionResult(false, reason, null); }
		public static DeletionResult failure(String reason, Throwable ex) { return new DeletionResult(false, reason, ex); }
	}

	// 尝试结果（包含失败原因）
	private static class AttemptResult {
		final boolean success;
		final String reason;
		AttemptResult(boolean success, String reason) { this.success = success; this.reason = reason; }
	}

	// 修改：返回 DeletionResult，记录详细原因
	public static CompletableFuture<DeletionResult> forceDeleteGuild(GuildPlugin plugin, Guild guild, CommandSender sender) {
		return CompletableFuture.supplyAsync(() -> {
			Object service = plugin.getGuildService();
			StringBuilder reasons = new StringBuilder();
			Throwable failureEx = null;

			// 1) 优先尝试常规删除方法
			try {
				AttemptResult first = attemptStandardDeletion(service, guild, plugin);
				if (first.success) {
					plugin.getLogger().info("删除工会成功 (常规删除): " + guild.getName());
					return DeletionResult.success();
				} else {
					reasons.append("常规删除失败: ").append(first.reason).append("; ");
				}
			} catch (Throwable ex) {
				plugin.getLogger().log(Level.WARNING, "尝试常规删除时发生异常，将转为强制删除: " + guild.getName(), ex);
				reasons.append("尝试常规删除时抛出异常: ").append(ex.getMessage()).append("; ");
				failureEx = ex;
			}

			// 2) 级联删除关系
			boolean relationsOk = deleteRelations(service, guild, plugin);
			if (!relationsOk) reasons.append("级联删除关系失败或未找到接口; ");

			// 3) 级联删除成员
			boolean membersOk = deleteMembers(service, guild, plugin);
			if (!membersOk) reasons.append("级联删除成员失败或未找到接口; ");

			// 4) 再次尝试删除
			try {
				AttemptResult second = attemptStandardDeletion(service, guild, plugin);
				if (second.success) {
					plugin.getLogger().info("删除工会成功 (级联后删除): " + guild.getName());
					return DeletionResult.success();
				} else {
					reasons.append("再次尝试删除失败: ").append(second.reason).append("; ");
				}
			} catch (Throwable ex) {
				plugin.getLogger().log(Level.SEVERE, "最终删除工会时发生异常: " + guild.getName(), ex);
				reasons.append("最终删除时抛出异常: ").append(ex.getMessage()).append("; ");
				if (failureEx == null) failureEx = ex;
			}

			// 如果都失败，尝试从服务缓存移除
			boolean cacheRemoved = removeFromServiceCache(service, guild, plugin);
			if (cacheRemoved) {
				reasons.append("已从服务缓存移除（可能需要手动清理数据库）; ");
			}

			plugin.getLogger().warning("强制删除未成功: " + guild.getName() + " 原因: " + reasons.toString());
			return failureEx == null ? DeletionResult.failure(reasons.toString()) : DeletionResult.failure(reasons.toString(), failureEx);
		});
	}

	// 修改：尝试常规删除改为返回 AttemptResult（包含失败原因）
	private static AttemptResult attemptStandardDeletion(Object service, Guild guild, GuildPlugin plugin) throws Exception {
		UUID id = guild.getId();

		// 常见异步签名
		String[] asyncNames = {"deleteGuildAsync", "forceDeleteGuildAsync", "removeGuildAsync", "deleteByIdAsync", "removeByIdAsync"};
		for (String name : asyncNames) {
			plugin.getLogger().info("检查异步方法: " + name);
			Method m = findMethod(service, new String[]{name}, UUID.class);
			plugin.getLogger().info("方法 " + name + " : " + (m != null ? "找到" : "未找到"));
			if (m != null) {
				try {
					Object res = m.invoke(service, id);
					CompletableFuture<Boolean> fut = toBooleanFuture(res);
					if (fut != null) {
						Boolean ok = fut.get();
						if (Boolean.TRUE.equals(ok)) return new AttemptResult(true, null);
						else return new AttemptResult(false, "异步方法 " + name + " 返回 false/失败");
					}
				} catch (Throwable ex) {
					return new AttemptResult(false, "异步方法 " + name + " 调用异常: " + ex.getMessage());
				}
			}
		}

		// 常见同步签名（UUID）
		String[] syncUUID = {"deleteGuild", "removeGuild", "forceDeleteGuild", "deleteById", "removeById"};
		for (String name : syncUUID) {
			plugin.getLogger().info("检查同步方法: " + name);
			Method m = findMethod(service, new String[]{name}, UUID.class);
			plugin.getLogger().info("方法 " + name + " : " + (m != null ? "找到" : "未找到"));
			if (m != null) {
				try {
					Object res = m.invoke(service, id);
					if (res == null) return new AttemptResult(true, null);                // void -> assume success
					if (res instanceof Boolean && (Boolean) res) return new AttemptResult(true, null);
					return new AttemptResult(false, "同步方法 " + name + " 返回 false/失败");
				} catch (Throwable ex) {
					return new AttemptResult(false, "同步方法 " + name + " 调用异常: " + ex.getMessage());
				}
			}
		}

		// 同步签名（Guild）
		Method mGuild = findMethod(service, new String[]{"deleteGuild","removeGuild"}, Guild.class);
		if (mGuild != null) {
			plugin.getLogger().info("检查 deleteGuild(Guild)");
			try {
				Object res = mGuild.invoke(service, guild);
				if (res == null) return new AttemptResult(true, null);
				if (res instanceof Boolean && (Boolean) res) return new AttemptResult(true, null);
				return new AttemptResult(false, "同步方法 deleteGuild(Guild) 返回 false/失败");
			} catch (Throwable ex) {
				return new AttemptResult(false, "同步方法 deleteGuild(Guild) 调用异常: " + ex.getMessage());
			}
		}

		return new AttemptResult(false, "未找到可用的删除接口 (尝试过 deleteGuildAsync/deleteGuild/removeGuild/deleteById 等)");
	}

	// 新增：如果无法通过 API 删除，尝试从 service 的缓存中移除（Map/Collection 或常见 unregister 方法）
	private static boolean removeFromServiceCache(Object service, Guild guild, GuildPlugin plugin) {
		boolean removed = false;
		try {
			for (Field f : service.getClass().getDeclaredFields()) {
				f.setAccessible(true);
				Object val = f.get(service);
				if (val instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<Object, Object> map = (Map<Object, Object>) val;
					if (map.containsKey(guild.getId())) {
						map.remove(guild.getId());
						plugin.getLogger().info("从服务缓存的 Map 字段移除了工会: " + f.getName());
						removed = true;
					} else if (map.containsValue(guild)) {
						map.values().remove(guild);
						plugin.getLogger().info("从服务缓存的 Map 字段移除了工会对象: " + f.getName());
						removed = true;
					}
				} else if (val instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<Object> col = (Collection<Object>) val;
					if (col.remove(guild)) {
						plugin.getLogger().info("从服务缓存的 Collection 字段移除了工会: " + f.getName());
						removed = true;
					}
				}
			}

			// 尝试常见 unregister 方法
			Method unReg = findMethod(service, new String[]{"unregisterGuild","removeFromCache","evictGuild","evict"}, UUID.class);
			if (unReg == null) unReg = findMethod(service, new String[]{"unregisterGuild","removeFromCache","evictGuild","evict"}, Guild.class);
			if (unReg != null) {
				try {
					if (unReg.getParameterTypes().length == 1 && unReg.getParameterTypes()[0].equals(UUID.class)) {
						unReg.invoke(service, guild.getId());
					} else {
						unReg.invoke(service, guild);
					}
					plugin.getLogger().info("已调用服务的卸载方法: " + unReg.getName());
					removed = true;
				} catch (Throwable e) {
					plugin.getLogger().log(Level.WARNING, "调用卸载方法失败: " + unReg.getName(), e);
				}
			}
		} catch (Throwable ex) {
			plugin.getLogger().log(Level.WARNING, "从服务缓存移除工会失败", ex);
		}
		return removed;
	}

	// 删除工会关系（如果 service 提供相关接口）
	private static boolean deleteRelations(Object service, Guild guild, GuildPlugin plugin) {
		try {
			Method getR = findMethod(service, new String[]{"getGuildRelationsAsync", "getGuildRelations"}, UUID.class);
			if (getR == null) return false;

			Object relObj = getR.invoke(service, guild.getId());
			@SuppressWarnings("unchecked")
			List<Object> relations = (List<Object>) toCompletableFuture(relObj).get();

			if (relations == null || relations.isEmpty()) return true;

			List<CompletableFuture<Boolean>> delFuts = new ArrayList<>();
			Method delRel = findMethod(service, new String[]{"deleteGuildRelationAsync", "deleteGuildRelation", "removeGuildRelation"}, UUID.class);

			for (Object rel : relations) {
				UUID relId = extractUuid(rel);
				if (relId == null) {
					plugin.getLogger().warning("无法提取关系ID，跳过: " + rel);
					continue;
				}
				if (delRel != null) {
					try {
						Object res = delRel.invoke(service, relId);
						CompletableFuture<Boolean> f = toBooleanFuture(res);
						if (f != null) delFuts.add(f);
					} catch (Throwable ex) {
						plugin.getLogger().log(Level.WARNING, "删除工会关系时发生错误: " + relId, ex);
					}
				} else {
					plugin.getLogger().warning("未找到删除工会关系的方法，关系需要手动清理: " + relId);
				}
			}

			if (!delFuts.isEmpty()) CompletableFuture.allOf(delFuts.toArray(new CompletableFuture[0])).get();
			return true;
		} catch (Throwable ex) {
			plugin.getLogger().log(Level.SEVERE, "删除工会关系失败: " + guild.getName(), ex);
			return false;
		}
	}

	// 删除工会成员（如果 service 提供相关接口）
	private static boolean deleteMembers(Object service, Guild guild, GuildPlugin plugin) {
		try {
			Method getM = findMethod(service, new String[]{"getGuildMembersAsync", "getMembersAsync", "getGuildMemberIdsAsync", "getMembers"}, UUID.class);
			if (getM == null) return false;

			Object memObj = getM.invoke(service, guild.getId());
			@SuppressWarnings("unchecked")
			List<Object> members = (List<Object>) toCompletableFuture(memObj).get();

			if (members == null || members.isEmpty()) return true;

			List<CompletableFuture<Boolean>> delFuts = new ArrayList<>();
			for (Object member : members) {
				UUID memberId = extractUuid(member);
				if (memberId == null) {
					plugin.getLogger().warning("无法提取成员ID，跳过: " + member);
					continue;
				}

				// 优先尝试 (guildId, memberId) 签名，其次尝试单参数签名
				Method delTwo = findMethod(service, new String[]{"removeGuildMemberAsync", "deleteGuildMemberAsync", "removeMemberFromGuildAsync", "deleteMemberFromGuildAsync"}, UUID.class, UUID.class);
				Method delOne = findMethod(service, new String[]{"removeGuildMemberAsync", "deleteGuildMemberAsync", "removeMemberAsync", "deleteMember"}, UUID.class);

				try {
					if (delTwo != null) {
						Object res = delTwo.invoke(service, guild.getId(), memberId);
						CompletableFuture<Boolean> f = toBooleanFuture(res);
						if (f != null) delFuts.add(f);
					} else if (delOne != null) {
						Object res = delOne.invoke(service, memberId);
						CompletableFuture<Boolean> f = toBooleanFuture(res);
						if (f != null) delFuts.add(f);
					} else {
						plugin.getLogger().warning("未找到删除成员的方法，成员需要手动清理: " + memberId);
					}
				} catch (Throwable ex) {
					plugin.getLogger().log(Level.WARNING, "删除工会成员时发生错误: " + memberId, ex);
				}
			}

			if (!delFuts.isEmpty()) CompletableFuture.allOf(delFuts.toArray(new CompletableFuture[0])).get();
			return true;
		} catch (Throwable ex) {
			plugin.getLogger().log(Level.SEVERE, "删除工会成员失败: " + guild.getName(), ex);
			return false;
		}
	}

	// --- 辅助方法 ---
	private static Method findMethod(Object obj, String[] names, Class<?>... params) {
		for (String name : names) {
			try {
				return obj.getClass().getMethod(name, params);
			} catch (NoSuchMethodException ignored) {}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> CompletableFuture<T> toCompletableFuture(Object obj) {
		if (obj == null) return CompletableFuture.completedFuture(null);
		if (obj instanceof CompletableFuture) return (CompletableFuture<T>) obj;
		if (obj instanceof java.util.concurrent.Future) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return (T) ((java.util.concurrent.Future<?>) obj).get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		// 同步返回（List 或 单对象）
		return CompletableFuture.completedFuture((T) obj);
	}

	@SuppressWarnings("unchecked")
	private static CompletableFuture<Boolean> toBooleanFuture(Object obj) {
		if (obj == null) return null;
		if (obj instanceof CompletableFuture) return (CompletableFuture<Boolean>) obj;
		if (obj instanceof java.util.concurrent.Future) {
			return CompletableFuture.supplyAsync(() -> {
				try {
					Object r = ((java.util.concurrent.Future<?>) obj).get();
					if (r instanceof Boolean) return (Boolean) r;
					return r != null;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}
		if (obj instanceof Boolean) return CompletableFuture.completedFuture((Boolean) obj);
		// 其他返回视为成功（例如 void）
		return CompletableFuture.completedFuture(true);
	}

	// 尝试从对象中提取 UUID（支持 UUID、String、以及常见的 getId/getUuid/getUniqueId 等方法）
	private static UUID extractUuid(Object obj) {
		if (obj == null) return null;
		if (obj instanceof UUID) return (UUID) obj;
		if (obj instanceof String) {
			try { return UUID.fromString((String) obj); } catch (Exception ignored) {}
		}
		String[] tryMethods = new String[]{"getId", "getUuid", "getUniqueId", "getPlayerId", "getMemberId", "id"};
		for (String mName : tryMethods) {
			try {
				Method m = obj.getClass().getMethod(mName);
				Object res = m.invoke(obj);
				if (res instanceof UUID) return (UUID) res;
				if (res instanceof String) {
					try { return UUID.fromString((String) res); } catch (Exception ignored) {}
				}
			} catch (Exception ignored) {}
		}
		return null;
	}
}
