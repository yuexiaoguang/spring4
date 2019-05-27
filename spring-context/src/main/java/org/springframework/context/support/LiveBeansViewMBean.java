package org.springframework.context.support;

/**
 * {@link LiveBeansView}功能的MBean操作接口.
 */
public interface LiveBeansViewMBean {

	/**
	 * 生成当前bean及其依赖项的JSON快照.
	 */
	String getSnapshotAsJson();

}
