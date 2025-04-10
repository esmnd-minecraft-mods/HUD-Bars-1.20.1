package com.esmnd.hud;

import com.esmnd.hud.HUDColors;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField; // Поле ввода
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern; // Для проверки hex

public class esmndConfigGUIColorsGradient extends GuiScreen {

    private GuiScreen parentScreen;
    private HUDColors config;

    // Поля ввода
    private GuiTextField gradientColorOneBox; // Renamed for clarity
    private GuiTextField gradientColorTwoBox; // Renamed for clarity
    private GuiTextField gradientBgColorBox;  // Renamed for clarity

    // Список всех текстовых полей
    private final List<GuiTextField> textFields = new ArrayList<>();

    // Паттерн для проверки hex символов
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]*$");

    // ID кнопок (можно начать с других ID, чтобы не пересекались с предыдущим экраном, если это важно)
    private static final int SAVE_BUTTON_ID = 200;
    private static final int DONE_BUTTON_ID = 201;
    private static final int RESET_BUTTON_ID = 202;

    public esmndConfigGUIColorsGradient(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = HUDColors.getInstance();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.textFields.clear();

        int leftColX = this.width / 4 - 30;
        int rightColX = (this.width / 4) * 3 - 30;
        int yStart = 40;
        int ySpacing = 25;
        int fieldWidth = 70;
        int widgetHeight = 20;

        // Поля для градиента (левая колонка)
        // Порядок изменен для соответствия лейблам
        gradientColorOneBox = createColorField(300, leftColX, yStart, fieldWidth, widgetHeight, config.getHardcoreGradientOne());
        gradientColorTwoBox = createColorField(301, leftColX, yStart + ySpacing, fieldWidth, widgetHeight, config.getHardcoreGradientTwo());
        gradientBgColorBox = createColorField(302, rightColX, yStart, fieldWidth, widgetHeight, config.getHardcoreGradientBackgroundColor());

        // Добавляем поля в список
        textFields.add(gradientColorOneBox);
        textFields.add(gradientColorTwoBox);
        textFields.add(gradientBgColorBox);

        // Кнопки управления
        int bottomButtonY = this.height - 28;
        this.buttonList.add(new GuiButton(SAVE_BUTTON_ID, this.width / 2 - 154, bottomButtonY, 100, widgetHeight, I18n.format("esmndnewhud.button.save")));
        this.buttonList.add(new GuiButton(DONE_BUTTON_ID, this.width / 2 - 50, bottomButtonY, 100, widgetHeight, I18n.format("gui.done")));
        this.buttonList.add(new GuiButton(RESET_BUTTON_ID, this.width / 2 + 54, bottomButtonY, 100, widgetHeight, I18n.format("esmndnewhud.button.reset")));

        // Устанавливаем фокус
        if (!textFields.isEmpty()) {
            textFields.get(0).setFocused(true);
        }
    }

    // Вспомогательный метод для создания GuiTextField (такой же)
    private GuiTextField createColorField(int id, int x, int y, int width, int height, int initialColor) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, width, height);
        field.setMaxStringLength(6);
        field.setText(String.format("%06X", initialColor & 0xFFFFFF));
        return field;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, I18n.format("esmndnewhud.configscreencolorhard.title"), this.width / 2, 15, 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks); // Рисуем кнопки

        // Рендерим текстовые поля и превью
        for (GuiTextField field : textFields) {
            field.drawTextBox();
            renderColorPreview(field);
        }

        // Рендерим лейблы
        renderLabels();
    }

    // Метод для отрисовки лейблов
    private void renderLabels() {
        boolean isUnicode = mc.fontRenderer.getUnicodeFlag();
        int labelOffsetX;
        // Подбираем отступы
        if (isUnicode) {
            labelOffsetX = 90; // Примерно
        } else {
            labelOffsetX = 105; // Примерно
        }
        int labelOffsetY = (20 - this.fontRenderer.FONT_HEIGHT) / 2 + 1;
        int textColor = 0xFFFFFF;

        // Используем this.drawString
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.gradienttwo"), gradientColorOneBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.gradienttwo") / 2), gradientColorOneBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.gradientone"), gradientColorTwoBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.gradientone") / 2), gradientColorTwoBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barhealthbg"), gradientBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barhealthbg") / 2), gradientBgColorBox.y + labelOffsetY, textColor); // Используем тот же ключ для BG
    }

    // Метод для отрисовки превью цвета (такой же)
    private void renderColorPreview(GuiTextField textField) {
        int previewSize = textField.height - 4;
        int previewX = textField.x + textField.width + 5;
        int previewY = textField.y + 2;
        int borderColor = 0xFF000000; // Черная рамка по умолчанию
        int fillColor = 0xFF000000;   // Черный цвет заливки по умолчанию

        String currentText = textField.getText(); // Получаем текст из поля

        // 1. Простая проверка длины и паттерна
        if (currentText != null && currentText.length() == 6 && HEX_PATTERN.matcher(currentText).matches()) {
            // Формат вроде бы верный
            try {
                // 2. Пытаемся спарсить Long, чтобы получить RGB
                long rgbLong = Long.parseLong(currentText, 16);
                int rgb = (int) (rgbLong & 0xFFFFFF); // Отбрасываем все лишнее, берем только 24 бита

                // 3. Добавляем альфа-канал для отображения
                fillColor = rgb | 0xFF000000;
                borderColor = 0xFF000000; // Черная рамка, т.к. формат верный

                // ОТЛАДКА: Выводим результат парсинга (только если значение изменилось с прошлого кадра - опционально)
                // if (!currentText.equals(textField.getPreviousText())) { // Предотвращаем спам
                //     System.out.println("TextField ID " + textField.getId() + ": Text='" + currentText + "' -> Parsed RGB: 0x" + Integer.toHexString(rgb).toUpperCase() + " -> Fill ARGB: 0x" + Integer.toHexString(fillColor).toUpperCase());
                // }

            } catch (NumberFormatException e) {
                // Если Long.parseLong выдал ошибку (не должно случиться при нашей проверке, но на всякий случай)
                borderColor = 0xFFFF0000; // Красная рамка - ошибка парсинга
                System.err.println("Preview Render Error (parseLong failed unexpectedly) for text: " + currentText); // <-- Отладка
            }
        } else {
            // Длина не 6 или символы не hex
            borderColor = 0xFF888888; // Серая рамка - неверный формат
            // ОТЛАДКА: Указываем причину
            // if (currentText == null) System.out.println("Preview: Text is null");
            // else if (currentText.length() != 6) System.out.println("Preview: Length is not 6 (" + currentText.length() + "): " + currentText);
            // else System.out.println("Preview: Pattern mismatch: " + currentText);
        }

        // 4. Рисуем квадрат с заливкой (fillColor)
        net.minecraft.client.gui.Gui.drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, fillColor);

        // 5. Рисуем рамку (borderColor) поверх
        //    Рисуем 4 линии для четкости
        net.minecraft.client.gui.Gui.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY, borderColor); // Top
        net.minecraft.client.gui.Gui.drawRect(previewX - 1, previewY + previewSize, previewX + previewSize + 1, previewY + previewSize + 1, borderColor); // Bottom
        net.minecraft.client.gui.Gui.drawRect(previewX - 1, previewY, previewX, previewY + previewSize, borderColor); // Left
        net.minecraft.client.gui.Gui.drawRect(previewX + previewSize, previewY, previewX + previewSize + 1, previewY + previewSize, borderColor); // Right
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        switch (button.id) {
            case SAVE_BUTTON_ID:
                saveChanges();
                break;
            case DONE_BUTTON_ID:
                this.mc.displayGuiScreen(this.parentScreen);
                break;
            case RESET_BUTTON_ID:
                resetToDefault();
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean consumed = false;
        for (GuiTextField field : textFields) {
            if (field.isFocused()) {
                if (HEX_PATTERN.matcher(String.valueOf(typedChar)).matches() ||
                        keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE ||
                        keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT ||
                        keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_END ||
                        GuiScreen.isKeyComboCtrlA(keyCode) || GuiScreen.isKeyComboCtrlC(keyCode) ||
                        GuiScreen.isKeyComboCtrlV(keyCode) || GuiScreen.isKeyComboCtrlX(keyCode))
                {
                    consumed = field.textboxKeyTyped(typedChar, keyCode);
                } else if (keyCode == Keyboard.KEY_TAB) {
                    changeFocus(GuiScreen.isShiftKeyDown());
                    consumed = true;
                }
                break;
            }
        }

        if (!consumed) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                saveChanges();
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                this.mc.displayGuiScreen(this.parentScreen);
            } else {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    // Переключение фокуса (такое же)
    private void changeFocus(boolean backwards) {
        int currentFocus = -1;
        for (int i = 0; i < textFields.size(); i++) {
            if (textFields.get(i).isFocused()) {
                currentFocus = i;
                textFields.get(i).setFocused(false);
                break;
            }
        }
        int nextFocus = (currentFocus + (backwards ? -1 : 1) + textFields.size()) % textFields.size();
        if (nextFocus >= 0 && nextFocus < textFields.size()) {
            textFields.get(nextFocus).setFocused(true);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : textFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : textFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void saveChanges() {
        try {
            // Сохраняем значения
            config.setHardcoreGradientOne(parseHexColor(gradientColorOneBox.getText()));
            config.setHardcoreGradientTwo(parseHexColor(gradientColorTwoBox.getText()));
            config.setHardcoreGradientBackgroundColor(parseHexColor(gradientBgColorBox.getText()));

            // config.saveConfig(); // Не нужно

            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.GREEN + "HUD Gradient colors saved successfully!"));
            }

        } catch (InvalidHexFormatException e) {
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "Error: Invalid HEX color format! Please enter 6 hexadecimal characters (0-9, A-F)."));
            }
        } catch (Exception e) {
            System.err.println("Unexpected error saving HUD gradient colors:");
            e.printStackTrace();
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "An unexpected error occurred while saving gradient colors. Check logs."));
            }
        }
    }

    private void resetToDefault() {
        HUDColors defaultConfig = new HUDColors();

        // Обновляем текст
        gradientColorOneBox.setText(String.format("%06X", defaultConfig.getHardcoreGradientOne() & 0xFFFFFF));
        gradientColorTwoBox.setText(String.format("%06X", defaultConfig.getHardcoreGradientTwo() & 0xFFFFFF));
        gradientBgColorBox.setText(String.format("%06X", defaultConfig.getHardcoreGradientBackgroundColor() & 0xFFFFFF));

        if (this.mc.player != null) {
            this.mc.player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "HUD Gradient colors reset to default. Press Save to apply."));
        }
    }

    // Используем тот же парсер с кастомным исключением
    private int parseHexColor(String hex) throws InvalidHexFormatException {
        if (hex == null || hex.length() != 6 || !HEX_PATTERN.matcher(hex).matches()) {
            throw new InvalidHexFormatException("Invalid hex format: " + hex);
        }
        try {
            return (int) (Long.parseLong(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            throw new InvalidHexFormatException("Invalid hex format: " + hex, e);
        }
    }

    // Кастомное исключение (можно вынести в отдельный файл или оставить здесь)
    private static class InvalidHexFormatException extends Exception {
        public InvalidHexFormatException(String message) { super(message); }
        public InvalidHexFormatException(String message, Throwable cause) { super(message, cause); }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}