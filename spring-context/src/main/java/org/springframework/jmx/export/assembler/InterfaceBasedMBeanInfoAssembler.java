package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler}的子类, 允许使用任意接口定义bean的管理接口.
 * 在这些接口中定义的任何方法或属性都作为MBean操作和属性公开.
 *
 * <p>默认情况下, 此类根据bean类实现的接口对每个操作或属性的包含进行投票.
 * 但是, 您可以通过{@code managedInterfaces}属性提供一组接口.
 * 如果你有多个bean, 并希望每个bean使用一组不同的接口,
 * 然后你可以使用{@code interfaceMappings}属性将bean键(这是用于将bean传递给{@code MBeanExporter}的名称) 映射到接口名称列表.
 *
 * <p>如果为{@code interfaceMappings}和{@code managedInterfaces}指定值, Spring将首先尝试在映射中查找接口.
 * 如果没有找到bean的接口, 它将使用{@code managedInterfaces}定义的接口.
 */
public class InterfaceBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler
		implements BeanClassLoaderAware, InitializingBean {

	private Class<?>[] managedInterfaces;

	/** 将bean键映射到类数组 */
	private Properties interfaceMappings;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** 将bean键映射到类数组 */
	private Map<String, Class<?>[]> resolvedInterfaceMappings;


	/**
	 * 设置用于创建管理信息的接口.
	 * 如果在{@code interfaceMappings}属性中找不到与该bean对应的条目, 则这些接口将用于bean.
	 * 
	 * @param managedInterfaces 要使用的接口. 每个条目必须是一个接口.
	 */
	public void setManagedInterfaces(Class<?>... managedInterfaces) {
		if (managedInterfaces != null) {
			for (Class<?> ifc : managedInterfaces) {
				if (!ifc.isInterface()) {
					throw new IllegalArgumentException(
							"Management interface [" + ifc.getName() + "] is not an interface");
				}
			}
		}
		this.managedInterfaces = managedInterfaces;
	}

	/**
	 * 将bean键的映射设置为以逗号分隔的接口名称列表.
	 * <p>属性键应与bean键匹配, 属性值应与接口名称列表匹配.
	 * 在搜索bean的接口时, Spring将首先检查这些映射.
	 * 
	 * @param mappings bean键到接口名称的映射
	 */
	public void setInterfaceMappings(Properties mappings) {
		this.interfaceMappings = mappings;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.interfaceMappings != null) {
			this.resolvedInterfaceMappings = resolveInterfaceMappings(this.interfaceMappings);
		}
	}

	/**
	 * 解析给定的接口映射, 将类名转换为Class对象.
	 * 
	 * @param mappings 指定的接口映射
	 * 
	 * @return 已解析的接口映射 (将Class对象作为值)
	 */
	private Map<String, Class<?>[]> resolveInterfaceMappings(Properties mappings) {
		Map<String, Class<?>[]> resolvedMappings = new HashMap<String, Class<?>[]>(mappings.size());
		for (Enumeration<?> en = mappings.propertyNames(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] classNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			Class<?>[] classes = resolveClassNames(classNames, beanKey);
			resolvedMappings.put(beanKey, classes);
		}
		return resolvedMappings;
	}

	/**
	 * 将给定的类名解析为Class对象.
	 * 
	 * @param classNames 要解析的类名
	 * @param beanKey 类名与之关联的bean键
	 * 
	 * @return 已解析的Class
	 */
	private Class<?>[] resolveClassNames(String[] classNames, String beanKey) {
		Class<?>[] classes = new Class<?>[classNames.length];
		for (int x = 0; x < classes.length; x++) {
			Class<?> cls = ClassUtils.resolveClassName(classNames[x].trim(), this.beanClassLoader);
			if (!cls.isInterface()) {
				throw new IllegalArgumentException(
						"Class [" + classNames[x] + "] mapped to bean key [" + beanKey + "] is no interface");
			}
			classes[x] = cls;
		}
		return classes;
	}


	/**
	 * 检查是否在其中一个已配置的接口中声明了{@code Method}并且它是 public的.
	 * 
	 * @param method {@code Method}访问器.
	 * @param beanKey 与{@code beans} {@code Map}中的MBean关联的Key.
	 * 
	 * @return {@code true} 如果在其中一个配置的接口中声明了{@code Method}, 否则{@code false}.
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查是否在其中一个已配置的接口中声明了{@code Method}并且它是 public的.
	 * 
	 * @param method the mutator {@code Method}.
	 * @param beanKey 与{@code beans} {@code Map}中的MBean关联的Key.
	 * 
	 * @return {@code true} 如果在其中一个配置的接口中声明了{@code Method}, 否则{@code false}.
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查是否在其中一个已配置的接口中声明了{@code Method}并且它是 public的.
	 * 
	 * @param method the operation {@code Method}.
	 * @param beanKey 与{@code beans} {@code Map}中的MBean关联的Key.
	 * 
	 * @return {@code true} 如果在其中一个配置的接口中声明了{@code Method}, 否则{@code false}.
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查{@code Method}是否是public, 并在其中一个配置的接口中声明.
	 * 
	 * @param method 要检查的{@code Method}
	 * @param beanKey 与bean Map中的MBean关联的键
	 * 
	 * @return {@code true} 如果{@code Method}在其中一个配置的接口中声明并且是public的, 否则{@code false}.
	 */
	private boolean isPublicInInterface(Method method, String beanKey) {
		return ((method.getModifiers() & Modifier.PUBLIC) > 0) && isDeclaredInInterface(method, beanKey);
	}

	/**
	 * 检查给定方法是否在给定bean的托管接口中声明.
	 */
	private boolean isDeclaredInInterface(Method method, String beanKey) {
		Class<?>[] ifaces = null;

		if (this.resolvedInterfaceMappings != null) {
			ifaces = this.resolvedInterfaceMappings.get(beanKey);
		}

		if (ifaces == null) {
			ifaces = this.managedInterfaces;
			if (ifaces == null) {
				ifaces = ClassUtils.getAllInterfacesForClass(method.getDeclaringClass());
			}
		}

		for (Class<?> ifc : ifaces) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (ifcMethod.getName().equals(method.getName()) &&
						Arrays.equals(ifcMethod.getParameterTypes(), method.getParameterTypes())) {
					return true;
				}
			}
		}

		return false;
	}

}
