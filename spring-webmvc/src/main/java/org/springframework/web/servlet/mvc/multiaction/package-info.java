/**
 * 允许MVC Controller实现处理<i>方法</i>而不是<i>类</i>级别的请求的包.
 * 当想要避免使用许多简单的控制器类时, 这非常有用, 因为在使用MVC框架时很容易发生这种情况.
 *
 * <p>通常, 处理多个请求类型的控制器将扩展MultiActionController,
 * 并实现多个将由反射调用的请求处理方法, 如果它们遵循此类的命名约定.
 * 在启动时分析类并缓存方法, 因此这种方法中反射的性能开销可以忽略不计.
 */
package org.springframework.web.servlet.mvc.multiaction;
