package org.springframework.core.serializer.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * 一个方便的委托, 具有预先安排的配置状态, 可满足常见的序列化需求.
 * 实现了{@link Serializer}和{@link Deserializer}, 所以也可以传递给更具体的回调方法.
 */
public class SerializationDelegate implements Serializer<Object>, Deserializer<Object> {

	private final Serializer<Object> serializer;

	private final Deserializer<Object> deserializer;


	/**
	 * 使用给定{@code ClassLoader}的默认序列化器/反序列化器.
	 */
	public SerializationDelegate(ClassLoader classLoader) {
		this.serializer = new DefaultSerializer();
		this.deserializer = new DefaultDeserializer(classLoader);
	}

	/**
	 * @param serializer 要使用的{@link Serializer} (never {@code null)}
	 * @param deserializer 要使用的{@link Deserializer} (never {@code null)}
	 */
	public SerializationDelegate(Serializer<Object> serializer, Deserializer<Object> deserializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.serializer = serializer;
		this.deserializer = deserializer;
	}


	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		this.serializer.serialize(object, outputStream);
	}

	@Override
	public Object deserialize(InputStream inputStream) throws IOException {
		return this.deserializer.deserialize(inputStream);
	}

}
