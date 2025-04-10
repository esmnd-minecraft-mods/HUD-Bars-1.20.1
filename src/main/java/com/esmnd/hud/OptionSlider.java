package com.esmnd.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
// Импортируем GuiSlider и его интерфейс ISlider из Forge config API
import net.minecraftforge.fml.client.config.GuiSlider;

import java.util.function.Consumer;

public class OptionSlider extends GuiSlider {

    private final Consumer<Double> onValueChanged;
    private final String labelPrefix;
    private double lastNotifiedValue; // Храним последнее значение, о котором уведомили Consumer

    public OptionSlider(int id, int x, int y, int width, int height,
                        double min, double max, double currentValue,
                        String translationKey, Consumer<Double> onValueChanged) {

        // Вызываем конструктор родителя GuiSlider, передаем null вместо ISlider
        super(id, x, y, width, height,
                I18n.format(translationKey) + ": ", // Префикс
                "", // Суффикс
                min, max, currentValue,
                true, // showDec
                true, // drawString
                null); // <--- Передаем null вместо this

        this.onValueChanged = onValueChanged;
        this.labelPrefix = I18n.format(translationKey) + ": ";
        this.lastNotifiedValue = currentValue; // Инициализируем последним значением

        // Сразу устанавливаем правильную строку
        updateDisplayString();
    }

    /**
     * Обновляет отображаемую строку с форматированием.
     */
    private void updateDisplayString() {
        double actualValue = this.getValue();
        this.displayString = this.labelPrefix + String.format("%.2f", actualValue);
    }

    /**
     * Вызывается при нажатии кнопки мыши на слайдере.
     * Обрабатываем нажатие и обновляем строку.
     */
    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        // Сначала даем родительскому классу обработать нажатие
        boolean pressed = super.mousePressed(mc, mouseX, mouseY);
        if (pressed) {
            // Сразу обновляем строку при нажатии
            updateDisplayString();
        }
        return pressed;
    }


    /**
     * Вызывается при перетаскивании ползунка.
     * Обновляем строку и (опционально) вызываем Consumer.
     */
    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        // Даем родительскому классу обновить значение слайдера
        super.mouseDragged(mc, mouseX, mouseY);
        // После обновления значения - обновляем нашу строку
        updateDisplayString();

        // --- ОПЦИОНАЛЬНО: Вызывать Consumer "в реальном времени" при перетаскивании ---
        // double currentValue = this.getValue();
        // if (currentValue != this.lastNotifiedValue && this.onValueChanged != null) {
        //     this.onValueChanged.accept(currentValue);
        //     this.lastNotifiedValue = currentValue;
        // }
        // --- КОНЕЦ ОПЦИОНАЛЬНОГО БЛОКА ---
    }

    /**
     * Вызывается при отпускании кнопки мыши после перетаскивания.
     * Здесь мы точно должны вызвать Consumer, если значение изменилось.
     */
    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        super.mouseReleased(mouseX, mouseY); // Завершаем перетаскивание в родительском классе

        // Вызываем Consumer, если значение изменилось с момента последнего уведомления
        double currentValue = this.getValue();
        if (currentValue != this.lastNotifiedValue && this.onValueChanged != null) {
            this.onValueChanged.accept(currentValue);
            this.lastNotifiedValue = currentValue;
            // System.out.println("Slider value applied: " + currentValue); // Для отладки
        }
    }

    /**
     * Переопределяем setValue для обновления строки при программном изменении значения.
     */
    @Override
    public void setValue(double d) {
        super.setValue(d);
        updateDisplayString();
        // Обновляем lastNotifiedValue, если значение меняется программно
        this.lastNotifiedValue = this.getValue();
    }
}