package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Controller;

/**
 * 使用{@link Controller @Controller}和{@link ResponseBody @ResponseBody}作为元注解.
 * <p>
 * 带有此注解的类型被视为控制器, 其中{@link RequestMapping @RequestMapping}方法默认采用{@link ResponseBody @ResponseBody}语义.
 *
 * <p><b>NOTE:</b> 如果配置了适当的{@code HandlerMapping}-{@code HandlerAdapter}对,
 * 例如{@code RequestMappingHandlerMapping}-{@code RequestMappingHandlerAdapter}对,
 * 它们是MVC Java配置和MVC命名空间中的默认值, 则会处理{@code @RestController}.
 * 特别是{@code @RestController}不支持弃用的
 * {@code DefaultAnnotationHandlerMapping}-{@code AnnotationMethodHandlerAdapter}对.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {

	/**
	 * 该值可以指示对逻辑组件名称的建议, 在自动检测组件时将其转换为Spring bean.
	 * 
	 * @return 建议的组件名称 (或空字符串)
	 */
	String value() default "";

}
