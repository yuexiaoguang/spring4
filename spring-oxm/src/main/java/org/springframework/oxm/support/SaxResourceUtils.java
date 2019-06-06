package org.springframework.oxm.support;

import java.io.IOException;

import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;

/**
 * 处理SAX的便捷实用方法.
 */
public abstract class SaxResourceUtils {

	/**
	 * 从给定资源创建SAX {@code InputSource}.
	 * <p>将系统标识符设置为资源的{@code URL}.
	 * 
	 * @param resource 资源
	 * 
	 * @return 从资源创建的输入源
	 * @throws IOException 如果发生I/O异常
	 */
	public static InputSource createInputSource(Resource resource) throws IOException {
		InputSource inputSource = new InputSource(resource.getInputStream());
		inputSource.setSystemId(getSystemId(resource));
		return inputSource;
	}

	/**
	 * 从给定资源中检索URL作为系统ID.
	 * <p>如果无法打开, 则返回{@code null}.
	 */
	private static String getSystemId(Resource resource) {
		try {
			return resource.getURI().toString();
		}
		catch (IOException ex) {
			return null;
		}
	}
}
