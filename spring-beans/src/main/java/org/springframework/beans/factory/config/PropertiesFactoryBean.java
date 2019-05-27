package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PropertiesLoaderSupport;

/**
 * 允许从类路径位置创建属性文件作为Bean工厂中的Properties实例.
 * 可用于通过bean引用填充Properties类型的任何bean属性.
 *
 * <p>支持从属性文件加载和/或在此FactoryBean上设置本地属性.
 * 创建的Properties实例将合并加载值和本地值.
 * 如果既未设置位置也未设置本地属性, 则初始化时将引发异常.
 *
 * <p>可以在每个请求上创建单例或新对象. 默认是单例.
 */
public class PropertiesFactoryBean extends PropertiesLoaderSupport
		implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	private Properties singletonInstance;


	/**
	 * 设置是否应创建共享的“单例”属性实例, 或者是每个请求上新的属性实例.
	 * <p>Default is "true" (共享的单例).
	 */
	public final void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public final boolean isSingleton() {
		return this.singleton;
	}


	@Override
	public final void afterPropertiesSet() throws IOException {
		if (this.singleton) {
			this.singletonInstance = createProperties();
		}
	}

	@Override
	public final Properties getObject() throws IOException {
		if (this.singleton) {
			return this.singletonInstance;
		}
		else {
			return createProperties();
		}
	}

	@Override
	public Class<Properties> getObjectType() {
		return Properties.class;
	}


	/**
	 * 子类可以重写的模板方法, 用于构造此工厂返回的对象.
	 * 默认实现返回合并的Properties实例.
	 * <p>在共享单例的情况下, 在初始化此FactoryBean时调用; 否则在 {@link #getObject()}时调用.
	 * 
	 * @return 该工厂返回的对象
	 * @throws IOException 如果在属性加载期间发生异常
	 */
	protected Properties createProperties() throws IOException {
		return mergeProperties();
	}

}
