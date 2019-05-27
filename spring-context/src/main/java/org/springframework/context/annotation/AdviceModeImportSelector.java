package org.springframework.context.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportSelector}实现的基类, 可根据注解中的{@link AdviceMode}值选择导入 (例如{@code @Enable*}注解).
 *
 * @param <A> 包含{@linkplain #getAdviceModeAttributeName() AdviceMode属性}的注解
 */
public abstract class AdviceModeImportSelector<A extends Annotation> implements ImportSelector {

	/**
	 * 默认的增强模式属性名称.
	 */
	public static final String DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME = "mode";


	/**
	 * 泛型类型{@code A}指定的注解的{@link AdviceMode}属性的名称.
	 * 默认是 {@value #DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME}, 但是子类可以覆盖以便自定义.
	 */
	protected String getAdviceModeAttributeName() {
		return DEFAULT_ADVICE_MODE_ATTRIBUTE_NAME;
	}

	/**
	 * 此实现解析了泛型元数据中的注解类型,
	 * 并验证 (a) 注解实际上存在于导入 {@code @Configuration}类中,
	 * 以及 (b) 给定注解具有{@link AdviceMode}类型的{@linkplain #getAdviceModeAttributeName()增强模式属性}.
	 * <p>然后调用{@link #selectImports(AdviceMode)}方法, 允许具体实现以安全方便的方式选择导入.
	 * 
	 * @throws IllegalArgumentException 如果预期的注解 {@code A} 不存在于导入{@code @Configuration}类中,
	 * 或者{@link #selectImports(AdviceMode)}返回{@code null}
	 */
	@Override
	public final String[] selectImports(AnnotationMetadata importingClassMetadata) {
		Class<?> annType = GenericTypeResolver.resolveTypeArgument(getClass(), AdviceModeImportSelector.class);
		AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
		if (attributes == null) {
			throw new IllegalArgumentException(String.format(
					"@%s is not present on importing class '%s' as expected",
					annType.getSimpleName(), importingClassMetadata.getClassName()));
		}

		AdviceMode adviceMode = attributes.getEnum(getAdviceModeAttributeName());
		String[] imports = selectImports(adviceMode);
		if (imports == null) {
			throw new IllegalArgumentException("Unknown AdviceMode: " + adviceMode);
		}
		return imports;
	}

	/**
	 * 根据给定的{@code AdviceMode}确定应导入哪些类.
	 * <p>从此方法返回{@code null}表示无法处理{@code AdviceMode}或未知, 并且应抛出{@code IllegalArgumentException}.
	 * 
	 * @param adviceMode 通过泛型指定的注解的{@linkplain #getAdviceModeAttributeName() 增强模式属性}的值.
	 * 
	 * @return 包含要导入的类的数组 (空数组; {@code null}如果给定的{@code AdviceMode}未知)
	 */
	protected abstract String[] selectImports(AdviceMode adviceMode);

}
