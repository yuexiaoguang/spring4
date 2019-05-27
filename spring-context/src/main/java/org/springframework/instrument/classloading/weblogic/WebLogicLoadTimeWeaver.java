package org.springframework.instrument.classloading.weblogic;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * WebLogic的可检测ClassLoader的{@link LoadTimeWeaver}实现.
 *
 * <p><b>NOTE:</b> 需要BEA WebLogic 10或更高版本.
 */
public class WebLogicLoadTimeWeaver implements LoadTimeWeaver {

	private final WebLogicClassLoaderAdapter classLoader;


	/**
	 * 使用默认的{@link ClassLoader class loader}.
	 */
	public WebLogicLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 要委托给用于织入的{@code ClassLoader} (must not be {@code null})
	 */
	public WebLogicLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = new WebLogicClassLoaderAdapter(classLoader);
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader.getClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new OverridingClassLoader(this.classLoader.getClassLoader(),
				this.classLoader.getThrowawayClassLoader());
	}

}
