package org.springframework.validation;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessException;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormatterPropertyEditorAdapter;
import org.springframework.lang.UsesJava8;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * 允许将属性值设置到目标对象上的Binder, 包括对验证和绑定结果分析的支持.
 * 可以通过指定允许的字段, 必填字段, 自定义编辑器等来自定义绑定过程.
 *
 * <p>请注意, 如果未设置允许的字段数组, 则可能存在安全隐患.
 * 例如, 在HTTP表单POST数据的情况下, 恶意客户端可以通过为表单上不存在的字段或属性提供值来尝试破坏应用程序.
 * 在某些情况下, 这可能导致在命令对象<i>或其嵌套对象</i>上设置非法数据.
 * 因此, <b>强烈建议在DataBinder上指定{@link #setAllowedFields allowedFields}属性</b>.
 *
 * <p>可以通过{@link BindingResult}接口检查绑定结果, 扩展{@link Errors}接口: 查看{@link #getBindingResult()}方法.
 * 缺少字段和属性访问异常将转换为{@link FieldError FieldErrors}, 使用以下错误代码在Errors实例中收集:
 *
 * <ul>
 * <li>缺少字段错误: "required"
 * <li>类型不匹配错误: "typeMismatch"
 * <li>方法调用错误: "methodInvocation"
 * </ul>
 *
 * <p>默认情况下, 绑定错误通过{@link BindingErrorProcessor}策略解析, 处理缺少字段和属性访问异常:
 * 查看{@link #setBindingErrorProcessor}方法.
 * 可以根据需要覆盖默认策略, 例如生成不同的错误代码.
 *
 * <p>之后可以添加自定义验证错误.
 * 通常希望将此类错误代码解析为正确的用户可见错误消息;
 * 这可以通过{@link org.springframework.context.MessageSource}解析每个错误来实现, 它可以解析{@link ObjectError}/{@link FieldError}
 * 通过它的
 * {@link org.springframework.context.MessageSource#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)}方法.
 * 可以通过{@link MessageCodesResolver}策略自定义消息代码列表:
 * 查看{@link #setMessageCodesResolver}方法. {@link DefaultMessageCodesResolver}的javadoc说明了默认解析规则的详细信息.
 *
 * <p>此通用数据绑定器可用于任何类型的环境.
 * 它通常由Spring Web MVC控制器使用,
 * 通过特定于Web的子类 {@link org.springframework.web.bind.ServletRequestDataBinder}
 * 和{@link org.springframework.web.portlet.bind.PortletRequestDataBinder}.
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {

	/** 用于绑定的默认对象名称: "target" */
	public static final String DEFAULT_OBJECT_NAME = "target";

	/** 数组和集合自动增长的默认限制: 256 */
	public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;


	/**
	 * 将创建许多DataBinder实例: Let's use a static logger.
	 */
	protected static final Log logger = LogFactory.getLog(DataBinder.class);

	private static Class<?> javaUtilOptionalClass = null;

	static {
		try {
			javaUtilOptionalClass =
					ClassUtils.forName("java.util.Optional", DataBinder.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			// Java 8 not available - Optional references simply not supported then.
		}
	}


	private final Object target;

	private final String objectName;

	private AbstractPropertyBindingResult bindingResult;

	private SimpleTypeConverter typeConverter;

	private boolean ignoreUnknownFields = true;

	private boolean ignoreInvalidFields = false;

	private boolean autoGrowNestedPaths = true;

	private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

	private String[] allowedFields;

	private String[] disallowedFields;

	private String[] requiredFields;

	private ConversionService conversionService;

	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

	private final List<Validator> validators = new ArrayList<Validator>();


	/**
	 * 使用默认的对象名称.
	 * 
	 * @param target 要绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 */
	public DataBinder(Object target) {
		this(target, DEFAULT_OBJECT_NAME);
	}

	/**
	 * @param target 要绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的名称
	 */
	public DataBinder(Object target, String objectName) {
		if (target != null && target.getClass() == javaUtilOptionalClass) {
			this.target = OptionalUnwrapper.unwrap(target);
		}
		else {
			this.target = target;
		}
		this.objectName = objectName;
	}


	/**
	 * 返回包装的目标对象.
	 */
	public Object getTarget() {
		return this.target;
	}

	/**
	 * 返回绑定对象的名称.
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * 设置此绑定器是否应尝试"自动增长"包含null值的嵌套路径.
	 * <p>如果为"true", 则将使用默认对象值填充null路径位置, 并遍历而不是导致异常.
	 * 此标志还允许在访问越界索引时, 自动增长集合元素.
	 * <p>标准DataBinder上的默认值为"true". 请注意, 自Spring 4.1以来, 此属性支持bean属性访问 (DataBinder的默认模式)和字段访问.
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * 返回是否已激活嵌套路径的"自动增长".
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * 指定数组和集合自动增长的限制.
	 * <p>默认 256, 在大索引的情况下防止OutOfMemoryErrors.
	 * 如果自动增长需求非常高, 请提高此限制.
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * 返回数组和集合自动增长的当前限制.
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * 初始化此DataBinder的标准JavaBean属性访问.
	 * <p>这是默认值; 显式调用只会导致实时的初始化.
	 */
	public void initBeanPropertyAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
		this.bindingResult = createBeanPropertyBindingResult();
	}

	/**
	 * 使用标准JavaBean属性访问, 创建{@link AbstractPropertyBindingResult}实例.
	 */
	protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 初始化此DataBinder的直接字段访问, 作为默认bean属性访问的替代方法.
	 */
	public void initDirectFieldAccess() {
		Assert.state(this.bindingResult == null,
				"DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
		this.bindingResult = createDirectFieldBindingResult();
	}

	/**
	 * 使用直接字段访问创建{@link AbstractPropertyBindingResult}实例.
	 */
	protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
		DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(),
				getObjectName(), isAutoGrowNestedPaths());

		if (this.conversionService != null) {
			result.initConversion(this.conversionService);
		}
		if (this.messageCodesResolver != null) {
			result.setMessageCodesResolver(this.messageCodesResolver);
		}

		return result;
	}

	/**
	 * 返回此DataBinder持有的内部BindingResult.
	 */
	protected AbstractPropertyBindingResult getInternalBindingResult() {
		if (this.bindingResult == null) {
			initBeanPropertyAccess();
		}
		return this.bindingResult;
	}

	/**
	 * 返回此绑定器的BindingResult的底层PropertyAccessor.
	 */
	protected ConfigurablePropertyAccessor getPropertyAccessor() {
		return getInternalBindingResult().getPropertyAccessor();
	}

	/**
	 * 返回此binder的底层SimpleTypeConverter.
	 */
	protected SimpleTypeConverter getSimpleTypeConverter() {
		if (this.typeConverter == null) {
			this.typeConverter = new SimpleTypeConverter();
			if (this.conversionService != null) {
				this.typeConverter.setConversionService(this.conversionService);
			}
		}
		return this.typeConverter;
	}

	/**
	 * 返回此绑定器的BindingResult的底层TypeConverter.
	 */
	protected PropertyEditorRegistry getPropertyEditorRegistry() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 返回此绑定器的BindingResult的底层TypeConverter.
	 */
	protected TypeConverter getTypeConverter() {
		if (getTarget() != null) {
			return getInternalBindingResult().getPropertyAccessor();
		}
		else {
			return getSimpleTypeConverter();
		}
	}

	/**
	 * 返回此DataBinder创建的BindingResult实例.
	 * 允许在绑定操作之后方便地访问绑定结果.
	 * 
	 * @return BindingResult实例, 被视为BindingResult或Errors实例 (Errors 是BindingResult的超级接口)
	 */
	public BindingResult getBindingResult() {
		return getInternalBindingResult();
	}


	/**
	 * 设置是否忽略未知字段, 即是否忽略目标对象中没有相应字段的绑定参数.
	 * <p>默认"true". 将其关闭, 以强制所有绑定参数必须在目标对象中具有匹配的字段.
	 * <p>请注意，此设置仅适用于此DataBinder上的<i>绑定</i>操作,
	 * 而不适用于通过其{@link #getBindingResult() BindingResult} <i>检索</i>值.
	 */
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * 返回绑定时是否忽略未知字段.
	 */
	public boolean isIgnoreUnknownFields() {
		return this.ignoreUnknownFields;
	}

	/**
	 * 设置是否忽略无效字段, 即是否忽略目标对象中具有不可访问的相应字段的绑定参数 (例如, 由于嵌套路径中的null值).
	 * <p>默认"false". 启用此选项, 可忽略目标对象图的不存在的部分中嵌套对象的绑定参数.
	 * <p>请注意，此设置仅适用于此DataBinder上的<i>绑定</i>操作,
	 * 而不适用于通过其{@link #getBindingResult() BindingResult} <i>检索</i>值.
	 */
	public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
		this.ignoreInvalidFields = ignoreInvalidFields;
	}

	/**
	 * 返回绑定时是否忽略无效字段.
	 */
	public boolean isIgnoreInvalidFields() {
		return this.ignoreInvalidFields;
	}

	/**
	 * 注册应该允许绑定的字段.
	 * 默认是所有字段. 例如, 限制此操作以避免恶意用户在绑定HTTP请求参数时, 进行不希望的修改.
	 * <p>支持"xxx*", "*xxx", "*xxx*"模式. 通过覆盖{@code isAllowed}方法可以实现更复杂的匹配.
	 * <p>或者, 指定<i>disallowed</i>字段列表.
	 * 
	 * @param allowedFields 字段名称
	 */
	public void setAllowedFields(String... allowedFields) {
		this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
	}

	/**
	 * 返回应该允许绑定的字段.
	 * 
	 * @return 字段名称
	 */
	public String[] getAllowedFields() {
		return this.allowedFields;
	}

	/**
	 * 注册<i>不</i>允许绑定的字段.
	 * 默认无. 例如, 将字段标记为不允许, 以避免恶意用户在绑定HTTP请求参数时进行不希望的修改.
	 * <p>支持"xxx*", "*xxx", "*xxx*"模式. 通过覆盖{@code isAllowed}方法可以实现更复杂的匹配.
	 * <p>或者, 指定<i>allowed</i>字段列表.
	 * 
	 * @param disallowedFields 字段名称
	 */
	public void setDisallowedFields(String... disallowedFields) {
		this.disallowedFields = PropertyAccessorUtils.canonicalPropertyNames(disallowedFields);
	}

	/**
	 * 返回<i>不</i>允许绑定的字段.
	 * 
	 * @return 字段名称
	 */
	public String[] getDisallowedFields() {
		return this.disallowedFields;
	}

	/**
	 * 注册每个绑定过程所需的字段.
	 * <p>如果传入属性值列表中未包含其中一个指定字段, 则将创建相应的"缺少字段"错误, 错误代码为"required" (由默认绑定错误处理器).
	 * 
	 * @param requiredFields 字段名称
	 */
	public void setRequiredFields(String... requiredFields) {
		this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
		if (logger.isDebugEnabled()) {
			logger.debug("DataBinder requires binding of required fields [" +
					StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
		}
	}

	/**
	 * 返回每个绑定过程所需的字段.
	 * 
	 * @return 字段名称
	 */
	public String[] getRequiredFields() {
		return this.requiredFields;
	}

	/**
	 * 设置在将属性编辑器应用于字段的新值时, 是否提取旧字段值.
	 * <p>默认"true", 将以前的字段值公开给自定义编辑器.
	 * 设置为"false"以避免getter引起的副作用.
	 * 
	 * @deprecated as of Spring 4.3.5, in favor of customizing this in
	 * {@link #createBeanPropertyBindingResult()} or
	 * {@link #createDirectFieldBindingResult()} itself
	 */
	@Deprecated
	public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
		getPropertyAccessor().setExtractOldValueForEditor(extractOldValueForEditor);
	}

	/**
	 * 设置用于将错误解析为消息代码的策略.
	 * 将给定策略应用于底层错误持有者.
	 * <p>默认是DefaultMessageCodesResolver.
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
		this.messageCodesResolver = messageCodesResolver;
		if (this.bindingResult != null && messageCodesResolver != null) {
			this.bindingResult.setMessageCodesResolver(messageCodesResolver);
		}
	}

	/**
	 * 设置用于处理绑定错误的策略, 即必需的字段错误和{@code PropertyAccessException}s.
	 * <p>默认是DefaultBindingErrorProcessor.
	 */
	public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * 返回用于处理绑定错误的策略.
	 */
	public BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * 设置在每个绑定步骤后应用的Validator.
	 */
	public void setValidator(Validator validator) {
		assertValidators(validator);
		this.validators.clear();
		this.validators.add(validator);
	}

	private void assertValidators(Validator... validators) {
		Assert.notNull(validators, "Validators required");
		Object target = getTarget();
		for (Validator validator : validators) {
			if (validator != null && (target != null && !validator.supports(target.getClass()))) {
				throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
			}
		}
	}

	/**
	 * 添加在每个绑定步骤后应用的Validator.
	 */
	public void addValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 替换在每个绑定步骤后要应用的Validator.
	 */
	public void replaceValidators(Validator... validators) {
		assertValidators(validators);
		this.validators.clear();
		this.validators.addAll(Arrays.asList(validators));
	}

	/**
	 * 返回在每个绑定步骤后要应用的主要Validator.
	 */
	public Validator getValidator() {
		return (this.validators.size() > 0 ? this.validators.get(0) : null);
	}

	/**
	 * 返回在数据绑定后要应用的Validator.
	 */
	public List<Validator> getValidators() {
		return Collections.unmodifiableList(this.validators);
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyEditorRegistry/TypeConverter interface
	//---------------------------------------------------------------------

	/**
	 * 指定用于转换属性值的Spring 3.0 ConversionService, 作为JavaBeans PropertyEditors的替代方法.
	 */
	public void setConversionService(ConversionService conversionService) {
		Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
		this.conversionService = conversionService;
		if (this.bindingResult != null && conversionService != null) {
			this.bindingResult.initConversion(conversionService);
		}
	}

	/**
	 * 返回关联的ConversionService.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * 添加自定义格式化器, 将其应用于与{@link Formatter}声明的类型匹配的所有字段.
	 * <p>在封面下注册相应的{@link PropertyEditor}适配器.
	 * 
	 * @param formatter 要添加的格式化器, 通常为特定类型声明
	 */
	public void addCustomFormatter(Formatter<?> formatter) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
	}

	/**
	 * 为{@link Formatter}类中指定的字段类型添加自定义格式化器, 仅将其应用于指定字段, 或者应用于所有字段.
	 * <p>在封面下注册相应的{@link PropertyEditor}适配器.
	 * 
	 * @param formatter 要添加的格式化器, 通常为特定类型声明
	 * @param fields 要应用格式化器的字段; 或空, 如果要应用于全部字段
	 */
	public void addCustomFormatter(Formatter<?> formatter, String... fields) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		Class<?> fieldType = adapter.getFieldType();
		if (ObjectUtils.isEmpty(fields)) {
			getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
		}
		else {
			for (String field : fields) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
			}
		}
	}

	/**
	 * 添加自定义格式化器, 仅将其应用于指定的字段类型, 或者应用于与{@link Formatter}声明的类型匹配的所有字段.
	 * <p>在封面下注册相应的{@link PropertyEditor}适配器.
	 * 
	 * @param formatter 要添加的格式化器 (如果字段类型明确指定为参数, 则不需要一般声明字段类型)
	 * @param fieldTypes 要应用格式化器的字段类型; 或无, 如果要从给定的{@link Formatter}实现类派生
	 */
	public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
		FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
		if (ObjectUtils.isEmpty(fieldTypes)) {
			getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
		}
		else {
			for (Class<?> fieldType : fieldTypes) {
				getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
			}
		}
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
	}

	@Override
	public void registerCustomEditor(Class<?> requiredType, String field, PropertyEditor propertyEditor) {
		getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
	}

	@Override
	public PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath) {
		return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
	}

	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
		return getTypeConverter().convertIfNecessary(value, requiredType);
	}

	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
			throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
	}

	@Override
	public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
			throws TypeMismatchException {

		return getTypeConverter().convertIfNecessary(value, requiredType, field);
	}


	/**
	 * 将给定的属性值绑定到此绑定器的目标.
	 * <p>此调用可以创建字段错误, 表示基本绑定错误, 如必填字段 (code "required"), 或者值和bean属性之间的类型不匹配 (code "typeMismatch").
	 * <p>请注意, 给定的PropertyValues应该是一次性实例:
	 * 为了提高效率, 如果它实现MutablePropertyValues接口, 它将被修改为仅包含允许的字段;
	 * 否则,将为此目的创建内部可变副本.
	 * 如果希望原始实例在任何情况下保持不变, 请传递PropertyValues的副本.
	 * 
	 * @param pvs property values to bind
	 */
	public void bind(PropertyValues pvs) {
		MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues) ?
				(MutablePropertyValues) pvs : new MutablePropertyValues(pvs);
		doBind(mpvs);
	}

	/**
	 * 绑定过程的实际实现, 使用传入的MutablePropertyValues实例.
	 * 
	 * @param mpvs 要绑定的属性值
	 */
	protected void doBind(MutablePropertyValues mpvs) {
		checkAllowedFields(mpvs);
		checkRequiredFields(mpvs);
		applyPropertyValues(mpvs);
	}

	/**
	 * 根据允许的字段检查给定的属性值, 删除不允许的字段的值.
	 * 
	 * @param mpvs 要绑定的属性值(可以修改)
	 */
	protected void checkAllowedFields(MutablePropertyValues mpvs) {
		PropertyValue[] pvs = mpvs.getPropertyValues();
		for (PropertyValue pv : pvs) {
			String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
			if (!isAllowed(field)) {
				mpvs.removePropertyValue(pv);
				getBindingResult().recordSuppressedField(field);
				if (logger.isDebugEnabled()) {
					logger.debug("Field [" + field + "] has been removed from PropertyValues " +
							"and will not be bound, because it has not been found in the list of allowed fields");
				}
			}
		}
	}

	/**
	 * 是否允许给定字段进行绑定.
	 * 为每个传入的属性值调用.
	 * <p>默认实现在允许字段和不允许字段的指定列表中检查"xxx*", "*xxx", "*xxx*"匹配, 以及直接相等.
	 * 即使匹配允许的列表中的模式, 也不会接受与不允许的模式匹配的字段.
	 * <p>可以在子类中重写.
	 * 
	 * @param field 要检查的字段
	 * 
	 * @return 字段是否允许
	 */
	protected boolean isAllowed(String field) {
		String[] allowed = getAllowedFields();
		String[] disallowed = getDisallowedFields();
		return ((ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) &&
				(ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field)));
	}

	/**
	 * 根据所需字段检查给定的属性值, 并在适当时生成缺少字段错误.
	 * 
	 * @param mpvs 要绑定的属性值 (可以修改)
	 */
	protected void checkRequiredFields(MutablePropertyValues mpvs) {
		String[] requiredFields = getRequiredFields();
		if (!ObjectUtils.isEmpty(requiredFields)) {
			Map<String, PropertyValue> propertyValues = new HashMap<String, PropertyValue>();
			PropertyValue[] pvs = mpvs.getPropertyValues();
			for (PropertyValue pv : pvs) {
				String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
				propertyValues.put(canonicalName, pv);
			}
			for (String field : requiredFields) {
				PropertyValue pv = propertyValues.get(field);
				boolean empty = (pv == null || pv.getValue() == null);
				if (!empty) {
					if (pv.getValue() instanceof String) {
						empty = !StringUtils.hasText((String) pv.getValue());
					}
					else if (pv.getValue() instanceof String[]) {
						String[] values = (String[]) pv.getValue();
						empty = (values.length == 0 || !StringUtils.hasText(values[0]));
					}
				}
				if (empty) {
					// 使用绑定错误处理器创建FieldError.
					getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
					// 从要绑定的属性值中删除属性:
					// 它已经导致拒绝的值的字段错误.
					if (pv != null) {
						mpvs.removePropertyValue(pv);
						propertyValues.remove(field);
					}
				}
			}
		}
	}

	/**
	 * 将给定的属性值应用于目标对象.
	 * <p>默认实现将所有提供的属性值应用为bean属性值.
	 * 默认情况下, 将忽略未知字段.
	 * 
	 * @param mpvs 要绑定的属性值(可以修改)
	 */
	protected void applyPropertyValues(MutablePropertyValues mpvs) {
		try {
			// 将请求参数绑定到目标对象上.
			getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
		}
		catch (PropertyBatchUpdateException ex) {
			// 使用绑定错误处理器创建FieldErrors.
			for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
				getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
			}
		}
	}


	/**
	 * 调用指定的Validators.
	 */
	public void validate() {
		for (Validator validator : this.validators) {
			validator.validate(getTarget(), getBindingResult());
		}
	}

	/**
	 * 调用指定的Validators, 使用给定的验证提示.
	 * <p>Note: 实际目标Validator可能会忽略验证提示.
	 * 
	 * @param validationHints 要传递给{@link SmartValidator}的一个或多个提示对象
	 */
	public void validate(Object... validationHints) {
		for (Validator validator : getValidators()) {
			if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
				((SmartValidator) validator).validate(getTarget(), getBindingResult(), validationHints);
			}
			else if (validator != null) {
				validator.validate(getTarget(), getBindingResult());
			}
		}
	}

	/**
	 * 关闭此DataBinder, 如果遇到任何错误, 可能会导致抛出BindException.
	 * 
	 * @return 模型Map, 包含目标对象和Errors实例
	 * @throws BindException 如果绑定操作中有任何错误
	 */
	public Map<?, ?> close() throws BindException {
		if (getBindingResult().hasErrors()) {
			throw new BindException(getBindingResult());
		}
		return getBindingResult().getModel();
	}


	/**
	 * 内部类, 以避免对Java 8的硬依赖.
	 */
	@UsesJava8
	private static class OptionalUnwrapper {

		public static Object unwrap(Object optionalObject) {
			Optional<?> optional = (Optional<?>) optionalObject;
			if (!optional.isPresent()) {
				return null;
			}
			Object result = optional.get();
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}
	}

}
