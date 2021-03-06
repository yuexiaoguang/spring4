package org.springframework.web.servlet.resource;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

/**
 * 一个{@code VersionStrategy}, 它根据资源的内容计算Hex MD5哈希并将其附加到文件名,
 * e.g. {@code "styles/main-e36d2e05253c6c7085a91522ce43a0b4.css"}.
 */
public class ContentVersionStrategy extends AbstractVersionStrategy {

	public ContentVersionStrategy() {
		super(new FileNameVersionPathStrategy());
	}

	@Override
	public String getResourceVersion(Resource resource) {
		try {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			return DigestUtils.md5DigestAsHex(content);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to calculate hash for " + resource, ex);
		}
	}
}
