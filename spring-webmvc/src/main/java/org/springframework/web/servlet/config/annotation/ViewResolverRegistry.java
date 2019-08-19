package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

/**
 * 协助配置{@link org.springframework.web.servlet.ViewResolver ViewResolver}实例链.
 * 该类预计将通过{@link WebMvcConfigurer#configureViewResolvers}使用.
 */
public class ViewResolverRegistry {

	private ContentNegotiationManager contentNegotiationManager;

	private ApplicationContext applicationContext;

	private ContentNegotiatingViewResolver contentNegotiatingResolver;

	private final List<ViewResolver> viewResolvers = new ArrayList<ViewResolver>(4);

	private Integer order;


	public ViewResolverRegistry(ContentNegotiationManager contentNegotiationManager, ApplicationContext context) {
		this.contentNegotiationManager = contentNegotiationManager;
		this.applicationContext = context;
	}

	@Deprecated
	public ViewResolverRegistry() {
	}


	/**
	 * 是否已注册视图解析器.
	 */
	public boolean hasRegistrations() {
		return (this.contentNegotiatingResolver != null || !this.viewResolvers.isEmpty());
	}

	/**
	 * 允许使用{@link ContentNegotiatingViewResolver}来显示所有其他已配置的视图解析器,
	 * 并根据客户端请求的媒体类型在所有选定的视图中进行选择 (e.g. 在Accept header中).
	 * <p>如果多次调用, 则提供的默认视图将添加到可能已配置的任何其他默认视图中.
	 */
	public void enableContentNegotiation(View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
	}

	/**
	 * 允许使用{@link ContentNegotiatingViewResolver}来显示所有其他已配置的视图解析器,
	 * 并根据客户端请求的媒体类型在所有选定的视图中进行选择 (e.g. 在Accept header中).
	 * <p>如果多次调用, 则提供的默认视图将添加到可能已配置的任何其他默认视图中.
	 */
	public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
		this.contentNegotiatingResolver.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
	}

	private void initContentNegotiatingViewResolver(View[] defaultViews) {
		// 注册表中的ContentNegotiatingResolver: 提升其优先级!
		this.order = (this.order != null ? this.order : Ordered.HIGHEST_PRECEDENCE);

		if (this.contentNegotiatingResolver != null) {
			if (!ObjectUtils.isEmpty(defaultViews)) {
				if (!CollectionUtils.isEmpty(this.contentNegotiatingResolver.getDefaultViews())) {
					List<View> views = new ArrayList<View>(this.contentNegotiatingResolver.getDefaultViews());
					views.addAll(Arrays.asList(defaultViews));
					this.contentNegotiatingResolver.setDefaultViews(views);
				}
			}
		}
		else {
			this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
			this.contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
			this.contentNegotiatingResolver.setViewResolvers(this.viewResolvers);
			this.contentNegotiatingResolver.setContentNegotiationManager(this.contentNegotiationManager);
		}
	}

	/**
	 * 使用默认视图名称前缀"/WEB-INF/"和默认后缀".jsp"注册JSP视图解析器.
	 * <p>当多次调用此方法时, 每次调用都将注册一个新的ViewResolver实例.
	 * 请注意, 由于在不转发JSP的情况下确定JSP是否存在并不容易,
	 * 因此使用多个基于JSP的视图解析器只能与解析器上的"viewNames"属性结合使用, 指示哪个视图名称由哪个解析器处理.
	 */
	public UrlBasedViewResolverRegistration jsp() {
		return jsp("/WEB-INF/", ".jsp");
	}

	/**
	 * 使用指定的前缀和后缀注册JSP视图解析器.
	 * <p>当多次调用此方法时, 每次调用都将注册一个新的ViewResolver实例.
	 * 请注意, 由于在不转发JSP的情况下确定JSP是否存在并不容易,
	 * 因此使用多个基于JSP的视图解析器只能与解析器上的"viewNames"属性结合使用, 指示哪个视图名称由哪个解析器处理.
	 */
	public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix(prefix);
		resolver.setSuffix(suffix);
		this.viewResolvers.add(resolver);
		return new UrlBasedViewResolverRegistration(resolver);
	}

	/**
	 * 注册Tiles 3.x视图解析器.
	 * <p><strong>Note</strong> 还必须通过添加
	 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} bean来配置Tiles.
	 */
	public UrlBasedViewResolverRegistration tiles() {
		if (!checkBeanOfType(TilesConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Tiles view resolver " +
					"there must also be a single TilesConfigurer bean in this web application context " +
					"(or its parent).");
		}
		TilesRegistration registration = new TilesRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册一个FreeMarker视图解析器, 其中包含一个空的默认视图名称前缀和一个默认后缀".ftl".
	 * <p><strong>Note</strong> 还必须通过添加
	 * {@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} bean来配置FreeMarker.
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和默认后缀".vm"注册Velocity视图解析器.
	 * <p><strong>Note</strong>还必须通过添加
	 * {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer} bean来配置Velocity.
	 * 
	 * @deprecated as of Spring 4.3, in favor of FreeMarker
	 */
	@Deprecated
	public UrlBasedViewResolverRegistration velocity() {
		if (!checkBeanOfType(org.springframework.web.servlet.view.velocity.VelocityConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Velocity view resolver " +
					"there must also be a single VelocityConfig bean in this web application context " +
					"(or its parent): VelocityConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		VelocityRegistration registration = new VelocityRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和默认后缀".tpl"注册Groovy标记视图解析器.
	 */
	public UrlBasedViewResolverRegistration groovy() {
		if (!checkBeanOfType(GroovyMarkupConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Groovy markup view resolver " +
					"there must also be a single GroovyMarkupConfig bean in this web application context " +
					"(or its parent): GroovyMarkupConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		GroovyMarkupRegistration registration = new GroovyMarkupRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 使用空的默认视图名称前缀和后缀注册脚本模板视图解析器.
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		ScriptRegistration registration = new ScriptRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册一个bean名称视图解析器, 将视图名称解释为{@link org.springframework.web.servlet.View} bean的名称.
	 */
	public void beanName() {
		BeanNameViewResolver resolver = new BeanNameViewResolver();
		this.viewResolvers.add(resolver);
	}

	/**
	 * 注册{@link ViewResolver} bean实例.
	 * 这可能对配置自定义 (或第三方) 解析器实现很有用.
	 * 当它们不公开某些更高级的属性时, 它也可以用作此类中其他注册方法的替代方法.
	 */
	public void viewResolver(ViewResolver viewResolver) {
		if (viewResolver instanceof ContentNegotiatingViewResolver) {
			throw new BeanInitializationException(
					"addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. " +
					"Please use the method enableContentNegotiation instead.");
		}
		this.viewResolvers.add(viewResolver);
	}

	/**
	 * 通过此注册表注册的ViewResolver封装在
	 * {@link org.springframework.web.servlet.view.ViewResolverComposite ViewResolverComposite}的实例中,
	 * 并按照注册顺序进行.
	 * 此属性确定ViewResolverComposite本身相对于Spring配置中其他ViewResolver(此处未注册)的顺序
	 * <p>默认情况下, 此属性未设置, 这意味着解析器在{@link Ordered#LOWEST_PRECEDENCE}处排序,
	 * 除非启用了内容协商, 在这种情况下, 顺序(如果未明确设置)更改为{@link Ordered#HIGHEST_PRECEDENCE}.
	 */
	public void order(int order) {
		this.order = order;
	}


	protected int getOrder() {
		return (this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE);
	}

	protected List<ViewResolver> getViewResolvers() {
		if (this.contentNegotiatingResolver != null) {
			return Collections.<ViewResolver>singletonList(this.contentNegotiatingResolver);
		}
		else {
			return this.viewResolvers;
		}
	}

	private boolean checkBeanOfType(Class<?> beanType) {
		return (this.applicationContext == null ||
				!ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.applicationContext, beanType, false, false)));
	}

	@Deprecated
	protected boolean hasBeanOfType(Class<?> beanType) {
		return !ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this.applicationContext, beanType, false, false));
	}

	@Deprecated
	protected void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	@Deprecated
	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	private static class TilesRegistration extends UrlBasedViewResolverRegistration {

		public TilesRegistration() {
			super(new TilesViewResolver());
		}
	}


	private static class VelocityRegistration extends UrlBasedViewResolverRegistration {

		@SuppressWarnings("deprecation")
		public VelocityRegistration() {
			super(new org.springframework.web.servlet.view.velocity.VelocityViewResolver());
			getViewResolver().setSuffix(".vm");
		}
	}


	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}


	private static class GroovyMarkupRegistration extends UrlBasedViewResolverRegistration {

		public GroovyMarkupRegistration() {
			super(new GroovyMarkupViewResolver());
			getViewResolver().setSuffix(".tpl");
		}
	}


	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
