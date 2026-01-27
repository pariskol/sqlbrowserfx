package gr.sqlbrowserfx.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;

public class FilesUtils {

	public static List<FileInfo> walkContentsWithLines(String dir, String regex, String extension, int depth) {
	    var pattern = Pattern.compile(regex);
	    var results = new ArrayList<FileInfo>();

	    try (var stream = Files.walk(Paths.get(dir), depth)) {
	        stream
		        .filter(file -> {
	                try {
	                    return !Files.isHidden(file); // skip hidden files
	                } catch (IOException e) {
	                    return false;
	                }
	            })
	            .filter(Files::isRegularFile) // skip directories
	            .filter(file -> extension.isEmpty() ? true : file.getFileName().toString().toLowerCase().endsWith(extension.toLowerCase()))
	            .forEach(file -> {
	                var matchingLines = new ArrayList<LineMatch>();
	                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
	                    String line;
	                    int lineNumber = 1;
	                    while ((line = reader.readLine()) != null) {
	                        if (pattern.matcher(line).find()) {
	                            matchingLines.add(
                            		new LineMatch(
                        				lineNumber, 
                        				line.trim()
									)
	                            );
	                        }
	                        lineNumber++;
	                    }
	                } catch (IOException | UncheckedIOException e) {
	                    // skip unreadable/binary files
	                }

	                if (!matchingLines.isEmpty()) {
							results.add(
								new FileInfo(
									dir,
									file.getFileName().toString(), 
									file.toAbsolutePath().toString(),
									matchingLines
								)
							);
	                }
	            });
	    } catch (Exception e) {
	        LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error("File search failed", e);
	    }

	    return results;
	}

	public static Set<String> walk(String dir, String pattern, int depth) {
	    try (var stream = Files.walk(Paths.get(dir), depth)) {
	        return stream
	          .filter(file -> !Files.isDirectory(file) && file.getFileName().toString().toLowerCase().contains(pattern.toLowerCase()))
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
