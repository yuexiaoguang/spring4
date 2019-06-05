package org.springframework.orm.jpa.vendor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;

/**
 * 来自{@code org.hibernate.jpa}包的标准{@link HibernatePersistenceProvider}的特定于Spring的子类,
 * 添加了对{@link SmartPersistenceUnitInfo#getManagedPackages()}的支持.
 *
 * <p>兼容Hibernate 4.3-5.0.
 * {@link SpringHibernateEjbPersistenceProvider}是与早期Hibernate版本(3.6-4.2)兼容的替代方案.
 */
class SpringHibernateJpaPersistenceProvider extends HibernatePersistenceProvider {

	@Override
	@SuppressWarnings("rawtypes")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		final List<String> mergedClassesAndPackages = new ArrayList<String>(info.getManagedClassNames());
		if (info instanceof SmartPersistenceUnitInfo) {
			mergedClassesAndPackages.addAll(((SmartPersistenceUnitInfo) info).getManagedPackages());
		}
		return new EntityManagerFactoryBuilderImpl(
				new PersistenceUnitInfoDescriptor(info) {
					@Override
					public List<String> getManagedClassNames() {
						return mergedClassesAndPackages;
					}
				}, properties).build();
	}

}
