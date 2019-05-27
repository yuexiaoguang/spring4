package org.springframework.util;

import java.util.UUID;

/**
 * 生成通用唯一标识符{@link UUID (UUIDs)}的约定.
 */
public interface IdGenerator {

	UUID generateId();

}
