package org.springframework.core.serializer;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于将InputStream中的数据转换为Object的策略接口.
 */
public interface Deserializer<T> {

	/**
	 * 从给定的InputStream中读取(assemble)类型为T的对象.
	 * <p>Note: 实现不应该关闭给定的InputStream (或该InputStream的任何装饰器), 而是将其留给调用者.
	 * 
	 * @param inputStream 输入流
	 * 
	 * @return 反序列化的对象
	 * @throws IOException 如果从流中读取错误
	 */
	T deserialize(InputStream inputStream) throws IOException;

}
