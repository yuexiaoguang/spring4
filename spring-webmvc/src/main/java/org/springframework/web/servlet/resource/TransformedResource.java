package org.springframework.web.servlet.resource;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * An extension of {@link org.springframework.core.io.ByteArrayResource}
 * that a {@link ResourceTransformer} can use to represent an original
 * resource preserving all other information except the content.
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