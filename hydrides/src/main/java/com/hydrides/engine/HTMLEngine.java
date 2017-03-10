package com.hydrides.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.hydrides.core.Component;
import com.hydrides.core.Constants;
import com.hydrides.core.Container;
import com.hydrides.core.Engine;
import com.hydrides.core.HydridesContext;
import com.hydrides.core.Interface;
import com.hydrides.core.Macro;
import com.hydrides.core.Processor;
import com.hydrides.core.Stream;
import com.hydrides.core.Wave;
import com.hydrides.processor.FileProcessor;
import com.hydrides.processor.HTMLProcessor;
import com.hydrides.processor.VelocityProcessor;

public class HTMLEngine extends Engine {

	@Override
	public void build(HydridesContext context, Container container) throws Exception {

		Map<String, Object> varmap = new HashMap<String, Object>();
		varmap.put("context", context);

		File file = context.getFile(container.getPath());

		for (Component comp2 : container.getComponent()) {

			Component comp = context.getComponent(comp2.getPath());
			String wave_path = comp.getWave();
			log("Loading component: " + comp2.getPath() + " wave :" + wave_path);
			// List<Processor> html = new ArrayList<Processor>();
			File htmlTemplate = context.getFile(comp.getPath());
			List<String> script = new ArrayList<String>();

			varmap.put("context", context);
			varmap.put("wave", wave_path);

			HTMLProcessor processor = new HTMLProcessor(htmlTemplate, Constants.RECORD.HTML);

			script.add("<script>");
			script.add(FileUtils.readFileToString(file));

			List<String> ready = new ArrayList<>();
			Map<String, String> reqJSInit = new HashMap<>();
			List<String> app = new ArrayList<>();

			List<String> ufunct = new ArrayList<>();
			if (comp.getInterface() != null) {
				for (Interface intf : comp.getInterface()) {

					Wave flow = getContext().getWaves(wave_path);

					varmap.put("intf", intf);
					varmap.put("wave", comp.getWave());
					varmap.put("flow", flow);

					// group macros with on
					Map<String, List<Macro>> events = new TreeMap<String, List<Macro>>(String.CASE_INSENSITIVE_ORDER);

					List<String> libs = Arrays.asList("button", "link", "jquery");

					if (!libs.contains(intf.getType())) {
						reqJSInit.put("'" + intf.getType() + "'", intf.getType());
					}

					if (intf.getMacros() != null) {
						for (Macro macro : intf.getMacros()) {

							if (!events.containsKey(macro.getOn())) {
								List list = new ArrayList<>();
								list.add(macro);
								events.put(macro.getOn(), list);
							} else {
								events.get(macro.getOn()).add(macro);
							}

							addProcessor(comp, intf, macro, processor);

							varmap.put("events", events);
							varmap.put("macro", macro);

							if ("function".equalsIgnoreCase(macro.getType())
									|| "ajax".equalsIgnoreCase(macro.getType())) {
								ufunct.add(getSnippet("jquery.custom_function", varmap));
								reqJSInit.put("'" + intf.getOn() + "_" + macro.getOn() + "_" + macro.getType() + "'",
										intf.getOn() + "_" + macro.getOn() + "_" + macro.getType());
							}

							String s = getSnippet("jquery." + macro.getType(), varmap);
							if (s != null && !s.isEmpty())
								app.add(s);

							Processor childProcessor = null;
							if ("html".equalsIgnoreCase(macro.getType())) {
								childProcessor = new HTMLProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.HTML);

							} else if ("velocity".equalsIgnoreCase(macro.getType())) {
								childProcessor = new VelocityProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.JSON);

							} else if ("json".equalsIgnoreCase(macro.getType())) {
								childProcessor = new FileProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.JSON);

							}

							if (childProcessor != null) {
								childProcessor.setParent(processor);
								addProcessor(comp, intf, macro, childProcessor);
							}
						}
					}

					// add function to App
					String s = getSnippet("jquery." + intf.getType(), varmap);
					if (s != null && !s.isEmpty()) {
						app.add(s);
					}

					if ("button".equalsIgnoreCase(intf.getType()) || "button".equalsIgnoreCase(intf.getType())) {
						String r = getSnippet("jquery." + intf.getType() + ".ready", varmap);
						if (r != null && !s.isEmpty()) {
							ready.add(r);
						}
					}

					if (flow.getStreams() != null) {
						for (Stream str : flow.getStreams()) {
							if (intf.getOn().equalsIgnoreCase(str.getOn())) {
								addProcessor(comp, intf, null, processor);
								String r = getSnippet("jquery." + intf.getType() + ".ready", varmap);
								if (r != null) {
									ready.add(r);
								}
							}
						}
					}

				}
				app.add(0, "var App = {_init:function() {" + StringUtils.join(ready, "\n") + "}");

				// Application function
				String app_funct = "";
				for (String s : reqJSInit.values()) {
					app_funct = app_funct + "App." + s + "_=" + s + ";\n";
				}
				varmap.put("functions", app_funct);
				varmap.put("references", StringUtils.join(reqJSInit.keySet(), ","));
				varmap.put("variables", StringUtils.join(reqJSInit.values(), ","));

				app.add(getSnippet("jquery.app", varmap));
				app.add("};");
				script.add(StringUtils.join(app, ",\n"));
				script.add(StringUtils.join(ufunct, "\n"));
				script.add(getSnippet("jquery.requirejs", varmap));
				script.add("</script>");

				File f = File.createTempFile("_" + comp.getAs(), ".js");
				FileUtils.writeStringToFile(f, StringUtils.join(script, "\n"), false);

				processor.addScript(f);

			}
		}

	}

}
