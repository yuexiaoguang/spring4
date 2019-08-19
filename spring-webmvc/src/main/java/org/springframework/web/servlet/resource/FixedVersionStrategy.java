package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * {@code VersionStrategy}依赖于作为请求路径前缀应用的固定版本, e.g. 短SHA, 版本名称, 发布日期等.
 *
 * <p>这很有用, 例如当无法使用{@link ContentVersionStrategy}时,
 * 例如使用负责加载JavaScript资源并需要知道其相对路径的JavaScript模块加载器时.
 */
public class FixedVersionStrategy extends AbstractVersionStrategy {

	private final String version;


	/**
	 * @param version 要使用的固定版本字符串
	 */
	public FixedVersionStrategy(String version) {
		super(new PrefixVersionPathStrategy(version));
		this.version = version;
	}


	@Override
	public String getResourceVersion(Resource resource) {
		return this.version;
	}

}
