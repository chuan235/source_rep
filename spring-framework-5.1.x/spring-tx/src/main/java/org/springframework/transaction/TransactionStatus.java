/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction;

import java.io.Flushable;

/**
 * Representation of the status of a transaction.
 *
 * <p>Transactional code can use this to retrieve status information,
 * and to programmatically request a rollback (instead of throwing
 * an exception that causes an implicit rollback).
 *
 * <p>Includes the {@link SavepointManager} interface to provide access
 * to savepoint management facilities. Note that savepoint management
 * is only available if supported by the underlying transaction manager.
 *
 * @author Juergen Hoeller
 * @since 27.03.2003
 * @see #setRollbackOnly()
 * @see PlatformTransactionManager#getTransaction
 * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
 * @see org.springframework.transaction.interceptor.TransactionInterceptor#currentTransactionStatus()
 */
public interface TransactionStatus extends SavepointManager, Flushable {

	/**
	 * Return whether the present transaction is new; otherwise participating
	 * in an existing transaction, or potentially not running in an actual
	 * transaction in the first place.
	 * 判断当前事务是不是一个全新的事务
	 */
	boolean isNewTransaction();

	/**
	 * Return whether this transaction internally carries a savepoint,
	 * that is, has been created as nested transaction based on a savepoint.
	 * <p>This method is mainly here for diagnostic purposes, alongside 主要用于检测
	 * {@link #isNewTransaction()}. For programmatic handling of custom
	 * savepoints, use the operations provided by {@link SavepointManager}.
	 * 使用 SavepointManager 进行自定义的保存点程序编写
	 * @see #isNewTransaction()
	 * @see #createSavepoint()
	 * @see #rollbackToSavepoint(Object)
	 * @see #releaseSavepoint(Object)
	 * 返回此事务是否在内部携带保存点，也就是说是否已基于保存点创建了嵌套事务。
	 */
	boolean hasSavepoint();

	/**
	 * Set the transaction rollback-only. This instructs the transaction manager
	 * that the only possible outcome of the transaction may be a rollback, as
	 * alternative to throwing an exception which would in turn trigger a rollback.
	 * <p>This is mainly intended for transactions managed by
	 * {@link org.springframework.transaction.support.TransactionTemplate} or
	 * {@link org.springframework.transaction.interceptor.TransactionInterceptor},
	 * where the actual commit/rollback decision is made by the container.
	 * @see org.springframework.transaction.support.TransactionCallback#doInTransaction
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#rollbackOn
	 * 设置仅事务回滚，这表示事务管理器的唯一结果可能就是回滚
	 * 替代抛出异常的方法
	 */
	void setRollbackOnly();

	/**
	 * Return whether the transaction has been marked as rollback-only
	 * (either by the application or by the transaction infrastructure).
	 * 判断事务是否标记为仅回滚模式
	 *
	 */
	boolean isRollbackOnly();

	/**
	 * Flush the underlying session to the datastore, if applicable:for example, all affected Hibernate/JPA sessions.
	 * <p>This is effectively just a hint/刷新提示 and may be a no-op if the underlying
	 * transaction manager does not have a flush concept/不支持会话的刷新. A flush signal may
	 * get applied to the primary resource or to transaction synchronizations,
	 * depending on the underlying resource.
	 * 刷新信号可能用于主要资源、事务同步 => 主要取决于session底层资源
	 * Flush the underlying session to the datastore
	 */
	@Override
	void flush();

	/**
	 * Return whether this transaction is completed, that is,
	 * whether it has already been committed or rolled back.
	 * @see PlatformTransactionManager#commit
	 * @see PlatformTransactionManager#rollback
	 */
	boolean isCompleted();

}
