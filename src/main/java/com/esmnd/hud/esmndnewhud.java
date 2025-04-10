package com.esmnd.hud;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;

@Mod(modid = "esmndnewhud", name = "HUD Bars by esmnd", version = "1.0.0", guiFactory = "com.esmnd.hud.esmnd_GUIFactory")
public class esmndnewhud {

    @Mod.Instance
    public static esmndnewhud instance;

    public esmndHudRenderer hudRenderer; // экземпляр рендера

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // logger = event.getModLog(); // Инициализация логгера

        // --- ИНИЦИАЛИЗАЦИЯ КОНФИГА ---
        File suggestedConfigFile = event.getSuggestedConfigurationFile();
        HUDColors.init(suggestedConfigFile);
        // --- КОНЕЦ ИНИЦИАЛИЗАЦИИ ---

        // создание экземпляра только на клиенте
        if (event.getSide() == Side.CLIENT) {
            hudRenderer = new esmndHudRenderer();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (hudRenderer != null) {
            MinecraftForge.EVENT_BUS.register(hudRenderer);
        }
    }
}