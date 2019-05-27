package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于ClassFileTransformer的织入器, 允许在类字节数组上应用变换器列表. 通常在类加载器内使用.
 *
 * <p>Note: 这个类是故意实现的, 用于最小的外部依赖, 因为它包含在weaver jar中 (要部署到应用程序服务器中).
 */
public class WeavingTransformer {

	private final ClassLoader classLoader;

	private final List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>();


	/**
	 * @param classLoader 用于构建变换器的ClassLoader
	 */
	public WeavingTransformer(ClassLoader classLoader) {
		if (classLoader == null) {
			throw new IllegalArgumentException("ClassLoader must not be null");
		}
		this.classLoader = classLoader;
	}


	/**
	 * 添加要由此weaver应用的类文件转换器.
	 * 
	 * @param transformer 要注册的类文件转换器
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		if (transformer == null) {
			throw new IllegalArgumentException("Transformer must not be null");
		}
		this.transformers.add(transformer);
	}


	/**
	 * 对给定的类字节定义应用转换.
	 * 该方法将始终返回非空字节数组 (如果未进行转换, 则数组内容将与原始数组相同).
	 * 
	 * @param className 点格式的类的完全限定名称 (i.e. some.package.SomeClass)
	 * @param bytes 类字节定义
	 * 
	 * @return (可能已转换) 类字节定义
	 */
	public byte[] transformIfNecessary(String className, byte[] bytes) {
		String internalName = className.replace(".", "/");
		return transformIfNecessary(className, internalName, bytes, null);
	}

	/**
	 * 对给定的类字节定义应用转换.
	 * 该方法将始终返回非空字节数组 (如果未进行转换, 则数组内容将与原始数组相同).
	 * 
	 * @param className 点格式的类的完全限定名称 (i.e. some.package.SomeClass)
	 * @param internalName 格式为/的类名称内部名称 (i.e. some/package/SomeClass)
	 * @param bytes 类字节定义
	 * @param pd 要使用的保护域 (can be null)
	 * 
	 * @return (可能已转换) 类字节定义
	 */
	public byte[] transformIfNecessary(String className, String internalName, byte[] bytes, ProtectionDomain pd) {
		byte[] result = bytes;
		for (ClassFileTransformer cft : this.transformers) {
			try {
				byte[] transformed = cft.transform(this.classLoader, internalName, null, pd, result);
				if (transformed != null) {
					result = transformed;
				}
			}
			catch (IllegalClassFormatException ex) {
				throw new IllegalStateException("Class file transformation failed", ex);
			}
		}
		return result;
	}

}
