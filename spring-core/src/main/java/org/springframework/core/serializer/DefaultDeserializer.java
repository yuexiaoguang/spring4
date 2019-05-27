package org.springframework.core.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.NestedIOException;

/**
 * 一个默认的{@link Deserializer}实现, 它使用Java序列化读取输入流.
 */
public class DefaultDeserializer implements Deserializer<Object> {

	private final ClassLoader classLoader;


	/**
	 * 创建具有默认{@link ObjectInputStream}配置的{@code DefaultDeserializer}, 使用"最新的用户定义的ClassLoader".
	 */
	public DefaultDeserializer() {
		this.classLoader = null;
	}

	/**
	 * 创建一个{@code DefaultDeserializer}, 使用{@link ObjectInputStream}和给定的{@code ClassLoader}.
	 */
	public DefaultDeserializer(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 从提供的{@code InputStream}中读取, 并将内容反序列化为对象.
	 */
	@Override
	@SuppressWarnings("resource")
	public Object deserialize(InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
		try {
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new NestedIOException("Failed to deserialize object type", ex);
		}
	}
}
