package org.springframework.context.annotation;

import java.beans.Introspector;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.support.BeanNameGenerator}实现,
 * 用于带有{@link org.springframework.stereotype.Component @Component}注解的bean类,
 * 或带有使用{@link org.springframework.stereotype.Component @Component}作为元注解的注解的bean类.
 * 例如, Spring的构造型注解 (例如{@link org.springframework.stereotype.Repository @Repository})
 * 本身带有{@link org.springframework.stereotype.Component @Component}注解.
 *
 * <p>还支持Java EE 6的{@link javax.annotation.ManagedBean}和JSR-330的{@link javax.inject.Named}注解.
 * 请注意, Spring组件注解始终覆盖此类标准注解.
 *
 * <p>如果注解的值不指示bean名称, 则将根据类的短名称构建适当的名称 (第一个字母是小写的). 例如:
 *
 * <pre class="code">com.xyz.FooServiceImpl -&gt; fooServiceImpl</pre>
 */
public class AnnotationBeanNameGenerator implements BeanNameGenerator {

	private static final String COMPONENT_ANNOTATION_CLASSNAME = "org.springframework.stereotype.Component";


	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		if (definition instanceof AnnotatedBeanDefinition) {
			String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
			if (StringUtils.hasText(beanName)) {
				// 找到的显式bean名称.
				return beanName;
			}
		}
		// Fallback: 生成唯一的默认bean名称.
		return buildDefaultBeanName(definition, registry);
	}

	/**
	 * 从类中的一个注解派生bean名称.
	 * 
	 * @param annotatedDef 注解感知bean定义
	 * 
	 * @return bean名称, 或{@code null}
	 */
	protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
		AnnotationMetadata amd = annotatedDef.getMetadata();
		Set<String> types = amd.getAnnotationTypes();
		String beanName = null;
		for (String type : types) {
			AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
			if (isStereotypeWithNameValue(type, amd.getMetaAnnotationTypes(type), attributes)) {
				Object value = attributes.get("value");
				if (value instanceof String) {
					String strVal = (String) value;
					if (StringUtils.hasLength(strVal)) {
						if (beanName != null && !strVal.equals(beanName)) {
							throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
									"component names: '" + beanName + "' versus '" + strVal + "'");
						}
						beanName = strVal;
					}
				}
			}
		}
		return beanName;
	}

	/**
	 * 检查给定的注解是否是构造型, 允许通过其注解{@code value()}建议组件名称.
	 * 
	 * @param annotationType 要检查的注解类的名称
	 * @param metaAnnotationTypes 给定注解上的元注解的名称
	 * @param attributes 给定注解的属性Map
	 * 
	 * @return 注解是否有资格作为具有组件名称的构造型
	 */
	protected boolean isStereotypeWithNameValue(String annotationType,
			Set<String> metaAnnotationTypes, Map<String, Object> attributes) {

		boolean isStereotype = annotationType.equals(COMPONENT_ANNOTATION_CLASSNAME) ||
				(metaAnnotationTypes != null && metaAnnotationTypes.contains(COMPONENT_ANNOTATION_CLASSNAME)) ||
				annotationType.equals("javax.annotation.ManagedBean") ||
				annotationType.equals("javax.inject.Named");

		return (isStereotype && attributes != null && attributes.containsKey("value"));
	}

	/**
	 * 从给定的bean定义派生默认bean名称.
	 * <p>默认实现委托给{@link #buildDefaultBeanName(BeanDefinition)}.
	 * 
	 * @param definition 用于构建bean名称的bean定义
	 * @param registry 注册给定bean定义的注册表
	 * 
	 * @return 默认bean名称 (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return buildDefaultBeanName(definition);
	}

	/**
	 * 从给定的bean定义派生默认bean名称.
	 * <p>默认实现只是构建短类名的decapitalized版本: e.g. "mypackage.MyJdbcDao" -> "myJdbcDao".
	 * <p>请注意, 内部类将具有 "outerClassName.InnerClassName"形式的名称, 如果按名称自动装配, 则由于名称中的句点可能会出现问题.
	 * 
	 * @param definition 用于构建bean名称的bean定义
	 * 
	 * @return 默认bean名称 (never {@code null})
	 */
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String shortClassName = ClassUtils.getShortName(definition.getBeanClassName());
		return Introspector.decapitalize(shortClassName);
	}

}
