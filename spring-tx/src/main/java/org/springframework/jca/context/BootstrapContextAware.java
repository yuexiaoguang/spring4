package org.springframework.jca.context;

import javax.resource.spi.BootstrapContext;

import org.springframework.beans.factory.Aware;

/**
 * 希望获得BootstrapContext (通常由{@link ResourceAdapterApplicationContext}确定)通知的任何对象实现的接口.
 */
public interface BootstrapContextAware extends Aware {

	/**
	 * 设置此对象运行的BootstrapContext.
	 * <p>在普通bean属性填充之后但在初始化回调之前调用, 例如InitializingBean的 {@code afterPropertiesSet}或自定义init方法.
	 * 在ApplicationContextAware的{@code setApplicationContext}之后调用.
	 * 
	 * @param bootstrapContext 此对象使用的BootstrapContext对象
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

}
