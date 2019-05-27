package org.springframework.aop.support;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.IntroductionInfo;
import org.springframework.util.ClassUtils;

/**
 * 支持{@link org.springframework.aop.IntroductionInfo}的实现.
 *
 * <p>允许子类方便地添加给定对象的所有接口, 并禁止不应添加的接口. 还允许查询所有引入的接口.
 */
@SuppressWarnings("serial")
public class IntroductionInfoSupport implements IntroductionInfo, Serializable {

	protected final Set<Class<?>> publishedInterfaces = new LinkedHashSet<Class<?>>();

	private transient Map<Method, Boolean> rememberedMethods = new ConcurrentHashMap<Method, Boolean>(32);


	/**
	 * 取消指定的接口, 由于委托实现它可能已被自动检测. 调用此方法可以排除内部接口在代理级别可见.
	 * <p>如果代理未实现接口，则不执行任何操作.
	 * 
	 * @param ifc 要取消的接口
	 */
	public void suppressInterface(Class<?> ifc) {
		this.publishedInterfaces.remove(ifc);
	}

	@Override
	public Class<?>[] getInterfaces() {
		return ClassUtils.toClassArray(this.publishedInterfaces);
	}

	/**
	 * 检查指定的接口是否为已发布的引入接口.
	 * 
	 * @param ifc 要检查的接口
	 * 
	 * @return 接口是否是此引入的一部分
	 */
	public boolean implementsInterface(Class<?> ifc) {
		for (Class<?> pubIfc : this.publishedInterfaces) {
			if (ifc.isInterface() && ifc.isAssignableFrom(pubIfc)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 发布给定委托在代理级别实现的所有接口.
	 * 
	 * @param delegate 委托对象
	 */
	protected void implementInterfacesOnObject(Object delegate) {
		this.publishedInterfaces.addAll(ClassUtils.getAllInterfacesAsSet(delegate));
	}

	/**
	 * 这个方法是在引入的接口上?
	 * 
	 * @param mi 方法调用
	 * 
	 * @return 调用的方法是否在引入的接口上
	 */
	protected final boolean isMethodOnIntroducedInterface(MethodInvocation mi) {
		Boolean rememberedResult = this.rememberedMethods.get(mi.getMethod());
		if (rememberedResult != null) {
			return rememberedResult;
		}
		else {
			// Work it out and cache it.
			boolean result = implementsInterface(mi.getMethod().getDeclaringClass());
			this.rememberedMethods.put(mi.getMethod(), result);
			return result;
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	/**
	 * 此方法仅用于还原记录器.
	 * 不会使记录器保持静态，因为这意味着子类将使用此类的日志类别.
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依靠默认序列化; 只是在反序列化后初始化状态.
		ois.defaultReadObject();
		// Initialize transient fields.
		this.rememberedMethods = new ConcurrentHashMap<Method, Boolean>(32);
	}

}
