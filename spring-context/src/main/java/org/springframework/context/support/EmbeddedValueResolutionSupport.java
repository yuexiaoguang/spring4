package org.springframework.context.support;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

/**
 * 方便的基类, 适用于需要嵌入式值解析的组件
 * (i.e. {@link org.springframework.context.EmbeddedValueResolverAware} 消费者).
 */
public class EmbeddedValueResolutionSupport implements EmbeddedValueResolverAware {

	private StringValueResolver embeddedValueResolver;


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * 通过此实例的{@link StringValueResolver}解析给定的嵌入值.
	 * 
	 * @param value 要解析的值
	 * 
	 * @return 解析后的值; 如果没有可用的解析器, 则始终为原始值
	 */
	protected String resolveEmbeddedValue(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


}
