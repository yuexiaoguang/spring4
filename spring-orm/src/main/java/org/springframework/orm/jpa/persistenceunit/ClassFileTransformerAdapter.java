package org.springframework.orm.jpa.persistenceunit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.persistence.spi.ClassTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Simple adapter that implements the {@code java.lang.instrument.ClassFileTransformer}
 * interface based on a JPA {@code ClassTransformer} which a JPA PersistenceProvider
 * asks the {@code PersistenceUnitInfo} to install in the current runtime.
 */
class ClassFileTransformerAdapter implements ClassFileTransformer {

	private static final Log logger = LogFactory.getLog(ClassFileTransformerAdapter.class);


	private final ClassTransformer classTransformer;

	private boolean currentlyTransforming = false;


	public ClassFileTransformerAdapter(ClassTransformer classTransformer) {
		Assert.notNull(classTransformer, "ClassTransformer must not be null");
		this.classTransformer = classTransformer;
	}


	@Override
	public byte[] transform(
			ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {

		synchronized (this) {
			if (this.currentlyTransforming) {
				// Defensively back out when called from within the transform delegate below:
				// in particular, for the over-eager transformer implementation in Hibernate 5.
				return null;
			}

			this.currentlyTransforming = true;
			try {
				byte[] transformed = this.classTransformer.transform(
						loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
				if (transformed != null && logger.isDebugEnabled()) {
					logger.debug("Transformer of class [" + this.classTransformer.getClass().getName() +
							"] transformed class [" + className + "]; bytes in=" +
							classfileBuffer.length + "; bytes out=" + transformed.length);
				}
				return transformed;
			}
			catch (ClassCircularityError ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Circularity error while weaving class [" + className + "] with " +
							"transformer of class [" + this.classTransformer.getClass().getName() + "]", ex);
				}
				throw new IllegalStateException("Failed to weave class [" + className + "]", ex);
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Error weaving class [" + className + "] with transformer of class [" +
							this.classTransformer.getClass().getName() + "]", ex);
				}
				// The exception will be ignored by the class loader, anyway...
				throw new IllegalStateException("Could not weave class [" + className + "]", ex);
			}
			finally {
				this.currentlyTransforming = false;
			}
		}
	}


	@Override
	public String toString() {
		return "Standard ClassFileTransformer wrapping JPA transformer: " + this.classTransformer;
	}

}
