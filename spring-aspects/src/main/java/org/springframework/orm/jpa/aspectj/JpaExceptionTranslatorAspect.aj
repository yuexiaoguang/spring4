package org.springframework.orm.jpa.aspectj;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.aspectj.lang.annotation.SuppressAjWarnings;

import org.springframework.dao.DataAccessException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

public aspect JpaExceptionTranslatorAspect {

	pointcut entityManagerCall():
		call(* EntityManager.*(..)) || call(* EntityManagerFactory.*(..)) ||
		call(* EntityTransaction.*(..)) || call(* Query.*(..));

	@SuppressAjWarnings("adviceDidNotMatch")
	after() throwing(RuntimeException re): entityManagerCall() {
		DataAccessException dex = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(re);
		if (dex != null) {
			throw dex;
		}
		else {
			throw re;
		}
	}

}
