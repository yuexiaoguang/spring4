package org.springframework.test.context;

/**
 * Strategy interface for programmatically resolving which <em>active bean
 * definition profiles</em> should be used when loading an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for a test class.
 *
 * <p>A custom {@code ActiveProfilesResolver} can be registered via the
 * {@link ActiveProfiles#resolver resolver} attribute of {@code @ActiveProfiles}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 */
public interface ActiveProfilesResolver {

	/**
	 * Resolve the <em>bean definition profiles</em> to use when loading an
	 * {@code ApplicationContext} for the given {@linkplain Class test class}.
	 * @param testClass the test class for which the profiles should be resolved;
	 * never {@code null}
	 * @return the list of bean definition profiles to use when loading the
	 * {@code ApplicationContext}; never {@code null}
	 */
	String[] resolve(Class<?> testClass);

}
