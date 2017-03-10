package com.hydrides.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Interface;
import com.hydrides.core.Macro;
import com.hydrides.core.Constants;
import com.hydrides.core.Container;
import com.hydrides.core.Component;
import com.hydrides.core.Engine;
import com.hydrides.core.HydridesContext;
import com.hydrides.core.Processor;
import com.hydrides.core.Stream;
import com.hydrides.processor.ContentProcessor;
import com.hydrides.processor.FileProcessor;
import com.hydrides.processor.SQLProcessor;

public class JPAEngine extends Engine {

	org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

	EntityManagerFactory factory = null;
	EntityManager manager = null;

	@Override
	public void build(HydridesContext context, Container container) throws Exception {
		final File file = context.getFile(container.getPath());
		Properties compProp = new Properties();

		compProp.load(new FileInputStream(file));
		compProp.setProperty("hibernate.connection.isolation", String.valueOf(Connection.TRANSACTION_READ_UNCOMMITTED));

		log.info("Creating Hibernate Persistent Unit " + file.getName().toUpperCase());

		List<String> mappingFileNames = new ArrayList<String>();

		for (Component comp2 : container.getComponent()) {
			Component comp = context.getComponent(comp2.getPath());
			File cfg = context.getFile(comp.getPath());
			String path = context.getRootPath() + File.separator + "META-INF" + File.separator + cfg.getName();
			FileUtils.copyFile(cfg, new File(path), true);
			mappingFileNames.add("META-INF/" + cfg.getName());

			log.info("Entity Manager Created " + path);

			for (Interface w : comp.getInterface()) {
			
				for (Macro c : w.getMacros()) {
					if ("table".equalsIgnoreCase(w.getType())) {
						if ("sql".equalsIgnoreCase(c.getType())) {
							SQLProcessor sql = new SQLProcessor(context.getContent(c.getPath()), c.getAs());
							addProcessor(comp,w, c, sql);
						}
					}
				}
			}

			addProcessor(comp, new FileProcessor(file, Constants.RECORD.MAP));
		}

		PersistenceUnitInfoImpl impl = new PersistenceUnitInfoImpl(file.getName().toUpperCase(), mappingFileNames, null,
				compProp);
		PersistenceUnitInfoDescriptor persistenceUnitInfo = new PersistenceUnitInfoDescriptor(impl);
		Map<String, Object> integrationSettings = new HashMap<String, Object>();
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
				persistenceUnitInfo, integrationSettings);
		factory = entityManagerFactoryBuilder.build();

	}

}
