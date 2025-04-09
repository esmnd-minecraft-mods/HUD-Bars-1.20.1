package com.esmnd.modhud;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class OptionSlider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final Consumer<Double> onValueChanged;

    public OptionSlider(int x, int y, int width, int height, double min, double max, double value, Consumer<Double> onValueChanged) {
        super(x, y, width, height, Component.empty(), value); // value от 0.0 до 1.0
        this.min = min;
        this.max = max;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double actualValue = min + value * (max - min);
        this.setMessage(Component.translatable("esmndnewhud.slider.size_dependency", String.format("%.2f", actualValue)));
    }

    @Override
    protected void applyValue() {
        double actualValue = min + value * (max - min);
        onValueChanged.accept(actualValue);
        updateMessage();
    }
}
