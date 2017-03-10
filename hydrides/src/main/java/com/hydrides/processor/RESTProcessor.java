package com.hydrides.processor;

import java.net.URL;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class RESTProcessor<K, V> extends Processor {

	private URL url = null;
	private Constants.RECORD type = null;
	private String method = null;

	public RESTProcessor(URL url, Constants.RECORD type, String method) {
		this.url = url;
		setType(type);
		this.method = method;
	}

	public Object process(List<Record> record) throws Exception {
		ClientConfig config = new DefaultClientConfig();

		Client client = Client.create(config);
		WebResource webResource = client.resource(UriBuilder.fromUri(url.toURI()).build());
		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
		if (record != null && record.size() > 0) {
			Record rec = record.get(0);
			for (org.apache.avro.Schema.Field f : rec.getSchema().getFields()) {
				if (rec.get(f.name()) != null) {
					formData.add(f.name(), rec.get(f.name()).toString());
				}
			}
		}
		ClientResponse response = null;
		if ("post".equalsIgnoreCase(method)) {
			l("POST : " + formData.toString());
			response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class,
					formData);

		} else if ("get".equalsIgnoreCase(method)) {
			l("GET : " + formData.toString());
			response = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).get(ClientResponse.class);
		}
		return response.getEntity(String.class);
	}

	public static void main(String[] args) throws Exception {
		RESTProcessor rest = new RESTProcessor(new URL("http://localhost:8080/oxygenweb/hydrogen"),
				Constants.RECORD.JSON, "get");
		Object obj = rest.process(null);
		System.out.println("object = .." + obj);
	}

}
