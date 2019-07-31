package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 创建一个{@link WebRequestDataBinder}实例, 并使用{@link WebBindingInitializer}初始化它.
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

	private final WebBindingInitializer initializer;


	/**
	 * @param initializer 用于全局数据绑定器初始化 (或{@code null})
	 */
	public DefaultDataBinderFactory(WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * 为给定的目标对象创建一个新的{@link WebDataBinder}, 并通过{@link WebBindingInitializer}初始化它.
	 * 
	 * @throws Exception 如果状态或参数无效
	 */
	@Override
	public final WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName)
			throws Exception {

		WebDataBinder dataBinder = createBinderInstance(target, objectName, webRequest);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder, webRequest);
		}
		initBinder(dataBinder, webRequest);
		return dataBinder;
	}

	/**
	 * 创建WebDataBinder实例的扩展点.
	 * 默认{@code WebRequestDataBinder}.
	 * 
	 * @param target 绑定目标或{@code null}仅用于类型转换
	 * @param objectName 绑定目标对象名称
	 * @param webRequest 当前请求
	 * 
	 * @throws Exception 如果状态或参数无效
	 */
	protected WebDataBinder createBinderInstance(Object target, String objectName, NativeWebRequest webRequest)
			throws Exception {

		return new WebRequestDataBinder(target, objectName);
	}

	/**
	 * 通过{@link WebBindingInitializer}进行"全局"初始化后,
	 * 进一步初始化创建的数据绑定器实例 (e.g. 使用{@code @InitBinder}方法)的扩展点.
	 * 
	 * @param dataBinder 要自定义的数据绑定器实例
	 * @param webRequest 当前请求
	 * 
	 * @throws Exception 如果初始化失败
	 */
	protected void initBinder(WebDataBinder dataBinder, NativeWebRequest webRequest) throws Exception {
	}

}
