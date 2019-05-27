package org.springframework.aop.target;

import java.util.HashSet;
import java.util.Set;

import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.NamedThreadLocal;

/**
 * 替代对象池. 这个{@link org.springframework.aop.TargetSource}使用一个线程模型, 其中每个线程都有自己的目标副本.
 * 目标没有竞争. 在正在运行的服务器上将目标对象创建保持在最低限度.
 *
 * <p>应用程序代码写入普通池; 调用者不能假设他们将在不同线程的调用中处理相同的实例.
 * 但是, 在单个线程的操作期间可以依赖状态: 例如, 如果一个调用者在AOP代理上重复调用.
 *
 * <p>在BeanFactory销毁时执行线程绑定对象的清理, 调用 {@code DisposableBean.destroy()}方法.
 * 请注意，在应用程序实际关闭之前，许多线程绑定对象都可以存在.
 */
@SuppressWarnings("serial")
public class ThreadLocalTargetSource extends AbstractPrototypeBasedTargetSource
		implements ThreadLocalTargetSourceStats, DisposableBean {

	/**
	 * ThreadLocal持有与当前线程关联的目标.
	 * 与大多数静态的ThreadLocals不同, 此变量是每个线程一个ThreadLocalTargetSource类实例.
	 */
	private final ThreadLocal<Object> targetInThread =
			new NamedThreadLocal<Object>("Thread-local instance of bean '" + getTargetBeanName() + "'");

	/**
	 * 管理的目标, 使我们能够跟踪我们创建的目标.
	 */
	private final Set<Object> targetSet = new HashSet<Object>();

	private int invocationCount;

	private int hitCount;


	/**
	 * 寻找ThreadLocal中的目标. 如果找不到, 创建一个并将其绑定到线程. 不需要同步.
	 */
	@Override
	public Object getTarget() throws BeansException {
		++this.invocationCount;
		Object target = this.targetInThread.get();
		if (target == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No target for prototype '" + getTargetBeanName() + "' bound to thread: " +
						"creating one and binding it to thread '" + Thread.currentThread().getName() + "'");
			}
			// Associate target with ThreadLocal.
			target = newPrototypeInstance();
			this.targetInThread.set(target);
			synchronized (this.targetSet) {
				this.targetSet.add(target);
			}
		}
		else {
			++this.hitCount;
		}
		return target;
	}

	/**
	 * 必要时处理目标; 清空 ThreadLocal.
	 */
	@Override
	public void destroy() {
		logger.debug("Destroying ThreadLocalTargetSource bindings");
		synchronized (this.targetSet) {
			for (Object target : this.targetSet) {
				destroyPrototypeInstance(target);
			}
			this.targetSet.clear();
		}
		// Clear ThreadLocal, just in case.
		this.targetInThread.remove();
	}


	@Override
	public int getInvocationCount() {
		return this.invocationCount;
	}

	@Override
	public int getHitCount() {
		return this.hitCount;
	}

	@Override
	public int getObjectCount() {
		synchronized (this.targetSet) {
			return this.targetSet.size();
		}
	}


	/**
	 * 返回一个引入切面mixin，它允许将AOP代理转换为ThreadLocalInvokerStats.
	 */
	public IntroductionAdvisor getStatsMixin() {
		DelegatingIntroductionInterceptor dii = new DelegatingIntroductionInterceptor(this);
		return new DefaultIntroductionAdvisor(dii, ThreadLocalTargetSourceStats.class);
	}

}
