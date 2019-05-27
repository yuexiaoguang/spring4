package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.core.type.MethodMetadata;

abstract class ConfigurationMethod {

	protected final MethodMetadata metadata;

	protected final ConfigurationClass configurationClass;


	public ConfigurationMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
		this.metadata = metadata;
		this.configurationClass = configurationClass;
	}


	public MethodMetadata getMetadata() {
		return this.metadata;
	}

	public ConfigurationClass getConfigurationClass() {
		return this.configurationClass;
	}

	public Location getResourceLocation() {
		return new Location(this.configurationClass.getResource(), this.metadata);
	}

	String getFullyQualifiedMethodName() {
		return this.metadata.getDeclaringClassName() + "#" + this.metadata.getMethodName();
	}

	static String getShortMethodName(String fullyQualifiedMethodName) {
		return fullyQualifiedMethodName.substring(fullyQualifiedMethodName.indexOf('#') + 1);
	}

	public void validate(ProblemReporter problemReporter) {
	}


	@Override
	public String toString() {
		return String.format("[%s:name=%s,declaringClass=%s]",
				getClass().getSimpleName(), getMetadata().getMethodName(), getMetadata().getDeclaringClassName());
	}

}
