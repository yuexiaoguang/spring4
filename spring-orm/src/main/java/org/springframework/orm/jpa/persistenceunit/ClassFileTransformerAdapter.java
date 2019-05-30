package org.springframework.orm.jpa.persistenceunit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.persistence.spi.ClassTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * 简单适配器, 实现{@code java.lang.instrument.ClassFileTransformer}接口,
 * 基于JPA {@code ClassTransformer}, JPA PersistenceProvider要求{@code PersistenceUnitInfo}在当前运行时安装.
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
				// 当从下面的变换委托中调用时, 防御性地退出:
				// 特别是对于Hibernate 5中过度实时的变换器实现.
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
				// 无论如何, 类加载器将忽略该异常...
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
