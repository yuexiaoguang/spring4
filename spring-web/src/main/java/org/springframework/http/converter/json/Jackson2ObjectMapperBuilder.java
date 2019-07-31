package org.springframework.http.converter.json;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * 用于使用流畅的API创建{@link ObjectMapper}实例的构建器.
 *
 * <p>它通过以下方式定制Jackson的默认属性:
 * <ul>
 * <li>禁止{@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁止{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
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
 * <p>从Spring 4.3开始, 与Jackson 2.6及更高版本兼容.
 */
public class Jackson2ObjectMapperBuilder {

	private boolean createXmlMapper = false;

	private DateFormat dateFormat;

	private Locale locale;

	private TimeZone timeZone;

	private AnnotationIntrospector annotationIntrospector;

	private PropertyNamingStrategy propertyNamingStrategy;

	private TypeResolverBuilder<?> defaultTyping;

	private JsonInclude.Include serializationInclusion;

	private FilterProvider filters;

	private final Map<Class<?>, Class<?>> mixIns = new HashMap<Class<?>, Class<?>>();

	private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<Class<?>, JsonSerializer<?>>();

	private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<Class<?>, JsonDeserializer<?>>();

	private final Map<Object, Boolean> features = new HashMap<Object, Boolean>();

	private List<Module> modules;

	private Class<? extends Module>[] moduleClasses;

	private boolean findModulesViaServiceLoader = false;

	private boolean findWellKnownModules = true;

	private ClassLoader moduleClassLoader = getClass().getClassLoader();

	private HandlerInstantiator handlerInstantiator;

	private ApplicationContext applicationContext;

	private Boolean defaultUseWrapper;


	/**
	 * 如果设置为{@code true}, 将使用其默认构造函数创建{@link XmlMapper}.
	 * 这仅适用于{@link #build()}调用, 而不适用于{@link #configure}调用.
	 */
	public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
		this.createXmlMapper = createXmlMapper;
		return this;
	}

	/**
	 * 使用给定的{@link DateFormat}定义日期/时间的格式.
	 * <p>Note: 根据Jackson的线程安全规则, 设置此属性会使公开的{@link ObjectMapper}非线程安全.
	 */
	public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
		return this;
	}

	/**
	 * 使用{@link SimpleDateFormat}定义日期/时间格式.
	 * <p>Note: 根据Jackson的线程安全规则, 设置此属性会使公开的{@link ObjectMapper}非线程安全.
	 */
	public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
		this.dateFormat = new SimpleDateFormat(format);
		return this;
	}

	/**
	 * 覆盖默认的{@link Locale}以用于格式化.
	 * 默认{@link Locale#getDefault()}.
	 */
	public Jackson2ObjectMapperBuilder locale(Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * 覆盖默认的{@link Locale}以用于格式化.
	 * 默认{@link Locale#getDefault()}.
	 * 
	 * @param localeString 区域设置ID
	 */
	public Jackson2ObjectMapperBuilder locale(String localeString) {
		this.locale = StringUtils.parseLocaleString(localeString);
		return this;
	}

	/**
	 * 覆盖默认的{@link TimeZone}以用于格式化.
	 * 默认UTC (不是本地时区).
	 */
	public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	/**
	 * 覆盖默认的{@link TimeZone}以用于格式化.
	 * 默认UTC (不是本地时区).
	 * 
	 * @param timeZoneString 区域ID
	 */
	public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
		this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
		return this;
	}

	/**
	 * 为序列化和反序列化设置{@link AnnotationIntrospector}.
	 */
	public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
		this.annotationIntrospector = annotationIntrospector;
		return this;
	}

	/**
	 * 指定配置{@link ObjectMapper}使用的{@link com.fasterxml.jackson.databind.PropertyNamingStrategy}.
	 */
	public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
		this.propertyNamingStrategy = propertyNamingStrategy;
		return this;
	}

	/**
	 * 指定用于Jackson的默认输入的{@link TypeResolverBuilder}.
	 */
	public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
		this.defaultTyping = typeResolverBuilder;
		return this;
	}

	/**
	 * 设置序列化的自定义包含策略.
	 */
	public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include serializationInclusion) {
		this.serializationInclusion = serializationInclusion;
		return this;
	}

	/**
	 * 设置要使用的全局过滤器以支持带{@link JsonFilter @JsonFilter}注解的POJO.
	 */
	public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
		this.filters = filters;
		return this;
	}

	/**
	 * 添加用于扩充指定的类或接口的混合注解.
	 * 
	 * @param target 类(或接口), 其注解将被覆盖
	 * @param mixinSource 类(或接口), 其注解将被"添加"到目标的注解中
	 */
	public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
		if (mixinSource != null) {
			this.mixIns.put(target, mixinSource);
		}
		return this;
	}

	/**
	 * 添加用于扩充指定的类或接口的混合注解.
	 * 
	 * @param mixIns 具有目标类(或接口)的条目的Map,
	 * 要覆盖其注解的目标类作为键, 其注解将被"添加"到目标注解的混合类(或接口)作为值.
	 */
	public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
		if (mixIns != null) {
			this.mixIns.putAll(mixIns);
		}
		return this;
	}

	/**
	 * 配置自定义序列化器. 每个序列化器都注册了{@link JsonSerializer#handledType()}返回的类型, 该类型不能是{@code null}.
	 */
	public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
		if (serializers != null) {
			for (JsonSerializer<?> serializer : serializers) {
				Class<?> handledType = serializer.handledType();
				if (handledType == null || handledType == Object.class) {
					throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
				}
				this.serializers.put(serializer.handledType(), serializer);
			}
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器.
	 */
	public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
		if (serializer != null) {
			this.serializers.put(type, serializer);
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义序列化器.
	 */
	public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
		if (serializers != null) {
			this.serializers.putAll(serializers);
		}
		return this;
	}

	/**
	 * 配置自定义反序列化器.
	 * 每个反序列化器都为{@link JsonDeserializer#handledType()}返回的类型注册, 该类型不能是{@code null}.
	 */
	public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
		if (deserializers != null) {
			for (JsonDeserializer<?> deserializer : deserializers) {
				Class<?> handledType = deserializer.handledType();
				if (handledType == null || handledType == Object.class) {
					throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
				}
				this.deserializers.put(deserializer.handledType(), deserializer);
			}
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器.
	 */
	public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
		if (deserializer != null) {
			this.deserializers.put(type, deserializer);
		}
		return this;
	}

	/**
	 * 为给定类型配置自定义反序列化器.
	 */
	public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
		if (deserializers != null) {
			this.deserializers.putAll(deserializers);
		}
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_FIELDS} option.
	 */
	public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
		this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#AUTO_DETECT_SETTERS}/
	 * {@link MapperFeature#AUTO_DETECT_GETTERS}/{@link MapperFeature#AUTO_DETECT_IS_GETTERS} options.
	 */
	public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
		this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
		this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
		return this;
	}

	/**
	 * Shortcut for {@link MapperFeature#DEFAULT_VIEW_INCLUSION} option.
	 */
	public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
		this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
		return this;
	}

	/**
	 * Shortcut for {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} option.
	 */
	public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
		this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
		return this;
	}

	/**
	 * Shortcut for {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} option.
	 */
	public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
		this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
		return this;
	}

	/**
	 * Shortcut for {@link SerializationFeature#INDENT_OUTPUT} option.
	 */
	public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
		this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
		return this;
	}

	/**
	 * 定义默认情况下是否将包装器用于索引的 (List, array)属性 (仅适用于{@link XmlMapper}).
	 */
	public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
		this.defaultUseWrapper = defaultUseWrapper;
		return this;
	}

	/**
	 * 指定要启用的功能.
	 */
	public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
		if (featuresToEnable != null) {
			for (Object feature : featuresToEnable) {
				this.features.put(feature, Boolean.TRUE);
			}
		}
		return this;
	}

	/**
	 * 指定要禁用的功能.
	 */
	public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
		if (featuresToDisable != null) {
			for (Object feature : featuresToDisable) {
				this.features.put(feature, Boolean.FALSE);
			}
		}
		return this;
	}

	/**
	 * 指定要在{@link ObjectMapper}中注册的一个或多个模块.
	 * <p>Note: 如果设置了这个, 那么找不到模块就不会发生 - 不是Jackson, 也不是Spring (see {@link #findModulesViaServiceLoader}).
	 * 因此, 在此处指定空列表将禁止任何类型的模块检测.
	 * <p>指定这个或{@link #modulesToInstall}, 而不是两者.
	 */
	public Jackson2ObjectMapperBuilder modules(Module... modules) {
		return modules(Arrays.asList(modules));
	}

	/**
	 * 设置要在{@link ObjectMapper}中注册的完整模块列表.
	 * <p>Note: 如果设置了这个, 那么找不到模块就不会发生 - 不是Jackson, 也不是Spring (see {@link #findModulesViaServiceLoader}).
	 * 因此, 在此处指定空列表将禁止任何类型的模块检测.
	 * <p>指定这个或{@link #modulesToInstall}, 而不是两者.
	 */
	public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
		this.modules = new LinkedList<Module>(modules);
		this.findModulesViaServiceLoader = false;
		this.findWellKnownModules = false;
		return this;
	}

	/**
	 * 指定要在{@link ObjectMapper}中注册的一个或多个模块.
	 * <p>这里指定的模块将在Spring自动检测JSR-310和Joda-Time之后注册,
	 * 或者Jackson查找模块之后 (see {@link #findModulesViaServiceLoader}), 允许最终覆盖其配置.
	 * <p>指定这个或{@link #modules}, 而不是两者.
	 */
	public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
		this.modules = Arrays.asList(modules);
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * 指定要在{@link ObjectMapper}中注册的一个或多个模块.
	 * <p>这里指定的模块将在Spring自动检测JSR-310和Joda-Time之后注册,
	 * 或者Jackson查找模块之后 (see {@link #findModulesViaServiceLoader}), 允许最终覆盖其配置.
	 * <p>指定这个或{@link #modules}, 而不是两者.
	 */
	@SuppressWarnings("unchecked")
	public Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
		this.moduleClasses = modules;
		this.findWellKnownModules = true;
		return this;
	}

	/**
	 * 设置是否让Jackson根据类路径中的META-INF元数据通过JDK ServiceLoader查找可用模块.
	 * 需要Jackson 2.2或更高版本.
	 * <p>如果未设置此模​​式, Spring的Jackson2ObjectMapperBuilder本身将尝试在类路径上
	 * 查找JSR-310和Joda-Time支持模块 - 前提是Java 8和Joda-Time本身都可用.
	 */
	public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
		this.findModulesViaServiceLoader = findModules;
		return this;
	}

	/**
	 * 设置用于加载Jackson扩展模块的ClassLoader.
	 */
	public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
		this.moduleClassLoader = moduleClassLoader;
		return this;
	}

	/**
	 * 定制Jackson处理器的构造
	 * ({@link JsonSerializer}, {@link JsonDeserializer},
	 * {@link KeyDeserializer}, {@code TypeResolverBuilder} and {@code TypeIdResolver}).
	 */
	public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
		this.handlerInstantiator = handlerInstantiator;
		return this;
	}

	/**
	 * 设置Spring {@link ApplicationContext}以自动装配Jackson处理器
	 * ({@link JsonSerializer}, {@link JsonDeserializer}, {@link KeyDeserializer},
	 * {@code TypeResolverBuilder} and {@code TypeIdResolver}).
	 */
	public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		return this;
	}


	/**
	 * 构建一个新的{@link ObjectMapper}实例.
	 * <p>每个构建操作都会生成一个独立的{@link ObjectMapper}实例.
	 * 可以修改构建器的设置, 然后使用后续构建操作, 然后根据最新设置生成新的{@link ObjectMapper}.
	 * 
	 * @return 新构建的ObjectMapper
	 */
	@SuppressWarnings("unchecked")
	public <T extends ObjectMapper> T build() {
		ObjectMapper mapper;
		if (this.createXmlMapper) {
			mapper = (this.defaultUseWrapper != null ?
					new XmlObjectMapperInitializer().create(this.defaultUseWrapper) :
					new XmlObjectMapperInitializer().create());
		}
		else {
			mapper = new ObjectMapper();
		}
		configure(mapper);
		return (T) mapper;
	}

	/**
	 * 使用此构建器的设置配置现有的{@link ObjectMapper}实例. 这可以应用于任意数量的{@code ObjectMappers}.
	 * 
	 * @param objectMapper 要配置的ObjectMapper
	 */
	public void configure(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");

		if (this.findModulesViaServiceLoader) {
			// Jackson 2.2+
			objectMapper.registerModules(ObjectMapper.findModules(this.moduleClassLoader));
		}
		else if (this.findWellKnownModules) {
			registerWellKnownModulesIfAvailable(objectMapper);
		}

		if (this.modules != null) {
			for (Module module : this.modules) {
				// Using Jackson 2.0+ registerModule method, not Jackson 2.2+ registerModules
				objectMapper.registerModule(module);
			}
		}
		if (this.moduleClasses != null) {
			for (Class<? extends Module> module : this.moduleClasses) {
				objectMapper.registerModule(BeanUtils.instantiate(module));
			}
		}

		if (this.dateFormat != null) {
			objectMapper.setDateFormat(this.dateFormat);
		}
		if (this.locale != null) {
			objectMapper.setLocale(this.locale);
		}
		if (this.timeZone != null) {
			objectMapper.setTimeZone(this.timeZone);
		}

		if (this.annotationIntrospector != null) {
			objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
		}
		if (this.propertyNamingStrategy != null) {
			objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
		}
		if (this.defaultTyping != null) {
			objectMapper.setDefaultTyping(this.defaultTyping);
		}
		if (this.serializationInclusion != null) {
			objectMapper.setSerializationInclusion(this.serializationInclusion);
		}

		if (this.filters != null) {
			objectMapper.setFilterProvider(this.filters);
		}

		for (Class<?> target : this.mixIns.keySet()) {
			objectMapper.addMixIn(target, this.mixIns.get(target));
		}

		if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
			SimpleModule module = new SimpleModule();
			addSerializers(module);
			addDeserializers(module);
			objectMapper.registerModule(module);
		}

		customizeDefaultFeatures(objectMapper);
		for (Object feature : this.features.keySet()) {
			configureFeature(objectMapper, feature, this.features.get(feature));
		}

		if (this.handlerInstantiator != null) {
			objectMapper.setHandlerInstantiator(this.handlerInstantiator);
		}
		else if (this.applicationContext != null) {
			objectMapper.setHandlerInstantiator(
					new SpringHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
		}
	}


	// 对此方法的任何更改也应该应用于spring-jms 和 spring-messaging MappingJackson2MessageConverter 默认构造函数
	private void customizeDefaultFeatures(ObjectMapper objectMapper) {
		if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
			configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		}
		if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
			configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addSerializers(SimpleModule module) {
		for (Class<?> type : this.serializers.keySet()) {
			module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) this.serializers.get(type));
		}
	}

	@SuppressWarnings("unchecked")
	private <T> void addDeserializers(SimpleModule module) {
		for (Class<?> type : this.deserializers.keySet()) {
			module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) this.deserializers.get(type));
		}
	}

	private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
		if (feature instanceof JsonParser.Feature) {
			objectMapper.configure((JsonParser.Feature) feature, enabled);
		}
		else if (feature instanceof JsonGenerator.Feature) {
			objectMapper.configure((JsonGenerator.Feature) feature, enabled);
		}
		else if (feature instanceof SerializationFeature) {
			objectMapper.configure((SerializationFeature) feature, enabled);
		}
		else if (feature instanceof DeserializationFeature) {
			objectMapper.configure((DeserializationFeature) feature, enabled);
		}
		else if (feature instanceof MapperFeature) {
			objectMapper.configure((MapperFeature) feature, enabled);
		}
		else {
			throw new FatalBeanException("Unknown feature class: " + feature.getClass().getName());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerWellKnownModulesIfAvailable(ObjectMapper objectMapper) {
		// Java 7 java.nio.file.Path class present?
		if (ClassUtils.isPresent("java.nio.file.Path", this.moduleClassLoader)) {
			try {
				Class<? extends Module> jdk7Module = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.jdk7.Jdk7Module", this.moduleClassLoader);
				objectMapper.registerModule(BeanUtils.instantiateClass(jdk7Module));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-jdk7 not available
			}
		}

		// Java 8 java.util.Optional class present?
		if (ClassUtils.isPresent("java.util.Optional", this.moduleClassLoader)) {
			try {
				Class<? extends Module> jdk8Module = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
				objectMapper.registerModule(BeanUtils.instantiateClass(jdk8Module));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-jdk8 not available
			}
		}

		// Java 8 java.time package present?
		if (ClassUtils.isPresent("java.time.LocalDate", this.moduleClassLoader)) {
			try {
				Class<? extends Module> javaTimeModule = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
				objectMapper.registerModule(BeanUtils.instantiateClass(javaTimeModule));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-jsr310 not available
			}
		}

		// Joda-Time present?
		if (ClassUtils.isPresent("org.joda.time.LocalDate", this.moduleClassLoader)) {
			try {
				Class<? extends Module> jodaModule = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", this.moduleClassLoader);
				objectMapper.registerModule(BeanUtils.instantiateClass(jodaModule));
			}
			catch (ClassNotFoundException ex) {
				// jackson-datatype-joda not available
			}
		}

		// Kotlin present?
		if (ClassUtils.isPresent("kotlin.Unit", this.moduleClassLoader)) {
			try {
				Class<? extends Module> kotlinModule = (Class<? extends Module>)
						ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", this.moduleClassLoader);
				objectMapper.registerModule(BeanUtils.instantiateClass(kotlinModule));
			}
			catch (ClassNotFoundException ex) {
				// jackson-module-kotlin not available
			}
		}
	}


	// Convenience factory methods

	/**
	 * 获取{@link Jackson2ObjectMapperBuilder}实例以构建常规JSON {@link ObjectMapper}实例.
	 */
	public static Jackson2ObjectMapperBuilder json() {
		return new Jackson2ObjectMapperBuilder();
	}

	/**
	 * 获取{@link Jackson2ObjectMapperBuilder}实例以构建{@link XmlMapper}实例.
	 */
	public static Jackson2ObjectMapperBuilder xml() {
		return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
	}


	private static class XmlObjectMapperInitializer {

		public ObjectMapper create() {
			return new XmlMapper(xmlInputFactory());
		}

		public ObjectMapper create(boolean defaultUseWrapper) {
			JacksonXmlModule module = new JacksonXmlModule();
			module.setDefaultUseWrapper(defaultUseWrapper);
			return new XmlMapper(new XmlFactory(xmlInputFactory()), module);
		}

		private static XMLInputFactory xmlInputFactory() {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
			return inputFactory;
		}

		private static final XMLResolver NO_OP_XML_RESOLVER = new XMLResolver() {
			@Override
			public Object resolveEntity(String publicID, String systemID, String base, String ns) {
				return StreamUtils.emptyInput();
			}
		};
	}
}
