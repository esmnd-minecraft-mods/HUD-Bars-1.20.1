package com.esmnd.hud;

import com.google.gson.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HUDColors {
    // --- ИЗМЕНЕНО: Путь к конфигу лучше получать через Forge, но пока оставим так ---
    // Forge 1.12.2 обычно предоставляет папку в FMLPreInitializationEvent
    private static final String CONFIG_FILE_NAME = "esmnd_hud_config.json";
    // Полный путь будет формироваться позже, чтобы можно было использовать папку конфига от Forge
    private static File configFile;

    private static HUDColors instance;

    // --- ИЗМЕНЕНО: Enum'ы перенесены внутрь HUDColors ---
    public enum PulseLowHealthMode {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;
        // Метод getDisplayName() здесь не нужен, он относится к GUI
    }

    public enum GradientModeUse {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;
        // Метод getDisplayName() здесь не нужен
    }
    // --- КОНЕЦ ПЕРЕНОСА ENUM ---


    // --- Поля конфига (остаются без изменений) ---
    public static int absorptionBarColor = 0xFFFFD700;
    public static int absorptionBarBackgroundColor = 0xFF806f00;
    private int healthBarColor = 0xFFFF0000;
    private int healthBarBackgroundColor = 0xFF800000;
    private int armorBarColor = 0xFFFFFFFF;
    private int armorBarBackgroundColor = 0xFF808080;
    private int foodBarColor = 0xFF00FF00;
    private int foodBarBackgroundColor = 0xFF008000;
    private int xpBarColor = 0xFF0000FF;
    private int xpBarBackgroundColor = 0xFF000080;
    private int airBarColor = 0xFF87CEEB;
    private int airBarBackgroundColor = 0xFF005269;

    private double hudScaleFixed = 1.25; // double вместо float

    private int hardcoreGradientOne = 0xFFFF00FF;
    private int hardcoreGradientTwo = 0xFFFF0000;
    private int hardcoreGradientBackgroundColor = 0xFF800000;

    private boolean FixedSizeHUD = false;
    private boolean PercentBars = false;

    // Используем перенесенные Enum'ы
    private PulseLowHealthMode pulseMode = PulseLowHealthMode.HARDCORE_ONLY;
    private GradientModeUse gradientMode = GradientModeUse.HARDCORE_ONLY;


    // --- Геттеры (остаются без изменений) ---
    public int getHealthBarColor() { return healthBarColor; }
    public int getHealthBarBackgroundColor() { return healthBarBackgroundColor; }
    public int getArmorBarColor() { return armorBarColor; }
    public int getArmorBarBackgroundColor() { return armorBarBackgroundColor; }
    public int getFoodBarColor() { return foodBarColor; }
    public int getFoodBarBackgroundColor() { return foodBarBackgroundColor; }
    public int getXpBarColor() { return xpBarColor; }
    public int getXpBarBackgroundColor() { return xpBarBackgroundColor; }
    public int getAirBarColor() { return airBarColor; }
    public int getAirBarBackgroundColor() { return airBarBackgroundColor; }
    public double getHudScaleFixed() { return hudScaleFixed; }
    public int getHardcoreGradientOne() { return hardcoreGradientOne; }
    public int getHardcoreGradientTwo() { return hardcoreGradientTwo; }
    public int getHardcoreGradientBackgroundColor() { return hardcoreGradientBackgroundColor; }
    public boolean isFixedSizeHUD() { return FixedSizeHUD; }
    public boolean isPercentBars() { return PercentBars; }
    public PulseLowHealthMode getPulseMode() { return pulseMode; }
    public GradientModeUse getGradientModeUse() { return gradientMode; }

    // --- Сеттеры (добавлен вызов saveConfig() и используется this) ---
    public void setHealthBarColor(int color) { this.healthBarColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setHealthBarBackgroundColor(int color) { this.healthBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setArmorBarColor(int color) { this.armorBarColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setArmorBarBackgroundColor(int color) { this.armorBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setFoodBarColor(int color) { this.foodBarColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setFoodBarBackgroundColor(int color) { this.foodBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setXpBarColor(int color) { this.xpBarColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setXpBarBackgroundColor(int color) { this.xpBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setAirBarColor(int color) { this.airBarColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setAirBarBackgroundColor(int color) { this.airBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setHardcoreGradientOne(int color) { this.hardcoreGradientOne = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setHardcoreGradientTwo(int color) { this.hardcoreGradientTwo = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setHardcoreGradientBackgroundColor(int color) { this.hardcoreGradientBackgroundColor = 0xFF000000 | (color & 0xFFFFFF); saveConfig(); }
    public void setPulseMode(PulseLowHealthMode mode) { if (mode != null) this.pulseMode = mode; saveConfig(); } // Добавлено saveConfig()
    public void setGradientMode(GradientModeUse mode) { if (mode != null) this.gradientMode = mode; saveConfig(); } // Добавлено saveConfig()
    public void setFixedSizeHUD(boolean fixedSizeHUD) { this.FixedSizeHUD = fixedSizeHUD; saveConfig(); } // Добавлено saveConfig()
    public void setHudScaleFixed(double hudScaleFixed) { this.hudScaleFixed = hudScaleFixed; saveConfig(); } // Добавлено saveConfig()
    public void setPercentBars(boolean percentBars) { this.PercentBars = percentBars; saveConfig(); } // Добавлено saveConfig()


    // --- Инициализация и Singleton ---

    /**
     * Этот метод нужно вызвать из FMLPreInitializationEvent вашего главного класса мода,
     * передав предложенный файл конфигурации Forge.
     */
    public static void init(File suggestedConfigFile) {
        configFile = suggestedConfigFile; // Устанавливаем путь к файлу
        getInstance(); // Загружаем или создаем конфиг
    }

    public static HUDColors getInstance() {
        if (instance == null) {
            if (configFile == null) {
                // Если init не был вызван, используем путь по умолчанию (менее предпочтительно)
                System.err.println("[esmndnewhud] WARN: HUDColors.init() was not called! Using default config path.");
                configFile = new File("config", CONFIG_FILE_NAME);
            }
            instance = loadConfig();
        }
        return instance;
    }

    // --- Загрузка и Сохранение ---

    private static HUDColors loadConfig() {
        if (!configFile.exists()) {
            System.out.println("[esmndnewhud] Config file not found, creating default: " + configFile.getAbsolutePath());
            HUDColors defaultConfig = new HUDColors();
            defaultConfig.saveConfig(); // Сохраняем дефолтный конфиг
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(configFile.toPath(), StandardCharsets.UTF_8)) {
            // --- ИЗМЕНЕНО: Ссылки на Enum'ы в адаптерах ---
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(PulseLowHealthMode.class, new PulseModeDeserializer()) // Используем локальный Enum
                    .registerTypeAdapter(GradientModeUse.class, new GradientModeDeserializer()) // Используем локальный Enum
                    // .registerTypeAdapter(Double.class, new DoubleSerializer()) // Можно раскомментировать, если нужен кастомный сериализатор double
                    .create();
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            HUDColors loadedConfig = gson.fromJson(reader, HUDColors.class);
            if (loadedConfig == null) { // Если файл пустой или некорректный
                System.err.println("[esmndnewhud] Failed to parse config file, using defaults.");
                return new HUDColors();
            }
            return loadedConfig;
        } catch (Exception e) {
            System.err.println("[esmndnewhud] Failed to load HUDColors configuration, using defaults:");
            e.printStackTrace();
            // Создаем резервную копию поврежденного файла
            File backupFile = new File(configFile.getAbsolutePath() + ".bak");
            try {
                Files.copy(configFile.toPath(), backupFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[esmndnewhud] Corrupted config backed up to: " + backupFile.getName());
            } catch (IOException ioex) {
                System.err.println("[esmndnewhud] Could not create backup of corrupted config.");
                ioex.printStackTrace();
            }
            return new HUDColors(); // При ошибке возвращаем дефолтные значения
        }
    }

    public void saveConfig() {
        if (configFile == null) {
            System.err.println("[esmndnewhud] ERROR: Cannot save config, file path is not initialized! Call HUDColors.init() first.");
            return;
        }
        try {
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    System.err.println("[esmndnewhud] ERROR: Could not create config directory: " + parentDir.getAbsolutePath());
                    return; // Не можем сохранить, если директорию не создать
                }
            }

            // --- ИЗМЕНЕНО: Ссылки на Enum'ы в адаптерах ---
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(PulseLowHealthMode.class, new PulseModeSerializer()) // Используем локальный Enum
                    .registerTypeAdapter(GradientModeUse.class, new GradientModeSerializer()) // Используем локальный Enum
                    // .registerTypeAdapter(Double.class, new DoubleSerializer()) // Можно раскомментировать
                    .create();
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            String json = gson.toJson(this);

            // --- ИЗМЕНЕНО: Запись файла через BufferedWriter (Java 8 совместимо) ---
            try (BufferedWriter writer = Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---

        } catch (Exception e) { // Ловим Exception для большей надежности
            System.err.println("[esmndnewhud] Failed to save HUDColors configuration:");
            e.printStackTrace();
        }
    }

    // --- Сериализаторы/Десериализаторы (ссылаются на внутренние Enum'ы) ---

    public static class PulseModeSerializer implements JsonSerializer<PulseLowHealthMode> { // <--- ИЗМЕНЕНО
        @Override
        public JsonElement serialize(PulseLowHealthMode src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    }

    public static class PulseModeDeserializer implements JsonDeserializer<PulseLowHealthMode> { // <--- ИЗМЕНЕНО
        @Override
        public PulseLowHealthMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return PulseLowHealthMode.valueOf(json.getAsString()); // <--- ИЗМЕНЕНО
            } catch (Exception e) { // Ловим шире ошибки
                System.err.println("[esmndnewhud] Failed to parse PulseLowHealthMode '" + json.getAsString() + "', using default.");
                return PulseLowHealthMode.HARDCORE_ONLY; // <--- ИЗМЕНЕНО
            }
        }
    }

    public static class GradientModeSerializer implements JsonSerializer<GradientModeUse> { // <--- ИЗМЕНЕНО
        @Override
        public JsonElement serialize(GradientModeUse src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    }

    public static class GradientModeDeserializer implements JsonDeserializer<GradientModeUse> { // <--- ИЗМЕНЕНО
        @Override
        public GradientModeUse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return GradientModeUse.valueOf(json.getAsString()); // <--- ИЗМЕНЕНО
            } catch (Exception e) {
                System.err.println("[esmndnewhud] Failed to parse GradientModeUse '" + json.getAsString() + "', using default.");
                return GradientModeUse.HARDCORE_ONLY; // <--- ИЗМЕНЕНО
            }
        }
    }

    // Сериализатор для Double (опционально, если стандартный не устраивает)
    /*
    public static class DoubleSerializer implements JsonSerializer<Double> {
        @Override
        public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
            double rounded = Math.round(src * 100.0) / 100.0;
            return new JsonPrimitive(rounded);
        }
    }
    */
}
