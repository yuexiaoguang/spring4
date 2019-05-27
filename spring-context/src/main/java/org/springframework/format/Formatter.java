package org.springframework.format;

/**
 * 格式化T类型的对象.
 * Formatter既是Printer又是对象类型的Parser.
 *
 * @param <T> 此Formatter格式化的对象类型
 */
public interface Formatter<T> extends Printer<T>, Parser<T> {

}
