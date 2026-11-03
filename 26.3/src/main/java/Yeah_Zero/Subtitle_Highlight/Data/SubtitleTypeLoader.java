package Yeah_Zero.Subtitle_Highlight.Data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import Yeah_Zero.Subtitle_Highlight.Util.ColorCode;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SubtitleTypeLoader implements PreparableReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("subtitle_highlight", "subtitle_types");
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath("subtitle_highlight", "subtitle_types.json");
    private static final Gson GSON = new Gson();

    private static volatile Map<String, Map<String, ColorCode>> subtitleTypes = new HashMap<>();

    private static final ConcurrentHashMap<String, ColorCode> colorCache = new ConcurrentHashMap<>();
    private static final String CACHE_KEY_SEPARATOR = ":";

    private static final ColorCode NULL_MARKER = ColorCode.GRAY;

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor, PreparationBarrier barrier, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> prepare(sharedState.resourceManager()), prepareExecutor)
                .thenCompose(prepared -> barrier.wait(prepared))
                .thenAcceptAsync(prepared -> apply(prepared, sharedState.resourceManager()), applyExecutor);
    }

    private Map<String, Map<String, ColorCode>> prepare(ResourceManager manager) {
        Map<String, Map<String, ColorCode>> loaded = new HashMap<>();
        try {
            Optional<Resource> resource = manager.getResource(DATA_ID);
            if (resource.isEmpty()) {
                return loadDefaultSubtitleTypes();
            }
            try (InputStream inputStream = resource.get().open();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
                if (jsonObject == null || !jsonObject.has("subtitle_types")) {
                    return loadDefaultSubtitleTypes();
                }
                loadSubtitleTypes(jsonObject.getAsJsonObject("subtitle_types"), "", loaded);
            }
            return loaded;
        } catch (Exception e) {
            e.printStackTrace();
            return loadDefaultSubtitleTypes();
        }
    }

    private void apply(Map<String, Map<String, ColorCode>> prepared, ResourceManager manager) {
        subtitleTypes = prepared;
        invalidateCache();
    }

    private void loadSubtitleTypes(JsonObject jsonObject, String path, Map<String, Map<String, ColorCode>> result) {
        for (String key : jsonObject.keySet()) {
            String currentPath = path.isEmpty() ? key : path + "." + key;
            if (jsonObject.get(key).isJsonObject()) {
                JsonObject nestedObject = jsonObject.getAsJsonObject(key);
                if (nestedObject.has("color")) {
                    String colorName = nestedObject.get("color").getAsString();
                    ColorCode colorCode = ColorCode.fromName(colorName);
                    if (colorCode != null) {
                        String parentPath = path.isEmpty() ? "" : path;
                        if (!result.containsKey(parentPath)) {
                            result.put(parentPath, new HashMap<>());
                        }
                        result.get(parentPath).put(key, colorCode);
                    }
                } else {
                    loadSubtitleTypes(nestedObject, currentPath, result);
                }
            }
        }
    }

    private static Map<String, Map<String, ColorCode>> loadDefaultSubtitleTypes() {
        Map<String, Map<String, ColorCode>> result = new HashMap<>();

        Map<String, ColorCode> ambientMap = new HashMap<>();
        ambientMap.put("ambient", ColorCode.DARK_BLUE);
        result.put("", ambientMap);

        Map<String, ColorCode> blockMap = new HashMap<>();
        blockMap.put("generic", ColorCode.GRAY);
        blockMap.put("interact", ColorCode.GREEN);
        blockMap.put("working", ColorCode.YELLOW);
        blockMap.put("dangerous", ColorCode.RED);
        blockMap.put("crop", ColorCode.GREEN);
        blockMap.put("other", ColorCode.GRAY);
        result.put("block", blockMap);

        Map<String, ColorCode> enchantMap = new HashMap<>();
        enchantMap.put("enchant", ColorCode.LIGHT_PURPLE);
        result.put("", enchantMap);

        Map<String, ColorCode> entityMobPlayerMap = new HashMap<>();
        entityMobPlayerMap.put("attack", ColorCode.RED);
        entityMobPlayerMap.put("hurt", ColorCode.RED);
        entityMobPlayerMap.put("other", ColorCode.WHITE);
        result.put("entity.mob.player", entityMobPlayerMap);

        Map<String, ColorCode> entityMobMap = new HashMap<>();
        entityMobMap.put("passive", ColorCode.GREEN);
        entityMobMap.put("neutral", ColorCode.YELLOW);
        entityMobMap.put("hostile", ColorCode.RED);
        entityMobMap.put("boss", ColorCode.DARK_PURPLE);
        result.put("entity.mob", entityMobMap);

        Map<String, ColorCode> entityMap = new HashMap<>();
        entityMap.put("vehicle", ColorCode.GRAY);
        entityMap.put("projectile", ColorCode.AQUA);
        entityMap.put("explosive", ColorCode.RED);
        entityMap.put("decoration", ColorCode.GRAY);
        entityMap.put("other", ColorCode.GRAY);
        result.put("entity", entityMap);

        Map<String, ColorCode> itemMap = new HashMap<>();
        itemMap.put("weapon", ColorCode.RED);
        itemMap.put("armor", ColorCode.GOLD);
        itemMap.put("tool", ColorCode.AQUA);
        itemMap.put("other", ColorCode.GRAY);
        result.put("item", itemMap);

        Map<String, ColorCode> otherMap = new HashMap<>();
        otherMap.put("other", ColorCode.GRAY);
        result.put("", otherMap);

        return result;
    }

    public static ColorCode getColor(String path, String key) {
        String cacheKey = buildCacheKey(path, key);

        ColorCode cached = colorCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_MARKER ? null : cached;
        }

        Map<String, ColorCode> map = subtitleTypes.get(path);
        ColorCode result = (map != null) ? map.get(key) : null;

        colorCache.put(cacheKey, result != null ? result : NULL_MARKER);

        return result;
    }

    private static String buildCacheKey(String path, String key) {
        return path + CACHE_KEY_SEPARATOR + key;
    }

    public static void invalidateCache() {
        colorCache.clear();
    }

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ID, new SubtitleTypeLoader());
    }
}
