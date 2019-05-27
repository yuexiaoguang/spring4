package org.springframework.orm.hibernate3.support;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.MergeEvent;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Extension of Hibernate's DefaultMergeEventListener, transferring the ids
 * of newly saved objects to the corresponding original objects (that are part
 * of the detached object graph passed into the {@code merge} method).
 *
 * <p>Transferring newly assigned ids to the original graph allows for continuing
 * to use the original object graph, despite merged copies being registered with
 * the current Hibernate Session. This is particularly useful for web applications
 * that might want to store an object graph and then render it in a web view,
 * with links that include the id of certain (potentially newly saved) objects.
 *
 * <p>The merge behavior given by this MergeEventListener is nearly identical
 * to TopLink's merge behavior. See PetClinic for an example, which relies on
 * ids being available for newly saved objects: the {@code HibernateClinic}
 * and {@code TopLinkClinic} DAO implementations both use straight merge
 * calls, with the Hibernate SessionFactory configuration specifying an
 * {@code IdTransferringMergeEventListener}.
 *
 * <p>Typically specified as entry for LocalSessionFactoryBean's "eventListeners"
 * map, with key "merge".
 *
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
@SuppressWarnings({"serial", "rawtypes"})
public class IdTransferringMergeEventListener extends DefaultMergeEventListener {

	/**
	 * Hibernate 3.1 implementation of ID transferral.
	 */
	@Override
	protected void entityIsTransient(MergeEvent event, Map copyCache) {
		super.entityIsTransient(event, copyCache);
		SessionImplementor session = event.getSession();
		EntityPersister persister = session.getEntityPersister(event.getEntityName(), event.getEntity());
		// Extract id from merged copy (which is currently registered with Session).
		Serializable id = persister.getIdentifier(event.getResult(), session.getEntityMode());
		// Set id on original object (which remains detached).
		persister.setIdentifier(event.getOriginal(), id, session.getEntityMode());
	}

}
