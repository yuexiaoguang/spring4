package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.util.Assert;

/**
 * 与CCI API一起使用的EIS操作对象的基类.
 * 封装CCI ConnectionFactory和CCI InteractionSpec.
 *
 * <p>适用于下面的CciTemplate实例. EIS操作对象是直接使用CciTemplate的替代方法.
 */
public abstract class EisOperation implements InitializingBean {

	private CciTemplate cciTemplate = new CciTemplate();

	private InteractionSpec interactionSpec;


	/**
	 * 设置此操作使用的CciTemplate.
	 * 或者, 指定CCI ConnectionFactory.
	 */
	public void setCciTemplate(CciTemplate cciTemplate) {
		Assert.notNull(cciTemplate, "CciTemplate must not be null");
		this.cciTemplate = cciTemplate;
	}

	/**
	 * 返回此操作使用的CciTemplate.
	 */
	public CciTemplate getCciTemplate() {
		return this.cciTemplate;
	}

	/**
	 * 设置此操作使用的CCI ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.cciTemplate.setConnectionFactory(connectionFactory);
	}

	/**
	 * 设置此操作的CCI InteractionSpec.
	 */
	public void setInteractionSpec(InteractionSpec interactionSpec) {
		this.interactionSpec = interactionSpec;
	}

	/**
	 * 返回此操作的CCI InteractionSpec.
	 */
	public InteractionSpec getInteractionSpec() {
		return this.interactionSpec;
	}


	@Override
	public void afterPropertiesSet() {
		this.cciTemplate.afterPropertiesSet();

		if (this.interactionSpec == null) {
			throw new IllegalArgumentException("InteractionSpec is required");
		}
	}

}
