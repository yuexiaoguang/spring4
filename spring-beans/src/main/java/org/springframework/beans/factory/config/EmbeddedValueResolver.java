package org.springframework.beans.factory.config;

import org.springframework.util.StringValueResolver;

/**
 * {@link StringValueResolver}适配器, 用于根据 {@link ConfigurableBeanFactory}解析占位符和表达式.
 *
 * <p>请注意, 与{@link ConfigurableBeanFactory#resolveEmbeddedValue}方法相比, 此适配器也会解析表达式.
 * 使用的{@link BeanExpressionContext}用于普通bean工厂, 没有为任何上下文对象指定范围.
 */
public class EmbeddedValueResolver implements StringValueResolver {

	private final BeanExpressionContext exprContext;

	private final BeanExpressionResolver exprResolver;


	public EmbeddedValueResolver(ConfigurableBeanFactory beanFactory) {
		this.exprContext = new BeanExpressionContext(beanFactory, null);
		this.exprResolver = beanFactory.getBeanExpressionResolver();
	}


	@Override
	public String resolveStringValue(String strVal) {
		String value = this.exprContext.getBeanFactory().resolveEmbeddedValue(strVal);
		if (this.exprResolver != null && value != null) {
			Object evaluated = this.exprResolver.evaluate(value, this.exprContext);
			value = (evaluated != null ? evaluated.toString() : null);
		}
		return value;
	}

}
