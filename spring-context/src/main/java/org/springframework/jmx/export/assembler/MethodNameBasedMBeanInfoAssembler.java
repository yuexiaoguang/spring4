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
 * {@code AbstractReflectiveMBeanInfoAssembler}的子类, 允许指定要作为MBean操作和属性公开的方法名称.
 * JavaBean getter和setter将自动公开为JMX属性.
 *
 * <p>可以通过{@code managedMethods}属性提供一组方法名称.
 * 如果您有多个bean, 并希望每个bean使用一组不同的方法名称,
 * 然后你可以使用{@code methodMappings}属性将bean键 (这是用于将bean传递给{@code MBeanExporter}的名称) 映射到方法名称列表.
 *
 * <p>如果为{@code methodMappings}和{@code managedMethods}指定值, Spring将首先尝试在映射中查找方法名称.
 * 如果找不到bean的方法名称, 它将使用{@code managedMethods}定义的方法名称.
 */
public class MethodNameBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	/**
	 * 存储用于创建管理接口的方法名称.
	 */
	private Set<String> managedMethods;

	/**
	 * 将bean键的映射存储到方法名称数组中.
	 */
	private Map<String, Set<String>> methodMappings;


	/**
	 * 设置用于创建管理信息的方法名称数组.
	 * 如果在{@code methodMappings}属性中找不到与该bean对应的条目, 则这些方法名称将用于bean.
	 * 
	 * @param methodNames 一组方法名称, 指示要使用的方法
	 */
	public void setManagedMethods(String... methodNames) {
		this.managedMethods = new HashSet<String>(Arrays.asList(methodNames));
	}

	/**
	 * 将bean键的映射设置为以逗号分隔的方法名称列表.
	 * 属性键应与bean键匹配, 属性值应与方法名称列表匹配.
	 * 在搜索bean的方法名称时, Spring将首先检查这些映射.
	 * 
	 * @param mappings bean键到方法名称的映射
	 */
	public void setMethodMappings(Properties mappings) {
		this.methodMappings = new HashMap<String, Set<String>>();
		for (Enumeration<?> en = mappings.keys(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			this.methodMappings.put(beanKey, new HashSet<String>(Arrays.asList(methodNames)));
		}
	}


	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	protected boolean isMatch(Method method, String beanKey) {
		if (this.methodMappings != null) {
			Set<String> methodNames = this.methodMappings.get(beanKey);
			if (methodNames != null) {
				return methodNames.contains(method.getName());
			}
		}
		return (this.managedMethods != null && this.managedMethods.contains(method.getName()));
	}

}
