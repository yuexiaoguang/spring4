package org.springframework.jca.cci.core;

import javax.resource.ResourceException;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;

/**
 * 用于创建CCI Record实例的回调接口, 通常基于传入的CCI RecordFactory.
 *
 * <p>用于在CciTemplate中创建输入记录.
 * 或者, Record实例可以直接传递到CciTemplate的相应{@code execute}方法,
 * 手动实例化或通过CciTemplate的Record工厂方法创建.
 *
 * <P>还用于在CciTemplate中创建默认输出记录.
 * 当JCA连接器需要显式输出Record实例时, 这很有用, 但是不应将输出记录传递给CciTemplate的{@code execute}方法.
 */
public interface RecordCreator {

	/**
	 * 创建一个CCI Record实例, 通常基于传入的CCI RecordFactory.
	 * <p>要用作使用CciTemplate的{@code execute}方法的<i>input</i>创建者, 此方法应创建<i>填充的</i>记录实例.
	 * 要用作<i>output</i> Record创建者, 它应该返回<i>空</i>记录实例.
	 * 
	 * @param recordFactory CCI RecordFactory (不能是{@code null}, 但不保证连接器支持它: 它的create方法可能抛出NotSupportedException)
	 * 
	 * @return Record实例
	 * @throws ResourceException 如果由CCI方法抛出, 则自动转换为DataAccessException
	 * @throws DataAccessException 自定义异常
	 */
	Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException;

}
