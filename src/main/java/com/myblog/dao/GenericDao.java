package com.myblog.dao;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.myblog.model.User;
import com.myblog.util.DaoUtil;

public class GenericDao<T> extends BaseDao {
	private static Logger logger = LoggerFactory.getLogger(GenericDao.class);
	private Class<T> typeClz;
	private String clzName;
	private Field[] clzFields;
	private Field idField;
	
	public GenericDao(Class<T> clz) {
		this.typeClz = clz;
		init();
	}
	
	private void init() {
		clzName = typeClz.getSimpleName();
		clzFields = typeClz.getDeclaredFields();
		
		filterDbField();
		
		try {
			Class supClz = typeClz.getSuperclass();
			if (supClz != null) {
				idField = supClz.getDeclaredField("id");
				idField.setAccessible(true);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private void filterDbField() {
		List<Field> fldLst = new ArrayList<Field>();
		
		for (Field fld : clzFields) {
			if (isDbDataType(fld.getType())) {
				fldLst.add(fld);
			}
		}
		clzFields = fldLst.toArray(new Field[fldLst.size()]);
	}
	
	public int add(T t) {
		String[] colNames = new String[clzFields.length];
		Object[] colVals = new Object[clzFields.length];
		for (int i = 0; i < clzFields.length; i++) {
			Field fld = clzFields[i];
			colNames[i] = fld.getName();
			fld.setAccessible(true);
			try {
				colVals[i] = fld.get(t); 
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		String colName = join(colNames);
		Arrays.fill(colNames, "?");
		String sql = String.format("insert into %s (%s) values(%s)",clzName, colName , join(colNames));
		
		return executeUpdate(sql, colVals);
	}
	
	private String buidSqlInsert(T t) {
		String[] colNames = new String[clzFields.length];
		Object[] colVals = new Object[clzFields.length];
		for (int i = 0; i < clzFields.length; i++) {
			Field fld = clzFields[i];
			colNames[i] = fld.getName();
			fld.setAccessible(true);
			try {
				colVals[i] = fld.get(t); 
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		return String.format("insert into %s (%s) values(%s)",clzName,  join(colNames), join(colVals));
	}
	
	private String buidSqlUpdate(T t) {
		return String.format("update %s set dd = dd, dd= dd where dd = 1", clzName);
	}
	
	private static <E, F> String join(E[] keys, F[] values) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < keys.length; i++) {
			sb.append(keys[i])
				.append("=")
				.append(i < values.length ? values[i] : "null")
				.append(",");
		}
		if (sb.toString().endsWith(",")) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}
	
	private static <E> String join(E... es) {
		StringBuffer sb = new StringBuffer();
		for (E e : es) {
			sb.append(e).append(",");
		}
		
		if (sb.toString().endsWith(",")) {
			sb.deleteCharAt(sb.length() - 1);
		}
		
		return sb.toString();
	}
	
	public int delete(int id) {
		String sql = String.format("delete from %s where id = ?", clzName);
		return executeUpdate(sql, id);
	}
	

	public T get() {
		String sql = "select * from user where id = dd and dd = dd";
		
		return null;
	}
	
	public T getOneBy(String colName, Object colVal) {
		List<T> ts = getBy(colName, colVal);
		if (ts.size() > 0) {
			return ts.get(0);
		}
		return null;
	}
	
	public List<T> getBy(String colName, Object colVal) {
		String sql = String.format("select * from %s where %s = ?", clzName, colName);
		ResultSet rs = executeQuery(sql, colVal);
		return parseResultSet(rs);
	}
	
	private List<T> parseResultSet(ResultSet rs) {
		List<T> retVal = new ArrayList<T>();
		try {
			while(rs.next()) {
				try {
					T instance = typeClz.newInstance();
					if (idField != null && findColumn(rs, "id") != -1) {
						idField.set(instance, rs.getObject("id"));
					}
					
					for (Field fld : clzFields) {
		//				if (isDbDataType(fld.getType()) == false) continue;
						try {
							int colIdx = findColumn(rs, fld.getName());
							if (colIdx == -1) continue;
							fld.setAccessible(true);
							Object val = rs.getObject(colIdx);
							if (fld.getType() == Date.class && val != null) {
								val = DaoUtil.parseDateString(val.toString());
							}
							fld.set(instance, val);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					retVal.add(instance);
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		} catch (Exception e) {
			logger.error("", e);
		}
		return retVal;
	}
	
	public List<T> getList() {
		String sql = "select * from " + clzName;
		ResultSet rs = executeQuery(sql);
		return parseResultSet(rs);
	}
	
	public boolean reCreateTable() {
		dropTable();
		return createTable();
	}
	
	public boolean createTable() {
		String sql = buidSqlCreate();
		if (sql == null) return false;
		return execute(sql);
	}
	
	private String buidSqlCreate() {
		int fildLen = clzFields.length;
		if (fildLen == 0) return null;
		String[] colNames = new String[fildLen];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < fildLen; i++) {
			Field fld = clzFields[i];
			String dbType = getDbDataType(fld.getType());
			if (dbType != null) {
				colNames[i] = fld.getName();
				sb.append(fld.getName());
				sb.append(" ");
				sb.append(dbType);
				sb.append(",");
			}
		}
		if (sb.length() != 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return String.format("create table if not exists %s " +
				"(id integer primary key autoincrement, %s )", clzName, sb.toString());
	}
	
	private String getDbDataType(Class clz) {
		String type = null;
		if (clz.isPrimitive()) {
			type = clz.getSimpleName();
		} else if (clz == String.class) {
			type = "varchar(1024)";
		} else if (clz == Date.class) {
			type = "datetime";
		}
		
		return type;
	}
	
	private boolean isDbDataType(Class clz) {
		return getDbDataType(clz) != null;
	}
	
	public void dropTable() {
		String sql = "drop table if exists " + clzName;
		execute(sql);
	}
	

	public static void main(String[] aregs) {
		GenericDao<User> dao = new GenericDao<User>(User.class);
		boolean res = dao.reCreateTable();
		System.out.println(res);
	}
}
