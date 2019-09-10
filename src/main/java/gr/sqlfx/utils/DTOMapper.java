package gr.sqlfx.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;



public class DTOMapper {

	/**
	 * Maps a row of result set into a HashMap.
	 * 
	 * @param rset
	 * @param clazz
	 * @return hashMap
	 * @throws Exception
	 */
	public static LinkedHashMap<String, Object> map(ResultSet rset) throws Exception {

		LinkedHashMap<String, Object> dto = new LinkedHashMap<>();
		ResultSetMetaData rsmd = rset.getMetaData();
		
		for (int i=1;i<= rsmd.getColumnCount();i++) {
			Object value = dto.get(rsmd.getColumnName(i));
			if (value != null)
				dto.put(rsmd.getColumnName(i)+" ("+rsmd.getTableName(i)+")", rset.getObject(i));
			else dto.put(rsmd.getColumnName(i), rset.getObject(i));
		}
		
		return dto;
	}

	/**
	 * Maps a row of result set in a given class (which has @DTO annotation and its fields have @Column annotation).
	 * Database's column types and class fields must be type compatible.
	 * 
	 * @param rset
	 * @param clazz
	 * @return
	 * @throws SQLException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static Object map(ResultSet rset, Class<?> clazz) throws SQLException, IllegalAccessException, InstantiationException{

		if (clazz.getAnnotation(DTO.class) == null)
			throw new IllegalAccessException(
					"Class " + clazz.getSimpleName() + " has no annotation " + DTO.class.getName());

		Object dto = clazz.newInstance();

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			Annotation annotation = field.getAnnotation(Column.class);
			if (annotation != null) {
				// get object from result set and cast it to field's class
				Object value = rset.getObject(((Column) annotation).value(), field.getType());
				field.set(dto, value);
			}
		}

		return dto;
	}
}