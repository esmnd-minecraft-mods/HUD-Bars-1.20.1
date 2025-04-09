package com.esmnd.modhud;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;


public class HUDColors {
    private static final String CONFIG_FILE = "config/esmnd_hud_config.json";
    private static HUDColors instance;

    // Цвета для баров (теперь без # и с правильным форматом)
    public static int absorptionBarColor = 0xFFFFD700; // Жёлтый
    public static int absorptionBarBackgroundColor = 0xFF806f00; // Тёмно-жёлтый
    private int healthBarColor = 0xFFFF0000;           // Красный
    private int healthBarBackgroundColor = 0xFF800000; // Тёмно-красный
    private int armorBarColor = 0xFFFFFFFF;           // Белый
    private int armorBarBackgroundColor = 0xFF808080; // Серый
    private int foodBarColor = 0xFF00FF00;            // Зелёный
    private int foodBarBackgroundColor = 0xFF008000;  // Тёмно-зелёный
    private int xpBarColor = 0xFF0000FF;             // Синий
    private int xpBarBackgroundColor = 0xFF000080;    // Тёмно-синий
    private int airBarColor = 0xFF87CEEB;            // Голубой
    private int airBarBackgroundColor = 0xFF005269;   // Тёмно-синий

    private double hudScaleFixed = 1.25f;

    // for Hardcore colors bar health
    private int hardcoreGradientOne = 0xFFFF00FF;           // Ярко-розовый
    private int hardcoreGradientTwo = 0xFFFF0000;           // Красный
    private int hardcoreGradientBackgroundColor = 0xFF800000; // Тёмно-красный

    // Геттеры для всех цветов
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

    // hardcore
    public int getHardcoreGradientOne() { return hardcoreGradientOne; }
    public int getHardcoreGradientTwo() { return hardcoreGradientTwo; }
    public int getHardcoreGradientBackgroundColor() { return hardcoreGradientBackgroundColor; }

    // use size vanilla UI
    private boolean FixedSizeHUD = false;

    // show percent all bars
    private boolean PercentBars = false;

    // pulse low health
    private esmndHUDConfigScreen.PulseLowHealthMode pulseMode = esmndHUDConfigScreen.PulseLowHealthMode.HARDCORE_ONLY;

    // Gradient Health Bar Mode
    private esmndHUDConfigScreen.GradientModeUse gradientMode = esmndHUDConfigScreen.GradientModeUse.HARDCORE_ONLY;

    // Сеттеры с правильным форматированием
    public void setHealthBarColor(int color) {
        this.healthBarColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setHealthBarBackgroundColor(int color) {
        this.healthBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setArmorBarColor(int color) {
        this.armorBarColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setArmorBarBackgroundColor(int color) {
        this.armorBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setFoodBarColor(int color) {
        this.foodBarColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setFoodBarBackgroundColor(int color) {
        this.foodBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setXpBarColor(int color) {
        this.xpBarColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setXpBarBackgroundColor(int color) {
        this.xpBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setAirBarColor(int color) {
        this.airBarColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }
    public void setAirBarBackgroundColor(int color) {
        this.airBarBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }

    // hardcore
    public void setHardcoreGradientOne(int color) {
        this.hardcoreGradientOne = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }

    public void setHardcoreGradientTwo(int color) {
        this.hardcoreGradientTwo = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }

    public void setHardcoreGradientBackgroundColor(int color) {
        this.hardcoreGradientBackgroundColor = 0xFF000000 | (color & 0xFFFFFF);
        saveConfig();
    }

    // Pulse low health
    public esmndHUDConfigScreen.PulseLowHealthMode getPulseMode() {
        return pulseMode;
    }

    public void setPulseMode(esmndHUDConfigScreen.PulseLowHealthMode mode) {
        if (mode != null)
            this.pulseMode = mode;
    }

    // Gradient Health Bar Mode
    public esmndHUDConfigScreen.GradientModeUse getGradientModeUse() {
            return gradientMode;
    }

    public void setGradientMode(esmndHUDConfigScreen.GradientModeUse mode) {
        if (mode != null)
            this.gradientMode = mode;
    }

    // size fixed HUD

    public boolean isFixedSizeHUD() {
        return FixedSizeHUD;
    }

    public void setFixedSizeHUD(boolean fixedSizeHUD) {
        FixedSizeHUD = fixedSizeHUD;
    }

    public void setHudScaleFixed(double hudScaleFixed) {
        this.hudScaleFixed = hudScaleFixed;
    }

    // Percent of all bars
    public boolean isPercentBars() {
        return PercentBars;
    }

    public void setPercentBars(boolean percentBars) {
        PercentBars = percentBars;
    }

    // Singleton получение экземпляра
    public static HUDColors getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    // Загрузка конфига с обработкой ошибок
    private static HUDColors loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            HUDColors defaultConfig = new HUDColors();
            defaultConfig.saveConfig();
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(configFile.toPath())) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(esmndHUDConfigScreen.PulseLowHealthMode.class, new PulseModeDeserializer())
                    .registerTypeAdapter(esmndHUDConfigScreen.GradientModeUse.class, new GradientModeDeserializer())
                    .create();
            return gson.fromJson(reader, HUDColors.class);
        } catch (Exception e) {
            System.err.println("Failed to load HUDColors configuration:");
            e.printStackTrace();
            return new HUDColors(); // При ошибке возвращаем дефолтные значения
        }
    }

    // Сохранение конфига
    public void saveConfig() {
        try {
            File configFile = new File(CONFIG_FILE);
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Создаем директорию, если её нет
            }

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(esmndHUDConfigScreen.PulseLowHealthMode.class, new PulseModeSerializer())
                    .registerTypeAdapter(esmndHUDConfigScreen.GradientModeUse.class, new GradientModeSerializer())
                    .create();
            String json = gson.toJson(this);

            Files.writeString(configFile.toPath(), json);
        } catch (IOException e) {
            System.err.println("Failed to save HUDColors configuration:");
            e.printStackTrace();
        }
    }

    public static class PulseModeSerializer implements JsonSerializer<esmndHUDConfigScreen.PulseLowHealthMode> {
        @Override
        public JsonElement serialize(esmndHUDConfigScreen.PulseLowHealthMode src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name()); // Сохраняем имя Enum как строку
        }
    }

    public static class PulseModeDeserializer implements JsonDeserializer<esmndHUDConfigScreen.PulseLowHealthMode> {
        @Override
        public esmndHUDConfigScreen.PulseLowHealthMode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return esmndHUDConfigScreen.PulseLowHealthMode.valueOf(json.getAsString()); // Преобразуем строку обратно в Enum
            } catch (IllegalArgumentException | NullPointerException e) {
                return esmndHUDConfigScreen.PulseLowHealthMode.HARDCORE_ONLY; // Значение по умолчанию в случае ошибки
            }
        }
    }

    public static class GradientModeSerializer implements JsonSerializer<esmndHUDConfigScreen.GradientModeUse> {
        @Override
        public JsonElement serialize(esmndHUDConfigScreen.GradientModeUse src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name()); // Сохраняем имя Enum как строку
        }
    }

    public static class GradientModeDeserializer implements JsonDeserializer<esmndHUDConfigScreen.GradientModeUse> {
        @Override
        public esmndHUDConfigScreen.GradientModeUse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                return esmndHUDConfigScreen.GradientModeUse.valueOf(json.getAsString()); // Преобразуем строку обратно в Enum
            } catch (IllegalArgumentException | NullPointerException e) {
                return esmndHUDConfigScreen.GradientModeUse.HARDCORE_ONLY; // Значение по умолчанию в случае ошибки
            }
        }
    }

    public static class DoubleSerializer implements JsonSerializer<Double> {
        @Override
        public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
            // Округляем до 2 знаков после запятой
            double rounded = Math.round(src * 100.0) / 100.0;
            return new JsonPrimitive(rounded);
        }
    }
}