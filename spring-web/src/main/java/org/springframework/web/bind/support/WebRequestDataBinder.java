package org.springframework.web.bind.support;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartRequest;

/**
 * 特殊{@link org.springframework.validation.DataBinder},
 * 用于执行从Web请求参数到JavaBeans的数据绑定, 包括对multipart文件的支持.
 *
 * <p>有关自定义选项, 请参阅 DataBinder/WebDataBinder超类, 其中包括指定允许/必需字段, 以及注册自定义属性编辑器.
 *
 * <p>也可以用于基于Spring的{@link org.springframework.web.context.request.WebRequest}抽象
 * 构建的自定义Web控制器或拦截器中的手动数据绑定:
 * e.g. 在{@link org.springframework.web.context.request.WebRequestInterceptor}实现中.
 * 只需为每个绑定过程实例化一个WebRequestDataBinder, 并以当前的WebRequest作为参数调用{@code bind}:
 *
 * <pre class="code">
 * MyBean myBean = new MyBean();
 * // apply binder to custom target object
 * WebRequestDataBinder binder = new WebRequestDataBinder(myBean);
 * // register custom editors, if desired
 * binder.registerCustomEditor(...);
 * // trigger actual binding of request parameters
 * binder.bind(request);
 * // optionally evaluate binding errors
 * Errors errors = binder.getErrors();
 * ...</pre>
 */
public class WebRequestDataBinder extends WebDataBinder {

	private static final boolean servlet3Parts = ClassUtils.hasMethod(HttpServletRequest.class, "getParts");


	/**
	 * @param target 绑定到的目标对象 (或{@code null}, 如果绑定器仅用于转换普通参数值)
	 */
	public WebRequestDataBinder(Object target) {
		super(target);
	}

	/**
	 * @param target 绑定到的目标对象 (或{@code null}, 如果绑定器仅用于转换普通参数值)
	 * @param objectName 目标对象的名称
	 */
	public WebRequestDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将给定请求的参数绑定到此绑定器的目标, 并在multipart请求的情况下绑定multipart文件.
	 * <p>此调用可以创建字段错误, 表示基本绑定错误, 如必填字段 (code "required"),
	 * 或者值和bean属性之间的类型不匹配 (code "typeMismatch").
	 * <p>Multipart文件通过其参数名称绑定, 就像普通的HTTP参数一样:
	 * i.e. "uploadedFile"到"uploadedFile" bean属性, 调用"setUploadedFile" setter方法.
	 * <p>multipart文件的目标属性的类型可以是 Part, MultipartFile, byte[], 或String.
	 * 后两者接收上传文件的内容; 在这些情况下, 所有元数据(如原始文件名, 内容类型等)都将丢失.
	 * 
	 * @param request 要绑定参数的请求 (可以是 multipart)
	 */
	public void bind(WebRequest request) {
		MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());
		if (isMultipartRequest(request) && request instanceof NativeWebRequest) {
			MultipartRequest multipartRequest = ((NativeWebRequest) request).getNativeRequest(MultipartRequest.class);
			if (multipartRequest != null) {
				bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
			}
			else if (servlet3Parts) {
				HttpServletRequest serlvetRequest = ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class);
				new Servlet3MultipartHelper(isBindEmptyMultipartFiles()).bindParts(serlvetRequest, mpvs);
			}
		}
		doBind(mpvs);
	}

	/**
	 * 检查请求是否是 multipart请求 (通过检查其 Content-Type header).
	 * 
	 * @param request 要绑定参数的请求
	 */
	private boolean isMultipartRequest(WebRequest request) {
		String contentType = request.getHeader("Content-Type");
		return (contentType != null && StringUtils.startsWithIgnoreCase(contentType, "multipart"));
	}

	/**
	 * 将错误视为致命的.
	 * <p>仅当输入无效时才使用此方法.
	 * 例如, 如果所有输入都来自下拉列表, 则这可能是合适的.
	 * 
	 * @throws BindException 如果遇到绑定错误
	 */
	public void closeNoCatch() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
	}


	/**
	 * 封装仅适用于Servlet 3.0+容器的Part绑定代码.
	 */
	private static class Servlet3MultipartHelper {

		private final boolean bindEmptyMultipartFiles;

		public Servlet3MultipartHelper(boolean bindEmptyMultipartFiles) {
			this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
		}

		public void bindParts(HttpServletRequest request, MutablePropertyValues mpvs) {
			try {
				MultiValueMap<String, Part> map = new LinkedMultiValueMap<String, Part>();
				for (Part part : request.getParts()) {
					map.add(part.getName(), part);
				}
				for (Map.Entry<String, List<Part>> entry: map.entrySet()) {
					if (entry.getValue().size() == 1) {
						Part part = entry.getValue().get(0);
						if (this.bindEmptyMultipartFiles || part.getSize() > 0) {
							mpvs.add(entry.getKey(), part);
						}
					}
					else {
						mpvs.add(entry.getKey(), entry.getValue());
					}
				}
			}
			catch (Exception ex) {
				throw new MultipartException("Failed to get request parts", ex);
			}
		}
	}

}
