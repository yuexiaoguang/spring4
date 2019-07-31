package org.springframework.web.method.annotation;

import java.util.Collections;
import java.util.List;

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * 通过{@code @InitBinder}方法向WebDataBinder添加初始化.
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

	private final List<InvocableHandlerMethod> binderMethods;


	/**
	 * @param binderMethods {@code @InitBinder}方法
	 * @param initializer 全局数据绑定器初始化的初始化器
	 */
	public InitBinderDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
		super(initializer);
		this.binderMethods = (binderMethods != null ? binderMethods : Collections.<InvocableHandlerMethod>emptyList());
	}


	/**
	 * <p>如果{@code @InitBinder}注解指定了属性名称, 则只有在名称包含目标对象名称时才会调用它.
	 * 
	 * @throws Exception 如果其中一个被调用的 @{@link InitBinder}方法失败
	 */
	@Override
	public void initBinder(WebDataBinder dataBinder, NativeWebRequest request) throws Exception {
		for (InvocableHandlerMethod binderMethod : this.binderMethods) {
			if (isBinderMethodApplicable(binderMethod, dataBinder)) {
				Object returnValue = binderMethod.invokeForRequest(request, null, dataBinder);
				if (returnValue != null) {
					throw new IllegalStateException(
							"@InitBinder methods must not return a value (should be void): " + binderMethod);
				}
			}
		}
	}

	/**
	 * 确定是否应使用给定的{@code @InitBinder}方法初始化给定的{@link WebDataBinder}实例.
	 * 默认情况下, 检查注解值中的指定属性名称.
	 */
	protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
		InitBinder ann = initBinderMethod.getMethodAnnotation(InitBinder.class);
		String[] names = ann.value();
		return (ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName()));
	}

}
