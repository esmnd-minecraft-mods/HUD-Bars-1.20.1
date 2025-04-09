package com.esmnd.esmndhud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class esmndHUDConfigScreen extends Screen {

    private PulseLowHealthMode pulseLowHealthMode;
    private GradientModeUse gradientMode;

    private Button gradientModeButton = null;
    private Button pulseModeButton = null;

    private Checkbox fixedHUDSize = null;
    private Checkbox percentBars = null;

    private AbstractSliderButton hudScaleSlider;

    private final HUDColors config;

    private Screen parent;

    // Pulse when low Health
    public enum PulseLowHealthMode
    {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;

        public Component getDisplayName() {
            return switch (this)
            {
                case OFF -> Component.translatable("esmndnewhud.button.pulseoff");
                case HARDCORE_ONLY -> Component.translatable("esmndnewhud.button.pulsehardcore");
                case SURVIVAL_ONLY -> Component.translatable("esmndnewhud.button.pulsesurvival");
                case BOTH -> Component.translatable("esmndnewhud.button.pulseboth");
            };
        }
    }

    public enum GradientModeUse
    {
        HARDCORE_ONLY,
        SURVIVAL_ONLY,
        BOTH,
        OFF;

        public Component getDisplayName() {
            return switch (this) {
                case OFF -> Component.translatable("esmndnewhud.button.gradientoff");
                case HARDCORE_ONLY -> Component.translatable("esmndnewhud.button.gradienthardcore");
                case SURVIVAL_ONLY -> Component.translatable("esmndnewhud.button.gradientsurvival");
                case BOTH -> Component.translatable("esmndnewhud.button.gradientboth");
            };
        }
    }

    public esmndHUDConfigScreen(Screen lastScreen) {
        super(Component.translatable("esmndnewhud.configscreen.title"));
        this.config = HUDColors.getInstance();
        this.pulseLowHealthMode = HUDColors.getInstance().getPulseMode();
        this.gradientMode = HUDColors.getInstance().getGradientModeUse();
        this.parent = lastScreen;
    }

    @Override
    protected void init() {
        int rowHeight = 25;
        int startY = this.height / 6;
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int spacing = 0;

        int currentRow = 0;

        gradientModeButton = Button.builder(
                        gradientMode.getDisplayName(),
                        this::changeModeGradient)
                .pos(centerX - 100, startY + rowHeight * currentRow++)
                .size(buttonWidth, 20) // Увеличьте размер, чтобы текст помещался
                .build();

        // button change mode gradient
        addRenderableWidget(gradientModeButton);

        startY += spacing;

        // button change mode pulse
        pulseModeButton = Button.builder(
                        pulseLowHealthMode.getDisplayName(),
                        this::changeModePulse)

                .pos(centerX - 100, startY + rowHeight * currentRow++)
                .size(buttonWidth, 20) // Увеличьте размер, чтобы текст помещался
                .build();
        addRenderableWidget(pulseModeButton);

        startY += spacing;

        // Кнопка "Изменение цвета"
        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.changecolor"), button -> {
                    // Переходим к экрану изменения цвета
                    Minecraft.getInstance().setScreen(new esmndHUDConfig_ColorScreen(this));
                }).pos(this.width / 2 - 50, this.height / 2) // Центр экрана
                .size(100, 20) // Размер кнопки
                .build());
        startY += spacing;

        percentBars = new Checkbox(
                centerX - 100, // x
                startY + rowHeight * currentRow++, // y
                buttonWidth, // width
                20, // height
                Component.translatable("esmndnewhud.checkbox.show_percent_bars"), // text
                config.isPercentBars()
        )
        {
            @Override
            public void onPress() {
                super.onPress();
                config.setPercentBars(this.selected());
            }
        };
        addRenderableWidget(percentBars);
        startY += spacing;

        fixedHUDSize = new Checkbox(
                centerX - 100, // x
                startY + rowHeight * currentRow++, // y
                buttonWidth, // width
                20, // height
                Component.translatable("esmndnewhud.checkbox.size_dependency"), // text
                config.isFixedSizeHUD()
        ) {
            @Override
            public void onPress() {
                super.onPress();
                config.setFixedSizeHUD(this.selected());

                hudScaleSlider.visible = this.selected();
                hudScaleSlider.active = this.selected();
            }
        };
        addRenderableWidget(fixedHUDSize);
        startY += spacing;

        double savedScale = config.getHudScaleFixed();
        double normalizedValue = (savedScale - 0.3) / (4.0 - 0.3);

        hudScaleSlider = new OptionSlider(centerX - 100, startY + rowHeight * currentRow++, 200, 20,
                0.3, 4.0, normalizedValue,
                newValue -> config.setHudScaleFixed(newValue));

        addRenderableWidget(hudScaleSlider);
        hudScaleSlider.visible = config.isFixedSizeHUD();
        hudScaleSlider.active = config.isFixedSizeHUD();

        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.save"), this::SaveChanges)
                .pos(this.width / 2 - 154, this.height - 28)
                .size(100, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("esmndnewhud.button.return"), button ->
                        this.minecraft.setScreen(parent))
                .pos(this.width / 2 - 50, this.height - 28)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Отрисовка фона и заголовка
        PoseStack poseStack = guiGraphics.pose();

        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        if (gradientModeButton.isMouseOver(mouseX, mouseY))
            guiGraphics.renderTooltip(this.font, Component.translatable("esmndnewhud.button.gradienttooltip"), mouseX, mouseY);

        if (pulseModeButton.isMouseOver(mouseX, mouseY))
            guiGraphics.renderTooltip(this.font, Component.translatable("esmndnewhud.button.pulsetooltip"), mouseX, mouseY);

        if (fixedHUDSize.isMouseOver(mouseX, mouseY))
            guiGraphics.renderTooltip(this.font, Component.translatable("esmndnewhud.checkbox.size_dependency_tooltip"), mouseX, mouseY);

        if (hudScaleSlider.isMouseOver(mouseX, mouseY))
            guiGraphics.renderTooltip(this.font, Component.translatable("esmndnewhud.slider.size_dependency_tooltip"), mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void changeModeGradient(Button button) {
        gradientMode = switch (gradientMode) {
            case OFF -> GradientModeUse.HARDCORE_ONLY;
            case HARDCORE_ONLY -> GradientModeUse.SURVIVAL_ONLY;
            case SURVIVAL_ONLY -> GradientModeUse.BOTH;
            case BOTH -> GradientModeUse.OFF;
        };
        gradientModeButton.setMessage(gradientMode.getDisplayName());
    }

    private void changeModePulse(Button button)
    {
        pulseLowHealthMode = switch (pulseLowHealthMode) {
            case OFF -> PulseLowHealthMode.HARDCORE_ONLY;
            case HARDCORE_ONLY -> PulseLowHealthMode.SURVIVAL_ONLY;
            case SURVIVAL_ONLY -> PulseLowHealthMode.BOTH;
            case BOTH -> PulseLowHealthMode.OFF;
        };
        pulseModeButton.setMessage(pulseLowHealthMode.getDisplayName());
    }

    private void SaveChanges(Button button) {
        try {
            // Сохраняем значения в конфиг
            config.setGradientMode(gradientMode);
            config.setPulseMode(pulseLowHealthMode);
            config.saveConfig();

            // Показываем сообщение об успешном сохранении
            assert minecraft != null;
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§aHUD Parameters saved successfully!"), false);
            }
        } catch (NumberFormatException e) {
            assert minecraft != null;
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§cError HUD Parameters: Invalid format!"), true);
            }
        }
    }

    private int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return 0xFFFFFFFF;
        }
        //return Integer.parseInt(hex, 16);
        return (int) Long.parseLong(hex, 16);
    }
}

