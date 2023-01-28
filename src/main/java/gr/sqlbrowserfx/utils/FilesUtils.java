package gr.sqlbrowserfx.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;

public class FilesUtils {

	public static Set<String> walk(String dir, String pattern, int depth) {
	    try (var stream = Files.walk(Paths.get(dir), depth)) {
	        return stream
	          .filter(file -> !Files.isDirectory(file) && file.getFileName().toString().contains(pattern))
	          .map(Path::toAbsolutePath)
	          .map(Path::toString)
	          .collect(Collectors.toSet());
	    } catch(Exception e) {
	    	LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("File search failed", e);
	    	return new HashSet<>();
	    }
	}
	
	public static Set<String> walk(String dir, String pattern) {
	    return walk(dir, pattern, 5);
	}
}
