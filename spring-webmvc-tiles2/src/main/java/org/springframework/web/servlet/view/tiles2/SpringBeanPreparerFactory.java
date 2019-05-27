package org.springframework.web.servlet.view.tiles2;

import org.apache.tiles.TilesException;
import org.apache.tiles.preparer.ViewPreparer;

import org.springframework.web.context.WebApplicationContext;

/**
 * Tiles {@link org.apache.tiles.preparer.PreparerFactory} implementation
 * that expects preparer bean names and obtains preparer beans from the
 * Spring ApplicationContext. The full bean creation process will be in
 * the control of the Spring application context in this case, allowing
 * for the use of scoped beans etc.
 *
 * <p><b>NOTE: Tiles 2 support is deprecated in favor of Tiles 3 and will be removed
 * as of Spring Framework 5.0.</b>.
 *
 * @deprecated as of Spring 4.2, in favor of Tiles 3
 */
@Deprecated
public class SpringBeanPreparerFactory extends AbstractSpringPreparerFactory {

	@Override
	protected ViewPreparer getPreparer(String name, WebApplicationContext context) throws TilesException {
		return context.getBean(name, ViewPreparer.class);
	}

}
