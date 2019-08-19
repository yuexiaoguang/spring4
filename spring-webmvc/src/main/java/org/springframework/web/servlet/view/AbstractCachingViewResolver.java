package org.springframework.web.servlet.view;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver}实现的便捷基类.
 * 缓存{@link org.springframework.web.servlet.View}对象一旦解析:
 * 这意味着无论初始视图检索的成本如何, 视图解析都不会成为性能问题.
 *
 * <p>子类需要实现{@link #loadView}模板方法, 为特定视图名称和语言环境构建View对象.
 */
public abstract class AbstractCachingViewResolver extends WebApplicationObjectSupport implements ViewResolver {

	/** 视图缓存的默认最大条目数: 1024 */
	public static final int DEFAULT_CACHE_LIMIT = 1024;

	/** 缓存Map中未解析视图的虚拟标记对象 */
	private static final View UNRESOLVED_VIEW = new View() {
		@Override
		public String getContentType() {
			return null;
		}
		@Override
		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
		}
	};


	/** 缓存中的最大条目数 */
	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	/** 一旦不能解析, 是否避免再次解析视图 */
	private boolean cacheUnresolved = true;

	/** View的快速访问缓存, 返回已缓存的实例而没有全局锁定 */
	private final Map<Object, View> viewAccessCache = new ConcurrentHashMap<Object, View>(DEFAULT_CACHE_LIMIT);

	/** 视图键映射到View实例, 同步View创建 */
	@SuppressWarnings("serial")
	private final Map<Object, View> viewCreationCache =
			new LinkedHashMap<Object, View>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Object, View> eldest) {
					if (size() > getCacheLimit()) {
						viewAccessCache.remove(eldest.getKey());
						return true;
					}
					else {
						return false;
					}
				}
			};


	/**
	 * 指定视图缓存的最大条目数.
	 * 默认为 1024.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回视图缓存的最大条目数.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}

	/**
	 * 启用或禁用缓存.
	 * <p>这相当于将{@link #setCacheLimit "cacheLimit"}属性分别设置为默认限制 (1024) 或 0.
	 * <p>默认为"true": 启用缓存.
	 * 禁用此功能仅用于调试和开发.
	 */
	public void setCache(boolean cache) {
		this.cacheLimit = (cache ? DEFAULT_CACHE_LIMIT : 0);
	}

	/**
	 * 返回是否启用了缓存.
	 */
	public boolean isCache() {
		return (this.cacheLimit > 0);
	}

	/**
	 * 视图名称一旦解析为{@code null}, 是否应该被缓存并随后自动解析为{@code null}.
	 * <p>默认为"true": 从Spring 3.1开始, 缓存未解析的视图名称.
	 * 请注意, 此标志仅适用于一般{@link #setCache "cache"}标志保持其默认值"true"的情况.
	 * <p>特别感兴趣的是一些AbstractUrlBasedView实现 (FreeMarker, Velocity, Tiles)
	 * 能够通过{@link AbstractUrlBasedView#checkResource(Locale)}检查底层资源是否存在.
	 * 将此标志设置为"false"时, 会注意并使用重新显示的底层资源. 将标志设置为"true"时, 仅进行一次检查.
	 */
	public void setCacheUnresolved(boolean cacheUnresolved) {
		this.cacheUnresolved = cacheUnresolved;
	}

	/**
	 * 返回是否启用了未解析视图的缓存.
	 */
	public boolean isCacheUnresolved() {
		return this.cacheUnresolved;
	}


	@Override
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		if (!isCache()) {
			return createView(viewName, locale);
		}
		else {
			Object cacheKey = getCacheKey(viewName, locale);
			View view = this.viewAccessCache.get(cacheKey);
			if (view == null) {
				synchronized (this.viewCreationCache) {
					view = this.viewCreationCache.get(cacheKey);
					if (view == null) {
						// 请求子类创建View对象.
						view = createView(viewName, locale);
						if (view == null && this.cacheUnresolved) {
							view = UNRESOLVED_VIEW;
						}
						if (view != null) {
							this.viewAccessCache.put(cacheKey, view);
							this.viewCreationCache.put(cacheKey, view);
							if (logger.isTraceEnabled()) {
								logger.trace("Cached view [" + cacheKey + "]");
							}
						}
					}
				}
			}
			return (view != UNRESOLVED_VIEW ? view : null);
		}
	}

	/**
	 * 返回给定视图名称和给定语言环境的缓存键.
	 * <p>默认值是由视图名称和区域设置后缀组成的字符串.
	 * 可以在子类中重写.
	 * <p>通常需要尊重语言环境, 因为不同的语言环境可能会导致不同的视图资源.
	 */
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName + '_' + locale;
	}

	/**
	 * 清除特定视图的缓存.
	 * <p>这可以很方便, 以防开发人员能够在运行时修改视图(e.g. Velocity模板), 之后需要清除指定视图的缓存.
	 * 
	 * @param viewName 需要删除的缓存视图对象的视图名称
	 * @param locale 应删除视图对象的语言环境
	 */
	public void removeFromCache(String viewName, Locale locale) {
		if (!isCache()) {
			logger.warn("View caching is SWITCHED OFF -- removal not necessary");
		}
		else {
			Object cacheKey = getCacheKey(viewName, locale);
			Object cachedView;
			synchronized (this.viewCreationCache) {
				this.viewAccessCache.remove(cacheKey);
				cachedView = this.viewCreationCache.remove(cacheKey);
			}
			if (logger.isDebugEnabled()) {
				// Some debug output might be useful...
				if (cachedView == null) {
					logger.debug("No cached instance for view '" + cacheKey + "' was found");
				}
				else {
					logger.debug("Cache for view " + cacheKey + " has been cleared");
				}
			}
		}
	}

	/**
	 * 清除整个视图缓存, 删除所有缓存的视图对象.
	 * 后续的解析调用将导致重新创建所需的视图对象.
	 */
	public void clearCache() {
		logger.debug("Clearing entire view cache");
		synchronized (this.viewCreationCache) {
			this.viewAccessCache.clear();
			this.viewCreationCache.clear();
		}
	}


	/**
	 * 创建实际的View对象.
	 * <p>默认实现委托给{@link #loadView}.
	 * 在委托给子类提供的实际{@code loadView}实现之前, 可以重写此方法以特殊方式解析某些视图名称.
	 * 
	 * @param viewName 要检索的视图的名称
	 * @param locale 用于检索视图的Locale
	 * 
	 * @return View实例, 或{@code null} (可选, 允许ViewResolver链接)
	 * @throws Exception 如果视图无法解析
	 */
	protected View createView(String viewName, Locale locale) throws Exception {
		return loadView(viewName, locale);
	}

	/**
	 * 子类必须实现此方法, 为指定视图构建View对象.
	 * 返回的View对象将由此ViewResolver基类进行缓存.
	 * <p>子类不被强制支持国际化: 不可以忽略locale参数的子类.
	 * 
	 * @param viewName 要检索的视图的名称
	 * @param locale 用于检索视图的Locale
	 * 
	 * @return View实例, 或{@code null} (可选, 允许ViewResolver链接)
	 * @throws Exception 如果视图无法解析
	 */
	protected abstract View loadView(String viewName, Locale locale) throws Exception;

}
