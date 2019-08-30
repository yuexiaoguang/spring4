package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

/**
 * 参数提取方法, 用于与数据绑定不同的方法, 其中需要特定类型的参数.
 *
 * <p>这种方法对于简单的提交非常有用, 其中绑定请求参数到命令对象是过度的.
 */
public abstract class PortletRequestUtils {

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
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return Integer值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static Integer getIntParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredIntParameter(request, name);
	}

	/**
	 * 获取带有回退值的int参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static int getIntParameter(PortletRequest request, String name, int defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredIntParameter(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个int参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static int[] getIntParameters(PortletRequest request, String name) {
		try {
			return getRequiredIntParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new int[0];
		}
	}

	/**
	 * 获取一个int参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static int getRequiredIntParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return INT_PARSER.parseInt(name, request.getParameter(name));
	}

	/**
	 * 获取一个int参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static int[] getRequiredIntParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return INT_PARSER.parseInts(name, request.getParameterValues(name));
	}


	/**
	 * 获取Long参数, 或{@code null}.
	 * 如果参数值不是数字, 则引发异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return Long值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static Long getLongParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredLongParameter(request, name);
	}

	/**
	 * 获取一个带有后备值的long参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static long getLongParameter(PortletRequest request, String name, long defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredLongParameter(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个long参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static long[] getLongParameters(PortletRequest request, String name) {
		try {
			return getRequiredLongParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new long[0];
		}
	}

	/**
	 * 获取一个long参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static long getRequiredLongParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return LONG_PARSER.parseLong(name, request.getParameter(name));
	}

	/**
	 * 获取一个long参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static long[] getRequiredLongParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return LONG_PARSER.parseLongs(name, request.getParameterValues(name));
	}


	/**
	 * 获取Float参数, 或{@code null}.
	 * 如果参数值不是数字, 则引发异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return Float值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static Float getFloatParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredFloatParameter(request, name);
	}

	/**
	 * 获取具有回退值的float参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static float getFloatParameter(PortletRequest request, String name, float defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredFloatParameter(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个float参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static float[] getFloatParameters(PortletRequest request, String name) {
		try {
			return getRequiredFloatParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new float[0];
		}
	}

	/**
	 * 获取一个float参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static float getRequiredFloatParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return FLOAT_PARSER.parseFloat(name, request.getParameter(name));
	}

	/**
	 * 获取一个float参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static float[] getRequiredFloatParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return FLOAT_PARSER.parseFloats(name, request.getParameterValues(name));
	}


	/**
	 * 获取Double参数, 或{@code null}.
	 * 如果参数值不是数字, 则引发异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return Double值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static Double getDoubleParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredDoubleParameter(request, name);
	}

	/**
	 * 获取带有后备值的double参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static double getDoubleParameter(PortletRequest request, String name, double defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredDoubleParameter(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个double参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static double[] getDoubleParameters(PortletRequest request, String name) {
		try {
			return getRequiredDoubleParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new double[0];
		}
	}

	/**
	 * 获取一个double参数, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static double getRequiredDoubleParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return DOUBLE_PARSER.parseDouble(name, request.getParameter(name));
	}

	/**
	 * 获取一个double参数数组, 如果找不到或者不是数字则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static double[] getRequiredDoubleParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return DOUBLE_PARSER.parseDoubles(name, request.getParameterValues(name));
	}


	/**
	 * 获取Boolean参数, 或{@code null}.
	 * 如果参数值不是boolean值, 则抛出异常.
	 * <p>接受"true", "on", "yes" 和"1"作为true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return Boolean值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static Boolean getBooleanParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return (getRequiredBooleanParameter(request, name));
	}

	/**
	 * 获取带有回退值的boolean参数. 永远不会抛出异常.
	 * <p>接受"true", "on", "yes" 和"1"作为true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static boolean getBooleanParameter(PortletRequest request, String name, boolean defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredBooleanParameter(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return defaultVal;
		}
	}

	/**
	 * 获取一个boolean参数数组, 如果找不到则返回一个空数组.
	 * <p>接受"true", "on", "yes" 和"1"作为true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static boolean[] getBooleanParameters(PortletRequest request, String name) {
		try {
			return getRequiredBooleanParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new boolean[0];
		}
	}

	/**
	 * 获取boolean参数, 如果找不到或者不是boolean值则抛出异常.
	 * <p>接受"true", "on", "yes" 和"1"作为true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static boolean getRequiredBooleanParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
	}

	/**
	 * 获取boolean参数数组, 如果找不到或者不是boolean值则抛出异常.
	 * <p>接受"true", "on", "yes" 和"1"作为true; 将每个其他非空值视为false (i.e. 宽松地解析).
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static boolean[] getRequiredBooleanParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return BOOLEAN_PARSER.parseBooleans(name, request.getParameterValues(name));
	}


	/**
	 * 获取String参数, 或{@code null}.
	 * 如果参数值为空, 则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @return String值, 或{@code null}
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static String getStringParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		if (request.getParameter(name) == null) {
			return null;
		}
		return getRequiredStringParameter(request, name);
	}

	/**
	 * 获取具有回退值的String参数. 永远不会抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * @param defaultVal 用作后备的默认值
	 */
	public static String getStringParameter(PortletRequest request, String name, String defaultVal) {
		String val = request.getParameter(name);
		return (val != null ? val : defaultVal);
	}

	/**
	 * 获取一个String参数数组, 如果找不到则返回一个空数组.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 具有多个可能值的参数的名称
	 */
	public static String[] getStringParameters(PortletRequest request, String name) {
		try {
			return getRequiredStringParameters(request, name);
		}
		catch (PortletRequestBindingException ex) {
			return new String[0];
		}
	}

	/**
	 * 获取String参数, 如果未找到或为空, 则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static String getRequiredStringParameter(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return STRING_PARSER.validateRequiredString(name, request.getParameter(name));
	}

	/**
	 * 获取一个String参数数组, 如果未找到或为空, 则抛出异常.
	 * 
	 * @param request 当前的portlet请求
	 * @param name 参数的名称
	 * 
	 * @throws PortletRequestBindingException PortletException的子类, 因此不需要捕获它
	 */
	public static String[] getRequiredStringParameters(PortletRequest request, String name)
			throws PortletRequestBindingException {

		return STRING_PARSER.validateRequiredStrings(name, request.getParameterValues(name));
	}


	private abstract static class ParameterParser<T> {

		protected final T parse(String name, String parameter) throws PortletRequestBindingException {
			validateRequiredParameter(name, parameter);
			try {
				return doParse(parameter);
			}
			catch (NumberFormatException ex) {
				throw new PortletRequestBindingException(
						"Required " + getType() + " parameter '" + name + "' with value of '" +
						parameter + "' is not a valid number", ex);
			}
		}

		protected final void validateRequiredParameter(String name, Object parameter)
				throws PortletRequestBindingException {

			if (parameter == null) {
				throw new MissingPortletRequestParameterException(name, getType());
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

		public int parseInt(String name, String parameter) throws PortletRequestBindingException {
			return parse(name, parameter);
		}

		public int[] parseInts(String name, String[] values) throws PortletRequestBindingException {
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

		public long parseLong(String name, String parameter) throws PortletRequestBindingException {
			return parse(name, parameter);
		}

		public long[] parseLongs(String name, String[] values) throws PortletRequestBindingException {
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

		public float parseFloat(String name, String parameter) throws PortletRequestBindingException {
			return parse(name, parameter);
		}

		public float[] parseFloats(String name, String[] values) throws PortletRequestBindingException {
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

		public double parseDouble(String name, String parameter) throws PortletRequestBindingException {
			return parse(name, parameter);
		}

		public double[] parseDoubles(String name, String[] values) throws PortletRequestBindingException {
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

		public boolean parseBoolean(String name, String parameter) throws PortletRequestBindingException {
			return parse(name, parameter);
		}

		public boolean[] parseBooleans(String name, String[] values) throws PortletRequestBindingException {
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

		public String validateRequiredString(String name, String value) throws PortletRequestBindingException {
			validateRequiredParameter(name, value);
			return value;
		}

		public String[] validateRequiredStrings(String name, String[] values) throws PortletRequestBindingException {
			validateRequiredParameter(name, values);
			for (String value : values) {
				validateRequiredParameter(name, value);
			}
			return values;
		}
	}

}
