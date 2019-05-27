package org.springframework.jmx.export.annotation;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;

/**
 * Spring的标准{@link MBeanExporter}的便捷子类, 激活Spring bean的JMX暴露的Java 5注解用法:
 * {@link ManagedResource}, {@link ManagedAttribute}, {@link ManagedOperation}, etc.
 *
 * <p>使用{@link AnnotationJmxAttributeSource}设置{@link MetadataNamingStrategy}和{@link MetadataMBeanInfoAssembler},
 * 并默认激活{@link #AUTODETECT_ALL}模式.
 */
public class AnnotationMBeanExporter extends MBeanExporter {

	private final AnnotationJmxAttributeSource annotationSource =
			new AnnotationJmxAttributeSource();

	private final MetadataNamingStrategy metadataNamingStrategy =
			new MetadataNamingStrategy(this.annotationSource);

	private final MetadataMBeanInfoAssembler metadataAssembler =
			new MetadataMBeanInfoAssembler(this.annotationSource);


	public AnnotationMBeanExporter() {
		setNamingStrategy(this.metadataNamingStrategy);
		setAssembler(this.metadataAssembler);
		setAutodetectMode(AUTODETECT_ALL);
	}


	/**
	 * 指定在未指定source级元数据时, 用于生成ObjectName的默认域.
	 * <p>默认使用bean名称中指定的域 (如果bean名称遵循JMX ObjectName语法);
	 * 否则, 托管的bean类的包名称.
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.metadataNamingStrategy.setDefaultDomain(defaultDomain);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.annotationSource.setBeanFactory(beanFactory);
	}
}
