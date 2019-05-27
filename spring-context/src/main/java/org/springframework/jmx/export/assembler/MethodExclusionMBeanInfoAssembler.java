package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler}子类, 允许将方法名称显式排除为MBean操作和属性.
 *
 * <p>未明确排除在管理接口之外的方法都将向JMX公开.
 * JavaBean getter和setter将自动公开为JMX属性.
 *
 * <p>可以通过{@code ignoredMethods}属性提供一组方法名称.
 * 如果您有多个bean, 并希望每个bean使用一组不同的方法名称,
 * 然后你可以使用{@code ignoredMethodMappings}属性将bean键(这是用于将bean传递给{@code MBeanExporter}的名称)映射到方法名称列表.
 *
 * <p>如果为{@code ignoredMethodMappings}和{@code ignoredMethods}指定值, Spring将首先尝试在映射中查找方法名称.
 * 如果找不到bean的方法名称, 它将使用{@code ignoredMethods}定义的方法名称.
 */
public class MethodExclusionMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	private Set<String> ignoredMethods;

	private Map<String, Set<String>> ignoredMethodMappings;


	/**
	 * 在创建管理信息时, 设置要忽略的方法名称.
	 * <p>如果在{@code ignoredMethodsMappings}属性中找不到与该bean对应的条目, 则这些方法名称将用于bean.
	 */
	public void setIgnoredMethods(String... ignoredMethodNames) {
		this.ignoredMethods = new HashSet<String>(Arrays.asList(ignoredMethodNames));
	}

	/**
	 * 将bean键的映射设置为以逗号分隔的方法名称列表.
	 * <p>创建管理接口时将忽略这些方法名称.
	 * <p>属性键必须与bean键匹配, 并且属性值必须与方法名称列表匹配.
	 * 在为bean查找忽略的方法名时, Spring将首先检查这些映射.
	 */
	public void setIgnoredMethodMappings(Properties mappings) {
		this.ignoredMethodMappings = new HashMap<String, Set<String>>();
		for (Enumeration<?> en = mappings.keys(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			this.ignoredMethodMappings.put(beanKey, new HashSet<String>(Arrays.asList(methodNames)));
		}
	}


	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	/**
	 * 确定是否应包含给定方法, 即未配置为忽略.
	 * 
	 * @param method 操作方法
	 * @param beanKey 与{@code MBeanExporter}的bean映射中的MBean关联的键
	 */
	protected boolean isNotIgnored(Method method, String beanKey) {
		if (this.ignoredMethodMappings != null) {
			Set<String> methodNames = this.ignoredMethodMappings.get(beanKey);
			if (methodNames != null) {
				return !methodNames.contains(method.getName());
			}
		}
		if (this.ignoredMethods != null) {
			return !this.ignoredMethods.contains(method.getName());
		}
		return true;
	}

}
