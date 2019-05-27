package org.springframework.instrument.classloading.websphere;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.CodeSource;

import org.springframework.util.FileCopyUtils;

/**
 * 实现WebSphere 7.0 ClassPreProcessPlugin接口的适配器, 委托给下面的标准JDK {@link ClassFileTransformer}.
 *
 * <p>为了避免再次使用供应商API进行编译时检查, 正在使用动态代理.
 */
class WebSphereClassPreDefinePlugin implements InvocationHandler {

	private final ClassFileTransformer transformer;


	/**
	 * @param transformer 要调整的{@link ClassFileTransformer} (must not be {@code null})
	 */
	public WebSphereClassPreDefinePlugin(ClassFileTransformer transformer) {
		this.transformer = transformer;
		ClassLoader classLoader = transformer.getClass().getClassLoader();

		// 首先通过调用虚拟类的转换来强制织入器的完整类加载
		try {
			String dummyClass = Dummy.class.getName().replace('.', '/');
			byte[] bytes = FileCopyUtils.copyToByteArray(classLoader.getResourceAsStream(dummyClass + ".class"));
			transformer.transform(classLoader, dummyClass, null, null, bytes);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot load transformer", ex);
		}
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();
		if ("equals".equals(name)) {
			return (proxy == args[0]);
		}
		else if ("hashCode".equals(name)) {
			return hashCode();
		}
		else if ("toString".equals(name)) {
			return toString();
		}
		else if ("transformClass".equals(name)) {
			return transform((String) args[0], (byte[]) args[1], (CodeSource) args[2], (ClassLoader) args[3]);
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	protected byte[] transform(String className, byte[] classfileBuffer, CodeSource codeSource, ClassLoader classLoader)
			throws Exception {

		// NB: WebSphere将className传递为 ".", 而没有类, 但转换器需要 VM "/" 格式
		byte[] result = transformer.transform(classLoader, className.replace('.', '/'), null, null, classfileBuffer);
		return (result != null ? result : classfileBuffer);
	}

	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}


	private static class Dummy {
	}

}
