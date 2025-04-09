package com.esmnd.modhud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod("esmndnewhud")
public class esmndnewhud
{
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


    public esmndnewhud()
    {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new esmndHUDConfigScreen(screen))
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

    /**
     * Рисует прямоугольник заданного цвета с пиксельной точностью,
     * учитывая текущую матрицу PoseStack.
     * x, y - верхний левый угол.
     * width, height - размеры в пикселях.
     */
    private void fillPixelPerfect(PoseStack guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Matrix4f matrix = guiGraphics.last().pose();
        int x2 = x + width;
        int y2 = y + height;

        // Компоненты цвета (0.0f - 1.0f)
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        // Настройка RenderSystem
        RenderSystem.enableBlend();
        // Обычно используют стандартное смешивание для GUI
        RenderSystem.defaultBlendFunc();
        // Указываем стандартный шейдер для цветных фигур без текстур
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Получаем Tesselator и BufferBuilder
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        // Начинаем рисовать Quad (прямоугольник)
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Добавляем вершины (против часовой стрелки или по часовой, главное консистентно)
        // Координата Z обычно 0 для GUI
        bufferbuilder.vertex(matrix, (float)x,  (float)y2, 0.0F).color(r, g, b, a).endVertex(); // Нижняя левая
        bufferbuilder.vertex(matrix, (float)x2, (float)y2, 0.0F).color(r, g, b, a).endVertex(); // Нижняя правая
        bufferbuilder.vertex(matrix, (float)x2, (float)y,  0.0F).color(r, g, b, a).endVertex(); // Верхняя правая
        bufferbuilder.vertex(matrix, (float)x,  (float)y,  0.0F).color(r, g, b, a).endVertex(); // Верхняя левая

        // Завершаем и отрисовываем буфер
        // В новых версиях это может быть RenderSystem.draw(bufferbuilder.end()) или похожий метод
        tesselator.end(); // или BufferUploader.drawWithShader(bufferbuilder.end()) в старых

        // Отключаем Blend после использования
        RenderSystem.disableBlend();
    }

    public boolean checkIfPlayerInCreative()
    {
        Player player = Minecraft.getInstance().player;

        return player.isCreative();
    }

    public boolean checkIfOpenedInventoryOrMagicWindow()
    {
        return Minecraft.getInstance().screen instanceof InventoryScreen
                || Minecraft.getInstance().screen instanceof EnchantmentScreen
                || Minecraft.getInstance().screen instanceof AnvilScreen;
    }

    @SubscribeEvent
    public void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // Отменяем отрисовку стандартного интерфейса здоровья и голода
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
                event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() || event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() || event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() || event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && !checkIfPlayerInCreative()) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player == null || mc.level == null) {
                previousLevel = -1; // Сброс при выходе
                levelUpTimestamp = 0;
                return;
            }

            smoothHealth += (mc.player.getHealth() - smoothHealth) * 0.1f;
            smoothFood += (mc.player.getFoodData().getFoodLevel() - smoothFood) * 0.1f;
            smoothArmor += (mc.player.getArmorValue() - smoothArmor) * 0.1f;
            float experienceProgress = mc.player.experienceProgress;
            smoothXp += (experienceProgress - smoothXp) * 0.1f;
            float absorption = mc.player.getAbsorptionAmount();
            int currentLevel = mc.player.experienceLevel;

            // 3. Рассчитываем ПРОЦЕНТЫ для отрисовки (0.0 - 1.0)
            float maxHealth = mc.player.getMaxHealth();
            float healthPercent = maxHealth > 0 ? Math.max(0, Math.min(smoothHealth / maxHealth, 1.0f)) : 0;
            float armorPercent = Math.max(0, Math.min(smoothArmor / 20.0f, 1.0f));
            float foodPercent = Math.max(0, Math.min(smoothFood / 20.0f, 1.0f));
            float xpPercent = Math.max(0, Math.min(smoothXp, 1.0f));
            float absorptionDisplayPercent = Math.max(0, Math.min(absorption / 20.0f, 1.0f));
            float absorptionActualPercent = maxHealth > 0 ? Math.max(0, Math.min(absorption / maxHealth, 1.0f)) : 0;

            PoseStack poseStack = event.getPoseStack();

            // *** НОВАЯ ЛОГИКА: Определяем, нужно ли показывать текст ***
            boolean inventoryOpen = checkIfOpenedInventoryOrMagicWindow();
            boolean recentlyLeveledUp = System.currentTimeMillis() < levelUpTimestamp + LEVEL_UP_DISPLAY_DURATION_MS;
            boolean shouldShowLevelText = inventoryOpen || recentlyLeveledUp;
            // *** КОНЕЦ НОВОЙ ЛОГИКИ ***

            // *** НОВАЯ ЛОГИКА: Обнаружение повышения уровня ***
            if (previousLevel == -1) { // Первая инициализация
                previousLevel = currentLevel;
            } else if (currentLevel > previousLevel) { // Уровень повышен!
                levelUpTimestamp = System.currentTimeMillis(); // Засекаем время
                previousLevel = currentLevel; // Обновляем предыдущий уровень
            } else if (currentLevel < previousLevel) { // Уровень понижен (на всякий случай)
                previousLevel = currentLevel; // Просто обновляем, таймер не трогаем
            }
            // *** КОНЕЦ НОВОЙ ЛОГИКИ ***

            // *** Константы для текста процентов ***
            final int TEXT_COLOR = 0xFFFFFFFF;
            final int TEXT_OFFSET_X_PIXELS = 4; // Отступ слева в пикселях
            final float TEXT_OFFSET_X_SCALED = 2.0f; // Отступ слева в scaled units
            final int FONT_HEIGHT = mc.font.lineHeight;

            // =============================================================
            // === РЕЖИМ ФИКСИРОВАННОГО РАЗМЕРА (НЕ ЗАВИСИТ ОТ GUI SCALE) ===
            // =============================================================
            if (HUDColors.getInstance().isFixedSizeHUD()) {
                // 4.1. Получаем реальные размеры окна, масштаб GUI и НАШ масштаб HUD
                int screenWidth = event.getWindow().getWidth();
                int screenHeight = event.getWindow().getHeight();
                double minecraftGuiScale = event.getWindow().getGuiScale();
                float mcGuiScaleFactor = (minecraftGuiScale <= 0) ? 1.0f : (float) minecraftGuiScale;
                // <<<--- ПОЛУЧАЕМ НАШ КАСТОМНЫЙ МАСШТАБ HUD --- >>>
                float fixedHudScale = (float)HUDColors.getInstance().getHudScaleFixed();

                // 4.2. Определяем БАЗОВЫЕ значения в ПИКСЕЛЯХ (при масштабе HUD = 1.0)
                // --- БАЗОВЫЕ значения в ПИКСЕЛЯХ ---
                int baseFixedBarWidth = 100;
                int baseFixedBarHeight = 5 + 3 * 2; // Высота контента + 2 * обводка
                int baseFixedOutline = 3;
                int baseFixedOffsetX = 15;
                int basePixelOffsetY = screenHeight / 8;
                int baseFixedVerticalSpacing = 5; // <<<--- ВОЗВРАЩАЕМ НЕБОЛЬШОЙ БАЗОВЫЙ ПРОБЕЛ (в пикселях)
                int baseTextPixelOffsetX = 5;
                int baseTextPixelOffsetY = 3;


                // --- АКТУАЛЬНЫЕ значения в ПИКСЕЛЯХ ---
                int actualFixedOutline = Math.max(1, Math.round(baseFixedOutline * fixedHudScale));
                int actualTotalBarWidth = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarWidth * fixedHudScale));
                int actualTotalBarHeight = Math.max(1 + 2 * actualFixedOutline, Math.round(baseFixedBarHeight * fixedHudScale));
                int actualFixedOffsetX = Math.round(baseFixedOffsetX * fixedHudScale);
                int actualFixedOffsetY = Math.round(basePixelOffsetY * fixedHudScale);
                int actualFixedVerticalSpacing = Math.max(1, Math.round(baseFixedVerticalSpacing * fixedHudScale)); // <<<--- РАССЧИТЫВАЕМ АКТУАЛЬНЫЙ ПРОБЕЛ
                int actualTextPixelOffsetX = Math.round(baseTextPixelOffsetX * fixedHudScale);
                int actualTextPixelOffsetY = Math.round(baseTextPixelOffsetY * fixedHudScale);

                // --- ПОЗИЦИЯ ---
                int pixelX = screenWidth - actualTotalBarWidth - actualFixedOffsetX;
                int pixelY_start = actualFixedOffsetY;
                int currentPixelY = pixelY_start;
                int lastBarBottomPixelY = 0;

                // Структура для хранения информации о тексте процентов
                record PercentTextInfo(String text, int pixelY) {}
                java.util.List<PercentTextInfo> percentTexts = new java.util.ArrayList<>();

                // --- Начало блока рендеринга с обратным масштабированием Minecraft GUI Scale ---
                poseStack.pushPose();
                // Масштабируем относительно Minecraft GUI Scale, чтобы пиксели были пикселями
                poseStack.scale(1.0f / mcGuiScaleFactor, 1.0f / mcGuiScaleFactor, 1.0f);

                // --- Отрисовка ПОЛОС и ТЕКСТА ПРОЦЕНТОВ ---
                float scaledTextWidth;
                int contentHeight = actualTotalBarHeight - 2 * actualFixedOutline; // Высота контента
                int contentCenterYOffset = contentHeight / 2; // Смещение до центра контента

                // 4.5. Отрисовка ПОЛОС с АКТУАЛЬНЫМИ ПИКСЕЛЬНЫМИ координатами/размерами
                // --- Отрисовка ПОЛОС ---
                if (absorption > 0) {
                    drawBarPixel(poseStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, absorptionDisplayPercent, HUDColors.getInstance().getHealthBarBackgroundColor(), HUDColors.absorptionBarColor);


                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", absorptionDisplayPercent * 100.0f), currentPixelY)); // Сохраняем текст и Y пиксел

                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing; // <<<--- ИЗМЕНЕНО: Добавляем пробел
                }


                drawCombineBarHealthPixel(poseStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, healthPercent, absorptionActualPercent);
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", healthPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing; // <<<--- ИЗМЕНЕНО


                if (mc.player.getArmorValue() > 0) {
                    drawBarPixel(poseStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, armorPercent, HUDColors.getInstance().getArmorBarBackgroundColor(), HUDColors.getInstance().getArmorBarColor());
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", armorPercent * 100.0f), currentPixelY));
                    currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing; // <<<--- ИЗМЕНЕНО
                }


                drawBarPixel(poseStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, foodPercent, HUDColors.getInstance().getFoodBarBackgroundColor(), HUDColors.getInstance().getFoodBarColor());
                percentTexts.add(new PercentTextInfo(String.format("%.0f%%", foodPercent * 100.0f), currentPixelY));
                currentPixelY += actualTotalBarHeight + actualFixedVerticalSpacing; // <<<--- ИЗМЕНЕНО

                // XP / Air - проверяем флаг внутри метода отрисовки
                int tempYBeforeXP = currentPixelY; // Запоминаем Y перед вызовом
                drawXPAndAirBarPixel(poseStack, pixelX, currentPixelY, actualTotalBarWidth, actualTotalBarHeight, actualFixedOutline, mc, xpPercent);
                boolean displayingAir = this.isDisplayingAirBar; // Получаем состояние после вызова drawXPAndAirBarPixel
                if (!displayingAir) {
                    percentTexts.add(new PercentTextInfo(String.format("%.0f%%", xpPercent * 100.0f), tempYBeforeXP)); // Используем Y до вызова
                }
                lastBarBottomPixelY = currentPixelY + actualTotalBarHeight;

                // Восстанавливаем масштаб Minecraft GUI ПЕРЕД отрисовкой текста
                poseStack.popPose();
                // --- Конец блока рендеринга полос ---

                // --- Отрисовка ТЕКСТОВ ПРОЦЕНТОВ (ВНЕ scale блока) ---
                if (HUDColors.getInstance().isPercentBars()) {
                    contentHeight = actualTotalBarHeight - 2 * actualFixedOutline;
                    contentCenterYOffset = contentHeight / 2;
                    for (PercentTextInfo info : percentTexts) {
                        scaledTextWidth = mc.font.width(info.text());
                        // Конвертируем пиксельные координаты в scaled units
                        float barLeftEdgeDrawX = (float) pixelX / mcGuiScaleFactor;
                        float barContentCenterDrawY = (float) (info.pixelY() + actualFixedOutline + contentCenterYOffset) / mcGuiScaleFactor;
                        // Отступ текста (пиксельный -> scaled)
                        float textScaledOffsetX = (float) TEXT_OFFSET_X_PIXELS / mcGuiScaleFactor;
                        // Координаты в scaled units
                        float textDrawX = barLeftEdgeDrawX - scaledTextWidth - textScaledOffsetX;
                        float textDrawY = barContentCenterDrawY - (float) FONT_HEIGHT / 2.0f; // Центрируем по вертикали
                        mc.font.drawShadow(poseStack, info.text(), textDrawX, textDrawY, TEXT_COLOR);
                    }
                }

                // 4.6. Отображение текста Уровня (позиция в СТАНДАРТНЫХ SCALED UNITS)
                // Рисуем ПОСЛЕ восстановления масштаба
                if (shouldShowLevelText) {
                    String levelText = String.valueOf(currentLevel);
                    scaledTextWidth = mc.font.width(levelText); // Стандартная ширина текста

                    // Конвертируем АКТУАЛЬНЫЕ пиксельные координаты краев HUD в стандартные scaled units
                    int barRightEdgePixelX = pixelX + actualTotalBarWidth;
                    float barRightEdgeDrawX = (float)barRightEdgePixelX / mcGuiScaleFactor;
                    float lastBarBottomDrawY = (float)lastBarBottomPixelY / mcGuiScaleFactor;

                    // Конвертируем АКТУАЛЬНЫЕ пиксельные отступы текста в стандартные scaled units
                    float textScaledOffsetX = (float)actualTextPixelOffsetX / mcGuiScaleFactor;
                    float textScaledOffsetY = (float)actualTextPixelOffsetY / mcGuiScaleFactor;

                    // Финальные координаты текста в SCALED UNITS
                    float textDrawX = barRightEdgeDrawX - scaledTextWidth - textScaledOffsetX;
                    float textDrawY = lastBarBottomDrawY + textScaledOffsetY;

                    mc.font.drawShadow(poseStack, levelText, textDrawX, textDrawY, 0xFFFFFF);
                }
            }
            // ========================================================
            // === РЕЖИМ ВАНИЛЬНОГО РАЗМЕРА (ЗАВИСИТ ОТ GUI SCALE) ===
            // ========================================================
            else {
                // 5.1. Получаем МАСШТАБИРОВАННЫЕ размеры окна
                int scaledWidth = event.getWindow().getGuiScaledWidth();
                int scaledHeight = event.getWindow().getGuiScaledHeight(); // Используем для Y

                // 5.2. Определяем размеры и отступы в МАСШТАБИРОВАННЫХ единицах (int)
                int scaledBarWidth = Math.min(Math.max(scaledWidth / 16, 50), 200); // Ширина КОНТЕНТА
                int scaledBarHeight = 3; // Высота КОНТЕНТА
                int scaledOutline = 2;  // Толщина обводки, которую твои методы добавляют
                int scaledOffsetX = 10;
                int scaledOffsetY = 75; // Твой оригинальный Y
                int scaledVerticalSpacing = 3; // <<<--- МАЛЕНЬКИЙ ПРОБЕЛ МЕЖДУ ПОЛОСАМИ

                // Полная высота одной полосы с обводкой для расчета смещения Y
                int totalBarHeightForSpacing = scaledBarHeight + 2 * scaledOutline;

                // 5.3. Рассчитываем позицию ЛЕВОГО ВЕРХНЕГО УГЛА КОНТЕНТА полосы-
                int scaledX = scaledWidth - scaledBarWidth - scaledOffsetX - scaledOutline; // Твой расчет X

                int scaledY_start = scaledOffsetY;
                int currentScaledY = scaledY_start;
                int lastScaledBarBottomY = 0;

                // --- Рендер ---
                poseStack.pushPose();

                // --- Отрисовка ПОЛОС (ТВОИМИ методами) и ТЕКСТА ПРОЦЕНТОВ ---
                String text; float scaledTextWidth; float textScaledX, textScaledY;

                // --- Полоса Поглощения ---
                if (absorption > 0) {
                    // 1. Рисуем бар (передаем КОНТЕНТНЫЕ размеры)
                    drawBarInt(poseStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, absorptionDisplayPercent, HUDColors.getInstance().getHealthBarBackgroundColor(), HUDColors.absorptionBarColor);
                    // 2. Рисуем текст процентов
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", absorptionDisplayPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        // X: Левее левой обводки (scaledX - scaledOutline)
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        // Y: Центрируем по высоте КОНТЕНТА
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(poseStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    // 3. Обновляем Y для следующей полосы (Высота + 2 Обводки + Пробел)
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }

                // --- Полоса Здоровья ---
                // 1. Рисуем бар
                drawCombineBarHealthInt(poseStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, healthPercent, absorptionActualPercent);
                // 2. Рисуем текст
                if (HUDColors.getInstance().isPercentBars()) {
                    text = String.format("%.0f%%", healthPercent * 100.0f);
                    scaledTextWidth = mc.font.width(text);
                    textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                    mc.font.drawShadow(poseStack, text, textScaledX, textScaledY, TEXT_COLOR);
                }
                // 3. Обновляем Y
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                // --- Полоса Брони ---
                if (mc.player.getArmorValue() > 0) {
                    // 1. Рисуем бар
                    drawBarInt(poseStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, armorPercent, HUDColors.getInstance().getArmorBarBackgroundColor(), HUDColors.getInstance().getArmorBarColor());
                    // 2. Рисуем текст
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", armorPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(poseStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                    // 3. Обновляем Y
                    currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;
                }

                // --- Полоса Голода ---
                // 1. Рисуем бар
                drawBarInt(poseStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, foodPercent, HUDColors.getInstance().getFoodBarBackgroundColor(), HUDColors.getInstance().getFoodBarColor());
                // 2. Рисуем текст
                if (HUDColors.getInstance().isPercentBars()) {
                    text = String.format("%.0f%%", foodPercent * 100.0f);
                    scaledTextWidth = mc.font.width(text);
                    textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                    textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                    mc.font.drawShadow(poseStack, text, textScaledX, textScaledY, TEXT_COLOR);
                }
                // 3. Обновляем Y
                currentScaledY += totalBarHeightForSpacing + scaledVerticalSpacing;

                // --- Полоса Опыта/Воздуха ---
                // 1. Рисуем бар
                drawXPAndAirBarInt(poseStack, scaledX, currentScaledY, scaledBarWidth, scaledBarHeight, scaledOutline, mc, xpPercent);
                // 2. Рисуем текст, если нужно
                boolean displayingAir = this.isDisplayingAirBar;
                if (!displayingAir) {
                    if (HUDColors.getInstance().isPercentBars()) {
                        text = String.format("%.0f%%", xpPercent * 100.0f);
                        scaledTextWidth = mc.font.width(text);
                        textScaledX = (scaledX - scaledOutline) - scaledTextWidth - TEXT_OFFSET_X_SCALED;
                        textScaledY = currentScaledY + ((float) scaledBarHeight / 2.0f) - ((float) FONT_HEIGHT / 2.0f);
                        mc.font.drawShadow(poseStack, text, textScaledX, textScaledY, TEXT_COLOR);
                    }
                }
                // 3. Запоминаем низ (учитывая обводку)
                lastScaledBarBottomY = currentScaledY + totalBarHeightForSpacing; // <<<--- Используем высоту с обводкой

                poseStack.popPose();

                // --- Отрисовка текста Уровня ---
                if (shouldShowLevelText) {
                    // Используем ТВОИ ОРИГИНАЛЬНЫЕ расчеты для текста уровня
                    String levelText=String.valueOf(currentLevel);
                    // !!! Убедись, что расчет textScaledX правильный для ТВОЕЙ логики обводки !!!
                    float LtextWidth=mc.font.width(levelText);
                    float LtextScaledOffsetX=5;
                    float LtextScaledOffsetY=2;
                    // Правый край обводки = scaledX (контент) + scaledBarWidth (контент) + scaledOutline (правая обводка)
                    float barRightEdgeDrawX = scaledX + scaledBarWidth + scaledOutline;
                    float LtextScaledX= barRightEdgeDrawX - LtextWidth - LtextScaledOffsetX;
                    // Y ниже последней полосы (lastScaledBarBottomY уже учитывает обводку)
                    float LtextScaledY=lastScaledBarBottomY + LtextScaledOffsetY;
                    mc.font.drawShadow(poseStack, levelText, LtextScaledX, LtextScaledY, 0xFFFFFF); // Используй levelText или fullText
                }
            }
        }
    }

    /**
     * Рисует стандартную полосу с обводкой (фон подложки) и ЦЕНТРИРОВАННЫМ заполнением В ПИКСЕЛЯХ.
     * x, y - координаты ВЕРХНЕГО ЛЕВОГО УГЛА ВНЕШНЕЙ ОБВОДКИ.
     */
    private void drawBarPixel(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        // Ширина и высота *внутренней* части (контента)
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return; // Не рисуем, если обводка съела всё

        // Ширина заполненной части *контента*
        int filledContentWidth = (int) (percentage * contentWidth);
        if (percentage > 0.99f && filledContentWidth < contentWidth) filledContentWidth = contentWidth;

        // Координаты верхнего левого угла *контента*
        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;

        // X-координата начала заполнения *контента* (центрирование)
        int fillStartX = contentX + (contentWidth - filledContentWidth) / 2;

        // 1. Рисуем подложку/обводку (черный прямоугольник на весь размер)
        fillPixelPerfect(poseStack, x, y, barWidth, barHeight, 0xFF000000);
        // 2. Рисуем фон поверх подложки (внутри обводки)
        fillPixelPerfect(poseStack, contentX, contentY, contentWidth, contentHeight, darkColor);
        // 3. Рисуем заполнение поверх фона
        if (filledContentWidth > 0) {
            fillPixelPerfect(poseStack, fillStartX, contentY, filledContentWidth, contentHeight, lightColor);
        }
    }

    /**
     * Рисует полосу здоровья с обводкой (фон подложки) и ЦЕНТРИРОВАННЫМ заполнением В ПИКСЕЛЯХ.
     * x, y - координаты ВЕРХНЕГО ЛЕВОГО УГЛА ВНЕШНЕЙ ОБВОДКИ.
     */
    private void drawCombineBarHealthPixel(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int healthFilledWidth = (int) (healthPercentage * contentWidth);
        if (healthPercentage > 0.99f && healthFilledWidth < contentWidth) healthFilledWidth = contentWidth;

        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;
        int healthStartX = contentX + (contentWidth - healthFilledWidth) / 2;

        int barColor = calculateHealthColor(healthPercentage);
        int bgColor = HUDColors.getInstance().getHealthBarBackgroundColor();

        // 1. Обводка/подложка
        fillPixelPerfect(poseStack, x, y, barWidth, barHeight, 0xFF000000);
        // 2. Фон
        fillPixelPerfect(poseStack, contentX, contentY, contentWidth, contentHeight, bgColor);
        // 3. Заполнение
        if (healthFilledWidth > 0) {
            fillPixelPerfect(poseStack, healthStartX, contentY, healthFilledWidth, contentHeight, barColor);
        }
    }

    /**
     * Рисует полосу Опыта или Воздуха В ПИКСЕЛЯХ с обводкой (фон подложки).
     * x, y - координаты ВЕРХНЕГО ЛЕВОГО УГЛА ВНЕШНЕЙ ОБВОДКИ.
     */
    private void drawXPAndAirBarPixel(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, Minecraft mc, float xpPercentage) {
        // Логика определения, что рисовать (воздух или опыт)
        int currentAir = mc.player.getAirSupply();
        int maxAir = mc.player.getMaxAirSupply();
        boolean isUnderwater = mc.player.isUnderWater();

        if (isUnderwater) {
            wasUnderWater = true;
            isDisplayingAirBar = true;
            maxAirSupply = maxAir; // Обновляем максимум на случай изменений
            lastAirSupply = currentAir; // Обновляем текущий воздух
            airPercentage = maxAir > 0 ? (float) currentAir / maxAir : 0; // Рассчитываем процент воздуха
        } else if (wasUnderWater) { // Если вышли из воды, но бар еще показывался
            isDisplayingAirBar = true;
            // Плавное восстановление воздуха в баре
            if (lastAirSupply < maxAirSupply) {
                // Инкрементируем запас воздуха для анимации (можно настроить скорость)
                lastAirSupply++;
                airPercentage = maxAirSupply > 0 ? (float) lastAirSupply / maxAirSupply : 0;
            }
            // Если бар восстановился, перестаем показывать воздух
            if (lastAirSupply >= maxAirSupply) {
                isDisplayingAirBar = false;
                wasUnderWater = false;
                airPercentage = 1.0f; // Сброс процента
            }
        } else { // Если не под водой и не было выхода только что
            isDisplayingAirBar = false;
            wasUnderWater = false;
        }
        int contentWidth = barWidth - 2 * outlineWidth;
        int contentHeight = barHeight - 2 * outlineWidth;
        if (contentWidth <= 0 || contentHeight <= 0) return;
        int contentX = x + outlineWidth;
        int contentY = y + outlineWidth;

        if (isDisplayingAirBar) {
            // Воздух
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = (int) (percentage * contentWidth);
            if (percentage > 0.99f && filledWidth < contentWidth) filledWidth = contentWidth;
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = HUDColors.getInstance().getAirBarBackgroundColor();
            int fgColor = HUDColors.getInstance().getAirBarColor();

            fillPixelPerfect(poseStack, x, y, barWidth, barHeight, 0xFF000000); // Обводка
            fillPixelPerfect(poseStack, contentX, contentY, contentWidth, contentHeight, bgColor); // Фон
            if (filledWidth > 0) fillPixelPerfect(poseStack, startX, contentY, filledWidth, contentHeight, fgColor); // Заполнение
        } else {
            // Опыт
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = (int) (percentage * contentWidth);
            if (percentage > 0.99f && filledWidth < contentWidth) filledWidth = contentWidth;
            int startX = contentX + (contentWidth - filledWidth) / 2;
            int bgColor = HUDColors.getInstance().getXpBarBackgroundColor();
            int fgColor = HUDColors.getInstance().getXpBarColor();

            fillPixelPerfect(poseStack, x, y, barWidth, barHeight, 0xFF000000); // Обводка
            fillPixelPerfect(poseStack, contentX, contentY, contentWidth, contentHeight, bgColor); // Фон
            if (filledWidth > 0) fillPixelPerfect(poseStack, startX, contentY, filledWidth, contentHeight, fgColor); // Заполнение
        }
    }

    // =======================================================
    // === СТАРЫЕ МЕТОДЫ ОТРИСОВКИ (принимают int) =========
    // === Используются для режима ВАНИЛЬНОГО РАЗМЕРА =======
    // =======================================================

    /** Версия drawBar с ЦЕНТРИРОВАННЫМ заполнением, принимающая int scaled units */
    private void drawBarInt(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float percentage, int darkColor, int lightColor) {
        percentage = Math.max(0, Math.min(percentage, 1.0f));
        int barWidthFilled = (int) (percentage * barWidth);
        if (percentage > 0.99f && barWidthFilled < barWidth) barWidthFilled = barWidth;
        int startX = x + (barWidth - barWidthFilled) / 2;

        GuiComponent.fill(poseStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
        GuiComponent.fill(poseStack, x, y, x + barWidth, y + barHeight, darkColor);
        if (barWidthFilled > 0) {
            GuiComponent.fill(poseStack, startX, y, startX + barWidthFilled, y + barHeight, lightColor);
        }
    }

    /** Версия drawCombineBarHealth с ЦЕНТРИРОВАННЫМ заполнением, принимающая int scaled units */
    private void drawCombineBarHealthInt(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, float healthPercentage, float absorptionPercentage) {
        healthPercentage = Math.max(0, Math.min(healthPercentage, 1));
        int healthWidth = (int) (healthPercentage * barWidth);
        if (healthPercentage > 0.99f && healthWidth < barWidth) healthWidth = barWidth;
        int healthStartX = x + (barWidth - healthWidth) / 2;

        GuiComponent.fill(poseStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
        GuiComponent.fill(poseStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getHealthBarBackgroundColor());
        int barColor = calculateHealthColor(healthPercentage);
        if (healthWidth > 0) {
            GuiComponent.fill(poseStack, healthStartX, y, healthStartX + healthWidth, y + barHeight, barColor);
        }
    }

    /**
     * Рисует полосу Опыта или Воздуха В SCALED UNITS (для ванильного режима).
     * Использует глобальные переменные состояния воздуха.
     * x, y - координаты ВЕРХНЕГО ЛЕВОГО угла самой полосы (без обводки).
     */
    private void drawXPAndAirBarInt(PoseStack poseStack, int x, int y, int barWidth, int barHeight, int outlineWidth, Minecraft mc, float xpPercentage) {
        // Логика определения, что рисовать (воздух или опыт) - ТА ЖЕ САМАЯ, ЧТО В PIXEL ВЕРСИИ
        int currentAir = mc.player.getAirSupply();
        int maxAir = mc.player.getMaxAirSupply();
        boolean isUnderwater = mc.player.isUnderWater();

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

        // Рисование
        if (isDisplayingAirBar) {
            // Рисуем Воздух
            float percentage = Math.max(0, Math.min(airPercentage, 1.0f));
            int filledWidth = (int) (percentage * barWidth);
            if (percentage > 0.99f && filledWidth < barWidth) filledWidth = barWidth;
            int startX = x + (barWidth - filledWidth) / 2;

            GuiComponent.fill(poseStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            GuiComponent.fill(poseStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getAirBarBackgroundColor()); // ЗАМЕНИ цвет
            if (filledWidth > 0) {
                GuiComponent.fill(poseStack, startX, y, startX + filledWidth, y + barHeight, HUDColors.getInstance().getAirBarColor()); // ЗАМЕНИ цвет
            }
        } else {
            // Рисуем Опыт
            float percentage = Math.max(0, Math.min(xpPercentage, 1.0f));
            int filledWidth = (int) (percentage * barWidth);
            if (percentage > 0.99f && filledWidth < barWidth) filledWidth = barWidth;
            int startX = x + (barWidth - filledWidth) / 2;

            GuiComponent.fill(poseStack, x - outlineWidth, y - outlineWidth, x + barWidth + outlineWidth, y + barHeight + outlineWidth, 0xFF000000);
            GuiComponent.fill(poseStack, x, y, x + barWidth, y + barHeight, HUDColors.getInstance().getXpBarBackgroundColor()); // ЗАМЕНИ цвет
            if (filledWidth > 0) {
                GuiComponent.fill(poseStack, startX, y, startX + filledWidth, y + barHeight, HUDColors.getInstance().getXpBarColor()); // ЗАМЕНИ цвет
            }
        }
    }

    // --- Метод расчета цвета здоровья (вынесен для чистоты) ---
    private int calculateHealthColor(float healthPercentage) {
        esmndHUDConfigScreen.GradientModeUse modeGradient = HUDColors.getInstance().getGradientModeUse(); // ЗАМЕНИ
        esmndHUDConfigScreen.PulseLowHealthMode pulseModeHealth = HUDColors.getInstance().getPulseMode(); // ЗАМЕНИ
        int barColor = HUDColors.getInstance().getHealthBarColor(); // ЗАМЕНИ

        // Градиент (логика из твоего кода)
        if (modeGradient != esmndHUDConfigScreen.GradientModeUse.OFF) {
            Minecraft mc = Minecraft.getInstance();
            boolean applyGradient = switch (modeGradient) {
                case HARDCORE_ONLY -> mc.level != null && mc.level.getLevelData().isHardcore();
                case SURVIVAL_ONLY -> isSurvivalNotHardcore();
                case BOTH -> true;
                default -> false;
            };
            if (applyGradient) {
                barColor = interpolateColor(HUDColors.getInstance().getHardcoreGradientOne(), HUDColors.getInstance().getHardcoreGradientTwo(), healthPercentage); // ЗАМЕНИ
            }
        }

        // Пульсация (логика из твоего кода)
        if (pulseModeHealth != esmndHUDConfigScreen.PulseLowHealthMode.OFF && healthPercentage < 0.4f) { // Порог 40%
            Minecraft mc = Minecraft.getInstance();
            boolean applyPulse = switch (pulseModeHealth) {
                case HARDCORE_ONLY -> mc.level != null && mc.level.getLevelData().isHardcore();
                case SURVIVAL_ONLY -> isSurvivalNotHardcore();
                case BOTH -> true;
                default -> false;
            };
            if (applyPulse) {
                float pulse = (float) (0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 200.0));
                int alpha = Math.max(0, Math.min(255, (int) (pulse * 255)));
                barColor = (barColor & 0x00FFFFFF) | (alpha << 24);
            }
        }
        return barColor;
    }

    private boolean isSurvivalNotHardcore()
    {
        return Minecraft.getInstance().level != null && !Minecraft.getInstance().level.getLevelData().isHardcore() && Minecraft.getInstance().gameMode.getPlayerMode().isSurvival();
    }

    public static int hexToColor(String hex) {
        try {
            return (int) Long.parseLong(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFF0000; // Возвращаем красный цвет по умолчанию в случае ошибки
        }
    }
}