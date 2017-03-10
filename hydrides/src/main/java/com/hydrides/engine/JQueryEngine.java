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

public class JQueryEngine extends Engine {

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
			HTMLProcessor processor = new HTMLProcessor(htmlTemplate,Constants.RECORD.HTML);
			script.add("<script>");
			script.add(FileUtils.readFileToString(file));

			List<String> dynahtml = new ArrayList<>();
			List<String> dynahtmlvar = new ArrayList<>();
			List<String> dynajs = new ArrayList<>();
			List<String> dynajsvar = new ArrayList<>();

			List<String> ready = new ArrayList<>();

			List<String> app = new ArrayList<>();
			// app.add("var App = {_init:function() {}");

			List<String> ufunct = new ArrayList<>();
			if (comp.getInterface() != null)
				for (Interface intf : comp.getInterface()) {

					Wave f2 = getContext().getWaves(wave_path);

					varmap.put("form", intf);
					varmap.put("wave", comp.getWave());
					varmap.put("formdef", f2);

					String mth = comp.getWave().replaceAll("/", "_") + "_" + intf.getOn();

					Map<String, List<Macro>> cmds = new TreeMap<String, List<Macro>>(String.CASE_INSENSITIVE_ORDER);

					List<String> types = Arrays.asList("ajax");

					if (intf.getMacros() != null)
						for (Macro macro : intf.getMacros()) {

							// if (intf.getPath() != null) {
							// html.add(new FileProcessor(context.getFile(intf),
							// Constants.RECORD.JS));
							// }

							if (cmds.containsKey(macro.getOn())) {
								cmds.get(macro.getOn()).add(macro);
							} else {
								List<Macro> list = new ArrayList<>();
								list.add(macro);
								cmds.put(macro.getOn(), list);
							}
						}

					varmap.put("events", cmds);

					for (String evnt : cmds.keySet()) {
						String function_return = "null";
						for (Macro i : cmds.get(evnt)) {
							varmap.put("event", evnt);
							varmap.put("comps", cmds.get(evnt));
							varmap.put("comp", i);
							if ("function".equalsIgnoreCase(i.getType())) {
								// script.add(getSnippet("jquery.comp." +
								// c.getType().toLowerCase(), varmap));
								ufunct.add(getSnippet("jquery." + i.getType().toLowerCase(), varmap));
								dynajs.add("'" + i.getAs() + "'");
								dynajsvar.add(i.getAs());
								// function_return = i.getAs();
								// } else if
								// ("button".equalsIgnoreCase(i.getType())) {
								// ready.add(getSnippet("jquery.comp." +
								// i.getType().toLowerCase(), varmap));
							} else if ("html".equalsIgnoreCase(i.getType())) {
								dynahtml.add("'text!" + i.getPath() + "'");
								dynahtmlvar.add(i.getAs());
							}
						}

						if ("ready".equalsIgnoreCase(evnt)) {
							if ("datatable".equalsIgnoreCase(intf.getType()))
								ready.add("App." + mth + "_" + evnt + "_init(null);");
							else
								ready.add("App." + mth + "_" + evnt + "(null);");
						} else if ("button".equalsIgnoreCase(intf.getType())) {
							ready.add("App." + mth + "_" + evnt + "(null);");
						}

					}

					String s = getSnippet("jquery." + intf.getType(), varmap);
					if (s != null && !s.isEmpty())
						app.add(s);

					processWave(comp, intf, cmds, processor);

				}

			app.add(0, "var App = {_init:function() {" + StringUtils.join(ready, "\n") + "}");

			varmap.put("dynahtml", dynahtml.size() > 0 ? "," + StringUtils.join(dynahtml, ",") : " ");
			varmap.put("dynahtmlvar", dynahtmlvar.size() > 0 ? "," + StringUtils.join(dynahtmlvar, ",") : " ");
			varmap.put("dynajs", dynajs.size() > 0 ? "," + StringUtils.join(dynajs, ",") : " ");
			varmap.put("dynajsvar", dynajsvar.size() > 0 ? "," + StringUtils.join(dynajsvar, ",") : " ");

			String app_funct = "";
			for (String s : dynajsvar) {
				app_funct = app_funct + "App." + s + "_=" + s + ";\n";
			}
			varmap.put("app_funct", app_funct);
			app.add(getSnippet("jquery.app", varmap));
			app.add("};");
			script.add(StringUtils.join(app, ",\n"));
			script.add(StringUtils.join(ufunct, "\n"));
			// varmap.put("ready", StringUtils.join(ready, "\n"));
			script.add(getSnippet("jquery.requirejs", varmap));
			script.add("</script>");

			// html.add(new ContentProcessor(StringUtils.join(script, "\n"),
			// Constants.RECORD.HTML));
			File f = File.createTempFile("_" + comp.getOn(), ".js");
			FileUtils.writeStringToFile(f, StringUtils.join(script, "\n"), Charset.defaultCharset());
//			processor.addScript(new FileInputStream(f));
			addProcessor(comp, processor);
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
					p = new  HTMLProcessor(getContext().getFile(cmp.getPath()), Constants.RECORD.HTML);
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
