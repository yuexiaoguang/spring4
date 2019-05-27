package org.springframework.ui.freemarker;

import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;

/**
 * 工厂bean, 创建一个FreeMarker配置并将其作为bean引用提供.
 * 此bean适用于应用程序代码中FreeMarker的任何用法, e.g. 用于生成邮件内容.
 * 对于Web视图, FreeMarkerConfigurer用于设置FreeMarkerConfigurationFactory.
 *
 * 使用此类的最简单方法是仅指定"templateLoaderPath"; 那么你不需要任何进一步的配置.
 * 例如, 在Web应用程序上下文中:
 *
 * <pre class="code"> &lt;bean id="freemarkerConfiguration" class="org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean"&gt;
 *   &lt;property name="templateLoaderPath" value="/WEB-INF/freemarker/"/&gt;
 * &lt;/bean&gt;</pre>
 * 
 * 有关配置详细信息, 请参阅基类FreeMarkerConfigurationFactory.
 *
 * <p>Note: Spring的FreeMarker支持需要FreeMarker 2.3或更高版本.
 */
public class FreeMarkerConfigurationFactoryBean extends FreeMarkerConfigurationFactory
		implements FactoryBean<Configuration>, InitializingBean, ResourceLoaderAware {

	private Configuration configuration;


	@Override
	public void afterPropertiesSet() throws IOException, TemplateException {
		this.configuration = createConfiguration();
	}


	@Override
	public Configuration getObject() {
		return this.configuration;
	}

	@Override
	public Class<? extends Configuration> getObjectType() {
		return Configuration.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
