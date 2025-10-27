package gr.sqlbrowserfx.nodes.codeareas;

public interface AiProvider {
	void getAiHelp(String question);
	String getAiGeneratedCode();
}
