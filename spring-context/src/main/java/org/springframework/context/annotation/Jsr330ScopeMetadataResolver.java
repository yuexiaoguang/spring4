package org.springframework.context.annotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * 遵循JSR-330作用域规则的简单{@link ScopeMetadataResolver}实现:
 * 除非{@link javax.inject.Singleton}存在, 否则默认为原型作用域.
 *
 * <p>此作用域解析器可与{@link ClassPathBeanDefinitionScanner}
 * 和{@link AnnotatedBeanDefinitionReader}一起使用, 以实现标准JSR-330合规性.
 * 但是, 实际上, 通常会使用Spring的丰富默认作用域 - 或者使用指向扩展Spring作用域的自定义作用域注解来扩展此解析器.
 */
public class Jsr330ScopeMetadataResolver implements ScopeMetadataResolver {

	private final Map<String, String> scopeMap = new HashMap<String, String>();


	public Jsr330ScopeMetadataResolver() {
		registerScope("javax.inject.Singleton", BeanDefinition.SCOPE_SINGLETON);
	}


	/**
	 * 注册扩展的JSR-330作用域注解, 按名称将其映射到特定的Spring作用域.
	 * 
	 * @param annotationType JSR-330注解类型
	 * @param scopeName Spring作用域名称
	 */
	public final void registerScope(Class<?> annotationType, String scopeName) {
		this.scopeMap.put(annotationType.getName(), scopeName);
	}

	/**
	 * 注册扩展的JSR-330作用域注解, 按名称将其映射到特定的Spring作用域.
	 * 
	 * @param annotationType JSR-330注解类型
	 * @param scopeName Spring作用域名称
	 */
	public final void registerScope(String annotationType, String scopeName) {
		this.scopeMap.put(annotationType, scopeName);
	}

	/**
	 * 将给定的注解类型解析为命名的Spring作用域.
	 * <p>默认实现只是检查已注册的作用域.
	 * 可以覆盖自定义映射规则, e.g. 命名约定.
	 * 
	 * @param annotationType JSR-330注解类型
	 * 
	 * @return Spring作用域名称
	 */
	protected String resolveScopeName(String annotationType) {
		return this.scopeMap.get(annotationType);
	}


	@Override
	public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {
		ScopeMetadata metadata = new ScopeMetadata();
		metadata.setScopeName(BeanDefinition.SCOPE_PROTOTYPE);
		if (definition instanceof AnnotatedBeanDefinition) {
			AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;
			Set<String> annTypes = annDef.getMetadata().getAnnotationTypes();
			String found = null;
			for (String annType : annTypes) {
				Set<String> metaAnns = annDef.getMetadata().getMetaAnnotationTypes(annType);
				if (metaAnns.contains("javax.inject.Scope")) {
					if (found != null) {
						throw new IllegalStateException("Found ambiguous scope annotations on bean class [" +
								definition.getBeanClassName() + "]: " + found + ", " + annType);
					}
					found = annType;
					String scopeName = resolveScopeName(annType);
					if (scopeName == null) {
						throw new IllegalStateException(
								"Unsupported scope annotation - not mapped onto Spring scope name: " + annType);
					}
					metadata.setScopeName(scopeName);
				}
			}
		}
		return metadata;
	}

}
