package gr.sqlbrowserfx.rest;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

	@Override
	public String render(Object object) throws Exception {
//		return object.toString();
		if (object instanceof List<?>)
			return new JSONArray((List<?>) object).toString();
		else
			return new JSONObject(object).toString();
	}

}
