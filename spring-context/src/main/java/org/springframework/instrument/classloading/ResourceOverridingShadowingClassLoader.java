package org.springframework.instrument.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * ShadowingClassLoader的子类, 它覆盖了查找某些文件的尝试.
 */
public class ResourceOverridingShadowingClassLoader extends ShadowingClassLoader {

	private static final Enumeration<URL> EMPTY_URL_ENUMERATION = new Enumeration<URL>() {
		@Override
		public boolean hasMoreElements() {
			return false;
		}
		@Override
		public URL nextElement() {
			throw new UnsupportedOperationException("Should not be called. I am empty.");
		}
	};


	/**
	 * value是实际的值
	 */
	private Map<String, String> overrides = new HashMap<String, String>();


	/**
	 * 装饰给定的ClassLoader.
	 * 
	 * @param enclosingClassLoader 要装饰的ClassLoader
	 */
	public ResourceOverridingShadowingClassLoader(ClassLoader enclosingClassLoader) {
		super(enclosingClassLoader);
	}


	/**
	 * 尝试在旧路径上查找资源时, 返回新路径上的资源.
	 * 
	 * @param oldPath 请求的路径
	 * @param newPath 要查找的实际路径
	 */
	public void override(String oldPath, String newPath) {
		this.overrides.put(oldPath, newPath);
	}

	/**
	 * 确保找不到具有给定路径的资源.
	 * 
	 * @param oldPath 要隐藏的资源的路径, 即使它存在于父级ClassLoader中
	 */
	public void suppress(String oldPath) {
		this.overrides.put(oldPath, null);
	}

	/**
	 * 复制给定的ClassLoader中的所有覆盖.
	 * 
	 * @param other 从中复制的另一个ClassLoader
	 */
	public void copyOverrides(ResourceOverridingShadowingClassLoader other) {
		Assert.notNull(other, "Other ClassLoader must not be null");
		this.overrides.putAll(other.overrides);
	}


	@Override
	public URL getResource(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResource(overriddenPath) : null);
		}
		else {
			return super.getResource(requestedPath);
		}
	}

	@Override
	public InputStream getResourceAsStream(String requestedPath) {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenPath = this.overrides.get(requestedPath);
			return (overriddenPath != null ? super.getResourceAsStream(overriddenPath) : null);
		}
		else {
			return super.getResourceAsStream(requestedPath);
		}
	}

	@Override
	public Enumeration<URL> getResources(String requestedPath) throws IOException {
		if (this.overrides.containsKey(requestedPath)) {
			String overriddenLocation = this.overrides.get(requestedPath);
			return (overriddenLocation != null ?
					super.getResources(overriddenLocation) : EMPTY_URL_ENUMERATION);
		}
		else {
			return super.getResources(requestedPath);
		}
	}

}
