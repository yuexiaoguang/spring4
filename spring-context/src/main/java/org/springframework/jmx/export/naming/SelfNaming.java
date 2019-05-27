package org.springframework.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 允许基础架构组件为{@code MBeanExporter}提供自己的{@code ObjectName}的接口.
 *
 * <p><b>Note:</b> 此接口主要用于内部使用.
 */
public interface SelfNaming {

	/**
	 * 返回实现对象的{@code ObjectName}.
	 * 
	 * @throws MalformedObjectNameException 如果由ObjectName构造函数抛出
	 */
	ObjectName getObjectName() throws MalformedObjectNameException;

}
