package org.springframework.web.socket.adapter.standard;

import java.nio.ByteBuffer;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;
import org.springframework.web.context.ContextLoader;

/**
 * 可用于实现标准{@link javax.websocket.Encoder}和/或{@link javax.websocket.Decoder}的基类.
 * 它提供了委托给Spring {@link ConversionService}的编码和解码方法实现.
 *
 * <p>默认情况下, 此类在名称为{@code 'webSocketConversionService'}的
 * {@link #getApplicationContext() 活动的ApplicationContext}中查找{@link ConversionService}.
 * 这适用于Servlet容器环境中的客户端和服务器端点.
 * 如果没有在Servlet容器中运行, 子类将需要覆盖{@link #getConversionService()}方法以提供替代查找策略.
 *
 * <p>子类可以扩展这个类, 还应该实现{@link javax.websocket.Encoder}和{@link javax.websocket.Decoder}中的一个或两个.
 * 为方便起见, 提供了{@link ConvertingEncoderDecoderSupport.BinaryEncoder},
 * {@link ConvertingEncoderDecoderSupport.BinaryDecoder},
 * {@link ConvertingEncoderDecoderSupport.TextEncoder}
 * 和{@link ConvertingEncoderDecoderSupport.TextDecoder}子类.
 *
 * <p>由于JSR-356只允许按类型注册编码器/解码器, 因此该类的实例由WebSocket运行时管理, 不需要注册为Spring Bean.
 * 但是, 他们可以通过{@link Autowired @Autowire}注入Spring管理的依赖项.
 *
 * <p>要在{@link #getType() type}和{@code String}或{@code ByteBuffer}之间进行转换的转换器应该注册.
 *
 * @param <T> 要在Encoder和Decoder之间转换的类型
 * @param <M> WebSocket消息类型 ({@link String} 或 {@link ByteBuffer})
 */
public abstract class ConvertingEncoderDecoderSupport<T, M> {

	private static final String CONVERSION_SERVICE_BEAN_NAME = "webSocketConversionService";


	public void init(EndpointConfig config) {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null && applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableListableBeanFactory beanFactory =
					((ConfigurableApplicationContext) applicationContext).getBeanFactory();
			beanFactory.autowireBean(this);
		}
	}

	public void destroy() {
	}

	/**
	 * 用于获取{@link ConversionService}的策略方法.
	 * 默认情况下, 此方法需要{@link #getApplicationContext() 活动的ApplicationContext}中
	 * 名为{@code 'webSocketConversionService'}的bean.
	 * 
	 * @return {@link ConversionService} (never null)
	 */
	protected ConversionService getConversionService() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "Unable to locate the Spring ApplicationContext");
		try {
			return applicationContext.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
		}
		catch (BeansException ex) {
			throw new IllegalStateException("Unable to find ConversionService: please configure a '" +
					CONVERSION_SERVICE_BEAN_NAME + "' or override the getConversionService() method", ex);
		}
	}

	/**
	 * 返回活动的{@link ApplicationContext}.
	 * 默认情况下, 此方法通过{@link ContextLoader#getCurrentWebApplicationContext()}获取上下文,
	 * 该文件通常在Servlet容器环境中查找通过{@link ContextLoader}加载的ApplicationContext.
	 * 如果没有在Servlet容器中运行且不使用{@link ContextLoader}, 则应该重写此方法.
	 * 
	 * @return {@link ApplicationContext} 或 {@code null}
	 */
	protected ApplicationContext getApplicationContext() {
		return ContextLoader.getCurrentWebApplicationContext();
	}

	/**
	 * 返回要转换的类型.
	 * 默认使用类的泛型参数解析类型.
	 */
	protected TypeDescriptor getType() {
		return TypeDescriptor.valueOf(resolveTypeArguments()[0]);
	}

	/**
	 * 返回websocket消息类型.
	 * 默认使用类的泛型参数解析类型.
	 */
	protected TypeDescriptor getMessageType() {
		return TypeDescriptor.valueOf(resolveTypeArguments()[1]);
	}

	private Class<?>[] resolveTypeArguments() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(getClass(), ConvertingEncoderDecoderSupport.class);
		if (resolved == null) {
			throw new IllegalStateException("ConvertingEncoderDecoderSupport's generic types T and M " +
					"need to be substituted in subclass: " + getClass());
		}
		return resolved;
	}

	@SuppressWarnings("unchecked")
	public M encode(T object) throws EncodeException {
		try {
			return (M) getConversionService().convert(object, getType(), getMessageType());
		}
		catch (ConversionException ex) {
			throw new EncodeException(object, "Unable to encode websocket message using ConversionService", ex);
		}
	}

	public boolean willDecode(M bytes) {
		return getConversionService().canConvert(getType(), getMessageType());
	}

	@SuppressWarnings("unchecked")
	public T decode(M message) throws DecodeException {
		try {
			return (T) getConversionService().convert(message, getMessageType(), getType());
		}
		catch (ConversionException ex) {
			if (message instanceof String) {
				throw new DecodeException((String) message,
						"Unable to decode websocket message using ConversionService", ex);
			}
			if (message instanceof ByteBuffer) {
				throw new DecodeException((ByteBuffer) message,
						"Unable to decode websocket message using ConversionService", ex);
			}
			throw ex;
		}
	}


	/**
	 * 委托给Spring转换服务的二进制{@link javax.websocket.Encoder.Binary javax.websocket.Encoder}.
	 * See {@link ConvertingEncoderDecoderSupport} for details.
	 * 
	 * @param <T> 此编码器可以转换为的类型
	 */
	public static abstract class BinaryEncoder<T> extends ConvertingEncoderDecoderSupport<T, ByteBuffer>
			implements Encoder.Binary<T> {
	}


	/**
	 * 委托给Spring转换服务的二进制{@link javax.websocket.Encoder.Binary javax.websocket.Encoder}.
	 * See {@link ConvertingEncoderDecoderSupport} for details.
	 * 
	 * @param <T> 此解码器可以转换的类型
	 */
	public static abstract class BinaryDecoder<T> extends ConvertingEncoderDecoderSupport<T, ByteBuffer>
			implements Decoder.Binary<T> {
	}


	/**
	 * 委托给Spring转换服务的文本{@link javax.websocket.Encoder.Text javax.websocket.Encoder}.
	 * See {@link ConvertingEncoderDecoderSupport} for details.
	 * 
	 * @param <T> 此编码器可以转换为的类型
	 */
	public static abstract class TextEncoder<T> extends ConvertingEncoderDecoderSupport<T, String>
			implements Encoder.Text<T> {
	}


	/**
	 * 委托给Spring转换服务的文本{@link javax.websocket.Encoder.Text javax.websocket.Encoder}.
	 * See {@link ConvertingEncoderDecoderSupport} for details.
	 * 
	 * @param <T> 此解码器可以转换的类型
	 */
	public static abstract class TextDecoder<T> extends ConvertingEncoderDecoderSupport<T, String>
			implements Decoder.Text<T> {
	}

}
