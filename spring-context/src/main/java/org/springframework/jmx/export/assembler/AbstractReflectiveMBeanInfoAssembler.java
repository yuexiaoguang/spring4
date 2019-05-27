package org.springframework.jmx.export.assembler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.jmx.support.JmxUtils;

/**
 * 构建在{@link AbstractMBeanInfoAssembler}超类上, 根据MBean类的反射元数据添加构建元数据的基本算法.
 *
 * <p>从反射元数据创建MBean元数据的逻辑包含在此类中, 但此类不会决定要公开哪些方法和属性.
 * 相反, 它通过{@code includeXXX}方法为子类提供了对每个属性或方法进行“投票”的机会.
 *
 * <p>通过{@code populateXXXDescriptor}方法组装元数据后, 子类还有机会使用其他描述符填充属性和操作元数据.
 */
public abstract class AbstractReflectiveMBeanInfoAssembler extends AbstractMBeanInfoAssembler {

	/**
	 * 标识JMX {@link Descriptor}中的getter方法.
	 */
	protected static final String FIELD_GET_METHOD = "getMethod";

	/**
	 * 标识JMX {@link Descriptor}中的setter方法.
	 */
	protected static final String FIELD_SET_METHOD = "setMethod";

	/**
	 * JMX {@link Descriptor}中角色字段的常量标识符.
	 */
	protected static final String FIELD_ROLE = "role";

	/**
	 * JMX {@link Descriptor}中的getter角色字段值的常量标识符.
	 */
	protected static final String ROLE_GETTER = "getter";

	/**
	 * JMX {@link Descriptor}中的setter角色字段值的常量标识符.
	 */
	protected static final String ROLE_SETTER = "setter";

	/**
	 * 标识JMX {@link Descriptor}中的操作(方法).
	 */
	protected static final String ROLE_OPERATION = "operation";

	/**
	 * JMX {@link Descriptor}中可见性字段的常量标识符.
	 */
	protected static final String FIELD_VISIBILITY = "visibility";

	/**
	 * 最低可见性, 用于与属性的访问器或变换器对应的操作.
	 */
	protected static final int ATTRIBUTE_OPERATION_VISIBILITY = 4;

	/**
	 * JMX {@link Descriptor}中的类字段的常量标识符.
	 */
	protected static final String FIELD_CLASS = "class";
	/**
	 * JMX {@link Descriptor}中日志字段的常量标识符.
	 */
	protected static final String FIELD_LOG = "log";

	/**
	 * JMX {@link Descriptor}中日志文件字段的常量标识符.
	 */
	protected static final String FIELD_LOG_FILE = "logFile";

	/**
	 * JMX {@link Descriptor}中的货币时间限制字段的常量标识符.
	 */
	protected static final String FIELD_CURRENCY_TIME_LIMIT = "currencyTimeLimit";

	/**
	 * JMX {@link Descriptor}中的默认字段的常量标识符.
	 */
	protected static final String FIELD_DEFAULT = "default";

	/**
	 * JMX {@link Descriptor}的persistPolicy字段的常量标识符.
	 */
	protected static final String FIELD_PERSIST_POLICY = "persistPolicy";

	/**
	 * JMX {@link Descriptor}中的persistPeriod字段的常量标识符.
	 */
	protected static final String FIELD_PERSIST_PERIOD = "persistPeriod";

	/**
	 * JMX {@link Descriptor}中的persistLocation字段的常量标识符.
	 */
	protected static final String FIELD_PERSIST_LOCATION = "persistLocation";

	/**
	 * JMX {@link Descriptor}中的persistName字段的常量标识符.
	 */
	protected static final String FIELD_PERSIST_NAME = "persistName";

	/**
	 * JMX {@link Descriptor}中的displayName字段的常量标识符.
	 */
	protected static final String FIELD_DISPLAY_NAME = "displayName";

	/**
	 * JMX {@link Descriptor}中的units字段的常量标识符.
	 */
	protected static final String FIELD_UNITS = "units";

	/**
	 * JMX {@link Descriptor}中的metricType字段的常量标识符.
	 */
	protected static final String FIELD_METRIC_TYPE = "metricType";

	/**
	 * JMX {@link Descriptor}中的自定义metricCategory字段的常量标识符.
	 */
	protected static final String FIELD_METRIC_CATEGORY = "metricCategory";


	/**
	 * JMX 字段 "currencyTimeLimit"的默认值.
	 */
	private Integer defaultCurrencyTimeLimit;

	/**
	 * 指示是否将严格模式用于属性.
	 */
	private boolean useStrictCasing = true;

	private boolean exposeClassDescriptor = false;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	/**
	 * 设置JMX字段"currencyTimeLimit"的默认值.
	 * 默认值通常表示从不缓存属性值.
	 * <p>默认无, 未显式设置该字段, 按JMX 1.2规范的建议.
	 * 这应该导致 "永不缓存"行为, 总是重新读取属性值 (对应于JMX 1.2中{@code -1}的"currencyTimeLimit").
	 * <p>但是, 一些JMX实现 (在这方面不遵循JMX 1.2规范) 可能需要在此处设置显式值, 以获得 "永不缓存"行为: 例如, JBoss 3.2.x.
	 * <p>请注意, 还可以在托管属性或操作上指定"currencyTimeLimit"值.
	 * 如果未使用"currencyTimeLimit"值 {@code >= 0}覆盖, 将应用默认值:
	 * 元数据"currencyTimeLimit"的{@code -1}值表示使用默认值;
	 * 值{@code 0}表示"始终缓存", 并将转换为{@code Integer.MAX_VALUE};
	 * 正值表示缓存秒数.
	 */
	public void setDefaultCurrencyTimeLimit(Integer defaultCurrencyTimeLimit) {
		this.defaultCurrencyTimeLimit = defaultCurrencyTimeLimit;
	}

	/**
	 * 返回JMX字段"currencyTimeLimit"的默认值.
	 */
	protected Integer getDefaultCurrencyTimeLimit() {
		return this.defaultCurrencyTimeLimit;
	}

	/**
	 * 设置是否对属性使用严格的大小写. 默认启用.
	 * <p>使用严格的大小写时, 带有getter的JavaBean属性 {@code getFoo()} 会转换为名为{@code Foo}的属性.
	 * 禁用严格的大小写后, {@code getFoo()}将转换为{@code foo}.
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * 返回是否启用了属性的严格的大小写.
	 */
	protected boolean isUseStrictCasing() {
		return this.useStrictCasing;
	}

	/**
	 * 设置是否为管理的操作公开JMX描述符字段 "class".
	 * 默认"false", 让JMX实现通过反射确定实际的类.
	 * <p>对于需要指定"class"字段的JMX实现, 将此属性设置为{@code true}, 例如WebLogic的.
	 * 在这种情况下, 如果是普通bean实例或CGLIB代理, Spring将在那里公开目标类名.
	 * 遇到JDK动态代理时, 将指定代理实现的第一个接口.
	 * <p><b>WARNING:</b> 通过JMX公开JDK动态代理时检查代理定义, 特别是将此属性转换为{@code true}:
	 * 在这种情况下, 指定的接口列表应该从您的管理接口开始, 其他所有接口跟随.
	 * 通常, 请考虑直接暴露目标bean或CGLIB代理.
	 */
	public void setExposeClassDescriptor(boolean exposeClassDescriptor) {
		this.exposeClassDescriptor = exposeClassDescriptor;
	}

	/**
	 * 返回是否为管理的操作公开JMX描述符字段 "class".
	 */
	protected boolean isExposeClassDescriptor() {
		return this.exposeClassDescriptor;
	}

	/**
	 * 如果需要, 设置ParameterNameDiscoverer, 用于解析方法参数名称 (e.g. 用于MBean操作方法的参数名称).
	 * <p>默认是 {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 如果需要, 返回ParameterNameDiscoverer, 用于解析方法参数名称 (可能是{@code null}以跳过参数检测).
	 */
	protected ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}


	/**
	 * 迭代MBean类上的所有属性, 并为子类提供了对访问器和变换器的包含进行投票的机会.
	 * 如果对特定访问者或变换器的包含进行投票, 则组合适当的元数据, 并将其传递给子类以用于描述符填充.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 属性元数据
	 * @throws JMException 发生错误
	 */
	@Override
	protected ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey) throws JMException {
		PropertyDescriptor[] props = BeanUtils.getPropertyDescriptors(getClassToExpose(managedBean));
		List<ModelMBeanAttributeInfo> infos = new ArrayList<ModelMBeanAttributeInfo>();

		for (PropertyDescriptor prop : props) {
			Method getter = prop.getReadMethod();
			if (getter != null && getter.getDeclaringClass() == Object.class) {
				continue;
			}
			if (getter != null && !includeReadAttribute(getter, beanKey)) {
				getter = null;
			}

			Method setter = prop.getWriteMethod();
			if (setter != null && !includeWriteAttribute(setter, beanKey)) {
				setter = null;
			}

			if (getter != null || setter != null) {
				// 如果getter和setter都为null, 则不需要公开.
				String attrName = JmxUtils.getAttributeName(prop, isUseStrictCasing());
				String description = getAttributeDescription(prop, beanKey);
				ModelMBeanAttributeInfo info = new ModelMBeanAttributeInfo(attrName, description, getter, setter);

				Descriptor desc = info.getDescriptor();
				if (getter != null) {
					desc.setField(FIELD_GET_METHOD, getter.getName());
				}
				if (setter != null) {
					desc.setField(FIELD_SET_METHOD, setter.getName());
				}

				populateAttributeDescriptor(desc, getter, setter, beanKey);
				info.setDescriptor(desc);
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanAttributeInfo[infos.size()]);
	}

	/**
	 * 迭代MBean类上的所有方法, 并为子类提供对它们的包含进行投票的机会.
	 * 如果特定方法对应于管理接口中包含的属性的访问器或变换器,
	 * 然后通过将 &quot;role&quot; 描述符字段设置为适当的值来公开相应的操作.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 操作元数据
	 */
	@Override
	protected ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey) {
		Method[] methods = getClassToExpose(managedBean).getMethods();
		List<ModelMBeanOperationInfo> infos = new ArrayList<ModelMBeanOperationInfo>();

		for (Method method : methods) {
			if (method.isSynthetic()) {
				continue;
			}
			if (Object.class == method.getDeclaringClass()) {
				continue;
			}

			ModelMBeanOperationInfo info = null;
			PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
			if (pd != null && ((method.equals(pd.getReadMethod()) && includeReadAttribute(method, beanKey)) ||
						(method.equals(pd.getWriteMethod()) && includeWriteAttribute(method, beanKey)))) {
				// 需要将其方法作为操作公开给JMX服务器的属性.
				info = createModelMBeanOperationInfo(method, pd.getName(), beanKey);
				Descriptor desc = info.getDescriptor();
				if (method.equals(pd.getReadMethod())) {
					desc.setField(FIELD_ROLE, ROLE_GETTER);
				}
				else {
					desc.setField(FIELD_ROLE, ROLE_SETTER);
				}
				desc.setField(FIELD_VISIBILITY, ATTRIBUTE_OPERATION_VISIBILITY);
				if (isExposeClassDescriptor()) {
					desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
				}
				info.setDescriptor(desc);
			}

			// 允许getter和setter直接标记为操作
			if (info == null && includeOperation(method, beanKey)) {
				info = createModelMBeanOperationInfo(method, method.getName(), beanKey);
				Descriptor desc = info.getDescriptor();
				desc.setField(FIELD_ROLE, ROLE_OPERATION);
				if (isExposeClassDescriptor()) {
					desc.setField(FIELD_CLASS, getClassForDescriptor(managedBean).getName());
				}
				populateOperationDescriptor(desc, method, beanKey);
				info.setDescriptor(desc);
			}

			if (info != null) {
				infos.add(info);
			}
		}

		return infos.toArray(new ModelMBeanOperationInfo[infos.size()]);
	}

	/**
	 * 为给定方法创建{@code ModelMBeanOperationInfo}的实例. 填充操作的参数信息.
	 * 
	 * @param method 用于创建{@code ModelMBeanOperationInfo}的{@code Method}
	 * @param name 操作的逻辑名称 (方法名称或属性名称);
	 * 默认实现不使用, 但可能由子类使用
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return {@code ModelMBeanOperationInfo}
	 */
	protected ModelMBeanOperationInfo createModelMBeanOperationInfo(Method method, String name, String beanKey) {
		MBeanParameterInfo[] params = getOperationParameters(method, beanKey);
		if (params.length == 0) {
			return new ModelMBeanOperationInfo(getOperationDescription(method, beanKey), method);
		}
		else {
			return new ModelMBeanOperationInfo(method.getName(),
				getOperationDescription(method, beanKey),
				getOperationParameters(method, beanKey),
				method.getReturnType().getName(),
				MBeanOperationInfo.UNKNOWN);
		}
	}

	/**
	 * 返回用于JMX描述符字段"class"的类.
	 * 仅在"exposeClassDescriptor"属性为"true"时应用.
	 * <p>默认实现返回JDK代理的第一个实现的接口, 或目标类.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * 
	 * @return 要在描述符字段 "class"中公开的类
	 */
	protected Class<?> getClassForDescriptor(Object managedBean) {
		if (AopUtils.isJdkDynamicProxy(managedBean)) {
			return AopProxyUtils.proxiedUserInterfaces(managedBean)[0];
		}
		return getClassToExpose(managedBean);
	}


	/**
	 * 允许子类对特定属性访问器的包含进行投票.
	 * 
	 * @param method {@code Method}访问器
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return {@code true} 如果访问器应该包含在管理接口中, 否则{@code false}
	 */
	protected abstract boolean includeReadAttribute(Method method, String beanKey);

	/**
	 * 允许子类对特定属性mutator的包含进行投票.
	 * 
	 * @param method the mutator {@code Method}.
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return {@code true} 如果mutator应该包含在管理接口中, 否则 {@code false}
	 */
	protected abstract boolean includeWriteAttribute(Method method, String beanKey);

	/**
	 * 允许子类对特定操作的包含进行投票.
	 * 
	 * @param method the operation method
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 操作是否应包含在管理接口中
	 */
	protected abstract boolean includeOperation(Method method, String beanKey);

	/**
	 * 获取特定属性的描述.
	 * <p>默认实现返回操作的描述, 该操作是相应的{@code Method}的名称.
	 * 
	 * @param propertyDescriptor 属性的PropertyDescriptor
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 属性的描述
	 */
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		return propertyDescriptor.getDisplayName();
	}

	/**
	 * 获取特定操作的描述.
	 * <p>默认实现返回操作的描述, 该操作是相应的{@code Method}的名称.
	 * 
	 * @param method 操作方法
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 操作的描述
	 */
	protected String getOperationDescription(Method method, String beanKey) {
		return method.getName();
	}

	/**
	 * 为给定方法创建参数信息.
	 * <p>默认实现返回一个{@code MBeanParameterInfo}的空数组.
	 * 
	 * @param method 要获取参数信息的{@code Method}
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return {@code MBeanParameterInfo}数组
	 */
	protected MBeanParameterInfo[] getOperationParameters(Method method, String beanKey) {
		ParameterNameDiscoverer paramNameDiscoverer = getParameterNameDiscoverer();
		String[] paramNames = (paramNameDiscoverer != null ? paramNameDiscoverer.getParameterNames(method) : null);
		if (paramNames == null) {
			return new MBeanParameterInfo[0];
		}

		MBeanParameterInfo[] info = new MBeanParameterInfo[paramNames.length];
		Class<?>[] typeParameters = method.getParameterTypes();
		for (int i = 0; i < info.length; i++) {
			info[i] = new MBeanParameterInfo(paramNames[i], typeParameters[i].getName(), paramNames[i]);
		}

		return info;
	}

	/**
	 * 允许子类为MBean的{@code Descriptor}添加额外的字段.
	 * <p>默认实现将{@code currencyTimeLimit}字段设置为指定的"defaultCurrencyTimeLimit", 如果有 (默认无).
	 * 
	 * @param descriptor MBean资源的{@code Descriptor}.
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 */
	@Override
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey) {
		applyDefaultCurrencyTimeLimit(descriptor);
	}

	/**
	 * 允许子类为特定属性的{@code Descriptor}添加额外字段.
	 * <p>默认实现将{@code currencyTimeLimit}字段设置为指定的"defaultCurrencyTimeLimit", 如果有 (默认无).
	 * 
	 * @param desc 属性描述符
	 * @param getter 属性的访问器方法
	 * @param setter 属性的mutator方法
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 */
	protected void populateAttributeDescriptor(Descriptor desc, Method getter, Method setter, String beanKey) {
		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * 允许子类为特定操作的{@code Descriptor}添加额外字段.
	 * <p>默认实现将{@code currencyTimeLimit}字段设置为指定的"defaultCurrencyTimeLimit", 如果有 (默认无).
	 * 
	 * @param desc 操作描述符
	 * @param method 对应于该操作的方法
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 */
	protected void populateOperationDescriptor(Descriptor desc, Method method, String beanKey) {
		applyDefaultCurrencyTimeLimit(desc);
	}

	/**
	 * 将{@code currencyTimeLimit}字段设置为指定的"defaultCurrencyTimeLimit", 如果有 (默认无).
	 * 
	 * @param desc JMX属性或操作描述符
	 */
	protected final void applyDefaultCurrencyTimeLimit(Descriptor desc) {
		if (getDefaultCurrencyTimeLimit() != null) {
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, getDefaultCurrencyTimeLimit().toString());
		}
	}

	/**
	 * 将给定的JMX "currencyTimeLimit"值应用于给定的描述符.
	 * <p>默认实现按原样设置值{@code >0} (作为缓存秒数),
	 * 将{@code 0}的值转换为{@code Integer.MAX_VALUE} ("始终缓存"),
	 * 并设置"defaultCurrencyTimeLimit" (如果有, 表示"从不缓存"), 如果值{@code <0}.
	 * 这符合JMX 1.2规范中的建议.
	 * 
	 * @param desc JMX属性或操作描述符
	 * @param currencyTimeLimit 要应用的"currencyTimeLimit"值
	 */
	protected void applyCurrencyTimeLimit(Descriptor desc, int currencyTimeLimit) {
		if (currencyTimeLimit > 0) {
			// 缓存秒数
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(currencyTimeLimit));
		}
		else if (currencyTimeLimit == 0) {
			// "always cache"
			desc.setField(FIELD_CURRENCY_TIME_LIMIT, Integer.toString(Integer.MAX_VALUE));
		}
		else {
			// "never cache"
			applyDefaultCurrencyTimeLimit(desc);
		}
	}

}
