package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;

/**
 * 用于处理不同JBoss类加载器适配器的简单接口.
 */
interface JBossClassLoaderAdapter {

	void addTransformer(ClassFileTransformer transformer);

	ClassLoader getInstrumentableClassLoader();

}
