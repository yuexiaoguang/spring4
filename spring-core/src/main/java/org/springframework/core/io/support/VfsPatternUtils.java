package org.springframework.core.io.support;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

import org.springframework.core.io.VfsUtils;

/**
 * 用于访问{@link VfsUtils}方法, 而不将它们暴露给整个世界的人工类.
 */
abstract class VfsPatternUtils extends VfsUtils {

	static Object getVisitorAttribute() {
		return doGetVisitorAttribute();
	}

	static String getPath(Object resource) {
		return doGetPath(resource);
	}

	static Object findRoot(URL url) throws IOException {
		return getRoot(url);
	}

	static void visit(Object resource, InvocationHandler visitor) throws IOException {
		Object visitorProxy = Proxy.newProxyInstance(
				VIRTUAL_FILE_VISITOR_INTERFACE.getClassLoader(),
				new Class<?>[] {VIRTUAL_FILE_VISITOR_INTERFACE}, visitor);
		invokeVfsMethod(VIRTUAL_FILE_METHOD_VISIT, resource, visitorProxy);
	}

}
