package com.hydrides.processor;

import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;
import com.hydrides.parser.Streamer;

public class VelocityProcessor extends Processor {

	private File file = null;
	private Constants.RECORD type = null;
	private RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();

	public VelocityProcessor(File file, Constants.RECORD type) {
		this.file = file;
		setType(type);
	}

	public Object process(List<Record> record) throws Exception {
		StringWriter sw = new StringWriter();

		SimpleNode node = runtimeServices.parse(new FileReader(file), "velocity");

		Template template = new Template();
		template.setRuntimeServices(runtimeServices);
		template.setData(node);
		template.initDocument();
		System.out.println(record);
		VelocityContext context = new VelocityContext();

		context.put("record", record);

		template.merge(context, sw);
		log(sw.toString());
		return new Streamer(IOUtils.toInputStream(sw.toString()), type);
	}

}
