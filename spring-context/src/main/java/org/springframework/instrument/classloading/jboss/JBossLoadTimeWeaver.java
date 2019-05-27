package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JBoss可检测的ClassLoader的{@link LoadTimeWeaver}实现.
 * 在运行时自动检测特定的JBoss版本: 目前支持JBoss AS 6和7, 以及WildFly 8和9 (截至Spring 4.2).
 *
 * <p><b>NOTE:</b> 在JBoss 6上, 避免容器在应用程序实际启动之前加载类,
 * 需要将<tt>WEB-INF/jboss-scanning.xml</tt>文件添加到应用程序存档中 - 包含以下内容:
 * <pre class="code">&lt;scanning xmlns="urn:jboss:scanning:1.0"/&gt;</pre>
 *
 * <p>感谢Ales Justin和Marius Bogoevici的初始原型.
 */
public class JBossLoadTimeWeaver implements LoadTimeWeaver {

	private final JBossClassLoaderAdapter adapter;


	/**
	 * 使用默认的{@link ClassLoader class loader}.
	 */
	public JBossLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 要委托给用于织入的{@code ClassLoader} (must not be {@code null})
	 */
	public JBossLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		if (classLoader.getClass().getName().startsWith("org.jboss.modules")) {
			// JBoss AS 7 or WildFly
			this.adapter = new JBossModulesAdapter(classLoader);
		}
		else {
			// JBoss AS 6
			this.adapter = new JBossMCAdapter(classLoader);
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.adapter.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.adapter.getInstrumentableClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

}
