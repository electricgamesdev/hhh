package com.hydrides.engine;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.LoggerFactory;

import com.hydrides.core.Component;
import com.hydrides.core.Constants;
import com.hydrides.core.Container;
import com.hydrides.core.Engine;
import com.hydrides.core.HydridesContext;
import com.hydrides.core.Interface;
import com.hydrides.core.Macro;
import com.hydrides.processor.JavaProcessor;

public class JavaEngine extends Engine {

	org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

	EntityManagerFactory factory = null;
	EntityManager manager = null;

	@Override
	public void build(HydridesContext context, Container container) throws Exception {
		final File file = context.getFile(container.getPath());
		Properties compProp = new Properties();

		compProp.load(new FileInputStream(file));

		log.info("Creating JVM " + file.getName().toUpperCase());

		for (Component comp2 : container.getComponent()) {
			Component comp = context.getComponent(comp2.getPath());

			for (Interface intf : comp.getInterface()) {
			
				for (Macro macro : intf.getMacros()) {
					if ("java".equalsIgnoreCase(intf.getType())) {
						if ("main".equalsIgnoreCase(macro.getType())) {
							addProcessor(comp,intf, macro, new JavaProcessor(file, Constants.RECORD.MAP, macro.getPath(), "main"));
						} else if ("method".equalsIgnoreCase(macro.getType())) {
							addProcessor(comp,intf, macro, new JavaProcessor(file, Constants.RECORD.MAP, macro.getPath(), macro.getOn()));
						} else if ("return".equalsIgnoreCase(macro.getType())) {
							addProcessor(comp,intf, macro, new JavaProcessor(file, Constants.RECORD.MAP, macro.getPath(),"return"));

						}
					}
				}
			}
		}

		
	}

	
}
