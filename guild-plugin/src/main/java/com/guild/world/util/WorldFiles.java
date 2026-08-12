package com.guild.world.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 世界磁盘路径工具。
 *
 * <p>Paper/Folia 26+ 改为接近原版的布局：
 * {@code <level-name>/dimensions/<namespace>/<key>/}，
 * 不再使用 CraftBukkit 时代的 {@code <worldContainer>/<worldName>/} 独立根目录。
 * {@link World#getWorldFolder()} 在 26+ 上返回的是共享根目录，维度目录需用
 * {@code getWorldPath()}（若存在）或本工具解析。
 */
public final class WorldFiles {

    private WorldFiles() {
    }

    /**
     * 解析世界数据目录（已加载优先走 API；未加载则探测经典布局与 Paper26 布局）。
     */
    public static File resolveWorldDirectory(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return new File(Bukkit.getWorldContainer(), "unknown");
        }
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            File fromApi = directoryFromLoadedWorld(loaded);
            if (fromApi != null) {
                return fromApi;
            }
        }

        File classic = classicDirectory(worldName);
        if (classic.isDirectory()) {
            return classic;
        }

        File paper26 = paper26DimensionDirectory(worldName);
        if (paper26.isDirectory()) {
            return paper26;
        }

        // 尚未落盘：按当前服务端布局返回“预期路径”
        return usesPaper26Layout() ? paper26 : classic;
    }

    /**
     * 世界数据目录是否存在（兼容经典 / Paper26）。
     */
    public static boolean worldDirectoryExists(String worldName) {
        World loaded = Bukkit.getWorld(worldName);
        if (loaded != null) {
            File fromApi = directoryFromLoadedWorld(loaded);
            if (fromApi != null && fromApi.isDirectory()) {
                return true;
            }
        }
        return classicDirectory(worldName).isDirectory()
                || paper26DimensionDirectory(worldName).isDirectory();
    }

    /**
     * 递归删除文件/目录（用于删除世界文件夹）。
     */
    public static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }

    /**
     * 删除指定世界的数据目录（自动解析路径；不会误删 Paper26 共享根目录）。
     */
    public static boolean deleteWorldDirectory(String worldName) {
        File dir = resolveWorldDirectory(worldName);
        if (isUnsafeDeleteTarget(dir, worldName)) {
            return false;
        }
        return deleteRecursively(dir);
    }

    private static boolean isUnsafeDeleteTarget(File dir, String worldName) {
        if (dir == null) {
            return true;
        }
        // 禁止删到服务器根 / 世界容器根 / 主世界根
        File container = Bukkit.getWorldContainer();
        try {
            String target = dir.getCanonicalPath();
            if (target.equals(container.getCanonicalPath())) {
                return true;
            }
            if (!Bukkit.getWorlds().isEmpty()) {
                File root = Bukkit.getWorlds().get(0).getWorldFolder();
                if (root != null && target.equals(root.getCanonicalPath())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return true;
        }
        // Paper26 维度目录名应匹配世界名（小写）；经典目录名匹配原名
        String name = dir.getName();
        return !name.equals(worldName) && !name.equalsIgnoreCase(worldName);
    }

    private static File directoryFromLoadedWorld(World world) {
        // Paper 26+：getWorldPath() 指向 dimensions/<ns>/<key>
        try {
            Method m = world.getClass().getMethod("getWorldPath");
            Object path = m.invoke(world);
            if (path instanceof Path p) {
                return p.toFile();
            }
            if (path instanceof File f) {
                return f;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        File folder = world.getWorldFolder();
        if (folder == null) {
            return null;
        }

        // 26+：getWorldFolder() 常为共享根，真实数据在 dimensions/
        File dim = new File(folder, "dimensions/minecraft/" + world.getName().toLowerCase(Locale.ROOT));
        if (dim.isDirectory()) {
            return dim;
        }
        // 经典：folder 本身含 region / level.dat
        if (new File(folder, "region").isDirectory()
                || new File(folder, "level.dat").isFile()
                || folder.getName().equals(world.getName())) {
            return folder;
        }
        return dim;
    }

    private static File classicDirectory(String worldName) {
        return new File(Bukkit.getWorldContainer(), worldName);
    }

    /**
     * Paper/Folia 26+：{@code <mainLevel>/dimensions/minecraft/<worldName>/}
     */
    private static File paper26DimensionDirectory(String worldName) {
        File root = paper26SharedRoot();
        return new File(root, "dimensions/minecraft/" + worldName.toLowerCase(Locale.ROOT));
    }

    private static File paper26SharedRoot() {
        if (!Bukkit.getWorlds().isEmpty()) {
            // 26+ getWorldFolder() = 共享 level 根（含 dimensions/）
            File folder = Bukkit.getWorlds().get(0).getWorldFolder();
            if (folder != null) {
                return folder;
            }
        }
        return new File(Bukkit.getWorldContainer(), "world");
    }

    /**
     * 是否表现为 Paper26 存储布局（主世界根下存在 dimensions/）。
     */
    public static boolean usesPaper26Layout() {
        if (!Bukkit.getWorlds().isEmpty()) {
            File root = Bukkit.getWorlds().get(0).getWorldFolder();
            if (root != null && new File(root, "dimensions").isDirectory()) {
                return true;
            }
        }
        return new File(Bukkit.getWorldContainer(), "world/dimensions").isDirectory();
    }
}
