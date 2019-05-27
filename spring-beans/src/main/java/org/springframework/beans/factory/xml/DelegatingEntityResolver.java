package org.springframework.beans.factory.xml;

import java.io.IOException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.springframework.util.Assert;

/**
 * {@link EntityResolver}实现分别委托给{@link BeansDtdResolver}和{@link PluggableSchemaResolver}, 用于DTD和XML模式.
 */
public class DelegatingEntityResolver implements EntityResolver {

	/** DTD文件的后缀 */
	public static final String DTD_SUFFIX = ".dtd";

	/** 模式定义文件的后缀 */
	public static final String XSD_SUFFIX = ".xsd";


	private final EntityResolver dtdResolver;

	private final EntityResolver schemaResolver;


	/**
	 * 创建一个新的DelegatingEntityResolver, 委托给默认的{@link BeansDtdResolver}和默认的{@link PluggableSchemaResolver}.
	 * <p>使用提供的{@link ClassLoader}配置 {@link PluggableSchemaResolver}.
	 * 
	 * @param classLoader 用于加载的ClassLoader (可以是 {@code null}使用默认的 ClassLoader)
	 */
	public DelegatingEntityResolver(ClassLoader classLoader) {
		this.dtdResolver = new BeansDtdResolver();
		this.schemaResolver = new PluggableSchemaResolver(classLoader);
	}

	/**
	 * 创建一个新的 DelegatingEntityResolver, 委托给给定的 {@link EntityResolver EntityResolvers}.
	 * 
	 * @param dtdResolver 用于解析DTD的EntityResolver
	 * @param schemaResolver 用于解析XML模式的EntityResolver
	 */
	public DelegatingEntityResolver(EntityResolver dtdResolver, EntityResolver schemaResolver) {
		Assert.notNull(dtdResolver, "'dtdResolver' is required");
		Assert.notNull(schemaResolver, "'schemaResolver' is required");
		this.dtdResolver = dtdResolver;
		this.schemaResolver = schemaResolver;
	}


	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if (systemId != null) {
			if (systemId.endsWith(DTD_SUFFIX)) {
				return this.dtdResolver.resolveEntity(publicId, systemId);
			}
			else if (systemId.endsWith(XSD_SUFFIX)) {
				return this.schemaResolver.resolveEntity(publicId, systemId);
			}
		}
		return null;
	}


	@Override
	public String toString() {
		return "EntityResolver delegating " + XSD_SUFFIX + " to " + this.schemaResolver +
				" and " + DTD_SUFFIX + " to " + this.dtdResolver;
	}

}
