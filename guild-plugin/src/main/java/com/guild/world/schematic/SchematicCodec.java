package com.guild.world.schematic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** {@link SchematicData} ↔ {@code .gws}（GZIP + JSON）编解码。 */
public final class SchematicCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SchematicCodec() {
    }

    public static void write(Path file, SchematicData data) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer w = new OutputStreamWriter(
                new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(file))),
                StandardCharsets.UTF_8)) {
            GSON.toJson(data, w);
        }
    }

    public static SchematicData read(Path file) throws IOException {
        try (Reader r = new InputStreamReader(
                new GZIPInputStream(new BufferedInputStream(Files.newInputStream(file))),
                StandardCharsets.UTF_8)) {
            SchematicData data = GSON.fromJson(r, SchematicData.class);
            if (data == null) {
                throw new IOException("Empty schematic: " + file);
            }
            if (data.palette == null) {
                data.palette = new java.util.ArrayList<>();
            }
            if (data.blocks == null) {
                data.blocks = new java.util.ArrayList<>();
            }
            if (data.blockEntities == null) {
                data.blockEntities = new java.util.ArrayList<>();
            }
            if (data.origin == null) {
                data.origin = new Vec3i(0, 0, 0);
            }
            if (data.size == null) {
                data.size = new Size3i(1, 1, 1);
            }
            return data;
        }
    }
}
