package com.esmnd.hud;

import com.esmnd.hud.HUDColors;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField; // <--- Поле ввода
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern; // Для проверки hex

public class esmndConfigGUIColors extends GuiScreen {

    private GuiScreen parentScreen;
    private HUDColors config;

    // Поля ввода
    private GuiTextField healthBarColorBox;
    private GuiTextField healthBarBgColorBox;
    private GuiTextField armorBarColorBox;
    private GuiTextField armorBarBgColorBox;
    private GuiTextField foodBarColorBox;
    private GuiTextField foodBarBgColorBox;
    private GuiTextField xpBarColorBox;
    private GuiTextField xpBarBgColorBox;
    private GuiTextField airBarColorBox;
    private GuiTextField airBarBgColorBox;

    // Список всех текстовых полей для удобства обработки
    private final List<GuiTextField> textFields = new ArrayList<>();

    // Паттерн для проверки hex символов (0-9, A-F, a-f)
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9A-Fa-f]*$");

    // ID кнопок
    private static final int SAVE_BUTTON_ID = 100;
    private static final int DONE_BUTTON_ID = 101; // Используем другое имя ("Done")
    private static final int RESET_BUTTON_ID = 102;
    private static final int CHANGE_GRADIENT_ID = 103;

    public esmndConfigGUIColors(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.config = HUDColors.getInstance();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true); // Включаем повтор событий клавиатуры для полей ввода
        this.buttonList.clear();
        this.textFields.clear(); // Очищаем список полей

        int leftColX = this.width / 4 - 30; // Сдвигаем чуть левее для лейблов
        int rightColX = (this.width / 4) * 3 - 30; // Сдвигаем чуть левее для лейблов
        int yStart = 40;
        int ySpacing = 25;
        int fieldWidth = 70;
        int widgetHeight = 20;

        // Основные цвета (левая колонка)
        healthBarColorBox = createColorField(0, leftColX, yStart, fieldWidth, widgetHeight, config.getHealthBarColor());
        armorBarColorBox = createColorField(1, leftColX, yStart + ySpacing, fieldWidth, widgetHeight, config.getArmorBarColor());
        foodBarColorBox = createColorField(2, leftColX, yStart + ySpacing * 2, fieldWidth, widgetHeight, config.getFoodBarColor());
        xpBarColorBox = createColorField(3, leftColX, yStart + ySpacing * 3, fieldWidth, widgetHeight, config.getXpBarColor());
        airBarColorBox = createColorField(4, leftColX, yStart + ySpacing * 4, fieldWidth, widgetHeight, config.getAirBarColor());

        // Фоновые цвета (правая колонка)
        healthBarBgColorBox = createColorField(5, rightColX, yStart, fieldWidth, widgetHeight, config.getHealthBarBackgroundColor());
        armorBarBgColorBox = createColorField(6, rightColX, yStart + ySpacing, fieldWidth, widgetHeight, config.getArmorBarBackgroundColor());
        foodBarBgColorBox = createColorField(7, rightColX, yStart + ySpacing * 2, fieldWidth, widgetHeight, config.getFoodBarBackgroundColor());
        xpBarBgColorBox = createColorField(8, rightColX, yStart + ySpacing * 3, fieldWidth, widgetHeight, config.getXpBarBackgroundColor());
        airBarBgColorBox = createColorField(9, rightColX, yStart + ySpacing * 4, fieldWidth, widgetHeight, config.getAirBarBackgroundColor());

        // Добавляем все поля в список
        textFields.add(healthBarColorBox); textFields.add(healthBarBgColorBox);
        textFields.add(armorBarColorBox); textFields.add(armorBarBgColorBox);
        textFields.add(foodBarColorBox); textFields.add(foodBarBgColorBox);
        textFields.add(xpBarColorBox); textFields.add(xpBarBgColorBox);
        textFields.add(airBarColorBox); textFields.add(airBarBgColorBox);


        // Кнопки управления
        int bottomButtonY = this.height - 28;
        this.buttonList.add(new GuiButton(SAVE_BUTTON_ID, this.width / 2 - 154, bottomButtonY, 100, widgetHeight, I18n.format("esmndnewhud.button.save")));
        this.buttonList.add(new GuiButton(DONE_BUTTON_ID, this.width / 2 - 50, bottomButtonY, 100, widgetHeight, I18n.format("gui.done"))); // "Готово"
        this.buttonList.add(new GuiButton(RESET_BUTTON_ID, this.width / 2 + 54, bottomButtonY, 100, widgetHeight, I18n.format("esmndnewhud.button.reset")));

        // Кнопка смены градиента
        this.buttonList.add(new GuiButton(CHANGE_GRADIENT_ID, this.width / 2 - 100, bottomButtonY - 25, 200, widgetHeight, I18n.format("esmndnewhud.button.changegradientcolor")));

        // Устанавливаем фокус на первое поле
        if (!textFields.isEmpty()) {
            textFields.get(0).setFocused(true);
        }
    }

    // Вспомогательный метод для создания GuiTextField
    private GuiTextField createColorField(int id, int x, int y, int width, int height, int initialColor) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, x, y, width, height);
        field.setMaxStringLength(6); // Максимум 6 hex символов
        field.setText(String.format("%06X", initialColor & 0xFFFFFF)); // Устанавливаем начальное значение без альфы
        // field.setEnableBackgroundDrawing(true); // Фон рисуется по умолчанию
        return field;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer, I18n.format("esmndnewhud.configscreencolor.title"), this.width / 2, 15, 0xFFFFFF);

        // Рендерим кнопки
        super.drawScreen(mouseX, mouseY, partialTicks);

        // Рендерим текстовые поля
        for (GuiTextField field : textFields) {
            field.drawTextBox();
            renderColorPreview(field); // Рисуем превью рядом
        }

        // Рендерим лейблы
        renderLabels();
    }

    // Метод для отрисовки лейблов рядом с полями
    private void renderLabels() {
        boolean isUnicode = mc.fontRenderer.getUnicodeFlag(); // Используем флаг из FontRenderer
        int labelOffsetX;
        // Подбираем отступы для 1.12.2 (могут отличаться от 1.19.4)
        if (isUnicode) {
            labelOffsetX = 65; // Примерный отступ для Unicode
        } else {
            labelOffsetX = 75; // Примерный отступ для стандартного
        }
        int labelOffsetY = (20 - this.fontRenderer.FONT_HEIGHT) / 2 + 1; // Центрируем по вертикали
        int textColor = 0xFFFFFF;

        // --- ЛЕЙБЛЫ --- (Используем this.drawString)
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barhealth"), healthBarColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barhealth") / 2), healthBarColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.bararmor"), armorBarColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.bararmor") / 2), armorBarColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barfood"), foodBarColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barfood") / 2), foodBarColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barxp"), xpBarColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barxp") / 2), xpBarColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barair"), airBarColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barair") / 2), airBarColorBox.y + labelOffsetY, textColor);

        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barhealthbg"), healthBarBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barhealthbg") / 2), healthBarBgColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.bararmorbg"), armorBarBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.bararmorbg") / 2), armorBarBgColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barfoodbg"), foodBarBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barfoodbg") / 2), foodBarBgColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barxpbg"), xpBarBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barxpbg") / 2), xpBarBgColorBox.y + labelOffsetY, textColor);
        this.drawString(fontRenderer, I18n.format("esmndnewhud.screen.barairbg"), airBarBgColorBox.x - (labelOffsetX + mc.fontRenderer.getStringWidth("esmndnewhud.screen.barairbg") / 2), airBarBgColorBox.y + labelOffsetY, textColor);
        // --- КОНЕЦ ЛЕЙБЛОВ ---
    }

    // Метод для отрисовки превью цвета
    private void renderColorPreview(GuiTextField textField) {
        int previewSize = textField.height - 4; // Размер квадратика превью
        int previewX = textField.x + textField.width + 5; // Справа от поля
        int previewY = textField.y + 2; // С небольшим отступом сверху

        try {
            String hex = textField.getText();
            if (hex.length() == 6) { // Рисуем только если введено 6 символов
                int color = parseHexColorWithError(hex) | 0xFF000000; // Добавляем альфа-канал
                // Рисуем квадрат с цветом
                Gui.drawRect(previewX, previewY, previewX + previewSize, previewY + previewSize, color);
                // Рисуем черную рамку вокруг
                Gui.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY, 0xFF000000); // Top
                Gui.drawRect(previewX - 1, previewY + previewSize, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF000000); // Bottom
                Gui.drawRect(previewX - 1, previewY, previewX, previewY + previewSize, 0xFF000000); // Left
                Gui.drawRect(previewX + previewSize, previewY, previewX + previewSize + 1, previewY + previewSize, 0xFF000000); // Right
            } else {
                // Можно нарисовать пустой квадрат или ничего не делать, если формат неверный
                Gui.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFF555555); // Серая рамка
            }
        } catch (NumberFormatException e) {
            Gui.drawRect(previewX - 1, previewY - 1, previewX + previewSize + 1, previewY + previewSize + 1, 0xFFFF0000); // Красная рамка при ошибке
        } catch (InvalidHexFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        switch (button.id) {
            case SAVE_BUTTON_ID:
                saveChanges();
                // Не выходим автоматически, чтобы пользователь видел сообщение
                break;
            case DONE_BUTTON_ID:
                this.mc.displayGuiScreen(this.parentScreen);
                break;
            case RESET_BUTTON_ID:
                resetToDefault();
                break;
            case CHANGE_GRADIENT_ID:
                this.mc.displayGuiScreen(new esmndConfigGUIColorsGradient(this));
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Сначала передаем ввод активному текстовому полю
        boolean consumed = false;
        for (GuiTextField field : textFields) {
            if (field.isFocused()) {
                // Проверяем, является ли символ допустимым для hex
                if (HEX_PATTERN.matcher(String.valueOf(typedChar)).matches() ||
                        keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE ||
                        keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT ||
                        keyCode == Keyboard.KEY_HOME || keyCode == Keyboard.KEY_END ||
                        (GuiScreen.isKeyComboCtrlA(keyCode)) || // Ctrl+A
                        (GuiScreen.isKeyComboCtrlC(keyCode)) || // Ctrl+C
                        (GuiScreen.isKeyComboCtrlV(keyCode)) || // Ctrl+V
                        (GuiScreen.isKeyComboCtrlX(keyCode)))   // Ctrl+X
                {
                    consumed = field.textboxKeyTyped(typedChar, keyCode);
                } else if (keyCode == Keyboard.KEY_TAB) {
                    // Обработка Tab для смены фокуса
                    changeFocus(GuiScreen.isShiftKeyDown()); // isShiftKeyDown() для Shift+Tab
                    consumed = true;
                }
                break; // Выходим из цикла, если нашли активное поле
            }
        }

        // Если ввод не был обработан полем или это не Tab:
        if (!consumed) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                // Сохраняем по Enter
                saveChanges();
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                // Выходим по Escape
                this.mc.displayGuiScreen(this.parentScreen);
            } else {
                // Передаем дальше, если это не наш случай
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    // Переключение фокуса между текстовыми полями
    private void changeFocus(boolean backwards) {
        int currentFocus = -1;
        for (int i = 0; i < textFields.size(); i++) {
            if (textFields.get(i).isFocused()) {
                currentFocus = i;
                textFields.get(i).setFocused(false);
                break;
            }
        }

        int nextFocus;
        if (backwards) {
            nextFocus = (currentFocus - 1 + textFields.size()) % textFields.size();
        } else {
            nextFocus = (currentFocus + 1) % textFields.size();
        }

        if (nextFocus >= 0 && nextFocus < textFields.size()) {
            textFields.get(nextFocus).setFocused(true);
        }
    }


    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton); // Обработка кнопок
        // Обработка кликов для текстовых полей
        for (GuiTextField field : textFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        // Обновляем курсоры текстовых полей
        for (GuiTextField field : textFields) {
            field.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false); // Выключаем повтор событий клавиатуры
    }


    private void saveChanges() {
        try {
            // Пытаемся сохранить все значения
            config.setHealthBarColor(parseHexColorWithError(healthBarColorBox.getText()));
            config.setHealthBarBackgroundColor(parseHexColorWithError(healthBarBgColorBox.getText()));
            config.setArmorBarColor(parseHexColorWithError(armorBarColorBox.getText()));
            config.setArmorBarBackgroundColor(parseHexColorWithError(armorBarBgColorBox.getText()));
            config.setFoodBarColor(parseHexColorWithError(foodBarColorBox.getText()));
            config.setFoodBarBackgroundColor(parseHexColorWithError(foodBarBgColorBox.getText()));
            config.setXpBarColor(parseHexColorWithError(xpBarColorBox.getText()));
            config.setXpBarBackgroundColor(parseHexColorWithError(xpBarBgColorBox.getText()));
            config.setAirBarColor(parseHexColorWithError(airBarColorBox.getText()));
            config.setAirBarBackgroundColor(parseHexColorWithError(airBarBgColorBox.getText()));

            // config.saveConfig(); // Не нужно, т.к. сеттеры сохраняют

            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.GREEN + "HUD colors saved successfully!"));
            }
            // Можно вернуться назад после сохранения:
            // this.mc.displayGuiScreen(this.parentScreen);

        } catch (InvalidHexFormatException e) {
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "Error: Invalid HEX color format in one or more fields! Please enter 6 hexadecimal characters (0-9, A-F)."));
            }
        } catch (Exception e) { // Ловим другие возможные ошибки
            System.err.println("Unexpected error saving HUD colors:");
            e.printStackTrace();
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "An unexpected error occurred while saving colors. Check logs."));
            }
        }
    }

    private void resetToDefault() {
        HUDColors defaultConfig = new HUDColors(); // Создаем дефолтный инстанс

        // Обновляем текст во всех полях
        healthBarColorBox.setText(String.format("%06X", defaultConfig.getHealthBarColor() & 0xFFFFFF));
        healthBarBgColorBox.setText(String.format("%06X", defaultConfig.getHealthBarBackgroundColor() & 0xFFFFFF));
        armorBarColorBox.setText(String.format("%06X", defaultConfig.getArmorBarColor() & 0xFFFFFF));
        armorBarBgColorBox.setText(String.format("%06X", defaultConfig.getArmorBarBackgroundColor() & 0xFFFFFF));
        foodBarColorBox.setText(String.format("%06X", defaultConfig.getFoodBarColor() & 0xFFFFFF));
        foodBarBgColorBox.setText(String.format("%06X", defaultConfig.getFoodBarBackgroundColor() & 0xFFFFFF));
        xpBarColorBox.setText(String.format("%06X", defaultConfig.getXpBarColor() & 0xFFFFFF));
        xpBarBgColorBox.setText(String.format("%06X", defaultConfig.getXpBarBackgroundColor() & 0xFFFFFF));
        airBarColorBox.setText(String.format("%06X", defaultConfig.getAirBarColor() & 0xFFFFFF));
        airBarBgColorBox.setText(String.format("%06X", defaultConfig.getAirBarBackgroundColor() & 0xFFFFFF));

        // Можно добавить сообщение о сбросе
        if (this.mc.player != null) {
            this.mc.player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "HUD colors reset to default. Press Save to apply."));
        }
    }

    // Парсер hex с проверкой длины и формата, выбрасывающий кастомное исключение
    private int parseHexColorWithError(String hex) throws InvalidHexFormatException {
        if (hex == null || hex.length() != 6 || !HEX_PATTERN.matcher(hex).matches()) {
            throw new InvalidHexFormatException("Invalid hex format: " + hex);
        }
        try {
            // Парсим как Long, чтобы избежать проблем со знаком у Integer.MAX_VALUE
            return (int) (Long.parseLong(hex, 16) & 0xFFFFFF); // Берем только RGB часть
        } catch (NumberFormatException e) {
            throw new InvalidHexFormatException("Invalid hex format: " + hex, e);
        }
    }

    // Простое исключение для индикации ошибки формата
    private static class InvalidHexFormatException extends Exception {
        public InvalidHexFormatException(String message) {
            super(message);
        }
        public InvalidHexFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}