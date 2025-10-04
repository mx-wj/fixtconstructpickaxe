package com.mx_wj.fixtconstructpickaxe.mixin.plugins;

import com.mx_wj.fixtconstructpickaxe.mixin.utils.FixTconstructPickaxeClassInfo;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FixTconstructPickaxePlugin implements ILaunchPluginService {
    private final Logger logger = LogManager.getLogger("CommonSword");
    public static final FixTconstructPickaxePlugin INSTANCE = new FixTconstructPickaxePlugin();

    @Override
    public String name() {
        return "FixTconstructPickaxePlugin";
    }

    @Override
    public EnumSet<ILaunchPluginService.Phase> handlesClass(Type classType, boolean isEmpty) {
        return EnumSet.of(ILaunchPluginService.Phase.BEFORE);
    }

    @Override
    public void initializeLaunch(ILaunchPluginService.ITransformerLoader transformerLoader, NamedPath[] specialPaths) {
        FixTconstructPickaxeClassInfo.setITransformerLoader(transformerLoader);
    }

    @Override
    public boolean processClass(final Phase phase, ClassNode classNode, final Type classType, String reason) {
        if(!reason.equals("classloading")){
            return false;
        }
        String className = classType.getClassName().replace("/", ".");
        int index = className.lastIndexOf(".");
        if(index != -1 && className.substring(index + 1).startsWith("__")){
            return false;
        }
        try{
            FixTconstructPickaxeClassInfo classInfo = FixTconstructPickaxeClassInfo.forName(classType.getClassName());
            if(classInfo != null && classInfo.hasSuperClass("net/minecraft/world/level/block/Block")){
                for(MethodNode methodNode : classNode.methods){
                    if(methodNode.name.equals("canHarvestBlock")){
                        int itemStackCount = 0;
                        List<AbstractInsnNode> checkCheckInsnNodes = new ArrayList<>();
                        List<AbstractInsnNode> getItemInsnNodes = new ArrayList<>();
                        List<AbstractInsnNode> removeInsnNodes = new ArrayList<>();
                        for(AbstractInsnNode insnNode : methodNode.instructions.toArray()){
                            if(insnNode instanceof TypeInsnNode typeInsnNode){
                                if(typeInsnNode.getOpcode() == Opcodes.INSTANCEOF && typeInsnNode.desc.equals("net/minecraft/world/item/PickaxeItem")){
                                    MethodInsnNode methodInsnNode = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mx_wj/fixtconstructpickaxe/core/EventUtils", "isPickaxe", "(Lnet/minecraft/world/item/Item;)Z", false);
                                    methodNode.instructions.set(typeInsnNode, methodInsnNode);
                                }
                                if(typeInsnNode.getOpcode() == Opcodes.CHECKCAST && typeInsnNode.desc.equals("net/minecraft/world/item/PickaxeItem")){
                                    MethodInsnNode methodInsnNode = new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mx_wj/fixtconstructpickaxe/core/EventUtils", "getTiered", "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/Tier;", false);
                                    methodNode.instructions.set(typeInsnNode, methodInsnNode);
                                    checkCheckInsnNodes.add(methodInsnNode);
                                }
                            }
                            if(insnNode instanceof MethodInsnNode methodInsnNode){
                                if(methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL && methodInsnNode.owner.equals("net/minecraft/world/item/ItemStack") && methodInsnNode.name.equals("m_41720_")){
                                    getItemInsnNodes.add(methodInsnNode);
                                }
                                if(methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL && methodInsnNode.owner.equals("net/minecraft/world/item/PickaxeItem") && methodInsnNode.name.equals("m_43314_")){
                                    removeInsnNodes.add(methodInsnNode);
                                }
                            }
                        }
                        itemStackCount = getItemInsnNodes.size();
                        for(int i = 0; i < getItemInsnNodes.size(); i++){
                            InsnList insnList = new InsnList();
                            insnList.add(new InsnNode(Opcodes.DUP));
                            // 直接使用 maxLocals + i，不再加1
                            insnList.add(new VarInsnNode(Opcodes.ASTORE, methodNode.maxLocals + i));
                            methodNode.instructions.insertBefore(getItemInsnNodes.get(i), insnList);
                        }
                        for(int i = 0; i < checkCheckInsnNodes.size(); i++){
                            int var = i;
                            if(itemStackCount <= i){
                                var = itemStackCount -1;
                            }
                            int indexForCheckCast = methodNode.instructions.indexOf(checkCheckInsnNodes.get(i));
                            AbstractInsnNode insnNode = methodNode.instructions.get(indexForCheckCast - 1);
                            if(insnNode instanceof VarInsnNode varInsnNode){
                                varInsnNode.var = methodNode.maxLocals + var;
                            }
                        }
                        for(AbstractInsnNode insnNode : removeInsnNodes){
                            methodNode.instructions.remove(insnNode);
                        }
                        methodNode.maxLocals += itemStackCount;
                        return true;
                    }
                }
            }
        }catch (Throwable t){
            t.printStackTrace();
        }
        /*if(classType.getClassName().equals("net/minecraft/world/item/PickaxeItem") || classType.getClassName().equals("slimeknights/tconstruct/library/tools/item/ModifiableItem")){
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, "getTier", "()Lnet/minecraft/world/item/Tier;", null, null);
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/item/TieredItem", "m_43314_", "()Lnet/minecraft/world/item/Tier;", false));
            methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(methodNode);
        }*/
        return false;
    }
}
