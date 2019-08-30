package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 特殊{@link org.springframework.validation.DataBinder},
 * 用于执行从portlet请求参数到JavaBeans的数据绑定, 包括对multipart文件的支持.
 *
 * <p>有关自定义选项, 请参阅DataBinder/WebDataBinder超类, 其中包括指定允许/必需的字段以及注册自定义属性编辑器.
 *
 * <p>也可用于自定义Web控制器中的手动数据绑定: 例如, 在一个普通的Portlet控制器实现中.
 * 只需为每个绑定过程实例化一个PortletRequestDataBinder, 并以当前的PortletRequest作为参数调用{@code bind}:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * PortletRequestDataBinder binder = new PortletRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 */
public class PortletRequestDataBinder extends WebDataBinder {

	/**
	 * 使用默认对象名称.
	 * 
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 */
	public PortletRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的名称
	 */
	public PortletRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标, 并在multipart请求的情况下绑定multipart文件.
	 * <p>此调用可以创建字段错误, 表示基本绑定错误, 如必填字段 (code "required"),
	 * 或者值和bean属性之间的类型不匹配 (code "typeMismatch").
	 * <p>Multipart文件通过其参数名称绑定, 就像普通的HTTP参数一样:
	 * i.e. "uploadedFile"到"uploadedFile" bean属性, 调用"setUploadedFile" setter方法.
	 * <p>multipart文件的目标属性的类型可以是 MultipartFile, byte[], or String.
	 * 后两者接收上传文件的内容; 在这些情况下, 所有元数据(如原始文件名, 内容类型等)都将丢失.
	 * 
	 * @param request 要绑定参数的请求 (可以是 multipart)
	 */
	public void bind(PortletRequest request) {
		MutablePropertyValues mpvs = new PortletRequestParameterPropertyValues(request);
		MultipartRequest multipartRequest = PortletUtils.getNativeRequest(request, MultipartRequest.class);
		if (multipartRequest != null) {
			bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
		}
		doBind(mpvs);
	}

	/**
	 * 将错误视为致命错误.
	 * <p>仅当输入无效时才使用此方法.
	 * 例如, 如果所有输入都来自下拉列表, 则这可能是合适的.
	 * 
	 * @throws PortletRequestBindingException 任何绑定问题的PortletException的子类
	 */
	public void closeNoCatch() throws PortletRequestBindingException {
		if (getBindingResult().hasErrors()) {
			throw new PortletRequestBindingException(
					"Errors binding onto object '" + getBindingResult().getObjectName() + "'",
					new BindException(getBindingResult()));
		}
	}
}
