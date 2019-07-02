package org.springframework.test.util;

import org.skyscreamer.jsonassert.JSONAssert;

/**
 * JSON内容断言的辅助类.
 *
 * <p>使用此类需要<a href="http://jsonassert.skyscreamer.org/">JSONassert<a/>库.
 */
public class JsonExpectationsHelper {

	/**
	 * 将预期字符串和实际字符串解析为JSON, 并断言两者是"相似的" - i.e. 它们包含相同的属性-值对,
	 * 而不管格式的宽松检查 (可扩展和非严格的数组排序).
	 *
	 * @param expected 预期的JSON内容
	 * @param actual 实际的JSON内容
	 */
	public void assertJsonEqual(String expected, String actual) throws Exception {
		assertJsonEqual(expected, actual, false);
	}

	/**
	 * 将预期字符串和实际字符串解析为JSON, 并断言两者是 "相似的" - i.e. 它们包含相同的属性-值对, 而不管格式如何.
	 *
	 * <p>可以在两种模式下进行比较, 具体取决于{@code strict}参数值:
	 * <ul>
	 *     <li>{@code true}: 严格检查. 不可扩展, 严格的数组排序.</li>
	 *     <li>{@code false}: 宽松的检查. 可扩展和非严格的数组排序.</li>
	 * </ul>
	 *
	 * @param expected 预期的JSON内容
	 * @param actual 实际的JSON内容
	 * @param strict 是否严格检查
	 */
	public void assertJsonEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertEquals(expected, actual, strict);
	}

	/**
	 * 将预期字符串和实际字符串解析为JSON, 并断言这两个字符串"不相似" - i.e. 它们包含不同的属性-值对,
	 * 而不管格式是否具有宽松检查 (可扩展和非严格的数组排序).
	 *
	 * @param expected 预期的JSON内容
	 * @param actual 实际的JSON内容
	 */
	public void assertJsonNotEqual(String expected, String actual) throws Exception {
		assertJsonNotEqual(expected, actual, false);
	}

	/**
	 * 将预期字符串和实际字符串解析为JSON, 并断言两者"不相似" - i.e. 它们包含不同的属性-值对, 而不管格式如何.
	 *
	 * <p>可以在两种模式下进行比较, 具体取决于{@code strict}参数值:
	 * <ul>
	 *     <li>{@code true}: 严格检查. 不可扩展, 严格的数组排序.</li>
	 *     <li>{@code false}: 宽松的检查. 可扩展和非严格的数组排序.</li>
	 * </ul>
	 *
	 * @param expected 预期的JSON内容
	 * @param actual 实际的JSON内容
	 * @param strict 是否严格检查
	 */
	public void assertJsonNotEqual(String expected, String actual, boolean strict) throws Exception {
		JSONAssert.assertNotEquals(expected, actual, strict);
	}
}
