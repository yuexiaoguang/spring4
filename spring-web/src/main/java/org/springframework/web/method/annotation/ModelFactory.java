package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 在调用控制器方法之前协助初始化{@link Model}, 并在调用之后对其进行更新.
 *
 * <p>在初始化时, 模型使用临时存储在会话中的属性, 并通过调用{@code @ModelAttribute}方法填充.
 *
 * <p>在更新模型时, 属性与会话同步, 如果缺少, 还会添加{@link BindingResult}属性.
 */
public final class ModelFactory {

	private static final Log logger = LogFactory.getLog(ModelFactory.class);

	private final List<ModelMethod> modelMethods = new ArrayList<ModelMethod>();

	private final WebDataBinderFactory dataBinderFactory;

	private final SessionAttributesHandler sessionAttributesHandler;


	/**
	 * @param handlerMethods 要调用的{@code @ModelAttribute}方法
	 * @param binderFactory 用于准备{@link BindingResult}属性
	 * @param attributeHandler 用于访问会话属性
	 */
	public ModelFactory(List<InvocableHandlerMethod> handlerMethods,
			WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {

		if (handlerMethods != null) {
			for (InvocableHandlerMethod handlerMethod : handlerMethods) {
				this.modelMethods.add(new ModelMethod(handlerMethod));
			}
		}
		this.dataBinderFactory = binderFactory;
		this.sessionAttributesHandler = attributeHandler;
	}


	/**
	 * 按以下顺序填充模型:
	 * <ol>
	 * <li>检索"已知"的会话属性, 作为{@code @SessionAttributes}列出.
	 * <li>调用{@code @ModelAttribute}方法
	 * <li>查找{@code @ModelAttribute}方法参数, 这些参数也作为{@code @SessionAttributes}列出,
	 * 并确保它们出现在模型中, 必要时会引发异常.
	 * </ol>
	 * 
	 * @param request 当前的请求
	 * @param container 具有要初始化的模型的容器
	 * @param handlerMethod 用于模型初始化的方法
	 * 
	 * @throws Exception 可能来自{@code @ModelAttribute}方法
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer container, HandlerMethod handlerMethod)
			throws Exception {

		Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
		container.mergeAttributes(sessionAttributes);
		invokeModelAttributeMethods(request, container);

		for (String name : findSessionAttributeArguments(handlerMethod)) {
			if (!container.containsAttribute(name)) {
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'", name);
				}
				container.addAttribute(name, value);
			}
		}
	}

	/**
	 * 调用模型属性方法以填充模型.
	 * 仅当模型中尚未存在属性时才添加属性.
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container)
			throws Exception {

		while (!this.modelMethods.isEmpty()) {
			InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
			ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
			if (container.containsAttribute(ann.name())) {
				if (!ann.binding()) {
					container.setBindingDisabled(ann.name());
				}
				continue;
			}

			Object returnValue = modelMethod.invokeForRequest(request, container);
			if (!modelMethod.isVoid()){
				String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
				if (!ann.binding()) {
					container.setBindingDisabled(returnValueName);
				}
				if (!container.containsAttribute(returnValueName)) {
					container.addAttribute(returnValueName, returnValue);
				}
			}
		}
	}

	private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
		for (ModelMethod modelMethod : this.modelMethods) {
			if (modelMethod.checkDependencies(container)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Selected @ModelAttribute method " + modelMethod);
				}
				this.modelMethods.remove(modelMethod);
				return modelMethod;
			}
		}
		ModelMethod modelMethod = this.modelMethods.get(0);
		if (logger.isTraceEnabled()) {
			logger.trace("Selected @ModelAttribute method (not present: " +
					modelMethod.getUnresolvedDependencies(container)+ ") " + modelMethod);
		}
		this.modelMethods.remove(modelMethod);
		return modelMethod;
	}

	/**
	 * 查找{@code @ModelAttribute}参数, 由{@code @SessionAttributes}列出.
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		List<String> result = new ArrayList<String>();
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(parameter);
				Class<?> paramType = parameter.getParameterType();
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					result.add(name);
				}
			}
		}
		return result;
	}

	/**
	 * 将由{@code @SessionAttributes}列出的模型属性放入会话.
	 * 必要时添加{@link BindingResult}属性.
	 * 
	 * @param request 当前的请求
	 * @param container 包含要更新的模型
	 * 
	 * @throws Exception 如果创建BindingResult属性失败
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
		ModelMap defaultModel = container.getDefaultModel();
		if (container.getSessionStatus().isComplete()){
			this.sessionAttributesHandler.cleanupAttributes(request);
		}
		else {
			this.sessionAttributesHandler.storeAttributes(request, defaultModel);
		}
		if (!container.isRequestHandled() && container.getModel() == defaultModel) {
			updateBindingResult(request, defaultModel);
		}
	}

	/**
	 * 将{@link BindingResult}属性添加到模型中.
	 */
	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
		List<String> keyNames = new ArrayList<String>(model.keySet());
		for (String name : keyNames) {
			Object value = model.get(name);
			if (isBindingCandidate(name, value)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
				if (!model.containsAttribute(bindingResultKey)) {
					WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
					model.put(bindingResultKey, dataBinder.getBindingResult());
				}
			}
		}
	}

	/**
	 * 给定属性是否需要模型中的{@link BindingResult}.
	 */
	private boolean isBindingCandidate(String attributeName, Object value) {
		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			return false;
		}

		Class<?> attrType = (value != null ? value.getClass() : null);
		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}

		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}


	/**
	 * 基于{@code @ModelAttribute}参数注解或基于参数类型的约定, 派生给定方法参数的模型属性名称.
	 * 
	 * @param parameter 方法参数的描述符
	 * 
	 * @return 派生的名称
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		String name = (ann != null ? ann.value() : null);
		return (StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter));
	}

	/**
	 * 根据给定的返回值派生模型属性名称:
	 * <ol>
	 * <li>方法 {@code ModelAttribute}注解值
	 * <li>声明的返回类型, 如果它比{@code Object}更具体
	 * <li>实际的返回值类型
	 * </ol>
	 * 
	 * @param returnValue 从方法调用返回的值
	 * @param returnType 该方法的返回类型的描述符
	 * 
	 * @return 派生的名称 (不会是{@code null}或空字符串)
	 */
	public static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			return ann.value();
		}
		else {
			Method method = returnType.getMethod();
			Class<?> containingClass = returnType.getContainingClass();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}


	private static class ModelMethod {

		private final InvocableHandlerMethod handlerMethod;

		private final Set<String> dependencies = new HashSet<String>();

		public ModelMethod(InvocableHandlerMethod handlerMethod) {
			this.handlerMethod = handlerMethod;
			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
				if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
					this.dependencies.add(getNameForParameter(parameter));
				}
			}
		}

		public InvocableHandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public boolean checkDependencies(ModelAndViewContainer mavContainer) {
			for (String name : this.dependencies) {
				if (!mavContainer.containsAttribute(name)) {
					return false;
				}
			}
			return true;
		}

		public List<String> getUnresolvedDependencies(ModelAndViewContainer mavContainer) {
			List<String> result = new ArrayList<String>(this.dependencies.size());
			for (String name : this.dependencies) {
				if (!mavContainer.containsAttribute(name)) {
					result.add(name);
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return this.handlerMethod.getMethod().toGenericString();
		}
	}

}
