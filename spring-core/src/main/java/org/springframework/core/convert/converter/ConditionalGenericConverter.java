package org.springframework.core.convert.converter;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 可以根据{@code source}和{@code target} {@link TypeDescriptor}的属性有条件地执行的{@link GenericConverter}.
 *
 * <p>See {@link ConditionalConverter} for details.
 */
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {

}
