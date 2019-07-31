package org.springframework.web.method.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 解析带{@code @ModelAttribute}注解的方法参数, 并处理从带{@code @ModelAttribute}注解的方法返回的值.
 *
 * <p>模型属性从模型中获取或使用默认构造函数创建 (然后添加到模型中).
 * 创建后, 通过数据绑定到Servlet请求参数来填充属性.
 * 如果参数使用{@code @javax.validation.Valid}注解
 * 或Spring自己的{@code @org.springframework.validation.annotation.Validated}注解, 则可以应用验证.
 *
 * <p>当使用{@code annotationNotRequired=true}创建此处理器时, 任何非简单类型参数和返回值都被视为模型属性,
 * 而不管{@code @ModelAttribute}注解是否存在.
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final boolean annotationNotRequired;


	/**
	 * @param annotationNotRequired 如果为"true", 则非简单方法参数和返回值被视为具有或不具有{@code @ModelAttribute}注解的模型属性
	 */
	public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
		this.annotationNotRequired = annotationNotRequired;
	}


	/**
	 * 如果参数使用{@link ModelAttribute}注解, 则返回{@code true}; 如果是默认解析模式, 则为非简单类型的任何方法参数.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(parameter.getParameterType())));
	}

	/**
	 * 从模型中解析参数, 或者如果没有找到, 则使用默认值将其实例化.
	 * 然后通过数据绑定使用请求值填充模型属性, 并且如果参数上存在{@code @java.validation.Valid}, 则可以选择验证.
	 * 
	 * @throws BindException 如果数据绑定和验证导致错误, 并且下一个方法参数不是{@link Errors}类型
	 * @throws Exception 如果WebDataBinder初始化失败
	 */
	@Override
	public final Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		String name = ModelFactory.getNameForParameter(parameter);
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		if (ann != null) {
			mavContainer.setBinding(name, ann.binding());
		}

		Object attribute = (mavContainer.containsAttribute(name) ? mavContainer.getModel().get(name) :
				createAttribute(name, parameter, binderFactory, webRequest));

		WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
		if (binder.getTarget() != null) {
			if (!mavContainer.isBindingDisabled(name)) {
				bindRequestParameters(binder, webRequest);
			}
			validateIfApplicable(binder, parameter);
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new BindException(binder.getBindingResult());
			}
		}

		// 在模型的末尾添加已解析的属性和BindingResult
		Map<String, Object> bindingResultModel = binder.getBindingResult().getModel();
		mavContainer.removeAttributes(bindingResultModel);
		mavContainer.addAllAttributes(bindingResultModel);

		return binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
	}

	/**
	 * 创建模型属性的扩展点, 如果在模型中找不到.
	 * 默认实现使用默认构造函数.
	 * 
	 * @param attributeName 属性的名称 (never {@code null})
	 * @param parameter 方法参数
	 * @param binderFactory 用于创建WebDataBinder实例
	 * @param webRequest 当前的请求
	 * 
	 * @return 创建的模型属性 (never {@code null})
	 */
	protected Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		return BeanUtils.instantiateClass(parameter.getParameterType());
	}

	/**
	 * 将请求绑定到目标对象的扩展点.
	 * 
	 * @param binder 用于绑定的数据绑定器实例
	 * @param request 当前的请求
	 */
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	/**
	 * 如果适用, 验证模型属性.
	 * <p>默认实现检查{@code @javax.validation.Valid}, Spring的{@link org.springframework.validation.annotation.Validated},
	 * 以及名称以"Valid"开头的自定义注解.
	 * 
	 * @param binder 要使用的DataBinder
	 * @param parameter 方法参数声明
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * 是否在验证错误上引发致命绑定异常.
	 * 
	 * @param binder 用于执行数据绑定的数据绑定器
	 * @param parameter 方法参数声明
	 * 
	 * @return {@code true} 如果下一个方法参数不是{@link Errors}类型
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

	/**
	 * 如果存在方法级{@code @ModelAttribute}, 则返回{@code true}; 或者在默认解析模式下, 返回非简单类型的任何返回值类型.
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(returnType.getParameterType())));
	}

	/**
	 * 将非null返回值添加到{@link ModelAndViewContainer}.
	 */
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue != null) {
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			mavContainer.addAttribute(name, returnValue);
		}
	}

}
