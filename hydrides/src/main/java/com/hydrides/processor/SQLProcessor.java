package com.hydrides.processor;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;

import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData.Record;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Processor;

public class SQLProcessor extends Processor {

	Logger log = LoggerFactory.getLogger(SQLProcessor.class);

	EntityManagerFactory entityManagerFactory = null;

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	private String query = null;
	private String opr = null;

	public SQLProcessor(String query, String opr) {
		this.query = query;
		this.opr = opr;
	}

	private EntityManager entityManager = null;

	@Override
	public Object process(List<Record> record) {

		entityManager = entityManagerFactory.createEntityManager(SynchronizationType.SYNCHRONIZED);

		try {

			// HibernateEntityManager hem = entityManager.getDelegate();

			Session session = (Session) entityManager.getDelegate();

			session.setHibernateFlushMode(FlushMode.ALWAYS);

			log.info(" SQL : " + query);

			for (Record insert : record) {

				Query q = session.createNativeQuery(query);

				if ("select".equalsIgnoreCase(opr) || "insert".equalsIgnoreCase(opr) || "update".equalsIgnoreCase(opr)
						|| "delete".equalsIgnoreCase(opr)) {

					for (Field f : insert.getSchema().getFields()) {
						q.setParameter(f.name(), insert.get(f.name()));
					}

					if ("select".equalsIgnoreCase(opr)) {
						// List<Object[]> l = q.getResultList();
						org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
						hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
						List<Map<String, Object>> data = hibernateQuery.getResultList();
						return data;
					} else {
						q.executeUpdate();
					}
				} else if ("create".equalsIgnoreCase(opr) || "drop".equalsIgnoreCase(opr)) {
					q.executeUpdate();
				}

			}

			session.flush();

		} catch (Exception e) {
			return null;
		}
		return null;
	}

}
