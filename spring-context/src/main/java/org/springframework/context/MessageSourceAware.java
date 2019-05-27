package org.springframework.context;

import org.springframework.beans.factory.Aware;

/**
 * 希望被通知其运行的MessageSource (通常是 ApplicationContext) 的对象实现的接口.
 *
 * <p>请注意, MessageSource通常也可以作为bean引用传递 (到任意bean属性或构造函数参数),
 * 因为它在应用程序上下文中定义了名称为 "messageSource"的bean.
 */
public interface MessageSourceAware extends Aware {

	/**
	 * 设置此对象运行的MessageSource.
	 * <p>在普通bean属性填充之后, 但在初始化回调之前调用, 例如InitializingBean的afterPropertiesSet或自定义init方法.
	 * 在ApplicationContextAware的setApplicationContext之前调用.
	 * 
	 * @param messageSource 由此对象使用的消息源
	 */
	void setMessageSource(MessageSource messageSource);

}
