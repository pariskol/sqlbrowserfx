package gr.sqlbrowserfx.rest.service;

public class RestServiceConfig {

	private String ip;
	private Integer port;
	private String database;
	
	public RestServiceConfig(String ip, Integer port, String database) {
		this.ip = ip;
		this.port = port;
		this.database = database;
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public String getDatabase() {
		return database;
	}
	public void setDatabase(String database) {
		this.database = database;
	}
	
	
}