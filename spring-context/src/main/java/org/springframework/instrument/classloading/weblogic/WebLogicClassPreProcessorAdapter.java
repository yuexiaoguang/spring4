package org.springframework.instrument.classloading.weblogic;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Hashtable;

/**
 * 实现WebLogic ClassPreProcessor接口的适配器, 委托给下面的标准JDK {@link ClassFileTransformer}.
 *
 * <p>为了避免再次使用供应商API进行编译时检查, 正在使用动态代理.
 */
class WebLogicClassPreProcessorAdapter implements InvocationHandler {

	private final ClassFileTransformer transformer;

	private final ClassLoader loader;


	public WebLogicClassPreProcessorAdapter(ClassFileTransformer transformer, ClassLoader loader) {
		this.transformer = transformer;
		this.loader = loader;
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
		else if ("initialize".equals(name)) {
			initialize((Hashtable<?, ?>) args[0]);
			return null;
		}
		else if ("preProcess".equals(name)) {
			return preProcess((String) args[0], (byte[]) args[1]);
		}
		else {
			throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	public void initialize(Hashtable<?, ?> params) {
	}

	public byte[] preProcess(String className, byte[] classBytes) {
		try {
			byte[] result = this.transformer.transform(this.loader, className, null, null, classBytes);
			return (result != null ? result : classBytes);
		}
		catch (IllegalClassFormatException ex) {
			throw new IllegalStateException("Cannot transform due to illegal class format", ex);
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}

}
