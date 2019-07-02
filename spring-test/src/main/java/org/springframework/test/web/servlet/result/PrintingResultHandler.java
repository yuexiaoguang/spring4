package org.springframework.test.web.servlet.result;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * 将{@link MvcResult}详细信息打印到给定的输出流的结果处理器 &mdash;
 * 例如: {@code System.out}, {@code System.err}, 自定义{@code java.io.PrintWriter}, etc.
 *
 * <p>通常通过{@link MockMvcResultHandlers}中的{@link MockMvcResultHandlers#print print}
 * 或{@link MockMvcResultHandlers#log log}方法访问此类的实例.
 */
public class PrintingResultHandler implements ResultHandler {

	private final ResultValuePrinter printer;


	/**
	 * @param printer 实际写入使用的{@link ResultValuePrinter}
	 */
	protected PrintingResultHandler(ResultValuePrinter printer) {
		this.printer = printer;
	}

	/**
	 * @return 结果值打印器
	 */
	protected ResultValuePrinter getPrinter() {
		return this.printer;
	}

	/**
	 * 打印{@link MvcResult}详细信息.
	 */
	@Override
	public final void handle(MvcResult result) throws Exception {
		this.printer.printHeading("MockHttpServletRequest");
		printRequest(result.getRequest());

		this.printer.printHeading("Handler");
		printHandler(result.getHandler(), result.getInterceptors());

		this.printer.printHeading("Async");
		printAsyncResult(result);

		this.printer.printHeading("Resolved Exception");
		printResolvedException(result.getResolvedException());

		this.printer.printHeading("ModelAndView");
		printModelAndView(result.getModelAndView());

		this.printer.printHeading("FlashMap");
		printFlashMap(RequestContextUtils.getOutputFlashMap(result.getRequest()));

		this.printer.printHeading("MockHttpServletResponse");
		printResponse(result.getResponse());
	}

	/**
	 * 打印请求.
	 */
	protected void printRequest(MockHttpServletRequest request) throws Exception {
		this.printer.printValue("HTTP Method", request.getMethod());
		this.printer.printValue("Request URI", request.getRequestURI());
		this.printer.printValue("Parameters", getParamsMultiValueMap(request));
		this.printer.printValue("Headers", getRequestHeaders(request));
	}

	protected final HttpHeaders getRequestHeaders(MockHttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<?> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			while (values.hasMoreElements()) {
				headers.add(name, values.nextElement());
			}
		}
		return headers;
	}

	protected final MultiValueMap<String, String> getParamsMultiValueMap(MockHttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();
		MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<String, String>();
		for (String name : params.keySet()) {
			if (params.get(name) != null) {
				for (String value : params.get(name)) {
					multiValueMap.add(name, value);
				}
			}
		}
		return multiValueMap;
	}

	protected void printAsyncResult(MvcResult result) throws Exception {
		HttpServletRequest request = result.getRequest();
		this.printer.printValue("Async started", request.isAsyncStarted());
		Object asyncResult = null;
		try {
			asyncResult = result.getAsyncResult(0);
		}
		catch (IllegalStateException ex) {
			// Not set
		}
		this.printer.printValue("Async result", asyncResult);
	}

	/**
	 * 打印处理器.
	 */
	protected void printHandler(Object handler, HandlerInterceptor[] interceptors) throws Exception {
		if (handler == null) {
			this.printer.printValue("Type", null);
		}
		else {
			if (handler instanceof HandlerMethod) {
				HandlerMethod handlerMethod = (HandlerMethod) handler;
				this.printer.printValue("Type", handlerMethod.getBeanType().getName());
				this.printer.printValue("Method", handlerMethod);
			}
			else {
				this.printer.printValue("Type", handler.getClass().getName());
			}
		}
	}

	/**
	 * 打印通过HandlerExceptionResolver解析的异常.
	 */
	protected void printResolvedException(Exception resolvedException) throws Exception {
		if (resolvedException == null) {
			this.printer.printValue("Type", null);
		}
		else {
			this.printer.printValue("Type", resolvedException.getClass().getName());
		}
	}

	/**
	 * 打印 ModelAndView.
	 */
	protected void printModelAndView(ModelAndView mav) throws Exception {
		this.printer.printValue("View name", (mav != null) ? mav.getViewName() : null);
		this.printer.printValue("View", (mav != null) ? mav.getView() : null);
		if (mav == null || mav.getModel().size() == 0) {
			this.printer.printValue("Model", null);
		}
		else {
			for (String name : mav.getModel().keySet()) {
				if (!name.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
					Object value = mav.getModel().get(name);
					this.printer.printValue("Attribute", name);
					this.printer.printValue("value", value);
					Errors errors = (Errors) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
					if (errors != null) {
						this.printer.printValue("errors", errors.getAllErrors());
					}
				}
			}
		}
	}

	/**
	 * 打印"output" flash属性.
	 */
	protected void printFlashMap(FlashMap flashMap) throws Exception {
		if (ObjectUtils.isEmpty(flashMap)) {
			this.printer.printValue("Attributes", null);
		}
		else {
			for (String name : flashMap.keySet()) {
				this.printer.printValue("Attribute", name);
				this.printer.printValue("value", flashMap.get(name));
			}
		}
	}

	/**
	 * 打印响应.
	 */
	protected void printResponse(MockHttpServletResponse response) throws Exception {
		this.printer.printValue("Status", response.getStatus());
		this.printer.printValue("Error message", response.getErrorMessage());
		this.printer.printValue("Headers", getResponseHeaders(response));
		this.printer.printValue("Content type", response.getContentType());
		this.printer.printValue("Body", response.getContentAsString());
		this.printer.printValue("Forwarded URL", response.getForwardedUrl());
		this.printer.printValue("Redirected URL", response.getRedirectedUrl());
		printCookies(response.getCookies());
	}

	/**
	 * 假设{@link Cookie}实现没有提供自己的{@code toString()}, 以人类可读的形式打印提供的cookie.
	 */
	private void printCookies(Cookie[] cookies) {
		String[] cookieStrings = new String[cookies.length];
		for (int i = 0; i < cookies.length; i++) {
			Cookie cookie = cookies[i];
			cookieStrings[i] = new ToStringCreator(cookie)
				.append("name", cookie.getName())
				.append("value", cookie.getValue())
				.append("comment", cookie.getComment())
				.append("domain", cookie.getDomain())
				.append("maxAge", cookie.getMaxAge())
				.append("path", cookie.getPath())
				.append("secure", cookie.getSecure())
				.append("version", cookie.getVersion())
				.append("httpOnly", cookie.isHttpOnly())
				.toString();
		}
		this.printer.printValue("Cookies", cookieStrings);
	}

	protected final HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
		HttpHeaders headers = new HttpHeaders();
		for (String name : response.getHeaderNames()) {
			headers.put(name, response.getHeaders(name));
		}
		return headers;
	}


	/**
	 * 如何实际写入结果信息的约定.
	 */
	protected interface ResultValuePrinter {

		void printHeading(String heading);

		void printValue(String label, Object value);
	}
}
