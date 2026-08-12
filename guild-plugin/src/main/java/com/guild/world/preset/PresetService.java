package com.guild.world.preset;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 预设仓库：{@code <name>.yml} 元数据 + {@code <name>.gws} schematic。
 */
public class PresetService {

    /** 相对 schematic.origin 的偏移（用于 spawn A/B/观众）。 */
    public record Anchor(double dx, double dy, double dz, float yaw, float pitch) {
        public static Anchor parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String[] p = raw.split(",");
            try {
                double x = Double.parseDouble(p[0].trim());
                double y = Double.parseDouble(p[1].trim());
                double z = Double.parseDouble(p[2].trim());
                float yaw = p.length >= 5 ? Float.parseFloat(p[3].trim()) : 0f;
                float pitch = p.length >= 5 ? Float.parseFloat(p[4].trim()) : 0f;
                return new Anchor(x, y, z, yaw, pitch);
            } catch (Exception e) {
                return null;
            }
        }

        public String serialize() {
            return dx + "," + dy + "," + dz + "," + yaw + "," + pitch;
        }
    }

    public record PresetMeta(
            String name,
            String sourceWorld,
            String createdBy,
            long createdAt,
            String note,
            boolean hasSchematic,
            int sizeX,
            int sizeY,
            int sizeZ,
            int blockEntities,
            /** schematic 粘贴默认世界锚点（绝对坐标字符串，可空） */
            String pasteOrigin,
            Anchor spawnA,
            Anchor spawnB,
            Anchor spectator
    ) {
    }

    private final File presetDir;
    private final Logger logger;

    public PresetService(File worldsDir, Logger logger) {
        this.presetDir = new File(worldsDir, "presets");
        this.logger = logger;
        if (!presetDir.exists() && !presetDir.mkdirs()) {
            logger.warning("[World] Failed to create presets directory: " + presetDir);
        }
    }

    public boolean exists(String name) {
        return yamlFile(name).isFile();
    }

    public boolean hasSchematicFile(String name) {
        return gwsFile(name).isFile();
    }

    public File gwsFile(String name) {
        return new File(presetDir, normalize(name) + ".gws");
    }

    public PresetMeta get(String name) {
        File file = yamlFile(name);
        if (!file.isFile()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean schem = yaml.getBoolean("schematic", false) || gwsFile(name).isFile();
        return new PresetMeta(
                normalize(name),
                yaml.getString("source-world", ""),
                yaml.getString("created-by", ""),
                yaml.getLong("created-at", file.lastModified()),
                yaml.getString("note", ""),
                schem,
                yaml.getInt("size.x", 0),
                yaml.getInt("size.y", 0),
                yaml.getInt("size.z", 0),
                yaml.getInt("block-entities", 0),
                yaml.getString("paste-origin", ""),
                Anchor.parse(yaml.getString("spawn-a", "")),
                Anchor.parse(yaml.getString("spawn-b", "")),
                Anchor.parse(yaml.getString("spectator", ""))
        );
    }

    public Collection<PresetMeta> list() {
        List<PresetMeta> list = new ArrayList<>();
        File[] files = presetDir.listFiles((dir, n) -> n.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return list;
        }
        for (File file : files) {
            String name = file.getName();
            name = name.substring(0, name.length() - 4);
            PresetMeta meta = get(name);
            if (meta != null) {
                list.add(meta);
            }
        }
        list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return list;
    }

    public PresetMeta saveMeta(String name, String sourceWorld, String createdBy, String note,
                               boolean hasSchematic, int sx, int sy, int sz, int blockEntities,
                               String pasteOrigin, Anchor spawnA, Anchor spawnB, Anchor spectator) {
        String normalized = normalize(name);
        validateName(normalized);
        File file = yamlFile(normalized);
        YamlConfiguration yaml = file.isFile()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        long now = System.currentTimeMillis();
        if (!yaml.contains("created-at")) {
            yaml.set("created-at", now);
        }
        yaml.set("name", normalized);
        yaml.set("source-world", nullToEmpty(sourceWorld));
        yaml.set("created-by", nullToEmpty(createdBy));
        yaml.set("updated-at", now);
        yaml.set("note", nullToEmpty(note));
        yaml.set("schematic", hasSchematic);
        yaml.set("size.x", sx);
        yaml.set("size.y", sy);
        yaml.set("size.z", sz);
        yaml.set("block-entities", blockEntities);
        yaml.set("paste-origin", nullToEmpty(pasteOrigin));
        yaml.set("spawn-a", spawnA == null ? "" : spawnA.serialize());
        yaml.set("spawn-b", spawnB == null ? "" : spawnB.serialize());
        yaml.set("spectator", spectator == null ? "" : spectator.serialize());
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[World] Failed to save preset '" + normalized + "'", e);
            throw new IllegalStateException("Failed to save preset: " + e.getMessage(), e);
        }
        return get(normalized);
    }

    /** 兼容旧调用：仅写元数据。 */
    public PresetMeta save(String name, String sourceWorld, String createdBy, String note) {
        PresetMeta old = get(name);
        return saveMeta(name, sourceWorld, createdBy, note,
                old != null && old.hasSchematic(),
                old == null ? 0 : old.sizeX(),
                old == null ? 0 : old.sizeY(),
                old == null ? 0 : old.sizeZ(),
                old == null ? 0 : old.blockEntities(),
                old == null ? "" : old.pasteOrigin(),
                old == null ? null : old.spawnA(),
                old == null ? null : old.spawnB(),
                old == null ? null : old.spectator());
    }

    public boolean delete(String name) {
        boolean ok = true;
        File yml = yamlFile(name);
        File gws = gwsFile(name);
        if (yml.exists() && !yml.delete()) {
            ok = false;
        }
        if (gws.exists() && !gws.delete()) {
            ok = false;
        }
        return ok;
    }

    public File getPresetDir() {
        return presetDir;
    }

    private File yamlFile(String name) {
        return new File(presetDir, normalize(name) + ".yml");
    }

    private static void validateName(String normalized) {
        if (normalized.isEmpty() || !normalized.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("Preset name must be [a-zA-Z0-9_-]{1,64}");
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @Override
    public String toString() {
        return "PresetService{dir=" + Objects.toString(presetDir) + "}";
    }
}
