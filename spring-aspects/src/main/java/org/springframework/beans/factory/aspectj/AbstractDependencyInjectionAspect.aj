package org.springframework.beans.factory.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.annotation.control.CodeGenerationHint;

/**
 * Abstract base aspect that can perform Dependency
 * Injection on objects, however they may be created.
 */
public abstract aspect AbstractDependencyInjectionAspect {

	private pointcut preConstructionCondition() :
			leastSpecificSuperTypeConstruction() && preConstructionConfiguration();

	private pointcut postConstructionCondition() :
			mostSpecificSubTypeConstruction() && !preConstructionConfiguration();

	/**
	 * Select least specific super type that is marked for DI
	 * (so that injection occurs only once with pre-construction injection).
	 */
	public abstract pointcut leastSpecificSuperTypeConstruction();

	/**
	 * Select the most-specific initialization join point
	 * (most concrete class) for the initialization of an instance.
	 */
	@CodeGenerationHint(ifNameSuffix="6f1")
	public pointcut mostSpecificSubTypeConstruction() :
			if (thisJoinPoint.getSignature().getDeclaringType() == thisJoinPoint.getThis().getClass());

	/**
	 * Select join points in beans to be configured prior to construction?
	 * By default, use post-construction injection matching the default in the Configurable annotation.
	 */
	public pointcut preConstructionConfiguration() : if (false);

	/**
	 * Select construction join points for objects to inject dependencies.
	 */
	public abstract pointcut beanConstruction(Object bean);

	/**
	 * Select deserialization join points for objects to inject dependencies.
	 */
	public abstract pointcut beanDeserialization(Object bean);

	/**
	 * Select join points in a configurable bean.
	 */
	public abstract pointcut inConfigurableBean();


	/**
	 * Pre-construction configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	before(Object bean) :
		beanConstruction(bean) && preConstructionCondition() && inConfigurableBean()  {
		configureBean(bean);
	}

	/**
	 * Post-construction configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning :
		beanConstruction(bean) && postConstructionCondition() && inConfigurableBean() {
		configureBean(bean);
	}

	/**
	 * Post-deserialization configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning :
		beanDeserialization(bean) && inConfigurableBean() {
		configureBean(bean);
	}


	/**
	 * Configure the given bean.
	 */
	public abstract void configureBean(Object bean);

}
