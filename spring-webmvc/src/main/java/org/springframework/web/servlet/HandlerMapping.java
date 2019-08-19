package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * 由定义请求和处理器对象之间的映射的对象实现的接口.
 *
 * <p>这个类可以由应用程序开发人员实现, 虽然这不是必需的,
 * 因为{@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * 和{@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}包含在框架中.
 * 如果在应用程序上下文中未注册HandlerMapping bean, 则前者是默认值.
 *
 * <p>HandlerMapping实现可以支持映射的拦截器, 但不必如此.
 * 处理器将始终包含在{@link HandlerExecutionChain}实例中, 可选地附带一些{@link HandlerInterceptor}实例.
 * DispatcherServlet将首先按给定的顺序调用每个HandlerInterceptor的{@code preHandle}方法,
 * 如果所有{@code preHandle}方法都返回{@code true}, 最后调用处理器本身.
 *
 * <p>参数化此映射的能力是此MVC框架的强大且不寻常的功能.
 * 例如, 可以基于会话状态, cookie状态或许多其他变量编写自定义映射. 没有其他MVC框架这样同样灵活.
 *
 * <p>Note: 实现可以实现{@link org.springframework.core.Ordered}接口, 以便能够指定排序顺序,
 * 从而获得DispatcherServlet应用的优先级. 非有序实例被视为最低优先级.
 */
public interface HandlerMapping {

	/**
	 * {@link HttpServletRequest}属性的名称, 该属性包含处理器映射中的路径,
	 * 如果是模式匹配, 或完整相关的URI (通常在DispatcherServlet的映射中).
	 * <p>Note: 所有HandlerMapping实现都不需要此属性.
	 * 基于URL的HandlerMappings通常会支持它, 但处理器不一定要求在所有场景中都存在此请求属性.
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * {@link HttpServletRequest}属性的名称, 该属性包含处理器映射中的最佳匹配模式.
	 * <p>Note: 所有HandlerMapping实现都不需要此属性.
	 * 基于URL的HandlerMappings通常会支持它, 但处理器不一定要求在所有场景中都存在此请求属性.
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * boolean {@link HttpServletRequest}属性的名称, 该属性指示是否应检查类型级别映射.
	 * <p>Note: 所有HandlerMapping实现都不需要此属性.
	 */
	String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";

	/**
	 * {@link HttpServletRequest}属性的名称, 该属性包含URI模板映射, 将变量名称映射到值.
	 * <p>Note: 所有HandlerMapping实现都不需要此属性.
	 * 基于URL的HandlerMappings通常会支持它, 但处理器不一定要求在所有场景中都存在此请求属性.
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * {@link HttpServletRequest}属性的名称, 该属性包含带有URI矩阵变量的映射.
	 * <p>Note: 所有HandlerMapping实现都不需要此属性, 也可能不存在此属性,
	 * 具体取决于HandlerMapping是否配置为将矩阵变量内容保留在请求URI中.
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * {@link HttpServletRequest}属性的名称, 该属性包含适用于映射的处理器的可生成的MediaType集.
	 * <p>Note: 所有HandlerMapping实现都不需要此属性.
	 * 处理器不一定要求在所有场景中都存在此请求属性.
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";

	/**
	 * 返回此请求的处理器和任何拦截器.
	 * 可以根据请求URL, 会话状态, 或实现类选择的任何因素进行选择.
	 * <p>返回的HandlerExecutionChain包含一个处理器Object, 而不是一个标记接口, 因此处理器不会受到任何限制.
	 * 例如, 可以编写HandlerAdapter以允许使用另一个框架的处理器对象.
	 * <p>如果未找到匹配项, 则返回{@code null}. 这不是错误.
	 * DispatcherServlet将查询所有已注册的HandlerMapping bean以查找匹配项, 并且只有在没有找到处理器时才会确定存在错误.
	 * 
	 * @param request 当前的HTTP请求
	 * 
	 * @return 包含处理器对象和任何拦截器的HandlerExecutionChain实例, 或{@code null} 如果未找到映射
	 * @throws Exception 如果有内部错误
	 */
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
