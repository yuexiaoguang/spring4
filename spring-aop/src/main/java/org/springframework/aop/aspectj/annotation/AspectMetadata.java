package org.springframework.aop.aspectj.annotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.TypePatternClassFilter;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.support.ComposablePointcut;

/**
 * AspectJ切面类的元数据, 为每个子句添加一个额外的Spring AOP切点.
 *
 * <p>使用AspectJ 5 AJType反射API, 使我们能够使用不同的AspectJ实例化模型, 例如 "singleton", "pertarget", "perthis".
 */
@SuppressWarnings("serial")
public class AspectMetadata implements Serializable {

	/**
	 * Spring定义的这个切面的名称 (bean名称) - 允许确定两条增强是否来自同一切面, 之后是它们的相对优先级.
	 */
	private final String aspectName;

	/**
	 * 切面类，单独存储，用于在反序列化时重新解析相应的AjType.
	 */
	private final Class<?> aspectClass;

	/**
	 * AspectJ反射信息 (AspectJ 5 / Java 5 规范).
	 * 在反序列化时重新解决，因为它本身不可序列化.
	 */
	private transient AjType<?> ajType;

	/**
	 * Spring AOP切点, 对应于切面的per子句. 在单例的情况下, 将是Pointcut.TRUE规范实例, 否则将是AspectJExpressionPointcut.
	 */
	private final Pointcut perClausePointcut;


	/**
	 * @param aspectClass 切面类
	 * @param aspectName 切面的名称
	 */
	public AspectMetadata(Class<?> aspectClass, String aspectName) {
		this.aspectName = aspectName;

		Class<?> currClass = aspectClass;
		AjType<?> ajType = null;
		while (currClass != Object.class) {
			AjType<?> ajTypeToCheck = AjTypeSystem.getAjType(currClass);
			if (ajTypeToCheck.isAspect()) {
				ajType = ajTypeToCheck;
				break;
			}
			currClass = currClass.getSuperclass();
		}
		if (ajType == null) {
			throw new IllegalArgumentException("Class '" + aspectClass.getName() + "' is not an @AspectJ aspect");
		}
		if (ajType.getDeclarePrecedence().length > 0) {
			throw new IllegalArgumentException("DeclarePrecendence not presently supported in Spring AOP");
		}
		this.aspectClass = ajType.getJavaClass();
		this.ajType = ajType;

		switch (this.ajType.getPerClause().getKind()) {
			case SINGLETON:
				this.perClausePointcut = Pointcut.TRUE;
				return;
			case PERTARGET:
			case PERTHIS:
				AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
				ajexp.setLocation(aspectClass.getName());
				ajexp.setExpression(findPerClause(aspectClass));
				ajexp.setPointcutDeclarationScope(aspectClass);
				this.perClausePointcut = ajexp;
				return;
			case PERTYPEWITHIN:
				// 使用类型模式
				this.perClausePointcut = new ComposablePointcut(new TypePatternClassFilter(findPerClause(aspectClass)));
				return;
			default:
				throw new AopConfigException(
						"PerClause " + ajType.getPerClause().getKind() + " not supported by Spring AOP for " + aspectClass);
		}
	}

	/**
	 * 从格式为{@code pertarget(contents)}的字符串中提取内容.
	 */
	private String findPerClause(Class<?> aspectClass) {
		String str = aspectClass.getAnnotation(Aspect.class).value();
		str = str.substring(str.indexOf('(') + 1);
		str = str.substring(0, str.length() - 1);
		return str;
	}


	/**
	 * 返回AspectJ反射信息.
	 */
	public AjType<?> getAjType() {
		return this.ajType;
	}

	/**
	 * 返回切面类.
	 */
	public Class<?> getAspectClass() {
		return this.aspectClass;
	}

	/**
	 * 返回切面名称.
	 */
	public String getAspectName() {
		return this.aspectName;
	}

	/**
	 * 返回单个切面的Spring切点表达式. (e.g. {@code Pointcut.TRUE}如果它是单例).
	 */
	public Pointcut getPerClausePointcut() {
		return this.perClausePointcut;
	}

	/**
	 * 返回切面是否定义为 "perthis" 或 "pertarget".
	 */
	public boolean isPerThisOrPerTarget() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTARGET || kind == PerClauseKind.PERTHIS);
	}

	/**
	 * 返回切面是否定义为 "pertypewithin".
	 */
	public boolean isPerTypeWithin() {
		PerClauseKind kind = getAjType().getPerClause().getKind();
		return (kind == PerClauseKind.PERTYPEWITHIN);
	}

	/**
	 * 返回切面是否需要延迟实例化.
	 */
	public boolean isLazilyInstantiated() {
		return (isPerThisOrPerTarget() || isPerTypeWithin());
	}

	private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.ajType = AjTypeSystem.getAjType(this.aspectClass);
	}
}
