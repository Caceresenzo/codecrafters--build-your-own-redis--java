package redis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
	
	private final Map<String, Object> map = new ConcurrentHashMap<>();
	
	public void set(String key, Object value) {
		map.put(key, value);
	}
	
	public Object get(String key) {
		return map.get(key);
	}

}