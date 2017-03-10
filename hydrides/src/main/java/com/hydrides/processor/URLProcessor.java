package com.hydrides.processor;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class URLProcessor extends Processor {

	private URL url = null;
	private Constants.RECORD type = null;
    private String method=null;
	
	public URLProcessor(URL url, Constants.RECORD type) {
		this.url = url;
		setType(type);
	}

	public Object process(List<Record> record) throws Exception {

		log(url.toString());
		URLConnection yc = url.openConnection();

		return yc.getInputStream();

	}

}
