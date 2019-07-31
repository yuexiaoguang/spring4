package org.springframework.http.converter.json;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link FactoryBean}, 用于创建Jackson 2.x {@link ObjectMapper} (默认) 或{@link XmlMapper} ({@code createXmlMapper}属性设置为 true),
 * 使用setter在XML配置中启用或禁用Jackson功能.
 *
 * <p>它使用以下属性自定义Jackson默认属性:
 * <ul>
 * <li>禁用{@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>{@link MappingJackson2HttpMessageConverter}的示例用法:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:autoDetectFields="false"
 *       p:autoDetectGettersSetters="false"
 *       p:annotationIntrospector-ref="jaxbAnnotationIntrospector" />
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>MappingJackson2JsonView的示例用法:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.web.servlet.view.json.MappingJackson2JsonView">
 *   &lt;property name="objectMapper">
 *     &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean"
 *       p:failOnEmptyBeans="false"
 *       p:indentOutput="true">
 *       &lt;property name="serializers">
 *         &lt;array>
 *           &lt;bean class="org.mycompany.MyCustomSerializer" />
 *         &lt;/array>
 *       &lt;/property>
 *     &lt;/bean>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>如果没有提供特定的setter (对于一些很少使用的选项),
 * 仍然可以使用更通用的方法{@link #setFeaturesToEnable}和{@link #setFeaturesToDisable}.
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean">
 *   &lt;property name="featuresToEnable">
 *     &lt;array>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE"/>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.SerializationFeature.CLOSE_CLOSEABLE"/>
 *     &lt;/array>
 *   &lt;/property>
 *   &lt;property name="featuresToDisable">
 *     &lt;array>
 *       &lt;util:constant static-field="com.fasterxml.jackson.databind.MapperFeature.USE_ANNOTATIONS"/>
 *     &lt;/array>
 *   &lt;/property>
 * &lt;/bean>
 * </pre>
 *
 * <p>如果在类路径上检测到它们, 它还会自动注册以下已知的模块:
 * <ul>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk7">jackson-datatype-jdk7</a>:
 * 支持Java 7类型, 如{@link java.nio.file.Path}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jdk8">jackson-datatype-jdk8</a>:
 * 支持其他Java 8类型, 如{@link java.util.Optional}</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-jsr310">jackson-datatype-jsr310</a>:
 * 支持Java 8 Date & Time API类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-datatype-joda">jackson-datatype-joda</a>:
 * 支持Joda-Time类型</li>
 * <li><a href="https://github.com/FasterXML/jackson-module-kotlin">jackson-module-kotlin</a>:
 * 支持Kotlin类和数据类</li>
 * </ul>
 *
 * <p>如果使用自定义{@link Module}配置Jackson的{@link ObjectMapper},
 * 可以通过{@link #setModulesToInstall}按类名注册一个或多个此类模块:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean">
 *   &lt;property name="modulesToInstall" value="myapp.jackson.MySampleModule,myapp.jackson.MyOtherModule"/>
 * &lt;/bean
 * </pre>
 *
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class Jackson2ObjectMapperFactoryBean implements FactoryBean<ObjectMapper>, BeanClassLoaderAware,
		ApplicationContextAware, InitializingBean {

	private final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

	private ObjectMapper objectMapper;


	/**
	 * 设置要使用的{@link ObjectMapper}实例.
	 * 如果未设置, 将使用其默认构造函数创建{@link ObjectMapper}.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * 如果设置为true且未设置自定义{@link ObjectMapper}, 则将使用其默认构造函数创建{@link XmlMapper}.
	 */
	public void setCreateXmlMapper(boolean createXmlMapper) {
		this.builder.createXmlMapper(createXmlMapper);
	}

	/**
	 * 使用给定的{@link DateFormat}定义日期/时间的格式.
	 * <p>Note: 根据Jackson的线程安全规则, 设置此属性会使公开的{@link ObjectMapper}非线程安全.
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.builder.dateFormat(dateFormat);
	}

	/**
	 * 使用{@link SimpleDateFormat}定义日期/时间格式.
	 * <p>Note: 根据Jackson的线程安全规则, 设置此属性会使公开的{@link ObjectMapper}非线程安全.
	 */
	public void setSimpleDateFormat(String format) {
		this.builder.simpleDateFormat(format);
	}

	/**
	 * 覆盖默认的{@link Locale}以用于格式化.
	 * 默认{@link Locale#getDefault()}.
	 */
	public void setLocale(Locale locale) {
		this.builder.locale(locale);
	}

	/**
	 * 覆盖默认的{@link TimeZone}以用于格式化.
	 * 默认UTC (非本地时区).
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.builder.timeZone(timeZone);
	}

	/**
	 * 为序列化和反序列化设置{@link AnnotationIntrospector}.
	 */
	public void setAnnotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.builder.annotationIntrospector(annotationIntrospector);
	}

	/**
	 * 指定配置{@link ObjectMapper}使用的{@link com.fasterxml.jackson.databind.PropertyNamingStrategy}.
	 */
	public void setPropertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.builder.propertyNamingStrategy(propertyNamingStrategy);
	}

	/**
	 * 指定用于Jackson的默认输入的{@link TypeResolverBuilder}.
	 */
	public void setDefaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.builder.defaultTyping(typeResolverBuilder);
	}

	/**
	 * 设置序列化的自定义包含策略.
	 */
	public void setSerializationInclusion(JsonInclude.Include serializationInclusion) {
		this.builder.serializationInclusion(serializationInclusion);
	}

	/**
	 * 设置要使用的全局过滤器以支持带{@link JsonFilter @JsonFilter}注解的 POJO.
	 */
	public void setFilters(FilterProvider filters) {
		this.builder.filters(filters);
	}

	/**
	 * 添加混合注解以用于扩充指定的类或接口.
	 * 
	 * @param mixIns 具有目标类 (或接口)的条目的Map, 该目标类的注解有效地覆盖为键和混合类 (或接口), 其注解将作为值"添加"到目标的注解.
	 */
	public void setMixIns(Map<Class<?>, Class<?>> mixIns) {
		this.builder.mixIns(mixIns);
	}

	/**
	 * 配置自定义序列化器. 每个序列化器都注册了{@link JsonSerializer#handledType()}返回的类型, 该类型不能是{@code null}.
	 */
	public void setSerializers(JsonSerializer<?>... serializers) {
		this.builder.serializers(serializers);
	}

	/**
	 * 为给定类型配置自定义序列化器.
	 */
	public void setSerializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		this.builder.serializersByType(serializers);
	}

	/**
	 * 配置自定义反序列化器.
	 * 每个反序列化器都为{@link JsonDeserializer#handledType()}返回的类型注册, 该类型不能是{@code null}.
	 */
	public void setDeserializers(JsonDeserializer<?>... deserializers) {
		this.builder.deserializers(deserializers);
	}

	/**
	 * 配置给定类型的自定义反序列化器.
	 */
	public void setDeserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		this.builder.deserializersByType(deserializers);
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
	 */
	public void setAutoDetectFields(boolean autoDetectFields) {
		this.builder.autoDetectFields(autoDetectFields);
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS} options.
	 */
	public void setAutoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.builder.autoDetectGettersSetters(autoDetectGettersSetters);
	}

	/**
	 * Shortcut for {@link MapperFeature#DEFAULT_VIEW_INCLUSION} option.
	 */
	public void setDefaultViewInclusion(boolean defaultViewInclusion) {
		this.builder.defaultViewInclusion(defaultViewInclusion);
	}

	/**
	 * Shortcut for {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} option.
	 */
	public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
		this.builder.failOnUnknownProperties(failOnUnknownProperties);
	}

	/**
	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
	 */
	public void setFailOnEmptyBeans(boolean failOnEmptyBeans) {
		this.builder.failOnEmptyBeans(failOnEmptyBeans);
	}

	/**
	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
	 */
	public void setIndentOutput(boolean indentOutput) {
		this.builder.indentOutput(indentOutput);
	}

	/**
	 * 定义默认情况下是否将包装器用于索引的 (List, array)属性 (仅适用于{@link XmlMapper}).
	 */
	public void setDefaultUseWrapper(boolean defaultUseWrapper) {
		this.builder.defaultUseWrapper(defaultUseWrapper);
	}

	/**
	 * 指定要启用的功能.
	 */
	public void setFeaturesToEnable(Object... featuresToEnable) {
		this.builder.featuresToEnable(featuresToEnable);
	}

	/**
	 * 指定要禁用的功能.
	 */
	public void setFeaturesToDisable(Object... featuresToDisable) {
		this.builder.featuresToDisable(featuresToDisable);
	}

	/**
	 * 设置要在{@link ObjectMapper}中注册的完整模块列表.
	 * <p>Note: 如果确定了这一点, 那么找不到模块就不会发生 - 不是Jackson, 也不是Spring (see {@link #setFindModulesViaServiceLoader}).
	 * 因此, 在此处指定空列表将禁止任何类型的模块检测.
	 * <p>指定this或{@link #setModulesToInstall}, 而不是两者.
	 */
	public void setModules(List<Module> modules) {
		this.builder.modules(modules);
	}

	/**
	 * 按类(或XML中的类名)指定一个或多个要在{@link ObjectMapper}中注册的模块.
	 * <p>这里指定的模块将在Spring自动检测JSR-310和Joda-Time之后注册,
	 * 或者Jackson的查找模块之后 (see {@link #setFindModulesViaServiceLoader}), 允许最终覆盖其配置.
	 * <p>指定this或{@link #setModules}, 而不是两者.
	 */
	@SuppressWarnings("unchecked")
	public void setModulesToInstall(Class<? extends Module>... modules) {
		this.builder.modulesToInstall(modules);
	}

	/**
	 * 设置是否让Jackson根据类路径中的META-INF元数据通过JDK ServiceLoader查找可用模块. 需要Jackson 2.2或更高版本.
	 * <p>如果未设置此模​​式, Spring的Jackson2ObjectMapperFactoryBean本身将尝试
	 * 在类路径上查找JSR-310和Joda-Time支持模块 - 前提是Java 8和Joda-Time本身都可用.
	 */
	public void setFindModulesViaServiceLoader(boolean findModules) {
		this.builder.findModulesViaServiceLoader(findModules);
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.builder.moduleClassLoader(beanClassLoader);
	}

	/**
	 * 定制Jackson处理器的构造
	 * ({@link JsonSerializer}, {@link JsonDeserializer}, {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
	 */
	public void setHandlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.builder.handlerInstantiator(handlerInstantiator);
	}

	/**
	 * 设置构建器{@link ApplicationContext}以自动装配Jackson处理器
	 * ({@link JsonSerializer}, {@link JsonDeserializer}, {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.builder.applicationContext(applicationContext);
	}


	@Override
	public void afterPropertiesSet() {
		if (this.objectMapper != null) {
			this.builder.configure(this.objectMapper);
		}
		else {
			this.objectMapper = this.builder.build();
		}
	}

	/**
	 * Return the singleton ObjectMapper.
	 */
	@Override
	public ObjectMapper getObject() {
		return this.objectMapper;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.objectMapper != null ? this.objectMapper.getClass() : null);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
