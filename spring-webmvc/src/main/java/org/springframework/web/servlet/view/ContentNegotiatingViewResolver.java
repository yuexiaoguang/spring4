package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link ViewResolver}的实现, 它根据请求文件名或{@code Accept} header解析视图.
 *
 * <p>{@code ContentNegotiatingViewResolver}本身不解析视图, 而是委托给其他{@link ViewResolver}.
 * 默认情况下, 这些其他视图解析器会自动从应用程序上下文中选取,
 * 但也可以使用{@link #setViewResolvers viewResolvers}属性显式设置它们.
 * <strong>Note</strong> 为了使此视图解析器正常工作,
 * 需要将{@link #setOrder order}属性设置为比其他解析器更高的优先级 (默认为{@link Ordered#HIGHEST_PRECEDENCE}).
 *
 * <p>此视图解析器使用请求的{@linkplain MediaType 媒体类型}为请求选择合适的{@link View}.
 * 请求的媒体类型通过配置的{@link ContentNegotiationManager}确定.
 * 确定所请求的媒体类型后, 此解析器将查询每个代理视图解析器以查找{@link View},
 * 并确定所请求的媒体类型是否与视图的{@linkplain View#getContentType() 内容类型}
 * {@linkplain MediaType#includes(MediaType) 兼容}).
 * 返回最兼容的视图.
 *
 * <p>此外, 此视图解析器公开{@link #setDefaultViews(List) defaultViews}属性, 允许覆盖视图解析器提供的视图.
 * 请注意, 这些默认视图是作为候选者提供的, 并且仍然需要具有所请求的内容类型 (通过文件扩展名, 参数, 或{@code Accept} header, 如上所述).
 *
 * <p>例如, 如果请求路径为{@code /view.html}, 则此视图解析器将查找具有{@code text/html}内容类型的视图 (基于{@code html}文件扩展名).
 * 使用{@code text/html}请求{@code Accept} header请求 {@code /view}具有相同的结果.
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport
		implements ViewResolver, Ordered, InitializingBean {

	private ContentNegotiationManager contentNegotiationManager;

	private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

	private boolean useNotAcceptableStatusCode = false;

	private List<View> defaultViews;

	private List<ViewResolver> viewResolvers;

	private int order = Ordered.HIGHEST_PRECEDENCE;


	/**
	 * 设置用于确定请求的媒体类型的{@link ContentNegotiationManager}.
	 * <p>如果未设置, 将使用ContentNegotiationManager的默认构造函数,
	 * 应用{@link org.springframework.web.accept.HeaderContentNegotiationStrategy}.
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回用于确定所请求的媒体类型的{@link ContentNegotiationManager}.
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 如果找不到合适的视图, 指明是否应返回{@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable}状态码.
	 * <p>默认为{@code false}, 这意味着当无法找到可接受的视图时, 此视图解析器会返回{@code null}
	 * 以获取{@link #resolveViewName(String, Locale)}.
	 * 这将允许视图解析器链接. 当此属性设置为{@code true}时, {@link #resolveViewName(String, Locale)}将响应一个视图,
	 * 将响应状态设置为{@code 406 Not Acceptable}.
	 */
	public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
		this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
	}

	/**
	 * 如果找不到合适的, 是否返回HTTP状态406.
	 */
	public boolean isUseNotAcceptableStatusCode() {
		return this.useNotAcceptableStatusCode;
	}

	/**
	 * 设置无法从{@link ViewResolver}链获取更具体的视图时使用的默认视图.
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews = defaultViews;
	}

	public List<View> getDefaultViews() {
		return Collections.unmodifiableList(this.defaultViews);
	}

	/**
	 * 设置要由此视图解析器包装的视图解析器.
	 * <p>如果未设置此属性, 将自动检测视图解析器.
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		Collection<ViewResolver> matchingBeans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(), ViewResolver.class).values();
		if (this.viewResolvers == null) {
			this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		}
		else {
			for (int i = 0; i < viewResolvers.size(); i++) {
				if (matchingBeans.contains(viewResolvers.get(i))) {
					continue;
				}
				String name = viewResolvers.get(i).getClass().getName() + i;
				getApplicationContext().getAutowireCapableBeanFactory().initializeBean(viewResolvers.get(i), name);
			}

		}
		if (this.viewResolvers.isEmpty()) {
			logger.warn("Did not find any ViewResolvers to delegate to; please configure them using the " +
					"'viewResolvers' property on the ContentNegotiatingViewResolver");
		}
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
		this.cnmFactoryBean.setServletContext(servletContext);
	}

	@Override
	public void afterPropertiesSet() {
		if (this.contentNegotiationManager == null) {
			this.cnmFactoryBean.afterPropertiesSet();
			this.contentNegotiationManager = this.cnmFactoryBean.getObject();
		}
	}


	@Override
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
		if (requestedMediaTypes != null) {
			List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
			View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
			if (bestView != null) {
				return bestView;
			}
		}
		if (this.useNotAcceptableStatusCode) {
			if (logger.isDebugEnabled()) {
				logger.debug("No acceptable view found; returning 406 (Not Acceptable) status code");
			}
			return NOT_ACCEPTABLE_VIEW;
		}
		else {
			logger.debug("No acceptable view found; returning null");
			return null;
		}
	}

	/**
	 * 确定给定{@link HttpServletRequest}的{@link MediaType}列表.
	 * 
	 * @param request 当前的servlet请求
	 * 
	 * @return 请求的媒体类型列表
	 */
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		try {
			ServletWebRequest webRequest = new ServletWebRequest(request);

			List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
			acceptableMediaTypes = (!acceptableMediaTypes.isEmpty() ? acceptableMediaTypes :
					Collections.singletonList(MediaType.ALL));

			List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
			Set<MediaType> compatibleMediaTypes = new LinkedHashSet<MediaType>();
			for (MediaType acceptable : acceptableMediaTypes) {
				for (MediaType producible : producibleMediaTypes) {
					if (acceptable.isCompatibleWith(producible)) {
						compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
					}
				}
			}
			List<MediaType> selectedMediaTypes = new ArrayList<MediaType>(compatibleMediaTypes);
			MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
			if (logger.isDebugEnabled()) {
				logger.debug("Requested media types are " + selectedMediaTypes + " based on Accept header types " +
						"and producible media types " + producibleMediaTypes + ")");
			}
			return selectedMediaTypes;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
		Set<MediaType> mediaTypes = (Set<MediaType>)
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<MediaType>(mediaTypes);
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	/**
	 * 使用前者的q值, 返回更具体的可接受和可生成的媒体类型.
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
	}

	private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
			throws Exception {

		List<View> candidateViews = new ArrayList<View>();
		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				candidateViews.add(view);
			}
			for (MediaType requestedMediaType : requestedMediaTypes) {
				List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
				for (String extension : extensions) {
					String viewNameWithExtension = viewName + '.' + extension;
					view = viewResolver.resolveViewName(viewNameWithExtension, locale);
					if (view != null) {
						candidateViews.add(view);
					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(this.defaultViews)) {
			candidateViews.addAll(this.defaultViews);
		}
		return candidateViews;
	}

	private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
		for (View candidateView : candidateViews) {
			if (candidateView instanceof SmartView) {
				SmartView smartView = (SmartView) candidateView;
				if (smartView.isRedirectView()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Returning redirect view [" + candidateView + "]");
					}
					return candidateView;
				}
			}
		}
		for (MediaType mediaType : requestedMediaTypes) {
			for (View candidateView : candidateViews) {
				if (StringUtils.hasText(candidateView.getContentType())) {
					MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
					if (mediaType.isCompatibleWith(candidateContentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Returning [" + candidateView + "] based on requested media type '" +
									mediaType + "'");
						}
						attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
						return candidateView;
					}
				}
			}
		}
		return null;
	}


	private static final View NOT_ACCEPTABLE_VIEW = new View() {

		@Override
		public String getContentType() {
			return null;
		}

		@Override
		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	};
}
