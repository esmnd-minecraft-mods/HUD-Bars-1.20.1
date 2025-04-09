package com.esmnd.esmndhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class esmndHUDConfig_ColorHardcore extends Screen {

    private final Screen lastScreen;
    private final HUDColors config;

    // EditBox компоненты
    private EditBox healthBarColorBoxGradientOne;
    private EditBox healthBarColorBoxGradientTwo;
    private EditBox healthBarBgColorBox;

    public esmndHUDConfig_ColorHardcore(Screen lastScreen) {
        super(Component.translatable("esmndnewhud.configscreencolorhard.title"));
        this.lastScreen = lastScreen;
        this.config = HUDColors.getInstance();
    }

    @Override
    protected void init() {
        int leftCol = this.width / 4;
        int rightCol = (this.width / 4) * 3;
        int yStart = 40;
        int ySpacing = 25;

        // Основной цвет градиента (левая колонка)
        healthBarColorBoxGradientTwo = createColorField(leftCol, yStart, "Health Bar Gradient One",
                String.format("%06X", config.getHardcoreGradientTwo() & 0xFFFFFF));

        healthBarColorBoxGradientOne = createColorField(leftCol, yStart + ySpacing, "Health Bar Gradient Two",
                String.format("%06X", config.getHardcoreGradientOne() & 0xFFFFFF));

        // Фоновые цвета (правая колонка)
        healthBarBgColorBox = createColorField(rightCol, yStart, "Health Bar BG",
                String.format("%06X", config.getHardcoreGradientBackgroundColor() & 0xFFFFFF));

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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        // Рендерим все EditBox'ы и другие виджеты
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Рендерим цветовые превью для каждого поля
        renderColorPreview(guiGraphics, healthBarColorBoxGradientTwo);
        renderColorPreview(guiGraphics, healthBarColorBoxGradientOne);
        renderColorPreview(guiGraphics, healthBarBgColorBox);

        if (Minecraft.getInstance().options.forceUnicodeFont().get() == Boolean.TRUE) {
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.getX() - 105, healthBarBgColorBox.getY() + 6, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.gradienttwo").getString(), healthBarColorBoxGradientOne.getX() - 115, healthBarColorBoxGradientOne.getY() + 6, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.gradientone").getString(), healthBarColorBoxGradientTwo.getX() - 115, healthBarColorBoxGradientTwo.getY() + 6, 0xFFFFFF);
        }
        else if (Minecraft.getInstance().options.forceUnicodeFont().get() == Boolean.FALSE)
        {
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.getX() - 135, healthBarBgColorBox.getY() + 6, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.gradienttwo").getString(), healthBarColorBoxGradientOne.getX() - 165, healthBarColorBoxGradientOne.getY() + 6, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.translatable("esmndnewhud.screen.gradientone").getString(), healthBarColorBoxGradientTwo.getX() - 165, healthBarColorBoxGradientTwo.getY() + 6, 0xFFFFFF);
        }
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

    private void renderColorPreview(GuiGraphics guiGraphics, EditBox editBox) {
        try {
            int color = Integer.parseInt(editBox.getValue(), 16) | 0xFF000000;
            guiGraphics.fill(
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
            config.setHardcoreGradientOne(parseHexColor(healthBarColorBoxGradientOne.getValue()));
            config.setHardcoreGradientTwo(parseHexColor(healthBarColorBoxGradientTwo.getValue()));
            config.setHardcoreGradientBackgroundColor(parseHexColor(healthBarBgColorBox.getValue()));

            // Возвращаемся к предыдущему экрану
            minecraft.setScreen(lastScreen);

            // Показываем сообщение об успешном сохранении
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§aHUD Hardcore colors saved successfully!"), false);
            }
        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§cError colors Hardcore: Invalid color format!"), true);
            }
        }
    }

    private void resetToDefault(Button button) {
        // Создаем новый конфиг с дефолтными значениями
        HUDColors defaultConfig = new HUDColors();

        // Обновляем все поля ввода
        healthBarColorBoxGradientOne.setValue(String.format("%06X", defaultConfig.getHardcoreGradientOne() & 0xFFFFFF));
        healthBarColorBoxGradientTwo.setValue(String.format("%06X", defaultConfig.getHardcoreGradientTwo() & 0xFFFFFF));
        healthBarBgColorBox.setValue(String.format("%06X", defaultConfig.getHardcoreGradientBackgroundColor() & 0xFFFFFF));
    }

    private int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFFFF;
        }
        //return Integer.parseInt(hex, 16);
        return (int) Long.parseLong(hex, 16);
    }
}