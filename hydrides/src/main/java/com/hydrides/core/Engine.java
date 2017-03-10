package com.hydrides.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.avro.generic.GenericData.Record;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hydrides.core.Constants.ENGINE;
import com.hydrides.core.Constants.KIND;
import com.hydrides.parser.AvroParser;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public abstract class Engine {

	@XmlAttribute
	private Constants.KIND kind = null;
	private HydridesContext context = null;
	@XmlElement
	private Container container = null;
	private Hydrides hydrides = null;
	private ConcurrentHashMap<String, Processor> map = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Stream> wave = new ConcurrentHashMap<>();

	Logger log = LoggerFactory.getLogger(Engine.class);
	private RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
	@XmlAttribute
	private ENGINE status = ENGINE.INIT;

	protected String getSnippet(String type, String name, Object obj) {
		Map<String, Object> map = new HashMap<String, Object>();
		return getSnippet(type, map);
	}

	public void setHydrides(Hydrides hydrides) {
		this.hydrides = hydrides;
	}

	protected void log(String msg) {
		log.info(getClass().getSimpleName() + " : " + msg);
	}

	protected String getSnippet(String type, Map<String, Object> map) {
		log("Generating Glue Code :" + type + ":" + this.getClass().getResource(type));
		StringWriter sw = new StringWriter();
		SimpleNode node = null;
		try {

			InputStream input = this.getClass().getResourceAsStream(type);
			if (input == null)
				return "";
			node = runtimeServices.parse(new InputStreamReader(input), type);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Template template = new Template();
		template.setRuntimeServices(runtimeServices);
		template.setData(node);
		template.initDocument();

		VelocityContext context = new VelocityContext();
		context.put("list", new HashSet());
		for (String key : map.keySet()) {
			context.put(key, map.get(key));
		}

		template.merge(context, sw);
		return sw.toString();
	}

	private Map<String, Processor> mainComponent = new HashMap<>();

	protected void addProcessor(Component c, Processor p) {
		// Interface w = new Interface();
		// w.setOn(c.getOn());
		// addProcessor(c, w, null, p);
		mainComponent.put(c.getAs(), p);

	}

	protected void addProcessor(Component c, Interface intf, Macro macro, Processor processor) {
		Wave waves = getContext().getWaves(c.getWave());
		if (waves.getStreams() != null) {
			waves.getStreams().forEach(s -> {
				s.setWave(c.getWave());
				s.setComponent(c);
				if (macro != null) {
					if (s.getOn().equalsIgnoreCase(intf.getOn() + "/" + macro.getOn())) {
						addWave(c.getWave() + "/" + intf.getOn() + "/" + macro.getOn(), s);
					}
				} else {
					if (s.getOn().equalsIgnoreCase(intf.getOn())) {
						addWave(intf.getOn(), s);
					}
				}
			});
		}

		String p = null;
		if (macro != null) {
			p = c.getWave() + "/" + intf.getOn() + "/" + macro.getOn();
			log(" interface added : " + p);
			map.put(p, processor);
		} else {
			p = c.getWave() + "/" + intf.getOn();
			log(" interface added : " + p);
			map.put(p, processor);
		}

		this.hydrides.registerPath(p, this);
	}

	private void addWave(String key, Stream s) {
		log("Stream added " + key);
		wave.put(key, s);
	}

	public boolean build() {
		log("  **** Building Engine Started ****");
		try {
			build(context, container);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		log("     * Building Engine Completed *  ");
		return true;
	}

	public abstract void build(HydridesContext context, Container container) throws Exception;

	public HydridesContext getContext() {
		return context;
	}

	public Container getContainer() {
		return container;
	}

	public void setContainer(Container container) {
		this.container = container;
	}

	public void setContext(HydridesContext context) {
		this.context = context;
	}

	public void setKind(KIND kind) {
		this.kind = kind;
	}

	public Constants.KIND getKind() {
		return kind;
	}

	public Object stream(String path, Object data) throws Exception {

		setStatus(Constants.ENGINE.STREAMING);
		Stream flow = null;
		log("1. STREAM : " + path + " : " + data);

		// convert to the record structure
		if (wave.containsKey(path)) {

			Processor p = map.get(path);
			if (p.getParent() != null && !p.getParent().isRunning(data)) {
				p.getParent().setCallBackPath(path);
				
				log("1.1 STREAM : " + path + " RETURNING CONTAINER " + data);
				
				return p.getParent().process(null);
			}

			flow = wave.get(path);
			

			log("1.2 STREAM : " + path + " WAVE  : " + flow);
			AvroParser parser = new AvroParser(flow);
			boolean dataParsed = false;
			// if input schema available, convert it to Avro Record
			if (flow.getInput() != null) {
				if (flow.getInput().getFields() != null && flow.getInput().getFields().size() > 0) {
					dataParsed = true;
					log("2.1 STREAM : " + path + " INPUT PARSE : " + data);
					data = parser.input(data);
					log("2.2 STREAM : " + path + " INPUT PARSED : " + data);
				}
			}

			if (flow.getInput() != null && flow.getInput().getStream() != null) {
				dataParsed = false;
				log("3.1 STREAM : " + path + " INPUT STREAM >>>>>>>>>>>>> " + flow.getInput().getStream());
				String wave = null;
				if (flow.getInput().getWave() != null)
					wave = flow.getInput().getWave();
				else
					wave = flow.getWave();
				data = this.hydrides.stream(wave + "/" + flow.getInput().getStream(), data);
				log("3.2 STREAM : " + path + " INPUT STREAM > DONE " + data);
			}

			// stream to external and convert its output to avro
			if (flow.getOutput() != null) {
				if (flow.getOutput().getStream() != null) {
					dataParsed = false;
					log("4.1 STREAM : " + path + " OUTPUT STREAM < " + flow.getOutput().getStream());
					String wave = null;
					if (flow.getOutput().getWave() != null)
						wave = flow.getOutput().getWave();
					else
						wave = flow.getWave();
					data = this.hydrides.stream(wave + "/" + flow.getOutput().getStream(), data);

					log("4.2 STREAM : " + path + "  OUTPUT STREAM < " + data);
				}
				if (flow.getOutput().getFields() != null && flow.getOutput().getFields().size() > 0) {
					dataParsed = true;
					log("5.1 STREAM : " + path + "  OUTPUT PARSE : " + data);
					data = parser.output(data);
					log("5.2 STREAM : " + path + "  OUTPUT PARSED : " + data);
				}
			}
			if (dataParsed) {
				log("6.1 STREAM : " + path + "  PROCESSOR START : " + data);
				data = process(path, (List<Record>) data);
				log("6.2 STREAM : " + path + "  PROCESSOR END : " + data);
			}
		} else {
			log("6.1 STREAM : " + path + "  PROCESSOR START : " + data);
			if (data instanceof List) {
				data = process(path, (List<Record>) data);
			} else {
				data = process(path, null);
			}
			log("6.1 STREAM : " + path + "  PROCESSOR END : " + data);
			
		}

		log("<<<<<<<<<<<< STREAMING : " + data);

		setStatus(Constants.ENGINE.READY);

		return data;
	}

	private Object process(String path, List<Record> input) throws Exception {
		Object output = null;
		Processor p = map.get(path);
		if (p != null) {
			log("PATH = " + path + "    Processor  : " + p.getClass().getSimpleName());
			output = p.execute(input);

		} else {
			throw new Exception("Processor Not Found for the path : " + path);
		}
		log("PATH = " + path + "    Processor  : " + p.getClass().getSimpleName() + " OUTPUT : " + output);
		return output;
	}

	private void setStatus(ENGINE status) {
		this.status = status;
	}

	public ENGINE getStatus() {
		return status;
	}

	public Constants.RECORD getContentType(String path, Object data) {
		Processor p = map.get(path);
		if (p.getParent() != null && !p.getParent().isRunning(data)) {
			return p.getParent().getType();
		}
		return map.get(path).getType();
	}
}
