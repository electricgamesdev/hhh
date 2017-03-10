package com.hydrides.engine;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.kie.api.runtime.Environment;

import com.hydrides.core.Component;
import com.hydrides.core.Constants;
import com.hydrides.core.Container;
import com.hydrides.core.Engine;
import com.hydrides.core.HydridesContext;
import com.hydrides.core.Interface;
import com.hydrides.core.Macro;
import com.hydrides.core.Stream;
import com.hydrides.processor.RESTProcessor;
import com.hydrides.processor.URLProcessor;

public class HTTPEngine extends Engine {

	@Override
	public void build(HydridesContext context, Container container) throws Exception {

		File host = context.getFile(container.getPath());
		String hostname = IOUtils.toString(new FileInputStream(host));
		for (Component comp2 : container.getComponent()) {
			Component comp = context.getComponent(comp2.getPath());

			for (Interface intf : comp.getInterface()) {
				
				if (intf.getType().equalsIgnoreCase("url")) {

					URL url = new URL(hostname + "/" + comp.getOn() + "/" + intf.getOn());
					log(" URL : " + url.toString());
					for (Macro macro : intf.getMacros()) {

						if ("json".equalsIgnoreCase(macro.getType())) {
							URLProcessor processor = new URLProcessor(url, Constants.RECORD.JSON);
							addProcessor(comp,intf, macro, processor);
						}
					}
				} else if (intf.getType().equalsIgnoreCase("rest")) {
					URL url = new URL(hostname + "/" + comp.getOn()+ "/" + intf.getOn());
					log(" URL : " + url.toString());
					for (Macro macro : intf.getMacros()) {

						if ("get".equalsIgnoreCase(macro.getType())) {
							RESTProcessor processor = new RESTProcessor(url, Constants.RECORD.JSON, macro.getType());
							addProcessor(comp,intf, macro, processor);
						} else if ("post".equalsIgnoreCase(macro.getType())) {
							RESTProcessor processor = new RESTProcessor(url, Constants.RECORD.JSON, macro.getType());
							addProcessor(comp,intf, macro, processor);
						}
					}

				}

			}
		}

	}

	private Environment environment = null;

	private Map<String, String> eventPathMap = new HashMap<String, String>();

	public String getEventPathMap(String key) {
		return eventPathMap.get(key);
	}

}
