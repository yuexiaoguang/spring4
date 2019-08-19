package org.springframework.web.servlet.resource;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.core.io.ByteArrayResource}的扩展,
 * {@link ResourceTransformer}可用于表示原始资源, 保留除内容之外的所有其他信息.
 */
public class TransformedResource extends ByteArrayResource {

	private final String filename;

	private final long lastModified;


	public TransformedResource(Resource original, byte[] transformedContent) {
		super(transformedContent);
		this.filename = original.getFilename();
		try {
			this.lastModified = original.lastModified();
		}
		catch (IOException ex) {
			// should never happen
			throw new IllegalArgumentException(ex);
		}
	}


	@Override
	public String getFilename() {
		return this.filename;
	}

	@Override
	public long lastModified() throws IOException {
		return this.lastModified;
	}

}