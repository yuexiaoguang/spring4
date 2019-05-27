package org.springframework.core.serializer.support;

import java.io.ByteArrayInputStream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.util.Assert;

/**
 * 委托给{@link org.springframework.core.serializer.Deserializer}的{@link Converter},
 * 将字节数组中的数据转换为对象.
 */
public class DeserializingConverter implements Converter<byte[], Object> {

	private final Deserializer<Object> deserializer;


	/**
	 * 使用默认的{@link java.io.ObjectInputStream}配置, 使用"用户最新定义的ClassLoader".
	 */
	public DeserializingConverter() {
		this.deserializer = new DefaultDeserializer();
	}

	/**
	 * 使用{@link java.io.ObjectInputStream}和给定的{@code ClassLoader}.
	 */
	public DeserializingConverter(ClassLoader classLoader) {
		this.deserializer = new DefaultDeserializer(classLoader);
	}

	/**
	 * 委托给提供的{@link Deserializer}.
	 */
	public DeserializingConverter(Deserializer<Object> deserializer) {
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.deserializer = deserializer;
	}


	@Override
	public Object convert(byte[] source) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
		try {
			return this.deserializer.deserialize(byteStream);
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to deserialize payload. " +
					"Is the byte array a result of corresponding serialization for " +
					this.deserializer.getClass().getSimpleName() + "?", ex);
		}
	}

}
