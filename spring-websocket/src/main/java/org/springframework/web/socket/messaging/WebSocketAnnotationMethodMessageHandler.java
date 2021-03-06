package org.springframework.web.socket.messaging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.web.method.ControllerAdviceBean;

/**
 * {@link SimpAnnotationMethodMessageHandler}的子类, 通过全局{@code @MessageExceptionHandler}方法
 * 为{@link org.springframework.web.bind.annotation.ControllerAdvice ControllerAdvice}提供支持.
 */
public class WebSocketAnnotationMethodMessageHandler extends SimpAnnotationMethodMessageHandler {

	public WebSocketAnnotationMethodMessageHandler(SubscribableChannel clientInChannel,
			MessageChannel clientOutChannel, SimpMessageSendingOperations brokerTemplate) {

		super(clientInChannel, clientOutChannel, brokerTemplate);
	}


	@Override
	public void afterPropertiesSet() {
		initControllerAdviceCache();
		super.afterPropertiesSet();
	}

	private void initControllerAdviceCache() {
		ApplicationContext context = getApplicationContext();
		if (context == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for @MessageExceptionHandler mappings: " + context);
		}
		List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(context);
		AnnotationAwareOrderComparator.sort(beans);
		initMessagingAdviceCache(MessagingControllerAdviceBean.createFromList(beans));
	}

	private void initMessagingAdviceCache(List<MessagingAdviceBean> beans) {
		if (beans == null) {
			return;
		}
		for (MessagingAdviceBean bean : beans) {
			Class<?> type = bean.getBeanType();
			AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(type);
			if (resolver.hasExceptionMappings()) {
				registerExceptionHandlerAdvice(bean, resolver);
				if (logger.isInfoEnabled()) {
					logger.info("Detected @MessageExceptionHandler methods in " + bean);
				}
			}
		}
	}


	/**
	 * 将ControllerAdviceBean适配为MessagingAdviceBean.
	 */
	private static class MessagingControllerAdviceBean implements MessagingAdviceBean {

		private final ControllerAdviceBean adviceBean;

		private MessagingControllerAdviceBean(ControllerAdviceBean adviceBean) {
			this.adviceBean = adviceBean;
		}

		public static List<MessagingAdviceBean> createFromList(List<ControllerAdviceBean> beans) {
			List<MessagingAdviceBean> result = new ArrayList<MessagingAdviceBean>(beans.size());
			for (ControllerAdviceBean bean : beans) {
				result.add(new MessagingControllerAdviceBean(bean));
			}
			return result;
		}

		@Override
		public Class<?> getBeanType() {
			return this.adviceBean.getBeanType();
		}

		@Override
		public Object resolveBean() {
			return this.adviceBean.resolveBean();
		}

		@Override
		public boolean isApplicableToBeanType(Class<?> beanType) {
			return this.adviceBean.isApplicableToBeanType(beanType);
		}

		@Override
		public int getOrder() {
			return this.adviceBean.getOrder();
		}
	}

}
