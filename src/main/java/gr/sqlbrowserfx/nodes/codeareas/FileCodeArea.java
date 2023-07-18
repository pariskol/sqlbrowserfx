package gr.sqlbrowserfx.nodes.codeareas;

public interface FileCodeArea {

	public boolean isTextDirty();
    public String getPath();
    public String getFileName();
	public void saveFileAction();
}
