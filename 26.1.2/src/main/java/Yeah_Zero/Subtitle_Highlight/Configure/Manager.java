package Yeah_Zero.Subtitle_Highlight.Configure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

public class Manager {
    public static final File file = new File(FabricLoader.getInstance().getConfigDir().toFile().getPath(), "subtitle_highlight.json");
    public static Settings settings;
    public static Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();

    public static void save() {
        // 先写临时文件再原子替换，避免写入过程中崩溃导致配置文件被截断为空。
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileWriter writer = new FileWriter(temp, StandardCharsets.UTF_8, false)) {
            writer.write(gson.toJson(settings));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load() {
        if (!file.exists() || !file.isFile()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            settings = new Settings();
            save();
            return;
        }
        Settings loaded;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            char[] content = new char[(int) file.length()];
            int read = reader.read(content);
            String json = read > 0 ? new String(content, 0, read).trim() : "";
            loaded = json.isEmpty() ? null : gson.fromJson(json, Settings.class);
        } catch (IOException | JsonSyntaxException e) {
            loaded = null;
        }
        settings = withDefaults(loaded);
        if (settings != loaded) {
            // 配置文件为空或无法解析，重建为默认配置
            save();
        }
    }

    private static Settings withDefaults(Settings loaded) {
        if (loaded == null) {
            return new Settings();
        }
        fillMissingValues(loaded, new Settings());
        return loaded;
    }

    /**
     * 用默认值补齐反序列化后为 null 的字段，覆盖旧版本配置缺少新字段、手改配置写错枚举名等情况，
     * 避免渲染路径上的空指针。
     */
    private static void fillMissingValues(Object target, Object defaults) {
        if (target == null || defaults == null) {
            return;
        }
        for (Field field : target.getClass().getFields()) {
            try {
                Object value = field.get(target);
                if (value == null) {
                    field.set(target, field.get(defaults));
                } else if (isNestedConfigType(field.getType())) {
                    fillMissingValues(value, field.get(defaults));
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // 单个字段补全失败不影响其余字段
            }
        }
    }

    private static boolean isNestedConfigType(Class<?> type) {
        return !type.isPrimitive()
                && !type.isEnum()
                && !type.isArray()
                && !Collection.class.isAssignableFrom(type)
                && !type.getName().startsWith("net.minecraft")
                && !type.getName().startsWith("java.");
    }

    public static void exportConfig(File exportFile) {
        try (FileWriter writer = new FileWriter(exportFile, StandardCharsets.UTF_8, false)) {
            writer.write(gson.toJson(settings));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void importConfig(File importFile) {
        if (!importFile.exists() || !importFile.isFile()) {
            throw new IllegalArgumentException("Import file does not exist or is not a file");
        }
        Settings loaded;
        try (FileReader reader = new FileReader(importFile, StandardCharsets.UTF_8)) {
            char[] content = new char[(int) importFile.length()];
            int read = reader.read(content);
            String json = read > 0 ? new String(content, 0, read).trim() : "";
            if (json.isEmpty()) {
                throw new IllegalArgumentException("Import file is empty");
            }
            try {
                loaded = gson.fromJson(json, Settings.class);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("Invalid configuration file format", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (loaded == null) {
            throw new IllegalArgumentException("Invalid configuration file format");
        }
        settings = withDefaults(loaded);
        save();
    }
}