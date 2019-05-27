package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

/**
 * Google Protocol Messages can contain message extensions that can be parsed if
 * the appropriate configuration has been registered in the {@code ExtensionRegistry}.
 *
 * <p>This interface provides a facility to populate the {@code ExtensionRegistry}.
 */
public interface ExtensionRegistryInitializer {

	/**
	 * Initializes the {@code ExtensionRegistry} with Protocol Message extensions.
	 * @param registry the registry to populate
	 */
    void initializeExtensionRegistry(ExtensionRegistry registry);

}
