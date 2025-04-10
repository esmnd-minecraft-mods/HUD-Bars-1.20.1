package com.esmnd.hud;

import com.esmnd.hud.esmndConfigGUI; // Наш GUI
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

public class esmnd_GUIFactory  implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        // Обычно ничего не делаем здесь
    }

    @Override
    public boolean hasConfigGui() {
        return true; // Да, у нас есть конфиг GUI
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        // Создаем и возвращаем наш экран настроек
        return new esmndConfigGUI(parentScreen);
    }

    // --- Методы ниже обычно не используются для простых GUI ---
    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}