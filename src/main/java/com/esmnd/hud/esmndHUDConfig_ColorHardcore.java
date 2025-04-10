package com.esmnd.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.UUID;

public class esmndHUDConfig_ColorHardcore extends Screen {
    private final Screen lastScreen;
    private final HUDColors config;

    // TextFields (EditBox в 1.20.1 это TextField в 1.16.5)
    private TextFieldWidget healthBarColorBoxGradientOne;
    private TextFieldWidget healthBarColorBoxGradientTwo;
    private TextFieldWidget healthBarBgColorBox;

    public esmndHUDConfig_ColorHardcore(Screen lastScreen) {
        super(new TranslationTextComponent("esmndnewhud.configscreencolorhard.title"));
        this.lastScreen = lastScreen;
        this.config = HUDColors.getInstance();
    }

    @Override
    protected void init() {
        int leftCol = this.width / 4;
        int rightCol = (this.width / 4) * 3;
        int yStart = 40;
        int ySpacing = 25;

        healthBarColorBoxGradientTwo = createColorField(leftCol, yStart, "",
                String.format("%06X", config.getHardcoreGradientTwo() & 0xFFFFFF));

        // Основной цвет градиента (левая колонка)
        healthBarColorBoxGradientOne = createColorField(leftCol, yStart + ySpacing, "",
                String.format("%06X", config.getHardcoreGradientOne() & 0xFFFFFF));

        // Фоновые цвета (правая колонка)
        healthBarBgColorBox = createColorField(rightCol, yStart, "",
                String.format("%06X", config.getHardcoreGradientBackgroundColor() & 0xFFFFFF));

        // Кнопки управления
        this.addButton(new Button(this.width / 2 - 154, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.save"), this::saveChanges));

        this.addButton(new Button(this.width / 2 - 50, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.return"),
                (button) -> this.minecraft.setScreen(lastScreen)));

        this.addButton(new Button(this.width / 2 + 54, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.reset"), this::resetToDefault));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, 15, 0xFFFFFF);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        // Рендерим цветовые превью для каждого поля
        renderColorPreview(matrixStack, healthBarColorBoxGradientOne);
        renderColorPreview(matrixStack, healthBarColorBoxGradientTwo);
        renderColorPreview(matrixStack, healthBarBgColorBox);

        healthBarColorBoxGradientOne.render(matrixStack, mouseX, mouseY, partialTicks);
        healthBarColorBoxGradientTwo.render(matrixStack, mouseX, mouseY, partialTicks);
        healthBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);

        if (Minecraft.getInstance().options.forceUnicodeFont == Boolean.TRUE) {
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealthbg").getString(), healthBarBgColorBox.x - 105, healthBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.gradientone").getString(), healthBarColorBoxGradientTwo.x - 125, healthBarColorBoxGradientTwo.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.gradienttwo").getString(), healthBarColorBoxGradientOne.x - 125, healthBarColorBoxGradientOne.y + 6, 0xFFFFFF);
        }
        else if (Minecraft.getInstance().options.forceUnicodeFont == Boolean.FALSE)
        {
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealthbg").getString(), healthBarBgColorBox.x - 135, healthBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.gradientone").getString(), healthBarColorBoxGradientTwo.x - 165, healthBarColorBoxGradientTwo.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.gradienttwo").getString(), healthBarColorBoxGradientOne.x - 165, healthBarColorBoxGradientOne.y + 6, 0xFFFFFF);
        }
    }

    private TextFieldWidget createColorField(int x, int y, String label, String defaultValue) {
        // Создаем поле ввода
        TextFieldWidget textField = new TextFieldWidget(this.font, x, y, 70, 20, StringTextComponent.EMPTY);
        textField.setValue(defaultValue);
        textField.setMaxLength(6);
        textField.setFilter(text -> text.matches("[0-9A-Fa-f]*"));
        textField.setVisible(true);
        textField.setEditable(true);

        this.children.add(textField);
        return textField;
    }

    private void renderColorPreview(MatrixStack matrixStack, TextFieldWidget textField) {
        try {
            int color = Integer.parseInt(textField.getValue(), 16) | 0xFF000000;
            fill(matrixStack,
                    textField.x + textField.getWidth() + 5,
                    textField.y,
                    textField.x + textField.getWidth() + 25,
                    textField.y + textField.getHeight(),
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

            assert minecraft != null;
            minecraft.setScreen(lastScreen);

            if (minecraft.player != null) {
                minecraft.player.sendMessage(
                        new StringTextComponent("§aHUD Gradient colors saved successfully!"), UUID.randomUUID());
            }
        } catch (NumberFormatException e) {
            assert minecraft != null;
            if (minecraft.player != null) {
                minecraft.player.sendMessage(
                        new StringTextComponent("§cError colors Gradient: Invalid color format!"), UUID.randomUUID());
            }
        }
    }

    private void resetToDefault(Button button) {
        HUDColors defaultConfig = new HUDColors();

        healthBarColorBoxGradientOne.setValue(String.format("%06X", defaultConfig.getHardcoreGradientOne() & 0xFFFFFF));
        healthBarColorBoxGradientTwo.setValue(String.format("%06X", defaultConfig.getHardcoreGradientTwo() & 0xFFFFFF));
        healthBarBgColorBox.setValue(String.format("%06X", defaultConfig.getHardcoreGradientBackgroundColor() & 0xFFFFFF));
    }

    private int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFFFF;
        }
        return (int) Long.parseLong(hex, 16);
    }
}
