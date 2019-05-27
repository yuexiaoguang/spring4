package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * 实现JBoss Translator接口的适配器, 委托给下面的标准JDK {@link ClassFileTransformer}.
 *
 * <p>为了避免再次使用供应商API进行编译时检查, 正在使用动态代理.
 */
class JBossMCTranslatorAdapter implements InvocationHandler {

	private final ClassFileTransformer transformer;


	public JBossMCTranslatorAdapter(ClassFileTransformer transformer) {
		this.transformer = transformer;
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String name = method.getName();
		if ("equals".equals(name)) {
			return proxy == args[0];
		}
		else if ("hashCode".equals(name)) {
			return hashCode();
		}
		else if ("toString".equals(name)) {
			return toString();
		}
		else if ("transform".equals(name)) {
			return transform((ClassLoader) args[0], (String) args[1], (Class<?>) args[2],
					(ProtectionDomain) args[3], (byte[]) args[4]);
		}
		else if ("unregisterClassLoader".equals(name)) {
			unregisterClassLoader((ClassLoader) args[0]);
			return null;
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws Exception {

		return this.transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
	}

	public void unregisterClassLoader(ClassLoader loader) {
	}


	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}

}
