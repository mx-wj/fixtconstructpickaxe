package com.mx_wj.fixtconstructpickaxe.mixin;

import com.mx_wj.fixtconstructpickaxe.plugins.FixTconstructPickaxePlugin;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FixTconstructPickaxeMixinConfigPlugin implements IMixinConfigPlugin {
    private final Logger logger = LogManager.getLogger("CommonSwordService");

    @Override
    public void onLoad(String mixinPackage) {
        try{
            logger.info("[FixTconstructPickaxeCore] init");
            Field field = Launcher.class.getDeclaredField("launchPlugins");
            field.setAccessible(true);
            LaunchPluginHandler launchPluginHandler = (LaunchPluginHandler)field.get(Launcher.INSTANCE);
            Field field1 = LaunchPluginHandler.class.getDeclaredField("plugins");
            field1.setAccessible(true);
            Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>)field1.get(launchPluginHandler);
            plugins.put("FixTconstructPickaxePlugin", FixTconstructPickaxePlugin.INSTANCE);
            logger.info("[FixTconstructPickaxeCore] init successfully");
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
