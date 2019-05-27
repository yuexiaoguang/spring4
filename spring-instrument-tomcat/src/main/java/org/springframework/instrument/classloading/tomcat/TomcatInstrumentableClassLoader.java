package org.springframework.instrument.classloading.tomcat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.catalina.loader.ResourceEntry;
import org.apache.catalina.loader.WebappClassLoader;

import org.springframework.instrument.classloading.WeavingTransformer;

/**
 * Tomcat的默认类加载器的扩展, 它向加载的类添加检测, 而不需要使用VM范围的代理.
 *
 * <p>要在{@code server.xml}文件中使用Tomcat的
 * <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/context.html">{@code Context}</a> 定义中的
 * <a href="http://tomcat.apache.org/tomcat-6.0-doc/config/loader.html">{@code Loader}</a>标签进行注册,
 * 并将Spring提供的"spring-instrument-tomcat.jar"文件部署到Tomcat的"lib" 目录中.
 * 所需的配置标签如下所示:
 *
 * <pre class="code">&lt;Loader loaderClass="org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader"/&gt;</pre>
 *
 * <p>通常与Spring应用程序上下文中定义的
 * {@link org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver}结合使用.
 * {@code addTransformer}和{@code getThrowawayClassLoader}方法镜像了LoadTimeWeaver接口中的相应方法,
 * 正如ReflectiveLoadTimeWeaver所期望的那样.
 *
 * <p><b>NOTE:</b> 从Spring 4.0开始, 需要Apache Tomcat 6.0或更高版本.
 * 此类不适用于Tomcat 8.0+; 请依赖Tomcat自己的{@code InstrumentableClassLoader}工具,
 * 由Spring的 {@link org.springframework.instrument.classloading.tomcat.TomcatLoadTimeWeaver}自动检测.
 */
public class TomcatInstrumentableClassLoader extends WebappClassLoader {

	private static final String CLASS_SUFFIX = ".class";

	/** 使用内部WeavingTransformer */
	private final WeavingTransformer weavingTransformer;


	/**
	 * 使用当前上下文类加载器.
	 */
	public TomcatInstrumentableClassLoader() {
		super();
		this.weavingTransformer = new WeavingTransformer(this);
	}

	/**
	 * @param parent 要使用的父{@link ClassLoader}
	 */
	public TomcatInstrumentableClassLoader(ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(this);
	}


	/**
	 * 委托给LoadTimeWeaver的{@code addTransformer}方法.
	 * 通常通过ReflectiveLoadTimeWeaver调用.
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}

	/**
	 * 委托给LoadTimeWeaver的{@code getThrowawayClassLoader}方法.
	 * 通常通过ReflectiveLoadTimeWeaver调用.
	 */
	public ClassLoader getThrowawayClassLoader() {
		WebappClassLoader tempLoader = new WebappClassLoader();
		// 使用反射复制所有字段, 因为它们不以任何其他方式公开.
		shallowCopyFieldState(this, tempLoader);
		return tempLoader;
	}


	@Override  // overriding the pre-7.0.63 variant of findResourceInternal
	protected ResourceEntry findResourceInternal(String name, String path) {
		ResourceEntry entry = super.findResourceInternal(name, path);
		if (entry != null && entry.binaryContent != null && path.endsWith(CLASS_SUFFIX)) {
			String className = (name.endsWith(CLASS_SUFFIX) ? name.substring(0, name.length() - CLASS_SUFFIX.length()) : name);
			entry.binaryContent = this.weavingTransformer.transformIfNecessary(className, entry.binaryContent);
		}
		return entry;
	}

	@Override  // overriding the 7.0.63+ variant of findResourceInternal
	protected ResourceEntry findResourceInternal(String name, String path, boolean manifestRequired) {
		ResourceEntry entry = super.findResourceInternal(name, path, manifestRequired);
		if (entry != null && entry.binaryContent != null && path.endsWith(CLASS_SUFFIX)) {
			String className = (name.endsWith(CLASS_SUFFIX) ? name.substring(0, name.length() - CLASS_SUFFIX.length()) : name);
			entry.binaryContent = this.weavingTransformer.transformIfNecessary(className, entry.binaryContent);
		}
		return entry;
	}

	@Override
	public String toString() {
		return getClass().getName() + "\r\n" + super.toString();
	}


	// 下面的代码最初来自ReflectionUtils, 并针对本地使用进行了优化.
	// ReflectionUtils没有依赖来保持这个类是自包含的 (因为它被部署到Tomcat的服务器类加载器中).
	private static void shallowCopyFieldState(final WebappClassLoader src, final WebappClassLoader dest) {
		Class<?> targetClass = WebappClassLoader.class;
		// 继续备份继承层次结构.
		do {
			Field[] fields = targetClass.getDeclaredFields();
			for (Field field : fields) {
				// 不要复制resourceEntries - 它是一个包含类条目的缓存.
				if (!(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()) ||
						field.getName().equals("resourceEntries"))) {
					try {
						field.setAccessible(true);
						Object srcValue = field.get(src);
						field.set(dest, srcValue);
					}
					catch (IllegalAccessException ex) {
						throw new IllegalStateException(
								"Shouldn't be illegal to access field '" + field.getName() + "': " + ex);
					}
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

}
