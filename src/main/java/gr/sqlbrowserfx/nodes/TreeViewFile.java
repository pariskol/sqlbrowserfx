package gr.sqlbrowserfx.nodes;

import java.io.File;
import java.io.Serial;

public class TreeViewFile extends File {

	@Serial
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
