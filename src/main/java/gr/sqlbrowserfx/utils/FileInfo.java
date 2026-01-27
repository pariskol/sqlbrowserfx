package gr.sqlbrowserfx.utils;

import java.util.List;

public class FileInfo {

	private final String name;
	private final String absolutePath;
	private final List<LineMatch> lineMatches;
	private final String rootDir;
	
	public FileInfo(String rootDir, String name, String absolutePath, List<LineMatch> lineMatches) {
		this.rootDir = rootDir;
		this.name = name;
		this.absolutePath = absolutePath;
		this.lineMatches = lineMatches;
	}

	public String getName() {
		return name;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public List<LineMatch> getLineMatches() {
		return lineMatches;
	}
	
	public Integer getMatchCount() {
		return lineMatches.size();
	}
	
	public String getRootDir() {
		return rootDir;
	}
	
	public String getRelativePath() {
		return absolutePath.substring(rootDir.length());
	}
}
