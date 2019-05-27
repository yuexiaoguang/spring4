package org.springframework.core.serializer.support;

import java.io.ByteArrayOutputStream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * 委托给{@link org.springframework.core.serializer.Serializer}的{@link Converter},
 * 以将对象转换为字节数组.
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private final Serializer<Object> serializer;


	/**
	 * 创建使用标准Java序列化的默认{@code SerializingConverter}.
	 */
	public SerializingConverter() {
		this.serializer = new DefaultSerializer();
	}

	/**
	 * 创建一个委托给提供的{@link Serializer}的{@code SerializingConverter}.
	 */
	public SerializingConverter(Serializer<Object> serializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		this.serializer = serializer;
	}


	/**
	 * 序列化源对象, 并返回字节数组结果.
	 */
	@Override
	public byte[] convert(Object source) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
		try  {
			this.serializer.serialize(source, byteStream);
			return byteStream.toByteArray();
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to serialize object using " +
					this.serializer.getClass().getSimpleName(), ex);
		}
	}
}
