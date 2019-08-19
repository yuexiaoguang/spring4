package org.springframework.web.servlet.view;

import org.springframework.util.ClassUtils;

/**
 * {@link UrlBasedViewResolver}的便捷子类, 支持{@link InternalResourceView} (i.e. Servlet和 JSP)和子类, 如{@link JstlView}.
 *
 * <p>可以通过{@link #setViewClass}指定此解析器生成的所有视图的视图类.
 * 有关详细信息, 请参阅{@link UrlBasedViewResolver}的javadoc.
 * 如果存在JSTL API, 则默认为{@link InternalResourceView}或{@link JstlView}.
 *
 * <p>顺便说一下, 最好将JSP文件放在WEB-INF下作为视图, 以隐藏它们不允许直接访问 (e.g. 通过手动输入的URL).
 * 只有控制器才能访问它们.
 *
 * <p><b>Note:</b> 链接ViewResolvers时, InternalResourceViewResolver总是需要在最后,
 * 因为它将尝试解析任何视图名称, 无论底层资源是否确实存在.
 */
public class InternalResourceViewResolver extends UrlBasedViewResolver {

	private static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.class.getClassLoader());

	private Boolean alwaysInclude;


	/**
	 * 将默认的{@link #setViewClass 视图类}设置为{@link #requiredViewClass}:
	 * 默认为{@link InternalResourceView}, 如果存在JSTL API, 则为{@link JstlView}.
	 */
	public InternalResourceViewResolver() {
		Class<?> viewClass = requiredViewClass();
		if (InternalResourceView.class == viewClass && jstlPresent) {
			viewClass = JstlView.class;
		}
		setViewClass(viewClass);
	}

	/**
	 * 一个便利构造函数, 允许指定{@link #setPrefix prefix}和{@link #setSuffix suffix}作为构造函数参数.
	 * 
	 * @param prefix 构建URL时视图名称的前缀
	 * @param suffix 构建URL时视图名称的后缀
	 */
	public InternalResourceViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 此解析器需要{@link InternalResourceView}.
	 */
	@Override
	protected Class<?> requiredViewClass() {
		return InternalResourceView.class;
	}

	/**
	 * 指定是始终包含视图还是转发.
	 * <p>默认为 "false". 切换此标志以强制使用Servlet包含, 即使可以转发.
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		InternalResourceView view = (InternalResourceView) super.buildView(viewName);
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		view.setPreventDispatchLoop(true);
		return view;
	}
}
