package com.hydrides.core;

public interface Hydrides {

	void registerPath(String string, Engine engine);

	Object stream(String path, Object o) throws Exception;
}
