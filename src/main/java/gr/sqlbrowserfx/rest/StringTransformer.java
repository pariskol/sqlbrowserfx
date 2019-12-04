package gr.sqlbrowserfx.rest;

import spark.ResponseTransformer;

public class StringTransformer implements ResponseTransformer {

	@Override
	public String render(Object object) throws Exception {
		return object.toString();
	}
}
