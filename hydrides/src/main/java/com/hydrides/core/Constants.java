package com.hydrides.core;

public interface Constants {
	enum RESOURCE {
		CONFIG, DOMAINS, ROOT, JTA
	}

	enum KIND {
		PROFILE, PROCESS, PROTOTYPE
	}

	enum RECORD {
		HTML, JSON, XML, TUPLE, MAP, LIST, EMPTY,JS, POJO
	}

	enum ENGINE {
		STREAMING, READY, ERROR, STOPPED, INIT
	}
}
