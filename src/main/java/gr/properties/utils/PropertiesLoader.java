package gr.properties.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public class PropertiesLoader {

	private static HashMap<String, Properties> propertiesMap = new HashMap<>();
	
	static {
		loadProperties("./");
	}
	
	public static void loadProperties(String rootPath) {
		try {
			Files.walk(Paths.get(rootPath))
	        .filter(Files::isRegularFile)
	        .filter(path -> path.toString().endsWith(".properties"))
	        .forEach(path -> {
	        	try (InputStream inputStream = new FileInputStream(path.toFile())) {
					Properties props = new Properties();
					props.load(inputStream);
					propertiesMap.put(path.getFileName().toString(), props);
		        } catch (IOException e) {
					e.printStackTrace();
				}
	        });
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Object resolveProperty(String fileKey, String key, Class<?> clazz) {
		Object value = propertiesMap.get(fileKey).get(key);
		try {
			Constructor<?> cons = clazz.getConstructor(String.class);
			Object returnedValue = cons.newInstance(value.toString());
			return returnedValue;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
