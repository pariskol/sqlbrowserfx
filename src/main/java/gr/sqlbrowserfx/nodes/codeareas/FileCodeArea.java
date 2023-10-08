package gr.sqlbrowserfx.nodes.codeareas;

public interface FileCodeArea {

	boolean isTextDirty();
    String getPath();
	void saveFileAction();
}
