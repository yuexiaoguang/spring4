package org.springframework.jmx.export.assembler;

import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.support.AopUtils;
import org.springframework.jmx.support.JmxUtils;

/**
 * {@code MBeanInfoAssembler}接口的抽象实现, 它封装了{@code ModelMBeanInfo}实例的创建, 但委托创建元数据到子类.
 *
 * <p>此类从托管bean实例提供两种类的类提取:
 * {@link #getTargetClass}, 提取任何类型的AOP代理后的目标类;
 * {@link #getClassToExpose}, 返回将搜索其注解并公开给JMX运行时的类或接口.
 */
public abstract class AbstractMBeanInfoAssembler implements MBeanInfoAssembler {

	/**
	 * 创建使用所有JMX实现提供的{@code ModelMBeanInfoSupport}类的实例, 并通过调用子类来填充元数据.
	 * 
	 * @param managedBean 将被暴露的bean (可能是AOP代理)
	 * @param beanKey 与托管bean关联的Key
	 * 
	 * @return 已填充的ModelMBeanInfo实例
	 * @throws JMException 发生错误
	 */
	@Override
	public ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException {
		checkManagedBean(managedBean);
		ModelMBeanInfo info = new ModelMBeanInfoSupport(
				getClassName(managedBean, beanKey), getDescription(managedBean, beanKey),
				getAttributeInfo(managedBean, beanKey), getConstructorInfo(managedBean, beanKey),
				getOperationInfo(managedBean, beanKey), getNotificationInfo(managedBean, beanKey));
		Descriptor desc = info.getMBeanDescriptor();
		populateMBeanDescriptor(desc, managedBean, beanKey);
		info.setMBeanDescriptor(desc);
		return info;
	}

	/**
	 * 检查给定的bean实例, 如果它不符合使用此assembler的公开条件, 则抛出IllegalArgumentException.
	 * <p>默认实现为空, 接受每个bean实例.
	 * 
	 * @param managedBean 将被暴露的bean (可能是AOP代理)
	 * 
	 * @throws IllegalArgumentException bean不被允许公开
	 */
	protected void checkManagedBean(Object managedBean) throws IllegalArgumentException {
	}

	/**
	 * 返回给定bean实例的实际bean类.
	 * 这是暴露给描述样式JMX属性的类.
	 * <p>默认实现返回AOP代理的目标类, 或者普通bean类.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * 
	 * @return 要公开的bean类
	 */
	protected Class<?> getTargetClass(Object managedBean) {
		return AopUtils.getTargetClass(managedBean);
	}

	/**
	 * 返回为给定bean公开的类或接口.
	 * 这是将搜索属性和操作的类 (例如, 检查注解).
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * 
	 * @return 要公开的bean类
	 */
	protected Class<?> getClassToExpose(Object managedBean) {
		return JmxUtils.getClassToExpose(managedBean);
	}

	/**
	 * 返回为给定bean类公开的类或接口.
	 * 这是将搜索属性和操作的类.
	 * 
	 * @param beanClass bean类(可能是AOP代理类)
	 * 
	 * @return 要公开的bean类
	 */
	protected Class<?> getClassToExpose(Class<?> beanClass) {
		return JmxUtils.getClassToExpose(beanClass);
	}

	/**
	 * 获取MBean资源的类名.
	 * <p>默认实现基于类名返回MBean的简单描述.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return MBean描述
	 * @throws JMException 发生错误
	 */
	protected String getClassName(Object managedBean, String beanKey) throws JMException {
		return getTargetClass(managedBean).getName();
	}

	/**
	 * 获取MBean资源的描述.
	 * <p>默认实现基于类名返回MBean的简单描述.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @throws JMException 发生错误
	 */
	protected String getDescription(Object managedBean, String beanKey) throws JMException {
		String targetClassName = getTargetClass(managedBean).getName();
		if (AopUtils.isAopProxy(managedBean)) {
			return "Proxy for " + targetClassName;
		}
		return targetClassName;
	}

	/**
	 * 在{@code ModelMBeanInfo}实例构建之后, 但在传递给{@code MBeanExporter}之前调用.
	 * <p>子类可以实现此方法以向MBean元数据添加其他描述符. 默认实现为空.
	 * 
	 * @param descriptor MBean资源的{@code Descriptor}.
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @throws JMException 发生错误
	 */
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey)
			throws JMException {
	}

	/**
	 * 获取MBean资源的构造函数元数据.
	 * 子类应实现此方法, 以便为应在托管资源的管理接口中公开的所有构造函数返回适当的元数据.
	 * <p>默认实现返回一个{@code ModelMBeanConstructorInfo}的空数组.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 构造函数元数据
	 * @throws JMException 发生错误
	 */
	protected ModelMBeanConstructorInfo[] getConstructorInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanConstructorInfo[0];
	}

	/**
	 * 获取MBean资源的通知元数据.
	 * 子类应实现此方法, 以便为应在托管资源的管理接口中公开的所有通知返回相应的元数据.
	 * <p>默认实现返回一个{@code ModelMBeanNotificationInfo}的空数组.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 通知元数据
	 * @throws JMException 发生错误
	 */
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanNotificationInfo[0];
	}


	/**
	 * 获取MBean资源的属性元数据.
	 * 子类应实现此方法, 以便为应在托管资源的管理接口中公开的所有属性返回适当的元数据.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 属性元数据
	 * @throws JMException 发生错误
	 */
	protected abstract ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey)
			throws JMException;

	/**
	 * 获取MBean资源的操作元数据.
	 * 子类应实现此方法, 以便为应在托管资源的管理接口中公开的所有操作返回适当的元数据.
	 * 
	 * @param managedBean bean实例(可能是AOP代理)
	 * @param beanKey 与{@code MBeanExporter}的bean Map中的MBean关联的键
	 * 
	 * @return 操作元数据
	 * @throws JMException 发生错误
	 */
	protected abstract ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey)
			throws JMException;

}
