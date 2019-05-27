package org.springframework.core.serializer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 一个{@link Serializer}实现, 它使用Java序列化将对象写入输出流.
 */
public class DefaultSerializer implements Serializer<Object> {

	/**
	 * 使用Java序列化将源对象写入输出流.
	 * 源对象必须实现{@link Serializable}.
	 */
	@Override
	public void serialize(Object object, OutputStream outputStream) throws IOException {
		if (!(object instanceof Serializable)) {
			throw new IllegalArgumentException(getClass().getSimpleName() + " requires a Serializable payload " +
					"but received an object of type [" + object.getClass().getName() + "]");
		}
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(object);
		objectOutputStream.flush();
	}

}
