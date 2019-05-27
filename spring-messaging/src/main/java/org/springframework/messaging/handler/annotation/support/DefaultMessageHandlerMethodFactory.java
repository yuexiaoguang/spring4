package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.validation.Validator;

/**
 * 默认的{@link MessageHandlerMethodFactory}实现使用必要的{@link HandlerMethodArgumentResolver}实例
 * 创建{@link InvocableHandlerMethod}, 以检测和处理
 * {@link org.springframework.messaging.handler.annotation.MessageMapping MessageMapping}定义的大多数用例.
 *
 * <p>可以添加额外的方法参数解析器来自定义可以处理的方法签名.
 *
 * <p>默认情况下, 验证过程重定向到无操作实现, 请参阅{@link #setValidator(Validator)}以自定义它.
 * 可以以类似的方式自定义{@link ConversionService}以调整消息有效负载的转换方式
 */
public class DefaultMessageHandlerMethodFactory implements MessageHandlerMethodFactory, BeanFactoryAware, InitializingBean {

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private MessageConverter messageConverter;

	private Validator validator;

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private BeanFactory beanFactory;


	/**
	 * 设置{@link ConversionService}, 用于转换原始消息有效负载或header.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 设置要使用的{@link MessageConverter}. 默认使用{@link GenericMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 设置用于验证@Payload 参数的Validator实例
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * 设置将在解析器之后用于支持的参数类型的自定义{@code HandlerMethodArgumentResolver}的列表.
	 * 
	 * @param customArgumentResolvers 解析器列表 (never {@code null})
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers = customArgumentResolvers;
	}

	/**
	 * 配置支持的参数类型的完整列表, 有效地覆盖默认配置的参数类型.
	 * 这是一个高级选项. 对于大多数用例, 使用{@link #setCustomArgumentResolvers(java.util.List)}就足够了.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	/**
	 * 只需要{@link BeanFactory}用于处理器方法参数中的占位符解析; 它是可选的.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.messageConverter == null) {
			this.messageConverter = new GenericMessageConverter(this.conversionService);
		}
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}
	}


	@Override
	public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(bean, method);
		handlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);
		return handlerMethod;
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();
		ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory ?
				(ConfigurableBeanFactory) this.beanFactory : null);

		// 基于注解的参数解析
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, cbf));
		resolvers.add(new HeadersMethodArgumentResolver());

		// 基于类型的参数解析
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		if (this.customArgumentResolvers != null) {
			resolvers.addAll(this.customArgumentResolvers);
		}
		resolvers.add(new PayloadArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

}
