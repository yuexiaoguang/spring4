package org.springframework.transaction.annotation;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.transaction.interceptor.TransactionAttributeSource}接口的实现,
 * 用于处理JDK 1.5+注解格式的事务元数据.
 *
 * <p>该类读取Spring的JDK 1.5+ {@link Transactional}注解, 并向Spring的事务基础结构公开相应的事务属性.
 * 还支持JTA 1.2的{@link javax.transaction.Transactional}和EJB3的{@link javax.ejb.TransactionAttribute}注解.
 * 此类还可以作为自定义TransactionAttributeSource的基类, 或通过{@link TransactionAnnotationParser}策略进行自定义.
 */
@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource
		implements Serializable {

	private static final boolean jta12Present = ClassUtils.isPresent(
			"javax.transaction.Transactional", AnnotationTransactionAttributeSource.class.getClassLoader());

	private static final boolean ejb3Present = ClassUtils.isPresent(
			"javax.ejb.TransactionAttribute", AnnotationTransactionAttributeSource.class.getClassLoader());

	private final boolean publicMethodsOnly;

	private final Set<TransactionAnnotationParser> annotationParsers;


	/**
	 * 支持带有{@code Transactional}注解或EJB3 {@link javax.ejb.TransactionAttribute}注解的public方法.
	 */
	public AnnotationTransactionAttributeSource() {
		this(true);
	}

	/**
	 * 支持带有{@code Transactional}注解或EJB3 {@link javax.ejb.TransactionAttribute}注解的public方法.
	 *
	 * @param publicMethodsOnly 是否支持仅带有{@code Transactional}注解的public方法 (通常用于基于代理的AOP),
	 * 或protected/private方法 (通常与AspectJ类织入一起使用)
	 */
	public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		this.annotationParsers = new LinkedHashSet<TransactionAnnotationParser>(4);
		this.annotationParsers.add(new SpringTransactionAnnotationParser());
		if (jta12Present) {
			this.annotationParsers.add(new JtaTransactionAnnotationParser());
		}
		if (ejb3Present) {
			this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
		}
	}

	/**
	 * @param annotationParser 要使用的TransactionAnnotationParser
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
		this.publicMethodsOnly = true;
		Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * @param annotationParsers 要使用的TransactionAnnotationParsers
	 */
	public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
		Set<TransactionAnnotationParser> parsers = new LinkedHashSet<TransactionAnnotationParser>(annotationParsers.length);
		Collections.addAll(parsers, annotationParsers);
		this.annotationParsers = parsers;
	}

	/**
	 * @param annotationParsers 要使用的TransactionAnnotationParsers
	 */
	public AnnotationTransactionAttributeSource(Set<TransactionAnnotationParser> annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	@Override
	protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
		return determineTransactionAttribute(clazz);
	}

	@Override
	protected TransactionAttribute findTransactionAttribute(Method method) {
		return determineTransactionAttribute(method);
	}

	/**
	 * 确定给定方法或类的事务属性.
	 * <p>此实现委托给配置的{@link TransactionAnnotationParser TransactionAnnotationParsers},
	 * 用于将已知注解解析为Spring的元数据属性类.
	 * 如果它不是事务性的, 则返回{@code null}.
	 * <p>可以重写以支持带有事务元数据的自定义注解.
	 * 
	 * @param element 带注解的方法或类
	 * 
	 * @return 配置的事务属性, 或{@code null}
	 */
	protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
		if (element.getAnnotations().length > 0) {
			for (TransactionAnnotationParser annotationParser : this.annotationParsers) {
				TransactionAttribute attr = annotationParser.parseTransactionAnnotation(element);
				if (attr != null) {
					return attr;
				}
			}
		}
		return null;
	}

	/**
	 * 默认情况下, 只有public方法可以进行事务处理.
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationTransactionAttributeSource)) {
			return false;
		}
		AnnotationTransactionAttributeSource otherTas = (AnnotationTransactionAttributeSource) other;
		return (this.annotationParsers.equals(otherTas.annotationParsers) &&
				this.publicMethodsOnly == otherTas.publicMethodsOnly);
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}

}
