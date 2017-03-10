package com.hydrides.processor;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.IOUtils;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class JavaProcessor extends Processor {

	private File file = null;
	private Constants.RECORD type = null;
	private String separator = System.getProperty("file.separator");
	private String classpath = System.getProperty("java.class.path");
	private String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
	private String clsName = null;
	private String method = null;

	public JavaProcessor(File file, Constants.RECORD type, String clsName, String method) {
		this.file = file;
		this.type = type;
		this.clsName = clsName;
		this.method = method;
	}

	public Object process(List<Record> record) {

		try {
			if ("main".equalsIgnoreCase(method)) {
				List<String> command = new ArrayList<>();
				command.add(path);
				command.add("-cp");
				command.add(classpath);
				command.add(clsName);

				for (Record key : record) {
					for (org.apache.avro.Schema.Field f : key.getSchema().getFields())
						command.add(key.get(f.name()).toString());

				}

				System.out.println("Command List " + command);
				ProcessBuilder processBuilder = new ProcessBuilder(command);
				Process process = processBuilder.start();
				int i = process.waitFor();

				InputStream error = process.getErrorStream();
				String e = IOUtils.toString(error);
				if (e != null && !e.isEmpty()) {
					System.err.println(e);
				} else {
					InputStream input = process.getInputStream();
					ObjectInputStream inputStream = new ObjectInputStream(input);
					Object obj = inputStream.readObject();
					inputStream.close();
					
					return new BeanMap(obj);
				}
//			} else if ("return".equalsIgnoreCase(method)) {
//				boolean dataavailable = true;
//				String fields = "";
//				for (Field k : record.getFields()) {
//					if (record.get(k.getName()) == null) {
//						dataavailable = false;
//						fields = fields + k + " ";
//					}
//				}
//				if (dataavailable)
//					return Record2.error(record.getName(), "Data not found for fields : " + fields);
			} else {

				System.out.println("calling method " + method + "(" + record.size() + ")");
				Class cls = Class.forName(this.clsName);
				Class[] params = null;
				Object[] values = null;
				if (record.size() > 0) {
					Record r = record.get(0);
					params = new Class[r.getSchema().getFields().size()];
					values = new Object[r.getSchema().getFields().size()];
					
					int i = 0;
					for (org.apache.avro.Schema.Field f : r.getSchema().getFields()) {
						params[i] = Class.forName(javaType(f.schema().getType().name()));
						values[i] = r.get(f.name());
						i++;
					}
				}

				Method mth = cls.getMethod(method, params);
				if (mth != null) {
					Object obj = cls.newInstance();
					Object object = mth.invoke(obj, values);
					return object;
				}
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	private String javaType(String type) {
		if ("string".equalsIgnoreCase(type)) {
			return "java.lang.String";
		} else if ("integer".equalsIgnoreCase(type)) {
			return "java.lang.Integer";
		} else if ("long".equalsIgnoreCase(type)) {
			return "java.lang.Long";
		} else if ("float".equalsIgnoreCase(type)) {
			return "java.lang.Float";
		} else if ("double".equalsIgnoreCase(type)) {
			return "java.lang.Double";
		} else if ("decimal".equalsIgnoreCase(type)) {
			return "java.lang.BigDecimal";
		} else if ("byte".equalsIgnoreCase(type)) {
			return "java.lang.Byte";
		} else if ("boolean".equalsIgnoreCase(type)) {
			return "java.lang.Boolean";
		}
		return type;
	}

}
