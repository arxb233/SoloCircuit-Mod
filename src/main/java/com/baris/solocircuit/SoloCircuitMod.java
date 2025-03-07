package com.baris.solocircuit;

import com.baris.solocircuit.command.*;
import com.baris.solocircuit.util.TreeChopper;
import net.fabricmc.api.ModInitializer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoloCircuitMod implements ModInitializer {
    public static final String MOD_ID = "solocircuit-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
        ScCommand.register();
        FillCommand.register();
        TreeCommand.register();
        TreeChopper.register();
        FlyCommand.register();
        DigCommand.register();
        EnderBoxCommand.register();
        NetherHereCommand.register();
        FlatCommand.register();
        SkyChunkCommand.register();
        EasyCommand.register();
        TrashCommand.register();
        BoxCommand.register();
        MaterialCommand.register();
        HurtCommand.register();
    }
}