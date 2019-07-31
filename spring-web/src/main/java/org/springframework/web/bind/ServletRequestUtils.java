package org.springframework.web.bind;

import javax.servlet.ServletRequest;

/**
 * 参数提取方法, 用于与数据绑定不同的方法, 其中需要特定类型的参数.
 *
 * <p>这种方法对于简单的提交非常有用, 其中绑定请求参数对命令对象来说是过度的.
 */
public abstract class ServletRequestUtils {

	private static final IntParser INT_PARSER = new IntParser();

	private static final LongParser LONG_PARSER = new LongParser();

	private static final FloatParser FLOAT_PARSER = new FloatParser();

	private static final DoubleParser DOUBLE_PARSER = new DoubleParser();

	private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();

	private static final StringParser STRING_PARSER = new StringParser();


	/**
	 * 获取Integer参数, 或{@code null}.
	 * 如果参数值不是数字, 则引发异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return Integer值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static Integer getIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredIntParameter(request, name);
	}

	/**
	 * 获取带有回退值的int参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static int getIntParameter(ServletRequest request, String name, int defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredIntParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个int参数数组, 或空数组.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static int[] getIntParameters(ServletRequest request, String name) {
		try {
			return getRequiredIntParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new int[0];
		}
	}

	/**
	 * 获取一个int参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static int getRequiredIntParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInt(name, request.getParameter(name));
	}

	/**
	 * 获取一个int参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static int[] getRequiredIntParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return INT_PARSER.parseInts(name, request.getParameterValues(name));
	}


	/**
	 * 获取Long参数, 或{@code null}.
	 * 如果参数值不是数字, 则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return Long值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static Long getLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredLongParameter(request, name);
	}

	/**
	 * 获取一个带有后备值的long参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static long getLongParameter(ServletRequest request, String name, long defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredLongParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个long参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static long[] getLongParameters(ServletRequest request, String name) {
		try {
			return getRequiredLongParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new long[0];
		}
	}

	/**
	 * 获取一个long参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static long getRequiredLongParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLong(name, request.getParameter(name));
	}

	/**
	 * 获取long参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static long[] getRequiredLongParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return LONG_PARSER.parseLongs(name, request.getParameterValues(name));
	}


	/**
	 * 获取Float参数, 或{@code null}.
	 * 如果参数值不是数字, 则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return Float值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static Float getFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredFloatParameter(request, name);
	}

	/**
	 * 获取具有回退值的float参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static float getFloatParameter(ServletRequest request, String name, float defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredFloatParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个float参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static float[] getFloatParameters(ServletRequest request, String name) {
		try {
			return getRequiredFloatParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new float[0];
		}
	}

	/**
	 * 获取float参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static float getRequiredFloatParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
	}

	/**
	 * 获取float参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static float[] getRequiredFloatParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return FLOAT_PARSER.parseFloats(name, request.getParameterValues(name));
	}


	/**
	 * 获取Double参数, 或{@code null}.
	 * 如果参数值不是数字, 则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return Double值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static Double getDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredDoubleParameter(request, name);
	}

	/**
	 * 获取带有后备值的double参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static double getDoubleParameter(ServletRequest request, String name, double defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredDoubleParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个double参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static double[] getDoubleParameters(ServletRequest request, String name) {
		try {
			return getRequiredDoubleParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new double[0];
		}
	}

	/**
	 * 获取一个double参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static double getRequiredDoubleParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
	}

	/**
	 * 获取double参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static double[] getRequiredDoubleParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return DOUBLE_PARSER.parseDoubles(name, request.getParameterValues(name));
	}


	/**
	 * 获取Boolean参数, 或{@code null}.
	 * 如果参数值不是boolean值, 则抛出异常.
	 * <p>接受"true", "on", "yes"和"1"作为 true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return Boolean值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static Boolean getBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return (getRequiredBooleanParameter(request, name));
	}

	/**
	 * 获取带有回退值的boolean参数. 永远不会抛出异常.
	 * <p>接受"true", "on", "yes"和"1"作为 true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static boolean getBooleanParameter(ServletRequest request, String name, boolean defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredBooleanParameter(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个boolean参数数组, 如果找不到则返回一个空数组.
	 * <p>接受"true", "on", "yes"和"1"作为 true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static boolean[] getBooleanParameters(ServletRequest request, String name) {
		try {
			return getRequiredBooleanParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new boolean[0];
		}
	}

	/**
	 * 获取boolean参数, 如果找不到或者不是boolean值则抛出异常.
	 * <p>接受"true", "on", "yes"和"1"作为 true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static boolean getRequiredBooleanParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
	}

	/**
	 * 获取一个boolean参数数组, 如果找不到或者一个不是boolean值则抛出异常.
	 * <p>接受"true", "on", "yes"和"1"作为 true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static boolean[] getRequiredBooleanParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return BOOLEAN_PARSER.parseBooleans(name, request.getParameterValues(name));
	}


	/**
	 * 获取String参数, 或{@code null}.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @return String值, 或{@code null}
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static String getStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredStringParameter(request, name);
	}

	/**
	 * 获取具有回退值的String参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * @param defaultVal 用作回退的默认值
	 */
	public static String getStringParameter(ServletRequest request, String name, String defaultVal) {
		String val = request.getParameter(name);
		return (val != null ? val : defaultVal);
	}

	/**
	 * 获取一个String参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 可能具有多个值的参数的名称
	 */
	public static String[] getStringParameters(ServletRequest request, String name) {
		try {
			return getRequiredStringParameters(request, name);
		}
		catch (ServletRequestBindingException ex) {
			return new String[0];
		}
	}

	/**
	 * 获取String参数, 如果找不到则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static String getRequiredStringParameter(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
	}

	/**
	 * 获取一个String参数数组, 如果找不到则抛出异常.
	 * 
	 * @param request 当前的HTTP请求
	 * @param name 参数的名称
	 * 
	 * @throws ServletRequestBindingException ServletException的子类, 因此不需要捕获它
	 */
	public static String[] getRequiredStringParameters(ServletRequest request, String name)
			throws ServletRequestBindingException {

		return STRING_PARSER.validateRequiredStrings(name, request.getParameterValues(name));
	}


	private abstract static class ParameterParser<T> {

		protected final T parse(String name, String parameter) throws ServletRequestBindingException {
			validateRequiredParameter(name, parameter);
			try {
				return doParse(parameter);
			}
			catch (NumberFormatException ex) {
				throw new ServletRequestBindingException(
						"Required " + getType() + " parameter '" + name + "' with value of '" +
						parameter + "' is not a valid number", ex);
			}
		}

		protected final void validateRequiredParameter(String name, Object parameter)
				throws ServletRequestBindingException {

			if (parameter == null) {
				throw new MissingServletRequestParameterException(name, getType());
			}
		}

		protected abstract String getType();

		protected abstract T doParse(String parameter) throws NumberFormatException;
	}


	private static class IntParser extends ParameterParser<Integer> {

		@Override
		protected String getType() {
			return "int";
		}

		@Override
		protected Integer doParse(String s) throws NumberFormatException {
			return Integer.valueOf(s);
		}

		public int parseInt(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public int[] parseInts(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			int[] parameters = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseInt(name, values[i]);
			}
			return parameters;
		}
	}


	private static class LongParser extends ParameterParser<Long> {

		@Override
		protected String getType() {
			return "long";
		}

		@Override
		protected Long doParse(String parameter) throws NumberFormatException {
			return Long.valueOf(parameter);
		}

		public long parseLong(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public long[] parseLongs(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			long[] parameters = new long[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseLong(name, values[i]);
			}
			return parameters;
		}
	}


	private static class FloatParser extends ParameterParser<Float> {

		@Override
		protected String getType() {
			return "float";
		}

		@Override
		protected Float doParse(String parameter) throws NumberFormatException {
			return Float.valueOf(parameter);
		}

		public float parseFloat(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public float[] parseFloats(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			float[] parameters = new float[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseFloat(name, values[i]);
			}
			return parameters;
		}
	}


	private static class DoubleParser extends ParameterParser<Double> {

		@Override
		protected String getType() {
			return "double";
		}

		@Override
		protected Double doParse(String parameter) throws NumberFormatException {
			return Double.valueOf(parameter);
		}

		public double parseDouble(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public double[] parseDoubles(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			double[] parameters = new double[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseDouble(name, values[i]);
			}
			return parameters;
		}
	}


	private static class BooleanParser extends ParameterParser<Boolean> {

		@Override
		protected String getType() {
			return "boolean";
		}

		@Override
		protected Boolean doParse(String parameter) throws NumberFormatException {
			return (parameter.equalsIgnoreCase("true") || parameter.equalsIgnoreCase("on") ||
					parameter.equalsIgnoreCase("yes") || parameter.equals("1"));
		}

		public boolean parseBoolean(String name, String parameter) throws ServletRequestBindingException {
			return parse(name, parameter);
		}

		public boolean[] parseBooleans(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			boolean[] parameters = new boolean[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseBoolean(name, values[i]);
			}
			return parameters;
		}
	}


	private static class StringParser extends ParameterParser<String> {

		@Override
		protected String getType() {
			return "string";
		}

		@Override
		protected String doParse(String parameter) throws NumberFormatException {
			return parameter;
		}

		public String validateRequiredString(String name, String value) throws ServletRequestBindingException {
			validateRequiredParameter(name, value);
			return value;
		}

		public String[] validateRequiredStrings(String name, String[] values) throws ServletRequestBindingException {
			validateRequiredParameter(name, values);
			for (String value : values) {
				validateRequiredParameter(name, value);
			}
			return values;
		}
	}

}
