package com.hydrides.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.io.IOUtils;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.kie.api.KieBase;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.UserGroupCallback;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
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
import com.hydrides.processor.FileProcessor;
import com.hydrides.processor.JBPMProcessProcessor;
import com.hydrides.processor.JBPMTaskProcessor;

public class JBPMEngine extends Engine {

	private KieBase kbase = null;
	private KieSession ksession = null;
	private ProcessInstance pinstance = null;

	public KieBase getKbase() {
		return kbase;
	}

	public KieSession getKsession() {
		return ksession;
	}

	public ProcessInstance getPinstance() {
		return pinstance;
	}

	@Override
	public void build(HydridesContext context, Container container) throws Exception {

		File file = context.getFile(container.getPath());

		Properties compProp = new Properties();

		compProp.load(new FileInputStream(file));

		List<String> mappingFiles = IOUtils.readLines(getClass().getResourceAsStream("mapping.jpa"));
		List<String> classiles = IOUtils.readLines(getClass().getResourceAsStream("class.jpa"));
		PersistenceUnitInfoImpl impl = new PersistenceUnitInfoImpl(
				container.getPath().replaceAll("/", "_").toUpperCase(), mappingFiles, classiles, compProp);
		PersistenceUnitInfoDescriptor persistenceUnitInfo = new PersistenceUnitInfoDescriptor(impl);

		Map<String, Object> integrationSettings = new HashMap<String, Object>();
		// integrationSettings.put(AvailableSettings.INTERCEPTOR, new
		// CustomSessionFactoryInterceptor());

		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder = new EntityManagerFactoryBuilderImpl(
				persistenceUnitInfo, integrationSettings);

		EntityManagerFactory entityManagerFactory = entityManagerFactoryBuilder.build();

		KieHelper kieHelper = new KieHelper();

		kbase = kieHelper.addResource(ResourceFactory.newFileResource(file)).build();

		environment = KnowledgeBaseFactory.newEnvironment();

		environment.set(EnvironmentName.ENTITY_MANAGER_FACTORY, entityManagerFactory);
		// environment.set(EnvironmentName.TRANSACTION_MANAGER,
		// TransactionManagerServices.getTransactionManager());

		// JAASUserGroupCallbackImpl callbackImpl = new
		// JAASUserGroupCallbackImpl(true);

		Properties p = new Properties();
		p.put("db.ds.jndi.name", "jdbc/flow");
		p.put("db.user.query", "select password from principles where user_id=?");
		p.put("db.user.roles.query", "select user_role, 'Roles' from principles where user_id=?");
		p.put("db.roles.query", "select user_role, 'Roles' from roles where user_id=?");

		// DBUserGroupCallbackImpl callbackImpl = new
		// DBUserGroupCallbackImpl(p);

		UserGroupCallback callbackImpl = new UserGroupCallback() {

			@Override
			public List<String> getGroupsForUser(String userId, List<String> groupIds,
					List<String> allExistingGroupIds) {
				return Arrays.asList("admin");
			}

			@Override
			public boolean existsUser(String userId) {
				return true;
			}

			@Override
			public boolean existsGroup(String groupId) {
				return true;
			}
		};

		for (Component comp2 : container.getComponent()) {

			Component comp = context.getComponent(comp2.getPath());

			File bpmn2 = context.getFile(comp.getPath());

			RuntimeEnvironmentBuilder builder = RuntimeEnvironmentBuilder.Factory.get().newDefaultBuilder()
					.entityManagerFactory(entityManagerFactory).knowledgeBase(kbase).userGroupCallback(callbackImpl);

			RuntimeManager manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(builder.get(),
					bpmn2.getName());

			for (Interface a : comp.getInterface()) {

				if (a.getType().equalsIgnoreCase("process")) {

					for (Macro c : a.getMacros()) {

						if ("java".equalsIgnoreCase(c.getType())) {

							if ("org.kie.api.runtime.KieSession".equalsIgnoreCase(c.getPath())) {
								JBPMProcessProcessor processor = new JBPMProcessProcessor(a.getOn(), c.getAs());
								addProcessor(comp,a, c, processor);
							} else if ("org.kie.api.task.TaskService".equalsIgnoreCase(c.getPath())) {
								JBPMTaskProcessor processor = new JBPMTaskProcessor(a.getOn(), c.getAs());
								addProcessor(comp,a, c, processor);
							}

						}

					}
				}

			}

			addProcessor(comp, new FileProcessor(bpmn2, Constants.RECORD.XML));

		}

	}

	private Environment environment = null;

	private Map<String, String> eventPathMap = new HashMap<String, String>();

	public String getEventPathMap(String key) {
		return eventPathMap.get(key);
	}

}
