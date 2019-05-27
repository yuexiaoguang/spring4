package org.springframework.asm;

/**
 * 访问Java模块的访问器.
 * 必须按以下顺序调用此类的方法:
 * <tt>visitVersion</tt> | <tt>visitMainClass</tt> |
 * <tt>visitTargetPlatform</tt> | ( <tt>visitConcealedPackage</tt> | <tt>visitRequire</tt> |
 * <tt>visitExport</tt> | <tt>visitUse</tt> | <tt>visitProvide</tt> )* <tt>visitEnd</tt>.
 */
public abstract class ModuleVisitor {
    /**
     * 此访问器实现的ASM API版本.
     * 该字段的值必须为{@link Opcodes#ASM6}.
     */
    protected final int api;
    
    /**
     * 此访问器必须委托方法调用的模块访问器.
     * May be null.
     */
    protected ModuleVisitor mv;
    
    
    public ModuleVisitor(final int api) {
        this(api, null);
    }

    /**
     * @param api
     *            此访问器实现的ASM API版本. Must be {@link Opcodes#ASM6}.
     * @param mv
     *            此访问器必须委托方法调用的模块访问器. May be null.
     */
    public ModuleVisitor(final int api, final ModuleVisitor mv) {
        if (api != Opcodes.ASM6) {
            throw new IllegalArgumentException();
        }
        this.api = api;
        this.mv = mv;
    }
    
    /**
     * 访问当前模块的主类.
     * 
     * @param mainClass 当前模块的主类.
     */
    public void visitMainClass(String mainClass) {
        if (mv != null) {
            mv.visitMainClass(mainClass);
        }
    }
    
    /**
     * 访问当前模块的隐藏包.
     * 
     * @param packaze 隐藏包的名称
     */
    public void visitPackage(String packaze) {
        if (mv != null) {
            mv.visitPackage(packaze);
        }
    }
    
    /**
     * 访问当前模块的依赖关系.
     * 
     * @param module 依赖的模块名称
     * @param access ACC_TRANSITIVE, ACC_STATIC_PHASE, ACC_SYNTHETIC 和 ACC_MANDATED之间的依赖关系的访问标志.
     * @param version 编译时的模块版本或null.
     */
    public void visitRequire(String module, int access, String version) {
        if (mv != null) {
            mv.visitRequire(module, access, version);
        }
    }
    
    /**
     * 访问当前模块的导出包.
     * 
     * @param packaze 导出的包的名称.
     * @param access 导出包的访问标志, 有效值是{@code ACC_SYNTHETIC} 或 {@code ACC_MANDATED}.
     * @param modules 可以访问导出的包的公共类的模块的名称或<tt>null</tt>.
     */
    public void visitExport(String packaze, int access, String... modules) {
        if (mv != null) {
            mv.visitExport(packaze, access, modules);
        }
    }
    
    /**
     * 访问当前模块的开放包.
     * 
     * @param packaze 开放包的名称.
     * @param access 开放包的访问标志, 有效值是{@code ACC_SYNTHETIC} 或 {@code ACC_MANDATED}.
     * @param modules 可以对open包的类使用深度反射的模块的名称或<tt>null</tt>.
     */
    public void visitOpen(String packaze, int access, String... modules) {
        if (mv != null) {
            mv.visitOpen(packaze, access, modules);
        }
    }
    
    /**
     * 访问当前模块使用的服务.
     * 名称必须是接口或抽象类的名称.
     * 
     * @param service 服务的内部名称.
     */
    public void visitUse(String service) {
        if (mv != null) {
            mv.visitUse(service);
        }
    }
    
    /**
     * 访问服务的实现.
     * 
     * @param service 服务的内部名称.
     * @param providers 服务的实现的内部名称 (至少有一个提供者).
     */
    public void visitProvide(String service, String... providers) {
        if (mv != null) {
            mv.visitProvide(service, providers);
        }
    }
    
    public void visitEnd() {
        if (mv != null) {
            mv.visitEnd();
        }
    }
}
