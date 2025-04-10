package com.esmnd.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.UUID;

public class esmndHUDConfig_ColorScreen extends Screen {
    private final Screen lastScreen;
    private final HUDColors config;

    // TextFields (EditBox in 1.20.1 is TextField in 1.16.5)
    private TextFieldWidget healthBarColorBox;
    private TextFieldWidget healthBarBgColorBox;
    private TextFieldWidget armorBarColorBox;
    private TextFieldWidget armorBarBgColorBox;
    private TextFieldWidget foodBarColorBox;
    private TextFieldWidget foodBarBgColorBox;
    private TextFieldWidget xpBarColorBox;
    private TextFieldWidget xpBarBgColorBox;
    private TextFieldWidget airBarColorBox;
    private TextFieldWidget airBarBgColorBox;

    public esmndHUDConfig_ColorScreen(Screen lastScreen) {
        super(new TranslationTextComponent("esmndnewhud.configscreencolor.title"));
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
        healthBarColorBox = createColorField(leftCol, yStart, new TranslationTextComponent("esmndnewhud.screen.barhealth").getString(),
                String.format("%06X", config.getHealthBarColor() & 0xFFFFFF));

        armorBarColorBox = createColorField(leftCol, yStart + ySpacing, new TranslationTextComponent("esmndnewhud.screen.bararmor").getString(),
                String.format("%06X", config.getArmorBarColor() & 0xFFFFFF));

        foodBarColorBox = createColorField(leftCol, yStart + ySpacing * 2, new TranslationTextComponent("esmndnewhud.screen.barfood").getString(),
                String.format("%06X", config.getFoodBarColor() & 0xFFFFFF));

        xpBarColorBox = createColorField(leftCol, yStart + ySpacing * 3, new TranslationTextComponent("esmndnewhud.screen.barxp").getString(),
                String.format("%06X", config.getXpBarColor() & 0xFFFFFF));

        airBarColorBox = createColorField(leftCol, yStart + ySpacing * 4, new TranslationTextComponent("esmndnewhud.screen.barair").getString(),
                String.format("%06X", config.getAirBarColor() & 0xFFFFFF));

        // Фоновые цвета (правая колонка)
        healthBarBgColorBox = createColorField(rightCol, yStart, new TranslationTextComponent("esmndnewhud.screen.barhealthbg").getString(),
                String.format("%06X", config.getHealthBarBackgroundColor() & 0xFFFFFF));

        armorBarBgColorBox = createColorField(rightCol, yStart + ySpacing, new TranslationTextComponent("esmndnewhud.screen.bararmorbg").getString(),
                String.format("%06X", config.getArmorBarBackgroundColor() & 0xFFFFFF));

        foodBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 2, new TranslationTextComponent("esmndnewhud.screen.barfoodbg").getString(),
                String.format("%06X", config.getFoodBarBackgroundColor() & 0xFFFFFF));

        xpBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 3, new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString(),
                String.format("%06X", config.getXpBarBackgroundColor() & 0xFFFFFF));

        airBarBgColorBox = createColorField(rightCol, yStart + ySpacing * 4, new TranslationTextComponent("esmndnewhud.screen.barairbg").getString(),
                String.format("%06X", config.getAirBarBackgroundColor() & 0xFFFFFF));

        // Кнопки управления
        this.addButton(new Button(this.width / 2 - 154, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.save"), this::saveChanges));

        this.addButton(new Button(this.width / 2 - 50, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.return"), (button) -> this.minecraft.setScreen(lastScreen)));

        this.addButton(new Button(this.width / 2 + 54, this.height - 28, 100, 20,
                new TranslationTextComponent("esmndnewhud.button.reset"), this::resetToDefault));

        this.addButton(new Button(this.width / 2 - 50, this.height - 64, 150, 20,
                new TranslationTextComponent("esmndnewhud.button.changegradientcolor"),
                (button) -> this.minecraft.setScreen(new esmndHUDConfig_ColorHardcore(this))));
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

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, 15, 0xFFFFFF);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        // Рендерим цветовые превью для каждого поля
        renderColorPreview(matrixStack, healthBarColorBox);
        renderColorPreview(matrixStack, healthBarBgColorBox);
        renderColorPreview(matrixStack, armorBarColorBox);
        renderColorPreview(matrixStack, armorBarBgColorBox);
        renderColorPreview(matrixStack, foodBarColorBox);
        renderColorPreview(matrixStack, foodBarBgColorBox);
        renderColorPreview(matrixStack, xpBarColorBox);
        renderColorPreview(matrixStack, xpBarBgColorBox);
        renderColorPreview(matrixStack, airBarColorBox);
        renderColorPreview(matrixStack, airBarBgColorBox);

        healthBarColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        healthBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        armorBarColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        armorBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        foodBarColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        foodBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        xpBarColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        xpBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        airBarColorBox.render(matrixStack, mouseX, mouseY, partialTicks);
        airBarBgColorBox.render(matrixStack, mouseX, mouseY, partialTicks);

        if (Minecraft.getInstance().options.forceUnicodeFont == Boolean.TRUE) {
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealth").getString(), healthBarColorBox.x - 105, healthBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.bararmor").getString(), armorBarColorBox.x - 105, armorBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barfood").getString(), foodBarColorBox.x - 105, foodBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxp").getString(), xpBarColorBox.x - 105, xpBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barair").getString(), airBarColorBox.x - 105, airBarColorBox.y + 6, 0xFFFFFF);

            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.x - 105, healthBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.bararmorbg").getString(), armorBarBgColorBox.x - 105, armorBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barfoodbg").getString(), foodBarBgColorBox.x - 105, foodBarBgColorBox.y + 6, 0xFFFFFF);

            if (Minecraft.getInstance().getLanguageManager().getSelected() == getMinecraft().getLanguageManager().getLanguage("ru_ru")) {
                drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.x - 105, xpBarBgColorBox.y + 6, 0xFFFFFF);
            } else if (Minecraft.getInstance().getLanguageManager().getSelected() == getMinecraft().getLanguageManager().getLanguage("en_us")) {
                drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.x - font.width(new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString()), xpBarBgColorBox.y + 6, 0xFFFFFF);
            }

            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barairbg").getString(), airBarBgColorBox.x - 105, airBarBgColorBox.y + 6, 0xFFFFFF);
        }
        else if (Minecraft.getInstance().options.forceUnicodeFont == Boolean.FALSE)
        {
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealth").getString(), healthBarColorBox.x - 135, healthBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.bararmor").getString(), armorBarColorBox.x - 135, armorBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barfood").getString(), foodBarColorBox.x - 135, foodBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxp").getString(), xpBarColorBox.x - 135, xpBarColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barair").getString(), airBarColorBox.x - 135, airBarColorBox.y + 6, 0xFFFFFF);

            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.x - 135, healthBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.bararmorbg").getString(), armorBarBgColorBox.x - 135, armorBarBgColorBox.y + 6, 0xFFFFFF);
            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barfoodbg").getString(), foodBarBgColorBox.x - 135, foodBarBgColorBox.y + 6, 0xFFFFFF);

            if (Minecraft.getInstance().getLanguageManager().getSelected() == getMinecraft().getLanguageManager().getLanguage("ru_ru")) {
                drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.x - 135, xpBarBgColorBox.y + 6, 0xFFFFFF);
            } else if (Minecraft.getInstance().getLanguageManager().getSelected() == getMinecraft().getLanguageManager().getLanguage("en_us")) {
                drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString(), xpBarBgColorBox.x - font.width(new TranslationTextComponent("esmndnewhud.screen.barxpbg").getString()), xpBarBgColorBox.y + 6, 0xFFFFFF);
            }

            drawString(matrixStack, this.font, new TranslationTextComponent("esmndnewhud.screen.barairbg").getString(), airBarBgColorBox.x - 135, airBarBgColorBox.y + 6, 0xFFFFFF);
        }
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

            minecraft.setScreen(lastScreen);

            if (minecraft.player != null) {
                Minecraft.getInstance().player.sendMessage(
                        new StringTextComponent("§aHUD colors saved successfully!"), UUID.randomUUID());
            }
        } catch (NumberFormatException e) {
            if (minecraft.player != null) {
                minecraft.player.sendMessage(
                        new StringTextComponent("§cError: Invalid color format!"), UUID.randomUUID());
            }
        }
    }

    private void resetToDefault(Button button) {
        HUDColors defaultConfig = new HUDColors();

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
        return (int) Long.parseLong(hex, 16);
    }
}