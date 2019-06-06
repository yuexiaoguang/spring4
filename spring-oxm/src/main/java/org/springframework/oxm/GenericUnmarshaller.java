package org.springframework.oxm;

import java.lang.reflect.Type;

/**
 * {@link Unmarshaller}的子接口, 支持Java 5泛型.
 */
public interface GenericUnmarshaller extends Unmarshaller {

	/**
	 * 指示此编组器是否可以编组所提供的泛型类型的实例.
	 * 
	 * @param genericType 要确定是否可以编组的的类型
	 * 
	 * @return {@code true}如果这个编组器确实可以编组所提供类型的实例; 否则{@code false}
	 */
	boolean supports(Type genericType);

}
