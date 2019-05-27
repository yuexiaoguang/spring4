package org.springframework.jms.listener.adapter;

import java.lang.reflect.InvocationTargetException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;

/**
 * 消息监听器适配器, 通过反射将消息处理委派给目标监听器方法, 并具有灵活的消息类型转换.
 * 允许监听器方法对消息内容类型进行操作, 完全独立于JMS API.
 *
 * <p>默认情况下, 传入的JMS消息的内容在传递到目标监听器方法之前被提取,
 * 以使目标方法对消息内容类型(如String或字节数组), 而不是原始{@link Message}进行操作.
 * 消息类型转换委托给Spring JMS {@link MessageConverter}. 默认将使用{@link SimpleMessageConverter}.
 * (如果不希望发生此类自动消息转换, 将{@link #setMessageConverter MessageConverter}设置为{@code null}.)
 *
 * <p>如果目标侦听器方法返回非null 对象(通常是消息内容类型, 如{@code String}或字节数组),
 * 它将被包装在JMS {@code Message}中并发送到响应目标
 * (JMS "回复"目标或{@link #setDefaultResponseDestination(javax.jms.Destination) 指定的默认目标}).
 *
 * <p><b>Note:</b> 只有在使用{@link SessionAwareMessageListener}入口点 (通常通过Spring消息监听器容器)时才能发送响应消息.
 * 标准JMS {@link MessageListener} <i>不</i>支持生成响应消息.
 *
 * <p>下面是一些符合此适配器类的方法签名示例.
 * 第一个示例处理所有{@code Message}类型, 并将每个{@code Message}类型的内容作为参数传递.
 * 由于所有这些方法都返回{@code void}, 因此不会发送{@code Message}.
 *
 * <pre class="code">public interface MessageContentsDelegate {
 *    void handleMessage(String text);
 *    void handleMessage(Map map);
 *    void handleMessage(byte[] bytes);
 *    void handleMessage(Serializable obj);
 * }</pre>
 *
 * 下一个示例处理所有{@code Message}类型, 并将实际 (原始) {@code Message}作为参数传递.
 * 同样, 由于所有这些方法都返回{@code void}, 因此不会发送{@code Message}.
 *
 * <pre class="code">public interface RawMessageDelegate {
 *    void handleMessage(TextMessage message);
 *    void handleMessage(MapMessage message);
 *    void handleMessage(BytesMessage message);
 *    void handleMessage(ObjectMessage message);
 * }</pre>
 *
 * 下一个示例演示了一个{@code Message}委托, 它只消费{@link javax.jms.TextMessage TextMessages}的{@code String}内容.
 * 另请注意{@code Message}处理方法的名称与{@link #ORIGINAL_DEFAULT_LISTENER_METHOD 原始}的不同之处 (必须在伴随的bean定义中配置).
 * 同样, 当方法返回{@code void}时, 不会发回{@code Message}.
 *
 * <pre class="code">public interface TextMessageContentDelegate {
 *    void onMessage(String text);
 * }</pre>
 *
 * 最后一个示例说明了{@code Message}委托, 它只消费{@link javax.jms.TextMessage TextMessages}的{@code String}内容.
 * 请注意此方法的返回类型是{@code String}: 这将导致配置的{@link MessageListenerAdapter}在响应中发送{@link javax.jms.TextMessage}.
 *
 * <pre class="code">public interface ResponsiveTextMessageContentDelegate {
 *    String handleMessage(String text);
 * }</pre>
 *
 * 有关更多示例和讨论, 请参阅Spring参考文档, 该文档详细描述了此类 (及其附带的XML配置).
 */
public class MessageListenerAdapter extends AbstractAdaptableMessageListener implements SubscriptionNameProvider {

	/**
	 * 默认监听器方法的开箱即用值: "handleMessage".
	 */
	public static final String ORIGINAL_DEFAULT_LISTENER_METHOD = "handleMessage";


	private Object delegate;

	private String defaultListenerMethod = ORIGINAL_DEFAULT_LISTENER_METHOD;


	@SuppressWarnings("deprecation")
	public MessageListenerAdapter() {
		initDefaultStrategies();
		this.delegate = this;
	}

	/**
	 * @param delegate 委托对象
	 */
	@SuppressWarnings("deprecation")
	public MessageListenerAdapter(Object delegate) {
		initDefaultStrategies();
		setDelegate(delegate);
	}


	/**
	 * 初始化适配器策略的默认实现.
	 * 
	 * @deprecated as of 4.1, in favor of calling the corresponding setters
	 * in the subclass constructor
	 */
	@Deprecated
	protected void initDefaultStrategies() {
	}

	/**
	 * 设置委托其监听消息的目标对象.
	 * 必须在此目标对象上存在指定的监听器方法.
	 * <p>如果未指定显式委托对象, 则应在此适配器实例上存在监听器方法, 即在此适配器的自定义子类上, 定义监听器方法.
	 */
	public void setDelegate(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * 返回委托其监听消息的目标对象.
	 */
	protected Object getDelegate() {
		return this.delegate;
	}

	/**
	 * 指定要委托给的默认监听器方法的名称, 用于未确定特定监听器方法的情况.
	 * 开箱即用的值是{@link #ORIGINAL_DEFAULT_LISTENER_METHOD "handleMessage"}.
	 */
	public void setDefaultListenerMethod(String defaultListenerMethod) {
		this.defaultListenerMethod = defaultListenerMethod;
	}

	/**
	 * 返回要委托给的默认监听器方法的名称.
	 */
	protected String getDefaultListenerMethod() {
		return this.defaultListenerMethod;
	}


	/**
	 * Spring {@link SessionAwareMessageListener}入口点.
	 * <p>将消息委派给目标监听器方法, 并对message参数进行适当的转换.
	 * 如果目标方法返回非null对象, 请包装JMS消息并将其发回.
	 * 
	 * @param message 传入的JMS消息
	 * @param session 要运行的JMS会话
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void onMessage(Message message, Session session) throws JMSException {
		// 检查委托是否是MessageListener 实现本身.
		// 在这种情况下, 适配器将只是作为传递.
		Object delegate = getDelegate();
		if (delegate != this) {
			if (delegate instanceof SessionAwareMessageListener) {
				if (session != null) {
					((SessionAwareMessageListener<Message>) delegate).onMessage(message, session);
					return;
				}
				else if (!(delegate instanceof MessageListener)) {
					throw new javax.jms.IllegalStateException("MessageListenerAdapter cannot handle a " +
							"SessionAwareMessageListener delegate if it hasn't been invoked with a Session itself");
				}
			}
			if (delegate instanceof MessageListener) {
				((MessageListener) delegate).onMessage(message);
				return;
			}
		}

		// 常规案例: 反射查找处理器方法.
		Object convertedMessage = extractMessage(message);
		String methodName = getListenerMethodName(message, convertedMessage);
		if (methodName == null) {
			throw new javax.jms.IllegalStateException("No default listener method specified: " +
					"Either specify a non-null value for the 'defaultListenerMethod' property or " +
					"override the 'getListenerMethodName' method.");
		}

		// 使用适当的参数调用处理器方法.
		Object[] listenerArguments = buildListenerArguments(convertedMessage);
		Object result = invokeListenerMethod(methodName, listenerArguments);
		if (result != null) {
			handleResult(result, message, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	@Override
	public String getSubscriptionName() {
		Object delegate = getDelegate();
		if (delegate != this && delegate instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) delegate).getSubscriptionName();
		}
		else {
			return delegate.getClass().getName();
		}
	}

	/**
	 * 确定应该处理给定消息的监听器方法的名称.
	 * <p>默认实现只返回配置的默认监听器方法.
	 * 
	 * @param originalMessage JMS请求消息
	 * @param extractedMessage 转换后的JMS请求消息, 作为参数传递给监听器方法
	 * 
	 * @return 监听器方法的名称 (never {@code null})
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected String getListenerMethodName(Message originalMessage, Object extractedMessage) throws JMSException {
		return getDefaultListenerMethod();
	}

	/**
	 * 构建要传递给目标监听器方法的参数数组.
	 * 允许从单个消息对象构建多个方法参数.
	 * <p>默认实现使用给定的消息对象作为唯一元素构建数组.
	 * 这意味着提取的消息将始终传递到<i>单个</i>方法参数, 即使它是一个数组, 目标方法具有声明的数组类型的相应单个参数.
	 * <p>这可以被覆盖以不同地处理特殊消息内容(例如数组), 例如将消息数组的每个元素作为不同的方法参数传递.
	 * 
	 * @param extractedMessage 消息的内容
	 * 
	 * @return 要传递给监听器方法的参数数组 (数组的每个元素对应于不同的方法参数)
	 */
	protected Object[] buildListenerArguments(Object extractedMessage) {
		return new Object[] {extractedMessage};
	}

	/**
	 * 调用指定的监听器方法.
	 * 
	 * @param methodName 监听器方法的名称
	 * @param arguments 要传入的消息参数
	 * 
	 * @return 从监听器方法返回的结果
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Object invokeListenerMethod(String methodName, Object[] arguments) throws JMSException {
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(getDelegate());
			methodInvoker.setTargetMethod(methodName);
			methodInvoker.setArguments(arguments);
			methodInvoker.prepare();
			return methodInvoker.invoke();
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof JMSException) {
				throw (JMSException) targetEx;
			}
			else {
				throw new ListenerExecutionFailedException(
						"Listener method '" + methodName + "' threw exception", targetEx);
			}
		}
		catch (Throwable ex) {
			throw new ListenerExecutionFailedException("Failed to invoke target method '" + methodName +
					"' with arguments " + ObjectUtils.nullSafeToString(arguments), ex);
		}
	}

}
