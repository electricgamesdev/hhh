package com.hydrides.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringBufferInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.mapred.JobHistory.RecordTypes;

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
import com.hydrides.processor.ContentProcessor;
import com.hydrides.processor.DummyProcessor;
import com.hydrides.processor.FileProcessor;
import com.hydrides.processor.FileStreamProcessor;
import com.hydrides.processor.HTMLProcessor;
import com.hydrides.processor.VelocityProcessor;

import ch.qos.logback.core.util.FileUtil;

public class HTMLEngine2 extends Engine {

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
			// html.add(new FileProcessor(htmlTemplate, Constants.RECORD.HTML));
			HTMLProcessor processor = new HTMLProcessor(htmlTemplate, Constants.RECORD.HTML);

			script.add("<script>");
			script.add(FileUtils.readFileToString(file));

			List<String> ready = new ArrayList<>();
			Map<String, String> reqJSInit = new HashMap<>();
			List<String> app = new ArrayList<>();
			// app.add("var App = {_init:function() {}");

			List<String> ufunct = new ArrayList<>();
			if (comp.getInterface() != null) {
				for (Interface intf : comp.getInterface()) {

					Wave flow = getContext().getWaves(wave_path);

					varmap.put("intf", intf);
					varmap.put("wave", comp.getWave());
					varmap.put("flow", flow);

					// group macros with on
					Map<String, List<Macro>> cmds = new TreeMap<String, List<Macro>>(String.CASE_INSENSITIVE_ORDER);

					List<String> libs = Arrays.asList("button", "link", "jquery");

					if (!libs.contains(intf.getType())) {
						reqJSInit.put("'" + intf.getType() + "'", intf.getType());
					}

					if (intf.getMacros() != null) {
						for (Macro macro : intf.getMacros()) {

							addProcessor(comp, intf, macro, processor);

							varmap.put("macro", macro);

							if (macro.getPath() != null && macro.getPath().endsWith(".js")) {
								ufunct.add(getSnippet("jquery.custom_function", varmap));
								reqJSInit.put("'" + macro.getAs() + "'", macro.getAs());
							}

							String s = getSnippet("jquery." + macro.getType(), varmap);
							if (s != null && !s.isEmpty())
								app.add(s);

							if (cmds.containsKey(macro.getOn())) {
								cmds.get(macro.getOn()).add(macro);
							} else {
								List<Macro> list = new ArrayList<>();
								list.add(macro);
								cmds.put(macro.getOn(), list);
							}

							if ("html".equalsIgnoreCase(macro.getType())) {
								// reqJSInit.put("'text!" + macro.getPath() +
								// "'", macro.getAs());
								Processor p = new HTMLProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.HTML);
								p.setParent(processor);
								macro.setOn(macro.getOn() + "/" + macro.getAs());
								addProcessor(comp, intf, macro, p);
							} else if ("velocity".equalsIgnoreCase(macro.getType())) {
								Processor p = new VelocityProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.JSON);
								p.setParent(processor);
								// macro.setOn(macro.getOn() + "/" +
								// macro.getAs());
								addProcessor(comp, intf, macro, p);
							} else if ("json".equalsIgnoreCase(macro.getType())) {
								// reqJSInit.put("'text!" + macro.getPath() +
								// "'", macro.getAs());
								Processor p = new FileProcessor(getContext().getFile(macro.getPath()),
										Constants.RECORD.JSON);
								p.setParent(processor);
								macro.setOn(macro.getOn() + "/" + macro.getAs());
								addProcessor(comp, intf, macro, p);
							}

						}
					}
					varmap.put("events", cmds);

					varmap.put("wave", wave_path);
					// add function to App
					String s = getSnippet("jquery." + intf.getType(), varmap);
					if (s != null && !s.isEmpty()) {
						app.add(s);
					}

					String r = getSnippet("jquery." + intf.getType() + ".ready", varmap);
					if (r != null) {
						ready.add(r);
					}

				}
				app.add(0, "var App = {_init:function() {" + StringUtils.join(ready, "\n") + "}");

				// varmap.put("reference", StringUtils.join(ready, "\n"));

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
				// varmap.put("ready", StringUtils.join(ready, "\n"));
				script.add(getSnippet("jquery.requirejs", varmap));
				script.add("</script>");

				// html.add(new ContentProcessor(StringUtils.join(script,
				// "\n"),
				// Constants.RECORD.HTML));
				File f = File.createTempFile("_" + comp.getAs(), ".js");
				FileUtils.writeStringToFile(f, StringUtils.join(script, "\n"), false);

				processor.addScript(f);
				// addProcessor(comp, processor);

			}
		}

	}

	public void processWave(Component comp, Interface wave, Map<String, List<Macro>> cmds, Processor main) {

		for (String event : cmds.keySet()) {

			Processor p = null;
			Macro intf = null;
			for (Macro cmp : cmds.get(event)) {
				intf = cmp;
				if ("velocity".equalsIgnoreCase(cmp.getType())) {
					p = new VelocityProcessor(getContext().getFile(cmp.getPath()), Constants.RECORD.JSON);
					addProcessor(comp, wave, cmp, p);
				} else if ("json".equalsIgnoreCase(cmp.getType())) {
					p = new FileProcessor(getContext().getFile(cmp.getPath()), Constants.RECORD.JSON);
					addProcessor(comp, wave, cmp, p);
				} else if ("xml".equalsIgnoreCase(cmp.getType())) {
					p = new FileProcessor(getContext().getFile(cmp.getPath()), Constants.RECORD.XML);
					addProcessor(comp, wave, cmp, p);
				} else if ("html".equalsIgnoreCase(cmp.getType())) {
					p = new HTMLProcessor(getContext().getFile(cmp.getPath()), Constants.RECORD.HTML);
					addProcessor(comp, wave, cmp, main);
					cmp.setOn(cmp.getOn() + "/" + cmp.getAs());
					addProcessor(comp, wave, cmp, p);
				}
			}

			if (p == null) {
				p = new DummyProcessor();
				addProcessor(comp, wave, intf, p);
			}

		}

	}

}
