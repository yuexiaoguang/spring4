package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 通过{@link Async}注解激活异步方法执行的Advisor.
 * 此注解可用于实现类和服务接口中的方法和类型级别.
 *
 * <p>此切面也检测EJB 3.1 {@code javax.ejb.Asynchronous}注解, 将其视为与Spring自己的 {@code Async}.
 * 此外, 可以通过{@link #setAsyncAnnotationType "asyncAnnotationType"}属性指定自定义异步注解类型.
 */
@SuppressWarnings("serial")
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private AsyncUncaughtExceptionHandler exceptionHandler;

	private Advice advice;

	private Pointcut pointcut;


	/**
	 * 用于bean格式配置.
	 */
	public AsyncAnnotationAdvisor() {
		this(null, null);
	}

	/**
	 * @param executor 用于异步方法的任务执行器 (可以是{@code null}触发默认执行器解析)
	 * @param exceptionHandler 用于处理异步方法执行引发的意外异常的{@link AsyncUncaughtExceptionHandler}
	 */
	@SuppressWarnings("unchecked")
	public AsyncAnnotationAdvisor(Executor executor, AsyncUncaughtExceptionHandler exceptionHandler) {
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(2);
		asyncAnnotationTypes.add(Async.class);
		try {
			asyncAnnotationTypes.add((Class<? extends Annotation>)
					ClassUtils.forName("javax.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
		}
		catch (ClassNotFoundException ex) {
			// If EJB 3.1 API not present, simply ignore.
		}
		if (exceptionHandler != null) {
			this.exceptionHandler = exceptionHandler;
		}
		else {
			this.exceptionHandler = new SimpleAsyncUncaughtExceptionHandler();
		}
		this.advice = buildAdvice(executor, this.exceptionHandler);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}


	/**
	 * 指定用于异步方法的默认任务执行器.
	 */
	public void setTaskExecutor(Executor executor) {
		this.advice = buildAdvice(executor, this.exceptionHandler);
	}

	/**
	 * 设置'async'注解类型.
	 * <p>默认的异步注解类型是{@link Async}注解, 以及EJB 3.1 {@code javax.ejb.Asynchronous}注解.
	 * <p>存在此setter属性, 以便开发人员可以提供自己的 (非Spring特定的) 注解类型, 以指示方法将异步执行.
	 * 
	 * @param asyncAnnotationType 所需的注解类型
	 */
	public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
		Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
		Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<Class<? extends Annotation>>();
		asyncAnnotationTypes.add(asyncAnnotationType);
		this.pointcut = buildPointcut(asyncAnnotationTypes);
	}

	/**
	 * 设置通过限定符查找执行器时要使用的{@code BeanFactory}.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.advice instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
		}
	}


	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}


	protected Advice buildAdvice(Executor executor, AsyncUncaughtExceptionHandler exceptionHandler) {
		return new AnnotationAsyncExecutionInterceptor(executor, exceptionHandler);
	}

	/**
	 * 计算给定异步注解类型的切点.
	 * 
	 * @param asyncAnnotationTypes 要内省的异步注解类型
	 * 
	 * @return 适用的Pointcut对象, 或{@code null}
	 */
	protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
		ComposablePointcut result = null;
		for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
			Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
			Pointcut mpc = AnnotationMatchingPointcut.forMethodAnnotation(asyncAnnotationType);
			if (result == null) {
				result = new ComposablePointcut(cpc);
			}
			else {
				result.union(cpc);
			}
			result = result.union(mpc);
		}
		return result;
	}

}
