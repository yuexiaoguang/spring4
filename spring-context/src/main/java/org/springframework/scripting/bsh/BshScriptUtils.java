package org.springframework.scripting.bsh;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.Primitive;
import bsh.XThis;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 用于处理BeanShell脚本对象的实用方法.
 */
public abstract class BshScriptUtils {

	/**
	 * 从给定的脚本源创建一个新的BeanShell脚本对象.
	 * <p>使用此{@code createBshObject}变体, 脚本需要声明一个完整的类或返回脚本对象的实际实例.
	 * 
	 * @param scriptSource 脚本源文本
	 * 
	 * @return 脚本Java对象
	 * @throws EvalError 在BeanShell解析失败的情况下
	 */
	public static Object createBshObject(String scriptSource) throws EvalError {
		return createBshObject(scriptSource, null, null);
	}

	/**
	 * 使用默认的ClassLoader从给定的脚本源创建新的BeanShell脚本对象.
	 * <p>该脚本可以是一个需要生成相应代理的简单脚本 (实现指定的接口),
	 * 或声明一个完整的类或返回脚本对象的实际实例 (在这种情况下, 指定的接口, 如果有的话, 需要由该类/实例实现).
	 * 
	 * @param scriptSource 脚本源文本
	 * @param scriptInterfaces 脚本Java对象应该实现的接口
	 * (如果脚本本身声明一个完整的类或返回脚本对象的实际实例, 则可能是{@code null}或为空)
	 * 
	 * @return 脚本Java对象
	 * @throws EvalError 在BeanShell解析失败的情况下
	 */
	public static Object createBshObject(String scriptSource, Class<?>... scriptInterfaces) throws EvalError {
		return createBshObject(scriptSource, scriptInterfaces, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 从给定的脚本源创建一个新的BeanShell脚本对象.
	 * <p>该脚本可以是一个需要生成相应代理的简单脚本 (实现指定的接口),
	 * 或声明一个完整的类或返回脚本对象的实际实例 (在这种情况下, 指定的接口, 如果有的话, 需要由该类/实例实现).
	 * 
	 * @param scriptSource 脚本源文本
	 * @param scriptInterfaces 脚本Java对象应该实现的接口
	 * (如果脚本本身声明一个完整的类或返回脚本对象的实际实例, 则可能是{@code null}或为空)
	 * @param classLoader 用于评估脚本的ClassLoader
	 * 
	 * @return 脚本Java对象
	 * @throws EvalError 在BeanShell解析失败的情况下
	 */
	public static Object createBshObject(String scriptSource, Class<?>[] scriptInterfaces, ClassLoader classLoader)
			throws EvalError {

		Object result = evaluateBshScript(scriptSource, scriptInterfaces, classLoader);
		if (result instanceof Class) {
			Class<?> clazz = (Class<?>) result;
			try {
				return clazz.newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			return result;
		}
	}

	/**
	 * 根据给定的脚本源评估指定的BeanShell脚本, 返回脚本定义的Class.
	 * <p>该脚本可以声明一个完整的类, 也可以返回脚本对象的实际实例 (在这种情况下, 将返回该对象的Class).
	 * 在任何其他情况下, 返回的类将是{@code null}.
	 * 
	 * @param scriptSource 脚本源文本
	 * @param classLoader 用于评估脚本的ClassLoader
	 * 
	 * @return 脚本化的Java类; 如果无法确定, 则为{@code null}
	 * @throws EvalError 在BeanShell解析失败的情况下
	 */
	static Class<?> determineBshObjectType(String scriptSource, ClassLoader classLoader) throws EvalError {
		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		interpreter.setClassLoader(classLoader);
		Object result = interpreter.eval(scriptSource);
		if (result instanceof Class) {
			return (Class<?>) result;
		}
		else if (result != null) {
			return result.getClass();
		}
		else {
			return null;
		}
	}

	/**
	 * 根据给定的脚本源评估指定的BeanShell脚本, 保持返回的脚本Class或脚本Object.
	 * <p>该脚本可以是一个需要生成相应代理的简单脚本 (实现指定的接口),
	 * 或声明一个完整的类或返回脚本对象的实际实例 (在这种情况下, 指定的接口, 如果有的话, 需要由该类/实例实现).
	 * 
	 * @param scriptSource 脚本源文本
	 * @param scriptInterfaces 脚本Java对象应该实现的接口
	 * (如果脚本本身声明一个完整的类或返回脚本对象的实际实例, 则可能是{@code null}或为空)
	 * @param classLoader 用于评估脚本的ClassLoader
	 * 
	 * @return 脚本的Java类或Java对象
	 * @throws EvalError 在BeanShell解析失败的情况下
	 */
	static Object evaluateBshScript(String scriptSource, Class<?>[] scriptInterfaces, ClassLoader classLoader)
			throws EvalError {

		Assert.hasText(scriptSource, "Script source must not be empty");
		Interpreter interpreter = new Interpreter();
		interpreter.setClassLoader(classLoader);
		Object result = interpreter.eval(scriptSource);
		if (result != null) {
			return result;
		}
		else {
			// Simple BeanShell script: 为它创建一个代理, 实现给定的接口.
			Assert.notEmpty(scriptInterfaces,
					"Given script requires a script proxy: At least one script interface is required.");
			XThis xt = (XThis) interpreter.eval("return this");
			return Proxy.newProxyInstance(classLoader, scriptInterfaces, new BshObjectInvocationHandler(xt));
		}
	}


	/**
	 * 调用BeanShell脚本方法的InvocationHandler.
	 */
	private static class BshObjectInvocationHandler implements InvocationHandler {

		private final XThis xt;

		public BshObjectInvocationHandler(XThis xt) {
			this.xt = xt;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				return (isProxyForSameBshObject(args[0]));
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				return this.xt.hashCode();
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "BeanShell object [" + this.xt + "]";
			}
			try {
				Object result = this.xt.invokeMethod(method.getName(), args);
				if (result == Primitive.NULL || result == Primitive.VOID) {
					return null;
				}
				if (result instanceof Primitive) {
					return ((Primitive) result).getValue();
				}
				return result;
			}
			catch (EvalError ex) {
				throw new BshExecutionException(ex);
			}
		}

		private boolean isProxyForSameBshObject(Object other) {
			if (!Proxy.isProxyClass(other.getClass())) {
				return false;
			}
			InvocationHandler ih = Proxy.getInvocationHandler(other);
			return (ih instanceof BshObjectInvocationHandler &&
					this.xt.equals(((BshObjectInvocationHandler) ih).xt));
		}
	}


	/**
	 * 脚本执行失败抛出的异常.
	 */
	@SuppressWarnings("serial")
	public static class BshExecutionException extends NestedRuntimeException {

		private BshExecutionException(EvalError ex) {
			super("BeanShell script execution failed", ex);
		}
	}
}
