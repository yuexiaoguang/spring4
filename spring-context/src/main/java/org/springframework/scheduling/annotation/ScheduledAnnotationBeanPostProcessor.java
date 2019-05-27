package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Bean后处理器, 根据通过注解提供的"fixedRate", "fixedDelay", "cron"表达式,
 * 注册由{@link org.springframework.scheduling.TaskScheduler}调用带@{@link Scheduled}注解的方法.
 *
 * <p>这个后处理器由Spring的{@code <task:annotation-driven>} XML元素以及{@link EnableScheduling @EnableScheduling}注解自动注册.
 *
 * <p>自动检测容器中的{@link SchedulingConfigurer}实例, 允许自定义要使用的调度器,
 * 或对任务注册进行细粒度控制 (e.g. 注册{@link Trigger}任务).
 * See the @{@link EnableScheduling} javadocs for complete usage details.
 */
public class ScheduledAnnotationBeanPostProcessor
		implements MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor,
		Ordered, EmbeddedValueResolverAware, BeanNameAware, BeanFactoryAware, ApplicationContextAware,
		SmartInitializingSingleton, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	/**
	 * 要选择的{@link TaskScheduler} bean的默认名称: {@value}.
	 * <p>请注意, 初始查找按类型进行; 这只是在上下文中找到多个调度器bean的情况下的后备.
	 */
	public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";


	protected final Log logger = LogFactory.getLog(getClass());

	private Object scheduler;

	private StringValueResolver embeddedValueResolver;

	private String beanName;

	private BeanFactory beanFactory;

	private ApplicationContext applicationContext;

	private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));

	private final Map<Object, Set<ScheduledTask>> scheduledTasks =
			new IdentityHashMap<Object, Set<ScheduledTask>>(16);


	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * 设置将调用定时方法的{@link org.springframework.scheduling.TaskScheduler},
	 * 或将{@link java.util.concurrent.ScheduledExecutorService}包装为TaskScheduler.
	 * <p>如果未指定, 则将应用默认调度器解析:
	 * 在上下文中搜索唯一的{@link TaskScheduler}, 或者在名为"taskScheduler"的{@link TaskScheduler} bean中搜索;
	 * 同样的查找也将针对{@link ScheduledExecutorService} bean执行.
	 * 如果两者都不可解析, 则将在注册器中创建本地单线程默认调度器.
	 */
	public void setScheduler(Object scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 使{@link BeanFactory}可用是可选的;
	 * 如果没有设置, {@link SchedulingConfigurer} bean将不会被自动检测, 并且必须显式配置{@link #setScheduler scheduler}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 设置{@link ApplicationContext}是可选的:
	 * 如果设置, 则将在{@link ContextRefreshedEvent}阶段激活已注册的任务;
	 * 如果没有设置, 它将发生在{@link #afterSingletonsInstantiated}时间.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (this.beanFactory == null) {
			this.beanFactory = applicationContext;
		}
	}


	@Override
	public void afterSingletonsInstantiated() {
		// 从缓存中删除已解析的单例类
		this.nonAnnotatedClasses.clear();

		if (this.applicationContext == null) {
			// 没有在ApplicationContext中运行 -> 尽早注册任务...
			finishRegistration();
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			// 在ApplicationContext中运行 -> 这么晚才注册任务...
			// 为其他ContextRefreshedEvent监听器提供同时执行其工作的机会 (e.g. Spring Batch的工作注册).
			finishRegistration();
		}
	}

	private void finishRegistration() {
		if (this.scheduler != null) {
			this.registrar.setScheduler(this.scheduler);
		}

		if (this.beanFactory instanceof ListableBeanFactory) {
			Map<String, SchedulingConfigurer> beans =
					((ListableBeanFactory) this.beanFactory).getBeansOfType(SchedulingConfigurer.class);
			List<SchedulingConfigurer> configurers = new ArrayList<SchedulingConfigurer>(beans.values());
			AnnotationAwareOrderComparator.sort(configurers);
			for (SchedulingConfigurer configurer : configurers) {
				configurer.configureTasks(this.registrar);
			}
		}

		if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set to find scheduler by type");
			try {
				// Search for TaskScheduler bean...
				this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, false));
			}
			catch (NoUniqueBeanDefinitionException ex) {
				logger.debug("Could not find unique TaskScheduler bean", ex);
				try {
					this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, true));
				}
				catch (NoSuchBeanDefinitionException ex2) {
					if (logger.isInfoEnabled()) {
						logger.info("More than one TaskScheduler bean exists within the context, and " +
								"none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
								"(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
								"ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
								ex.getBeanNamesFound());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				logger.debug("Could not find default TaskScheduler bean", ex);
				// Search for ScheduledExecutorService bean next...
				try {
					this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, false));
				}
				catch (NoUniqueBeanDefinitionException ex2) {
					logger.debug("Could not find unique ScheduledExecutorService bean", ex2);
					try {
						this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, true));
					}
					catch (NoSuchBeanDefinitionException ex3) {
						if (logger.isInfoEnabled()) {
							logger.info("More than one ScheduledExecutorService bean exists within the context, and " +
									"none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
									"(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
									"ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
									ex2.getBeanNamesFound());
						}
					}
				}
				catch (NoSuchBeanDefinitionException ex2) {
					logger.debug("Could not find default ScheduledExecutorService bean", ex2);
					// Giving up -> 回退到注册商中的默认调度器...
					logger.info("No TaskScheduler/ScheduledExecutorService bean found for scheduled processing");
				}
			}
		}

		this.registrar.afterPropertiesSet();
	}

	private <T> T resolveSchedulerBean(Class<T> schedulerType, boolean byName) {
		if (byName) {
			T scheduler = this.beanFactory.getBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, schedulerType);
			if (this.beanFactory instanceof ConfigurableBeanFactory) {
				((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
						DEFAULT_TASK_SCHEDULER_BEAN_NAME, this.beanName);
			}
			return scheduler;
		}
		else if (this.beanFactory instanceof AutowireCapableBeanFactory) {
			NamedBeanHolder<T> holder = ((AutowireCapableBeanFactory) this.beanFactory).resolveNamedBean(schedulerType);
			if (this.beanFactory instanceof ConfigurableBeanFactory) {
				((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
						holder.getBeanName(), this.beanName);
			}
			return holder.getBeanInstance();
		}
		else {
			return this.beanFactory.getBean(schedulerType);
		}
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) {
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
		if (!this.nonAnnotatedClasses.contains(targetClass)) {
			Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
					new MethodIntrospector.MetadataLookup<Set<Scheduled>>() {
						@Override
						public Set<Scheduled> inspect(Method method) {
							Set<Scheduled> scheduledMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
									method, Scheduled.class, Schedules.class);
							return (!scheduledMethods.isEmpty() ? scheduledMethods : null);
						}
					});
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(targetClass);
				if (logger.isTraceEnabled()) {
					logger.trace("No @Scheduled annotations found on bean class: " + bean.getClass());
				}
			}
			else {
				// Non-empty set of methods
				for (Map.Entry<Method, Set<Scheduled>> entry : annotatedMethods.entrySet()) {
					Method method = entry.getKey();
					for (Scheduled scheduled : entry.getValue()) {
						processScheduled(scheduled, method, bean);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName +
							"': " + annotatedMethods);
				}
			}
		}
		return bean;
	}

	protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
		try {
			Assert.isTrue(method.getParameterTypes().length == 0,
					"Only no-arg methods may be annotated with @Scheduled");

			Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
			Runnable runnable = new ScheduledMethodRunnable(bean, invocableMethod);
			boolean processedSchedule = false;
			String errorMessage =
					"Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

			Set<ScheduledTask> tasks = new LinkedHashSet<ScheduledTask>(4);

			// Determine initial delay
			long initialDelay = scheduled.initialDelay();
			String initialDelayString = scheduled.initialDelayString();
			if (StringUtils.hasText(initialDelayString)) {
				Assert.isTrue(initialDelay < 0, "Specify 'initialDelay' or 'initialDelayString', not both");
				if (this.embeddedValueResolver != null) {
					initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
				}
				try {
					initialDelay = Long.parseLong(initialDelayString);
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException(
							"Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into integer");
				}
			}

			// Check cron expression
			String cron = scheduled.cron();
			if (StringUtils.hasText(cron)) {
				Assert.isTrue(initialDelay == -1, "'initialDelay' not supported for cron triggers");
				processedSchedule = true;
				String zone = scheduled.zone();
				if (this.embeddedValueResolver != null) {
					cron = this.embeddedValueResolver.resolveStringValue(cron);
					zone = this.embeddedValueResolver.resolveStringValue(zone);
				}
				TimeZone timeZone;
				if (StringUtils.hasText(zone)) {
					timeZone = StringUtils.parseTimeZoneString(zone);
				}
				else {
					timeZone = TimeZone.getDefault();
				}
				tasks.add(this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(cron, timeZone))));
			}

			// 此时, 我们不再需要区分是否是初始延迟设置
			if (initialDelay < 0) {
				initialDelay = 0;
			}

			// Check fixed delay
			long fixedDelay = scheduled.fixedDelay();
			if (fixedDelay >= 0) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				tasks.add(this.registrar.scheduleFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay)));
			}
			String fixedDelayString = scheduled.fixedDelayString();
			if (StringUtils.hasText(fixedDelayString)) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				if (this.embeddedValueResolver != null) {
					fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
				}
				try {
					fixedDelay = Long.parseLong(fixedDelayString);
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException(
							"Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into integer");
				}
				tasks.add(this.registrar.scheduleFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay)));
			}

			// Check fixed rate
			long fixedRate = scheduled.fixedRate();
			if (fixedRate >= 0) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				tasks.add(this.registrar.scheduleFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay)));
			}
			String fixedRateString = scheduled.fixedRateString();
			if (StringUtils.hasText(fixedRateString)) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				if (this.embeddedValueResolver != null) {
					fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
				}
				try {
					fixedRate = Long.parseLong(fixedRateString);
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException(
							"Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into integer");
				}
				tasks.add(this.registrar.scheduleFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay)));
			}

			// 检查是否有属性设置
			Assert.isTrue(processedSchedule, errorMessage);

			// Finally register the scheduled tasks
			synchronized (this.scheduledTasks) {
				Set<ScheduledTask> registeredTasks = this.scheduledTasks.get(bean);
				if (registeredTasks == null) {
					registeredTasks = new LinkedHashSet<ScheduledTask>(4);
					this.scheduledTasks.put(bean, registeredTasks);
				}
				registeredTasks.addAll(tasks);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"Encountered invalid @Scheduled method '" + method.getName() + "': " + ex.getMessage());
		}
	}


	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) {
		Set<ScheduledTask> tasks;
		synchronized (this.scheduledTasks) {
			tasks = this.scheduledTasks.remove(bean);
		}
		if (tasks != null) {
			for (ScheduledTask task : tasks) {
				task.cancel();
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		synchronized (this.scheduledTasks) {
			return this.scheduledTasks.containsKey(bean);
		}
	}

	@Override
	public void destroy() {
		synchronized (this.scheduledTasks) {
			Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
			for (Set<ScheduledTask> tasks : allTasks) {
				for (ScheduledTask task : tasks) {
					task.cancel();
				}
			}
			this.scheduledTasks.clear();
		}
		this.registrar.destroy();
	}
}