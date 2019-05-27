package org.springframework.jdbc.object;

/**
 * 具体实现可以在应用程序上下文中定义RDBMS存储过程, 而无需编写自定义Java实现类.
 * <p>
 * 此实现不提供用于调用的类型化方法, 因此执行必须使用通用{@link StoredProcedure#execute(java.util.Map)}或
 * {@link StoredProcedure#execute(org.springframework.jdbc.core.ParameterMapper)}方法之一.
 */
public class GenericStoredProcedure extends StoredProcedure {

}
