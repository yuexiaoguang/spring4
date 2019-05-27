package org.springframework.jdbc.object;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * 用于RDBMS存储过程的对象抽象的超类.
 * 这个类是抽象的, 并且子类将提供一个类型化的调用方法, 该方法委托给提供的{@link #execute}方法.
 *
 * <p>继承的{@code sql}属性是RDBMS中存储过程的名称.
 */
public abstract class StoredProcedure extends SqlCall {

	protected StoredProcedure() {
	}

	/**
	 * 为存储过程创建新的对象包装器.
	 * 
	 * @param ds 在此对象的整个生命周期内使用, 以获取连接的DataSource
	 * @param name 数据库中存储过程的名称
	 */
	protected StoredProcedure(DataSource ds, String name) {
		setDataSource(ds);
		setSql(name);
	}

	/**
	 * 为存储过程创建新的对象包装器.
	 * 
	 * @param jdbcTemplate 包装DataSource的JdbcTemplate
	 * @param name 数据库中存储过程的名称
	 */
	protected StoredProcedure(JdbcTemplate jdbcTemplate, String name) {
		setJdbcTemplate(jdbcTemplate);
		setSql(name);
	}


	/**
	 * 默认情况下, StoredProcedure参数Map允许包含实际上不用作参数的其他条目.
	 */
	@Override
	protected boolean allowsUnusedParameters() {
		return true;
	}

	/**
	 * 声明一个参数.
	 * 声明为{@code SqlParameter}和{@code SqlInOutParameter}的参数将始终用于提供输入值.
	 * 除此之外, 任何声明为{@code SqlOutParameter}的参数(其中提供非空输入值)也将用作输入参数.
	 * <b>Note: 对declareParameter的调用必须按照它们在数据库的存储过程参数列表中出现的顺序进行.</b>
	 * 名称纯粹用于帮助映射.
	 * 
	 * @param param 参数对象
	 */
	@Override
	public void declareParameter(SqlParameter param) throws InvalidDataAccessApiUsageException {
		if (param.getName() == null) {
			throw new InvalidDataAccessApiUsageException("Parameters to stored procedures must have names as well as types");
		}
		super.declareParameter(param);
	}

	/**
	 * 使用提供的参数值执行存储过程.
	 * 这是一种方便的方法, 其中传入的参数值的顺序必须与声明的参数的顺序相匹配.
	 * 
	 * @param inParams 可变数量的输入参数. 输出参数不应包含在此映射中.
	 * 值为{@code null}是合法的, 这将使用存储过程的NULL参数产生正确的行为.
	 * 
	 * @return 输出参数的映射, 在参数声明中按名称键入.
	 * 输出参数将在此处显示, 并在调用存储过程后显示其值.
	 */
	public Map<String, Object> execute(Object... inParams) {
		Map<String, Object> paramsToUse = new HashMap<String, Object>();
		validateParameters(inParams);
		int i = 0;
		for (SqlParameter sqlParameter : getDeclaredParameters()) {
			if (sqlParameter.isInputValueProvided()) {
				if (i < inParams.length) {
					paramsToUse.put(sqlParameter.getName(), inParams[i++]);
				}
			}
		}
		return getJdbcTemplate().call(newCallableStatementCreator(paramsToUse), getDeclaredParameters());
	}

	/**
	 * 执行存储过程.
	 * 子类应定义一个强类型的execute方法 (具有有意义的名称), 该方法调用此方法, 填充输入映射并从输出映射中提取类型值.
	 * 子类execute方法通常将域对象作为参数并返回值.
	 * 或者, 可以返回 void.
	 * 
	 * @param inParams 输入参数的Map, 按名称键入, 如参数声明中所示.
	 * 输出参数不需要 (但可以) 包含在此Map中.
	 * Map条目为{@code null}是合法的, 这将使用存储过程的NULL参数生成正确的行为.
	 * 
	 * @return 输出参数的Map, 在参数声明中按名称键入.
	 * 输出参数将在此处显示, 并在调用存储过程后显示其值.
	 */
	public Map<String, Object> execute(Map<String, ?> inParams) throws DataAccessException {
		validateParameters(inParams.values().toArray());
		return getJdbcTemplate().call(newCallableStatementCreator(inParams), getDeclaredParameters());
	}

	/**
	 * 执行存储过程.
	 * 子类应该定义一个强类型的执行方法 (有一个有意义的名称)来调用这个方法, 传入一个将填充输入Map的ParameterMapper.
	 * 这允许映射数据库特定的功能, 因为ParameterMapper可以访问Connection对象.
	 * execute方法还负责从输出Map中提取类型化值.
	 * 子类execute方法通常将域对象作为参数并返回值.
	 * 或者, 可以返回 void.
	 * 
	 * @param inParamMapper 输入参数的Map, 按名称键入, 如参数声明中所示.
	 * 输出参数不需要 (但可以)包含在此Map中.
	 * Map条目为{@code null}是合法的, 这将使用存储过程的NULL参数生成正确的行为.
	 * 
	 * @return 输出参数的Map, 在参数声明中按名称键入.
	 * 输出参数将在此处显示, 并在调用存储过程后显示其值.
	 */
	public Map<String, Object> execute(ParameterMapper inParamMapper) throws DataAccessException {
		checkCompiled();
		return getJdbcTemplate().call(newCallableStatementCreator(inParamMapper), getDeclaredParameters());
	}
}
