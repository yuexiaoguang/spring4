package org.springframework.core;

/**
 * 由重新加载识别的ClassLoader (e.g. 基于Groovy的ClassLoader)实现的接口.
 * 例如，由Spring的CGLIB代理工厂检测以进行缓存决策.
 *
 * <p>如果ClassLoader <i>不</i>实现此接口, 那么从中获取的所有类都应被视为不可重新加载 (i.e. 可缓存).
 */
public interface SmartClassLoader {

	/**
	 * 确定给定的类是否可重载 (在此ClassLoader中).
	 * <p>通常用于检查结果是否可以缓存 (对于此ClassLoader), 或是否应每次重新获取结果.
	 * 
	 * @param clazz 要检查的类 (通常从此ClassLoader加载)
	 * 
	 * @return 是否应该期望该类出现在重新加载的版本中 (使用不同的{@code Class}对象)
	 */
	boolean isClassReloadable(Class<?> clazz);

}
