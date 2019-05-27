package org.springframework.beans.support;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;

import org.xml.sax.InputSource;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.propertyeditors.ClassArrayEditor;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.InputSourceEditor;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.PathEditor;
import org.springframework.beans.propertyeditors.ReaderEditor;
import org.springframework.beans.propertyeditors.URIEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

/**
 * PropertyEditorRegistrar实现, 使用资源编辑器填充给定的{@link org.springframework.beans.PropertyEditorRegistry}
 * (通常是{@link org.springframework.beans.BeanWrapper}, 用于在 {@link org.springframework.context.ApplicationContext}中创建bean).
 * 由{@link org.springframework.context.support.AbstractApplicationContext}使用.
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

	private static Class<?> pathClass;

	static {
		try {
			pathClass = ClassUtils.forName("java.nio.file.Path", ResourceEditorRegistrar.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Java 7 Path class not available
			pathClass = null;
		}
	}


	private final PropertyResolver propertyResolver;

	private final ResourceLoader resourceLoader;


	/**
	 * 为给定的{@link ResourceLoader}和{@link PropertyResolver}创建一个新的ResourceEditorRegistrar.
	 * 
	 * @param resourceLoader 要创建编辑器的ResourceLoader (或ResourcePatternResolver) (通常是ApplicationContext)
	 * @param propertyResolver PropertyResolver (通常是 Environment)
	 */
	public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
	}


	/**
	 * 使用以下资源编辑器填充给定的 {@code registry}:
	 * ResourceEditor, InputStreamEditor, InputSourceEditor, FileEditor, URLEditor, URIEditor, ClassEditor, ClassArrayEditor.
	 * <p>如果此注册表已经使用 {@link ResourcePatternResolver}配置, 则还将注册ResourceArrayPropertyEditor.
	 */
	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
		doRegisterEditor(registry, Resource.class, baseEditor);
		doRegisterEditor(registry, ContextResource.class, baseEditor);
		doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
		doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
		doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
		if (pathClass != null) {
			doRegisterEditor(registry, pathClass, new PathEditor(baseEditor));
		}
		doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
		doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

		ClassLoader classLoader = this.resourceLoader.getClassLoader();
		doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
		doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
		doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

		if (this.resourceLoader instanceof ResourcePatternResolver) {
			doRegisterEditor(registry, Resource[].class,
					new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
		}
	}

	/**
	 * 覆盖默认的编辑器 (因为这就是我们在这里真正要做的事情);
	 * 否则注册为自定义编辑器.
	 */
	private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
		if (registry instanceof PropertyEditorRegistrySupport) {
			((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
		}
		else {
			registry.registerCustomEditor(requiredType, editor);
		}
	}

}
