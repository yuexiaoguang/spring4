package org.springframework.test.annotation;

/**
 * <p>
 * Strategy interface for retrieving <em>profile values</em> for a given
 * testing environment.
 * </p>
 * <p>
 * Concrete implementations must provide a {@code public} no-args
 * constructor.
 * </p>
 * <p>
 * Spring provides the following out-of-the-box implementations:
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 */
public interface ProfileValueSource {

	/**
	 * Get the <em>profile value</em> indicated by the specified key.
	 * @param key the name of the <em>profile value</em>
	 * @return the String value of the <em>profile value</em>, or {@code null}
	 * if there is no <em>profile value</em> with that key
	 */
	String get(String key);

}
