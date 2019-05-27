package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.util.StringValueResolver;

/**
 * 希望获得<b>StringValueResolver</b>通知的对象实现的接口, 用于解析嵌套的定义值.
 *
 * <p>这是通过ApplicationContextAware/BeanFactoryAware接口替代完整的ConfigurableBeanFactory依赖项.
 */
public interface EmbeddedValueResolverAware extends Aware {

	/**
	 * 设置用于解析嵌套的定义值的StringValueResolver.
	 */
	void setEmbeddedValueResolver(StringValueResolver resolver);

}
