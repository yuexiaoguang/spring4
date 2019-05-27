package org.springframework.jmx.export.assembler;

import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanInfo;

/**
 * 由可以为托管资源创建管理接口元数据的所有类实现的接口.
 *
 * <p>{@code MBeanExporter}使用它为非MBean的bean生成管理接口.
 */
public interface MBeanInfoAssembler {

	/**
	 * 为给定的受管资源创建ModelMBeanInfo.
	 * 
	 * @param managedBean 将被暴露的bean (可能是AOP代理)
	 * @param beanKey 与托管bean关联的Key
	 * 
	 * @return ModelMBeanInfo元数据对象
	 * @throws JMException 发生错误
	 */
	ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException;

}
