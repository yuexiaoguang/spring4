package org.springframework.jdbc.core.namedparam;

import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * JdbcDaoSupport的扩展, 它也公开了NamedParameterJdbcTemplate.
 */
public class NamedParameterJdbcDaoSupport extends JdbcDaoSupport {

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


	/**
	 * 基于配置的JdbcTemplate创建NamedParameterJdbcTemplate
	 */
	@Override
	protected void initTemplateConfig() {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate());
	}

	/**
	 * 返回包装已配置的JdbcTemplate的NamedParameterJdbcTemplate.
	 */
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
	  return namedParameterJdbcTemplate;
	}
}
