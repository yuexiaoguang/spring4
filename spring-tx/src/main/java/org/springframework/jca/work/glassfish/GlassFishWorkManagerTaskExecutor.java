package org.springframework.jca.work.glassfish;

import java.lang.reflect.Method;
import javax.resource.spi.work.WorkManager;

import org.springframework.jca.work.WorkManagerTaskExecutor;
import org.springframework.util.ReflectionUtils;

/**
 * 用于GlassFish JCA WorkManager的Spring TaskExecutor适配器.
 * 可以在Web应用程序中定义以使TaskExecutor引用可用, 与下面的GlassFish WorkManager(线程池)通信.
 *
 * <p>这是用于WebLogic和WebSphere的
 * CommonJ {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor}适配器的GlassFish等价物.
 *
 * <p>Note: 在GlassFish 4及更高版本上, 在Java EE 7中支持JSR-236后,
 * 应首选{@link org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor}.
 */
public class GlassFishWorkManagerTaskExecutor extends WorkManagerTaskExecutor {

	private static final String WORK_MANAGER_FACTORY_CLASS = "com.sun.enterprise.connectors.work.WorkManagerFactory";

	private final Method getWorkManagerMethod;


	public GlassFishWorkManagerTaskExecutor() {
		try {
			Class<?> wmf = getClass().getClassLoader().loadClass(WORK_MANAGER_FACTORY_CLASS);
			this.getWorkManagerMethod = wmf.getMethod("getWorkManager", String.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFishWorkManagerTaskExecutor because GlassFish API is not available", ex);
		}
	}

	/**
	 * 标识要与之通信的特定GlassFish线程池.
	 * <p>线程池名称与默认RAR部署场景中的资源适配器名称匹配.
	 */
	public void setThreadPoolName(String threadPoolName) {
		WorkManager wm = (WorkManager) ReflectionUtils.invokeMethod(this.getWorkManagerMethod, null, threadPoolName);
		if (wm == null) {
			throw new IllegalArgumentException("Specified thread pool name '" + threadPoolName +
					"' does not correspond to an actual pool definition in GlassFish. Check your configuration!");
		}
		setWorkManager(wm);
	}

	/**
	 * 获得GlassFish的默认线程池.
	 */
	@Override
	protected WorkManager getDefaultWorkManager() {
		return (WorkManager) ReflectionUtils.invokeMethod(this.getWorkManagerMethod, null, new Object[] {null});
	}

}
