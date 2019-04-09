package com.sbatis.core;

import com.sbatis.config.response.result.PageResultInfo;
import com.sbatis.core.constant.CoreCommonStants;
import com.sbatis.core.constant.type.PageEnum;
import com.sbatis.core.exception.HandleException;
import com.sbatis.core.mapper.BaseBeanMapper;
import com.sbatis.core.mapper.BaseResultMapper;
import com.sbatis.core.mapper.factory.BeanMapperFactory;
import com.sbatis.core.mapper.factory.ResultMapperFactory;
import com.sbatis.core.sql.QueryProvider;
import com.sbatis.core.sql.SqlProvider;
import com.sbatis.core.util.CacheInfoConstant;
import com.sbatis.validate.ValidateTool;
import org.apache.ibatis.session.SqlSession;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础BaseDAO
 * @author HuangLongPu
 * @param <T>
 */
@SuppressWarnings("deprecation")
public abstract class BaseDAO<T extends BaseCommonEntity> {

	private Class<T> cls;
	private String tableName;
	private String canonicalName;
	private BaseBeanMapper<T> mapper;
	@Resource
	private SqlSession sqlSession;

	@SuppressWarnings("unchecked")
	private BaseBeanMapper<T> getMapper() {

		if (cls == null) {
			getCls();
		}
		if (mapper != null) {
			return mapper;
		}
		mapper = (BaseBeanMapper<T>) BeanMapperFactory.getMapper(sqlSession, canonicalName);
		return mapper;
	}

	@SuppressWarnings("unchecked")
	private <M> BaseResultMapper<M> getResultMapper(Class<M> resultCls) {
		if (resultCls == null) {
			throw new HandleException("error: Class<M> resultCls is null !!!");
		}

		Map<String, BaseResultMapper<M>> resultMapperMap = new HashMap<String, BaseResultMapper<M>>();
		if (resultMapperMap.containsKey(resultCls.getCanonicalName())) {
			return resultMapperMap.get(resultCls.getCanonicalName());
		}

		BaseResultMapper<M> resultMapper = (BaseResultMapper<M>) ResultMapperFactory.getMapper(sqlSession, resultCls);
		resultMapperMap.put(resultCls.getCanonicalName(), resultMapper);
		return resultMapper;
	}

	@SuppressWarnings("unchecked")
	private void getCls() {
		cls = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		canonicalName = cls.getCanonicalName();
	}

	public String getTableName() throws HandleException {

		if (cls == null) {
			getCls();
		}
		if (ValidateTool.isEmpty(tableName)) {
			String clsName = cls.getCanonicalName();
			if (CacheInfoConstant.TABLE_CACHE.containsKey(clsName)) {
				tableName = CacheInfoConstant.TABLE_CACHE.get(clsName);
			}
		}
		return tableName;
	}

	/**
	 * 单个添加，传入一个BaseEntity对象，并返回影响行数
	 * @author HuangLongPu
	 * @param t
	 * @return
	 */
	public int insert(T t) throws HandleException {
		if (!(t instanceof BaseCommonEntity)) {
			throw new HandleException("error: the entity is not instanceof BaseCommonEntity!!!");
		}
		return this.getMapper().insert(t, getTableName(), cls);
	}

	/**
	 * 批量添加，传入一个list集合的BaseEntity对象，并返回影响行数
	 * @param list
	 * @return
	 */
	public int insertBatch(List<T> list) throws HandleException {
		return this.getMapper().insertBatch(list, getTableName(), cls);
	}

	/**
	 * 根据传入的QueryParam对象，进行更新操作
	 * @param param
	 * @return
	 */
	public int update(QueryProvider param) throws HandleException {
		
		if(param == null) {
			throw new HandleException("error: update QueryProvider is null !!!");
		}
		
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().update(paramMap, this.getTableName());
	}
	
	public int updateBatch(List<QueryProvider> list) throws HandleException {
		
		if(list == null || list.isEmpty()) {
			throw new HandleException("error: updateBatch QueryProvider is empty !!!");
		}
		
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, list);
		return this.getMapper().updateBatch(paramMap, this.getTableName());
	}
	
	/**
	 * 根据传入的id主键，删除一条记录
	 * @param id
	 * @return
	 */
	public int deleteById(BigInteger id) throws HandleException {
		return this.getMapper().deleteById(id, this.getTableName());
	}

	/**
	 * 根据传入的QueryParam对象，进行删除操作
	 * 
	 * @param param
	 * @return
	 */
	public int delete(QueryProvider param) throws HandleException {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().delete(paramMap, this.getTableName());
	}

	/**
	 * 1、根据id主键查询一条记录，返回所有字段。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param id
	 * @return
	 */
	public T getById(BigInteger id) {
		QueryProvider param = new QueryProvider();
		param.addFilterEquals(BaseCommonField.FIELD_ID, id);
		return this.get(param);
	}

	/**
	 * 1、根据id主键查询一条记录，返回所有字段，返回类型为预设的class类型，需强制转换一次。
	 * 2、如果根据条件有多条数据符合，则抛出异常。
	 * @param id
	 * @param cls
	 * @return
	 */
	public <M> M getById(BigInteger id, Class<M> cls) {
		QueryProvider param = new QueryProvider();
		param.addFilterEquals(BaseCommonField.FIELD_ID, id);
		return this.get(param, cls);
	}

	/**
	 * 1、根据id主键查询一条记录，返回设定的字段。 2、如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @param id
	 * @return
	 */
	public T getById(QueryProvider param, BigInteger id) {
		param.addFilterEquals(BaseCommonField.FIELD_ID, id);
		return this.get(param);
	}

	/**
	 * 1、根据id主键查询一条记录，返回设定的字段，返回类型为预设的class类型，需强制转换一次。 2、如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @param id
	 * @param cls
	 * @return
	 */
	public <M> M getById(QueryProvider param, BigInteger id, Class<M> cls) {
		param.addFilterEquals(BaseCommonField.FIELD_ID, id);
		return this.get(param, cls);
	}

	/**
	 * 1、根据传入的QueryParam对象，查询一条BaseEntity记录。 2、如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	public T get(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().find(paramMap, this.getTableName());
	}

	/**
	 * 1、根据传入的QueryParam对象，返回类型为预设的class类型，需强制转换一次。 2、如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @param resultCls
	 * @return
	 */
	public <M> M get(QueryProvider param, Class<M> resultCls) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getResultMapper(resultCls).findR(paramMap, this.getTableName());
	}
	
	/**
	 * 校验方法，只需传入 param的条件值即可，映射的SQL语句例如：select count(1) from test t where t.name='test';
	 * @param param
	 * @return
	 */
	public boolean validate(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().validate(paramMap, this.getTableName()) > 0;
	}

	/**
	 * 根据传入的QueryParam对象，返回一条Map格式记录。 如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	public Map<String, Object> getToMap(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().findToMap(paramMap, this.getTableName());
	}

	/**
	 * 根据传入的QueryParam对象，返回符合条件的list集合的BaseEntity记录。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * 
	 * @param param
	 * @return
	 */
	public List<T> list(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().list(paramMap, this.getTableName());
	}

	/**
	 * 根据传入的QueryParam对象，返回符合条件的list集合，返回类型为预设的class类型，需强制转换一次。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * 
	 * @param param
	 * @return
	 */
	public <M> List<M> list(QueryProvider param, Class<M> resultCls) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getResultMapper(resultCls).listR(paramMap, this.getTableName());
	}

	/**
	 * 根据传入的QueryParam对象，返回符合条件的List集合的Map格式记录。
	 * 如果有传入分页标识，只返回设置页面的极限值，否则返回所有符合条件的数据。
	 * 
	 * @param param
	 * @return
	 */
	public List<Map<String, Object>> listToMap(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().query(paramMap, this.getTableName());
	}

	/**
	 * 根据传入的QueryParam对象，返回BigDecimal的类型值。 该方法常用于查询金额字段。 如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	public BigDecimal getBigDecimal(QueryProvider param) {
		Object obj = this.getObject(param);
		if (obj != null) {
			return new BigDecimal(obj.toString());
		}
		return BigDecimal.ZERO;
	}

	/**
	 * 根据传入的QueryParam对象，返回int的类型值。 该方法常用于查询count等类型的业务。 如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	public int getInt(QueryProvider param) {
		Object obj = this.getObject(param);
		if (obj != null) {
			return Integer.valueOf(obj.toString());
		}
		return 0;
	}

	/**
	 * 根据传入的QueryParam对象，返回Double的类型值。 如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	public Double getDouble(QueryProvider param) {
		Object obj = this.getObject(param);
		if (obj != null) {
			return Double.valueOf(obj.toString());
		}
		return 0D;
	}

	/**
	 * 根据传入的QueryParam对象，返回Object的类型值。 如果根据条件有多条数据符合，则抛出异常。
	 * 
	 * @param param
	 * @return
	 */
	private Object getObject(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		return this.getMapper().getObject(paramMap, this.getTableName());
	}

	/**
	 * 需传入的条件值。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @return
	 */
	public T getBySql(String sql, List<Object> param) {
		return this.getMapper().getBySql(sql, param);
	}

	/**
	 * 返回Object 类型，比如int、decimal、String等。
	 * 
	 * @param sql
	 * @param param
	 * @return
	 */
	public Object getObjectBySql(String sql, List<Object> param) {
		return this.getMapper().getObjectBySql(sql, param);
	}

	/**
	 * 获取总条数，针对count 等SQL语句。
	 * 
	 * @param sql
	 * @param param
	 * @return
	 */
	public int getTotal(String sql, List<Object> param) {
		return this.getMapper().getTotalByParam(sql, param);
	}

	/**
	 * 传入SQL，返回预设类型对象。返回类型为预设的class类型，需强制转换一次。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @param resultCls
	 *            返回类型
	 * @return
	 */
	public <M> M getBySql(String sql, List<Object> param, Class<M> resultCls) {
		return this.getResultMapper(resultCls).getBySqlR(sql, param);
	}

	/**
	 * 传入SQL，返回map类型。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @return
	 */
	public Map<String, Object> getMapBySql(String sql, List<Object> param) {
		return this.getMapper().getMapBySql(sql, param);
	}

	/**
	 * 根据传入的SQL语句，返回符合条件的list集合的Map格式记录。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @return
	 */
	public List<T> listBySql(String sql, List<Object> param) {
		return this.getMapper().listBySql(sql, param);
	}

	/**
	 * 传入SQL，返回预设类型集合。返回类型为预设的class类型，需强制转换一次。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @param resultCls
	 *            返回bean类型
	 * @return
	 */
	public <M> List<M> listBySql(String sql, List<Object> param, Class<M> resultCls) {
		return this.getResultMapper(resultCls).listBySqlR(sql, param);
	}

	/**
	 * 根据传入的SQL语句，返回符合条件的list集合的Map格式记录。
	 * 
	 * @param sql
	 *            sql语句中的条件，用 "?" 号代替，防止SQL注入
	 * @param param
	 *            需传入的条件值，按顺序存放
	 * @return
	 */
	public List<Map<String, Object>> listMapBySql(String sql, List<Object> param) {
		return this.getMapper().listMapBySql(sql, param);
	}


	/**
	 * 2.5版本新增的分页方法，主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param sql 主体查询语句
	 * @param totalSql 总条数查询语句
	 * @param param 条件值
	 * @param indexPage 页面
	 * @param pageSize 每行显示条数
	 * @return
	 */
	public PageResultInfo<T> page(String sql, String totalSql, List<Object> param, int indexPage, int pageSize) {

		int total = this.getTotal(totalSql, param);
		PageResultInfo<T> page = new PageResultInfo<>();
		this.setPageInfo(page, total);
		if (total == 0) {
			// 当没有数据的时候，直接不进行数据查询
			return page;
		}
		boolean reset = this.getPageInfo(total, indexPage, pageSize);
		sql = SqlProvider.appendPageSql(sql, indexPage, pageSize, reset);
		page.setList(this.getMapper().listBySql(sql, param));
		return page;
	}

	/**
	 * 2.5版本新增的分页方法，主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。
	 * @param sql 主体查询语句
	 * @param totalSql 总条数查询语句
	 * @param param 条件值
	 * @param indexPage 页面
	 * @param pageSize 每行显示条数
	 * @param resultCls resultCls 返回 预定义的 resultCls Bean 泛型数据类型
	 * @return
	 */
	public <M> PageResultInfo<M> page(String sql, String totalSql, List<Object> param, int indexPage, int pageSize, Class<M> resultCls) {
		int total = this.getTotal(totalSql, param);
		PageResultInfo<M> page = new PageResultInfo<M>();
		this.setPageInfo(page, total);
		if (total == 0) {
			// 当没有数据的时候，直接不进行数据查询
			return page;
		}
		boolean reset = this.getPageInfo(total, indexPage, pageSize);
		sql = SqlProvider.appendPageSql(sql, indexPage, pageSize, reset);
		page.setList(this.getResultMapper(resultCls).listBySqlR(sql, param));
		return page;
	}

	/**
	 * 2.5版本新增的分页方法，主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数，返回 Map 数据。
	 * @param sql 主体查询语句
	 * @param totalSql 总条数查询语句
	 * @param param 条件值
	 * @param indexPage 页面
	 * @param pageSize 每行显示条数
	 * @return
	 */
	public PageResultInfo<Map<String, Object>> pageResultMap(String sql, String totalSql, List<Object> param, int indexPage, int pageSize) {
		int total = this.getTotal(totalSql, param);
		PageResultInfo<Map<String, Object>> page = new PageResultInfo<Map<String, Object>>();
		this.setPageInfo(page, total);
		if (total == 0) {
			// 当没有数据的时候，直接不进行数据查询
			return page;
		}
		boolean reset = this.getPageInfo(total, indexPage, pageSize);
		sql = SqlProvider.appendPageSql(sql, indexPage, pageSize, reset);
		page.setList(this.getMapper().listMapBySql(sql, param));
		return page;
	}


	/**
	 * 2.1版本新增的分页查询方法，主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。 
	 * @param param 封装的参数对象
	 * @return
	 */
	public PageResultInfo<T> page(QueryProvider param) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		param.setIsPage(PageEnum.IS_PAGE_TRUE);
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		// 拼装SQL语句
		SqlProvider.getQueryPageSql(paramMap, this.getTableName());

		int total = this.getMapper().getTotal((String) paramMap.get(CoreCommonStants.COUNT_SQL), paramMap);
		PageResultInfo<T> page = new PageResultInfo<T>();
		this.setPageInfo(page, total);
		if (total == 0) {
			// 当总条数为0时，直接取消数据查询
			return page;
		}

		String querySql = (String) paramMap.get(CoreCommonStants.QUERY_SQL);
		// 说明页面超出真实数据，为了保证前端的兼容效果，重置到第一页
		boolean resetIndexPage = getPageInfo(total, param.getIndexPage(), param.getPageSize());
		if (resetIndexPage) {
			param.setResetIndexPage(true);
		}

		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		page.setList(this.getMapper().page(querySql, paramMap));
		return page;
	}
	
	/**
	 * 2.5版本新增的分页方法，主要实现于在前端查询时选中的页面超过总条数，非前端分页查询，不建议使用。
	 * 分页查询，同时返回分页数据和总条数。 
	 * @param param 封装的参数对象
	 * @param resultCls 返回 预定义的 resultCls Bean 泛型数据类型
	 * @return 
	 */
	public <M> PageResultInfo<M> PageResultInfo(QueryProvider param, Class<M> resultCls) {
		Map<String, Object> paramMap = new HashMap<String, Object>();
		param.setIsPage(PageEnum.IS_PAGE_TRUE);
		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		// 拼装SQL语句
		SqlProvider.getQueryPageSql(paramMap, this.getTableName());

		int total = this.getMapper().getTotal((String) paramMap.get(CoreCommonStants.COUNT_SQL), paramMap);
		PageResultInfo<M> page = new PageResultInfo<M>();
		this.setPageInfo(page, total);
		
		if (total == 0) {
			// 当总条数为0时，直接取消数据查询
			return page;
		}

		String querySql = (String) paramMap.get(CoreCommonStants.QUERY_SQL);

		// 说明页面超出真实数据，为了保证前端的兼容效果，重置到第一页
		boolean resetIndexPage = getPageInfo(total, param.getIndexPage(), param.getPageSize());
		if (resetIndexPage) {
			param.setResetIndexPage(true);
		}

		paramMap.put(CoreCommonStants.PARAM_OBJ, param);
		page.setList(this.getResultMapper(resultCls).pageR(querySql, paramMap));
		return page;
	}
	
	private void setPageInfo(PageResultInfo<?> page, long total) {
		page.setTotal(total);
	}

	private boolean getPageInfo(int total, int indexPage, int pageSize) {
		int index = (indexPage - 1) * pageSize;
		return total > 0 && index > total;
	}

	private long getPages(long total, int pageSize) {
		long pages = total / pageSize;
		if (total % pageSize != 0) {
			pages += 1;
		}
		return pages;
	}

}
