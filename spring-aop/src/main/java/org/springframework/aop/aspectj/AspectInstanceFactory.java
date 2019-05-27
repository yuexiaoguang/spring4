package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * 实现此接口是为了提供AspectJ切面的实例. 与Spring的bean工厂分离.
 *
 * <p>扩展 {@link org.springframework.core.Ordered}接口表示链中底层切面的顺序值.
 */
public interface AspectInstanceFactory extends Ordered {

	/**
	 * 创建此工厂切面的实例.
	 * 
	 * @return 切面实例 (永远不会是 {@code null})
	 */
	Object getAspectInstance();

	/**
	 * 公开此工厂使用的切面类加载器.
	 * 
	 * @return 切面类加载器 (永远不会是 {@code null})
	 */
	ClassLoader getAspectClassLoader();

}
