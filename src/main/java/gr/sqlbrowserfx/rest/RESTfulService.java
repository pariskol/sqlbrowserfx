package gr.sqlbrowserfx.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.javalin.plugin.bundled.CorsPluginConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import io.javalin.Javalin;

public class RESTfulService {

	private static Javalin APP;
	private static int PORT;
	private static String IP;
	static Logger logger = LoggerFactory.getLogger(LoggerConf.LOGGER_NAME);

	
	public static void init(SqlConnector sqlConnector) {
		APP = Javalin.create(config -> {
			config.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
			config.plugins.enableDevLogging();
		});
		APP.exception(Exception.class, (e, ctx) -> {
			logger.error(e.getMessage());
			ctx.status(500).result("{ \"message\":\"Oops something went wrong!\" }");
		});
		
		APP.before(ctx -> {
			ctx.res().addHeader("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
			ctx.res().addHeader("Access-Control-Allow-Origin", "*");
			ctx.res().addHeader("Access-Control-Allow-Headers", "*");
			ctx.res().addHeader("Access-Control-Allow-Credentials", "true");
			ctx.res().addHeader("Content-Type", "application/json");
		});
		
		
		APP.get("/api/tables", ctx-> {
			List<String> data = new ArrayList<>(sqlConnector.getTables());
			data.addAll(sqlConnector.getViews());
			ctx.result(new JSONArray(data).toString());
		});

		
		APP.post("/api/save/{table}", (ctx) -> {
			JSONObject jsonObject = new JSONObject(ctx.body());
			
			String table = ctx.pathParam("table");
			StringBuilder columns = new StringBuilder();
			StringBuilder values = new StringBuilder();
			List<Object> params = new ArrayList<>();
			
			for (String key : jsonObject.keySet()) {
				columns.append(key).append(", ");
				values.append("?, ");
				params.add(jsonObject.get(key));
			}
			
			columns = new StringBuilder(columns.substring(0, columns.length() - ", ".length()));
			values = new StringBuilder(values.substring(0, values.length() - ", ".length()));
			
			String query = "insert into " + table + " (" + columns
					+ ") values (" + values + ")";
			sqlConnector.executeUpdate(query, params);
			
			ctx.result("{ \"message\": \"Data has been saved\"}");
		});
		
		APP.post("/api/delete/{table}", (ctx) -> {
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
		
		APP.get("/api/get/{table}", (ctx) -> {
			String table = ctx.pathParam("table");

			StringBuilder whereFilter = new StringBuilder(" where 1=1 ");
			List<Object> params = new ArrayList<>();
			
			ctx.queryParamMap().forEach((actualParam, value1) -> {
                String value = value1.get(0);
                String actualValue = value;
                String operator = "=";
                String logic = " and ";

                if (value.startsWith(">=") || value.startsWith("<=")) {
                    operator = value.substring(0, 2);
                    actualValue = value.substring(2);
                } else if (value.startsWith(">") || value.startsWith("<")) {
                    operator = value.substring(0, 1);
                    actualValue = value.substring(1);
                }

                if (actualParam.startsWith("|")) {
                    logic = " or ";
                    actualParam = actualParam.substring(1);
                }

                whereFilter.append(logic).append(actualParam).append(operator).append("?");
                params.add(actualValue);
            });

			List<Object> data = new ArrayList<>();
            logger.debug("Executing : select * from " + table + " " + whereFilter + " , " + params);
            sqlConnector.executeQuery("select * from " + table + whereFilter, params, rset -> {
                HashMap<String, Object> dto = DTOMapper.mapUnsafely(rset);
                data.add(dto);
            });

            ctx.result(new JSONArray(data).toString());
		});

	}
	
	public static void configure(String ip, int port) {
		IP = ip;
		PORT = port;
	}
	
	public static void start() {
		APP.start(IP,PORT);
	}
	
	public static void stop() {
		APP.stop();
	}
}
