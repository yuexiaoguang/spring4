package org.springframework.context.weaving;

import org.springframework.beans.factory.Aware;
import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 希望获得应用程序上下文默认 {@link LoadTimeWeaver}的通知的任何对象实现的接口.
 */
public interface LoadTimeWeaverAware extends Aware {

	/**
	 * 设置此对象的包含{@link org.springframework.context.ApplicationContext ApplicationContext}的{@link LoadTimeWeaver}.
	 * <p>在普通bean属性填充之后但在初始化回调之前调用, 如
	 * {@link org.springframework.beans.factory.InitializingBean InitializingBean's}
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet() afterPropertiesSet()}
	 * 或自定义init方法.
	 * 在
	 * {@link org.springframework.context.ApplicationContextAware ApplicationContextAware's}
	 * {@link org.springframework.context.ApplicationContextAware#setApplicationContext setApplicationContext(..)}之后调用.
	 * <p><b>NOTE:</b> 只有在应用程序上下文中实际存在{@code LoadTimeWeaver}时, 才会调用此方法.
	 * 如果没有, 则不会调用该方法, 假定实现对象能够相应地激活其织入的依赖项.
	 * 
	 * @param loadTimeWeaver {@code LoadTimeWeaver}实例 (never {@code null})
	 */
	void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver);

}
