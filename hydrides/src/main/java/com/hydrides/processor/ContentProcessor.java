package com.hydrides.processor;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Constants;
import com.hydrides.core.Processor;

public class ContentProcessor extends Processor {

	private String content = null;
	private Constants.RECORD type = null;

	public ContentProcessor(String content, Constants.RECORD type) {
		this.content = content;
		this.type = type;
	}

	public Object process(List<Record> record) {

		return new ByteArrayInputStream(content.getBytes());

	}

}
