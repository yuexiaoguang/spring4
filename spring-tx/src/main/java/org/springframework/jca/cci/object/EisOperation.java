package org.springframework.jca.cci.object;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.util.Assert;

/**
 * Base class for EIS operation objects that work with the CCI API.
 * Encapsulates a CCI ConnectionFactory and a CCI InteractionSpec.
 *
 * <p>Works with a CciTemplate instance underneath. EIS operation objects
 * are an alternative to working with a CciTemplate directly.
 */
public abstract class EisOperation implements InitializingBean {

	private CciTemplate cciTemplate = new CciTemplate();

	private InteractionSpec interactionSpec;


	/**
	 * Set the CciTemplate to be used by this operation.
	 * Alternatively, specify a CCI ConnectionFactory.
	 */
	public void setCciTemplate(CciTemplate cciTemplate) {
		Assert.notNull(cciTemplate, "CciTemplate must not be null");
		this.cciTemplate = cciTemplate;
	}

	/**
	 * Return the CciTemplate used by this operation.
	 */
	public CciTemplate getCciTemplate() {
		return this.cciTemplate;
	}

	/**
	 * Set the CCI ConnectionFactory to be used by this operation.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.cciTemplate.setConnectionFactory(connectionFactory);
	}

	/**
	 * Set the CCI InteractionSpec for this operation.
	 */
	public void setInteractionSpec(InteractionSpec interactionSpec) {
		this.interactionSpec = interactionSpec;
	}

	/**
	 * Return the CCI InteractionSpec for this operation.
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
