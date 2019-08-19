package org.springframework.web.servlet.mvc.method.annotation;

import java.util.List;

import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.InitBinderDataBinderFactory;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * 创建{@code ServletRequestDataBinder}.
 */
public class ServletRequestDataBinderFactory extends InitBinderDataBinderFactory {

	/**
	 * @param binderMethods 一个或多个{@code @InitBinder}方法
	 * @param initializer 提供全局数据绑定器初始化
	 */
	public ServletRequestDataBinderFactory(List<InvocableHandlerMethod> binderMethods, WebBindingInitializer initializer) {
		super(binderMethods, initializer);
	}

	/**
	 * 返回{@link ExtendedServletRequestDataBinder}的实例.
	 */
	@Override
	protected ServletRequestDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest request) {
		return new ExtendedServletRequestDataBinder(target, objectName);
	}

}
