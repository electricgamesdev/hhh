package com.hydrides.processor;

import java.util.List;

import org.apache.avro.generic.GenericData.Record;

import com.hydrides.core.Processor;

public class DummyProcessor extends Processor {

	public DummyProcessor() {
	}

	public Object process(List<Record> record) {

		return record;

	}

}
