package org.apache.catalina.loader;

/**
 * Tomcat的{@code WebappClassLoader}的模拟, 仅用于Spring的编译目的.
 * 公开7.0.63之前, 以及{@code findResourceInternal}的7.0.63+变体.
 */
public class WebappClassLoader extends ClassLoader {

	public WebappClassLoader() {
	}

	public WebappClassLoader(ClassLoader parent) {
		super(parent);
	}


	protected ResourceEntry findResourceInternal(String name, String path) {
		throw new UnsupportedOperationException();
	}

	protected ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
		throw new UnsupportedOperationException();
	}

}
