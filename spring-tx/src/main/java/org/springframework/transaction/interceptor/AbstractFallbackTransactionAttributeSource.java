package org.springframework.transaction.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

/**
 * {@link TransactionAttributeSource}的抽象实现, 它缓存方法的属性并实现回退策略:
 * 1. 具体目标方法;
 * 2. 目标类;
 * 3. 声明方法;
 * 4. 声明类/接口.
 *
 * <p>如果没有与目标方法关联, 则默认使用目标类的事务属性.
 * 与目标方法关联的任何事务属性都会完全覆盖类事务属性.
 * 如果在目标类上找不到, 则将检查已调用调用方法的接口 (如果是JDK代理).
 *
 * <p>此实现在首次使用后按方法缓存属性.
 * 如果希望允许动态更改事务属性 (这是非常不可能的), 则可以使缓存可配置.
 * 由于评估回滚规则的成本, 缓存是可取的.
 */
public abstract class AbstractFallbackTransactionAttributeSource implements TransactionAttributeSource {

	/**
	 * 缓存中保存的规范值表示没有找到此方法的事务属性, 不需要再查看.
	 */
	@SuppressWarnings("serial")
	private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
		@Override
		public String toString() {
			return "null";
		}
	};


	/**
	 * Logger available to subclasses.
	 * <p>由于此基类未标记为Serializable, 因此在序列化后将重新创建记录器 - 前提是具体子类为Serializable.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * TransactionAttributes的缓存, 由特定目标类的方法作为键.
	 * <p>由于此基类未标记为Serializable, 因此在序列化后将重新创建缓存 - 前提是具体子类为Serializable.
	 */
	private final Map<Object, TransactionAttribute> attributeCache =
			new ConcurrentHashMap<Object, TransactionAttribute>(1024);


	/**
	 * 确定此方法调用的事务属性.
	 * <p>如果未找到方法属性, 则默认为类的事务属性.
	 * 
	 * @param method 当前调用的方法 (never {@code null})
	 * @param targetClass 此调用的目标类 (may be {@code null})
	 * 
	 * @return 此方法的TransactionAttribute, 或{@code null} 如果方法不是事务性的
	 */
	@Override
	public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
		if (method.getDeclaringClass() == Object.class) {
			return null;
		}

		// 首先, 看看是否有缓存值.
		Object cacheKey = getCacheKey(method, targetClass);
		TransactionAttribute cached = this.attributeCache.get(cacheKey);
		if (cached != null) {
			// 值将是规范值, 表示没有事务属性或实际事务属性.
			if (cached == NULL_TRANSACTION_ATTRIBUTE) {
				return null;
			}
			else {
				return cached;
			}
		}
		else {
			// 需要解决这个问题.
			TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
			// 把它放在缓存中.
			if (txAttr == null) {
				this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
			}
			else {
				String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
				if (txAttr instanceof DefaultTransactionAttribute) {
					((DefaultTransactionAttribute) txAttr).setDescriptor(methodIdentification);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
				}
				this.attributeCache.put(cacheKey, txAttr);
			}
			return txAttr;
		}
	}

	/**
	 * 确定给定方法和目标类的缓存键.
	 * <p>不得为重载方法生成相同的键.
	 * 必须为同一方法的不同实例生成相同的键.
	 * 
	 * @param method 方法 (never {@code null})
	 * @param targetClass 目标类 (may be {@code null})
	 * 
	 * @return 缓存键 (never {@code null})
	 */
	protected Object getCacheKey(Method method, Class<?> targetClass) {
		return new MethodClassKey(method, targetClass);
	}

	/**
	 * 与{@link #getTransactionAttribute}相同的签名, 但不缓存结果.
	 * {@link #getTransactionAttribute}实际上是此方法的缓存装饰器.
	 * <p>从4.1.8开始, 可以覆盖此方法.
	 */
	protected TransactionAttribute computeTransactionAttribute(Method method, Class<?> targetClass) {
		// 不允许非public方法.
		if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		// 忽略CGLIB子类 - 内省实际的用户类.
		Class<?> userClass = ClassUtils.getUserClass(targetClass);
		// 该方法可以在接口上, 但需要来自目标类的属性.
		// 如果目标类为null, 则方法将保持不变.
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
		// 如果使用泛型参数处理方法, 找到原始方法.
		specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

		// 首先尝试的是目标类中的方法.
		TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
		if (txAttr != null) {
			return txAttr;
		}

		// 第二次尝试是目标类的事务属性.
		txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
		if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
			return txAttr;
		}

		if (specificMethod != method) {
			// 回退是看原始方法.
			txAttr = findTransactionAttribute(method);
			if (txAttr != null) {
				return txAttr;
			}
			// 最后一个回退是原始方法的类.
			txAttr = findTransactionAttribute(method.getDeclaringClass());
			if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
				return txAttr;
			}
		}

		return null;
	}


	/**
	 * 子类需要实现它来返回给定类的事务属性.
	 * 
	 * @param clazz 要检索属性的类
	 * 
	 * @return 与此类关联的所有事务属性, 或{@code null}
	 */
	protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

	/**
	 * 子类需要实现它来返回给定方法的事务属性.
	 * 
	 * @param method 要检索属性的方法
	 * 
	 * @return 与此方法关联的所有事务属性, 或{@code null}
	 */
	protected abstract TransactionAttribute findTransactionAttribute(Method method);

	/**
	 * 应该只允许public方法具有事务语义?
	 * <p>默认实现返回{@code false}.
	 */
	protected boolean allowPublicMethodsOnly() {
		return false;
	}
}
