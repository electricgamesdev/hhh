package com.hydrides.parser;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hydrides.core.Constants;
import com.hydrides.core.Constants.RECORD;

public class Streamer extends SequenceInputStream{

	private Constants.RECORD type = RECORD.JSON;
	
	
	public Streamer(InputStream e,RECORD type)  {
		super(Collections.enumeration(Arrays.asList(e)));
		this.type=type;
	}
	
	public Streamer(List<? extends InputStream> e,RECORD type)  {
		super(Collections.enumeration(e));
		this.type=type;
	}
	
	public Constants.RECORD getType() {
		return type;
	}

}
