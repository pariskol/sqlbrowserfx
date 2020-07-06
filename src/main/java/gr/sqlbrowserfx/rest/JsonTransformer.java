package gr.sqlbrowserfx.rest;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import spark.ResponseTransformer;

public class JsonTransformer implements ResponseTransformer {

	@Override
	public String render(Object object) throws Exception {
		if (object instanceof Collection<?>)
			return new JSONArray((Collection<?>) object).toString();
		else
			return new JSONObject(object).toString();
	}

}
