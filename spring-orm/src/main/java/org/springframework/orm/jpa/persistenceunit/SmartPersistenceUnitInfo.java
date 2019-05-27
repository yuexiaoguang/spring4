package org.springframework.orm.jpa.persistenceunit;

import java.util.List;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Extension of the standard JPA PersistenceUnitInfo interface, for advanced collaboration
 * between Spring's {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}
 * and {@link PersistenceUnitManager} implementations.
 */
public interface SmartPersistenceUnitInfo extends PersistenceUnitInfo {

	/**
	 * Return a list of managed Java packages, to be introspected by the persistence provider.
	 * Typically found through scanning but not exposable through {@link #getManagedClassNames()}.
	 * @return a list of names of managed Java packages (potentially empty)
	 */
	List<String> getManagedPackages();

	/**
	 * Set the persistence provider's own package name, for exclusion from class transformation.
	 */
	void setPersistenceProviderPackageName(String persistenceProviderPackageName);

}
