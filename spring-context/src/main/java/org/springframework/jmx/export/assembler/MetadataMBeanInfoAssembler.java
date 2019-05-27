package org.springframework.jmx.export.assembler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanNotificationInfo;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxMetadataUtils;
import org.springframework.jmx.export.metadata.ManagedAttribute;
import org.springframework.jmx.export.metadata.ManagedMetric;
import org.springframework.jmx.export.metadata.ManagedNotification;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MBeanInfoAssembler}接口的实现, 从source级元数据中读取管理接口信息.
 *
 * <p>使用{@link JmxAttributeSource}策略接口, 以便可以使用任何支持的实现读取元数据.
 * 开箱即用, Spring提供了基于注解的实现: {@code AnnotationJmxAttributeSource}.
 */
public class MetadataMBeanInfoAssembler extends AbstractReflectiveMBeanInfoAssembler
		implements AutodetectCapableMBeanInfoAssembler, InitializingBean {

	private JmxAttributeSource attributeSource;


	/**
	 * 创建一个新的{@code MetadataMBeanInfoAssembler}, 需要通过{@link #setAttributeSource}方法进行配置.
	 */
	public MetadataMBeanInfoAssembler() {
	}

	/**
	 * @param attributeSource 要使用的JmxAttributeSource
	 */
	public MetadataMBeanInfoAssembler(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}


	/**
	 * 设置用于从bean类读取元数据的{@code JmxAttributeSource}实现.
	 */
	public void setAttributeSource(JmxAttributeSource attributeSource) {
		Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
		this.attributeSource = attributeSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.attributeSource == null) {
			throw new IllegalArgumentException("Property 'attributeSource' is required");
		}
	}


	/**
	 * 如果遇到JDK动态代理, 则抛出IllegalArgumentException.
	 * 元数据只能从目标类和CGLIB代理中读取!
	 */
	@Override
	protected void checkManagedBean(Object managedBean) throws IllegalArgumentException {
		if (AopUtils.isJdkDynamicProxy(managedBean)) {
			throw new IllegalArgumentException(
					"MetadataMBeanInfoAssembler does not support JDK dynamic proxies - " +
					"export the target beans directly or use CGLIB proxies instead");
		}
	}

	/**
	 * 用于自动检测bean. 检查bean的类是否具有 {@code ManagedResource}属性. 如果是这样, 它将被添加到包含的bean的列表.
	 * 
	 * @param beanClass bean的类
	 * @param beanName bean工厂中bean的名称
	 */
	@Override
	public boolean includeBean(Class<?> beanClass, String beanName) {
		return (this.attributeSource.getManagedResource(getClassToExpose(beanClass)) != null);
	}

	/**
	 * 投票属性访问器的包含.
	 * 
	 * @param method 方法访问器
	 * @param beanKey 与bean Map中的MBean关联的键
	 * 
	 * @return 方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return hasManagedAttribute(method) || hasManagedMetric(method);
	}

	/**
	 * 投票属性mutator的包含.
	 * 
	 * @param method the mutator method
	 * @param beanKey 与bean Map中的MBean关联的键
	 * 
	 * @return 方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return hasManagedAttribute(method);
	}

	/**
	 * 投票操作的包含.
	 * 
	 * @param method the operation method
	 * @param beanKey 与bean Map中的MBean关联的键
	 * 
	 * @return 方法是否具有适当的元数据
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		if (pd != null) {
			if (hasManagedAttribute(method)) {
				return true;
			}
		}
		return hasManagedOperation(method);
	}

	/**
	 * 检查给定的Method是否具有{@code ManagedAttribute}属性.
	 */
	private boolean hasManagedAttribute(Method method) {
		return (this.attributeSource.getManagedAttribute(method) != null);
	}

	/**
	 * 检查给定的Method是否具有{@code ManagedMetric}属性.
	 */
	private boolean hasManagedMetric(Method method) {
		return (this.attributeSource.getManagedMetric(method) != null);
	}

	/**
	 * 检查给定的Method是否具有{@code ManagedOperation}属性.
	 * 
	 * @param method 要检查的方法
	 */
	private boolean hasManagedOperation(Method method) {
		return (this.attributeSource.getManagedOperation(method) != null);
	}


	/**
	 * 从source级元数据中读取受管资源描述.
	 * 如果找不到描述, 则返回空{@code String}.
	 */
	@Override
	protected String getDescription(Object managedBean, String beanKey) {
		ManagedResource mr = this.attributeSource.getManagedResource(getClassToExpose(managedBean));
		return (mr != null ? mr.getDescription() : "");
	}

	/**
	 * 创建与此属性描述符对应的属性的描述.
	 * 尝试使用getter或setter属性中的元数据创建描述, 否则使用属性名称.
	 */
	@Override
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		Method readMethod = propertyDescriptor.getReadMethod();
		Method writeMethod = propertyDescriptor.getWriteMethod();

		ManagedAttribute getter =
				(readMethod != null ? this.attributeSource.getManagedAttribute(readMethod) : null);
		ManagedAttribute setter =
				(writeMethod != null ? this.attributeSource.getManagedAttribute(writeMethod) : null);

		if (getter != null && StringUtils.hasText(getter.getDescription())) {
			return getter.getDescription();
		}
		else if (setter != null && StringUtils.hasText(setter.getDescription())) {
			return setter.getDescription();
		}

		ManagedMetric metric = (readMethod != null ? this.attributeSource.getManagedMetric(readMethod) : null);
		if (metric != null && StringUtils.hasText(metric.getDescription())) {
			return metric.getDescription();
		}

		return propertyDescriptor.getDisplayName();
	}

	/**
	 * 从元数据中检索提供的{@code Method}的描述.
	 * 使用方法名称, 如果元数据中没有描述.
	 */
	@Override
	protected String getOperationDescription(Method method, String beanKey) {
		PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
		if (pd != null) {
			ManagedAttribute ma = this.attributeSource.getManagedAttribute(method);
			if (ma != null && StringUtils.hasText(ma.getDescription())) {
				return ma.getDescription();
			}
			ManagedMetric metric = this.attributeSource.getManagedMetric(method);
			if (metric != null && StringUtils.hasText(metric.getDescription())) {
				return metric.getDescription();
			}
			return method.getName();
		}
		else {
			ManagedOperation mo = this.attributeSource.getManagedOperation(method);
			if (mo != null && StringUtils.hasText(mo.getDescription())) {
				return mo.getDescription();
			}
			return method.getName();
		}
	}

	/**
	 * 从附加到方法的{@code ManagedOperationParameter}属性中读取{@code MBeanParameterInfo}.
	 * 如果未找到任何属性, 则返回{@code MBeanParameterInfo}的空数组.
	 */
	@Override
	protected MBeanParameterInfo[] getOperationParameters(Method method, String beanKey) {
		ManagedOperationParameter[] params = this.attributeSource.getManagedOperationParameters(method);
		if (ObjectUtils.isEmpty(params)) {
			return super.getOperationParameters(method, beanKey);
		}

		MBeanParameterInfo[] parameterInfo = new MBeanParameterInfo[params.length];
		Class<?>[] methodParameters = method.getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			ManagedOperationParameter param = params[i];
			parameterInfo[i] =
					new MBeanParameterInfo(param.getName(), methodParameters[i].getName(), param.getDescription());
		}
		return parameterInfo;
	}

	/**
	 * 从托管资源的{@code Class}读取{@link ManagedNotification}元数据, 并生成和返回相应的{@link ModelMBeanNotificationInfo}元数据.
	 */
	@Override
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey) {
		ManagedNotification[] notificationAttributes =
				this.attributeSource.getManagedNotifications(getClassToExpose(managedBean));
		ModelMBeanNotificationInfo[] notificationInfos =
				new ModelMBeanNotificationInfo[notificationAttributes.length];

		for (int i = 0; i < notificationAttributes.length; i++) {
			ManagedNotification attribute = notificationAttributes[i];
			notificationInfos[i] = JmxMetadataUtils.convertToModelMBeanNotificationInfo(attribute);
		}

		return notificationInfos;
	}

	/**
	 * 将{@code ManagedResource}属性中的描述符字段添加到MBean描述符.
	 * 具体而言, 如果元数据中存在{@code currencyTimeLimit}, {@code persistPolicy}, {@code persistPeriod},
	 * {@code persistLocation}, {@code persistName}, 将它们添加到描述符字段.
	 */
	@Override
	protected void populateMBeanDescriptor(Descriptor desc, Object managedBean, String beanKey) {
		ManagedResource mr = this.attributeSource.getManagedResource(getClassToExpose(managedBean));
		if (mr == null) {
			throw new InvalidMetadataException(
					"No ManagedResource attribute found for class: " + getClassToExpose(managedBean));
		}

		applyCurrencyTimeLimit(desc, mr.getCurrencyTimeLimit());

		if (mr.isLog()) {
			desc.setField(FIELD_LOG, "true");
		}
		if (StringUtils.hasLength(mr.getLogFile())) {
			desc.setField(FIELD_LOG_FILE, mr.getLogFile());
		}

		if (StringUtils.hasLength(mr.getPersistPolicy())) {
			desc.setField(FIELD_PERSIST_POLICY, mr.getPersistPolicy());
		}
		if (mr.getPersistPeriod() >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(mr.getPersistPeriod()));
		}
		if (StringUtils.hasLength(mr.getPersistName())) {
			desc.setField(FIELD_PERSIST_NAME, mr.getPersistName());
		}
		if (StringUtils.hasLength(mr.getPersistLocation())) {
			desc.setField(FIELD_PERSIST_LOCATION, mr.getPersistLocation());
		}
	}

	/**
	 * 将{@code ManagedAttribute}属性或{@code ManagedMetric}属性中的描述符字段添加到属性描述符.
	 */
	@Override
	protected void populateAttributeDescriptor(Descriptor desc, Method getter, Method setter, String beanKey) {
		if (getter != null && hasManagedMetric(getter)) {
			populateMetricDescriptor(desc, this.attributeSource.getManagedMetric(getter));
		}
		else {
			ManagedAttribute gma =
					(getter == null) ? ManagedAttribute.EMPTY : this.attributeSource.getManagedAttribute(getter);
			ManagedAttribute sma =
					(setter == null) ? ManagedAttribute.EMPTY : this.attributeSource.getManagedAttribute(setter);
			populateAttributeDescriptor(desc,gma,sma);
		}
	}

	private void populateAttributeDescriptor(Descriptor desc, ManagedAttribute gma, ManagedAttribute sma) {
		applyCurrencyTimeLimit(desc, resolveIntDescriptor(gma.getCurrencyTimeLimit(), sma.getCurrencyTimeLimit()));

		Object defaultValue = resolveObjectDescriptor(gma.getDefaultValue(), sma.getDefaultValue());
		desc.setField(FIELD_DEFAULT, defaultValue);

		String persistPolicy = resolveStringDescriptor(gma.getPersistPolicy(), sma.getPersistPolicy());
		if (StringUtils.hasLength(persistPolicy)) {
			desc.setField(FIELD_PERSIST_POLICY, persistPolicy);
		}
		int persistPeriod = resolveIntDescriptor(gma.getPersistPeriod(), sma.getPersistPeriod());
		if (persistPeriod >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(persistPeriod));
		}
	}

	private void populateMetricDescriptor(Descriptor desc, ManagedMetric metric) {
		applyCurrencyTimeLimit(desc, metric.getCurrencyTimeLimit());

		if (StringUtils.hasLength(metric.getPersistPolicy())) {
			desc.setField(FIELD_PERSIST_POLICY, metric.getPersistPolicy());
		}
		if (metric.getPersistPeriod() >= 0) {
			desc.setField(FIELD_PERSIST_PERIOD, Integer.toString(metric.getPersistPeriod()));
		}

		if (StringUtils.hasLength(metric.getDisplayName())) {
			desc.setField(FIELD_DISPLAY_NAME, metric.getDisplayName());
		}

		if (StringUtils.hasLength(metric.getUnit())) {
			desc.setField(FIELD_UNITS, metric.getUnit());
		}

		if (StringUtils.hasLength(metric.getCategory())) {
			desc.setField(FIELD_METRIC_CATEGORY, metric.getCategory());
		}

		desc.setField(FIELD_METRIC_TYPE, metric.getMetricType().toString());
	}

	/**
	 * 将{@code ManagedAttribute}属性中的描述符字段添加到属性描述符中.
	 * 具体而言, 如果元数据中存在{@code currencyTimeLimit}描述符字段, 则添加该字段.
	 */
	@Override
	protected void populateOperationDescriptor(Descriptor desc, Method method, String beanKey) {
		ManagedOperation mo = this.attributeSource.getManagedOperation(method);
		if (mo != null) {
			applyCurrencyTimeLimit(desc, mo.getCurrencyTimeLimit());
		}
	}

	/**
	 * 确定应将两个{@code int}值中的哪一个用作属性描述符的值.
	 * 通常, 只有getter或setter具有非负值, 因此使用该值.
	 * 如果两个值都是非负值, 使用两者中较大的一个.
	 * 此方法可用于解析任何{@code int}值描述符, 其中有两个可能的值.
	 * 
	 * @param getter 与此属性的getter关联的int值
	 * @param setter 与此属性的setter关联的int
	 */
	private int resolveIntDescriptor(int getter, int setter) {
		return (getter >= setter ? getter : setter);
	}

	/**
	 * 根据附加到getter和setter方法的值来定位描述符的值.
	 * 如果两者都提供了值, 则首选附加到getter的值.
	 * 
	 * @param getter 与get方法关联的Object值
	 * @param setter 与set方法关联的Object值
	 * 
	 * @return 用作描述符的值的适当Object
	 */
	private Object resolveObjectDescriptor(Object getter, Object setter) {
		return (getter != null ? getter : setter);
	}

	/**
	 * 根据附加到getter和setter方法的值来定位描述符的值.
	 * 如果两者都提供了值, 则首选附加到getter的值.
	 * 提供的默认值用于检查与getter关联的值是否已更改为默认值.
	 * 
	 * @param getter 与get方法关联的String值
	 * @param setter 与set方法关联的String值
	 * 
	 * @return 用作描述符的值的适当String
	 */
	private String resolveStringDescriptor(String getter, String setter) {
		return (StringUtils.hasLength(getter) ? getter : setter);
	}

}
