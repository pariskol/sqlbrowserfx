package gr.sqlbrowserfx.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.utils.mapper.DTOMapper;
import spark.Spark;

public class SparkRESTfulService {

	static Logger logger = LoggerFactory.getLogger("SQLBROWSER");

	public static void init(SqlConnector sqlConnector) {
		JsonTransformer jsonTransformer = new JsonTransformer();
		
		Spark.initExceptionHandler((e) -> logger.error(e.getMessage(), e));
		Spark.after((request, response) -> {
			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
			response.header("Access-Control-Allow-Headers",
					"Authorization, Origin, Accept, Content-Type, X-Requested-With");
		});
		
		Spark.options("/*", (request, response) -> {

		    String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
		    if (accessControlRequestHeaders != null) {
		        response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
		    }

		    String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
		    if (accessControlRequestMethod != null) {
		        response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
		    }
		    return "OK";
		});
		
		Spark.get("/tables", (request, response) -> {
			List<String> data = new ArrayList<>(sqlConnector.getTables());
			data.addAll(sqlConnector.getViews());
			return data;
		}, jsonTransformer);

		
		Spark.post("/save", (request, response) -> {
			JSONObject jsonObject = new JSONObject(request.body());
			System.out.println(jsonObject.toString());
			
			String table = request.queryParams("table");
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
			
			return "{ message: \"Data has been saved\"}";
		}, jsonTransformer);
		
		Spark.post("/delete", (request, response) -> {
			JSONObject jsonObject = new JSONObject(request.body());
			
			String table = request.queryParams("table");
			
			List<Object> params = new ArrayList<>();
			String primaryKey = sqlConnector.findPrimaryKey(table);
			String query = "delete from  " + table + " where " + primaryKey + " = ?";
			for (String key : jsonObject.keySet()) {
				if (key.equals(primaryKey)) {
					params.add(jsonObject.get(key));
				}
			}
			sqlConnector.executeUpdate(query, params);
			return "{ message: \"Data has been deleted\"}";
		}, jsonTransformer);
		
		Spark.get("/get/:table", (request, response) -> {
			String table = request.params(":table");
			if (table == null)
				Spark.halt(404);

			StringBuilder whereFilter = new StringBuilder(" where ");
			List<Object> params = new ArrayList<>();
			for (String queryParam : request.queryParams()) {
				whereFilter.append(queryParam + " = ? and ");
				params.add(request.queryParams(queryParam));
			}
			if (params.size() > 0)
				whereFilter.delete(whereFilter.length() - "and ".length(), whereFilter.length());
			else
				whereFilter.delete(whereFilter.length() - " where ".length(), whereFilter.length());

			List<Object> data = new ArrayList<>();
			try {
				logger.debug("About to execute query");
				sqlConnector.executeQuery("select * from " + table + whereFilter.toString(), params, rset -> {
					try {
						HashMap<String, Object> dto = DTOMapper.map(rset);
						data.add(dto);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						Spark.halt(500);
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				Spark.halt(500);
			}

			return data;
		}, jsonTransformer);

		Spark.exception(Exception.class, (exception, request, response) -> {
			logger.error(exception.getMessage(), exception);
			response.body("Ops something went wrong!");
			Spark.halt(500);
		});
		
	}
	
	public static void configure(String ip, int port) {
		Spark.ipAddress(ip);
		Spark.port(port);
	}
	
	public static void start() {
		Spark.init();
	}
	
	public static void stop() {
		Spark.stop();
	}
}
