package org.springframework.oxm.jaxb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class for {@link Jaxb2Marshaller} that scans given packages for classes marked with JAXB2 annotations.
 */
class ClassPathJaxb2TypeScanner {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final TypeFilter[] JAXB2_TYPE_FILTERS = new TypeFilter[] {
			new AnnotationTypeFilter(XmlRootElement.class, false),
			new AnnotationTypeFilter(XmlType.class, false),
			new AnnotationTypeFilter(XmlSeeAlso.class, false),
			new AnnotationTypeFilter(XmlEnum.class, false),
			new AnnotationTypeFilter(XmlRegistry.class, false)};


	private final ResourcePatternResolver resourcePatternResolver;

	private final String[] packagesToScan;


	public ClassPathJaxb2TypeScanner(ClassLoader classLoader, String... packagesToScan) {
		Assert.notEmpty(packagesToScan, "'packagesToScan' must not be empty");
		this.resourcePatternResolver = new PathMatchingResourcePatternResolver(classLoader);
		this.packagesToScan = packagesToScan;
	}


	/**
	 * Scan the packages for classes marked with JAXB2 annotations.
	 * @throws UncategorizedMappingException in case of errors
	 */
	public Class<?>[] scanPackages() throws UncategorizedMappingException {
		try {
			List<Class<?>> jaxb2Classes = new ArrayList<Class<?>>();
			for (String packageToScan : this.packagesToScan) {
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(packageToScan) + RESOURCE_PATTERN;
				Resource[] resources = this.resourcePatternResolver.getResources(pattern);
				MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
				for (Resource resource : resources) {
					MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
					if (isJaxb2Class(metadataReader, metadataReaderFactory)) {
						String className = metadataReader.getClassMetadata().getClassName();
						Class<?> jaxb2AnnotatedClass = this.resourcePatternResolver.getClassLoader().loadClass(className);
						jaxb2Classes.add(jaxb2AnnotatedClass);
					}
				}
			}
			return ClassUtils.toClassArray(jaxb2Classes);
		}
		catch (IOException ex) {
			throw new UncategorizedMappingException("Failed to scan classpath for unlisted classes", ex);
		}
		catch (ClassNotFoundException ex) {
			throw new UncategorizedMappingException("Failed to load annotated classes from classpath", ex);
		}
	}

	protected boolean isJaxb2Class(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
		for (TypeFilter filter : JAXB2_TYPE_FILTERS) {
			if (filter.match(reader, factory) && !reader.getClassMetadata().isInterface() ) {
				return true;
			}
		}
		return false;
	}

}
