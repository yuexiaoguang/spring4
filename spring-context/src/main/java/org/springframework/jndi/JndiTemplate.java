package org.springframework.jndi;

import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.CollectionUtils;

/**
 * 简化了JNDI操作的Helper类.
 * 它提供了查找和绑定对象的方法, 并允许{@link JndiCallback}接口的实现通过提供的JNDI命名上下文执行他们喜欢的任何操作.
 */
public class JndiTemplate {

	protected final Log logger = LogFactory.getLog(getClass());

	private Properties environment;


	public JndiTemplate() {
	}

	public JndiTemplate(Properties environment) {
		this.environment = environment;
	}


	/**
	 * 设置JNDI InitialContext的环境.
	 */
	public void setEnvironment(Properties environment) {
		this.environment = environment;
	}

	/**
	 * 返回JNDI InitialContext的环境.
	 */
	public Properties getEnvironment() {
		return this.environment;
	}


	/**
	 * 执行给定的JNDI上下文回调实现.
	 * 
	 * @param contextCallback JndiCallback实现
	 * 
	 * @return 回调返回的结果对象, 或{@code null}
	 * @throws NamingException 由回调实现抛出
	 */
	public <T> T execute(JndiCallback<T> contextCallback) throws NamingException {
		Context ctx = getContext();
		try {
			return contextCallback.doInContext(ctx);
		}
		finally {
			releaseContext(ctx);
		}
	}

	/**
	 * 获取与此模板配置对应的JNDI上下文.
	 * 由{@link #execute}调用; 也可以直接调用.
	 * <p>默认实现委托给{@link #createInitialContext()}.
	 * 
	 * @return JNDI上下文 (never {@code null})
	 * @throws NamingException 如果上下文检索失败
	 */
	public Context getContext() throws NamingException {
		return createInitialContext();
	}

	/**
	 * 释放从{@link #getContext()}获取的JNDI上下文.
	 * 
	 * @param ctx 要释放的JNDI上下文 (may be {@code null})
	 */
	public void releaseContext(Context ctx) {
		if (ctx != null) {
			try {
				ctx.close();
			}
			catch (NamingException ex) {
				logger.debug("Could not close JNDI InitialContext", ex);
			}
		}
	}

	/**
	 * 创建一个新的JNDI初始上下文. 由{@link #getContext}调用.
	 * <p>默认实现使用此模板的环境设置.
	 * 可以为自定义上下文子类化, e.g. 用于测试.
	 * 
	 * @return 最初的Context实例
	 * @throws NamingException 初始化错误
	 */
	protected Context createInitialContext() throws NamingException {
		Hashtable<?, ?> icEnv = null;
		Properties env = getEnvironment();
		if (env != null) {
			icEnv = new Hashtable<Object, Object>(env.size());
			CollectionUtils.mergePropertiesIntoMap(env, icEnv);
		}
		return new InitialContext(icEnv);
	}


	/**
	 * 在当前JNDI上下文中查找具有给定名称的对象.
	 * 
	 * @param name 对象的JNDI名称
	 * 
	 * @return 找到的对象 (cannot be {@code null}; 如果不太好的JNDI实现返回null, 则抛出NamingException)
	 * @throws NamingException 如果没有绑定到JNDI的给定名称的对象
	 */
	public Object lookup(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up JNDI object with name [" + name + "]");
		}
		return execute(new JndiCallback<Object>() {
			@Override
			public Object doInContext(Context ctx) throws NamingException {
				Object located = ctx.lookup(name);
				if (located == null) {
					throw new NameNotFoundException(
							"JNDI object with [" + name + "] not found: JNDI implementation returned null");
				}
				return located;
			}
		});
	}

	/**
	 * 在当前JNDI上下文中查找具有给定名称的对象.
	 * 
	 * @param name 对象的JNDI名称
	 * @param requiredType JNDI对象必须匹配的类型.
	 * 可以是实际类的接口或超类, 也可以是{@code null}以用于任何匹配.
	 * 例如, 如果值为{@code Object.class}, 则此方法将成功, 无论返回的实例是什么类型.
	 * 
	 * @return 找到的对象(cannot be {@code null}; 如果不太好的JNDI实现返回null, 则抛出NamingException)
	 * @throws NamingException 如果没有绑定到JNDI的给定名称的对象
	 */
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name, Class<T> requiredType) throws NamingException {
		Object jndiObject = lookup(name);
		if (requiredType != null && !requiredType.isInstance(jndiObject)) {
			throw new TypeMismatchNamingException(
					name, requiredType, (jndiObject != null ? jndiObject.getClass() : null));
		}
		return (T) jndiObject;
	}

	/**
	 * 使用给定名称将给定对象绑定到当前JNDI上下文.
	 * 
	 * @param name 对象的JNDI名称
	 * @param object 要绑定的对象
	 * 
	 * @throws NamingException 由JNDI抛出, 大多数名字已经绑定
	 */
	public void bind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding JNDI object with name [" + name + "]");
		}
		execute(new JndiCallback<Object>() {
			@Override
			public Object doInContext(Context ctx) throws NamingException {
				ctx.bind(name, object);
				return null;
			}
		});
	}

	/**
	 * 使用给定名称将给定对象重新绑定到当前JNDI上下文.
	 * 覆盖任何现有绑定.
	 * 
	 * @param name 对象的JNDI名称
	 * @param object 要重新绑定的对象
	 * 
	 * @throws NamingException 由JNDI抛出
	 */
	public void rebind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Rebinding JNDI object with name [" + name + "]");
		}
		execute(new JndiCallback<Object>() {
			@Override
			public Object doInContext(Context ctx) throws NamingException {
				ctx.rebind(name, object);
				return null;
			}
		});
	}

	/**
	 * 从当前JNDI上下文中删除给定名称的绑定.
	 * 
	 * @param name 对象的JNDI名称
	 * 
	 * @throws NamingException 由JNDI引发, 主要是找不到名称
	 */
	public void unbind(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding JNDI object with name [" + name + "]");
		}
		execute(new JndiCallback<Object>() {
			@Override
			public Object doInContext(Context ctx) throws NamingException {
				ctx.unbind(name);
				return null;
			}
		});
	}
}
