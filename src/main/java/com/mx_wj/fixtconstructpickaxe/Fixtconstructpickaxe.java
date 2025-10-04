package com.mx_wj.fixtconstructpickaxe;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Fixtconstructpickaxe.MODID)
public class Fixtconstructpickaxe {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "fixtconstructpickaxe";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Fixtconstructpickaxe() {
        LOGGER.info("FixTconstructPickaxe Loaded!");
    }
}
