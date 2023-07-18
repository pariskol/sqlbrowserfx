package gr.sqlbrowserfx.nodes;

import java.io.File;

public class TreeViewFile extends File {

	private static final long serialVersionUID = 1L;

	public TreeViewFile(String pathname) {
		super(pathname);
	}
	

	public File asFile() {
		return (File) this;
	}
	
	@Override
	public String toString() {
		return super.getName();
	}
}
