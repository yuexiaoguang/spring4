package org.springframework.ui.velocity;

import java.io.IOException;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

/**
 * Factory bean, 用于配置VelocityEngine并将其作为bean引用提供.
 * 此bean适用于应用程序代码中Velocity的各种用法, e.g. 用于生成邮件内容.
 * 对于Web视图, VelocityConfigurer用于为视图设置VelocityEngine.
 *
 * <p>使用此类的最简单方法是指定"resourceLoaderPath"; 那么不需要任何进一步的配置.
 * 例如, 在Web应用程序上下文中:
 *
 * <pre class="code"> &lt;bean id="velocityEngine" class="org.springframework.ui.velocity.VelocityEngineFactoryBean"&gt;
 *   &lt;property name="resourceLoaderPath" value="/WEB-INF/velocity/"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * 有关配置详细信息, 请参阅基类VelocityEngineFactory.
 *
 * @deprecated as of Spring 4.3, in favor of FreeMarker
 */
@Deprecated
public class VelocityEngineFactoryBean extends VelocityEngineFactory
		implements FactoryBean<VelocityEngine>, InitializingBean, ResourceLoaderAware {

	private VelocityEngine velocityEngine;


	@Override
	public void afterPropertiesSet() throws IOException, VelocityException {
		this.velocityEngine = createVelocityEngine();
	}


	@Override
	public VelocityEngine getObject() {
		return this.velocityEngine;
	}

	@Override
	public Class<? extends VelocityEngine> getObjectType() {
		return VelocityEngine.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
