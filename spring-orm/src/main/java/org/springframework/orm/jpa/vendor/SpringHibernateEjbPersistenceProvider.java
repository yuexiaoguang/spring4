package org.springframework.orm.jpa.vendor;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernatePersistence;

import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;

/**
 * 来自{@code org.hibernate.ejb}包的标准{@link HibernatePersistence}提供者的特定于Spring的子类,
 * 添加了对{@link SmartPersistenceUnitInfo#getManagedPackages()}的支持.
 *
 * <p>兼容Hibernate 3.6和4.0-4.2.
 */
class SpringHibernateEjbPersistenceProvider extends HibernatePersistence {

	@SuppressWarnings("rawtypes")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		Ejb3Configuration cfg = new Ejb3Configuration();
		if (info instanceof SmartPersistenceUnitInfo) {
			for (String managedPackage : ((SmartPersistenceUnitInfo) info).getManagedPackages()) {
				cfg.addPackage(managedPackage);
			}
		}
		Ejb3Configuration configured = cfg.configure(info, properties);
		return (configured != null ? configured.buildEntityManagerFactory() : null);
	}

}
