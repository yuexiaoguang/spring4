package org.springframework.web.method.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link UriComponentsContributor}, 包含委托的其他贡献者列表,
 * 还包含一个特定的{{@link ConversionService}, 用于将方法参数值格式化为字符串.
 */
public class CompositeUriComponentsContributor implements UriComponentsContributor {

	private final List<Object> contributors = new LinkedList<Object>();

	private final ConversionService conversionService;


	/**
	 * 由于这两者都倾向于由同一个类实现, 因此最方便的选择是在{@code RequestMappingHandlerAdapter}中
	 * 获取配置的{@code HandlerMethodArgumentResolvers}, 并将其提供给此构造函数.
	 * 
	 * @param contributors {@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver}
	 */
	public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
		Collections.addAll(this.contributors, contributors);
		this.conversionService = new DefaultFormattingConversionService();
	}

	/**
	 * 由于这两者都倾向于由同一个类实现, 因此最方便的选择是在{@code RequestMappingHandlerAdapter}中
	 * 获取配置的{@code HandlerMethodArgumentResolvers}, 并将其提供给此构造函数.
	 * 
	 * @param contributors {@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver}
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors) {
		this(contributors, null);
	}

	/**
	 * 由于这两者都倾向于由同一个类实现, 因此最方便的选择是在{@code RequestMappingHandlerAdapter}中
	 * 获取配置的{@code HandlerMethodArgumentResolvers}, 并将其提供给此构造函数.
	 * <p>如果{@link ConversionService}参数是{@code null},
	 * 默认将使用{@link org.springframework.format.support.DefaultFormattingConversionService}.
	 * 
	 * @param contributors {@link UriComponentsContributor}或{@link HandlerMethodArgumentResolver}
	 * @param cs 在将方法参数值添加到URI之前, 需要将其格式化为字符串时, 使用的ConversionService
	 */
	public CompositeUriComponentsContributor(Collection<?> contributors, ConversionService cs) {
		Assert.notNull(contributors, "'uriComponentsContributors' must not be null");
		this.contributors.addAll(contributors);
		this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
	}


	public boolean hasContributors() {
		return this.contributors.isEmpty();
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		for (Object c : this.contributors) {
			if (c instanceof UriComponentsContributor) {
				UriComponentsContributor contributor = (UriComponentsContributor) c;
				if (contributor.supportsParameter(parameter)) {
					return true;
				}
			}
			else if (c instanceof HandlerMethodArgumentResolver) {
				if (((HandlerMethodArgumentResolver) c).supportsParameter(parameter)) {
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public void contributeMethodArgument(MethodParameter parameter, Object value,
			UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {

		for (Object contributor : this.contributors) {
			if (contributor instanceof UriComponentsContributor) {
				UriComponentsContributor ucc = (UriComponentsContributor) contributor;
				if (ucc.supportsParameter(parameter)) {
					ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
					break;
				}
			}
			else if (contributor instanceof HandlerMethodArgumentResolver) {
				if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
					break;
				}
			}
		}
	}

	/**
	 * 一个重载方法, 它使用在构造时创建的ConversionService.
	 */
	public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
			Map<String, Object> uriVariables) {

		this.contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
	}

}
