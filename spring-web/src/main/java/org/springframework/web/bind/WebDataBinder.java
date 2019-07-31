package org.springframework.web.bind;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.CollectionFactory;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 特殊的{@link DataBinder}, 用于从Web请求参数到JavaBean对象的数据绑定.
 * 专为Web环境而设计, 但不依赖于Servlet API; 作为更具体的DataBinder变体的基类,
 * 例如{@link org.springframework.web.bind.ServletRequestDataBinder}.
 *
 * <p>包括对字段标记的支持, 这些标记解决了HTML复选框和select选项的常见问题:
 * 检测到某个字段是表单的一部分, 但没有生成请求参数, 因为它是空的.
 * 字段标记允许检测该状态, 并相应地重置对应的bean属性.
 * 对于不存在的参数，默认值可以指定字段的值, 而不是空.
 */
public class WebDataBinder extends DataBinder {

	/**
	 * 字段标记参数的默认前缀, 后跟字段名称:
	 * e.g. "_subscribeToNewsletter"用于字段"subscribeToNewsletter".
	 * <p>这样的标记参数表示该字段是可见的, 即以导致提交的形式存在.
	 * 如果未找到相应的字段值参数, 则将重置该字段.
	 * 在这种情况下, 字段标记参数的值无关紧要; 可以使用任意值.
	 * 这对HTML复选框和select选项特别有用.
	 */
	public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";

	/**
	 * 字段默认参数的默认前缀, 后跟字段名称:
	 * e.g. "!subscribeToNewsletter"用于字段"subscribeToNewsletter".
	 * <p>默认参数与字段标记的不同之处在于它提供默认值, 而不是空值.
	 */
	public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";

	private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;

	private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;

	private boolean bindEmptyMultipartFiles = true;


	/**
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 */
	public WebDataBinder(Object target) {
		super(target);
	}

	/**
	 * @param target 绑定到的目标对象 (如果绑定器仅用于转换普通参数值, 则为{@code null})
	 * @param objectName 目标对象的名称
	 */
	public WebDataBinder(Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 指定一个前缀, 该前缀可用于标记可能为空的字段的参数, 并将"prefix + field"作为名称.
	 * 存在这样的标记参数: 可以为其发送任何值, 例如"visible".
	 * 这对HTML复选框和select选项特别有用.
	 * <p>默认 "_", 用于"_FIELD"参数 (e.g. "_subscribeToNewsletter").
	 * 如果要完全关闭空字段检查, 将此设置为null.
	 * <p>HTML复选框仅在选中时发送一个值, 因此无法检测到以前选中的复选框刚被取消选中, 至少不能使用标准HTML方法.
	 * <p>解决此问题的一种方法是, 如果知道复选框已在表单中可见, 则查找复选框参数值, 如果未找到任何值, 则重置复选框.
	 * 在Spring web MVC中, 这通常发生在自定义的{@code onBind}实现中.
	 * <p>这个自动重置机制解决了这个缺陷, 前提是为每个复选框字段发送了一个标记参数,
	 * 如"_subscribeToNewsletter"用于"subscribeToNewsletter"字段.
	 * 由于在任何情况下都会发送标记参数, 因此数据绑定器可以检测空字段并自动重置其值.
	 */
	public void setFieldMarkerPrefix(String fieldMarkerPrefix) {
		this.fieldMarkerPrefix = fieldMarkerPrefix;
	}

	/**
	 * 返回标记为可能为空的字段的参数的前缀.
	 */
	public String getFieldMarkerPrefix() {
		return this.fieldMarkerPrefix;
	}

	/**
	 * 指定可用于指示默认值字段的参数的前缀, 其名称为"prefix + field".
	 * 未提供字段时使用默认字段的值.
	 * <p>默认 "!", 用于"!FIELD"参数 (e.g. "!subscribeToNewsletter").
	 * 如果要完全关闭字段默认值, 设置为null.
	 * <p>HTML复选框仅在选中时发送一个值, 因此无法检测到以前选中的复选框刚被取消选中, 至少不能使用标准HTML方法.
	 * 当复选框表示非布尔值时, 默认字段特别有用.
	 * <p>默认参数的存在优先于给定字段的字段标记的行为.
	 */
	public void setFieldDefaultPrefix(String fieldDefaultPrefix) {
		this.fieldDefaultPrefix = fieldDefaultPrefix;
	}

	/**
	 * 返回标记默认字段的参数的前缀.
	 */
	public String getFieldDefaultPrefix() {
		return this.fieldDefaultPrefix;
	}

	/**
	 * 设置是否绑定空的MultipartFile参数. 默认"true".
	 * <p>如果要在用户重新提交表单时保留已绑定的MultipartFile, 而不选择其他文件, 将其关闭.
	 * 否则, 已绑定的MultipartFile将被空的MultipartFile保存器替换.
	 */
	public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
		this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
	}

	/**
	 * 返回是否绑定空的MultipartFile参数.
	 */
	public boolean isBindEmptyMultipartFiles() {
		return this.bindEmptyMultipartFiles;
	}


	/**
	 * 此实现在委托给超类绑定进程之前执行字段默认值和标记检查.
	 */
	@Override
	protected void doBind(MutablePropertyValues mpvs) {
		checkFieldDefaults(mpvs);
		checkFieldMarkers(mpvs);
		super.doBind(mpvs);
	}

	/**
	 * 检查字段默认值的给定属性值, i.e. 对于以字段默认前缀开头的字段.
	 * <p>字段默认值的存在表示, 如果该字段不存在, 则应使用指定的值.
	 * 
	 * @param mpvs the property values to be bound (can be modified)
	 */
	protected void checkFieldDefaults(MutablePropertyValues mpvs) {
		String fieldDefaultPrefix = getFieldDefaultPrefix();
		if (fieldDefaultPrefix != null) {
			PropertyValue[] pvArray = mpvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getName().startsWith(fieldDefaultPrefix)) {
					String field = pv.getName().substring(fieldDefaultPrefix.length());
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						mpvs.add(field, pv.getValue());
					}
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 检查字段标记的给定属性值, i.e. 对于以字段标记前缀开头的字段.
	 * <p>字段标记的存在表明指定的字段存在于表单中.
	 * 如果属性值不包含相应的字段值, 则该字段将被视为空, 并将被适当地重置.
	 * 
	 * @param mpvs 要绑定的属性值 (可以被修改)
	 */
	protected void checkFieldMarkers(MutablePropertyValues mpvs) {
		String fieldMarkerPrefix = getFieldMarkerPrefix();
		if (fieldMarkerPrefix != null) {
			PropertyValue[] pvArray = mpvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getName().startsWith(fieldMarkerPrefix)) {
					String field = pv.getName().substring(fieldMarkerPrefix.length());
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						Class<?> fieldType = getPropertyAccessor().getPropertyType(field);
						mpvs.add(field, getEmptyValue(field, fieldType));
					}
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 确定指定字段的空值.
	 * <p>默认实现返回:
	 * <ul>
	 * <li>boolean字段默认为{@code Boolean.FALSE}
	 * <li>数组类型默认为空数组
	 * <li>Collection类型默认为Collection实现
	 * <li>Map类型默认为 Map实现
	 * <li>否则默认为{@code null}
	 * </ul>
	 * 
	 * @param field 字段名称
	 * @param fieldType 字段类型
	 * 
	 * @return 空值 (大多数字段为: null)
	 */
	protected Object getEmptyValue(String field, Class<?> fieldType) {
		if (fieldType != null) {
			try {
				if (boolean.class == fieldType || Boolean.class == fieldType) {
					// Special handling of boolean property.
					return Boolean.FALSE;
				}
				else if (fieldType.isArray()) {
					// Special handling of array property.
					return Array.newInstance(fieldType.getComponentType(), 0);
				}
				else if (Collection.class.isAssignableFrom(fieldType)) {
					return CollectionFactory.createCollection(fieldType, 0);
				}
				else if (Map.class.isAssignableFrom(fieldType)) {
					return CollectionFactory.createMap(fieldType, 0);
				}
			}
			catch (IllegalArgumentException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to create default value - falling back to null: " + ex.getMessage());
				}
			}
		}
		// Default value: null.
		return null;
	}

	/**
	 * 绑定给定请求中包含的所有multipart文件 (如果是multipart请求). 由子类调用.
	 * <p>如果Multipart文件不为空, 或者我们也配置为绑定空的Multipart文件, 则只会将其添加到属性值中.
	 * 
	 * @param multipartFiles 字段名称字符串到MultipartFile对象的映射
	 * @param mpvs 要绑定的属性值 (可以修改)
	 */
	protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, MutablePropertyValues mpvs) {
		for (Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
			String key = entry.getKey();
			List<MultipartFile> values = entry.getValue();
			if (values.size() == 1) {
				MultipartFile value = values.get(0);
				if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
					mpvs.add(key, value);
				}
			}
			else {
				mpvs.add(key, values);
			}
		}
	}
}
