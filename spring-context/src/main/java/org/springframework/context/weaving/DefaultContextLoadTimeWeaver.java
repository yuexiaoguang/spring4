package org.springframework.context.weaving;

import java.lang.instrument.ClassFileTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver;
import org.springframework.instrument.classloading.glassfish.GlassFishLoadTimeWeaver;
import org.springframework.instrument.classloading.jboss.JBossLoadTimeWeaver;
import org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver;
import org.springframework.instrument.classloading.weblogic.WebLogicLoadTimeWeaver;
import org.springframework.instrument.classloading.websphere.WebSphereLoadTimeWeaver;

/**
 * 用于应用程序上下文的默认{@link LoadTimeWeaver} bean, 用于装饰自动检测到的内部 {@code LoadTimeWeaver}.
 *
 * <p>通常注册为默认bean名称 "{@code loadTimeWeaver}";
 * 实现这一目标最方便的方法是Spring的 {@code <context:load-time-weaver>} XML标签.
 *
 * <p>此类实现运行时环境检查以获取适当的weaver实现:
 * 从Spring 4.0开始, 它检测Oracle WebLogic 10, GlassFish 3, Tomcat 6, 7 和 8, JBoss AS 5, 6 和 7, IBM WebSphere 7 和 8,
 * {@link InstrumentationSavingAgent Spring's VM agent}, 以及Spring的{@link ReflectiveLoadTimeWeaver}支持的{@link ClassLoader}.
 */
public class DefaultContextLoadTimeWeaver implements LoadTimeWeaver, BeanClassLoaderAware, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private LoadTimeWeaver loadTimeWeaver;


	public DefaultContextLoadTimeWeaver() {
	}

	public DefaultContextLoadTimeWeaver(ClassLoader beanClassLoader) {
		setBeanClassLoader(beanClassLoader);
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		LoadTimeWeaver serverSpecificLoadTimeWeaver = createServerSpecificLoadTimeWeaver(classLoader);
		if (serverSpecificLoadTimeWeaver != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Determined server-specific load-time weaver: " +
						serverSpecificLoadTimeWeaver.getClass().getName());
			}
			this.loadTimeWeaver = serverSpecificLoadTimeWeaver;
		}
		else if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			logger.info("Found Spring's JVM agent for instrumentation");
			this.loadTimeWeaver = new InstrumentationLoadTimeWeaver(classLoader);
		}
		else {
			try {
				this.loadTimeWeaver = new ReflectiveLoadTimeWeaver(classLoader);
				if (logger.isInfoEnabled()) {
					logger.info("Using a reflective load-time weaver for class loader: " +
							this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
				}
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException(ex.getMessage() + " Specify a custom LoadTimeWeaver or start your " +
						"Java virtual machine with Spring's agent: -javaagent:org.springframework.instrument.jar");
			}
		}
	}

	/*
	 * 此方法永远不会失败, 允许尝试其他可能的方法来使用与服务器无关的织入器.
	 * 这种非错误逻辑是必需的, 因为仅基于ClassLoader名称确定加载时织入器, 可能由于其他不匹配而合法地失败.
	 * 具体案例:
	 * WebLogicLoadTimeWeaver的使用适用于WLS 10, 但由于缺少针对早期版本的特定方法 (addInstanceClassPreProcessor)而失败,
	 * 即使ClassLoader名称相同.
	 */
	protected LoadTimeWeaver createServerSpecificLoadTimeWeaver(ClassLoader classLoader) {
		String name = classLoader.getClass().getName();
		try {
			if (name.startsWith("org.apache.catalina")) {
				return new TomcatLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.glassfish")) {
				return new GlassFishLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("org.jboss")) {
				return new JBossLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("com.ibm")) {
				return new WebSphereLoadTimeWeaver(classLoader);
			}
			else if (name.startsWith("weblogic")) {
				return new WebLogicLoadTimeWeaver(classLoader);
			}
		}
		catch (Exception ex) {
			if (logger.isInfoEnabled()) {
				logger.info("Could not obtain server-specific LoadTimeWeaver: " + ex.getMessage());
			}
		}
		return null;
	}

	@Override
	public void destroy() {
		if (this.loadTimeWeaver instanceof InstrumentationLoadTimeWeaver) {
			if (logger.isInfoEnabled()) {
				logger.info("Removing all registered transformers for class loader: " +
						this.loadTimeWeaver.getInstrumentableClassLoader().getClass().getName());
			}
			((InstrumentationLoadTimeWeaver) this.loadTimeWeaver).removeTransformers();
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.loadTimeWeaver.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.loadTimeWeaver.getInstrumentableClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return this.loadTimeWeaver.getThrowawayClassLoader();
	}

}
