package org.springframework.scheduling.quartz;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

/**
 * 公开{@link org.quartz.JobDetail}对象的{@link org.springframework.beans.factory.FactoryBean},
 * 该对象将作业执行委托给指定的(静态或非静态)方法.
 * 避免实现仅在Spring管理的目标bean上调用现有服务方法的单行Quartz作业.
 *
 * <p>从{@link MethodInvoker}基类继承常用配置属性,
 * 例如{@link #setTargetObject "targetObject"}和{@link #setTargetMethod "targetMethod"},
 * 通过 {@link #setTargetBeanName "targetBeanName"}属性添加对按名称查找目标bean的支持
 * (作为直接指定"targetObject"的替代方法, 允许非单例目标对象).
 *
 * <p>通过"concurrent"属性支持并发运行的作业和非当前运行的作业.
 * 此MethodInvokingJobDetailFactoryBean创建的作业, 默认是易失性且持久的 (根据Quartz术语).
 *
 * <p><b>NOTE: 通过此FactoryBean创建的JobDetails不可序列化, 因此不适用于持久性作业存储.</b>
 * 对于希望将持久作业委托给特定服务方法的情况, 需要将自己的Quartz Job实现为瘦包装器.
 *
 * <p>从Spring 4.1开始, 与Quartz 2.1.4及更高版本兼容.
 */
public class MethodInvokingJobDetailFactoryBean extends ArgumentConvertingMethodInvoker
		implements FactoryBean<JobDetail>, BeanNameAware, BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	private String name;

	private String group = Scheduler.DEFAULT_GROUP;

	private boolean concurrent = true;

	private String targetBeanName;

	private String beanName;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private BeanFactory beanFactory;

	private JobDetail jobDetail;


	/**
	 * 设置作业的名称.
	 * <p>默认是此FactoryBean的bean名称.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置作业组.
	 * <p>默认是Scheduler的默认组.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * 指定是否应以并发方式运行多个作业.
	 * 通过添加{@code @PersistJobDataAfterExecution}和{@code @DisallowConcurrentExecution}标记,
	 * 来实现不希望执行并发作业的行为.
	 * 有关有状态与无状态工作的更多信息可以在
	 * <a href="http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03">here</a>中找到.
	 * <p>默认设置是并发运行作业.
	 */
	public void setConcurrent(boolean concurrent) {
		this.concurrent = concurrent;
	}

	/**
	 * 设置Spring BeanFactory中目标bean的名称.
	 * <p>这是指定{@link #setTargetObject "targetObject"}的替代方法, 允许调用非单例bean.
	 * 请注意, 指定的"targetObject"和{@link #setTargetClass "targetClass"}值将覆盖此"targetBeanName"设置的相应效果
	 * (i.e. 静态预定义bean类型, 或bean对象).
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}


	@Override
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		prepare();

		// 使用特定名称, 或回退到bean名称.
		String name = (this.name != null ? this.name : this.beanName);

		// 考虑并发标志, 以在有状态和无状态作业之间进行选择.
		Class<?> jobClass = (this.concurrent ? MethodInvokingJob.class : StatefulMethodInvokingJob.class);

		// Build JobDetail instance.
		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(name);
		jdi.setGroup(this.group);
		jdi.setJobClass((Class) jobClass);
		jdi.setDurability(true);
		jdi.getJobDataMap().put("methodInvoker", this);
		this.jobDetail = jdi;

		postProcessJobDetail(this.jobDetail);
	}

	/**
	 * 用于对此FactoryBean公开的JobDetail进行后处理的回调.
	 * <p>默认实现为空. 可以在子类中重写.
	 * 
	 * @param jobDetail 这个FactoryBean准备的JobDetail
	 */
	protected void postProcessJobDetail(JobDetail jobDetail) {
	}


	/**
	 * 重写以支持{@link #setTargetBeanName "targetBeanName"}功能.
	 */
	@Override
	public Class<?> getTargetClass() {
		Class<?> targetClass = super.getTargetClass();
		if (targetClass == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetClass = this.beanFactory.getType(this.targetBeanName);
		}
		return targetClass;
	}

	/**
	 * 重写以支持{@link #setTargetBeanName "targetBeanName"}功能.
	 */
	@Override
	public Object getTargetObject() {
		Object targetObject = super.getTargetObject();
		if (targetObject == null && this.targetBeanName != null) {
			Assert.state(this.beanFactory != null, "BeanFactory must be set when using 'targetBeanName'");
			targetObject = this.beanFactory.getBean(this.targetBeanName);
		}
		return targetObject;
	}


	@Override
	public JobDetail getObject() {
		return this.jobDetail;
	}

	@Override
	public Class<? extends JobDetail> getObjectType() {
		return (this.jobDetail != null ? this.jobDetail.getClass() : JobDetail.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 调用指定方法的Quartz Job实现.
	 * 由MethodInvokingJobDetailFactoryBean自动应用.
	 */
	public static class MethodInvokingJob extends QuartzJobBean {

		protected static final Log logger = LogFactory.getLog(MethodInvokingJob.class);

		private MethodInvoker methodInvoker;

		/**
		 * 设置要使用的MethodInvoker.
		 */
		public void setMethodInvoker(MethodInvoker methodInvoker) {
			this.methodInvoker = methodInvoker;
		}

		/**
		 * 通过MethodInvoker调用方法.
		 */
		@Override
		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
			try {
				context.setResult(this.methodInvoker.invoke());
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof JobExecutionException) {
					// -> JobExecutionException, to be logged at info level by Quartz
					throw (JobExecutionException) ex.getTargetException();
				}
				else {
					// -> "unhandled exception", to be logged at error level by Quartz
					throw new JobMethodInvocationFailedException(this.methodInvoker, ex.getTargetException());
				}
			}
			catch (Exception ex) {
				// -> "unhandled exception", to be logged at error level by Quartz
				throw new JobMethodInvocationFailedException(this.methodInvoker, ex);
			}
		}
	}


	/**
	 * MethodInvokingJob的扩展, 实现StatefulJob接口.
	 * Quartz检查作业是否有状态, 如果是, 则不会让作业相互干扰.
	 */
	@PersistJobDataAfterExecution
	@DisallowConcurrentExecution
	public static class StatefulMethodInvokingJob extends MethodInvokingJob {

		// 没有实现, 只是添加标记接口StatefulJob, 以允许有状态的方法调用作业.
	}
}
