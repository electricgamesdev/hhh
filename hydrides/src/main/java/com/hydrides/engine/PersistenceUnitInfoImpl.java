package com.hydrides.engine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

	private final String persistenceUnitName;

	private PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.JTA;

	private List<String> mappingFiles = null;
	private List<String> managedClasses = null;

	private Properties properties;

	private DataSource jtaDataSource;

	private DataSource nonJtaDataSource;

	public PersistenceUnitInfoImpl(String persistenceUnitName, List<String> mappingFiles, List<String> managedClasses,
			Properties properties) {
		this.persistenceUnitName = persistenceUnitName;
		this.mappingFiles = mappingFiles;
		this.managedClasses = managedClasses;
		this.properties = properties;

//		try {
//			InitialContext context = new InitialContext();
//			System.out.println("................" + properties.getProperty("hibernate.connection.datasource"));
//
//			this.jtaDataSource = (DataSource) context.lookup(properties.getProperty("hibernate.connection.datasource"));
//
//		} catch (NamingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}

	@Override
	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	@Override
	public String getPersistenceProviderClassName() {
		return HibernatePersistenceProvider.class.getName();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	@Override
	public DataSource getJtaDataSource() {
		return jtaDataSource;
	}

	public PersistenceUnitInfoImpl setJtaDataSource(DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
		this.nonJtaDataSource = null;
		transactionType = PersistenceUnitTransactionType.JTA;
		return this;
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public PersistenceUnitInfoImpl setNonJtaDataSource(DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
		this.jtaDataSource = null;
		transactionType = PersistenceUnitTransactionType.JTA;
		return this;
	}

	@Override
	public List<String> getMappingFileNames() {
		return mappingFiles;
	}

	@Override
	public List<URL> getJarFileUrls() {
		return Collections.emptyList();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return getClass().getClassLoader().getResource("META-INF/persistence.xml");
	}

	@Override
	public List<String> getManagedClassNames() {
		return this.managedClasses;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return true;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return SharedCacheMode.UNSPECIFIED;
	}

	@Override
	public ValidationMode getValidationMode() {
		return ValidationMode.AUTO;
	}

	public Properties getProperties() {
		return properties;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return "2.1";
	}

	@Override
	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {

	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}
}