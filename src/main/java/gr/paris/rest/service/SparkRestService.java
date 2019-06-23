package gr.paris.rest.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.utils.DTOMapper;
import spark.Spark;

public class SparkRestService {

	static Logger logger = LoggerFactory.getLogger("SPARK");

	public static void init(SqlConnector sqlConnector) {
		StringTransformer stringTransformer = new StringTransformer();
		
		Spark.initExceptionHandler((e) -> logger.error(e.getMessage(), e));
		Spark.after((request, response) -> {
			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS");
			response.header("Access-Control-Allow-Headers",
					"Authorization, Origin, Accept, Content-Type, X-Requested-With");
		});
		
		Spark.get("/tables", (request, response) -> {
			List<Object> data = new ArrayList<>();
			sqlConnector.executeQuery("select name from sqlite_master where type='table'", rset -> {
				try {
					HashMap<String, Object> dto = DTOMapper.map(rset);
					data.add(dto);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					Spark.halt(500);
				}
			});
			return new JSONArray(data).toString();
		}, stringTransformer);

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
				logger.debug("Aboout to execute query");
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

			return new JSONArray(data).toString();
		}, stringTransformer);

		Spark.exception(Exception.class, (exception, request, response) -> {
			logger.error(exception.getMessage(), exception);
//			response.body("Ops something went wrong!");
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
