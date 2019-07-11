package org.springframework.jca.cci.core;

import java.sql.SQLException;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jca.cci.CannotCreateRecordException;
import org.springframework.jca.cci.CciOperationNotSupportedException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jca.cci.RecordTypeNotSupportedException;
import org.springframework.jca.cci.connection.ConnectionFactoryUtils;
import org.springframework.jca.cci.connection.NotSupportedRecordFactory;
import org.springframework.util.Assert;

/**
 * <b>这是CCI核心包中的中心类.</b>
 * 它简化了CCI的使用, 有助于避免常见错误.
 * 它执行核心CCI工作流程, 留下应用程序代码为CCI提供参数并提取结果.
 * 此类执行EIS查询或更新, 捕获ResourceExceptions, 并将它们转换为{@code org.springframework.dao}包中定义的通用异常层次结构.
 *
 * <p>使用此类的代码可以传入并接收{@link javax.resource.cci.Record}实例, 或者实现回调接口以创建输入记录,
 * 并从输出记录 (或CCI ResultSets)中提取结果对象.
 *
 * <p>可以通过使用ConnectionFactory引用直接实例化在服务实现中使用, 或者在应用程序上下文中准备, 并作为bean引用提供给服务.
 * Note: ConnectionFactory应始终在应用程序上下文中配置为bean, 在第一种情况下直接提供给服务, 在第二种情况下配置为准备好的模板.
 */
public class CciTemplate implements CciOperations {

	private final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory connectionFactory;

	private ConnectionSpec connectionSpec;

	private RecordCreator outputRecordCreator;


	/**
	 * <p>Note: 必须在使用实例之前设置ConnectionFactory.
	 */
	public CciTemplate() {
	}

	/**
	 * Note: 这将触发异常转换器的实时初始化.
	 * 
	 * @param connectionFactory 从中获取Connection的JCA ConnectionFactory
	 */
	public CciTemplate(ConnectionFactory connectionFactory) {
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * Note: 这将触发异常转换器的实时初始化.
	 * 
	 * @param connectionFactory 从中获取Connection的JCA ConnectionFactory
	 * @param connectionSpec 从中获取Connection的CCI ConnectionSpec (may be {@code null})
	 */
	public CciTemplate(ConnectionFactory connectionFactory, ConnectionSpec connectionSpec) {
		setConnectionFactory(connectionFactory);
		setConnectionSpec(connectionSpec);
		afterPropertiesSet();
	}


	/**
	 * 设置从中获取Connection的 CCI ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回此模板使用的CCI ConnectionFactory.
	 */
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 设置此模板实例应该获取Connections的CCI ConnectionSpec.
	 */
	public void setConnectionSpec(ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * 返回此模板使用的CCI ConnectionSpec.
	 */
	public ConnectionSpec getConnectionSpec() {
		return this.connectionSpec;
	}

	/**
	 * 设置应该用于创建默认输出记录的RecordCreator.
	 * <p>默认无: 当没有显式输出Record传递到{@code execute}方法时,
	 * 将调用返回输出Record的CCI的{@code Interaction.execute}变体.
	 * <p>如果总是需要使用传入的输出Record调用CCI的{@code Interaction.execute}变体, 请在此处指定RecordCreator.
	 * 除非有明确指定的输出Record, 否则CciTemplate将调用此RecordCreator来创建默认输出Record实例.
	 */
	public void setOutputRecordCreator(RecordCreator creator) {
		this.outputRecordCreator = creator;
	}

	/**
	 * 返回应该用于创建默认输出记录的RecordCreator.
	 */
	public RecordCreator getOutputRecordCreator() {
		return this.outputRecordCreator;
	}

	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * 创建从此模板实例派生的模板, 继承ConnectionFactory和其他设置, 但覆盖用于获取Connections的ConnectionSpec.
	 * 
	 * @param connectionSpec 派生的模板实例应该获取连接的CCI ConnectionSpec
	 * 
	 * @return the derived template instance
	 */
	public CciTemplate getDerivedTemplate(ConnectionSpec connectionSpec) {
		CciTemplate derived = new CciTemplate();
		derived.setConnectionFactory(getConnectionFactory());
		derived.setConnectionSpec(connectionSpec);
		derived.setOutputRecordCreator(getOutputRecordCreator());
		return derived;
	}


	@Override
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		Connection con = ConnectionFactoryUtils.getConnection(getConnectionFactory(), getConnectionSpec());
		try {
			return action.doInConnection(con, getConnectionFactory());
		}
		catch (NotSupportedException ex) {
			throw new CciOperationNotSupportedException("CCI operation not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new DataAccessResourceFailureException("CCI operation failed", ex);
		}
		catch (SQLException ex) {
			throw new InvalidResultSetAccessException("Parsing of CCI ResultSet failed", ex);
		}
		finally {
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
		}
	}

	@Override
	public <T> T execute(final InteractionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(new ConnectionCallback<T>() {
			@Override
			public T doInConnection(Connection connection, ConnectionFactory connectionFactory)
					throws ResourceException, SQLException, DataAccessException {
				Interaction interaction = connection.createInteraction();
				try {
					return action.doInInteraction(interaction, connectionFactory);
				}
				finally {
					closeInteraction(interaction);
				}
			}
		});
	}

	@Override
	public Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException {
		return doExecute(spec, inputRecord, null, new SimpleRecordExtractor());
	}

	@Override
	public void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException {
		doExecute(spec, inputRecord, outputRecord, null);
	}

	@Override
	public Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException {
		return doExecute(spec, createRecord(inputCreator), null, new SimpleRecordExtractor());
	}

	@Override
	public <T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, inputRecord, null, outputExtractor);
	}

	@Override
	public <T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, createRecord(inputCreator), null, outputExtractor);
	}

	/**
	 * 使用CCI在EIS上执行指定的交互.
	 * 所有其他交互执行方法都经历了这一点.
	 * 
	 * @param spec 定义交互的CCI InteractionSpec实例 (特定于连接器)
	 * @param inputRecord 输入记录
	 * @param outputRecord 输出记录 (can be {@code null})
	 * @param outputExtractor 将输出记录转换为结果对象的对象
	 * 
	 * @return 使用RecordExtractor对象提取的输出数据
	 * @throws DataAccessException
	 */
	protected <T> T doExecute(
			final InteractionSpec spec, final Record inputRecord, final Record outputRecord,
			final RecordExtractor<T> outputExtractor) throws DataAccessException {

		return execute(new InteractionCallback<T>() {
			@Override
			public T doInInteraction(Interaction interaction, ConnectionFactory connectionFactory)
					throws ResourceException, SQLException, DataAccessException {
				Record outputRecordToUse = outputRecord;
				try {
					if (outputRecord != null || getOutputRecordCreator() != null) {
						// 使用带有输出记录的CCI执行方法作为参数.
						if (outputRecord == null) {
							RecordFactory recordFactory = getRecordFactory(connectionFactory);
							outputRecordToUse = getOutputRecordCreator().createRecord(recordFactory);
						}
						interaction.execute(spec, inputRecord, outputRecordToUse);
					}
					else {
						outputRecordToUse = interaction.execute(spec, inputRecord);
					}
					return (outputExtractor != null ? outputExtractor.extractData(outputRecordToUse) : null);
				}
				finally {
					if (outputRecordToUse instanceof ResultSet) {
						closeResultSet((ResultSet) outputRecordToUse);
					}
				}
			}
		});
	}


	/**
	 * 通过ConnectionFactory的RecordFactory创建一个索引的记录.
	 * 
	 * @param name 记录的名称
	 * 
	 * @return the Record
	 * @throws DataAccessException 如果创建记录失败
	 */
	public IndexedRecord createIndexedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(getConnectionFactory());
			return recordFactory.createIndexedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException("Creation of indexed Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of indexed Record failed", ex);
		}
	}

	/**
	 * 从ConnectionFactory的RecordFactory创建一个映射的记录.
	 * 
	 * @param name 记录名称
	 * 
	 * @return the Record
	 * @throws DataAccessException 如果创建记录失败
	 */
	public MappedRecord createMappedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(getConnectionFactory());
			return recordFactory.createMappedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException("Creation of mapped Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of mapped Record failed", ex);
		}
	}

	/**
	 * 调用给定的RecordCreator, 将JCA ResourceExceptions转换为Spring的DataAccessException层次结构.
	 * 
	 * @param recordCreator 要调用的RecordCreator
	 * 
	 * @return 创建的Record
	 * @throws DataAccessException 如果创建记录失败
	 */
	protected Record createRecord(RecordCreator recordCreator) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(getConnectionFactory());
			return recordCreator.createRecord(recordFactory);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException(
					"Creation of the desired Record type not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of the desired Record failed", ex);
		}
	}

	/**
	 * 返回给定ConnectionFactory的RecordFactory.
	 * <p>默认实现返回连接器的RecordFactory, 回退到NotSupportedRecordFactory占位符.
	 * 这允许在任何情况下使用非null RecordFactory引用调用RecordCreator回调.
	 * 
	 * @param connectionFactory the CCI ConnectionFactory
	 * 
	 * @return 用于ConnectionFactory的CCI RecordFactory
	 * @throws ResourceException 如果被CCI方法抛出
	 */
	protected RecordFactory getRecordFactory(ConnectionFactory connectionFactory) throws ResourceException {
		try {
			return connectionFactory.getRecordFactory();
		}
		catch (NotSupportedException ex) {
			return new NotSupportedRecordFactory();
		}
	}


	/**
	 * 关闭给定的CCI Interaction并忽略任何抛出的异常.
	 * 这对于手动CCI代码中的典型finally块非常有用.
	 * 
	 * @param interaction 要关闭的CCI Interaction
	 */
	private void closeInteraction(Interaction interaction) {
		if (interaction != null) {
			try {
				interaction.close();
			}
			catch (ResourceException ex) {
				logger.trace("Could not close CCI Interaction", ex);
			}
			catch (Throwable ex) {
				// 不信任CCI驱动程序: 它可能会抛出RuntimeException或Error.
				logger.trace("Unexpected exception on closing CCI Interaction", ex);
			}
		}
	}

	/**
	 * 关闭给定的CCI ResultSet并忽略任何抛出的异常.
	 * 这对于手动CCI代码中的典型finally块非常有用.
	 * 
	 * @param resultSet 要关闭的CCI ResultSet
	 */
	private void closeResultSet(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close CCI ResultSet", ex);
			}
			catch (Throwable ex) {
				// 不信任CCI驱动程序: 它可能会抛出RuntimeException或Error.
				logger.trace("Unexpected exception on closing CCI ResultSet", ex);
			}
		}
	}


	private static class SimpleRecordExtractor implements RecordExtractor<Record> {

		@Override
		public Record extractData(Record record) {
			return record;
		}
	}
}
