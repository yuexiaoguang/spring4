package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.UsesJava8;

/**
 * 创建JSR-310 {@link java.time.format.DateTimeFormatter}的{@link FactoryBean}.
 * See the {@link DateTimeFormatterFactory base class} for configuration details.
 */
@UsesJava8
public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory
		implements FactoryBean<DateTimeFormatter>, InitializingBean {

	private DateTimeFormatter dateTimeFormatter;


	@Override
	public void afterPropertiesSet() {
		this.dateTimeFormatter = createDateTimeFormatter();
	}

	@Override
	public DateTimeFormatter getObject() {
		return this.dateTimeFormatter;
	}

	@Override
	public Class<?> getObjectType() {
		return DateTimeFormatter.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
