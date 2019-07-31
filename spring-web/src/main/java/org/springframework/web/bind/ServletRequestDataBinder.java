package org.springframework.web.bind;

import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.util.WebUtils;

/**
 * 特殊{@link org.springframework.validation.DataBinder},
 * 用于执行从servlet请求参数到JavaBeans的数据绑定, 包括对多部分文件的支持.
 *
 * <p>有关自定义选项, 请参阅 DataBinder/WebDataBinder超类, 其中包括指定允许/必需字段, 以及注册自定义属性编辑器.
 *
 * <p>也可用于自定义Web控制器中的手动数据绑定:
 * 例如, 在普通的Controller实现中或在MultiActionController处理器方法中.
 * 只需为每个绑定进程实例化一个ServletRequestDataBinder, 并以当前的ServletRequest作为参数调用{@code bind}:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * ServletRequestDataBinder binder = new ServletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 */
public class ServletRequestDataBinder extends WebDataBinder {

	/**
	 * @param target 要绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 */
	public ServletRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * @param target 要绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的名称
	 */
	public ServletRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标, 并在multipart请求的情况下绑定multipart文件.
	 * <p>此调用可以创建字段错误, 表示基本绑定错误, 如必填字段 (code "required"), 或者值和bean属性之间的类型不匹配 (code "typeMismatch").
	 * <p>Multipart文件通过其参数名称绑定, 就像普通的HTTP参数一样:
	 * i.e. "uploadedFile"到"uploadedFile" bean属性, 调用"setUploadedFile" setter方法.
	 * <p>multipart文件的目标属性的类型可以是 MultipartFile, byte[], 或String.
	 * 后两者接收上传文件的内容; 在这些情况下, 所有元数据(如原始文件名, 内容类型等)都将丢失.
	 * 
	 * @param request 包含要绑定的参数的请求(可以是 multipart)
	 */
	public void bind(ServletRequest request) {
		MutablePropertyValues mpvs = new ServletRequestParameterPropertyValues(request);
		MultipartRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartRequest.class);
		if (multipartRequest != null) {
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		}
		addBindValues(mpvs, request);
		doBind(mpvs);
	}

	/**
	 * 子类可用于为请求添加额外绑定值的扩展点.
	 * 在{@link #doBind(MutablePropertyValues)}之前调用. 默认实现为空.
	 * 
	 * @param mpvs 将用于数据绑定的属性值
	 * @param request 当前请求
	 */
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
	}

	/**
	 * 将错误视为致命错误.
	 * <p>仅当输入无效时才使用此方法.
	 * 例如, 如果所有输入都来自下拉列表, 则这可能是合适的.
	 * 
	 * @throws ServletRequestBindingException 任何绑定问题的ServletException的子类
	 */
	public void closeNoCatch() throws ServletRequestBindingException {
		if (getBindingResult().hasErrors()) {
			throw new ServletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}

}
