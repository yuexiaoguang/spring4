package org.springframework.jms.listener.endpoint;

import javax.jms.Session;
import javax.resource.spi.ResourceAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;

/**
 * {@link JmsActivationSpecFactory}接口的默认实现.
 * 通过自动检测已知的特定于供应商的提供者属性, 支持JCA 1.5规范定义的标准JMS属性,
 * 以及Spring的扩展"maxConcurrency" 和 "prefetchSize"设置.
 *
 * <p>ActivationSpec工厂实际上依赖于具体的JMS提供者, e.g. on ActiveMQ.
 * 此默认实现只是从提供程序的类名中猜测ActivationSpec类名
 * ("ActiveMQResourceAdapter" -> "ActiveMQActivationSpec"在同一个包中,
 * 或"ActivationSpecImpl"在与ResourceAdapter类相同的包中),
 * 并按照JCA 1.5规范 (附录 B)的建议填充ActivationSpec属性.
 * 如果这些默认命名规则不适用, 显式指定'activationSpecClass'属性.
 *
 * <p>Note: 在扩展设置方面支持ActiveMQ, JORAM 和 WebSphere (通过检测其bean属性命名约定).
 * 默认的ActivationSpec类检测规则也可以应用于其他JMS提供者.
 *
 * <p>Thanks to Agim Emruli and Laurie Chan for pointing out WebSphere MQ settings and contributing corresponding tests!
 */
public class DefaultJmsActivationSpecFactory extends StandardJmsActivationSpecFactory {

	private static final String RESOURCE_ADAPTER_SUFFIX = "ResourceAdapter";

	private static final String RESOURCE_ADAPTER_IMPL_SUFFIX = "ResourceAdapterImpl";

	private static final String ACTIVATION_SPEC_SUFFIX = "ActivationSpec";

	private static final String ACTIVATION_SPEC_IMPL_SUFFIX = "ActivationSpecImpl";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 此实现从提供者的类名称中猜出ActivationSpec类名称:
	 * e.g. "ActiveMQResourceAdapter" -> "ActiveMQActivationSpec"在同一个包中,
	 * 或者与ResourceAdapter类在同一个包中名为"ActivationSpecImpl"的类.
	 */
	@Override
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		String adapterClassName = adapter.getClass().getName();

		if (adapterClassName.endsWith(RESOURCE_ADAPTER_SUFFIX)) {
			// e.g. ActiveMQ
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				logger.debug("No default <Provider>ActivationSpec class found: " + specClassName);
			}
		}

		else if (adapterClassName.endsWith(RESOURCE_ADAPTER_IMPL_SUFFIX)){
			//e.g. WebSphere
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_IMPL_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_IMPL_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				logger.debug("No default <Provider>ActivationSpecImpl class found: " + specClassName);
			}
		}

		// e.g. JORAM
		String providerPackage = adapterClassName.substring(0, adapterClassName.lastIndexOf('.') + 1);
		String specClassName = providerPackage + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			logger.debug("No default ActivationSpecImpl class found in provider package: " + specClassName);
		}

		// ActivationSpecImpl class in "inbound" subpackage (WebSphere MQ 6.0.2.1)
		specClassName = providerPackage + "inbound." + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			logger.debug("No default ActivationSpecImpl class found in inbound subpackage: " + specClassName);
		}

		throw new IllegalStateException("No ActivationSpec class defined - " +
				"specify the 'activationSpecClass' property or override the 'determineActivationSpecClass' method");
	}

	/**
	 * 此实现通过检测相应的ActivationSpec属性来支持Spring的扩展"maxConcurrency" 和 "prefetchSize"设置:
	 * "maxSessions"/"maxNumberOfWorks" 和 "maxMessagesPerSessions"/"maxMessages"
	 * (遵循ActiveMQ和JORAM的命名约定).
	 */
	@Override
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		super.populateActivationSpecProperties(bw, config);
		if (config.getMaxConcurrency() > 0) {
			if (bw.isWritableProperty("maxSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxSessions", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxNumberOfWorks")) {
				// JORAM
				bw.setPropertyValue("maxNumberOfWorks", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxConcurrency")){
				// WebSphere
				bw.setPropertyValue("maxConcurrency", Integer.toString(config.getMaxConcurrency()));
			}
		}
		if (config.getPrefetchSize() > 0) {
			if (bw.isWritableProperty("maxMessagesPerSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxMessagesPerSessions", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxMessages")) {
				// JORAM
				bw.setPropertyValue("maxMessages", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxBatchSize")){
				// WebSphere
				bw.setPropertyValue("maxBatchSize", Integer.toString(config.getPrefetchSize()));
			}
		}
	}

	/**
	 * 此实现将{@code SESSION_TRANSACTED}映射到名为"useRAManagedTransaction"的ActivationSpec属性 (遵循ActiveMQ的命名约定).
	 */
	@Override
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED && bw.isWritableProperty("useRAManagedTransaction")) {
			// ActiveMQ
			bw.setPropertyValue("useRAManagedTransaction", "true");
		}
		else {
			super.applyAcknowledgeMode(bw, ackMode);
		}
	}
}
