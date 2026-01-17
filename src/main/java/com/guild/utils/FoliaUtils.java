package com.guild.utils;

public final class FoliaUtils {
	private FoliaUtils() {}

	/**
	 * 检测当前运行环境是否为 Folia（通过是否存在 io.papermc.paper.threadedregions.RegionizedServer 类）
	 */
	public static boolean isFolia() {
		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
			return true;
		} catch (ClassNotFoundException ignore) {
			return false;
		}
	}
}
