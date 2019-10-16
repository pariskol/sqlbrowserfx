package gr.sqlbrowserfx.restService;

import spark.ResponseTransformer;

public class StringTransformer implements ResponseTransformer {

	@Override
	public String render(Object object) throws Exception {
		return object.toString();
	}
}
