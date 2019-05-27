package org.springframework.scheduling.quartz;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.CollectionUtils;

/**
 * 创建和配置Quartz {@link org.quartz.Scheduler}的{@link FactoryBean},
 * 将其生命周期作为Spring应用程序上下文的一部分进行管理, 并将Scheduler作为bean引用公开给依赖注入.
 *
 * <p>允许注册JobDetails, Calendars和Triggers, 在初始化时自动启动调度器, 并在销毁时将其关闭.
 * 在只需要在启动时静态注册作业的场景中, 无需在应用程序代码中访问Scheduler实例本身.
 *
 * <p>要在运行时动态注册作业, 请使用对此SchedulerFactoryBean的bean引用以直接访问Quartz Scheduler ({@code org.quartz.Scheduler}).
 * 这允许创建新的作业和触发器, 以及控制和监视整个调度器.
 *
 * <p>请注意, Quartz为每次执行实例化一个新Job, 而Timer则使用在重复执行之间共享的TimerTask实例.
 * 只是JobDetail描述符是共享的.
 *
 * <p>使用持久性作业时, 强烈建议在Spring管理(或纯JTA)事务中对Scheduler执行所有操作.
 * 否则, 数据库锁将无法正常工作, 甚至可能会中断. (See {@link #setDataSource setDataSource} javadoc for details.)
 *
 * <p>实现事务执行的首选方法是, 在业务外观级别划分声明式事务, 这将自动应用于在这些范围内执行的调度器操作.
 * 或者, 可以为Scheduler本身添加事务增强.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public class SchedulerFactoryBean extends SchedulerAccessor implements FactoryBean<Scheduler>,
		BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle {

	public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";

	public static final int DEFAULT_THREAD_COUNT = 10;


	private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder =
			new ThreadLocal<ResourceLoader>();

	private static final ThreadLocal<Executor> configTimeTaskExecutorHolder =
			new ThreadLocal<Executor>();

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder =
			new ThreadLocal<DataSource>();

	private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder =
			new ThreadLocal<DataSource>();

	/**
	 * 返回当前配置的Quartz Scheduler的{@link ResourceLoader}, 供{@link ResourceLoaderClassLoadHelper}使用.
	 * <p>此实例将在相应Scheduler初始化之前设置, 并在之后立即重置.
	 * 因此仅在配置期间可用.
	 */
	public static ResourceLoader getConfigTimeResourceLoader() {
		return configTimeResourceLoaderHolder.get();
	}

	/**
	 * 返回当前配置的Quartz Scheduler的{@link Executor}, 供{@link LocalTask​​ExecutorThreadPool}使用.
	 * <p>此实例将在相应Scheduler初始化之前设置, 并在之后立即重置.
	 * 因此仅在配置期间可用.
	 */
	public static Executor getConfigTimeTaskExecutor() {
		return configTimeTaskExecutorHolder.get();
	}

	/**
	 * 返回当前配置的Quartz Scheduler的{@link DataSource}, 供{@link LocalDataSourceJobStore}使用.
	 * <p>此实例将在相应Scheduler初始化之前设置, 并在之后立即重置.
	 * 因此仅在配置期间可用.
	 */
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * 返回当前配置的Quartz Scheduler的非事务性{@link DataSource}, 供{@link LocalDataSourceJobStore}使用.
	 * <p>此实例将在相应Scheduler初始化之前设置, 并在之后立即重置.
	 * 因此仅在配置期间可用.
	 */
	public static DataSource getConfigTimeNonTransactionalDataSource() {
		return configTimeNonTransactionalDataSourceHolder.get();
	}


	private SchedulerFactory schedulerFactory;

	private Class<? extends SchedulerFactory> schedulerFactoryClass = StdSchedulerFactory.class;

	private String schedulerName;

	private Resource configLocation;

	private Properties quartzProperties;

	private Executor taskExecutor;

	private DataSource dataSource;

	private DataSource nonTransactionalDataSource;

    private Map<String, ?> schedulerContextMap;

	private ApplicationContext applicationContext;

	private String applicationContextSchedulerContextKey;

	private JobFactory jobFactory;

	private boolean jobFactorySet = false;

	private boolean autoStartup = true;

	private int startupDelay = 0;

	private int phase = Integer.MAX_VALUE;

	private boolean exposeSchedulerInRepository = false;

	private boolean waitForJobsToCompleteOnShutdown = false;

	private Scheduler scheduler;


	/**
	 * 设置要使用的外部Quartz {@link SchedulerFactory}实例.
	 * <p>默认是内部{@link StdSchedulerFactory}实例.
	 * 如果调用此方法, 它将覆盖通过{@link #setSchedulerFactoryClass}指定的类,
	 * 以及通过{@link #setConfigLocation}, {@link #setQuartzProperties},
	 * {@link #setTaskExecutor}, {@link #setDataSource}指定的设置.
	 * <p><b>NOTE:</b> 使用外部提供的{@code SchedulerFactory}实例,
	 * 在{@code SchedulerFactoryBean}中将忽略{@link #setConfigLocation}或{@link #setQuartzProperties}等本地设置,
	 * 期望外部{@code SchedulerFactory}实例自己初始化.
	 */
	public void setSchedulerFactory(SchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
	}

	/**
	 * 设置要使用的Quartz {@link SchedulerFactory}实现.
	 * <p>默认是 {@link StdSchedulerFactory}类, 从{@code quartz.jar}读取标准{@code quartz.properties}.
	 * 要应用自定义Quartz属性, 请在此本地{@code SchedulerFactoryBean}实例上指定
	 * {@link #setConfigLocation "configLocation"}和/或{@link #setQuartzProperties "quartzProperties"}等.
	 */
	public void setSchedulerFactoryClass(Class<? extends SchedulerFactory> schedulerFactoryClass) {
		this.schedulerFactoryClass = schedulerFactoryClass;
	}

	/**
	 * 设置要通过SchedulerFactory创建的Scheduler的名称.
	 * <p>如果未指定, 则bean名称将用作默认调度器名称.
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * 设置Quartz属性配置文件的位置, 例如classpath资源"classpath:quartz.properties".
	 * <p>Note: 当通过此bean在本地指定所有必需的属性时, 或者依赖于Quartz的默认配置时, 可以省略.
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置Quartz属性, 例如"org.quartz.threadPool.class".
	 * <p>可用于覆盖Quartz属性配置文件中的值, 或在本地指定所有必需的属性.
	 */
	public void setQuartzProperties(Properties quartzProperties) {
		this.quartzProperties = quartzProperties;
	}

	/**
	 * 设置一个Spring管理的{@link Executor}作为Quartz后端.
	 * 通过Quartz SPI作为线程池公开.
	 * <p>可用于将本地JDK ThreadPoolExecutor或CommonJ WorkManager指定为Quartz后端, 以避免Quartz的手动创建线程.
	 * <p>默认情况下, 将使用Quartz SimpleThreadPool, 通过相应的Quartz属性进行配置.
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 设置Scheduler使用的默认{@link DataSource}.
	 * 如果设置, 这将覆盖Quartz属性中的相应设置.
	 * <p>Note: 如果设置了此项, 则Quartz设置不应定义作业存储"dataSource"以避免无意义的双重配置.
	 * <p>将使用Quartz的JobStoreCMT的Spring特定子类.
	 * 因此, 强烈建议在Spring管理(或普通JTA)事务中对Scheduler执行所有操作.
	 * 否则, 数据库锁将无法正常工作, 甚至可能中断 (e.g. 如果尝试在没有事务的情况下获取对Oracle的锁).
	 * <p>支持事务和非事务DataSource访问.
	 * 使用非XA DataSource和本地Spring事务, 单个DataSource参数就足够了.
	 * 对于XA DataSource和全局JTA事务, 应设置SchedulerFactoryBean的"nonTransactionalDataSource"属性,
	 * 传入不参与全局事务的非XA DataSource.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 将{@link DataSource}设置为<i>用于非事务性访问</i>.
	 * <p>仅当默认DataSource是始终参与事务的XA DataSource时, 才需要这样做:
	 * 在这种情况下, 应将该DataSource的非XA版本指定为"nonTransactionalDataSource".
	 * <p>这与本地DataSource实例和Spring事务无关.
	 * 将单个默认DataSource指定为"dataSource"就足够了.
	 */
	public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource) {
		this.nonTransactionalDataSource = nonTransactionalDataSource;
	}

	/**
	 * 通过给定的Map在Scheduler上下文中注册对象.
	 * 这些对象可用于在此Scheduler中运行的任何作业.
	 * <p>Note: 使用其JobDetail保存在数据库中的持久作业时,
	 * 不要将Spring管理的bean或ApplicationContext引用放入JobDataMap, 而是放入SchedulerContext.
	 * 
	 * @param schedulerContextAsMap 任何对象作为值(例如Spring管理的bean)
	 */
	public void setSchedulerContextAsMap(Map<String, ?> schedulerContextAsMap) {
		this.schedulerContextMap = schedulerContextAsMap;
	}

	/**
	 * 设置{@link ApplicationContext}引用的键, 以在SchedulerContext中公开, 例如"applicationContext".
	 * 默认无. 仅适用于在Spring ApplicationContext中运行时.
	 * <p>Note: 使用其JobDetail保存在数据库中的持久作业时, 不要将ApplicationContext引用放入JobDataMap, 而是放入SchedulerContext.
	 * <p>对于QuartzJobBean, 引用将作为bean属性应用于Job实例.
	 * "applicationContext"属性将对应于该场景中的"setApplicationContext"方法.
	 * <p>请注意, 像ApplicationContextAware这样的BeanFactory回调接口不会自动应用于Quartz Job实例, 因为Quartz本身负责其作业的生命周期.
	 */
	public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey) {
		this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
	}

	/**
	 * 设置用于此Scheduler的Quartz {@link JobFactory}.
	 * <p>默认是Spring的{@link AdaptableJobFactory}, 它支持{@link java.lang.Runnable}对象, 以及标准的Quartz {@link org.quartz.Job}实例.
	 * 请注意, 此默认值仅适用于<<i>本地</i> Scheduler, 而不适用于RemoteScheduler (Quartz不支持设置自定义JobFactory).
	 * <p>在此处指定Spring的{@link SpringBeanJobFactory}实例 (通常作为内部bean定义),
	 * 以从指定的作业数据映射和调度程序上下文自动填充作业的bean属性.
	 */
	public void setJobFactory(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.jobFactorySet = true;
	}

	/**
	 * 设置是否在初始化后自动启动调度器.
	 * <p>默认"true"; 设置为"false"以允许手动启动.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * 返回此调度器是否配置为自动启动.
	 * 如果为"true", 则调度器将在刷新上下文之后, 以及启动延迟之后, 启动.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定应启动和停止此调度器的阶段.
	 * 启动顺序从最低到最高, 关闭顺序与此相反.
	 * 默认情况下, 此值为{@code Integer.MAX_VALUE}, 表示此调度器尽可能晚启动, 并尽快停止.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回应启动和停止此调度器的阶段.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * 设置在异步启动调度器之前, 初始化之后等待的秒数.
	 * 默认为 0, 表示在初始化此bean时立即同步启动.
	 * <p>如果在整个应用程序启动之前不应运行任何作业, 则将此值设置为10或20秒.
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * 设置是否在Quartz {@link SchedulerRepository}中公开Spring管理的{@link Scheduler}实例.
	 * 默认为"false", 因为Spring管理的Scheduler通常专门用于在Spring上下文中进行访问.
	 * <p>将此标志切换为"true"以便全局公开Scheduler.
	 * 除非有一个依赖于此行为的现有Spring应用程序, 否则不建议这样做.
	 * 请注意, 此类全局公开是早期Spring版本中的意外默认值; 从Spring 2.5.6开始修复了这个问题.
	 */
	public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository) {
		this.exposeSchedulerInRepository = exposeSchedulerInRepository;
	}

	/**
	 * 设置关闭时是否等待运行的作业完成.
	 * <p>默认"false".
	 * 如果希望以更长的关闭阶段为代价完全完成作业, 请将其切换为"true".
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void setBeanName(String name) {
		if (this.schedulerName == null) {
			this.schedulerName = name;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	//---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.dataSource == null && this.nonTransactionalDataSource != null) {
			this.dataSource = this.nonTransactionalDataSource;
		}

		if (this.applicationContext != null && this.resourceLoader == null) {
			this.resourceLoader = this.applicationContext;
		}

		// Initialize the Scheduler instance...
		this.scheduler = prepareScheduler(prepareSchedulerFactory());
		try {
			registerListeners();
			registerJobsAndTriggers();
		}
		catch (Exception ex) {
			try {
				this.scheduler.shutdown(true);
			}
			catch (Exception ex2) {
				logger.debug("Scheduler shutdown exception after registration failure", ex2);
			}
			throw ex;
		}
	}


	/**
	 * 如有必要, 创建一个SchedulerFactory, 并将本地定义的Quartz属性应用于它.
	 * 
	 * @return 初始化后的SchedulerFactory
	 */
	private SchedulerFactory prepareSchedulerFactory() throws SchedulerException, IOException {
		SchedulerFactory schedulerFactory = this.schedulerFactory;
		if (schedulerFactory == null) {
			// Create local SchedulerFactory instance (typically a StdSchedulerFactory)
			schedulerFactory = BeanUtils.instantiateClass(this.schedulerFactoryClass);
			if (schedulerFactory instanceof StdSchedulerFactory) {
				initSchedulerFactory((StdSchedulerFactory) schedulerFactory);
			}
			else if (this.configLocation != null || this.quartzProperties != null ||
					this.taskExecutor != null || this.dataSource != null) {
				throw new IllegalArgumentException(
						"StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
			}
			// 否则, 不通过StdSchedulerFactory.initialize(Properties)应用本地设置
		}
		// 否则, 假设已使用适当的设置初始化外部提供的工厂
		return schedulerFactory;
	}

	/**
	 * 初始化给定的SchedulerFactory, 将本地定义的Quartz属性应用于它.
	 * 
	 * @param schedulerFactory 要初始化的SchedulerFactory
	 */
	private void initSchedulerFactory(StdSchedulerFactory schedulerFactory) throws SchedulerException, IOException {
		Properties mergedProps = new Properties();
		if (this.resourceLoader != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
					ResourceLoaderClassLoadHelper.class.getName());
		}

		if (this.taskExecutor != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
					LocalTaskExecutorThreadPool.class.getName());
		}
		else {
			// 在此处设置必要的默认属性, 因为Quartz在显式指定属性时不会应用其默认配置.
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
			mergedProps.setProperty(PROP_THREAD_COUNT, Integer.toString(DEFAULT_THREAD_COUNT));
		}

		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading Quartz config from [" + this.configLocation + "]");
			}
			PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
		}

		CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
		if (this.dataSource != null) {
			mergedProps.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
		}
		if (this.schedulerName != null) {
			mergedProps.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.schedulerName);
		}

		schedulerFactory.initialize(mergedProps);
	}

	private Scheduler prepareScheduler(SchedulerFactory schedulerFactory) throws SchedulerException {
		if (this.resourceLoader != null) {
			// 为SchedulerFactory配置提供给定的ResourceLoader.
			configTimeResourceLoaderHolder.set(this.resourceLoader);
		}
		if (this.taskExecutor != null) {
			// 使给定的TaskExecutor可用于SchedulerFactory配置.
			configTimeTaskExecutorHolder.set(this.taskExecutor);
		}
		if (this.dataSource != null) {
			// 使给定的DataSource可用于SchedulerFactory配置.
			configTimeDataSourceHolder.set(this.dataSource);
		}
		if (this.nonTransactionalDataSource != null) {
			// 为SchedulerFactory配置提供给定的非事务性DataSource.
			configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
		}

		// 从SchedulerFactory获取Scheduler实例.
		try {
			Scheduler scheduler = createScheduler(schedulerFactory, this.schedulerName);
			populateSchedulerContext(scheduler);

			if (!this.jobFactorySet && !(scheduler instanceof RemoteScheduler)) {
				// 使用AdaptableJobFactory作为本地Scheduler的默认值, 除非通过"jobFactory" bean属性显式指定null值.
				this.jobFactory = new AdaptableJobFactory();
			}
			if (this.jobFactory != null) {
				if (this.jobFactory instanceof SchedulerContextAware) {
					((SchedulerContextAware) this.jobFactory).setSchedulerContext(scheduler.getContext());
				}
				scheduler.setJobFactory(this.jobFactory);
			}
			return scheduler;
		}

		finally {
			if (this.resourceLoader != null) {
				configTimeResourceLoaderHolder.remove();
			}
			if (this.taskExecutor != null) {
				configTimeTaskExecutorHolder.remove();
			}
			if (this.dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.nonTransactionalDataSource != null) {
				configTimeNonTransactionalDataSourceHolder.remove();
			}
		}
	}

	/**
	 * 为给定的工厂和调度器名称创建Scheduler实例.
	 * 由{@link #afterPropertiesSet}调用.
	 * <p>默认实现调用SchedulerFactory的{@code getScheduler}方法.
	 * 可以重写以自定义Scheduler的创建.
	 * 
	 * @param schedulerFactory 用来创建Scheduler的工厂
	 * @param schedulerName 要创建的调度器的名称
	 * 
	 * @return Scheduler实例
	 * @throws SchedulerException 如果由Quartz方法抛出
	 */
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName)
			throws SchedulerException {

		// 覆盖线程上下文ClassLoader, 以解决Quartz ClassLoadHelper的加载问题.
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader = (this.resourceLoader != null &&
				this.resourceLoader.getClassLoader() != threadContextClassLoader);
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.resourceLoader.getClassLoader());
		}
		try {
			SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				Scheduler existingScheduler = (schedulerName != null ? repository.lookup(schedulerName) : null);
				Scheduler newScheduler = schedulerFactory.getScheduler();
				if (newScheduler == existingScheduler) {
					throw new IllegalStateException("Active Scheduler of name '" + schedulerName + "' already registered " +
							"in Quartz SchedulerRepository. Cannot create a new Spring-managed Scheduler of the same name!");
				}
				if (!this.exposeSchedulerInRepository) {
					// 在这种情况下需要删除它, 因为Quartz默认共享Scheduler实例!
					SchedulerRepository.getInstance().remove(newScheduler.getSchedulerName());
				}
				return newScheduler;
			}
		}
		finally {
			if (overrideClassLoader) {
				// 重置原始线程上下文ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * 在Quartz SchedulerContext中公开指定的上下文属性和/或当前的ApplicationContext.
	 */
	private void populateSchedulerContext(Scheduler scheduler) throws SchedulerException {
		// 将指定的对象放入Scheduler上下文中.
		if (this.schedulerContextMap != null) {
			scheduler.getContext().putAll(this.schedulerContextMap);
		}

		// 在Scheduler上下文中注册ApplicationContext.
		if (this.applicationContextSchedulerContextKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
					"SchedulerFactoryBean needs to be set up in an ApplicationContext " +
					"to be able to handle an 'applicationContextSchedulerContextKey'");
			}
			scheduler.getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
		}
	}


	/**
	 * 启动Quartz Scheduler, 遵循"startupDelay"设置.
	 * 
	 * @param scheduler 要启动的Scheduler
	 * @param startupDelay 异步启动Scheduler之前等待的秒数
	 */
	protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {
		if (startupDelay <= 0) {
			logger.info("Starting Quartz Scheduler now");
			scheduler.start();
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() +
						"] in " + startupDelay + " seconds");
			}
			// 不使用Quartz startDelayed方法, 因为我们在这里显式地想要一个守护进程线程, 而不是在所有其他线程结束时让JVM继续活着.
			Thread schedulerThread = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(startupDelay * 1000);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						// simply proceed
					}
					if (logger.isInfoEnabled()) {
						logger.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
					}
					try {
						scheduler.start();
					}
					catch (SchedulerException ex) {
						throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
					}
				}
			};
			schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
			schedulerThread.setDaemon(true);
			schedulerThread.start();
		}
	}


	//---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	//---------------------------------------------------------------------

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public Scheduler getObject() {
		return this.scheduler;
	}

	@Override
	public Class<? extends Scheduler> getObjectType() {
		return (this.scheduler != null ? this.scheduler.getClass() : Scheduler.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// Implementation of SmartLifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				startScheduler(this.scheduler, this.startupDelay);
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not start Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				this.scheduler.standby();
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not stop Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop(Runnable callback) throws SchedulingException {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				return !this.scheduler.isInStandbyMode();
			}
			catch (SchedulerException ex) {
				return false;
			}
		}
		return false;
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * 在bean工厂关闭时关闭Quartz调度器, 停止所有计划的作业.
	 */
	@Override
	public void destroy() throws SchedulerException {
		if (this.scheduler != null) {
			logger.info("Shutting down Quartz Scheduler");
			this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
		}
	}
}
