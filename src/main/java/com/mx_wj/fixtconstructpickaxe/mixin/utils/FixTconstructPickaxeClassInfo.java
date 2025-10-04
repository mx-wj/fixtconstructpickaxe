/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mx_wj.fixtconstructpickaxe.mixin.utils;

import com.google.common.io.Resources;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class FixTconstructPickaxeClassInfo {
    private static final Map<String, FixTconstructPickaxeClassInfo> cache;

    private static ILaunchPluginService.ITransformerLoader iTransformerLoader;

    public static final FixTconstructPickaxeClassInfo OBJECT_CLASS_INFO;

    static {
        cache = new HashMap<>();
        OBJECT_CLASS_INFO = new FixTconstructPickaxeClassInfo("java/lang/Object", null);
        cache.put("java/lang/Object", OBJECT_CLASS_INFO);
    }

    public String name;
    public String superName;
    public FixTconstructPickaxeClassInfo superClassInfo;

    public FixTconstructPickaxeClassInfo(String name, String superName) {
        this.name = name;
        this.superName = superName;
    }

    public FixTconstructPickaxeClassInfo getSuperClassInfo() {
        if (superClassInfo == null && superName != null) {
            superClassInfo = FixTconstructPickaxeClassInfo.forName(superName);
        }
        return superClassInfo;
    }

    public boolean isSuperClass(String name) {
        name = name.replace('.', '/');
        for (FixTconstructPickaxeClassInfo current = this; current != null; current = current.getSuperClassInfo()) {
            if (Objects.equals(current.name, name)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSuperClass(String superClassName) {
        if(superClassName == null){
            return false;
        }
        superClassName = superClassName.replace('.', '/');
        if (superClassName.equals("java/lang/Object")) {
            return true;
        }
        return this.findSuperClass(superClassName) != null;
    }

    public boolean hasSuperClass(FixTconstructPickaxeClassInfo superClass) {
        if(superClass == null){
            return false;
        }
        if (FixTconstructPickaxeClassInfo.OBJECT_CLASS_INFO == superClass) {
            return true;
        }
        return this.findSuperClass(superClass.name) != null;
    }

    public FixTconstructPickaxeClassInfo findSuperClass(String superClass) {
        if (FixTconstructPickaxeClassInfo.OBJECT_CLASS_INFO.name.equals(superClass)) {
            return null;
        }
        FixTconstructPickaxeClassInfo superClassInfo = this.getSuperClassInfo();
        if (superClassInfo != null) {
            if (superClass.equals(superClassInfo.name)) {
                return superClassInfo;
            }
            FixTconstructPickaxeClassInfo found = superClassInfo.findSuperClass(superClass);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static FixTconstructPickaxeClassInfo forName(String name) {
        if(iTransformerLoader == null){
            throw new IllegalStateException("iTransformerLoader is null, which is too early!");
        }

        name = name.replace('.', '/');
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        byte[] classBytes = null;
        try {
            classBytes = FixTconstructPickaxeClassInfo.getClassBytes(name);
        } catch (Throwable t){
            t.printStackTrace();
            cache.put(name, null);
        }

        if(classBytes != null){
            ClassReader classReader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, 0);
            FixTconstructPickaxeClassInfo classInfo = new FixTconstructPickaxeClassInfo(name, classNode.superName);
            cache.put(name, classInfo);
            return classInfo;
        }
        return OBJECT_CLASS_INFO;
    }

    private static byte[] getClassBytes(String name) throws Throwable {
        if(iTransformerLoader == null){
            throw new IllegalStateException("iTransformerLoader is null, which is too early!");
        }
        String canonicalName = name.replace('/', '.');
        String internalName = name.replace('.', '/');
        byte[] classBytes;
        try {
            classBytes = iTransformerLoader.buildTransformedClassNodeFor(canonicalName);
        } catch (ClassNotFoundException ex) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(internalName + ".class");
            if (url == null) {
                throw ex;
            }
            try {
                classBytes = Resources.asByteSource(url).read();
            } catch (IOException ioex) {
                throw ex;
            }
        }
        return classBytes;
    }

    public static void setITransformerLoader(ILaunchPluginService.ITransformerLoader iTransformerLoader) {
        FixTconstructPickaxeClassInfo.iTransformerLoader = iTransformerLoader;
    }
}
