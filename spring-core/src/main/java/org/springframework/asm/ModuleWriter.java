package org.springframework.asm;

final class ModuleWriter extends ModuleVisitor {
    /**
     * 必须添加此Module属性的类编写器.
     */
    private final ClassWriter cw;
    
    /**
     * Module属性的字节大小.
     */
    int size;
    
    /**
     * 与当前模块关联的属性数量 (Version, ConcealPackages, etc) 
     */
    int attributeCount;
    
    /**
     * 与当前模块关联的属性的字节大小
     */
    int attributesSize;
    
    /**
     * 常量池中的模块名称索引
     */
    private final int name;
    
    /**
     * 模块访问标志
     */
    private final int access;
    
    /**
     * 常量池中的模块版本索引或0
     */
    private final int version;
    
    /**
     * 常量池中的模块主类索引或0
     */
    private int mainClass;
    
    /**
     * 包的数量
     */
    private int packageCount;
    
    /**
     * 字节码形式的包.
     * 此字节向量仅包含项本身, 项的数量存储在packageCount中
     */
    private ByteVector packages;
    
    /**
     * 所需项的数量
     */
    private int requireCount;
    
    /**
     * 所需项的字节码形式.
     * 此字节向量仅包含项本身, 项的数量存储在requireCount中
     */
    private ByteVector requires;
    
    /**
     * 导出项的数量
     */
    private int exportCount;
    
    /**
     * 导出项的字节码形式.
     * 此字节向量仅包含项本身, 项的数量存储在exportCount中
     */
    private ByteVector exports;
    
    /**
     * 开放项的数量
     */
    private int openCount;
    
    /**
     * 开放项的字节码形式.
     * 此字节向量仅包含项本身, 项的数量存储在 openCount中
     */
    private ByteVector opens;
    
    /**
     * 使用项的数量
     */
    private int useCount;
    
    /**
     * 使用项的字节码形式.
     * 此字节向量仅包含项本身, 项的数量存储在 useCount中
     */
    private ByteVector uses;
    
    /**
     * 提供项的数量
     */
    private int provideCount;
    
    /**
     * 提供项的字节码形式.
     * 此字节向量仅包含项本身, 项的数量存储在 provideCount中
     */
    private ByteVector provides;
    
    ModuleWriter(final ClassWriter cw, final int name,
            final int access, final int version) {
        super(Opcodes.ASM6);
        this.cw = cw;
        this.size = 16;  // name + access + version + 5 counts
        this.name = name;
        this.access = access;
        this.version = version;
    }
    
    @Override
    public void visitMainClass(String mainClass) {
        if (this.mainClass == 0) { // 防止多次调用visitMainClass
            cw.newUTF8("ModuleMainClass");
            attributeCount++;
            attributesSize += 8;
        }
        this.mainClass = cw.newClass(mainClass);
    }
    
    @Override
    public void visitPackage(String packaze) {
        if (packages == null) { 
            // 防止多次调用 visitPackage
            cw.newUTF8("ModulePackages");
            packages = new ByteVector();
            attributeCount++;
            attributesSize += 8;
        }
        packages.putShort(cw.newPackage(packaze));
        packageCount++;
        attributesSize += 2;
    }
    
    @Override
    public void visitRequire(String module, int access, String version) {
        if (requires == null) {
            requires = new ByteVector();
        }
        requires.putShort(cw.newModule(module))
                .putShort(access)
                .putShort(version == null? 0: cw.newUTF8(version));
        requireCount++;
        size += 6;
    }
    
    @Override
    public void visitExport(String packaze, int access, String... modules) {
        if (exports == null) {
            exports = new ByteVector();
        }
        exports.putShort(cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            exports.putShort(0);
            size += 6;
        } else {
            exports.putShort(modules.length);
            for(String module: modules) {
                exports.putShort(cw.newModule(module));
            }    
            size += 6 + 2 * modules.length; 
        }
        exportCount++;
    }
    
    @Override
    public void visitOpen(String packaze, int access, String... modules) {
        if (opens == null) {
            opens = new ByteVector();
        }
        opens.putShort(cw.newPackage(packaze)).putShort(access);
        if (modules == null) {
            opens.putShort(0);
            size += 6;
        } else {
            opens.putShort(modules.length);
            for(String module: modules) {
                opens.putShort(cw.newModule(module));
            }    
            size += 6 + 2 * modules.length; 
        }
        openCount++;
    }
    
    @Override
    public void visitUse(String service) {
        if (uses == null) {
            uses = new ByteVector();
        }
        uses.putShort(cw.newClass(service));
        useCount++;
        size += 2;
    }
    
    @Override
    public void visitProvide(String service, String... providers) {
        if (provides == null) {
            provides = new ByteVector();
        }
        provides.putShort(cw.newClass(service));
        provides.putShort(providers.length);
        for(String provider: providers) {
            provides.putShort(cw.newClass(provider));
        }
        provideCount++;
        size += 4 + 2 * providers.length; 
    }
    
    @Override
    public void visitEnd() {
        // empty
    }

    void putAttributes(ByteVector out) {
        if (mainClass != 0) {
            out.putShort(cw.newUTF8("ModuleMainClass")).putInt(2).putShort(mainClass);
        }
        if (packages != null) {
            out.putShort(cw.newUTF8("ModulePackages"))
               .putInt(2 + 2 * packageCount)
               .putShort(packageCount)
               .putByteArray(packages.data, 0, packages.length);
        }
    }

    void put(ByteVector out) {
        out.putInt(size);
        out.putShort(name).putShort(access).putShort(version);
        out.putShort(requireCount);
        if (requires != null) {
            out.putByteArray(requires.data, 0, requires.length);
        }
        out.putShort(exportCount);
        if (exports != null) {
            out.putByteArray(exports.data, 0, exports.length);
        }
        out.putShort(openCount);
        if (opens != null) {
            out.putByteArray(opens.data, 0, opens.length);
        }
        out.putShort(useCount);
        if (uses != null) {
            out.putByteArray(uses.data, 0, uses.length);
        }
        out.putShort(provideCount);
        if (provides != null) {
            out.putByteArray(provides.data, 0, provides.length);
        }
    }    
}
