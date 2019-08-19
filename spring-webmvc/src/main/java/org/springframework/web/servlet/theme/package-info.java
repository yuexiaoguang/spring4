/**
 * Spring的web MVC框架的主题支持类.
 * 提供标准的ThemeResolver实现, 以及用于主题更改的HandlerInterceptor.
 *
 * <p>
 * <ul>
 * <li>如果没有提供其中一个类的bean作为{@code themeResolver}, 那么{@code FixedThemeResolver}将提供默认主题名称 'theme'.</li>
 * <li>如果使用已定义的{@code FixedThemeResolver}, 可以使用另一个主题名称作为默认值, 但用户将始终使用此主题.</li>
 * <li>使用{@code CookieThemeResolver}或{@code SessionThemeResolver}, 可以允许用户更改其当前主题.</li>
 * <li>通常, 将在主题资源包中放入CSS文件, 图像和HTML结构的路径.</li>
 * <li>要检索主题数据, 可以在JSP中使用 spring:theme 标记, 也可以通过{@code RequestContext}访问其他视图技术.</li>
 * <li>{@code pagedlist}演示应用程序使用主题</li>
 * </ul>
 */
package org.springframework.web.servlet.theme;
