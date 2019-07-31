package org.springframework.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 一个通用的复合servlet {@link Filter}, 它只将其行为委托给用户提供的过滤器的链 (list),
 * 实现{@link FilterChain}的功能, 但只使用{@link Filter}实例.
 *
 * <p>这对需要依赖注入的过滤器很有用, 因此可以在Spring应用程序上下文中进行设置.
 * 通常, 此组合将与{@link DelegatingFilterProxy}一起使用, 以便它可以在Spring中声明, 但应用于servlet上下文.
 */
public class CompositeFilter implements Filter {

	private List<? extends Filter> filters = new ArrayList<Filter>();


	public void setFilters(List<? extends Filter> filters) {
		this.filters = new ArrayList<Filter>(filters);
	}


	/**
	 * 初始化所有过滤器, 按提供的顺序依次调用每个过滤器的init方法.
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		for (Filter filter : this.filters) {
			filter.init(config);
		}
	}

	/**
	 * 从提供的委托过滤器列表 ({@link #setFilters})中形成一个临时链, 并按顺序执行它们.
	 * 每个过滤器都委托给列表中的下一个过滤器, 实现{@link FilterChain}的正常行为, 尽管这是{@link Filter}.
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		new VirtualFilterChain(chain, this.filters).doFilter(request, response);
	}

	/**
	 * 清理所有提供的过滤器, 依次调用每个过滤器的销毁方法, 但顺序相反.
	 */
	@Override
	public void destroy() {
		for (int i = this.filters.size(); i-- > 0;) {
			Filter filter = this.filters.get(i);
			filter.destroy();
		}
	}


	private static class VirtualFilterChain implements FilterChain {

		private final FilterChain originalChain;

		private final List<? extends Filter> additionalFilters;

		private int currentPosition = 0;

		public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
			this.originalChain = chain;
			this.additionalFilters = additionalFilters;
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response)
				throws IOException, ServletException {

			if (this.currentPosition == this.additionalFilters.size()) {
				this.originalChain.doFilter(request, response);
			}
			else {
				this.currentPosition++;
				Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
				nextFilter.doFilter(request, response, this);
			}
		}
	}

}
