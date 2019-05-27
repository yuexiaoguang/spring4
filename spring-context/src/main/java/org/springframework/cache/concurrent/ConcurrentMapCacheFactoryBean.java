package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

/**
 * 在Spring容器中使用时可以轻松配置{@link ConcurrentMapCache}.
 * 可以通过bean属性配置; 使用指定的Spring bean名称作为默认缓存名称.
 *
 * <p>用于测试或简单缓存场景, 通常与 {@link org.springframework.cache.support.SimpleCacheManager}结合使用,
 * 或通过 {@link ConcurrentMapCacheManager}动态实现.
 */
public class ConcurrentMapCacheFactoryBean
		implements FactoryBean<ConcurrentMapCache>, BeanNameAware, InitializingBean {

	private String name = "";

	private ConcurrentMap<Object, Object> store;

	private boolean allowNullValues = true;

	private ConcurrentMapCache cache;


	/**
	 * 指定缓存的名称.
	 * <p>默认是 "" (empty String).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定用作内部存储的ConcurrentMap(可能已预先填充).
	 * <p>默认是标准的 {@link java.util.concurrent.ConcurrentHashMap}.
	 */
	public void setStore(ConcurrentMap<Object, Object> store) {
		this.store = store;
	}

	/**
	 * 设置是否允许{@code null}值 (使它们适应内部null持有者值).
	 * <p>默认是 "true".
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	@Override
	public void setBeanName(String beanName) {
		if (!StringUtils.hasLength(this.name)) {
			setName(beanName);
		}
	}

	@Override
	public void afterPropertiesSet() {
		this.cache = (this.store != null ? new ConcurrentMapCache(this.name, this.store, this.allowNullValues) :
				new ConcurrentMapCache(this.name, this.allowNullValues));
	}


	@Override
	public ConcurrentMapCache getObject() {
		return this.cache;
	}

	@Override
	public Class<?> getObjectType() {
		return ConcurrentMapCache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
