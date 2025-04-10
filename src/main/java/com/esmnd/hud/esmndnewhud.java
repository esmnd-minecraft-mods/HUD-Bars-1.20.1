package com.esmnd.hud;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.EnchantmentScreen;
import net.minecraft.client.gui.screen.inventory.AnvilScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("esmndnewhud")
public class esmndnewhud {
    private float smoothHealth = 10f;
    private float smoothFood = 20f;
    private float smoothArmor = 10f;
    private float smoothXp = 20f;

    // *** НОВЫЕ ПОЛЯ для временного показа уровня ***
    private int previousLevel = -1; // Предыдущий уровень игрока
    private long levelUpTimestamp = 0; // Время последнего повышения уровня (мс)
    private static final long LEVEL_UP_DISPLAY_DURATION_MS = 3000; // Как долго показывать текст (3 сек)

    private boolean isDisplayingAirBar = false;
    private int lastAirSupply = 0;
    private int maxAirSupply = 0;
    private float airPercentage = 0f;
    private boolean wasUnderWater = false;

    // --- ИЗМЕНЕНО: Record заменен на обычный класс ---
    private static class PercentTextInfo {
        final String text;
        final int pixelY;
        PercentTextInfo(String text, int pixelY) {
            this.text = text;
            this.pixelY = pixelY;
        }
        // Геттеры или прямой доступ к полям
        public String getText() { return text; }
        public int getPixelY() { return pixelY; }
    }
    // --------------------------------------------

    public esmndnewhud() {
        MinecraftForge.EVENT_BUS.register(this);

        // screen seting

        ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY,
                () -> (mc, previousScreen) -> new esmndHUDConfigScreen(previousScreen)
        );
    }


    /**
     * Интерполяция цвета от startColor до endColor на основе процента (progress).
     * @param startColor Начальный цвет (ARGB).
     * @param endColor Конечный цвет (ARGB).
     * @param progress Прогресс от 0.0 до 1.0.
     * @return Интерполированный цвет.
     */
    private int interpolateColor(int startColor, int endColor, float progress) {
        int startA = (startColor >> 24) & 0xFF;
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;

        int endA = (endColor >> 24) & 0xFF;
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;

        int interpolatedA = (int) (startA + (endA - startA) * progress);
        int interpolatedR = (int) (startR + (endR - startR) * progress);
        int interpolatedG = (int) (startG + (endG - startG) * progress);
        int interpolatedB = (int) (startB + (endB - startB) * progress);

        return (interpolatedA << 24) | (interpolatedR << 16) | (interpolatedG << 8) | interpolatedB;
    }

    // --- ИЗМЕНЕНА сигнатура и реализация fillPixelPerfect ---
    private void fillPixelPerfect(MatrixStack matrixStack, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        // Получаем матрицу из MatrixStack
        Matrix4f matrix = matrixStack.last().pose(); // В 1.16.5 может быть matrixStack.getLast().getMatrix() или похожее
        int x2 = x + width;
        int y2 = y + height;

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        // Настройки RenderSystem (проверить актуальность для 1.16.5)
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Или RenderSystem.blendFuncSeparate(...)
        // RenderSystem.setShader(GameRenderer::getPositionColorShader); // <--- ЗАМЕНИТЬ: В 1.16.5 обычно не нужно указывать шейдер так для простых цветов
        RenderSystem.disableTexture(); // Важно отключить текстуры для цветных прямоугольников

        Tessellator tesselator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        // Формат вершин и режим (GL11.GL_QUADS может быть нужен)
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR); // 7 это GL_QUADS

        // Добавляем вершины с использованием Matrix4f из MatrixStack
        bufferbuilder.vertex(matrix, (float)x,  (float)y2, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, (float)x2, (float)y2, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, (float)x2, (float)y,  0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, (float)x,  (float)y,  0.0F).color(r, g, b, a).endVertex();

        // Отрисовка буфера (В 1.16.5 может быть tesselator.draw() или через BufferUploader)
        tesselator.end(); // или BufferUploader.draw(bufferbuilder.end());

        RenderSystem.enableTexture(); // Включаем текстуры обратно
        RenderSystem.disableBlend();
    }
    // --- КОНЕЦ ИЗМЕНЕНИЙ fillPixelPerfect ---

    public boolean checkIfPlayerInCreative() {
        assert Minecraft.getInstance().player != null;
        return Minecraft.getInstance().player.isCreative();
    }

    public boolean checkIfOpenedInventoryOrMagicWindow() {
        return Minecraft.getInstance().screen instanceof InventoryScreen
                || Minecraft.getInstance().screen instanceof EnchantmentScreen
                || Minecraft.getInstance().screen instanceof AnvilScreen;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        // В 1.16.5 используются другие элементы оверлея
        if (event.getType() == RenderGameOverlayEvent.ElementType.HEALTH ||
                event.getType() == RenderGameOverlayEvent.ElementType.FOOD ||
                event.getType() == RenderGameOverlayEvent.ElementType.ARMOR ||
                event.getType() == RenderGameOverlayEvent.ElementType.EXPERIENCE ||
                event.getType() == RenderGameOverlayEvent.ElementType.AIR) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // Рендерим наши элементы ПОСЛЕ того, как определенный ванильный элемент был (или должен был быть) отрендерен
        // Обычно привязываются к ElementType.HOTBAR или ElementType.ALL, чтобы рисовать поверх всего
        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && !checkIfPlayerInCreative()) { // Или ElementType.ALL
            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player; // PlayerEntity

            if (player == null || mc.level == null) { // level вместо world в 1.16.5 часто
                previousLevel = -1;
                levelUpTimestamp = 0;
                return;
            }

            // Проверить методы получения данных игрока (getHealth, getFoodStats, getArmorValue и т.д.)
            smoothHealth += (player.getHealth() - smoothHealth) * 0.1f;
            smoothFood += (player.getFoodData().getFoodLevel() - smoothFood) * 0.1f; // getFoodData() или getFoodStats()
            smoothArmor += (player.getArmorValue() - smoothArmor) * 0.1f;
            float experienceProgress = player.experienceProgress;
            smoothXp += (experienceProgress - smoothXp) * 0.1f;
            float absorption = player.getAbsorptionAmount();
            int currentLevel = player.experienceLevel;

            float maxHealth = player.getMaxHealth();
            float healthPercent = maxHealth > 0 ? Math.max(0, Math.min(smoothHealth / maxHealth, 1.0f)) : 0;
            float armorPercent = Math.max(0, Math.min(smoothArmor / 20.0f, 1.0f)); // Макс броня все еще 20? Проверить.
            float foodPercent = Math.max(0, Math.min(smoothFood / 20.0f, 1.0f));
            float xpPercent = Math.max(0, Math.min(smoothXp, 1.0f));
            float absorptionDisplayPercent = Math.max(0, Math.min(absorption / 20.0f, 1.0f)); // Отображение относительно 20
            float absorptionActualPercent = maxHealth > 0 ? Math.max(0, Math.min(absorption / maxHealth, 1.0f)) : 0; // Реальный процент

            // Получаем MatrixStack из эвента
            MatrixStack matrixStack = event.getMatrixStack(); // <--- ИЗМЕНЕНО

            boolean inventoryOpen = checkIfOpenedInventoryOrMagicWindow();
            boolean recentlyLeveledUp = System.currentTimeMillis() < levelUpTimestamp + LEVEL_UP_DISPLAY_DURATION_MS;
            boolean shouldShowLevelText = inventoryOpen || recentlyLeveledUp;

            if (previousLevel == -1) {
                previousLevel = currentLevel;
            } else if (currentLevel > previousLevel) {
                levelUpTimestamp = System.currentTimeMillis();
                previousLevel = currentLevel;
            } else if (currentLevel < previousLevel) {
                previousLevel = currentLevel;
            }

            final int TEXT_COLOR = 0xFFFFFFFF;
            final int TEXT_OFFSET_X_PIXELS = 4;
            final float TEXT_OFFSET_X_SCALED = 2.0f;
            final int FONT_HEIGHT = mc.font.lineHeight; // font вместо fontRenderer

            // =============================================================
            // === РЕЖИМ ФИКСИРОВАННОГО РАЗМЕРА (НЕ ЗАВИСИТ ОТ GUI SCALE) ===
            // =============================================================
            if (HUDColors.getInstance().isFixedSizeHUD()) { // Убедись, что HUDColors адаптирован
                int screenWidth = event.getWindow().getWidth();
                int screenHeight = event.getWindow().getHeight();
                double minecraftGuiScale = event.getWindow().getGuiScale();
                float mcGuiScaleFactor = (minecraftGuiScale <= 0) ? 1.0f : (float) minecraftGuiScale;
                float fixedHudScale = (float)HUDColors.getInstance().getHudScaleFixed();

                int baseFixedBarWidth = 100;
                int baseFixedBarHeight = 5 + 3 * 2;
                int baseFixedOutline = 3;
                int baseFixedOffsetX = 15;
                int basePixelOffsetY = screenHeight / 8;
                int baseFixedVerticalSpacing = 5;
                int baseTextPixelOffsetX = 5;
                int baseTextPixelOffsetY = 3;

                int actualFixedOutline = Math.max(1, Math.round(baseFixedOutline * fixedHudScale));
                int actualTotalBarWidth = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarWidth * fixedHudScale));
                int actualTotalBarHeight = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarHeight * fixedHudScale));
                int actualFixedOffsetX = Math.round(baseFixedOffsetX * fixedHudScale);
                int actualFixedOffsetY = Math.round(basePixelOffsetY * fixedHudScale);
                int actualFixedVerticalSpacing = Math.max(1, Math.round(baseFixedVerticalSpacing * fixedHudScale));
                int actualTextPixelOffsetX = Math.round(baseTextPixelOffsetX * fixedHudScale);
                int actualTextPixelOffsetY = Math.round(baseTextPixelOffsetY * fixedHudScale);

                int pixelX = screenWidth - actualTotalBarWidth - actualFixedOffsetX;
                int pixelY_start = actualFixedOffsetY;
                int currentPixelY = pixelY_start;
                int lastBarBottomPixelY = 0;

                // Используем созданный PercentTextInfo class
                java.util.List<PercentTextInfo> percentTexts = new java.util.ArrayList<>();

                matrixStack.pushPose(); // <--- ИЗМЕНЕНО: pushPose вместо push
                matrixStack.scale(1.0f / mcGuiScaleFactor, 1.0f / mcGuiScaleFactor, 1.0f);

                float scaledTextWidth;
                int contentHeight = actualTotalBarHeight - 2 * actualFixedOutline;
                int contentCenterYOffset = contentHeight / 2;

                // Передаем matrixStack вместо poseStack в методы отрисовки
                if (absorption > 0) {
                    drawBarPixel(matrixStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, absorptionDisplayPercent, HUDColors.getInstance().getHealthBarBackgroundColor(), HUDColors.absorptionBarColor);
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", absorptionDisplayPercent * 100.0f), currentPixelY));
                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;
                }

                drawCombineBarHealthPixel(matrixStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, healthPercent, absorptionActualPercent);
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", healthPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;

                if (player.getArmorValue() > 0) { // Используем player
                    drawBarPixel(matrixStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, armorPercent, HUDColors.getInstance().getArmorBarBackgroundColor(), HUDColors.getInstance().getArmorBarColor());
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", armorPercent * 100.0f), currentPixelY));
                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;
                }

                drawBarPixel(matrixStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, foodPercent, HUDColors.getInstance().getFoodBarBackgroundColor(), HUDColors.getInstance().getFoodBarColor());
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", foodPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;

                int tempYBeforeXP = currentPixelY;
                drawXPAndAirBarPixel(matrixStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, mc, xpPercent);
                boolean displayingAir = this.isDisplayingAirBar;
                if (!displayingAir) {
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", xpPercent * 100.0f), tempYBeforeXP));
                }
                lastBarBottomPixelY = currentPixelY + actualTotalBarHeight;

                matrixStack.popPose(); // <--- ИЗМЕНЕНО: popPose вместо pop

                if (HUDColors.getInstance().isPercentBars()) {
                    contentHeight = actualTotalBarHeight - 2 * actualFixedOutline;
                    contentCenterYOffset = contentHeight / 2;
                    for (PercentTextInfo info : percentTexts) {
                        scaledTextWidth = mc.font.width(info.getText()); // font вместо fontRenderer
                        float barLeftEdgeDrawX = (float) pixelX / mcGuiScaleFactor;
                        float barContentCenterDrawY = (float) (info.getPixelY() + actualFixedOutline + contentCenterYOffset) / mcGuiScaleFactor;
                        float textScaledOffsetX = (float) TEXT_OFFSET_X_PIXELS / mcGuiScaleFactor;
                        float textDrawX = barLeftEdgeDrawX - scaledTextWidth - textScaledOffsetX;
                        float textDrawY = barContentCenterDrawY - (float) FONT_HEIGHT / 2.0f;
                        // Проверить параметры drawShadow в 1.16.5
                        mc.font.drawShadow(matrixStack, info.getText(), textDrawX, textDrawY, TEXT_COLOR);
                    }
                }

                if (shouldShowLevelText) {
                    String levelText = String.valueOf(currentLevel);
                    scaledTextWidth = mc.font.width(levelText);
                    int barRightEdgePixelX = pixelX + actualTotalBarWidth;
                    float barRightEdgeDrawX = (float)barRightEdgePixelX / mcGuiScaleFactor;
                    float lastBarBottomDrawY = (float)lastBarBottomPixelY / mcGuiScaleFactor;
                    float textScaledOffsetX = (float)actualTextPixelOffsetX / mcGuiScaleFactor;
                    float textScaledOffsetY = (float)actualTextPixelOffsetY / mcGuiScaleFactor;
                    float textDrawX = barRightEdgeDrawX - scaledTextWidth - textScaledOffsetX;
                    float textDrawY = lastBarBottomDrawY + textScaledOffsetY;
                    mc.font.drawShadow(matrixStack, levelText, textDrawX, textDrawY, 0xFFFFFF);
                }
            }
            // ========================================================
            // === РЕЖИМ ВАНИЛЬНОГО РАЗМЕРА (ЗАВИСИТ ОТ GUI SCALE) ===
            // ========================================================
            else {
                int scaledWidth = event.getWindow().getGuiScaledWidth();
                int scaledHeight = event.getWindow().getGuiScaledHeight();

                int scaledBarWidth = Math.min(Math.max(scaledWidth / 16, 50), 200);
                int scaledBarHeight = 3;
                int scaledOutline = 2;
                int scaledOffsetX = 10;
                int scaledOffsetY = 75;
                int scaledVerticalSpacing = 3;

                int totalBarHeightForSpacing = scaledBarHeight + 2 * scaledOutline;
                int scaledX = scaledWidth - scaledBarWidth - scaledOffsetX - scaledOutline;
                int scaledY_start = scaledOffsetY;
                int currentScaledY = scaledY_start;
                int lastScaledBarBottomY = 0;

                matrixStack.pushPose(); // <--- ИЗМЕНЕНО

                String text; float scaledTextWidth; float textScaledX, textScaledY;

                if (absorption > 0) {
                    drawBarInt(matrixStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, absorptionDisplayPercent, HUDColors.getInstance().getHealthBarBackgroundColor(), HUDColors.absorptionBarColor);
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", absorptionDisplayPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(matrixStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }

                drawCombineBarHealthInt(matrixStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, healthPercent, absorptionActualPercent);
                if (HUDColors.getInstance().isPercentBars()) {
                    text = String.format("%.0f%%", healthPercent * 100.0f);
                    scaledTextWidth = mc.font.width(text);
                    textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                    mc.font.drawShadow(matrixStack, text, textScaledX, textScaledY, TEXT_COLOR);
                }
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                if (player.getArmorValue() > 0) { // Используем player
                    drawBarInt(matrixStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, armorPercent, HUDColors.getInstance().getArmorBarBackgroundColor(), HUDColors.getInstance().getArmorBarColor());
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", armorPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(matrixStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }

                drawBarInt(matrixStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, foodPercent, HUDColors.getInstance().getFoodBarBackgroundColor(), HUDColors.getInstance().getFoodBarColor());
                if (HUDColors.getInstance().isPercentBars()) {
                    text = String.format("%.0f%%", foodPercent * 100.0f);
                    scaledTextWidth = mc.font.width(text);
                    textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                    mc.font.drawShadow(matrixStack, text, textScaledX, textScaledY, TEXT_COLOR);
                }
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                drawXPAndAirBarInt(matrixStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, mc, xpPercent);
                boolean displayingAir = this.isDisplayingAirBar;
                if (!displayingAir) {
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", xpPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(matrixStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                }
                lastScaledBarBottomY = currentScaledY + totalBarHeightForSpacing;

                matrixStack.popPose(); // <--- ИЗМЕНЕНО

                if (shouldShowLevelText) {
                    String levelText=String.valueOf(currentLevel);
                    float LtextWidth=mc.font.width(levelText);
                    float LtextScaledOffsetX=5;
                    float LtextScaledOffsetY=2;
                    float barRightEdgeDrawX = scaledX + scaledBarWidth + scaledOutline;
                    float LtextScaledX= barRightEdgeDrawX - LtextWidth - LtextScaledOffsetX;
                    float LtextScaledY=lastScaledBarBottomY + LtextScaledOffsetY;
                    mc.font.drawShadow(matrixStack, levelText, LtextScaledX, LtextScaledY, 0xFFFFFF);
                }
            }
        }
    }

    // --- Методы отрисовки адаптированы для MatrixStack ---

    private void drawBarPixel(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int filledContentWidth = (int) (percentage * contentWidth);
        if (percentage > 0.99f && filledContentWidth < contentWidth) filledContentWidth = contentWidth;

        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;
        int fillStartX = contentX + (contentWidth - filledContentWidth) / 2;

        // Используем адаптированный fillPixelPerfect
        fillPixelPerfect(matrixStack, x, y, barWidth, barHeight, 0xFF000000);
        fillPixelPerfect(matrixStack, contentX, contentY, contentWidth, contentHeight, darkColor);
        if (filledContentWidth > 0) {
            fillPixelPerfect(matrixStack, fillStartX, contentY, filledContentWidth, contentHeight, lightColor);
        }
    }

    private void drawCombineBarHealthPixel(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int healthFilledWidth = (int) (healthPercentage * contentWidth);
        if (healthPercentage > 0.99f && healthFilledWidth < contentWidth) healthFilledWidth = contentWidth;

        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;
        int healthStartX = contentX + (contentWidth - healthFilledWidth) / 2;

        int barColor = calculateHealthColor(healthPercentage); // Проверить, что calculateHealthColor адаптирован
        int bgColor = HUDColors.getInstance().getHealthBarBackgroundColor();

        fillPixelPerfect(matrixStack, x, y, barWidth, barHeight, 0xFF000000);
        fillPixelPerfect(matrixStack, contentX, contentY, contentWidth, contentHeight, bgColor);
        if (healthFilledWidth > 0) {
            fillPixelPerfect(matrixStack, healthStartX, contentY, healthFilledWidth, contentHeight, barColor);
        }
    }

    private void drawXPAndAirBarPixel(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, Minecraft mc, float xpPercentage) {
        // Логика определения воздуха/опыта остается той же
        PlayerEntity player = mc.player; // PlayerEntity
        int currentAir = player.getAirSupply(); // Проверить метод getAirSupply/getAir
        int maxAir = player.getMaxAirSupply();
        boolean isUnderwater = player.isUnderWater(); // Проверить метод isUnderWater/isInWater

        // ... (остальная логика определения isDisplayingAirBar такая же) ...
        if (isUnderwater) {
            wasUnderWater = true; isDisplayingAirBar = true; maxAirSupply = maxAir; lastAirSupply = currentAir;
            airPercentage = maxAir > 0 ? (float) currentAir / maxAir : 0;
        } else if (wasUnderWater) {
            isDisplayingAirBar = true;
            if (lastAirSupply < maxAirSupply) {
                lastAirSupply++; airPercentage = maxAirSupply > 0 ? (float) lastAirSupply / maxAirSupply : 0;
            }
            if (lastAirSupply >= maxAirSupply) {
                isDisplayingAirBar = false; wasUnderWater = false; airPercentage = 1.0f;
            }
        } else {
            isDisplayingAirBar = false; wasUnderWater = false;
        }
        // --- Конец логики определения ---

        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;
        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;

        if (isDisplayingAirBar) {
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = (int) (percentage * contentWidth);
            if (percentage > 0.99f && filledWidth < contentWidth) filledWidth = contentWidth;
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = HUDColors.getInstance().getAirBarBackgroundColor();
            int fgColor = HUDColors.getInstance().getAirBarColor();

            fillPixelPerfect(matrixStack, x, y, barWidth, barHeight, 0xFF000000);
            fillPixelPerfect(matrixStack, contentX, contentY, contentWidth, contentHeight, bgColor);
            if (filledWidth > 0) fillPixelPerfect(matrixStack, startX, contentY, filledWidth, contentHeight, fgColor);
        } else {
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = (int) (percentage * contentWidth);
            if (percentage > 0.99f && filledWidth < contentWidth) filledWidth = contentWidth;
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = HUDColors.getInstance().getXpBarBackgroundColor();
            int fgColor = HUDColors.getInstance().getXpBarColor();

            fillPixelPerfect(matrixStack, x, y, barWidth, barHeight, 0xFF000000);
            fillPixelPerfect(matrixStack, contentX, contentY, contentWidth, contentHeight, bgColor);
            if (filledWidth > 0) fillPixelPerfect(matrixStack, startX, contentY, filledWidth, contentHeight, fgColor);
        }
    }

    // === МЕТОДЫ ДЛЯ ВАНИЛЬНОГО РАЗМЕРА ===

    // Используем AbstractGui.fill и MatrixStack
    private void drawBarInt(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        int barWidthFilled = (int) (percentage * barWidth);
        if (percentage > 0.99f && barWidthFilled < barWidth) barWidthFilled = barWidth;
        int startX = x + (barWidth - barWidthFilled) / 2;

        // AbstractGui.fill требует MatrixStack первым аргументом
        AbstractGui.fill(matrixStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
        AbstractGui.fill(matrixStack, x, y, x + barWidth, y + barHeight, darkColor);
        if (barWidthFilled > 0) {
            AbstractGui.fill(matrixStack, startX, y, startX + barWidthFilled, y + barHeight, lightColor);
        }
    }

    private void drawCombineBarHealthInt(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int healthWidth = (int) (healthPercentage * barWidth);
        if (healthPercentage > 0.99f && healthWidth < barWidth) healthWidth = barWidth;
        int healthStartX = x + (barWidth - healthWidth) / 2;

        AbstractGui.fill(matrixStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
        AbstractGui.fill(matrixStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getHealthBarBackgroundColor());
        int barColor = calculateHealthColor(healthPercentage);
        if (healthWidth > 0) {
            AbstractGui.fill(matrixStack, healthStartX, y, healthStartX + healthWidth, y + barHeight, barColor);
        }
    }

    private void drawXPAndAirBarInt(MatrixStack matrixStack, int x, int y, int barWidth, int barHeight, int outlineWidth, Minecraft mc, float xpPercentage) {
        // Логика определения воздуха/опыта та же, что и в drawXPAndAirBarPixel
        PlayerEntity player = mc.player;
        int currentAir = player.getAirSupply();
        int maxAir = player.getMaxAirSupply();
        boolean isUnderwater = player.isUnderWater();

        // ... (логика isDisplayingAirBar) ...
        if (isUnderwater) {
            wasUnderWater = true; isDisplayingAirBar = true; maxAirSupply = maxAir; lastAirSupply = currentAir;
            airPercentage = maxAir > 0 ? (float) currentAir / maxAir : 0;
        } else if (wasUnderWater) {
            isDisplayingAirBar = true;
            if (lastAirSupply < maxAirSupply) {
                lastAirSupply++; airPercentage = maxAirSupply > 0 ? (float) lastAirSupply / maxAirSupply : 0;
            }
            if (lastAirSupply >= maxAirSupply) {
                isDisplayingAirBar = false; wasUnderWater = false; airPercentage = 1.0f;
            }
        } else {
            isDisplayingAirBar = false; wasUnderWater = false;
        }
        // --- Конец логики ---

        if (isDisplayingAirBar) {
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = (int) (percentage * barWidth);
            if (percentage > 0.99f && filledWidth < barWidth) filledWidth = barWidth;
            int startX = x + (barWidth - filledWidth) / 2;

            AbstractGui.fill(matrixStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            AbstractGui.fill(matrixStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getAirBarBackgroundColor());
            if (filledWidth > 0) {
                AbstractGui.fill(matrixStack, startX, y, startX + filledWidth, y + barHeight, HUDColors.getInstance().getAirBarColor());
            }
        } else {
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = (int) (percentage * barWidth);
            if (percentage > 0.99f && filledWidth < barWidth) filledWidth = barWidth;
            int startX = x + (barWidth - filledWidth) / 2;

            AbstractGui.fill(matrixStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            AbstractGui.fill(matrixStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getXpBarBackgroundColor());
            if (filledWidth > 0) {
                AbstractGui.fill(matrixStack, startX, y, startX + filledWidth, y + barHeight, HUDColors.getInstance().getXpBarColor());
            }
        }
    }

    // --- Адаптация calculateHealthColor ---
    private int calculateHealthColor(float healthPercentage) {
        // Убедись, что классы esmndHUDConfigScreen и HUDColors адаптированы для 1.16.5
        esmndHUDConfigScreen.GradientModeUse modeGradient = HUDColors.getInstance().getGradientModeUse();
        esmndHUDConfigScreen.PulseLowHealthMode pulseModeHealth = HUDColors.getInstance().getPulseMode();
        int barColor = HUDColors.getInstance().getHealthBarColor();

        if (modeGradient != esmndHUDConfigScreen.GradientModeUse.OFF) {
            Minecraft mc = Minecraft.getInstance();
            boolean applyGradient = false;
            if (mc.level != null) { // level вместо world
                // --- ИЗМЕНЕНО: Switch expression заменен на switch statement ---
                switch (modeGradient) {
                    case HARDCORE_ONLY:
                        // Проверить методы getLevelData().isHardcore()
                        applyGradient = mc.level.getLevelData().isHardcore();
                        break;
                    case SURVIVAL_ONLY:
                        applyGradient = isSurvivalNotHardcore();
                        break;
                    case BOTH:
                        applyGradient = true;
                        break;
                    // default не нужен, так как applyGradient уже false
                }
                // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            }
            if (applyGradient) {
                barColor = interpolateColor(HUDColors.getInstance().getHardcoreGradientOne(), HUDColors.getInstance().getHardcoreGradientTwo(), healthPercentage);
            }
        }

        if (pulseModeHealth != esmndHUDConfigScreen.PulseLowHealthMode.OFF && healthPercentage < 0.4f) {
            Minecraft mc = Minecraft.getInstance();
            boolean applyPulse = false;
            if (mc.level != null) {
                // --- ИЗМЕНЕНО: Switch expression заменен на switch statement ---
                switch (pulseModeHealth) {
                    case HARDCORE_ONLY:
                        applyPulse = mc.level.getLevelData().isHardcore();
                        break;
                    case SURVIVAL_ONLY:
                        applyPulse = isSurvivalNotHardcore();
                        break;
                    case BOTH:
                        applyPulse = true;
                        break;
                    // default не нужен
                }
                // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            }
            if (applyPulse) {
                float pulse = (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 200.0));
                int alpha = Math.max(0, Math.min(255, (int) (pulse * 255)));
                barColor = (barColor & 0x00FFFFFF) | (alpha << 24);
            }
        }
        return barColor;
    }

    private boolean isSurvivalNotHardcore() {
        assert Minecraft.getInstance().level != null;
        return (!Minecraft.getInstance().level.getLevelData().isHardcore());
    }
}