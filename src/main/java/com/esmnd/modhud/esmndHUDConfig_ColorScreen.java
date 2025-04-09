package com.esmnd.modhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;

public class esmndHUDConfig_ColorScreen extends Screen {

    private final Screen lastScreen;
    private final HUDColors config;

    // EditBox компоненты
    private EditBox healthBarColorBox;
    private EditBox healthBarBgColorBox;
    private EditBox armorBarColorBox;
    private EditBox armorBarBgColorBox;
    private EditBox foodBarColorBox;
    private EditBox foodBarBgColorBox;
    private EditBox xpBarColorBox;
    private EditBox xpBarBgColorBox;
    private EditBox airBarColorBox;
    private EditBox airBarBgColorBox;

    public esmndHUDConfig_ColorScreen(Screen lastScreen) {
        super(Component.translatable("esmndnewhud.configscreencolor.title"));
        this.lastScreen = lastScreen;
        this.config = HUDColors.getInstance();
    }

    @Override
    protected void init() {
        int leftCol = this.width / 4;
        int rightCol = (this.width / 4) * 3;
        int yStart = 40;
        int ySpacing = 25;

        // Основные цвета (левая колонка)
        healthBarColorBox = createColorField(leftCol, yStart, "Health Bar",
                String.format("%06X", config.getHealthBarColor() & 0xFFFFFF));

        armorBarColorBox = createColorField(leftCol, yStart + ySpacing, "Armor Bar",
                String.format("%06X", config.getArmorBarColor() & 0xFFFFFF));

        foodBarColorBox = createColorField(leftCol, yStart + ySpacing * 2, "Food Bar",
                String.format("%06X", config.getFoodBarColor() & 0xFFFFFF));

        xpBarColorBox = createColorField(leftCol, yStart + ySpacing * 3, "XP Bar",
                String.format("%06X", config.getXpBarColor() & 0xFFFFFF));

        airBarColorBox = createColorField(leftCol, yStart + ySpacing * 4, "Air Bar",
                String.format("%06X", config.getAirBarColor() & 0xFFFFFF));

        // Фоновые цвета (правая колонка)
        healthBarBgColorBox = createColorField(rightCol, yStart, "Health Bar BG",
                String.format("%06X", config.getHealthBarBackgroundColor() & 0xFFFFFF));

        armorBarBgColorBox = createColorField(rightCol, yStart + ySpacing, "Armor Bar BG",
                String.format("%06X", config.getArmorBarBackgroundColor() & 0xFFFFFF));

        foodBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 2, "Food Bar BG",
                String.format("%06X", config.getFoodBarBackgroundColor() & 0xFFFFFF));

        xpBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 3, "XP Bar BG",
                String.format("%06X", config.getXpBarBackgroundColor() & 0xFFFFFF));

        airBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 4, "Air Bar BG",
                String.format("%06X", config.getAirBarBackgroundColor() & 0xFFFFFF));

        // Кнопки управления
        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.save"), this::saveChanges)
                .pos(this.width / 2 - 154, this.height - 28)
                .size(100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.return"), button ->
                        this.minecraft.setScreen(lastScreen))
                .pos(this.width / 2 - 50, this.height - 28)
                .size(100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.reset"), this::resetToDefault)
                .pos(this.width / 2 + 54, this.height - 28)
                .size(100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.changegradientcolor"), button ->  Minecraft.getInstance().setScreen(new esmndHUDConfig_ColorHardcore(this)))
                .pos(this.width / 2 - 50, this.height - 64)
                .size(100, 20)
                .build());

        // Добавляем все поля ввода в список активных виджетов
        setInitialFocus(healthBarColorBox);
    }

    private EditBox createColorField(int x, int y, String label, String defaultValue) {
        // Создаем поле ввода
        EditBox editBox = new EditBox(this.font, x, y, 70, 20, Component.empty());
        editBox.setValue(defaultValue);
        editBox.setMaxLength(6);
        editBox.setFilter(text -> text.matches("[0-9A-Fa-f]*"));
        editBox.setVisible(true);
        editBox.setEditable(true);

        // Важно! Добавляем поле в список рендеринга
        addRenderableWidget(editBox);

        return editBox;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Рендерим все EditBox'ы и другие виджеты
        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Рендерим цветовые превью для каждого поля
        renderColorPreview(poseStack, healthBarColorBox);
        renderColorPreview(poseStack, healthBarBgColorBox);
        renderColorPreview(poseStack, armorBarColorBox);
        renderColorPreview(poseStack, armorBarBgColorBox);
        renderColorPreview(poseStack, foodBarColorBox);
        renderColorPreview(poseStack, foodBarBgColorBox);
        renderColorPreview(poseStack, xpBarColorBox);
        renderColorPreview(poseStack, xpBarBgColorBox);
        renderColorPreview(poseStack, airBarColorBox);
        renderColorPreview(poseStack, airBarBgColorBox);

        /*

        if (Minecraft.getInstance().options.forceUnicodeFont().get() == Boolean.TRUE) {
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealth").getString(), healthBarColorBox.getX() - 105, healthBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmor").getString(), armorBarColorBox.getX() - 105, armorBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfood").getString(), foodBarColorBox.getX() - 105, foodBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxp").getString(), xpBarColorBox.getX() - 105, xpBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barair").getString(), airBarColorBox.getX() - 105, airBarColorBox.getY() + 6, 0xFFFFFF);

            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.getX() - 105, healthBarBgColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmorbg").getString(), armorBarBgColorBox.getX() - 105, armorBarBgColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfoodbg").getString(), foodBarBgColorBox.getX() - 105, foodBarBgColorBox.getY() + 6, 0xFFFFFF);

            if (Minecraft.getInstance().getLanguageManager().getSelected().equals("ru_ru")) {
                drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.getX() - 105, xpBarBgColorBox.getY() + 6, 0xFFFFFF);
            } else if (Minecraft.getInstance().getLanguageManager().getSelected().equals("en_us")) {
                drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.getX() - font.width(Component.translatable("esmndnewhud.screen.barxpbg").getString()), xpBarBgColorBox.getY() + 6, 0xFFFFFF);
            }

            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barairbg").getString(), airBarBgColorBox.getX() - 105, airBarBgColorBox.getY() + 6, 0xFFFFFF);
        }
        else if (Minecraft.getInstance().options.forceUnicodeFont().get() == Boolean.FALSE) {
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealth").getString(), healthBarColorBox.getX() - 135, healthBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmor").getString(), armorBarColorBox.getX() - 135, armorBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfood").getString(), foodBarColorBox.getX() - 135, foodBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxp").getString(), xpBarColorBox.getX() - 135, xpBarColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barair").getString(), airBarColorBox.getX() - 135, airBarColorBox.getY() + 6, 0xFFFFFF);

            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.getX() - 135, healthBarBgColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmorbg").getString(), armorBarBgColorBox.getX() - 135, armorBarBgColorBox.getY() + 6, 0xFFFFFF);
            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfoodbg").getString(), foodBarBgColorBox.getX() - 135, foodBarBgColorBox.getY() + 6, 0xFFFFFF);

            if (Minecraft.getInstance().getLanguageManager().getSelected().equals("ru_ru")) {
                drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.getX() - 135, xpBarBgColorBox.getY() + 6, 0xFFFFFF);
            } else if (Minecraft.getInstance().getLanguageManager().getSelected().equals("en_us")) {
                drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.getX() - font.width(Component.translatable("esmndnewhud.screen.barxpbg").getString()), xpBarBgColorBox.getY() + 6, 0xFFFFFF);
            }

            drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barairbg").getString(), airBarBgColorBox.getX() - 135, airBarBgColorBox.getY() + 6, 0xFFFFFF);
        }

         */

        // --- НАЧАЛО ИСПРАВЛЕННОГО БЛОКА ОТРИСОВКИ ЛЕЙБЛОВ ---
        // *** НОВОЕ: Определяем отступ в зависимости от шрифта ***
        boolean isUnicode = Minecraft.getInstance().isEnforceUnicode(); // Проверяем, включен ли Unicode
        int labelOffsetX;
        if (isUnicode) {
            labelOffsetX = 115; // Отступ для Unicode (подбери значение)
        } else {
            labelOffsetX = 175; // Отступ для стандартного шрифта (подбери значение)
        }
        // *** КОНЕЦ НОВОГО ***
        int labelOffsetY = 6;   // Вертикальное смещение текста относительно EditBox
        int textColor = 0xFFFFFF; // Белый цвет

        // Левая колонка (основные цвета)
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealth"), healthBarColorBox.getX() - labelOffsetX, healthBarColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmor"), armorBarColorBox.getX() - labelOffsetX, armorBarColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfood"), foodBarColorBox.getX() - labelOffsetX, foodBarColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxp"), xpBarColorBox.getX() - labelOffsetX, xpBarColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barair"), airBarColorBox.getX() - labelOffsetX, airBarColorBox.getY() + labelOffsetY, textColor);

        // Правая колонка (фоновые цвета)
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.getX() - labelOffsetX, healthBarBgColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.bararmorbg"), armorBarBgColorBox.getX() - labelOffsetX, armorBarBgColorBox.getY() + labelOffsetY, textColor);
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barfoodbg"), foodBarBgColorBox.getX() - labelOffsetX, foodBarBgColorBox.getY() + labelOffsetY, textColor);
        // !!! Вот исправленный рендер для XP BG !!!
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barxpbg"), xpBarBgColorBox.getX() - labelOffsetX, xpBarBgColorBox.getY() + labelOffsetY, textColor);
        // !!! КОНЕЦ ИСПРАВЛЕНИЯ !!!
        drawString(poseStack, this.font, Component.translatable("esmndnewhud.screen.barairbg"), airBarBgColorBox.getX() - labelOffsetX, airBarBgColorBox.getY() + labelOffsetY, textColor);
        // --- КОНЕЦ ИСПРАВЛЕННОГО БЛОКА ---
    }

    private void renderColorPreview(PoseStack poseStack, EditBox editBox) {
        try {
            int color = Integer.parseInt(editBox.getValue(), 16) | 0xFF000000;
            fill(poseStack,
                    editBox.getX() + editBox.getWidth() + 5,
                    editBox.getY(),
                    editBox.getX() + editBox.getWidth() + 25,
                    editBox.getY() + editBox.getHeight(),
                    color);
        } catch (NumberFormatException e) {
            // Игнорируем неверный формат
        }
    }

    private void saveChanges(Button button) {
        try {
            // Сохраняем значения в конфиг
            config.setHealthBarColor(parseHexColor(healthBarColorBox.getValue()));
            config.setHealthBarBackgroundColor(parseHexColor(healthBarBgColorBox.getValue()));
            config.setArmorBarColor(parseHexColor(armorBarColorBox.getValue()));
            config.setArmorBarBackgroundColor(parseHexColor(armorBarBgColorBox.getValue()));
            config.setFoodBarColor(parseHexColor(foodBarColorBox.getValue()));
            config.setFoodBarBackgroundColor(parseHexColor(foodBarBgColorBox.getValue()));
            config.setXpBarColor(parseHexColor(xpBarColorBox.getValue()));
            config.setXpBarBackgroundColor(parseHexColor(xpBarBgColorBox.getValue()));
            config.setAirBarColor(parseHexColor(airBarColorBox.getValue()));
            config.setAirBarBackgroundColor(parseHexColor(airBarBgColorBox.getValue()));

            // Возвращаемся к предыдущему экрану
            minecraft.setScreen(lastScreen);

            // Показываем сообщение об успешном сохранении
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§aHUD colors saved successfully!"), false);
            }
        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§cError: Invalid color format!"), true);
            }
        }
    }

    private void resetToDefault(Button button) {
        // Создаем новый конфиг с дефолтными значениями
        HUDColors defaultConfig = new HUDColors();

        // Обновляем все поля ввода
        healthBarColorBox.setValue(String.format("%06X", defaultConfig.getHealthBarColor() & 0xFFFFFF));
        healthBarBgColorBox.setValue(String.format("%06X", defaultConfig.getHealthBarBackgroundColor() & 0xFFFFFF));
        armorBarColorBox.setValue(String.format("%06X", defaultConfig.getArmorBarColor() & 0xFFFFFF));
        armorBarBgColorBox.setValue(String.format("%06X", defaultConfig.getArmorBarBackgroundColor() & 0xFFFFFF));
        foodBarColorBox.setValue(String.format("%06X", defaultConfig.getFoodBarColor() & 0xFFFFFF));
        foodBarBgColorBox.setValue(String.format("%06X", defaultConfig.getFoodBarBackgroundColor() & 0xFFFFFF));
        xpBarColorBox.setValue(String.format("%06X", defaultConfig.getXpBarColor() & 0xFFFFFF));
        xpBarBgColorBox.setValue(String.format("%06X", defaultConfig.getXpBarBackgroundColor() & 0xFFFFFF));
        airBarColorBox.setValue(String.format("%06X", defaultConfig.getAirBarColor() & 0xFFFFFF));
        airBarBgColorBox.setValue(String.format("%06X", defaultConfig.getAirBarBackgroundColor() & 0xFFFFFF));
    }

    private int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFFFF;
        }
        //return Integer.parseInt(hex, 16);
        return (int) Long.parseLong(hex, 16);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}