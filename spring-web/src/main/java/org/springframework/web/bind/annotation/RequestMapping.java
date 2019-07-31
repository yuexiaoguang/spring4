package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * 用于将Web请求映射到特定处理器类和/或处理器方法.
 * 在Servlet和Portlet环境之间提供一致的样式, 使用适应具体环境的语义.
 *
 * <p><b>NOTE:</b> Servlet支持的功能是Portlet支持的功能的超集.
 * 适用的位置在此源文件中标有"Servlet-only"标签.
 * 对于Servlet环境, 还有一些区别, 具体取决于应用程序是否配置了{@literal "@MVC 3.0"}或{@literal "@MVC 3.1"}支持类.
 * 适用的地方在此源文件中标有{@literal "@MVC 3.1-only"}.
 * 有关更多详细信息, 请参阅下面的Spring MVC 3.1中添加的新支持类的说明.
 *
 * <p>使用此注解的处理器方法允许具有非常灵活的签名.
 * 它们可以以任意顺序具有以下类型的参数 (验证结果除外, 如果需要, 需要在相应的命令对象之后立即执行):
 * <ul>
 * <li>请求和/或响应对象 (Servlet API或Portlet API).
 * 可以选择任何特定的请求/响应类型, e.g.
 * {@link javax.servlet.ServletRequest} / {@link javax.servlet.http.HttpServletRequest}
 * 或{@link javax.portlet.PortletRequest} / {@link javax.portlet.ActionRequest} /
 * {@link javax.portlet.RenderRequest}.
 * 请注意, 在Portlet的情况下, 显式声明的action/render参数也用于将特定请求类型映射到处理器方法
 * (如果没有给出区分action和render请求的其他信息).
 * 
 * <li>Session对象(Servlet API 或 Portlet API):
 * {@link javax.servlet.http.HttpSession}或{@link javax.portlet.PortletSession}.
 * 此类型的参数将强制存在相应的会话. 因此, 这样的参数永远不会是{@code null}.
 * <i>请注意, 会话访问可能不是线程安全的, 特别是在Servlet环境中:
 * 如果允许多个请求同时访问会话, 将
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
 * "synchronizeOnSession"}标志切换为"true".</i>
 * 
 * <li>{@link org.springframework.web.context.request.WebRequest}
 * 或{@link org.springframework.web.context.request.NativeWebRequest}.
 * 允许通用请求参数访问以及请求/会话属性访问, 而不与本机Servlet/Portlet API绑定.
 * 
 * <li>用于当前请求区域设置的{@link java.util.Locale} (由可用的最具体的区域设置解析器确定,
 * i.e. Servlet环境中配置的{@link org.springframework.web.servlet.LocaleResolver}和Portlet环境中的portal区域设置).
 * 
 * <li>用于访问请求的内容的{@link java.io.InputStream} / {@link java.io.Reader}.
 * 这将是Servlet/Portlet API公开的原始InputStream/Reader.
 * 
 * <li>用于生成响应的内容的{@link java.io.OutputStream} / {@link java.io.Writer}.
 * 这将是Servlet/Portlet API公开的原始OutputStream/Writer.
 * 
 * <li>用于HTTP请求方法的{@link org.springframework.http.HttpMethod}</li>
 * 
 * <li>带{@link PathVariable @PathVariable}注解的参数 (仅限Servlet), 用于访问URI模板值 (i.e. /hotels/{hotel}).
 * 变量值将转换为声明的方法参数类型.
 * 默认情况下, URI模板将匹配正则表达式 {@code [^\.]*} (i.e. 点以外的任何字符),
 * 但可以通过指定另一个正则表达式来更改, 如下所示: /hotels/{hotel:\d+}.
 * 此外, 可以在{@link java.util.Map Map&lt;String, String&gt;}上使用{@code @PathVariable}来访问所有URI模板变量.
 * 
 * <li>带{@link MatrixVariable @MatrixVariable}注解的参数 (仅限Servlet), 用于访问位于URI路径段中的name-value对.
 * 矩阵变量必须用URI模板变量表示. 例如 /hotels/{hotel}, 其中传入的URL可能是"/hotels/42;q=1".
 * 此外, {@code @MatrixVariable}可用于{@link java.util.Map Map&lt;String, String&gt;},
 * 以访问URL中的所有矩阵变量, 或特定路径变量中的所有矩阵变量.
 * 
 * <li>带{@link RequestParam @RequestParam}注解的参数, 用于访问特定Servlet/Portlet请求参数.
 * 参数值将转换为声明的方法参数类型.
 * 此外, 可以在{@link java.util.Map Map&lt;String, String&gt;}
 * 或{@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * 方法参数上使用{@code @RequestParam}来访问所有请求参数.
 * 
 * <li>带{@link RequestHeader @RequestHeader}注解的参数, 用于访问特定Servlet/Portlet请求HTTP header.
 * 参数值将转换为声明的方法参数类型.
 * 此外, 可以在{@link java.util.Map Map&lt;String, String&gt;},
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;},
 * 或 {@link org.springframework.http.HttpHeaders HttpHeaders}方法参数上使用{@code @RequestHeader}来访问所有请求header.
 * 
 * <li>带{@link RequestBody @RequestBody}注解的参数 (仅限Servlet), 用于访问Servlet请求HTTP内容.
 * 请求流将使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}转换为声明的方法参数类型.
 * 这些参数可以选择使用{@code @Valid}注解, 并且还支持通过{@link org.springframework.validation.Errors}参数访问验证结果.
 * 而不是抛出{@link org.springframework.web.bind.MethodArgumentNotValidException}异常.
 * 
 * <li>带{@link RequestPart @RequestPart}注解的参数 (Servlet-only, {@literal @MVC 3.1-only}),
 * 用于访问"multipart/form-data"请求的一部分内容.
 * 请求部分流将使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}转换为声明的方法参数类型.
 * 这些参数可以选择使用{@code @Valid}注解, 并且还支持通过{@link org.springframework.validation.Errors}参数访问验证结果.
 * 而不是抛出{@link org.springframework.web.bind.MethodArgumentNotValidException}异常.
 * 
 * <li>带{@link SessionAttribute @SessionAttribute}注解的参数, 用于访问现有的永久会话属性 (e.g. 用户身份验证对象),
 * 而不是通过{@link SessionAttributes}临时存储在会话中作为Controller工作流的一部分的模型属性.
 * 
 * <li>带{@link RequestAttribute @RequestAttribute}注解的参数, 用于访问请求属性.
 * 
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;}参数 (Servlet-only),
 * 用于访问Servlet请求HTTP header和内容.
 * 请求流将使用
 * {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}转换为实体主体.
 * 
 * <li>{@link java.util.Map} / {@link org.springframework.ui.Model} /
 * {@link org.springframework.ui.ModelMap}, 用于丰富将公开给Web视图的隐式模型.
 * 
 * <li>{@link org.springframework.web.servlet.mvc.support.RedirectAttributes} (Servlet-only, {@literal @MVC 3.1-only})
 * 用于指定在重定向的情况下要使用的确切属性集合, 还要添加flash属性 (临时存储在服务器端的属性, 以使其在重定向后可用于请求).
 * 如果方法返回带"redirect:"前缀的视图名称或{@code RedirectView}, 则使用{@code RedirectAttributes}代替隐式模型.
 * 
 * <li>命令/表单对象将参数绑定到: 作为bean属性或字段, 具有可自定义的类型转换,
 * 具体取决于{@link InitBinder}方法和/或HandlerAdapter配置
 *  - 请参阅RequestMappingHandlerMethodAdapter上的"webBindingInitializer"属性.
 * 此类命令对象及其验证结果将作为模型属性公开, 默认情况下使用属性表示法中的非限定命令类名称
 * (e.g. "orderAddress"表示类型"mypackage.OrderAddress").
 * 指定参数级{@link ModelAttribute @ModelAttribute}注解, 以声明特定的模型属性名称.
 * 
 * <li>{@link org.springframework.validation.Errors} /
 * {@link org.springframework.validation.BindingResult}表示前面的命令/表单对象的验证结果.
 * 
 * <li>{@link org.springframework.web.bind.support.SessionStatus}状态句柄, 用于将表单处理标记为完成
 * (触发会话属性的清理, 该属性在处理器类型级别由{@link SessionAttributes @SessionAttributes}注解指示).
 * 
 * <li>{@link org.springframework.web.util.UriComponentsBuilder} (Servlet-only, {@literal @MVC 3.1-only})
 * 用于准备相对于当前请求的主机, 端口, scheme, 上下文路径和servlet映射的文字部分的URL.
 * </ul>
 *
 * <p><strong>Note:</strong> 支持Java 8的{@code java.util.Optional}作为方法参数类型,
 * 其注解提供{@code required}属性 (e.g. {@code @RequestParam}, {@code @RequestHeader}, etc.).
 * 在这些情况下使用{@code java.util.Optional}等同于{@code required=false}.
 *
 * <p>处理器方法支持以下返回类型:
 * <ul>
 * <li>{@code ModelAndView}对象 (Servlet MVC 或 Portlet MVC),
 * 模型隐式地丰富了命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法的结果.
 * 
 * <li>{@link org.springframework.ui.Model Model}对象,
 * 通过{@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定视图名称,
 * 并且模型隐式地丰富了命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法的结果.
 * 
 * <li>用于公开模型的{@link java.util.Map}对象,
 * 通过{@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定视图名称,
 * 并且模型隐式地丰富了命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法的结果.
 * 
 * <li>{@link org.springframework.web.servlet.View}对象,
 * 通过命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法隐式地确定模型.
 * 处理器方法还可以通过声明{@link org.springframework.ui.Model}参数以编程方式丰富模型 (see above).
 * 
 * <li>{@link String}值, 将被解释为视图名称,
 * 通过命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法隐式地确定模型.
 * 处理器方法还可以通过声明{@link org.springframework.ui.ModelMap}参数以编程方式丰富模型 (see above).
 * 
 * <li>带{@link ResponseBody @ResponseBody}注解的方法 (Servlet-only), 用于访问Servlet响应HTTP内容.
 * 使用{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}将返回值转换为响应流.
 * 
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;}
 * 或{@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;}对象 (Servlet-only),
 * 访问Servlet响应HTTP header和内容.
 * 使用{@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}将实体主体转换为响应流.
 * 
 * <li>{@link org.springframework.http.HttpHeaders HttpHeaders}对象, 返回没有正文的响应.</li>
 * 
 * <li>{@link Callable}, 由Spring MVC用于在由Spring MVC代表应用程序透明管理的单独线程中异步获取返回值.
 * 
 * <li>{@link org.springframework.web.context.request.async.DeferredResult},
 * 应用程序用于在自己选择的单独线程中生成返回值, 作为返回Callable的替代方法.
 * 
 * <li>{@link org.springframework.util.concurrent.ListenableFuture},
 * 应用程序用于在自己选择的单独线程中生成返回值, 作为返回Callable的替代方法.
 * 
 * <li>{@link java.util.concurrent.CompletionStage} (由{@link java.util.concurrent.CompletableFuture}实现),
 * 应用程序使用它在自己选择的单独线程中生成返回值, 作为返回Callable的替代方法.
 * 
 * <li>{@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter},
 * 用于异步写入多个对象到响应; 也支持{@code ResponseEntity}中的正文.</li>
 * 
 * <li>{@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter},
 * 用于异步写入响应的Server-Sent Event; 也支持{@code ResponseEntity}中的正文.</li>
 * 
 * <li>{@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody},
 * 用于异步写入响应; 也支持{@code ResponseEntity}中的正文.</li>
 * 
 * <li>{@code void}, 如果方法处理响应本身 (直接写入响应内容, 声明
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * / {@link javax.portlet.RenderResponse}类型的参数)
 * 或者如果应该通过
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}隐式确定视图名称
 * (不在处理器方法签名中声明响应参数; 仅适用于Servlet环境).
 * 
 * <li>任何其他返回类型将被视为要暴露给视图的单个模型属性,
 * 使用在方法级别通过{@link ModelAttribute @ModelAttribute}指定的属性名称 (或者基于返回类型的类名称的默认属性名称).
 * 将使用命令对象和带{@link ModelAttribute @ModelAttribute}注解的引用数据访问器方法的结果隐式地丰富该模型.
 * </ul>
 *
 * <p><b>NOTE:</b>只有在配置了适当的{@code HandlerMapping}-{@code HandlerAdapter}对之后,
 * 才会处理{@code @RequestMapping}.
 * 默认情况下, {@code DispatcherServlet}和{@code DispatcherPortlet}都是这种情况.
 * 但是, 如果要定义自定义{@code HandlerMappings}或{@code HandlerAdapters},
 * 则需要将{@code DefaultAnnotationHandlerMapping}和{@code AnnotationMethodHandlerAdapter}添加到配置中.</code>.
 *
 * <p><b>NOTE:</b> Spring 3.1为Servlet环境中的{@code @RequestMapping}方法引入了一组新的支持类,
 * 名为{@code RequestMappingHandlerMapping}和{@code RequestMappingHandlerAdapter}.
 * 建议使用它们, 甚至需要利用Spring MVC 3.1中的新功能 (在此源文件中搜索{@literal "@MVC 3.1-only"}).
 * 默认从MVC命名空间和使用MVC Java配置 ({@code @EnableWebMvc})启用新的支持类, 但如果两者都不使用, 则必须明确配置.
 *
 * <p><b>NOTE:</b> 使用控制器接口时 (e.g. 用于AOP代理), 请确保在控制器上始终放入<i>所有</i>映射注解
 *  - 例如{@code @RequestMapping}和{@code @SessionAttributes} - 在控制器<i>接口</i>上, 而不是在实现类上.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * 为此映射指定名称.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在两个级别上使用时, 组合名称通过使用"#"作为分隔符的串联派生.
	 */
	String name() default "";

	/**
	 * 此注解表示的主要映射.
	 * <p>在Servlet环境中, 这是{@link #path}的别名.
	 * 例如, {@code @RequestMapping("/foo")}等同于{@code @RequestMapping(path="/foo")}.
	 * <p>在Portlet环境中, 这是映射的portlet模式(i.e. "EDIT", "VIEW", "HELP"或自定义模式).
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别映射都会继承此主映射, 从而缩小特定处理器方法的范围.
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * 仅在Servlet环境中: 路径映射URI (e.g. "/myPath.do").
	 * 还支持Ant样式的路径模式 (e.g. "/myPath/*.do").
	 * 在方法级别, 在类型级别表示的主映射中支持相对路径 (e.g. "edit.do").
	 * 路径映射URI可能包含占位符 (e.g. "/${connect}").
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别映射都会继承此主映射, 从而缩小特定处理器方法的范围.
	 */
	@AliasFor("value")
	String[] path() default {};

	/**
	 * 要映射到的HTTP请求方法, 缩小主映射:
	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别的映射都会继承此HTTP方法限制 (i.e. 甚至在解析处理器方法之前检查类型级别限制).
	 * <p>支持Servlet环境以及Portlet 2.0环境.
	 */
	RequestMethod[] method() default {};

	/**
	 * 映射请求的参数, 缩小主映射.
	 * <p>任何环境的格式相同: 一系列"myParam=myValue"样式表达式, 只有在发现每个此类参数具有给定值时才会映射请求.
	 * 使用"!="运算符表示否定表达式, 如 "myParam!=myValue".
	 * 还支持"myParam"样式表达式, 这些参数必须存在于请求中 (允许具有任何值).
	 * 最后, "!myParam"样式表达式表明指定的参数<i>不</i>应该出现在请求中.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别映射都继承此参数限制 (i.e. 甚至在解析处理器方法之前检查类型级别限制).
	 * <p>在Servlet环境中, 参数映射在类型级别被视为强制的限制.
	 * 主路径映射 (i.e. 指定的URI值) 仍然必须唯一地标识目标处理器, 参数映射仅表示调用处理器的前提条件.
	 * <p>在Portlet环境中,参数作为映射区分器, i.e. 主要Portlet模式映射加上参数条件唯一标识目标处理器.
	 * 可以将不同的处理器映射到相同的portlet模式, 只要它们的参数映射不同即可.
	 */
	String[] params() default {};

	/**
	 * 映射的请求的header, 缩小主映射.
	 * <p>任何环境的格式相同: 一系列"My-Header=myValue"样式表达式, 只有在发现每个此类header具有给定值时才会映射请求.
	 * 使用"!="运算符表示否定表达式, 如 "My-Header!=myValue".
	 * 还支持"My-Header"样式表达式, 请求中必须有这些 header (允许具有任何值).
	 * 最后, "!My-Header"样式表达式表明指定的header <i>不</i>应该出现在请求中.
	 * <p>还支持媒体类型通配符 (*), 用于诸如Accept 和 Content-Type之类的header.
	 * 例如,
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * 将匹配Content-Type为"text/html", "text/plain"等的请求.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别映射都继承此header限制 (i.e. 在解析处理器方法之前检查类型级别限制).
	 * <p>针对Servlet环境中的HttpServletRequest header映射, 以及针对Portlet 2.0环境中的PortletRequest属性.
	 */
	String[] headers() default {};

	/**
	 * 映射请求的可消耗媒体类型, 缩小主映射.
	 * <p>格式是单个媒体类型或媒体类型序列, 仅当{@code Content-Type}与其中一种媒体类型匹配时才会映射请求.
	 * Examples:
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * </pre>
	 * 使用"!"运算符表示否定表达式, 如"!text/plain", 它匹配除"text/plain"之外的{@code Content-Type}的所有请求.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别映射都会覆盖此消耗限制.
	 */
	String[] consumes() default {};

	/**
	 * 映射请求的可生成媒体类型, 缩小主映射.
	 * <p>格式是单个媒体类型或媒体类型序列, 仅当{@code Accept}与其中一种媒体类型匹配时才会映射请求.
	 * Examples:
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = "application/json; charset=UTF-8"
	 * </pre>
	 * <p>它会影响写入的实际内容类型, 例如, 使用UTF-8编码生成JSON响应,
	 * 应使用{@code "application/json; charset=UTF-8"}.
	 * <p>使用"!"运算符表示否定表达式, 如"!text/plain", 它匹配除"text/plain"之外的{@code Accept}的所有请求.
	 * <p><b>支持类型级别和方法级别!</b>
	 * 在类型级别使用时, 所有方法级别的映射都会覆盖此限制.
	 */
	String[] produces() default {};

}
