package org.springframework.context;

import java.util.EventListener;

/**
 * 应用程序事件监听器实现的接口.
 * 基于Observer设计模式的标准{@code java.util.EventListener}接口.
 *
 * <p>从Spring 3.0开始, ApplicationListener一般可以声明它感兴趣的事件类型.
 * 使用Spring ApplicationContext注册时, 将相应地过滤事件, 仅调用侦听器以匹配事件对象.
 */
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * 处理应用程序事件.
	 * 
	 * @param event 要响应的事件
	 */
	void onApplicationEvent(E event);

}
