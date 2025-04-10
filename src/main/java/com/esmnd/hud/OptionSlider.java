package com.esmnd.hud;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

import java.util.function.Consumer;

public class OptionSlider extends Slider {
    private final Consumer<Double> onValueChanged;
    private final String translationKey;
    // Forge Slider сам хранит min/max/value, нам не нужно их дублировать

    public OptionSlider(int x, int y, int width, int height,
                        double min, double max, double currentValue, // Наши реальные значения
                        String translationKey, Consumer<Double> onValueChanged) {

        // Вызываем конструктор Forge Slider, подставляя наши значения
        super(x, y, width, height,
                StringTextComponent.EMPTY, // prefix - пока пустой
                StringTextComponent.EMPTY, // suffix - пока пустой
                min,                       // minVal - наш минимум
                max,                       // maxVal - наш максимум
                currentValue,              // currentVal - наше текущее значение
                true,                      // showDec - показываем дробную часть
                true,                      // drawStr - пусть слайдер рисует сообщение
                (button) -> {}             // handler (IPressable) - пустой обработчик нажатия,
                // основная логика будет при изменении значения
        );

        this.onValueChanged = onValueChanged;
        this.translationKey = translationKey;

        // Сразу устанавливаем корректное сообщение (это может переопределить updateMessage)
        updateSliderMessage();
    }

    /**
     * Этот метод вызывается Forge Slider для генерации сообщения.
     * Переопределяем его, чтобы использовать наш формат и ключ перевода.
     */
    @Override
    public void updateSlider() {
        // Этот метод вызывается при изменении значения слайдера (в т.ч. во время перетаскивания)
        super.updateSlider(); // Даем родительскому классу обновить внутреннее значение
        updateSliderMessage(); // Обновляем наше сообщение
        // Вызываем наш Consumer *только* когда значение применяется (в applyValue/onRelease)
    }

    /**
     * Обновляет текст сообщения слайдера.
     */
    private void updateSliderMessage() {
        // getValue() возвращает текущее значение слайдера в диапазоне min/max
        double actualValue = this.getValue();
        setMessage(new TranslationTextComponent(this.translationKey, String.format("%.2f", actualValue)));
    }


    /**
     * Этот метод (или аналогичный, например onRelease) вызывается,
     * когда пользователь отпускает кнопку мыши после изменения значения.
     * Здесь мы должны вызвать наш Consumer.
     * ВАЖНО: В Forge 1.16.5 Slider может не быть прямого applyValue.
     * Логику вызова Consumer часто помещают в onRelease или внутри updateSlider,
     * но вызов в updateSlider может быть слишком частым.
     * Попробуем переопределить onRelease.
     */
    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY); // Вызов родительского метода
        // Вызываем наш Consumer с текущим значением слайдера
        if (this.onValueChanged != null) {
            // getValue() из Forge Slider возвращает актуальное значение
            this.onValueChanged.accept(this.getValue());
        }
    }

    // getValue() уже есть в Forge Slider, возвращает текущее значение double
    // public double getCurrentValue() { return this.getValue(); }

    // setValue(double) тоже есть в Forge Slider
    // public void setCurrentValue(double newValue) { this.setValue(newValue); }
}
