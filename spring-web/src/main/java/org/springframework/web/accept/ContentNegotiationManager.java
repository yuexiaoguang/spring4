package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于确定请求的{@linkplain MediaType 媒体类型}的中心类.
 * 这是通过委托给配置的{@code ContentNegotiationStrategy}实例列表来完成的.
 *
 * <p>还提供了查找媒体类型的文件扩展名的方法.
 * 这是通过委托给配置的{@code MediaTypeFileExtensionResolver}实例列表来完成的.
 */
public class ContentNegotiationManager implements ContentNegotiationStrategy, MediaTypeFileExtensionResolver {

	private static final List<MediaType> MEDIA_TYPE_ALL = Collections.<MediaType>singletonList(MediaType.ALL);


	private final List<ContentNegotiationStrategy> strategies = new ArrayList<ContentNegotiationStrategy>();

	private final Set<MediaTypeFileExtensionResolver> resolvers = new LinkedHashSet<MediaTypeFileExtensionResolver>();


	/**
	 * 使用给定的{@code ContentNegotiationStrategy}策略列表创建一个实例,
	 * 每个策略也可以是{@code MediaTypeFileExtensionResolver}的实例.
	 * 
	 * @param strategies 要使用的策略
	 */
	public ContentNegotiationManager(ContentNegotiationStrategy... strategies) {
		this(Arrays.asList(strategies));
	}

	/**
	 * 基于集合的{@link #ContentNegotiationManager(ContentNegotiationStrategy...)}的替代方案.
	 * 
	 * @param strategies 要使用的策略
	 */
	public ContentNegotiationManager(Collection<ContentNegotiationStrategy> strategies) {
		Assert.notEmpty(strategies, "At least one ContentNegotiationStrategy is expected");
		this.strategies.addAll(strategies);
		for (ContentNegotiationStrategy strategy : this.strategies) {
			if (strategy instanceof MediaTypeFileExtensionResolver) {
				this.resolvers.add((MediaTypeFileExtensionResolver) strategy);
			}
		}
	}

	public ContentNegotiationManager() {
		this(new HeaderContentNegotiationStrategy());
	}


	/**
	 * 返回配置的内容协商策略.
	 */
	public List<ContentNegotiationStrategy> getStrategies() {
		return this.strategies;
	}

	/**
	 * 查找给定类型的{@code ContentNegotiationStrategy}.
	 * 
	 * @param strategyType 策略类型
	 * 
	 * @return 第一个匹配策略, 或{@code null}
	 */
	@SuppressWarnings("unchecked")
	public <T extends ContentNegotiationStrategy> T getStrategy(Class<T> strategyType) {
		for (ContentNegotiationStrategy strategy : getStrategies()) {
			if (strategyType.isInstance(strategy)) {
				return (T) strategy;
			}
		}
		return null;
	}

	/**
	 * 除了在构造中检测到的实例外, 还要注册更多的{@code MediaTypeFileExtensionResolver}实例.
	 * 
	 * @param resolvers 要添加的解析器
	 */
	public void addFileExtensionResolvers(MediaTypeFileExtensionResolver... resolvers) {
		this.resolvers.addAll(Arrays.asList(resolvers));
	}

	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
		for (ContentNegotiationStrategy strategy : this.strategies) {
			List<MediaType> mediaTypes = strategy.resolveMediaTypes(request);
			if (mediaTypes.isEmpty() || mediaTypes.equals(MEDIA_TYPE_ALL)) {
				continue;
			}
			return mediaTypes;
		}
		return Collections.emptyList();
	}

	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		Set<String> result = new LinkedHashSet<String>();
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			result.addAll(resolver.resolveFileExtensions(mediaType));
		}
		return new ArrayList<String>(result);
	}

	/**
	 * {@inheritDoc}
	 * <p>在启动时, 此方法返回使用{@link PathExtensionContentNegotiationStrategy}
	 * 或{@link ParameterContentNegotiationStrategy}显式注册的扩展名.
	 * 在运行时, 如果存在"路径扩展"策略
	 * 并且其{@link PathExtensionContentNegotiationStrategy#setUseJaf(boolean) useJaf}属性设置为"true",
	 * 则扩展名列表可能会增加, 因为文件扩展名是通过JAF解析并缓存的.
	 */
	@Override
	public List<String> getAllFileExtensions() {
		Set<String> result = new LinkedHashSet<String>();
		for (MediaTypeFileExtensionResolver resolver : this.resolvers) {
			result.addAll(resolver.getAllFileExtensions());
		}
		return new ArrayList<String>(result);
	}

}
