package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 默认Key生成器.
 * 如果没有提供参数, 则返回{@value #NO_PARAM_KEY}; 如果只给出一个参数本身, 则返回参数本身, 或者根据所有给定参数的哈希码值计算哈希码.
 * 对于给定的任何{@code null}参数, 使用常量值{@value #NULL_PARAM_KEY}.
 *
 * <p>NOTE: 由于此实现仅返回参数的散列, 因此可能发生键冲突.
 * 从Spring 4.0开始, 当没有定义明确的Key生成器时, 使用{@link SimpleKeyGenerator}.
 * 此类仍适用于不希望迁移到{@link SimpleKeyGenerator}的应用程序.
 *
 * @deprecated as of Spring 4.0, in favor of {@link SimpleKeyGenerator}
 * or custom {@link KeyGenerator} implementations based on hash codes
 */
@Deprecated
public class DefaultKeyGenerator implements KeyGenerator {

	public static final int NO_PARAM_KEY = 0;

	public static final int NULL_PARAM_KEY = 53;


	@Override
	public Object generate(Object target, Method method, Object... params) {
		if (params.length == 0) {
			return NO_PARAM_KEY;
		}
		if (params.length == 1) {
			Object param = params[0];
			if (param == null) {
				return NULL_PARAM_KEY;
			}
			if (!param.getClass().isArray()) {
				return param;
			}
		}
		return Arrays.deepHashCode(params);
	}

}
