package org.springframework.orm.jpa.persistenceunit;

import javax.persistence.spi.ClassTransformer;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.util.Assert;

/**
 * {@link MutablePersistenceUnitInfo}的子类,
 * 它基于Spring的{@link org.springframework.instrument.classloading.LoadTimeWeaver}抽象添加了检测钩子.
 *
 * <p>与其超类相比, 此类仅限于包可见.
 */
class SpringPersistenceUnitInfo extends MutablePersistenceUnitInfo {

	private LoadTimeWeaver loadTimeWeaver;

	private ClassLoader classLoader;


	/**
	 * 使用Spring使用的LoadTimeWeaver SPI接口初始化此PersistenceUnitInfo, 以将检测添加到当前类加载器.
	 */
	public void init(LoadTimeWeaver loadTimeWeaver) {
		Assert.notNull(loadTimeWeaver, "LoadTimeWeaver must not be null");
		this.loadTimeWeaver = loadTimeWeaver;
		this.classLoader = loadTimeWeaver.getInstrumentableClassLoader();
	}

	/**
	 * 使用当前的类加载器(而不是使用 LoadTimeWeaver)初始化此PersistenceUnitInfo.
	 */
	public void init(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
	}


	/**
	 * 如果指定, 此实现将返回LoadTimeWeaver的可检测ClassLoader.
	 */
	@Override
	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * 如果指定, 此实现将委托给LoadTimeWeaver.
	 */
	@Override
	public void addTransformer(ClassTransformer classTransformer) {
		if (this.loadTimeWeaver == null) {
			throw new IllegalStateException("Cannot apply class transformer without LoadTimeWeaver specified");
		}
		this.loadTimeWeaver.addTransformer(new ClassFileTransformerAdapter(classTransformer));
	}

	/**
	 * 如果指定, 此实现将委托给LoadTimeWeaver.
	 */
	@Override
	public ClassLoader getNewTempClassLoader() {
		ClassLoader tcl = (this.loadTimeWeaver != null ? this.loadTimeWeaver.getThrowawayClassLoader() :
				new SimpleThrowawayClassLoader(this.classLoader));
		String packageToExclude = getPersistenceProviderPackageName();
		if (packageToExclude != null && tcl instanceof DecoratingClassLoader) {
			((DecoratingClassLoader) tcl).excludePackage(packageToExclude);
		}
		return tcl;
	}

}
