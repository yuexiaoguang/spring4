package org.springframework.web.portlet.mvc.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.portlet.ActionRequest;
import javax.portlet.ClientDataRequest;
import javax.portlet.Event;
import javax.portlet.EventRequest;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.WindowState;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.portlet.bind.PortletRequestBindingException;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.EventMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;
import org.springframework.web.portlet.handler.AbstractMapBasedHandlerMapping;
import org.springframework.web.portlet.handler.PortletRequestMethodNotSupportedException;

/**
 * {@link org.springframework.web.portlet.HandlerMapping}接口的实现,
 * 基于通过类型或方法级别的{@link RequestMapping}注解表示的portlet模式映射处理器.
 *
 * <p>默认在{@link org.springframework.web.portlet.DispatcherPortlet}中注册.
 * <b>NOTE:</b> 如果在DispatcherPortlet上下文中定义自定义HandlerMapping bean,
 * 则需要显式添加 DefaultAnnotationHandlerMapping bean, 因为自定义HandlerMapping bean会替换默认映射策略.
 * 定义DefaultAnnotationHandlerMapping还允许注册自定义拦截器:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.portlet.mvc.annotation.DefaultAnnotationHandlerMapping"&gt;
 *   &lt;property name="interceptors"&gt;
 *     ...
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * 带注解的控制器通常在类型级别用{@link Controller}构造型标记.
 * 当在类型级别应用{@link RequestMapping}时, 这不是绝对必要的
 * (因为这样的处理器通常实现了{@link org.springframework.web.portlet.mvc.Controller}接口).
 * 但是, 在方法级别检测{@link RequestMapping}注解需要{@link Controller}.
 *
 * <p><b>NOTE:</b> 方法级映射仅允许缩小在类级别表示的映射.
 * portlet模式与特定参数条件相结合需要唯一地映射到一个特定的处理器bean, 而不是分布在多个处理器bean中.
 * 强烈建议将相关的处理器方法放在同一个bean中.
 *
 * <p>{@link AnnotationMethodHandlerAdapter}负责处理带注解的处理器方法, 由此HandlerMapping映射.
 * 对于类型级别的{@link RequestMapping}, 特定的HandlerAdapters,
 * 例如{@link org.springframework.web.portlet.mvc.SimpleControllerHandlerAdapter}适用.
 */
public class DefaultAnnotationHandlerMapping extends AbstractMapBasedHandlerMapping<PortletMode> {

	/**
	 * 除了超类的初始化之外, 还调用{@code registerHandlers}方法.
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		detectHandlers();
	}

	/**
	 * 注册Portlet模式映射中为相应模式指定的所有处理器.
	 * 
	 * @throws org.springframework.beans.BeansException 如果处理器无法注册
	 */
	protected void detectHandlers() throws BeansException {
		ApplicationContext context = getApplicationContext();
		String[] beanNames = context.getBeanNamesForType(Object.class);
		for (String beanName : beanNames) {
			Class<?> handlerType = context.getType(beanName);
			RequestMapping mapping = context.findAnnotationOnBean(beanName, RequestMapping.class);
			if (mapping != null) {
				// @RequestMapping found at type level
				String[] modeKeys = mapping.value();
				String[] params = mapping.params();
				boolean registerHandlerType = true;
				if (modeKeys.length == 0 || params.length == 0) {
					registerHandlerType = !detectHandlerMethods(handlerType, beanName, mapping);
				}
				if (registerHandlerType) {
					AbstractParameterMappingPredicate predicate = new TypeLevelMappingPredicate(
							params, mapping.headers(), mapping.method());
					for (String modeKey : modeKeys) {
						registerHandler(new PortletMode(modeKey), beanName, predicate);
					}
				}
			}
			else if (AnnotationUtils.findAnnotation(handlerType, Controller.class) != null) {
				detectHandlerMethods(handlerType, beanName, mapping);
			}
		}
	}

	/**
	 * 从处理器的方法级映射派生portlet模式映射.
	 * 
	 * @param handlerType 要内省的处理器类型
	 * @param beanName 要内省的bean的名称
	 * @param typeMapping 类型级别映射
	 * 
	 * @return {@code true} 如果已注册至少1个处理器方法; 否则{@code false}
	 */
	protected boolean detectHandlerMethods(Class<?> handlerType, final String beanName, final RequestMapping typeMapping) {
		final Set<Boolean> handlersRegistered = new HashSet<Boolean>(1);
		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
		handlerTypes.add(handlerType);
		handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
		for (Class<?> currentHandlerType : handlerTypes) {
			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) {
					PortletRequestMappingPredicate predicate = null;
					String[] modeKeys = new String[0];
					String[] params = new String[0];
					if (typeMapping != null) {
						params = PortletAnnotationMappingUtils.mergeStringArrays(typeMapping.params(), params);
					}
					ActionMapping actionMapping = AnnotationUtils.findAnnotation(method, ActionMapping.class);
					RenderMapping renderMapping = AnnotationUtils.findAnnotation(method, RenderMapping.class);
					ResourceMapping resourceMapping = AnnotationUtils.findAnnotation(method, ResourceMapping.class);
					EventMapping eventMapping = AnnotationUtils.findAnnotation(method, EventMapping.class);
					RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
					if (actionMapping != null) {
						params = PortletAnnotationMappingUtils.mergeStringArrays(params, actionMapping.params());
						predicate = new ActionMappingPredicate(actionMapping.name(), params);
					}
					else if (renderMapping != null) {
						params = PortletAnnotationMappingUtils.mergeStringArrays(params, renderMapping.params());
						predicate = new RenderMappingPredicate(renderMapping.windowState(), params);
					}
					else if (resourceMapping != null) {
						predicate = new ResourceMappingPredicate(resourceMapping.value());
					}
					else if (eventMapping != null) {
						predicate = new EventMappingPredicate(eventMapping.value());
					}
					if (requestMapping != null) {
						modeKeys = requestMapping.value();
						if (typeMapping != null) {
							if (!PortletAnnotationMappingUtils.validateModeMapping(modeKeys, typeMapping.value())) {
								throw new IllegalStateException("Mode mappings conflict between method and type level: " +
										Arrays.asList(modeKeys) + " versus " + Arrays.asList(typeMapping.value()));
							}
						}
						params = PortletAnnotationMappingUtils.mergeStringArrays(params, requestMapping.params());
						if (predicate == null) {
							predicate = new MethodLevelMappingPredicate(params);
						}
					}
					if (predicate != null) {
						if (modeKeys.length == 0) {
							if (typeMapping != null) {
								modeKeys = typeMapping.value();
							}
							if (modeKeys.length == 0) {
								throw new IllegalStateException(
										"No portlet mode mappings specified - neither at type nor at method level");
							}
						}
						for (String modeKey : modeKeys) {
							registerHandler(new PortletMode(modeKey), beanName, predicate);
							handlersRegistered.add(Boolean.TRUE);
						}
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		return !handlersRegistered.isEmpty();
	}

	/**
	 * 使用当前的PortletMode作为查找键.
	 */
	@Override
	protected PortletMode getLookupKey(PortletRequest request) throws Exception {
		return request.getPortletMode();
	}


	private interface SpecialRequestTypePredicate {
	}


	private static abstract class AbstractParameterMappingPredicate implements PortletRequestMappingPredicate {

		private final String[] params;

		public AbstractParameterMappingPredicate(String[] params) {
			this.params = params;
		}

		@Override
		public boolean match(PortletRequest request) {
			return PortletAnnotationMappingUtils.checkParameters(this.params, request);
		}

		protected int compareParams(AbstractParameterMappingPredicate other) {
			return new Integer(other.params.length).compareTo(this.params.length);
		}

		protected int compareParams(Object other) {
			if (other instanceof AbstractParameterMappingPredicate) {
				return compareParams((AbstractParameterMappingPredicate) other);
			}
			return 0;
		}
	}


	private static class TypeLevelMappingPredicate extends AbstractParameterMappingPredicate {

		private final String[] headers;

		private final Set<String> methods = new HashSet<String>();

		public TypeLevelMappingPredicate(String[] params, String[] headers, RequestMethod[] methods) {
			super(params);
			this.headers = headers;
			if (methods != null) {
				for (RequestMethod method : methods) {
					this.methods.add(method.name());
				}
			}
		}

		@Override
		public void validate(PortletRequest request) throws PortletException {
			if (!PortletAnnotationMappingUtils.checkHeaders(this.headers, request)) {
				throw new PortletRequestBindingException("Header conditions \"" +
						StringUtils.arrayToDelimitedString(this.headers, ", ") +
						"\" not met for actual request");
			}
			if (!this.methods.isEmpty()) {
				if (!(request instanceof ClientDataRequest)) {
					throw new PortletRequestMethodNotSupportedException(StringUtils.toStringArray(this.methods));
				}
				String method = ((ClientDataRequest) request).getMethod();
				if (!this.methods.contains(method)) {
					throw new PortletRequestMethodNotSupportedException(method, StringUtils.toStringArray(this.methods));
				}
			}
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			return (other instanceof SpecialRequestTypePredicate ? -1 : compareParams(other));
		}
	}


	private static class MethodLevelMappingPredicate extends AbstractParameterMappingPredicate {

		public MethodLevelMappingPredicate(String[] params) {
			super(params);
		}

		@Override
		public void validate(PortletRequest request) throws PortletException {
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			return (other instanceof SpecialRequestTypePredicate ? 1 : compareParams(other));
		}
	}


	private static class ActionMappingPredicate extends AbstractParameterMappingPredicate implements SpecialRequestTypePredicate {

		private final String actionName;

		public ActionMappingPredicate(String actionName, String[] params) {
			super(params);
			this.actionName = actionName;
		}

		@Override
		public boolean match(PortletRequest request) {
			return (PortletRequest.ACTION_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE)) &&
					("".equals(this.actionName) || this.actionName.equals(request.getParameter(ActionRequest.ACTION_NAME))) &&
					super.match(request));
		}

		@Override
		public void validate(PortletRequest request) {
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			if (other instanceof TypeLevelMappingPredicate) {
				return 1;
			}
			else if (other instanceof ActionMappingPredicate) {
				ActionMappingPredicate otherAction = (ActionMappingPredicate) other;
				boolean hasActionName = "".equals(this.actionName);
				boolean otherHasActionName = "".equals(otherAction.actionName);
				if (hasActionName != otherHasActionName) {
					return (hasActionName ? -1 : 1);
				}
				else {
					return compareParams(otherAction);
				}
			}
			if (other instanceof SpecialRequestTypePredicate) {
				return this.getClass().getName().compareTo(other.getClass().getName());
			}
			return -1;
		}
	}


	private static class RenderMappingPredicate extends AbstractParameterMappingPredicate implements SpecialRequestTypePredicate{

		private final WindowState windowState;

		public RenderMappingPredicate(String windowState, String[] params) {
			super(params);
			this.windowState = ("".equals(windowState) ? null : new WindowState(windowState));
		}

		@Override
		public boolean match(PortletRequest request) {
			return (PortletRequest.RENDER_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE)) &&
					(this.windowState == null || this.windowState.equals(request.getWindowState())) &&
					super.match(request));
		}

		@Override
		public void validate(PortletRequest request) {
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			if (other instanceof TypeLevelMappingPredicate) {
				return 1;
			}
			else if (other instanceof RenderMappingPredicate) {
				RenderMappingPredicate otherRender = (RenderMappingPredicate) other;
				boolean hasWindowState = (this.windowState != null);
				boolean otherHasWindowState = (otherRender.windowState != null);
				if (hasWindowState != otherHasWindowState) {
					return (hasWindowState ? -1 : 1);
				}
				else {
					return compareParams(otherRender);
				}
			}
			if (other instanceof SpecialRequestTypePredicate) {
				return this.getClass().getName().compareTo(other.getClass().getName());
			}
			return -1;
		}
	}


	private static class ResourceMappingPredicate implements PortletRequestMappingPredicate, SpecialRequestTypePredicate {

		private final String resourceId;

		public ResourceMappingPredicate(String resourceId) {
			this.resourceId = resourceId;
		}

		@Override
		public boolean match(PortletRequest request) {
			return (PortletRequest.RESOURCE_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE)) &&
					("".equals(this.resourceId) || this.resourceId.equals(((ResourceRequest) request).getResourceID())));
		}

		@Override
		public void validate(PortletRequest request) {
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			if (other instanceof ResourceMappingPredicate) {
				boolean hasResourceId = !"".equals(this.resourceId);
				boolean otherHasResourceId = !"".equals(((ResourceMappingPredicate) other).resourceId);
				if (hasResourceId != otherHasResourceId) {
					return (hasResourceId ? -1 : 1);
				}
			}
			if (other instanceof SpecialRequestTypePredicate) {
				return this.getClass().getName().compareTo(other.getClass().getName());
			}
			return -1;
		}
	}


	private static class EventMappingPredicate implements PortletRequestMappingPredicate, SpecialRequestTypePredicate {

		private final String eventName;

		public EventMappingPredicate(String eventName) {
			this.eventName = eventName;
		}

		@Override
		public boolean match(PortletRequest request) {
			if (!PortletRequest.EVENT_PHASE.equals(request.getAttribute(PortletRequest.LIFECYCLE_PHASE))) {
				return false;
			}
			if ("".equals(this.eventName)) {
				return true;
			}
			Event event = ((EventRequest) request).getEvent();
			return (this.eventName.equals(event.getName()) || this.eventName.equals(event.getQName().toString()));
		}

		@Override
		public void validate(PortletRequest request) {
		}

		@Override
		public int compareTo(PortletRequestMappingPredicate other) {
			if (other instanceof EventMappingPredicate) {
				boolean hasEventName = !"".equals(this.eventName);
				boolean otherHasEventName = !"".equals(((EventMappingPredicate) other).eventName);
				if (hasEventName != otherHasEventName) {
					return (hasEventName ? -1 : 1);
				}
			}
			if (other instanceof SpecialRequestTypePredicate) {
				return this.getClass().getName().compareTo(other.getClass().getName());
			}
			return -1;
		}
	}

}
