package org.springframework.test.web.servlet.result;

import java.lang.reflect.Method;

import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodInvocationInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

/**
 * 在选定的处理器或处理器方法上进行断言的工厂.
 *
 * <p>通常通过{@link MockMvcResultMatchers#handler}访问此类的实例.
 *
 * <p><strong>Note:</strong> 断言用于处理请求的控制器方法的期望,
 * 仅适用于使用{@link RequestMappingHandlerMapping}和{@link RequestMappingHandlerAdapter}处理的请求,
 * 默认情况下使用Spring MVC Java配置和XML命名空间.
 */
public class HandlerResultMatchers {

	/**
	 * Use {@link MockMvcResultMatchers#handler()}.
	 */
	protected HandlerResultMatchers() {
	}


	/**
	 * 断言处理请求的处理器的类型.
	 */
	public ResultMatcher handlerType(final Class<?> type) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				Object handler = result.getHandler();
				assertTrue("No handler", handler != null);
				Class<?> actual = handler.getClass();
				if (HandlerMethod.class.isInstance(handler)) {
					actual = ((HandlerMethod) handler).getBeanType();
				}
				assertEquals("Handler type", type, ClassUtils.getUserClass(actual));
			}
		};
	}

	/**
	 * 断言用于处理请求的控制器方法.
	 * <p>期望的方法是通过"模拟"控制器方法调用指定的, 类似于{@link MvcUriComponentsBuilder#fromMethodCall(Object)}.
	 * <p>例如, 给定此控制器:
	 * <pre class="code">
	 * &#064;RestController
	 * public class SimpleController {
	 *
	 *     &#064;RequestMapping("/")
	 *     public ResponseEntity<Void> handle() {
	 *         return ResponseEntity.ok().build();
	 *     }
	 * }
	 * </pre>
	 * <p>静态导入{@link MvcUriComponentsBuilder#on}的测试可以按如下方式执行:
	 * <pre class="code">
	 * mockMvc.perform(get("/"))
	 *     .andExpect(handler().methodCall(on(SimpleController.class).handle()));
	 * </pre>
	 * 
	 * @param obj 调用之后从"模拟"控制器调用返回的值或"模拟"控制器本身返回的值
	 */
	public ResultMatcher methodCall(final Object obj) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				if (!(obj instanceof MethodInvocationInfo)) {
					fail(String.format("The supplied object [%s] is not an instance of %s. " +
							"Ensure that you invoke the handler method via MvcUriComponentsBuilder.on().",
							obj, MethodInvocationInfo.class.getName()));
				}
				MethodInvocationInfo invocationInfo = (MethodInvocationInfo) obj;
				Method expected = invocationInfo.getControllerMethod();
				Method actual = getHandlerMethod(result).getMethod();
				assertEquals("Handler method", expected, actual);
			}
		};
	}

	/**
	 * 使用给定的Hamcrest {@link Matcher}断言用于处理请求的控制器方法的名称.
	 */
	public ResultMatcher methodName(final Matcher<? super String> matcher) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertThat("Handler method", handlerMethod.getMethod().getName(), matcher);
			}
		};
	}

	/**
	 * 断言用于处理请求的控制器方法的名称.
	 */
	public ResultMatcher methodName(final String name) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertEquals("Handler method", name, handlerMethod.getMethod().getName());
			}
		};
	}

	/**
	 * 断言用于处理请求的控制器方法.
	 */
	public ResultMatcher method(final Method method) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) throws Exception {
				HandlerMethod handlerMethod = getHandlerMethod(result);
				assertEquals("Handler method", method, handlerMethod.getMethod());
			}
		};
	}


	private static HandlerMethod getHandlerMethod(MvcResult result) {
		Object handler = result.getHandler();
		assertTrue("No handler: ", handler != null);
		assertTrue("Not a HandlerMethod: " + handler, HandlerMethod.class.isInstance(handler));
		return (HandlerMethod) handler;
	}

}
