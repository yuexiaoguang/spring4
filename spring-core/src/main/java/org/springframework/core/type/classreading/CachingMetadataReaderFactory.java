package org.springframework.core.type.classreading;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * 缓存{@link MetadataReaderFactory}接口的实现,
 * 按照Spring {@link Resource}句柄(i.e. 每个".class"文件)缓存{@link MetadataReader}实例.
 */
public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

	/** MetadataReader缓存的默认最大条目数: 256 */
	public static final int DEFAULT_CACHE_LIMIT = 256;


	private volatile int cacheLimit = DEFAULT_CACHE_LIMIT;

	@SuppressWarnings("serial")
	private final Map<Resource, MetadataReader> metadataReaderCache =
			new LinkedHashMap<Resource, MetadataReader>(DEFAULT_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
					return size() > getCacheLimit();
				}
			};


	/**
	 * 为默认的类加载器创建一个新的CachingMetadataReaderFactory.
	 */
	public CachingMetadataReaderFactory() {
		super();
	}

	/**
	 * 为给定的资源加载器创建一个新的CachingMetadataReaderFactory.
	 * 
	 * @param resourceLoader 要使用的Spring ResourceLoader (也确定要使用的ClassLoader)
	 */
	public CachingMetadataReaderFactory(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}

	/**
	 * 为给定的类加载器创建一个新的CachingMetadataReaderFactory.
	 * 
	 * @param classLoader 要使用的ClassLoader
	 */
	public CachingMetadataReaderFactory(ClassLoader classLoader) {
		super(classLoader);
	}


	/**
	 * 指定MetadataReader缓存的最大条目数.
	 * <p>默认 256.
	 */
	public void setCacheLimit(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	/**
	 * 返回MetadataReader缓存的最大条目数.
	 */
	public int getCacheLimit() {
		return this.cacheLimit;
	}


	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		if (getCacheLimit() <= 0) {
			return super.getMetadataReader(resource);
		}
		synchronized (this.metadataReaderCache) {
			MetadataReader metadataReader = this.metadataReaderCache.get(resource);
			if (metadataReader == null) {
				metadataReader = super.getMetadataReader(resource);
				this.metadataReaderCache.put(resource, metadataReader);
			}
			return metadataReader;
		}
	}

	/**
	 * 清除整个MetadataReader缓存, 删除所有缓存的类元数据.
	 */
	public void clearCache() {
		synchronized (this.metadataReaderCache) {
			this.metadataReaderCache.clear();
		}
	}
}
