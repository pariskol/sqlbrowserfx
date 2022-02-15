package gr.sqlbrowserfx.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import io.javalin.Javalin;

public class RESTfulService {

	private static Javalin APP;
	private static int IP, PORT;
	static Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	
	public static void init(SqlConnector sqlConnector) {
		APP = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
			config.enableDevLogging();
		});
		APP.exception(Exception.class, (e, ctx) -> {
			logger.error(e.getMessage());
			ctx.status(500).result("{ \"message\":\"Oops something went wrong!\" }");
		});
		
		APP.before(ctx -> {
			ctx.res.addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
			ctx.res.addHeader("Access-Control-Allow-Origin", "*");
			ctx.res.addHeader("Access-Control-Allow-Headers", "*");
			ctx.res.addHeader("Access-Control-Allow-Credentials", "true");
			ctx.res.addHeader("Content-Type", "application/json");
		});
		
		
		APP.get("/tables", ctx-> {
			List<String> data = new ArrayList<>(sqlConnector.getTables());
			data.addAll(sqlConnector.getViews());
			ctx.result(new JSONArray(data).toString());
		});

		
		APP.post("/save/{table}", (ctx) -> {
			JSONObject jsonObject = new JSONObject(ctx.body());
			
			String table = ctx.pathParam("table");
			String columns = "";
			String values = "";
			List<Object> params = new ArrayList<>();
			
			for (String key : jsonObject.keySet()) {
				columns += key + ", ";
				values += "?, ";
				params.add(jsonObject.get(key));
			}
			
			columns = columns.substring(0, columns.length() - ", ".length());
			values = values.substring(0, values.length() - ", ".length());
			
			String query = "insert into " + table + " (" + columns
					+ ") values (" + values + ")";
			sqlConnector.executeUpdate(query, params);
			
			ctx.result("{ \"message\": \"Data has been saved\"}");
		});
		
		APP.post("/delete/{table}", (ctx) -> {
			JSONObject jsonObject = new JSONObject(ctx.body());
			
			String table = ctx.pathParam("table");
			
			List<Object> params = new ArrayList<>();
			String primaryKey = sqlConnector.findPrimaryKey(table);
			String query = "delete from  " + table + " where " + primaryKey + " = ?";
			for (String key : jsonObject.keySet()) {
				if (key.equals(primaryKey)) {
					params.add(jsonObject.get(key));
				}
			}
			sqlConnector.executeUpdate(query, params);
			ctx.result("{ message: \"Data has been deleted\"}");
		});
		
		APP.get("/get/{table}", (ctx) -> {
			String table = ctx.pathParam("table");
			if (table == null)
				throw new Exception("param 'table' is invalid");

			StringBuilder whereFilter = new StringBuilder(" where 1=1 ");
			List<Object> params = new ArrayList<>();
			
			ctx.queryParamMap().entrySet().forEach(e -> {
				whereFilter.append(" and " + e.getKey() + " = ? ");
				params.add(e.getValue().get(0));
			});

			List<Object> data = new ArrayList<>();
			try {
				logger.debug("Executing : select * from " + table + " " + whereFilter.toString()  + " , " + params.toString());
				sqlConnector.executeQuery("select * from " + table + whereFilter.toString(), params, rset -> {
					HashMap<String, Object> dto = DTOMapper.mapu(rset);
					data.add(dto);
				});
			} catch (Exception e) {
				throw e;
			}

			ctx.result(new JSONArray(data).toString());
		});

	}
	
	public static void configure(String ip, int port) {
		PORT = port;
	}
	
	public static void start() {
		APP.start(PORT);
	}
	
	public static void stop() {
		APP.stop();
	}
}