package wtf.demise.features.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import wtf.demise.Demise;
import wtf.demise.features.config.impl.ModuleConfig;
import wtf.demise.features.config.impl.WidgetConfig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {
    private final ModuleConfig setting = new ModuleConfig("default");
    private final WidgetConfig elements = new WidgetConfig("elements");

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    @Setter
    private String currentConfig = "default";

    public ConfigManager() {
        loadConfigs();
    }

    public boolean loadConfig(Config config) {
        if (config == null) {
            Demise.LOGGER.warn("Attempted to load a null configuration.");
            return false;
        }

        try (InputStream in = new FileInputStream(config.getFile())) {
            // strip byte order mark if present to prevent parser crash
            byte[] bom = new byte[3];
            int read = in.read(bom, 0, 3);
            InputStream targetStream = in;
            if (read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                // bom skipped
            } else if (read > 0) {
                byte[] rest = new byte[read];
                System.arraycopy(bom, 0, rest, 0, read);
                targetStream = new SequenceInputStream(new ByteArrayInputStream(rest), in);
            }
            try (Reader reader = new InputStreamReader(targetStream, StandardCharsets.UTF_8)) {
                JsonParser parser = new JsonParser();
                JsonObject jsonObject = parser.parse(reader).getAsJsonObject();
                config.loadConfig(jsonObject);
                Demise.LOGGER.info("Loaded config: {}", config.getName());
                return true;
            }
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to load config: {}", config.getName(), e);
            return false;
        }
    }

    public boolean loadOnlineConfig(Config config, JsonObject jsonObject) {
        if (config == null || jsonObject == null) {
            Demise.LOGGER.warn("Config or JsonObject is null. Cannot load online config.");
            return false;
        }

        try {
            config.loadConfig(jsonObject);
            Demise.LOGGER.info("Loaded online config: {}", config.getName());
            return true;
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to load online config: {}", config.getName(), e);
            return false;
        }
    }

    public boolean saveConfig(Config config) {
        if (config == null) {
            Demise.LOGGER.warn("Attempted to save a null configuration.");
            return false;
        }

        JsonObject jsonObject = config.saveConfig();
        String jsonString = gson.toJson(jsonObject);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(config.getFile()), StandardCharsets.UTF_8)) {
            writer.write(jsonString);
            Demise.LOGGER.info("Saved config: {}", config.getName());
            return true;
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to save config: {}", config.getName(), e);
            return false;
        }
    }

    public void saveConfigs() {
        if (!saveConfig(setting)) {
            Demise.LOGGER.warn("Failed to save setting config.");
        }
        if (!saveConfig(elements)) {
            Demise.LOGGER.warn("Failed to save elements config.");
        }
    }

    public void loadConfigs() {
        if (!loadConfig(setting)) {
            Demise.LOGGER.warn("Failed to load setting config.");
        }
        if (!loadConfig(elements)) {
            Demise.LOGGER.warn("Failed to load elements config.");
        }
    }

    public List<String> getConfigList() {
        File directory = Demise.INSTANCE.getMainDir();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return List.of();
        }

        return Arrays.stream(files).filter(File::isFile).map(file -> file.getName().replace(".json", "")).collect(Collectors.toList());
    }
}