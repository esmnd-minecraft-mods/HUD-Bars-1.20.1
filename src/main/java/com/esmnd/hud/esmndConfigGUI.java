package com.esmnd.hud;

import com.esmnd.hud.HUDColors; // Наш конфиг
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
// Импорты для виджетов Forge (убедись, что Forge API доступно)
import net.minecraftforge.fml.client.config.GuiCheckBox;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraft.client.resources.I18n; // Для локализации
import net.minecraft.util.text.TextComponentString; // Для сообщений в чат
import net.minecraft.util.text.TextFormatting; // Для форматирования чата
import org.lwjgl.input.Keyboard; // Для обработки Escape

import java.io.IOException; // Для actionPerformed
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;

public class esmndConfigGUI extends GuiScreen {
    private final GuiScreen parentScreen;
    private final HUDColors config;

    // Состояния для кнопок-переключателей
    private HUDColors.PulseLowHealthMode currentPulseMode;
    private HUDColors.GradientModeUse currentGradientMode;

    // Виджеты
    private GuiButton gradientModeButton;
    private GuiButton pulseModeButton;
    private GuiCheckBox fixedHUDSizeCheckbox;
    private GuiCheckBox percentBarsCheckbox;
    private GuiSlider hudScaleSlider;
    private GuiButton changeColorButton;
    private GuiButton saveButton;
    private GuiButton doneButton;

    // Константы для ID кнопок (важно для actionPerformed)
    private static final int GRADIENT_MODE_ID = 0;
    private static final int PULSE_MODE_ID = 1;
    private static final int CHANGE_COLOR_ID = 2;
    private static final int PERCENT_BARS_ID = 3;
    private static final int FIXED_HUD_SIZE_ID = 4;
    private static final int HUD_SCALE_SLIDER_ID = 5; // GuiSlider тоже кнопка
    private static final int SAVE_BUTTON_ID = 6;
    private static final int DONE_BUTTON_ID = 7;


    public esmndConfigGUI(GuiScreen parent) {
        this.parentScreen = parent;
        this.config = HUDColors.getInstance();
        // Загружаем текущие настройки
        this.currentPulseMode = config.getPulseMode();
        this.currentGradientMode = config.getGradientModeUse();
    }

    /**
     * Инициализация GUI. Вызывается при открытии экрана и изменении размера окна.
     */
    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear(); // Очищаем список кнопок перед добавлением новых

        int rowHeight = 25;
        int startY = this.height / 6; // Используем this.height
        int centerX = this.width / 2; // Используем this.width
        int buttonWidth = 200;
        int widgetHeight = 20; // Стандартная высота виджетов в 1.12.2
        int spacing = 5; // Небольшой отступ

        int currentY = startY;

        // --- Кнопка режима градиента ---
        gradientModeButton = new GuiButton(GRADIENT_MODE_ID,
                centerX - buttonWidth / 2, currentY,
                buttonWidth, widgetHeight,
                getGradientButtonText()); // Получаем текст кнопки
        this.buttonList.add(gradientModeButton);
        currentY += rowHeight;

        // --- Кнопка режима пульсации ---
        pulseModeButton = new GuiButton(PULSE_MODE_ID,
                centerX - buttonWidth / 2, currentY,
                buttonWidth, widgetHeight,
                getPulseButtonText());
        this.buttonList.add(pulseModeButton);
        currentY += rowHeight;

        // --- Чекбокс процентов ---
        percentBarsCheckbox = new GuiCheckBox(PERCENT_BARS_ID,
                centerX - buttonWidth / 2, currentY,
                I18n.format("esmndnewhud.checkbox.show_percent_bars"), // Локализация
                config.isPercentBars());
        this.buttonList.add(percentBarsCheckbox);
        currentY += rowHeight;

        // --- Чекбокс фикс. размера ---
        fixedHUDSizeCheckbox = new GuiCheckBox(FIXED_HUD_SIZE_ID,
                centerX - buttonWidth / 2, currentY,
                I18n.format("esmndnewhud.checkbox.size_dependency"),
                config.isFixedSizeHUD());
        this.buttonList.add(fixedHUDSizeCheckbox);
        currentY += rowHeight;

        // --- Слайдер масштаба ---
        double minScale = 0.3;
        double maxScale = 4.0;
        double currentScale = config.getHudScaleFixed();

        hudScaleSlider = new OptionSlider(
                HUD_SCALE_SLIDER_ID, // ID кнопки/слайдера
                centerX - buttonWidth / 2, currentY,
                buttonWidth, widgetHeight,
                minScale, maxScale, currentScale, // min, max, current (реальные)
                "esmndnewhud.slider.size_dependency", // Ключ локализации для префикса
                config::setHudScaleFixed // Наш Consumer<Double>
        );
        this.buttonList.add(hudScaleSlider);
        // Устанавливаем видимость/активность ПОСЛЕ создания
        updateSliderState();
        currentY += rowHeight;


        // --- Кнопка "Изменение цвета" ---
        changeColorButton = new GuiButton(CHANGE_COLOR_ID,
                centerX - 50, // Примерно по центру
                currentY + spacing, // Небольшой отступ вниз
                100, widgetHeight,
                I18n.format("esmndnewhud.button.changecolor"));
        this.buttonList.add(changeColorButton);
        // currentY += rowHeight; // Не увеличиваем Y, т.к. кнопки "Save" и "Done" ниже


        // --- Кнопки Сохранить и Готово ---
        int bottomButtonY = this.height - 28;
        saveButton = new GuiButton(SAVE_BUTTON_ID,
                centerX - 100 - 2, bottomButtonY,
                100, widgetHeight,
                I18n.format("esmndnewhud.button.save"));
        this.buttonList.add(saveButton);

        doneButton = new GuiButton(DONE_BUTTON_ID,
                centerX + 2, bottomButtonY,
                100, widgetHeight,
                I18n.format("gui.done")); // Стандартная кнопка "Готово"
        this.buttonList.add(doneButton);
    }

    /**
     * Обработка нажатий кнопок.
     */
    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button); // Важно для GuiScreen

        switch (button.id) {
            case GRADIENT_MODE_ID:
                cycleGradientMode();
                gradientModeButton.displayString = getGradientButtonText(); // Обновляем текст кнопки
                break;
            case PULSE_MODE_ID:
                cyclePulseMode();
                pulseModeButton.displayString = getPulseButtonText();
                break;
            case CHANGE_COLOR_ID:
                 this.mc.displayGuiScreen(new esmndConfigGUIColors(this));
                break;
            case FIXED_HUD_SIZE_ID:
                // Checkbox сам меняет состояние, нам нужно обновить слайдер
                updateSliderState();
                // Сохраняем значение чекбокса в конфиг сразу
                config.setFixedSizeHUD(fixedHUDSizeCheckbox.isChecked());
                break;
            case PERCENT_BARS_ID:
                // Сохраняем значение чекбокса в конфиг сразу
                config.setPercentBars(percentBarsCheckbox.isChecked());
                break;
            // Слайдер обрабатывается через ISlider responder
            // case HUD_SCALE_SLIDER_ID: break;
            case SAVE_BUTTON_ID:
                saveChanges();
                break;
            case DONE_BUTTON_ID:
                // Можно сохранить перед выходом, если нужно
                // saveChanges();
                this.mc.displayGuiScreen(this.parentScreen); // Возвращаемся
                break;
        }
    }

    /**
     * Обновляет состояние (видимость, активность) слайдера
     * в зависимости от чекбокса fixedHUDSize.
     */
    private void updateSliderState() {
        if (hudScaleSlider != null) {
            boolean enabled = fixedHUDSizeCheckbox.isChecked();
            hudScaleSlider.visible = enabled;
            hudScaleSlider.enabled = enabled; // enabled в 1.12.2
        }
    }

    /**
     * Циклически переключает режим градиента.
     */
    private void cycleGradientMode() {
        HUDColors.GradientModeUse[] modes = HUDColors.GradientModeUse.values();
        int nextOrdinal = (currentGradientMode.ordinal() + 1) % modes.length;
        currentGradientMode = modes[nextOrdinal];
    }

    /**
     * Циклически переключает режим пульсации.
     */
    private void cyclePulseMode() {
        HUDColors.PulseLowHealthMode[] modes = HUDColors.PulseLowHealthMode.values();
        int nextOrdinal = (currentPulseMode.ordinal() + 1) % modes.length;
        currentPulseMode = modes[nextOrdinal];
    }

    /**
     * Возвращает локализованный текст для кнопки градиента.
     */
    private String getGradientButtonText() {
        String key = "esmndnewhud.button.gradientoff"; // Default
        switch (currentGradientMode) {
            case HARDCORE_ONLY: key = "esmndnewhud.button.gradienthardcore"; break;
            case SURVIVAL_ONLY: key = "esmndnewhud.button.gradientsurvival"; break;
            case BOTH:          key = "esmndnewhud.button.gradientboth"; break;
        }
        return I18n.format(key);
    }

    /**
     * Возвращает локализованный текст для кнопки пульсации.
     */
    private String getPulseButtonText() {
        String key = "esmndnewhud.button.pulseoff"; // Default
        switch (currentPulseMode) {
            case HARDCORE_ONLY: key = "esmndnewhud.button.pulsehardcore"; break;
            case SURVIVAL_ONLY: key = "esmndnewhud.button.pulsesurvival"; break;
            case BOTH:          key = "esmndnewhud.button.pulseboth"; break;
        }
        return I18n.format(key);
    }

    /**
     * Сохраняет текущие настройки в объект конфига и файл.
     */
    private void saveChanges() {
        try {
            // Обновляем конфиг из текущих состояний GUI
            config.setGradientMode(currentGradientMode);
            config.setPulseMode(currentPulseMode);
            config.setPercentBars(percentBarsCheckbox.isChecked());
            config.setFixedSizeHUD(fixedHUDSizeCheckbox.isChecked());
            // Значение слайдера уже должно быть сохранено через ISlider
            // config.setHudScaleFixed(hudScaleSlider.getValue());

            // Сохраняем весь объект конфига (вызовет saveConfig внутри сеттеров, но для надежности можно вызвать еще раз)
            // config.saveConfig(); // Не обязательно, если сеттеры уже сохраняют

            // Показываем сообщение об успешном сохранении
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.GREEN + "HUD Parameters saved successfully!"));
            }
        } catch (Exception e) {
            System.err.println("Error saving HUD config from GUI:");
            e.printStackTrace();
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(TextFormatting.RED + "Error saving HUD Parameters! Check logs."));
            }
        }
    }

    /**
     * Отрисовка экрана.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground(); // Рисуем фон
        this.drawCenteredString(this.fontRenderer, I18n.format("esmndnewhud.configscreen.title"), this.width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks); // Рисуем кнопки и виджеты

        // --- ИЗМЕНЕНО: Собираем тултип и определяем Y для отрисовки ---
        List<String> tooltipLines = new ArrayList<>();
        int tooltipDrawY = mouseY; // По умолчанию рисуем у мыши
        int widgetHeight = 20; // Стандартная высота виджета
        int tooltipOffset = 8; // Небольшой отступ под виджетом

        // Определяем, над каким виджетом мышь, и собираем тултип
        if (gradientModeButton != null && gradientModeButton.isMouseOver()) {
            tooltipLines.add(I18n.format("esmndnewhud.button.gradienttooltip"));
            tooltipDrawY = gradientModeButton.y + widgetHeight + tooltipOffset; // Y под кнопкой
        } else if (pulseModeButton != null && pulseModeButton.isMouseOver()) {
            tooltipLines.add(I18n.format("esmndnewhud.button.pulsetooltip"));
            tooltipDrawY = pulseModeButton.y + widgetHeight + tooltipOffset;
        } else if (fixedHUDSizeCheckbox != null && fixedHUDSizeCheckbox.isMouseOver()) {
            tooltipLines.add(I18n.format("esmndnewhud.checkbox.size_dependency_tooltip"));
            tooltipDrawY = fixedHUDSizeCheckbox.y + widgetHeight + tooltipOffset; // Используем Y чекбокса
        } else if (hudScaleSlider != null && hudScaleSlider.isMouseOver() && hudScaleSlider.visible) {
            tooltipLines.add(I18n.format("esmndnewhud.slider.size_dependency_tooltip"));
            tooltipDrawY = hudScaleSlider.y + widgetHeight + tooltipOffset;
        }
        // Добавь другие 'else if' для остальных виджетов

        // Рисуем тултип стандартным методом, но со сдвинутой Y координатой
        if (!tooltipLines.isEmpty()) {
            // Дополнительная проверка, чтобы тултип не уходил за нижний край экрана
            int tooltipHeight = 8 + (tooltipLines.size() > 1 ? (tooltipLines.size() - 1) * 10 + 2 : 0);
            if (tooltipDrawY + tooltipHeight > this.height) {
                // Если уходит вниз, рисуем над виджетом или просто у мыши
                // Найдем Y виджета, над которым курсор (это немного дублирует логику выше)
                int widgetY = -1;
                if (gradientModeButton != null && gradientModeButton.isMouseOver()) widgetY = gradientModeButton.y;
                else if (pulseModeButton != null && pulseModeButton.isMouseOver()) widgetY = pulseModeButton.y;
                else if (fixedHUDSizeCheckbox != null && fixedHUDSizeCheckbox.isMouseOver()) widgetY = fixedHUDSizeCheckbox.y;
                else if (percentBarsCheckbox != null && percentBarsCheckbox.isMouseOver()) widgetY = percentBarsCheckbox.y;
                else if (hudScaleSlider != null && hudScaleSlider.isMouseOver()) widgetY = hudScaleSlider.y;

                if (widgetY != -1 && widgetY - tooltipHeight - 5 >= 0) { // Если есть место над виджетом
                    tooltipDrawY = widgetY - tooltipHeight - 5;
                } else { // Иначе рисуем у курсора (стандартное поведение)
                    tooltipDrawY = mouseY;
                }
            }

            // Рисуем стандартный тултип
            this.drawHoveringText(tooltipLines, mouseX, tooltipDrawY);
        }
        // --- КОНЕЦ ИЗМЕНЕНИЯ ---
    }

    /**
     * Вызывается при нажатии клавиш.
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Закрываем по Escape
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    /**
     * Должен ли экран ставить игру на паузу?
     */
    @Override
    public boolean doesGuiPauseGame() {
        return false; // Обычно конфиг экраны не ставят игру на паузу
    }
}