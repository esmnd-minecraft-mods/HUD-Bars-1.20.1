package com.esmnd.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;

public class esmndHUDConfigScreen extends Screen {
    private PulseLowHealthMode pulseLowHealthMode;
    private GradientModeUse gradientMode;

    private Button gradientModeButton = null;
    private Button pulseModeButton = null;

    private CheckboxButton fixedHUDSize;
    private CheckboxButton percentBars;

    private OptionSlider hudScaleSlider;

    private final HUDColors config;

    private Screen parent;

    public enum PulseLowHealthMode {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;

        public TranslationTextComponent getDisplayName() {
            switch (this) {
                case OFF:
                    return new TranslationTextComponent("esmndnewhud.button.pulseoff");
                case HARDCORE_ONLY:
                    return new TranslationTextComponent("esmndnewhud.button.pulsehardcore");
                case SURVIVAL_ONLY:
                    return new TranslationTextComponent("esmndnewhud.button.pulsesurvival");
                case BOTH:
                    return new TranslationTextComponent("esmndnewhud.button.pulseboth");
                default:
                    return new TranslationTextComponent("Unknown");
            }
        }
    }

    public enum GradientModeUse {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;

        public TranslationTextComponent getDisplayName() {
            switch (this) {
                case OFF:
                    return new TranslationTextComponent("esmndnewhud.button.gradientoff");
                case HARDCORE_ONLY:
                    return new TranslationTextComponent("esmndnewhud.button.gradienthardcore");
                case SURVIVAL_ONLY:
                    return new TranslationTextComponent("esmndnewhud.button.gradientsurvival");
                case BOTH:
                    return new TranslationTextComponent("esmndnewhud.button.gradientboth");
                default:
                    return new TranslationTextComponent("Unknown");
            }
        }
    }

    public esmndHUDConfigScreen(Screen lastScreen) {
        super(new TranslationTextComponent("esmndnewhud.configscreen.title"));
        this.config = HUDColors.getInstance();
        this.pulseLowHealthMode = HUDColors.getInstance().getPulseMode();
        this.gradientMode = HUDColors.getInstance().getGradientModeUse();
        this.parent = lastScreen;
    }

    @Override
    protected void init() {
        super.init(); // Важно вызвать super.init() в 1.16.5

        int rowHeight = 25;
        int startY = this.height / 6;
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int widgetHeight = 20; // Стандартная высота виджетов
        int spacing = 5; // Небольшой отступ между элементами

        int currentY = startY;

        // --- ИЗМЕНЕНО: Создание кнопок через new Button(...) ---
        gradientModeButton = new Button(
                centerX - buttonWidth / 2, // x
                currentY,                  // y
                buttonWidth,               // width
                widgetHeight,              // height
                gradientMode.getDisplayName(), // message (ITextComponent)
                this::changeModeGradient,      // onPress
                (button, matrixStack, mouseX, mouseY) -> { // onTooltip
                    if (button.isHovered()) {
                        renderTooltip(matrixStack, new TranslationTextComponent("esmndnewhud.button.gradienttooltip"), mouseX, mouseY);
                    }
                }
        );
        this.addButton(gradientModeButton); // <--- ИЗМЕНЕНО: Используем this.addButton
        currentY += rowHeight; // Увеличиваем Y для следующего ряда

        pulseModeButton = new Button(
                centerX - buttonWidth / 2,
                currentY,
                buttonWidth,
                widgetHeight,
                pulseLowHealthMode.getDisplayName(),
                this::changeModePulse,
                (button, matrixStack, mouseX, mouseY) -> {
                    if (button.isHovered()) {
                        renderTooltip(matrixStack, new TranslationTextComponent("esmndnewhud.button.pulsetooltip"), mouseX, mouseY);
                    }
                }
        );
        this.addButton(pulseModeButton);
        currentY += rowHeight;

        // --- Кнопка "Изменение цвета" ---
        this.addButton(new Button(
                centerX - 50, // Примерно по центру
                currentY,
                100,          // Ширина
                widgetHeight,
                new TranslationTextComponent("esmndnewhud.button.changecolor"),
                (button) -> {
                    // Убедись, что esmndHUDConfig_ColorScreen адаптирован для 1.16.5
                    Minecraft.getInstance().setScreen(new esmndHUDConfig_ColorScreen(this));
                }
                // Без тултипа для этой кнопки
        ));
        currentY += rowHeight;

        // --- ИЗМЕНЕНО: Checkbox ---
        percentBars = new CheckboxButton(
                centerX - buttonWidth / 2, // x
                currentY,                  // y
                buttonWidth,               // width
                widgetHeight,              // height
                new TranslationTextComponent("esmndnewhud.checkbox.show_percent_bars"), // message (ITextComponent)
                config.isPercentBars(),    // isSelected
                true                       // visible - пока всегда видимый
        ) {
            // Переопределяем onPress для добавления логики
            @Override
            public void onPress() {
                super.onPress(); // Вызываем стандартное изменение состояния
                config.setPercentBars(this.selected()); // selected() в 1.16.5 или isSelected()
            }
            // Добавляем тултип через renderToolTip
            @Override
            public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY) {
                if (this.isHovered) { // isHovered в 1.16.5
                    // Используем метод рендера тултипа родительского экрана
                    esmndHUDConfigScreen.this.renderTooltip(matrixStack, new TranslationTextComponent("esmndnewhud.checkbox.show_percent_bars_tooltip"), mouseX, mouseY); // Пример тултипа
                }
            }
        };
        this.addButton(percentBars);
        currentY += rowHeight;

        fixedHUDSize = new CheckboxButton(
                centerX - buttonWidth / 2,
                currentY,
                buttonWidth,
                widgetHeight,
                new TranslationTextComponent("esmndnewhud.checkbox.size_dependency"),
                config.isFixedSizeHUD(),
                true
        ) {
            @Override
            public void onPress() {
                super.onPress();
                boolean selected = this.selected(); // selected() или isSelected()
                config.setFixedSizeHUD(selected);
                if (hudScaleSlider != null) { // Проверка на null перед доступом
                    hudScaleSlider.visible = selected;
                    hudScaleSlider.active = selected; // active в 1.16.5 или isActived()
                }
            }

            @Override
            public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY) {
                if (this.isHovered) {
                    esmndHUDConfigScreen.this.renderTooltip(matrixStack, new TranslationTextComponent("esmndnewhud.checkbox.size_dependency_tooltip"), mouseX, mouseY);
                }
            }
        };
        this.addButton(fixedHUDSize);
        currentY += rowHeight;

        // --- ИЗМЕНЕНО: OptionSlider ---
        // Используем конструктор нашего адаптированного OptionSlider (на базе Forge Slider)
        double savedScale = config.getHudScaleFixed();
        // Нам НЕ НУЖНА нормализация, так как наш OptionSlider принимает реальные min/max/value
        hudScaleSlider = new OptionSlider(
                centerX - buttonWidth / 2, // x
                currentY,                  // y
                buttonWidth,               // width
                widgetHeight,              // height
                0.3,                       // min (реальное)
                4.0,                       // max (реальное)
                savedScale,                // current (реальное)
                "esmndnewhud.slider.size_dependency", // Ключ для сообщения (для TranslationTextComponent внутри слайдера)
                config::setHudScaleFixed   // Consumer (ссылка на метод)
                // Тултип можно добавить в сам класс OptionSlider или здесь через onTooltip лямбду, если конструктор позволяет
        );
        // Добавляем тултип для слайдера отдельно (т.к. наш OptionSlider не имеет параметра onTooltip в конструкторе)
        // Это немного костыльно, лучше добавить onTooltip в OptionSlider, если возможно.
        // Как вариант - создать анонимный класс или обернуть OptionSlider.
        this.addButton(hudScaleSlider); // Добавляем слайдер
        currentY += rowHeight;

        // Устанавливаем видимость/активность слайдера ПОСЛЕ его создания
        hudScaleSlider.visible = config.isFixedSizeHUD();
        hudScaleSlider.active = config.isFixedSizeHUD(); // active или isActived

        // --- Кнопки Сохранить и Назад ---
        int bottomButtonY = this.height - 28;
        this.addButton(new Button(
                centerX - 100 - 2, // Немного левее центра
                bottomButtonY,
                100,
                widgetHeight,
                new TranslationTextComponent("esmndnewhud.button.save"),
                this::SaveChanges
        ));

        this.addButton(new Button(
                centerX + 2, // Немного правее центра
                bottomButtonY,
                100,
                widgetHeight,
                new TranslationTextComponent("esmndnewhud.button.return"), // В 1.16.5 часто используют "gui.done" или "gui.cancel"
                (button) -> this.minecraft.setScreen(parent) // parent должен быть не null
        ));
    }

    // --- ИЗМЕНЕНО: render() ---
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) { // <--- ИЗМЕНЕНО: MatrixStack
        this.renderBackground(matrixStack); // <--- ИЗМЕНЕНО: matrixStack
        // Рисуем заголовок
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF); // <--- ИЗМЕНЕНО: matrixStack

        // Рендерим все добавленные виджеты (кнопки, чекбоксы, слайдер)
        super.render(matrixStack, mouseX, mouseY, partialTicks); // <--- ИЗМЕНЕНО: matrixStack

        // Тултипы теперь обрабатываются в лямбдах onTooltip у кнопок или в renderToolTip у кастомных виджетов.
        // Если для слайдера не удалось добавить onTooltip, можно проверить hover здесь:
        if (hudScaleSlider != null && hudScaleSlider.isHovered() && hudScaleSlider.visible) { // isHovered() или isHovered
            renderTooltip(matrixStack, new TranslationTextComponent("esmndnewhud.slider.size_dependency_tooltip"), mouseX, mouseY);
        }

    }

    private void changeModeGradient(Button button) {
        switch (gradientMode) {
            case OFF:
                gradientMode = GradientModeUse.HARDCORE_ONLY;
                break;
            case HARDCORE_ONLY:
                gradientMode = GradientModeUse.SURVIVAL_ONLY;
                break;
            case SURVIVAL_ONLY:
                gradientMode = GradientModeUse.BOTH;
                break;
            case BOTH:
                gradientMode = GradientModeUse.OFF;
                break;
        }
        gradientModeButton.setMessage(gradientMode.getDisplayName());
    }

    private void changeModePulse(Button button) {
        switch (pulseLowHealthMode) {
            case OFF:
                pulseLowHealthMode = PulseLowHealthMode.HARDCORE_ONLY;
                break;
            case HARDCORE_ONLY:
                pulseLowHealthMode = PulseLowHealthMode.SURVIVAL_ONLY;
                break;
            case SURVIVAL_ONLY:
                pulseLowHealthMode = PulseLowHealthMode.BOTH;
                break;
            case BOTH:
                pulseLowHealthMode = PulseLowHealthMode.OFF;
                break;
        }
        pulseModeButton.setMessage(pulseLowHealthMode.getDisplayName());
    }

    @Override
    public void onClose() {
        // Можно добавить автосохранение при закрытии, если нужно
        // SaveChanges(null); // Вызвать сохранение
        this.minecraft.setScreen(parent); // Вернуться на родительский экран
    }

    private void SaveChanges(Button button) {
        try {
            config.setGradientMode(gradientMode);
            config.setPulseMode(pulseLowHealthMode);
            config.saveConfig();
        } catch (NumberFormatException e) {
            System.err.println(e);
        }
    }

    private int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFFFF;
        }
        return (int) Long.parseLong(hex, 16);
    }
}