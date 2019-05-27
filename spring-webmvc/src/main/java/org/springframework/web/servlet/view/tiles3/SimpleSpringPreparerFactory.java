package org.springframework.web.servlet.view.tiles3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.PreparerException;
import org.apache.tiles.preparer.ViewPreparer;
import org.apache.tiles.preparer.factory.NoSuchPreparerException;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory} implementation
 * that expects preparer class names and builds preparer instances for those,
 * creating them through the Spring ApplicationContext in order to apply
 * Spring container callbacks and configured Spring BeanPostProcessors.
 */
public class SimpleSpringPreparerFactory extends AbstractSpringPreparerFactory {

	/** Cache of shared ViewPreparer instances: bean name -> bean instance */
	private final Map<String, ViewPreparer> sharedPreparers = new ConcurrentHashMap<String, ViewPreparer>(16);


	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		// Quick check on the concurrent map first, with minimal locking.
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
