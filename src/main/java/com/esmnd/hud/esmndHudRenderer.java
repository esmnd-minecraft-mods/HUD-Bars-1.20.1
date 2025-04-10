package com.esmnd.hud;

import com.esmnd.hud.HUDColors; // Импорт нашего адаптированного конфига
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP; // Клиентский игрок
import net.minecraft.client.gui.Gui; // Содержит drawRect
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer; // Базовый класс для инвентарей
import net.minecraft.client.gui.inventory.GuiInventory; // Инвентарь игрока
import net.minecraft.client.gui.inventory.GuiScreenHorseInventory; // Пример другого инвентаря (если нужно исключать)
import net.minecraft.client.renderer.BufferBuilder; // VertexBuffer в некоторых маппингах
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
// Импорты для ванильных экранов (проверить точные имена в 1.12.2)
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiRepair; // Anvil

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge; // Для регистрации
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11; // Для режимов рисования (GL_QUADS)

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class esmndHudRenderer {

    private float smoothHealth = 10f;
    private float smoothFood = 20f;
    private float smoothArmor = 10f;
    private float smoothXp = 20f;

    private int previousLevel = -1;
    private long levelUpTimestamp = 0;
    private static final long LEVEL_UP_DISPLAY_DURATION_MS = 3000;

    private boolean isDisplayingAirBar = false;
    private int lastAirSupply = 0;
    private int maxAirSupply = 300; // В 1.12.2 обычно 300, нет метода getMaxAirSupply
    private float airPercentage = 0f;
    private boolean wasUnderWater = false;

    private Minecraft mc;
    private HUDColors config; // Ссылка на конфиг

    // --- ИЗМЕНЕНО: Замена Record на внутренний класс ---
    private static class PercentTextInfo {
        final String text;
        final int pixelY;
        PercentTextInfo(String text, int pixelY) {
            this.text = text;
            this.pixelY = pixelY;
        }
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    public esmndHudRenderer()
    {
        this.mc = Minecraft.getMinecraft();
        this.config = HUDColors.getInstance();
    }

    public void register()
    {
        MinecraftForge.EVENT_BUS.register(this);
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

    // --- ИЗМЕНЕНО: Рендеринг прямоугольника через Tessellator/GlStateManager ---
    /**
     * Рисует цветной прямоугольник без текстуры.
     * x, y - верхний левый угол.
     */
    private void drawColoredRect(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        int x2 = x + width;
        int y2 = y + height;

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer(); // getBuffer() в 1.12.2

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D(); // Отключаем текстуры для цветных фигур
        // Устанавливаем стандартный режим смешивания
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // Устанавливаем цвет
        GlStateManager.color(r, g, b, a);

        // Начинаем рисовать прямоугольник (QUADS)
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION); // Достаточно только POSITION, цвет задан через GlStateManager
        bufferbuilder.pos((double)x,  (double)y2, 0.0D).endVertex(); // Нижний левый
        bufferbuilder.pos((double)x2, (double)y2, 0.0D).endVertex(); // Нижний правый
        bufferbuilder.pos((double)x2, (double)y,  0.0D).endVertex(); // Верхний правый
        bufferbuilder.pos((double)x,  (double)y,  0.0D).endVertex(); // Верхний левый
        tessellator.draw(); // Отрисовываем

        // Возвращаем состояние
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); // Сбрасываем цвет на белый
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    // --- ИЗМЕНЕНО: Проверка на креатив ---
    public boolean checkIfPlayerInCreative() {
        EntityPlayerSP player = mc.player;
        // В 1.12.2 используется isCreativeMode() или проверка gameType
        return player != null && (player.isCreative() || player.isSpectator());
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    // --- ИЗМЕНЕНО: Проверка открытых экранов ---
    public boolean checkIfOpenedInventoryOrMagicWindow() {
        // Проверяем текущий экран, сравнивая с классами экранов 1.12.2
        return mc.currentScreen instanceof GuiInventory // Инвентарь игрока
                || mc.currentScreen instanceof GuiEnchantment // Стол зачарования
                || mc.currentScreen instanceof GuiRepair; // Наковальня
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    // --- ИЗМЕНЕНО: Обработчик RenderGameOverlayEvent.Pre ---
    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        // Отменяем стандартные элементы
        RenderGameOverlayEvent.ElementType elementType = event.getType();
        if (elementType == RenderGameOverlayEvent.ElementType.HEALTH ||
                elementType == RenderGameOverlayEvent.ElementType.FOOD ||
                elementType == RenderGameOverlayEvent.ElementType.ARMOR ||
                elementType == RenderGameOverlayEvent.ElementType.EXPERIENCE ||
                elementType == RenderGameOverlayEvent.ElementType.AIR) {
            event.setCanceled(true);
        }
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    // --- ИЗМЕНЕНО: Обработчик RenderGameOverlayEvent.Post ---
    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        // Рисуем наш HUD после отрисовки хотбара (или другого элемента)
        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && !checkIfPlayerInCreative()) {
            EntityPlayerSP player = mc.player;

            if (player == null || mc.world == null) { // mc.world в 1.12.2
                previousLevel = -1;
                levelUpTimestamp = 0;
                return;
            }

            // --- Получение данных игрока (проверить методы 1.12.2) ---
            smoothHealth += (player.getHealth() - smoothHealth) * 0.1f;
            smoothFood += (player.getFoodStats().getFoodLevel() - smoothFood) * 0.1f; // getFoodStats()
            smoothArmor += (player.getTotalArmorValue() - smoothArmor) * 0.1f; // getTotalArmorValue()
            float experienceProgress = player.experience; // Поле experience в 1.12.2 это прогресс 0-1
            smoothXp += (experienceProgress - smoothXp) * 0.1f;
            float absorption = player.getAbsorptionAmount();
            int currentLevel = player.experienceLevel;
            // --- КОНЕЦ получения данных ---

            float maxHealth = player.getMaxHealth();
            float healthPercent = maxHealth > 0 ? Math.max(0, Math.min(smoothHealth / maxHealth, 1.0f)) : 0;
            float armorPercent = Math.max(0, Math.min(smoothArmor / 20.0f, 1.0f)); // Макс броня 20
            float foodPercent = Math.max(0, Math.min(smoothFood / 20.0f, 1.0f)); // Макс еда 20
            float xpPercent = Math.max(0, Math.min(smoothXp, 1.0f));
            float absorptionDisplayPercent = Math.max(0, Math.min(absorption / 20.0f, 1.0f));
            float absorptionActualPercent = maxHealth > 0 ? Math.max(0, Math.min(absorption / maxHealth, 1.0f)) : 0;

            // Логика показа текста уровня остается той же
            boolean inventoryOpen = checkIfOpenedInventoryOrMagicWindow();
            boolean recentlyLeveledUp = System.currentTimeMillis() < levelUpTimestamp + LEVEL_UP_DISPLAY_DURATION_MS;
            boolean shouldShowLevelText = inventoryOpen || recentlyLeveledUp;

            if (previousLevel == -1) { previousLevel = currentLevel; }
            else if (currentLevel > previousLevel) { levelUpTimestamp = System.currentTimeMillis(); previousLevel = currentLevel; }
            else if (currentLevel < previousLevel) { previousLevel = currentLevel; }

            final int TEXT_COLOR = 0xFFFFFFFF;
            final int TEXT_OFFSET_X_PIXELS = 4;
            final float TEXT_OFFSET_X_SCALED = 2.0f; // В 1.12.2 масштаб применяется явно
            final int FONT_HEIGHT = mc.fontRenderer.FONT_HEIGHT; // fontRenderer

            // --- Получаем разрешение и масштаб ---
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int screenWidth = scaledResolution.getScaledWidth(); // Масштабированные размеры
            int screenHeight = scaledResolution.getScaledHeight();
            // Для пиксельного режима нужны реальные размеры окна
            int actualScreenWidth = mc.displayWidth;
            int actualScreenHeight = mc.displayHeight;
            // и масштаб
            int guiScaleFactor = scaledResolution.getScaleFactor();
            // --- КОНЕЦ получения разрешения ---


            // =============================================================
            // === РЕЖИМ ФИКСИРОВАННОГО РАЗМЕРА (НЕ ЗАВИСИТ ОТ GUI SCALE) ===
            // =============================================================
            if (config.isFixedSizeHUD()) { // Используем config
                float fixedHudScale = (float)config.getHudScaleFixed();

                // Базовые значения в ПИКСЕЛЯХ (остаются те же)
                int baseFixedBarWidth = 100; int baseFixedBarHeight = 5 + 3 * 2; int baseFixedOutline = 3;
                int baseFixedOffsetX = 15; int basePixelOffsetY = actualScreenHeight / 8; // Используем actualScreenHeight
                int baseFixedVerticalSpacing = 5; int baseTextPixelOffsetX = 5; int baseTextPixelOffsetY = 3;

                // Актуальные значения в ПИКСЕЛЯХ (остаются те же)
                int actualFixedOutline = Math.max(1, Math.round(baseFixedOutline * fixedHudScale));
                int actualTotalBarWidth = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarWidth * fixedHudScale));
                int actualTotalBarHeight = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarHeight * fixedHudScale));
                int actualFixedOffsetX = Math.round(baseFixedOffsetX * fixedHudScale);
                int actualFixedOffsetY = Math.round(basePixelOffsetY * fixedHudScale);
                int actualFixedVerticalSpacing = Math.max(1, Math.round(baseFixedVerticalSpacing * fixedHudScale));
                int actualTextPixelOffsetX = Math.round(baseTextPixelOffsetX * fixedHudScale);
                int actualTextPixelOffsetY = Math.round(baseTextPixelOffsetY * fixedHudScale);

                // Позиция в ПИКСЕЛЯХ
                int pixelX = actualScreenWidth - actualTotalBarWidth - actualFixedOffsetX;
                int pixelY_start = actualFixedOffsetY;
                int currentPixelY = pixelY_start;
                int lastBarBottomPixelY = 0;

                // Используем наш внутренний класс PercentTextInfo
                List<PercentTextInfo> percentTexts = new ArrayList<>();

                // --- ИЗМЕНЕНО: Рендеринг с GlStateManager ---
                GlStateManager.pushMatrix(); // Сохраняем текущую матрицу
                // Масштабируем под пиксели (делим на GUI масштаб)
                GlStateManager.scale(1.0f / guiScaleFactor, 1.0f / guiScaleFactor, 1.0f);

                // --- Отрисовка ПОЛОС (пиксельные методы) ---
                int contentHeight = actualTotalBarHeight - 2 * actualFixedOutline;
                int contentCenterYOffset = contentHeight / 2;

                if (absorption > 0) {
                    drawBarPixel(pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, absorptionDisplayPercent, config.getHealthBarBackgroundColor(), HUDColors.absorptionBarColor); // Используем статический цвет поглощения
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", absorptionDisplayPercent * 100.0f), currentPixelY));
                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;
                }
                drawCombineBarHealthPixel(pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, healthPercent, absorptionActualPercent);
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", healthPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;

                if (player.getTotalArmorValue() > 0) {
                    drawBarPixel(pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, armorPercent, config.getArmorBarBackgroundColor(), config.getArmorBarColor());
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", armorPercent * 100.0f), currentPixelY));
                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;
                }
                drawBarPixel(pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, foodPercent, config.getFoodBarBackgroundColor(), config.getFoodBarColor());
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", foodPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing;

                int tempYBeforeXP = currentPixelY;
                drawXPAndAirBarPixel(pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, xpPercent); // Передаем только xpPercent
                boolean displayingAir = this.isDisplayingAirBar; // Флаг устанавливается внутри drawXPAndAirBarPixel
                if (!displayingAir) {
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", xpPercent * 100.0f), tempYBeforeXP));
                }
                lastBarBottomPixelY = currentPixelY + actualTotalBarHeight;

                GlStateManager.popMatrix(); // Восстанавливаем матрицу (до масштабирования)
                // --- КОНЕЦ ИЗМЕНЕНИЯ Рендеринга ---

                // --- Отрисовка ТЕКСТА ПРОЦЕНТОВ (уже в масштабированных координатах) ---
                if (config.isPercentBars()) {
                    contentHeight = actualTotalBarHeight - 2 * actualFixedOutline;
                    contentCenterYOffset = contentHeight / 2;
                    for (PercentTextInfo info : percentTexts) {
                        String text = info.text;
                        int textPixelY = info.pixelY;
                        int textWidth = mc.fontRenderer.getStringWidth(text); // getStringWidth

                        // Конвертируем пиксельные координаты в scaled units
                        float barLeftEdgeDrawX = (float) pixelX / guiScaleFactor;
                        float barContentCenterDrawY = (float) (textPixelY + actualFixedOutline + contentCenterYOffset) / guiScaleFactor;
                        // Отступ текста (пиксельный -> scaled)
                        float textScaledOffsetX = (float) TEXT_OFFSET_X_PIXELS / guiScaleFactor;
                        // Финальные координаты в scaled units
                        float textDrawX = barLeftEdgeDrawX - (float)textWidth - textScaledOffsetX;
                        float textDrawY = barContentCenterDrawY - (float) FONT_HEIGHT / 2.0f;

                        // Рисуем тень (белый цвет)
                        mc.fontRenderer.drawStringWithShadow(text, textDrawX, textDrawY, TEXT_COLOR);
                    }
                }

                // --- Отрисовка текста Уровня ---
                if (shouldShowLevelText) {
                    String levelText = String.valueOf(currentLevel);
                    int LtextWidth = mc.fontRenderer.getStringWidth(levelText);

                    // Конвертируем пиксельные координаты в scaled units
                    float barRightEdgeDrawX = (float)(pixelX + actualTotalBarWidth) / guiScaleFactor;
                    float lastBarBottomDrawY = (float)lastBarBottomPixelY / guiScaleFactor;
                    float textScaledOffsetX = (float)actualTextPixelOffsetX / guiScaleFactor;
                    float textScaledOffsetY = (float)actualTextPixelOffsetY / guiScaleFactor;

                    float textDrawX = barRightEdgeDrawX - (float)LtextWidth - textScaledOffsetX;
                    float textDrawY = lastBarBottomDrawY + textScaledOffsetY;

                    mc.fontRenderer.drawStringWithShadow(levelText, textDrawX, textDrawY, 0xFFFFFF);
                }
            }
            // ========================================================
            // === РЕЖИМ ВАНИЛЬНОГО РАЗМЕРА (ЗАВИСИТ ОТ GUI SCALE) ===
            // ========================================================
            else {
                // Используем screenWidth и screenHeight (уже масштабированные)
                int scaledBarWidth = Math.min(Math.max(screenWidth / 16, 50), 100);
                int scaledBarHeight = 3;
                int scaledOutline = 2;
                int scaledOffsetX = 10;
                int scaledOffsetY = 75; // Оригинальный Y
                int scaledVerticalSpacing = 3;

                int totalBarHeightForSpacing = scaledBarHeight + 2 * scaledOutline;
                int scaledX = screenWidth - scaledBarWidth - scaledOffsetX - scaledOutline; // X левого верхнего угла КОНТЕНТА
                int currentScaledY = scaledOffsetY; // Y левого верхнего угла КОНТЕНТА
                int lastScaledBarBottomY = 0;

                // Матрицу менять не нужно, рисуем в стандартных координатах GUI
                // GlStateManager.pushMatrix(); // Не обязательно, если не меняем матрицу

                String text; int textWidth; float textScaledX, textScaledY;

                // --- Отрисовка ПОЛОС (методы *Int используют Gui.drawRect) и ТЕКСТА ПРОЦЕНТОВ ---
                if (absorption > 0) {
                    drawBarInt(scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, absorptionDisplayPercent, config.getHealthBarBackgroundColor(), HUDColors.absorptionBarColor);
                    if (config.isPercentBars()) {
                        text = String.format("%.0f%%", absorptionDisplayPercent * 100.0f);
                        textWidth = mc.fontRenderer.getStringWidth(text);
                        textScaledX = (scaledX - scaledOutline) - textWidth - TEXT_OFFSET_X_SCALED; // От левой обводки
                        textScaledY = currentScaledY + (scaledBarHeight / 2.0f) - (FONT_HEIGHT / 2.0f);
                        mc.fontRenderer.drawStringWithShadow(text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }
                drawCombineBarHealthInt(scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, healthPercent, absorptionActualPercent);
                if (config.isPercentBars()) {
                    text = String.format("%.0f%%", healthPercent * 100.0f);
                    textWidth = mc.fontRenderer.getStringWidth(text);
                    textScaledX = (scaledX - scaledOutline) - textWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + (scaledBarHeight / 2.0f) - (FONT_HEIGHT / 2.0f);
                    mc.fontRenderer.drawStringWithShadow(text, textScaledX, textScaledY, TEXT_COLOR);
                }
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                if (player.getTotalArmorValue() > 0) {
                    drawBarInt(scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, armorPercent, config.getArmorBarBackgroundColor(), config.getArmorBarColor());
                    if (config.isPercentBars()) {
                        text = String.format("%.0f%%", armorPercent * 100.0f);
                        textWidth = mc.fontRenderer.getStringWidth(text);
                        textScaledX = (scaledX - scaledOutline) - textWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + (scaledBarHeight / 2.0f) - (FONT_HEIGHT / 2.0f);
                        mc.fontRenderer.drawStringWithShadow(text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }
                drawBarInt(scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, foodPercent, config.getFoodBarBackgroundColor(), config.getFoodBarColor());
                if (config.isPercentBars()) {
                    text = String.format("%.0f%%", foodPercent * 100.0f);
                    textWidth = mc.fontRenderer.getStringWidth(text);
                    textScaledX = (scaledX - scaledOutline) - textWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + (scaledBarHeight / 2.0f) - (FONT_HEIGHT / 2.0f);
                    mc.fontRenderer.drawStringWithShadow(text, textScaledX, textScaledY, TEXT_COLOR);
                }
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                drawXPAndAirBarInt(scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, xpPercent);
                boolean displayingAir = this.isDisplayingAirBar;
                if (!displayingAir) {
                    if (config.isPercentBars()) {
                        text = String.format("%.0f%%", xpPercent * 100.0f);
                        textWidth = mc.fontRenderer.getStringWidth(text);
                        textScaledX = (scaledX - scaledOutline) - textWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + (scaledBarHeight / 2.0f) - (FONT_HEIGHT / 2.0f);
                        mc.fontRenderer.drawStringWithShadow(text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                }
                lastScaledBarBottomY = currentScaledY + totalBarHeightForSpacing;

                // GlStateManager.popMatrix(); // Если не делали push

                // --- Отрисовка текста Уровня ---
                if (shouldShowLevelText) {
                    String levelText = String.valueOf(currentLevel);
                    int LtextWidth = mc.fontRenderer.getStringWidth(levelText);
                    float LtextScaledOffsetX = 5;
                    float LtextScaledOffsetY = 2;
                    float barRightEdgeDrawX = scaledX + scaledBarWidth + scaledOutline; // Правая обводка
                    float LtextScaledX = barRightEdgeDrawX - LtextWidth - LtextScaledOffsetX;
                    float LtextScaledY = lastScaledBarBottomY + LtextScaledOffsetY; // Ниже последней полосы
                    mc.fontRenderer.drawStringWithShadow(levelText, LtextScaledX, LtextScaledY, 0xFFFFFF);
                }
            }
        }
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ Post ---

    // --- МЕТОДЫ ОТРИСОВКИ ПОЛОС ---

    // --- ИЗМЕНЕНО: drawBarPixel (использует drawColoredRect) ---
    private void drawBarPixel(int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int filledContentWidth = Math.round(percentage * contentWidth); // Используем Math.round для точности
        // if (percentage > 0.99f && filledContentWidth < contentWidth) filledContentWidth = contentWidth; // Можно убрать, округление справится

        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;
        int fillStartX = contentX + (contentWidth - filledContentWidth) / 2; // Центрирование

        // Рисуем через новый метод
        drawColoredRect(x, y, barWidth, barHeight, 0xFF000000); // Обводка/подложка
        drawColoredRect(contentX, contentY, contentWidth, contentHeight, darkColor); // Фон
        if (filledContentWidth > 0) {
            drawColoredRect(fillStartX, contentY, filledContentWidth, contentHeight, lightColor); // Заполнение
        }
    }

    // --- ИЗМЕНЕНО: drawCombineBarHealthPixel ---
    private void drawCombineBarHealthPixel(int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int healthFilledWidth = Math.round(healthPercentage * contentWidth);
        // if (healthPercentage > 0.99f && healthFilledWidth < contentWidth) healthFilledWidth = contentWidth;

        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;
        int healthStartX = contentX + (contentWidth - healthFilledWidth) / 2;

        int barColor = calculateHealthColor(healthPercentage); // Используем адапт. метод
        int bgColor = config.getHealthBarBackgroundColor();

        drawColoredRect(x, y, barWidth, barHeight, 0xFF000000);
        drawColoredRect(contentX, contentY, contentWidth, contentHeight, bgColor);
        if (healthFilledWidth > 0) {
            drawColoredRect(healthStartX, contentY, healthFilledWidth, contentHeight, barColor);
        }
    }

    // --- ИЗМЕНЕНО: drawXPAndAirBarPixel ---
    private void drawXPAndAirBarPixel(int x, int y, int barWidth, int barHeight, int outlineWidth, float xpPercentage) {
        // --- Логика определения воздуха/опыта ---
        EntityPlayerSP player = mc.player;
        int currentAir = player.getAir(); // getAir() в 1.12.2
        // maxAir = 300 (поле класса)
        boolean isUnderwater = player.isInWater(); // isInWater()

        // Логика определения isDisplayingAirBar остается той же
        if (isUnderwater) {
            wasUnderWater = true; isDisplayingAirBar = true; /* maxAirSupply уже 300 */ lastAirSupply = currentAir;
            airPercentage = maxAirSupply > 0 ? (float) currentAir / maxAirSupply : 0;
        } else if (wasUnderWater) {
            isDisplayingAirBar = true;
            if (lastAirSupply < maxAirSupply) {
                lastAirSupply += 4; // Восстанавливаем быстрее
                airPercentage = maxAirSupply > 0 ? Math.min(1.0f, (float) lastAirSupply / maxAirSupply) : 0;
            } else { // Полностью восстановился
                isDisplayingAirBar = false; wasUnderWater = false; airPercentage = 1.0f;
            }
        } else {
            isDisplayingAirBar = false; wasUnderWater = false;
        }
        // --- Конец логики воздуха ---

        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;
        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;

        if (isDisplayingAirBar) {
            // Воздух
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = Math.round(percentage * contentWidth);
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = config.getAirBarBackgroundColor();
            int fgColor = config.getAirBarColor();

            drawColoredRect(x, y, barWidth, barHeight, 0xFF000000);
            drawColoredRect(contentX, contentY, contentWidth, contentHeight, bgColor);
            if (filledWidth > 0) drawColoredRect(startX, contentY, filledWidth, contentHeight, fgColor);
        } else {
            // Опыт
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = Math.round(percentage * contentWidth);
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = config.getXpBarBackgroundColor();
            int fgColor = config.getXpBarColor();

            drawColoredRect(x, y, barWidth, barHeight, 0xFF000000);
            drawColoredRect(contentX, contentY, contentWidth, contentHeight, bgColor);
            if (filledWidth > 0) drawColoredRect(startX, contentY, filledWidth, contentHeight, fgColor);
        }
    }


    // --- МЕТОДЫ ДЛЯ ВАНИЛЬНОГО РАЗМЕРА (используют Gui.drawRect) ---

    // --- ИЗМЕНЕНО: drawBarInt ---
    private void drawBarInt(int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        int barWidthFilled = Math.round(percentage * barWidth);
        int startX = x + (barWidth - barWidthFilled) / 2; // Центрирование

        // Рисуем через статический метод Gui.drawRect(left, top, right, bottom, color)
        Gui.drawRect(x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000); // Обводка
        Gui.drawRect(x, y, x + barWidth, y + barHeight, darkColor); // Фон
        if (barWidthFilled > 0) {
            Gui.drawRect(startX, y, startX + barWidthFilled, y + barHeight, lightColor); // Заполнение
        }
    }

    // --- ИЗМЕНЕНО: drawCombineBarHealthInt ---
    private void drawCombineBarHealthInt(int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int healthWidth = Math.round(healthPercentage * barWidth);
        int healthStartX = x + (barWidth - healthWidth) / 2;

        int barColor = calculateHealthColor(healthPercentage);
        int bgColor = config.getHealthBarBackgroundColor();

        Gui.drawRect(x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
        Gui.drawRect(x, y, x + barWidth, y + barHeight, bgColor);
        if (healthWidth > 0) {
            Gui.drawRect(healthStartX, y, healthStartX + healthWidth, y + barHeight, barColor);
        }
    }

    // --- ИЗМЕНЕНО: drawXPAndAirBarInt ---
    private void drawXPAndAirBarInt(int x, int y, int barWidth, int barHeight, int outlineWidth, float xpPercentage) {
        // Логика определения воздуха/опыта (та же, что в drawXPAndAirBarPixel)
        EntityPlayerSP player = mc.player;
        int currentAir = player.getAir();
        boolean isUnderwater = player.isInWater();

        if (isUnderwater) {
            wasUnderWater = true;
            isDisplayingAirBar = true;
            lastAirSupply = currentAir;
            airPercentage = maxAirSupply > 0 ? (float) currentAir / maxAirSupply : 0;
        }
        else if (wasUnderWater) {
            isDisplayingAirBar = true;
            if (lastAirSupply < maxAirSupply) {
                lastAirSupply += 4;
                airPercentage = maxAirSupply > 0 ? Math.min(1.0f, (float) lastAirSupply / maxAirSupply) : 0;
            } else {
                isDisplayingAirBar = false;
                wasUnderWater = false;
                airPercentage = 1.0f;
            }
        }
        else {
            isDisplayingAirBar = false;
            wasUnderWater = false;
        }
        // --- Конец логики ---


        if (isDisplayingAirBar) {
            // Воздух
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = Math.round(percentage * barWidth);
            int startX = x + (barWidth - filledWidth) / 2;
            int bgColor = config.getAirBarBackgroundColor();
            int fgColor = config.getAirBarColor();

            Gui.drawRect(x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            Gui.drawRect(x, y, x + barWidth, y + barHeight, bgColor);
            if (filledWidth > 0) Gui.drawRect(startX, y, startX + filledWidth, y + barHeight, fgColor);
        } else {
            // Опыт
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = Math.round(percentage * barWidth);
            int startX = x + (barWidth - filledWidth) / 2;
            int bgColor = config.getXpBarBackgroundColor();
            int fgColor = config.getXpBarColor();

            Gui.drawRect(x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            Gui.drawRect(x, y, x + barWidth, y + barHeight, bgColor);
            if (filledWidth > 0) Gui.drawRect(startX, y, startX + filledWidth, y + barHeight, fgColor);
        }
    }


    // --- ИЗМЕНЕНО: calculateHealthColor (Switch выражения заменены) ---
    private int calculateHealthColor(float healthPercentage) {
        HUDColors.GradientModeUse modeGradient = config.getGradientModeUse(); // Используем Enum из HUDColors
        HUDColors.PulseLowHealthMode pulseModeHealth = config.getPulseMode(); // Используем Enum из HUDColors
        int barColor = config.getHealthBarColor();

        // Градиент
        if (modeGradient != HUDColors.GradientModeUse.OFF) {
            boolean applyGradient = false;
            // Проверка на хардкор и выживание для 1.12.2
            if (mc.world != null) { // mc.world
                switch (modeGradient) {
                    case HARDCORE_ONLY:
                        applyGradient = mc.world.getWorldInfo().isHardcoreModeEnabled(); // getWorldInfo().isHardcoreMode()
                        break;
                    case SURVIVAL_ONLY:
                        applyGradient = isSurvivalNotHardcore();
                        break;
                    case BOTH:
                        applyGradient = true;
                        break;
                }
            }
            if (applyGradient) {
                barColor = interpolateColor(config.getHardcoreGradientOne(), config.getHardcoreGradientTwo(), healthPercentage);
            }
        }

        // Пульсация
        if (pulseModeHealth != HUDColors.PulseLowHealthMode.OFF && healthPercentage < 0.4f) {
            boolean applyPulse = false;
            if (mc.world != null) {
                switch (pulseModeHealth) {
                    case HARDCORE_ONLY:
                        applyPulse = mc.world.getWorldInfo().isHardcoreModeEnabled();
                        break;
                    case SURVIVAL_ONLY:
                        applyPulse = isSurvivalNotHardcore();
                        break;
                    case BOTH:
                        applyPulse = true;
                        break;
                }
            }
            if (applyPulse) {
                float pulse = (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 200.0));
                int alpha = Math.max(0, Math.min(255, (int) (pulse * 255)));
                barColor = (barColor & 0x00FFFFFF) | (alpha << 24);
            }
        }
        return barColor;
    }

    // --- ИЗМЕНЕНО: isSurvivalNotHardcore ---
    private boolean isSurvivalNotHardcore() {
        // Проверка для 1.12.2
        return mc.player != null && mc.world != null
                && !mc.player.isCreative() // Не креатив
                && !mc.player.isSpectator() // Не наблюдатель
                && !mc.world.getWorldInfo().isHardcoreModeEnabled(); // Не хардкор
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---

    // hexToColor остается без изменений
    public static int hexToColor(String hex) {
        try {
            if (hex == null) return 0xFFFF0000;
            String cleanHex = hex.replace("#", "");
            if (cleanHex.length() == 6) {
                cleanHex = "FF" + cleanHex; // Добавляем альфа, если нет
            }
            if (cleanHex.length() != 8) return 0xFFFF0000; // Неверный формат
            // Используем Long.parseUnsignedLong для обработки ARGB
            return (int) Long.parseUnsignedLong(cleanHex, 16);
        } catch (NumberFormatException e) {
            System.err.println("[esmndnewhud] Invalid hex color format: " + hex);
            return 0xFFFF0000;
        }
    }
}