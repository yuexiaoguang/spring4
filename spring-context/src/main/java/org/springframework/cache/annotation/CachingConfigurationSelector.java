package org.springframework.cache.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 根据导入{@code @Configuration}类中{@link EnableCaching#mode}的值,
 * 选择应使用的{@link AbstractCachingConfiguration}的实现.
 *
 * <p>检测JSR-107的存在并相应地启用JCache支持.
 */
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

	private static final String PROXY_JCACHE_CONFIGURATION_CLASS =
			"org.springframework.cache.jcache.config.ProxyJCacheConfiguration";

	private static final String CACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJCachingConfiguration";

	private static final String JCACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJJCacheConfiguration";


	private static final boolean jsr107Present = ClassUtils.isPresent(
			"javax.cache.Cache", CachingConfigurationSelector.class.getClassLoader());

	private static final boolean jcacheImplPresent = ClassUtils.isPresent(
			PROXY_JCACHE_CONFIGURATION_CLASS, CachingConfigurationSelector.class.getClassLoader());


	/**
	 * 分别为{@link EnableCaching#mode()}的{@code PROXY}和{@code ASPECTJ}的值
	 * 返回{@link ProxyCachingConfiguration}或{@code AspectJCachingConfiguration}.
	 * 可能还包括相应的JCache配置.
	 */
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return getProxyImports();
			case ASPECTJ:
				return getAspectJImports();
			default:
				return null;
		}
	}

	/**
	 * 如果{@link AdviceMode}设置为{@link AdviceMode#PROXY}, 则返回要使用的导入.
	 * <p>如果可用, 请注意添加必要的JSR-107导入.
	 */
	private String[] getProxyImports() {
		List<String> result = new ArrayList<String>(3);
		result.add(AutoProxyRegistrar.class.getName());
		result.add(ProxyCachingConfiguration.class.getName());
		if (jsr107Present && jcacheImplPresent) {
			result.add(PROXY_JCACHE_CONFIGURATION_CLASS);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 如果{@link AdviceMode}设置为 {@link AdviceMode#ASPECTJ}, 则返回要使用的导入.
	 * <p>如果可用, 请注意添加必要的JSR-107导入.
	 */
	private String[] getAspectJImports() {
		List<String> result = new ArrayList<String>(2);
		result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		if (jsr107Present && jcacheImplPresent) {
			result.add(JCACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		}
		return StringUtils.toStringArray(result);
	}

}
