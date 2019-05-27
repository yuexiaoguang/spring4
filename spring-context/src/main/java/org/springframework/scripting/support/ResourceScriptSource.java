package org.springframework.scripting.support;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.scripting.ScriptSource}实现, 基于Spring的{@link org.springframework.core.io.Resource}抽象.
 * 从底层资源的{@link org.springframework.core.io.Resource#getFile() File}
 * 或{@link org.springframework.core.io.Resource#getInputStream() InputStream}加载脚本文本,
 * 并跟踪文件的上次修改时间戳.
 */
public class ResourceScriptSource implements ScriptSource {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private EncodedResource resource;

	private long lastModified = -1;

	private final Object lastModifiedMonitor = new Object();


	/**
	 * @param resource 从中加载脚本的EncodedResource
	 */
	public ResourceScriptSource(EncodedResource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
	}

	/**
	 * @param resource 从中加载脚本的Resource (使用UTF-8编码)
	 */
	public ResourceScriptSource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = new EncodedResource(resource, "UTF-8");
	}


	/**
	 * 返回从中加载脚本的{@link org.springframework.core.io.Resource}.
	 */
	public final Resource getResource() {
		return this.resource.getResource();
	}

	/**
	 * 设置用于读取脚本资源的编码.
	 * <p>常规资源的默认值为 "UTF-8". {@code null}值表示平台默认值.
	 */
	public void setEncoding(String encoding) {
		this.resource = new EncodedResource(this.resource.getResource(), encoding);
	}


	@Override
	public String getScriptAsString() throws IOException {
		synchronized (this.lastModifiedMonitor) {
			this.lastModified = retrieveLastModifiedTime();
		}
		Reader reader = this.resource.getReader();
		return FileCopyUtils.copyToString(reader);
	}

	@Override
	public boolean isModified() {
		synchronized (this.lastModifiedMonitor) {
			return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
		}
	}

	/**
	 * 检索底层资源的当前上次修改时间戳.
	 * 
	 * @return 当前时间戳, 如果不可确定则为0
	 */
	protected long retrieveLastModifiedTime() {
		try {
			return getResource().lastModified();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(getResource() + " could not be resolved in the file system - " +
						"current timestamp not available for script modification check", ex);
			}
			return 0;
		}
	}

	@Override
	public String suggestedClassName() {
		return StringUtils.stripFilenameExtension(getResource().getFilename());
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

}
