package org.springframework.beans.factory.aspectj;

/**
 * Generic-based dependency injection aspect.
 * <p>
 * This aspect allows users to implement efficient, type-safe dependency injection without
 * the use of the &#64;Configurable annotation.
 *
 * The subaspect of this aspect doesn't need to include any AOP constructs.
 * For example, here is a subaspect that configures the {@code PricingStrategyClient} objects.
 * <pre class="code">
 * aspect PricingStrategyDependencyInjectionAspect
 *        extends GenericInterfaceDrivenDependencyInjectionAspect<PricingStrategyClient> {
 *     private PricingStrategy pricingStrategy;
 *
 *     public void configure(PricingStrategyClient bean) {
 *         bean.setPricingStrategy(pricingStrategy);
 *     }
 *
 *     public void setPricingStrategy(PricingStrategy pricingStrategy) {
 *         this.pricingStrategy = pricingStrategy;
 *     }
 * }
 * </pre>
 */
public abstract aspect GenericInterfaceDrivenDependencyInjectionAspect<I> extends AbstractInterfaceDrivenDependencyInjectionAspect {
    declare parents: I implements ConfigurableObject;

	public pointcut inConfigurableBean() : within(I+);

	public final void configureBean(Object bean) {
		configure((I)bean);
	}

	// Unfortunately, erasure used with generics won't allow to use the same named method
	protected abstract void configure(I bean);
}
