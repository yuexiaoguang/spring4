package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInfo;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * AOP代理配置管理器的基类.
 * 这些本身不是AOP代理, 但是这个类的子类通常是直接从中获取AOP代理实例的工厂.
 *
 * <p>该类释放了增强和切面管理的子类, 但实际上并没有实现子类提供的代理创建方法.
 *
 * <p>这个类是可序列化的; 子类不必是. 此类用于保存代理的快照.
 */
public class AdvisedSupport extends ProxyConfig implements Advised {

	/** 使用Spring 2.0中的serialVersionUID实现互操作性 */
	private static final long serialVersionUID = 2651364800145442165L;


	/**
	 * 当没有目标时, 默认的TargetSource, 并且行为由切面提供.
	 */
	public static final TargetSource EMPTY_TARGET_SOURCE = EmptyTargetSource.INSTANCE;


	/** 包级私有, 允许直接访问以提高效率 */
	TargetSource targetSource = EMPTY_TARGET_SOURCE;

	/** 是否已针对特定目标类过滤了切面 */
	private boolean preFiltered = false;

	/** 使用的AdvisorChainFactory */
	AdvisorChainFactory advisorChainFactory = new DefaultAdvisorChainFactory();

	/** 缓存, Method作为 key, 切面链List作为值 */
	private transient Map<MethodCacheKey, List<Object>> methodCache;

	/**
	 * 由代理实现的接口. 在列表中保留以保持注册顺序, 使用指定的接口顺序创建JDK代理.
	 */
	private List<Class<?>> interfaces = new ArrayList<Class<?>>();

	/**
	 * Advisor列表. 如果添加了Advice, 在添加到这个List之前, 它将被包装在Advisor中.
	 */
	private List<Advisor> advisors = new ArrayList<Advisor>();

	/**
	 * 更新了对切面列表的更改的数组，这更容易在内部操作.
	 */
	private Advisor[] advisorArray = new Advisor[0];


	public AdvisedSupport() {
		initMethodCache();
	}

	/**
	 * @param interfaces 代理的接口
	 */
	public AdvisedSupport(Class<?>... interfaces) {
		this();
		setInterfaces(interfaces);
	}

	/**
	 * 初始化方法缓存.
	 */
	private void initMethodCache() {
		this.methodCache = new ConcurrentHashMap<MethodCacheKey, List<Object>>(32);
	}


	/**
	 * 将给定对象设置为目标.
	 * 将为该对象创建SingletonTargetSource.
	 */
	public void setTarget(Object target) {
		setTargetSource(new SingletonTargetSource(target));
	}

	@Override
	public void setTargetSource(TargetSource targetSource) {
		this.targetSource = (targetSource != null ? targetSource : EMPTY_TARGET_SOURCE);
	}

	@Override
	public TargetSource getTargetSource() {
		return this.targetSource;
	}

	/**
	 * 设置要代理的目标类, 指示代理应该可以转换为给定的类.
	 * <p>内部将使用给定目标类的{@link org.springframework.aop.target.EmptyTargetSource}.
	 * 所需的代理类型将根据代理的实际创建来确定.
	 * <p>这是设置“targetSource”或“target”的替代品, 用于想要基于目标类（可以是接口或具体类）的代理, 而没有完全能力的TargetSource的情况.
	 */
	public void setTargetClass(Class<?> targetClass) {
		this.targetSource = EmptyTargetSource.forClass(targetClass);
	}

	@Override
	public Class<?> getTargetClass() {
		return this.targetSource.getTargetClass();
	}

	@Override
	public void setPreFiltered(boolean preFiltered) {
		this.preFiltered = preFiltered;
	}

	@Override
	public boolean isPreFiltered() {
		return this.preFiltered;
	}

	/**
	 * 设置要使用的切面链工厂.
	 * <p>默认是 {@link DefaultAdvisorChainFactory}.
	 */
	public void setAdvisorChainFactory(AdvisorChainFactory advisorChainFactory) {
		Assert.notNull(advisorChainFactory, "AdvisorChainFactory must not be null");
		this.advisorChainFactory = advisorChainFactory;
	}

	/**
	 * 返回要使用的切面链工厂 (不会是 {@code null}).
	 */
	public AdvisorChainFactory getAdvisorChainFactory() {
		return this.advisorChainFactory;
	}


	/**
	 * 设置要代理的接口.
	 */
	public void setInterfaces(Class<?>... interfaces) {
		Assert.notNull(interfaces, "Interfaces must not be null");
		this.interfaces.clear();
		for (Class<?> ifc : interfaces) {
			addInterface(ifc);
		}
	}

	/**
	 * 添加新的代理接口.
	 * 
	 * @param intf 要代理的附加接口
	 */
	public void addInterface(Class<?> intf) {
		Assert.notNull(intf, "Interface must not be null");
		if (!intf.isInterface()) {
			throw new IllegalArgumentException("[" + intf.getName() + "] is not an interface");
		}
		if (!this.interfaces.contains(intf)) {
			this.interfaces.add(intf);
			adviceChanged();
		}
	}

	/**
	 * 删除代理的接口.
	 * <p>如果给定的接口未被代理，则不执行任何操作.
	 * 
	 * @param intf 要从代理中删除的接口
	 * 
	 * @return {@code true}如果接口被删除; {@code false}如果未找到接口，则无法删除
	 */
	public boolean removeInterface(Class<?> intf) {
		return this.interfaces.remove(intf);
	}

	@Override
	public Class<?>[] getProxiedInterfaces() {
		return ClassUtils.toClassArray(this.interfaces);
	}

	@Override
	public boolean isInterfaceProxied(Class<?> intf) {
		for (Class<?> proxyIntf : this.interfaces) {
			if (intf.isAssignableFrom(proxyIntf)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public final Advisor[] getAdvisors() {
		return this.advisorArray;
	}

	@Override
	public void addAdvisor(Advisor advisor) {
		int pos = this.advisors.size();
		addAdvisor(pos, advisor);
	}

	@Override
	public void addAdvisor(int pos, Advisor advisor) throws AopConfigException {
		if (advisor instanceof IntroductionAdvisor) {
			validateIntroductionAdvisor((IntroductionAdvisor) advisor);
		}
		addAdvisorInternal(pos, advisor);
	}

	@Override
	public boolean removeAdvisor(Advisor advisor) {
		int index = indexOf(advisor);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public void removeAdvisor(int index) throws AopConfigException {
		if (isFrozen()) {
			throw new AopConfigException("Cannot remove Advisor: Configuration is frozen.");
		}
		if (index < 0 || index > this.advisors.size() - 1) {
			throw new AopConfigException("Advisor index " + index + " is out of bounds: " +
					"This configuration only has " + this.advisors.size() + " advisors.");
		}

		Advisor advisor = this.advisors.get(index);
		if (advisor instanceof IntroductionAdvisor) {
			IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
			// 需要删除引入接口.
			for (int j = 0; j < ia.getInterfaces().length; j++) {
				removeInterface(ia.getInterfaces()[j]);
			}
		}

		this.advisors.remove(index);
		updateAdvisorArray();
		adviceChanged();
	}

	@Override
	public int indexOf(Advisor advisor) {
		Assert.notNull(advisor, "Advisor must not be null");
		return this.advisors.indexOf(advisor);
	}

	@Override
	public boolean replaceAdvisor(Advisor a, Advisor b) throws AopConfigException {
		Assert.notNull(a, "Advisor a must not be null");
		Assert.notNull(b, "Advisor b must not be null");
		int index = indexOf(a);
		if (index == -1) {
			return false;
		}
		removeAdvisor(index);
		addAdvisor(index, b);
		return true;
	}

	/**
	 * 将所有给定的切面添加到此代理配置中.
	 * 
	 * @param advisors 要注册的切面
	 */
	public void addAdvisors(Advisor... advisors) {
		addAdvisors(Arrays.asList(advisors));
	}

	/**
	 * 将所有给定的切面添加到此代理配置中.
	 * 
	 * @param advisors 要注册的切面
	 */
	public void addAdvisors(Collection<Advisor> advisors) {
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (!CollectionUtils.isEmpty(advisors)) {
			for (Advisor advisor : advisors) {
				if (advisor instanceof IntroductionAdvisor) {
					validateIntroductionAdvisor((IntroductionAdvisor) advisor);
				}
				Assert.notNull(advisor, "Advisor must not be null");
				this.advisors.add(advisor);
			}
			updateAdvisorArray();
			adviceChanged();
		}
	}

	private void validateIntroductionAdvisor(IntroductionAdvisor advisor) {
		advisor.validateInterfaces();
		// 如果切面通过验证, 可以修改.
		Class<?>[] ifcs = advisor.getInterfaces();
		for (Class<?> ifc : ifcs) {
			addInterface(ifc);
		}
	}

	private void addAdvisorInternal(int pos, Advisor advisor) throws AopConfigException {
		Assert.notNull(advisor, "Advisor must not be null");
		if (isFrozen()) {
			throw new AopConfigException("Cannot add advisor: Configuration is frozen.");
		}
		if (pos > this.advisors.size()) {
			throw new IllegalArgumentException(
					"Illegal position " + pos + " in advisor list with size " + this.advisors.size());
		}
		this.advisors.add(pos, advisor);
		updateAdvisorArray();
		adviceChanged();
	}

	/**
	 * 更新数组.
	 */
	protected final void updateAdvisorArray() {
		this.advisorArray = this.advisors.toArray(new Advisor[this.advisors.size()]);
	}

	/**
	 * 允许不受控制地访问{@link Advisor Advisors}的{@link List}.
	 * <p>小心使用, 并且在进行任何修改时记得{@link #updateAdvisorArray() 刷新切面数组} 和{@link #adviceChanged() 触发增强修改事件}.
	 */
	protected final List<Advisor> getAdvisorsInternal() {
		return this.advisors;
	}


	@Override
	public void addAdvice(Advice advice) throws AopConfigException {
		int pos = this.advisors.size();
		addAdvice(pos, advice);
	}

	/**
	 * 除非增强实现了IntroductionInfo, 否则不能以这种方式添加引入.
	 */
	@Override
	public void addAdvice(int pos, Advice advice) throws AopConfigException {
		Assert.notNull(advice, "Advice must not be null");
		if (advice instanceof IntroductionInfo) {
			// 这种引入不需要IntroductionAdvisor: 它完全是自我描述的.
			addAdvisor(pos, new DefaultIntroductionAdvisor(advice, (IntroductionInfo) advice));
		}
		else if (advice instanceof DynamicIntroductionAdvice) {
			// 需要一个IntroductionAdvisor来进行此类引入.
			throw new AopConfigException("DynamicIntroductionAdvice may only be added as part of IntroductionAdvisor");
		}
		else {
			addAdvisor(pos, new DefaultPointcutAdvisor(advice));
		}
	}

	@Override
	public boolean removeAdvice(Advice advice) throws AopConfigException {
		int index = indexOf(advice);
		if (index == -1) {
			return false;
		}
		else {
			removeAdvisor(index);
			return true;
		}
	}

	@Override
	public int indexOf(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		for (int i = 0; i < this.advisors.size(); i++) {
			Advisor advisor = this.advisors.get(i);
			if (advisor.getAdvice() == advice) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 此代理配置中的切面是否包含给定的增强?
	 * 
	 * @param advice 检查包含的增强
	 * 
	 * @return 是否包含此增强实例
	 */
	public boolean adviceIncluded(Advice advice) {
		if (advice != null) {
			for (Advisor advisor : this.advisors) {
				if (advisor.getAdvice() == advice) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 计算给定类的增强.
	 * 
	 * @param adviceClass 要检查的增强类
	 * 
	 * @return 此类或子类的拦截器数
	 */
	public int countAdvicesOfType(Class<?> adviceClass) {
		int count = 0;
		if (adviceClass != null) {
			for (Advisor advisor : this.advisors) {
				if (adviceClass.isInstance(advisor.getAdvice())) {
					count++;
				}
			}
		}
		return count;
	}


	/**
	 * 确定给定方法的{@link org.aopalliance.intercept.MethodInterceptor}对象列表, 基于此配置.
	 * 
	 * @param method 代理的方法
	 * @param targetClass 目标类
	 * 
	 * @return MethodInterceptor列表(也可能包括InterceptorAndDynamicMethodMatcher)
	 */
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
		MethodCacheKey cacheKey = new MethodCacheKey(method);
		List<Object> cached = this.methodCache.get(cacheKey);
		if (cached == null) {
			cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
					this, method, targetClass);
			this.methodCache.put(cacheKey, cached);
		}
		return cached;
	}

	/**
	 * 增强发生变化时调用.
	 */
	protected void adviceChanged() {
		this.methodCache.clear();
	}

	/**
	 * 在no-arg构造函数创建的新实例上调用此方法，以从给定对象创建配置的独立副本.
	 * 
	 * @param other 从中复制配置的AdvisedSupport对象
	 */
	protected void copyConfigurationFrom(AdvisedSupport other) {
		copyConfigurationFrom(other, other.targetSource, new ArrayList<Advisor>(other.advisors));
	}

	/**
	 * 从给定的AdvisedSupport对象复制AOP配置, 但允许替换新的TargetSource和给定的拦截器链.
	 * 
	 * @param other 从中获取代理配置的AdvisedSupport对象
	 * @param targetSource 新的TargetSource
	 * @param advisors Advisor链
	 */
	protected void copyConfigurationFrom(AdvisedSupport other, TargetSource targetSource, List<Advisor> advisors) {
		copyFrom(other);
		this.targetSource = targetSource;
		this.advisorChainFactory = other.advisorChainFactory;
		this.interfaces = new ArrayList<Class<?>>(other.interfaces);
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				validateIntroductionAdvisor((IntroductionAdvisor) advisor);
			}
			Assert.notNull(advisor, "Advisor must not be null");
			this.advisors.add(advisor);
		}
		updateAdvisorArray();
		adviceChanged();
	}

	/**
	 * 构建此AdvisedSupport的只有配置副本, 替换TargetSource.
	 */
	AdvisedSupport getConfigurationOnlyCopy() {
		AdvisedSupport copy = new AdvisedSupport();
		copy.copyFrom(this);
		copy.targetSource = EmptyTargetSource.forClass(getTargetClass(), getTargetSource().isStatic());
		copy.advisorChainFactory = this.advisorChainFactory;
		copy.interfaces = this.interfaces;
		copy.advisors = this.advisors;
		copy.updateAdvisorArray();
		return copy;
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化; 只是在反序列化后初始化状态.
		ois.defaultReadObject();

		// Initialize transient fields.
		initMethodCache();
	}


	@Override
	public String toProxyConfigString() {
		return toString();
	}

	/**
	 * 用于调试/诊断.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		sb.append(": ").append(this.interfaces.size()).append(" interfaces ");
		sb.append(ClassUtils.classNamesToString(this.interfaces)).append("; ");
		sb.append(this.advisors.size()).append(" advisors ");
		sb.append(this.advisors).append("; ");
		sb.append("targetSource [").append(this.targetSource).append("]; ");
		sb.append(super.toString());
		return sb.toString();
	}


	/**
	 * 围绕Method的简单包装类. 用作缓存方法时的Key, 用于有效的equals和hashCode比较.
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final Method method;

		private final int hashCode;

		public MethodCacheKey(Method method) {
			this.method = method;
			this.hashCode = method.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			return (this == other || (other instanceof MethodCacheKey &&
					this.method == ((MethodCacheKey) other).method));
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public String toString() {
			return this.method.toString();
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.method.getName().compareTo(other.method.getName());
			if (result == 0) {
				result = this.method.toString().compareTo(other.method.toString());
			}
			return result;
		}
	}
}
