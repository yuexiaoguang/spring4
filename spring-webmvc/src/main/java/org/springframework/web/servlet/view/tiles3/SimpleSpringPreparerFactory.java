package org.springframework.web.servlet.view.tiles3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.PreparerException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.NoSuchPreparerException;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory}实现,
 * 它需要preparer类名并为这些类名构建preparer实例, 通过Spring ApplicationContext创建它们,
 * 以便应用Spring容器回调和配置的Spring BeanPostProcessors.
 */
public class SimpleSpringPreparerFactory extends AbstractSpringPreparerFactory {

	/** 共享的ViewPreparer实例的缓存: bean name -> bean instance */
	private final Map<String, ViewPreparer> sharedPreparers = new ConcurrentHashMap<String, ViewPreparer>(16);


	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		// 首先快速检查并发映射, 最小锁定.
		ViewPreparer preparer = this.sharedPreparers.get(name);
		if (preparer == null) {
			synchronized (this.sharedPreparers) {
				preparer = this.sharedPreparers.get(name);
				if (preparer == null) {
					try {
						Class<?> beanClass = context.getClassLoader().loadClass(name);
						if (!ViewPreparer.class.isAssignableFrom(beanClass)) {
							throw new PreparerException(
									"Invalid preparer class [" + name + "]: does not implement ViewPreparer interface");
						}
						preparer = (ViewPreparer) context.getAutowireCapableBeanFactory().createBean(beanClass);
						this.sharedPreparers.put(name, preparer);
					}
					catch (ClassNotFoundException ex) {
						throw new NoSuchPreparerException("Preparer class [" + name + "] not found", ex);
					}
				}
			}
		}
		return preparer;
	}

}
