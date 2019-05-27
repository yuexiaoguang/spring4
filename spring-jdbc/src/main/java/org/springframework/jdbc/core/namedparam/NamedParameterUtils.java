package org.springframework.jdbc.core.namedparam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.util.Assert;

/**
 * 用于命名参数解析的工具方法.
 *
 * <p>仅供Spring的JDBC框架内部使用.
 */
public abstract class NamedParameterUtils {

	/**
	 * 注释或引用起始字符.
	 */
	private static final String[] START_SKIP = new String[] {"'", "\"", "--", "/*"};

	/**
	 * 相应的注释或引号结束字符.
	 */
	private static final String[] STOP_SKIP = new String[] {"'", "\"", "\n", "*/"};

	/**
	 * 参数分隔符, 表示SQL字符串中的参数名称已结束.
	 */
	private static final char[] PARAMETER_SEPARATORS =
			new char[] {'"', '\'', ':', '&', ',', ';', '(', ')', '|', '=', '+', '-', '*', '%', '/', '\\', '<', '>', '^'};


	//-------------------------------------------------------------------------
	// Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
	//-------------------------------------------------------------------------

	/**
	 * 解析SQL语句, 并找到任何占位符或命名参数.
	 * 命名参数替换JDBC占位符.
	 * 
	 * @param sql SQL语句
	 * 
	 * @return 解析的语句, 表示为ParsedSql实例
	 */
	public static ParsedSql parseSqlStatement(final String sql) {
		Assert.notNull(sql, "SQL must not be null");

		Set<String> namedParameters = new HashSet<String>();
		String sqlToUse = sql;
		List<ParameterHolder> parameterList = new ArrayList<ParameterHolder>();

		char[] statement = sql.toCharArray();
		int namedParameterCount = 0;
		int unnamedParameterCount = 0;
		int totalParameterCount = 0;

		int escapes = 0;
		int i = 0;
		while (i < statement.length) {
			int skipToPosition = i;
			while (i < statement.length) {
				skipToPosition = skipCommentsAndQuotes(statement, i);
				if (i == skipToPosition) {
					break;
				}
				else {
					i = skipToPosition;
				}
			}
			if (i >= statement.length) {
				break;
			}
			char c = statement[i];
			if (c == ':' || c == '&') {
				int j = i + 1;
				if (j < statement.length && statement[j] == ':' && c == ':') {
					// 应跳过Postgres风格的 "::" 投射操作符
					i = i + 2;
					continue;
				}
				String parameter = null;
				if (j < statement.length && c == ':' && statement[j] == '{') {
					// :{x} style parameter
					while (j < statement.length && statement[j] != '}') {
						j++;
						if (statement[j] == ':' || statement[j] == '{') {
							throw new InvalidDataAccessApiUsageException("Parameter name contains invalid character '" +
									statement[j] + "' at position " + i + " in statement: " + sql);
						}
					}
					if (j >= statement.length) {
						throw new InvalidDataAccessApiUsageException(
								"Non-terminated named parameter declaration at position " + i + " in statement: " + sql);
					}
					if (j - i > 2) {
						parameter = sql.substring(i + 2, j);
						namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j + 1, parameter);
					}
					j++;
				}
				else {
					while (j < statement.length && !isParameterSeparator(statement[j])) {
						j++;
					}
					if (j - i > 1) {
						parameter = sql.substring(i + 1, j);
						namedParameterCount = addNewNamedParameter(namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j, parameter);
					}
				}
				i = j - 1;
			}
			else {
				if (c == '\\') {
					int j = i + 1;
					if (j < statement.length && statement[j] == ':') {
						// escaped ":" should be skipped
						sqlToUse = sqlToUse.substring(0, i - escapes) + sqlToUse.substring(i - escapes + 1);
						escapes++;
						i = i + 2;
						continue;
					}
				}
				if (c == '?') {
					int j = i + 1;
					if (j < statement.length && (statement[j] == '?' || statement[j] == '|' || statement[j] == '&')) {
						// Postgres-style "??", "?|", "?&" operator should be skipped
						i = i + 2;
						continue;
					}
					unnamedParameterCount++;
					totalParameterCount++;
				}
			}
			i++;
		}
		ParsedSql parsedSql = new ParsedSql(sqlToUse);
		for (ParameterHolder ph : parameterList) {
			parsedSql.addNamedParameter(ph.getParameterName(), ph.getStartIndex(), ph.getEndIndex());
		}
		parsedSql.setNamedParameterCount(namedParameterCount);
		parsedSql.setUnnamedParameterCount(unnamedParameterCount);
		parsedSql.setTotalParameterCount(totalParameterCount);
		return parsedSql;
	}

	private static int addNamedParameter(
			List<ParameterHolder> parameterList, int totalParameterCount, int escapes, int i, int j, String parameter) {

		parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
		totalParameterCount++;
		return totalParameterCount;
	}

	private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
		if (!namedParameters.contains(parameter)) {
			namedParameters.add(parameter);
			namedParameterCount++;
		}
		return namedParameterCount;
	}

	/**
	 * 跳过SQL语句中的注释和引用名称
	 * 
	 * @param statement 包含SQL语句的字符数组
	 * @param position 语句的当前位置
	 * 
	 * @return 在跳过任何注释或引号后, 要处理的下一个位置
	 */
	private static int skipCommentsAndQuotes(char[] statement, int position) {
		for (int i = 0; i < START_SKIP.length; i++) {
			if (statement[position] == START_SKIP[i].charAt(0)) {
				boolean match = true;
				for (int j = 1; j < START_SKIP[i].length(); j++) {
					if (statement[position + j] != START_SKIP[i].charAt(j)) {
						match = false;
						break;
					}
				}
				if (match) {
					int offset = START_SKIP[i].length();
					for (int m = position + offset; m < statement.length; m++) {
						if (statement[m] == STOP_SKIP[i].charAt(0)) {
							boolean endMatch = true;
							int endPos = m;
							for (int n = 1; n < STOP_SKIP[i].length(); n++) {
								if (m + n >= statement.length) {
									// 最后注释没有正确关闭
									return statement.length;
								}
								if (statement[m + n] != STOP_SKIP[i].charAt(n)) {
									endMatch = false;
									break;
								}
								endPos = m + n;
							}
							if (endMatch) {
								// 找到字符序列结束注释或引用
								return endPos + 1;
							}
						}
					}
					// 未找到字符序列结束注释或引用
					return statement.length;
				}
			}
		}
		return position;
	}

	/**
	 * 解析SQL语句, 并找到任何占位符或命名参数.
	 * 命名参数替换JDBC占位符, 任何select列表都扩展为所需的占位符数.
	 * select列表可能包含一个对象数组, 在这种情况下, 占位符将被分组并用括号括起来.
	 * 这允许在SQL语句中使用"表达式列表": <br /><br />
	 * {@code select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))}
	 * <p>传入的参数值用于确定要用于select列表的占位符数.
	 * select列表应限制为100个或更少的元素.
	 * 数据库不保证支持更多元素, 并且严格依赖于供应商.
	 * 
	 * @param parsedSql SQL语句已解析的表示
	 * @param paramSource 命名参数的源
	 * 
	 * @return 已替换参数的SQL语句
	 */
	public static String substituteNamedParameters(ParsedSql parsedSql, SqlParameterSource paramSource) {
		String originalSql = parsedSql.getOriginalSql();
		List<String> paramNames = parsedSql.getParameterNames();
		if (paramNames.isEmpty()) {
			return originalSql;
		}
		StringBuilder actualSql = new StringBuilder(originalSql.length());
		int lastIndex = 0;
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			int[] indexes = parsedSql.getParameterIndexes(i);
			int startIndex = indexes[0];
			int endIndex = indexes[1];
			actualSql.append(originalSql, lastIndex, startIndex);
			if (paramSource != null && paramSource.hasValue(paramName)) {
				Object value = paramSource.getValue(paramName);
				if (value instanceof SqlParameterValue) {
					value = ((SqlParameterValue) value).getValue();
				}
				if (value instanceof Collection) {
					Iterator<?> entryIter = ((Collection<?>) value).iterator();
					int k = 0;
					while (entryIter.hasNext()) {
						if (k > 0) {
							actualSql.append(", ");
						}
						k++;
						Object entryItem = entryIter.next();
						if (entryItem instanceof Object[]) {
							Object[] expressionList = (Object[]) entryItem;
							actualSql.append('(');
							for (int m = 0; m < expressionList.length; m++) {
								if (m > 0) {
									actualSql.append(", ");
								}
								actualSql.append('?');
							}
							actualSql.append(')');
						}
						else {
							actualSql.append('?');
						}
					}
				}
				else {
					actualSql.append('?');
				}
			}
			else {
				actualSql.append('?');
			}
			lastIndex = endIndex;
		}
		actualSql.append(originalSql, lastIndex, originalSql.length());
		return actualSql.toString();
	}

	/**
	 * 将命名参数值的Map转换为相应的数组.
	 * 
	 * @param parsedSql 已解析的SQL语句
	 * @param paramSource 命名参数的源
	 * @param declaredParams 声明的SqlParameter对象的列表 (may be {@code null}).
	 * 如果指定, 参数元数据将以SqlParameterValue对象的形式构建到值数组中.
	 * 
	 * @return 值数组
	 */
	public static Object[] buildValueArray(
			ParsedSql parsedSql, SqlParameterSource paramSource, List<SqlParameter> declaredParams) {

		Object[] paramArray = new Object[parsedSql.getTotalParameterCount()];
		if (parsedSql.getNamedParameterCount() > 0 && parsedSql.getUnnamedParameterCount() > 0) {
			throw new InvalidDataAccessApiUsageException(
					"Not allowed to mix named and traditional ? placeholders. You have " +
					parsedSql.getNamedParameterCount() + " named parameter(s) and " +
					parsedSql.getUnnamedParameterCount() + " traditional placeholder(s) in statement: " +
					parsedSql.getOriginalSql());
		}
		List<String> paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			try {
				Object value = paramSource.getValue(paramName);
				SqlParameter param = findParameter(declaredParams, paramName, i);
				paramArray[i] = (param != null ? new SqlParameterValue(param, value) : value);
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(
						"No value supplied for the SQL parameter '" + paramName + "': " + ex.getMessage());
			}
		}
		return paramArray;
	}

	/**
	 * 在给定的声明参数列表中查找匹配的参数.
	 * 
	 * @param declaredParams 声明的SqlParameter对象
	 * @param paramName 所需参数的名称
	 * @param paramIndex 所需参数的索引
	 * 
	 * @return 声明的SqlParameter, 或{@code null}
	 */
	private static SqlParameter findParameter(List<SqlParameter> declaredParams, String paramName, int paramIndex) {
		if (declaredParams != null) {
			// 第一遍: 寻找命名参数匹配.
			for (SqlParameter declaredParam : declaredParams) {
				if (paramName.equals(declaredParam.getName())) {
					return declaredParam;
				}
			}
			// 第二遍: 寻找参数索引匹配.
			if (paramIndex < declaredParams.size()) {
				SqlParameter declaredParam = declaredParams.get(paramIndex);
				// 仅接受索引匹配的未命名参数.
				if (declaredParam.getName() == null) {
					return declaredParam;
				}
			}
		}
		return null;
	}

	/**
	 * 确定参数名称是否在当前位置结束, 即给定字符是否有资格作为分隔符.
	 */
	private static boolean isParameterSeparator(char c) {
		if (Character.isWhitespace(c)) {
			return true;
		}
		for (char separator : PARAMETER_SEPARATORS) {
			if (c == separator) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 将参数类型从SqlParameterSource转换为相应的int数组.
	 * 为了在JdbcTemplate上重用现有方法, 这是必需的.
	 * 根据解析的SQL语句信息, 任何命名参数类型都放置在Object数组中的正确位置.
	 * 
	 * @param parsedSql 已解析的SQL语句
	 * @param paramSource 命名参数的源
	 */
	public static int[] buildSqlTypeArray(ParsedSql parsedSql, SqlParameterSource paramSource) {
		int[] sqlTypes = new int[parsedSql.getTotalParameterCount()];
		List<String> paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			sqlTypes[i] = paramSource.getSqlType(paramName);
		}
		return sqlTypes;
	}

	/**
	 * 将参数声明从SqlParameterSource转换为相应的SqlParameter列表.
	 * 为了在JdbcTemplate上重用现有方法, 这是必需的.
	 * 命名参数的SqlParameter根据解析的SQL语句信息放置在结果列表中的正确位置.
	 * 
	 * @param parsedSql 已解析的SQL语句
	 * @param paramSource 命名参数的源
	 */
	public static List<SqlParameter> buildSqlParameterList(ParsedSql parsedSql, SqlParameterSource paramSource) {
		List<String> paramNames = parsedSql.getParameterNames();
		List<SqlParameter> params = new ArrayList<SqlParameter>(paramNames.size());
		for (String paramName : paramNames) {
			params.add(new SqlParameter(
					paramName, paramSource.getSqlType(paramName), paramSource.getTypeName(paramName)));
		}
		return params;
	}


	//-------------------------------------------------------------------------
	// Convenience methods operating on a plain SQL String
	//-------------------------------------------------------------------------

	/**
	 * 解析SQL语句, 并查找任何占位符或命名参数.
	 * 命名参数替换JDBC占位符.
	 * <p>这是{@link #parseSqlStatement(String)}的快捷版本,
	 * 与{@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}结合使用.
	 * 
	 * @param sql SQL语句
	 * 
	 * @return 实际(已解析的)的SQL语句
	 */
	public static String parseSqlStatementIntoString(String sql) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return substituteNamedParameters(parsedSql, null);
	}

	/**
	 * 解析SQL语句, 并查找任何占位符或命名参数.
	 * 命名参数替换JDBC占位符, 任何select列表都将扩展为所需的占位符数.
	 * <p>这是{@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}的快捷版本.
	 * 
	 * @param sql SQL语句
	 * @param paramSource 命名参数的源
	 * 
	 * @return 已替换参数的SQL语句
	 */
	public static String substituteNamedParameters(String sql, SqlParameterSource paramSource) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return substituteNamedParameters(parsedSql, paramSource);
	}

	/**
	 * 将命名参数值的Map转换为相应的数组.
	 * <p>这是{@link #buildValueArray(ParsedSql, SqlParameterSource, java.util.List)}的快捷版本.
	 * 
	 * @param sql SQL语句
	 * @param paramMap 参数的Map
	 * 
	 * @return 值数组
	 */
	public static Object[] buildValueArray(String sql, Map<String, ?> paramMap) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return buildValueArray(parsedSql, new MapSqlParameterSource(paramMap), null);
	}


	private static class ParameterHolder {

		private final String parameterName;

		private final int startIndex;

		private final int endIndex;

		public ParameterHolder(String parameterName, int startIndex, int endIndex) {
			this.parameterName = parameterName;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}

		public String getParameterName() {
			return this.parameterName;
		}

		public int getStartIndex() {
			return this.startIndex;
		}

		public int getEndIndex() {
			return this.endIndex;
		}
	}
}
