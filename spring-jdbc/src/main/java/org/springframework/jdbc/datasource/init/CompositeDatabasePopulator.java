package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 复合{@link DatabasePopulator}, 它委托给给定的{@code DatabasePopulator}实现列表, 执行所有脚本.
 */
public class CompositeDatabasePopulator implements DatabasePopulator {

	private final List<DatabasePopulator> populators = new ArrayList<DatabasePopulator>(4);


	public CompositeDatabasePopulator() {
	}

	/**
	 * @param populators 要委托给的一个或多个填充器
	 */
	public CompositeDatabasePopulator(Collection<DatabasePopulator> populators) {
		this.populators.addAll(populators);
	}

	/**
	 * @param populators 要委托给的一个或多个填充器
	 */
	public CompositeDatabasePopulator(DatabasePopulator... populators) {
		this.populators.addAll(Arrays.asList(populators));
	}


	/**
	 * 指定要委托给的一个或多个填充器.
	 */
	public void setPopulators(DatabasePopulator... populators) {
		this.populators.clear();
		this.populators.addAll(Arrays.asList(populators));
	}

	/**
	 * 将一个或多个填充器添加到委托列表中.
	 */
	public void addPopulators(DatabasePopulator... populators) {
		this.populators.addAll(Arrays.asList(populators));
	}


	@Override
	public void populate(Connection connection) throws SQLException, ScriptException {
		for (DatabasePopulator populator : this.populators) {
			populator.populate(connection);
		}
	}

}
