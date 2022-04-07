package gr.sqlbrowserfx.utils.mapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.LinkedHashMap;



public class DTOMapper {

	/**
	 * Maps a row of result set into a HashMap. 
	 * For mapping columns aliases are used.
	 * 
	 * @param rset
	 * @return LinkedHashMap
	 * @throws Exception
	 */
	public static LinkedHashMap<String, Object> map(ResultSet rset) throws Exception {

		LinkedHashMap<String, Object> dto = new LinkedHashMap<>();
		ResultSetMetaData rsmd = rset.getMetaData();
		
		for (int i=1;i<= rsmd.getColumnCount();i++) {
			Object value = dto.get(rsmd.getColumnLabel(i));
			if (value != null)
				dto.put(rsmd.getTableName(i) + "." + rsmd.getColumnLabel(i), rset.getObject(i));
			else dto.put(rsmd.getColumnLabel(i), rset.getObject(i));
		}
		
		return dto;
	}

	
	/**
	 * Maps a row of result set into a HashMap. 
	 * For mapping columns real names are used.
	 * 
	 * @param rset
	 * @return LinkedHashMap
	 * @throws Exception
	 */
	public static LinkedHashMap<String, Object> mapUsingRealColumnNames(ResultSet rset) throws Exception {

		LinkedHashMap<String, Object> dto = new LinkedHashMap<>();
		ResultSetMetaData rsmd = rset.getMetaData();
		
		for (int i=1;i<= rsmd.getColumnCount();i++) {
			Object value = dto.get(rsmd.getColumnName(i));
			if (value != null)
				dto.put(rsmd.getTableName(i) + "." + rsmd.getColumnName(i), rset.getObject(i));
			else dto.put(rsmd.getColumnName(i), rset.getObject(i));
		}
		
		return dto;
	}
	
	/**
	 * 
	 * @param rset
	 * @return HashMap
	 * @throws RuntimeException
	 */
	public static HashMap<String, Object> mapUnsafely(ResultSet rset) throws RuntimeException {

		LinkedHashMap<String, Object> dto = null;
		try {
			dto = map(rset);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return dto;
	}
	
	public static Object mapu(ResultSet rset, Class<?> clazz) {
		Object dto = null;
		try {
			dto = map(rset, clazz);
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage(), e);
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
	 * @throws Exception 
	 */
	public static Object map(ResultSet rset, Class<?> clazz) throws Exception {

		if (clazz.getAnnotation(DTO.class) == null)
			throw new IllegalAccessException(
					"Class " + clazz.getSimpleName() + " has no annotation " + DTO.class.getName());

		Object dto = clazz.getDeclaredConstructor().newInstance();

		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			Annotation annotation = field.getAnnotation(Column.class);
			if (annotation != null) {
				// get object from result set and cast it to field's class
				Object value = null;
				try {
					value = rset.getObject(((Column) annotation).value(), field.getType());
				} catch(Exception e) {
					// getObject(name, class) may be unsupported in some jdbc drivers (ex sqlite)
					value = rset.getObject(((Column) annotation).value());
				}
				field.set(dto, value);
			}
		}

		return dto;
	}
}