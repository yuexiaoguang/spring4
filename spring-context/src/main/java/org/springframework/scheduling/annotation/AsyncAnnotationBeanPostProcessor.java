package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Bean后处理器, 通过向公开的代理添加相应的{@link AsyncAnnotationAdvisor},
 * 自动应用异步调用行为于bean, 该bean在类或方法级别上带有{@link Async}注解
 * (现有的AOP代理, 或实现所有目标接口的新生成的代理).
 *
 * <p>可以提供负责异步执行的{@link TaskExecutor}, 以及指示应该异步调用方法的注解类型.
 * 如果未指定注解类型, 则此后处理器将检测Spring的{@link Async @Async}注解以及 EJB 3.1 {@code javax.ejb.Asynchronous}注解.
 *
 * <p>对于具有{@code void}返回类型的方法, 调用者无法访问异步方法调用期间抛出的任何异常.
 * 可以指定{@link AsyncUncaughtExceptionHandler}来处理这些情况.
 *
 * <p>Note: 默认情况下, 底层异步切面在现有切面之前应用, 以便在调用链中尽早切换到异步执行.
 */
@SuppressWarnings("serial")
public class AsyncAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	/**
	 * 要获取的{@link TaskExecutor} bean的默认名称: "taskExecutor".
	 * <p>请注意, 初始查找按类型进行; 这只是在上下文中找到多个执行器bean的情况下的后备.
	 */
	public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME =
			AnnotationAsyncExecutionInterceptor.DEFAULT_TASK_EXECUTOR_BEAN_NAME;


	protected final Log logger = LogFactory.getLog(getClass());

	private Class<? extends Annotation> asyncAnnotationType;

	private Executor executor;

	private AsyncUncaughtExceptionHandler exceptionHandler;


	public AsyncAnnotationBeanPostProcessor() {
		setBeforeExistingAdvisors(true);
	}


	/**
	 * 设置要在类或方法级别检测的'async'注解类型.
	 * 默认情况下, 将检测{@link Async}注解和EJB 3.1 {@code javax.ejb.Asynchronous}注解.
	 * <p>此setter属性存在, 以便开发人员可以提供自己的(非Spring特定的)注释类型, 以指示应异步调用方法(或给定类的所有方法).
	 * 
	 * @param asyncAnnotationType 所需的注解类型
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		this.asyncAnnotationType = asyncAnnotationType;
	}

	/**
	 * 设置在异步调用方法时使用的{@link Executor}.
	 * <p>如果未指定, 则将应用默认执行器解析:
	 * 在上下文中搜索唯一的{@link TaskExecutor} bean, 或者在名为"taskExecutor"的{@link Executor} bean中搜索.
	 * 如果两者都不可解析, 则将在拦截器中创建本地默认执行器.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 设置用于处理异步方法执行引发的未捕获的异常的{@link AsyncUncaughtExceptionHandler}.
	 */
	public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		AsyncAnnotationAdvisor advisor = new AsyncAnnotationAdvisor(this.executor, this.exceptionHandler);
		if (this.asyncAnnotationType != null) {
			advisor.setAsyncAnnotationType(this.asyncAnnotationType);
		}
		advisor.setBeanFactory(beanFactory);
		this.advisor = advisor;
	}

}
