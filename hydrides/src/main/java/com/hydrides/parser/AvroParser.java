package com.hydrides.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.apache.avro.generic.GenericData.Record;
import org.apache.commons.beanutils.BeanMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hydrides.core.Field;
import com.hydrides.core.Stream;

public class AvroParser {

	Logger log = LoggerFactory.getLogger(AvroParser.class);
	private Schema inputSchema = null;
	private String inputType = null;
	private Schema outputSchema = null;
	private String outputType = null;

	private static Schema errorSchema = null;
	private Schema schema = null;
	static {
		FieldAssembler<Schema> fields = SchemaBuilder.record("error").namespace("error").fields();

		fields.requiredString("message");

		errorSchema = fields.endRecord();

	}

	public static Record error(String message) {
		Record record = new Record(errorSchema);
		record.put("message", message);
		return record;
	}

	public AvroParser(Stream wave) {

		String namespace = wave.getOn().replaceAll("/", ".");

		if (wave.getInput() != null && wave.getInput().getFields() != null && wave.getInput().getFields().size() > 0) {
			String input = wave.getInput().getStream() != null ? wave.getInput().getStream().replaceAll("/", "_")
					: "input";
			inputType = wave.getInput().getType();

			this.inputSchema = schema(SchemaBuilder.record(input).namespace(namespace).fields(),
					wave.getInput().getFields());
		}

		if (wave.getOutput() != null && wave.getOutput().getFields() != null
				&& wave.getOutput().getFields().size() > 0) {
			String output = wave.getOutput().getStream() != null ? wave.getOutput().getStream().replaceAll("/", "_")
					: "output";
			outputType = wave.getOutput().getType();
			this.outputSchema = schema(SchemaBuilder.record(output).namespace(namespace).fields(),
					wave.getOutput().getFields());
		}

	}

	private Schema schema(FieldAssembler<Schema> fields, List<Field> flds) {
		for (Field f : flds) {
			if ("string".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredString(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableString(f.getName().toLowerCase(), f.getValue());
				} else {
					fields.optionalString(f.getName());
				}
			} else if ("int".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredInt(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableInt(f.getName().toLowerCase(), Integer.parseInt(f.getValue()));
				} else {
					fields.optionalInt(f.getName());
				}
			} else if ("float".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredFloat(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableFloat(f.getName().toLowerCase(), Float.parseFloat(f.getValue()));
				} else {
					fields.optionalFloat(f.getName());
				}
			} else if ("double".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredDouble(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableDouble(f.getName().toLowerCase(), Double.parseDouble(f.getValue()));
				} else {
					fields.optionalDouble(f.getName());
				}
			} else if ("long".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredLong(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableLong(f.getName().toLowerCase(), Long.parseLong(f.getValue()));
				} else {
					fields.optionalString(f.getName());
				}
			} else if ("boolean".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredBoolean(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableBoolean(f.getName().toLowerCase(), Boolean.parseBoolean(f.getValue()));
				} else {
					fields.optionalBoolean(f.getName());
				}
			} else if ("bytes".equalsIgnoreCase(f.getType())) {
				if (f.isRequired()) {
					fields.requiredBytes(f.getName().toLowerCase());
				} else if (f.getValue() != null) {
					fields.nullableBytes(f.getName().toLowerCase(), f.getValue().getBytes());
				} else {
					fields.optionalBytes(f.getName());
				}
			}
		}

		return fields.endRecord();
	}

	public List<Record> output(Object o) throws Exception {
		this.schema = this.outputSchema;
		if (o instanceof InputStream) {
			return from((InputStream) o);
		} else if (o instanceof Map) {
			return from((Map) o);
		} else if (o instanceof String) {
			return from((String) o);
		} else if (o instanceof List) {
			return from((List) o);
		} else if (o instanceof Set) {
			return from((Set) o);
		} else if (o instanceof HttpServletRequest) {
			return from((HttpServletRequest) o);
		} else {
			return from(new BeanMap(o));
		}

	}

	public List<Record> input(Object o) throws Exception {
		this.schema = this.inputSchema;
		if (o instanceof InputStream) {
			return from((InputStream) o);
		} else if (o instanceof Map) {
			return from((Map) o);
		} else if (o instanceof String) {
			return from((String) o);
		} else if (o instanceof List) {
			return from((List) o);
		} else if (o instanceof Set) {
			return from((Set) o);
		} else if (o instanceof HttpServletRequest) {
			return from((HttpServletRequest) o);
		} else {
			return from(new BeanMap(o));
		}

	}

	public List<Record> from(HttpServletRequest req) throws Exception {
		log.info("PARSING HTTP STREAM : " + req.getContentType());
		if ("session".equalsIgnoreCase(inputType)) {
			HttpSession session = req.getSession(false);
			Enumeration<String> names = session.getAttributeNames();
			Map<String, Object> map = new HashMap<>();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				Object value = session.getAttribute(name);
				map.put(name, value);
			}

			log.info("SESSION DATA : " + map);
			return from(map);
		} else if ("map".equalsIgnoreCase(inputType)) {
			Map<String, Object> map = new HashMap<>();
			Map<String, String[]> params = req.getParameterMap();
			for (String key : params.keySet()) {
				map.put(key, params.get(key));
			}
			return from(map);
		} else {
			// if ("application/json".equalsIgnoreCase(req.getContentType())) {
			BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
			String json = br.readLine();
			return from(json);
		}
	}

	public List<Record> from(InputStream io) throws Exception {
		log.info("PARSING INPUTSTREAM : " + io);
		BufferedReader br = new BufferedReader(new InputStreamReader(io));
		String json = br.readLine();
		return from(json);
	}

	public Record record(JsonNode jsonNode) {
		Record record = new Record(schema);
		for (org.apache.avro.Schema.Field f : schema.getFields()) {
			if (jsonNode.has(f.name()) && jsonNode.get(f.name()) != null) {
				log.info(f.name() + "=" + jsonNode.get(f.name()).textValue() + " type=" + f.schema().getType());
				if ("int".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).intValue());
				} else if ("float".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).floatValue());
				} else if ("long".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).longValue());
				} else if ("double".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).doubleValue());
				} else if ("boolean".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).booleanValue());
				} else if ("byte".equalsIgnoreCase(f.schema().getType().name())) {
					try {
						if (jsonNode.get(f.name()) != null)
							record.put(f.name(), jsonNode.get(f.name()).binaryValue());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if ("biginteger".equalsIgnoreCase(f.schema().getType().name())) {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).bigIntegerValue());

				} else {
					if (jsonNode.get(f.name()) != null)
						record.put(f.name(), jsonNode.get(f.name()).textValue());
					else
						record.put(f.name(), "START");

				}
			}
		}
		return record;
	}

	private void jsonNode(JsonNode jsonNode, List<Record> list) {
		if (jsonNode != null) {
			if (jsonNode.isObject()) {
				list.add(record(jsonNode));
			} else {
				for (int i = 0; i < jsonNode.size(); i++) {
					JsonNode jsonObject = jsonNode.get(i);
					if (jsonNode != null)
						jsonNode(jsonObject, list);
				}
			}
		}
	}

	public List<Record> from(String json) throws Exception {

		log.info("PARSING JSON : " + json);

		ArrayList<Record> list = new ArrayList<>();
		if (json != null && !json.isEmpty()) {

			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(json);
			jsonNode(jsonNode, list);
		}
		return list;
	}

	public List<Record> from(Map<String, Object> data) throws Exception {
		log.info("PARSING MAP : " + data);
		HashSet<Map<String, Object>> setMap = new HashSet<>();
		setMap.add(data);
		return from(setMap);
	}

	public List<Record> from(Set<Map<String, Object>> list2) throws Exception {
		log.info("PARSING SET of MAP : " + list2);
		ArrayList<Record> list = new ArrayList<>();
		if (list2 != null && list2.size() > 0) {
			for (Map<String, Object> data : list2) {
				Record record = new Record(schema);
				for (String k : data.keySet()) {
					if (record.getSchema().getField(k) != null)
						if (data.get(k) != null)
							record.put(k, data.get(k));
				}
				list.add(record);
			}
		}
		return list;
	}

	public List<Record> from(List<Record> data) throws Exception {
		log.info("PARSING AVRO : " + data);
		ArrayList<Record> list = new ArrayList<>();
		if (data != null) {
			for (Record k : data) {
				List<org.apache.avro.Schema.Field> df = k.getSchema().getFields();

				Record record = new Record(schema);
				for (org.apache.avro.Schema.Field f : record.getSchema().getFields()) {
					if (df.contains(f)) {
						record.put(f.name(), record.get(f.name()));
					}
				}
				list.add(record);

			}

		}
		return list;
	}
}
