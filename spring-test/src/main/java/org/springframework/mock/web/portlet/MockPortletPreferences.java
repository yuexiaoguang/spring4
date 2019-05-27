package org.springframework.mock.web.portlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.portlet.PortletPreferences;
import javax.portlet.PreferencesValidator;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.portlet.PortletPreferences} interface.
 */
public class MockPortletPreferences implements PortletPreferences {

	private PreferencesValidator preferencesValidator;

	private final Map<String, String[]> preferences = new LinkedHashMap<String, String[]>();

	private final Set<String> readOnly = new HashSet<String>();


	public void setReadOnly(String key, boolean readOnly) {
		Assert.notNull(key, "Key must not be null");
		if (readOnly) {
			this.readOnly.add(key);
		}
		else {
			this.readOnly.remove(key);
		}
	}

	@Override
	public boolean isReadOnly(String key) {
		Assert.notNull(key, "Key must not be null");
		return this.readOnly.contains(key);
	}

	@Override
	public String getValue(String key, String def) {
		Assert.notNull(key, "Key must not be null");
		String[] values = this.preferences.get(key);
		return (values != null && values.length > 0 ? values[0] : def);
	}

	@Override
	public String[] getValues(String key, String[] def) {
		Assert.notNull(key, "Key must not be null");
		String[] values = this.preferences.get(key);
		return (values != null && values.length > 0 ? values : def);
	}

	@Override
	public void setValue(String key, String value) throws ReadOnlyException {
		setValues(key, new String[] {value});
	}

	@Override
	public void setValues(String key, String[] values) throws ReadOnlyException {
		Assert.notNull(key, "Key must not be null");
		if (isReadOnly(key)) {
			throw new ReadOnlyException("Preference '" + key + "' is read-only");
		}
		this.preferences.put(key, values);
	}

	@Override
	public Enumeration<String> getNames() {
		return Collections.enumeration(this.preferences.keySet());
	}

	@Override
	public Map<String, String[]> getMap() {
		return Collections.unmodifiableMap(this.preferences);
	}

	@Override
	public void reset(String key) throws ReadOnlyException {
		Assert.notNull(key, "Key must not be null");
		if (isReadOnly(key)) {
			throw new ReadOnlyException("Preference '" + key + "' is read-only");
		}
		this.preferences.remove(key);
	}

	public void setPreferencesValidator(PreferencesValidator preferencesValidator) {
		this.preferencesValidator = preferencesValidator;
	}

	@Override
	public void store() throws IOException, ValidatorException {
		if (this.preferencesValidator != null) {
			this.preferencesValidator.validate(this);
		}
	}

}
