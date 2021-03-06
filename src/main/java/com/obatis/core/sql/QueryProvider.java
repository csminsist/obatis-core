package com.obatis.core.sql;

import com.obatis.config.request.PageParam;
import com.obatis.config.request.RequestConstant;
import com.obatis.config.request.RequestParam;
import com.obatis.core.constant.type.FilterEnum;
import com.obatis.core.constant.type.OrderEnum;
import com.obatis.core.constant.type.SqlHandleEnum;
import com.obatis.core.exception.HandleException;
import com.obatis.core.result.ResultInfoOutput;
import com.obatis.core.convert.BeanCacheConvert;
import com.obatis.validate.ValidateTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作 sql 封装操作类，除使用直接拼装 sql 外，其余数据库操作全部使用这个类提供的属性进行操作
 * @author HuangLongPu
 */
public class QueryProvider {

	protected static final String JOIN_AND_EXPRESS = " and ";
	protected static final String JOIN_OR_EXPRESS = " or ";

	protected static AbstractOrder abstractOrder;

	private static final Map<Integer, OrderEnum> ORDER_TYPE_MAP = new HashMap<>();

	static {
		// 加载排序方式和值
		ORDER_TYPE_MAP.put(RequestConstant.ORDER_ASC, OrderEnum.ORDER_ASC);
		ORDER_TYPE_MAP.put(RequestConstant.ORDER_DESC, OrderEnum.ORDER_DESC);
	}

	private int pageNumber = RequestConstant.DEFAULT_PAGE;
	private int pageSize = RequestConstant.DEFAULT_ROWS;

	private List<Object[]> fields;
	private List<Object[]> filters;
	private List<String[]> orders;
	private List<String> groups;
	private List<QueryProvider> orProviders;
	private Map<String, String> notFields;
	private List<Object[]> leftJoinProviders;
	private String joinTableName;

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setPage(PageParam pageParam) {
		this.setPageNumber(pageParam.getPage());
		this.setPageSize(pageParam.getRows());

		String sort = pageParam.getSort();
		if (!ValidateTool.isEmpty(sort)) {
			this.setOrder(sort, ORDER_TYPE_MAP.get(pageParam.getOrder()));
		}
	}

	public List<Object[]> getFields() {
		return fields;
	}

	public List<Object[]> getFilters() {
		return filters;
	}

	public List<String[]> getOrders() {
		return orders;
	}

	public List<String> getGroups() {
		return groups;
	}

	public List<QueryProvider> getOrProviders() {
		return orProviders;
	}

	public Map<String, String> getNotFields() {
		return notFields;
	}

	public List<Object[]> getLeftJoinProviders() {
		return leftJoinProviders;
	}

	protected String getJoinTableName() {
		return joinTableName;
	}

	/**
	 * 设置连接查询时 QueryProvider 属性表名，如果只是简单常规单表查询，即使设置了也无效。 目前主要支持 left join
	 * @param joinTableName
	 */
	public void setJoinTableName(String joinTableName) {
		if (ValidateTool.isEmpty(joinTableName)) {
			throw new HandleException("error: joinTableName is null");
		}
		this.joinTableName = joinTableName;
	}

	/**
	 * 添加字段方法，接收一个参数，此方法主要用于查询 传入的值表示为要查询的字段名称
	 * @param fieldName
	 * @throws HandleException
	 */
	public void add(String fieldName) throws HandleException {
		this.add(fieldName, null);
	}

	/**
	 * 添加字段方法，接收两个参数，此方法主要用于查询(select)或者修改(update) 此方法用于查询或者修改
	 * 用于查询时，第一个参数为要查询的字段名称，第二个参数可为null或者为要查询的别名，类似sql语句中的as name
	 * 用于修改时，第一个参数为要修改的字段名称，第二个为修改后的值
	 * @param fieldName
	 * @param value
	 * @throws HandleException
	 */
	public void add(String fieldName, Object value) throws HandleException {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_DEFAULT, value);
	}

	/**
	 * 实现累加，比如money = money + 20类似的SQL语句; fieldName 表示要操作的字段名称,value 表示要操作的值
	 * @param fieldName
	 * @param value
	 */
	public void addUp(String fieldName, Object value) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_UP, value);
	}

	/**
	 * 实现累加，比如money = money - 20类似的SQL语句; fieldName 表示要操作的字段名称,value 表示要操作的值
	 * @param fieldName
	 * @param value
	 */
	public void addReduce(String fieldName, Object value) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_REDUCE, value);
	}
	
	/**
	 * count 统计函数 >> count(1)
	 */
	public void addCount() {
		this.addCount("");
	}
	
	/**
	 * count 统计函数 >> count(1) as 'aliasName'
	 * @author HuangLongPu
	 * @param aliasName
	 */
	public void addCount(String  aliasName) {
		this.addValue("", SqlHandleEnum.HANDLE_COUNT, aliasName);
	}
	
	/**
	 * distinct 去重函数 >> distinct 'fieldName'
	 * @param fieldName
	 */
	public void addCountDistinct(String fieldName) {
		if(ValidateTool.isEmpty(fieldName)) {
			throw new HandleException("error:field is null");
		}
		this.addCountDistinct(fieldName, "");
	}
	
	/**
	 * distinct 去重函数 >> distinct 'fieldName' as 'aliasName'
	 * @param fieldName
	 * @param aliasName
	 */
	public void addCountDistinct(String fieldName, String aliasName) {
		if(ValidateTool.isEmpty(fieldName)) {
			throw new HandleException("error: field is null");
		}
		this.addValue(fieldName, SqlHandleEnum.HANDLE_COUNT, aliasName);
	}
	
	/**
	 * sum 求和函数 >> sum('fieldName')
	 * @param fieldName
	 */
	public void addSum(String fieldName) {
		this.addSum(fieldName, null);
	}
	
	/**
	 * sum 求和函数 >> sum('fieldName') as 'aliasName'
	 * @param fieldName
	 * @param aliasName
	 */
	public void addSum(String fieldName, String  aliasName) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_SUM, aliasName);
	}
	
	/**
	 * min 最小值函数 >> min('fieldName')
	 * @param fieldName
	 */
	public void addMin(String fieldName) {
		this.addMin(fieldName, null);
	}
	
	/**
	 * min 最小值函数 >> min('fieldName') as 'aliasName'
	 * @param fieldName
	 * @param aliasName
	 */
	public void addMin(String fieldName, String  aliasName) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_MIN, aliasName);
	}
	
	/**
	 * max 最大值函数 >> max('fieldName')
	 * @param fieldName
	 */
	public void addMax(String fieldName) {
		this.addMax(fieldName, null);
	}
	
	/**
	 * max 最大值函数 >> max('fieldName') as 'aliasName'
	 * @param fieldName    字段名
	 * @param aliasName    别名
	 */
	public void addMax(String fieldName, String  aliasName) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_MAX, aliasName);
	}
	
	/**
	 * avg 平均值函数 >> avg('fieldName')
	 * @param fieldName    字段名
	 */
	public void addAvg(String fieldName) {
		this.addAvg(fieldName, null);
	}
	
	/**
	 * avg 平均值函数 >> avg('fieldName') as 'aliasName'
	 * @param fieldName    字段名
	 * @param aliasName    别名
	 */
	public void addAvg(String fieldName, String  aliasName) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_AVG, aliasName);
	}
	
	/**
	 * 表达式函数，非聚合函数时使用，如需聚合，直接使用提供的聚合函数方法即可，同等原理
	 * @param fieldName
	 */
	public void addExp(String fieldName) {
		this.addExp(fieldName, null);
	}
	
	/**
	 * 表达式函数，非聚合函数时使用，如需聚合，直接使用提供的聚合函数方法即可，同等原理
	 * @param fieldName
	 * @param aliasName
	 */
	public void addExp(String fieldName, String  aliasName) {
		this.addValue(fieldName, SqlHandleEnum.HANDLE_EXP, aliasName);
	}

	/**
	 * 设置表达式属性
	 * @param fieldName
	 * @param fieldType
	 * @param value
	 */
	private void addValue(String fieldName, SqlHandleEnum fieldType, Object value) {
		if (ValidateTool.isEmpty(fieldName) && !SqlHandleEnum.HANDLE_COUNT.equals(fieldType)) {
			throw new HandleException("error: field is null");
		}
		if (this.fields == null) {
			this.fields = new ArrayList<>();
		}
		Object[] obj = { fieldName, fieldType, value };
		this.fields.add(obj);
	}

	/**
	 * 添加不需要查询的字段，主要针对实体泛型返回的查询中，如果字段被加入，则会在 SQL 中过滤。
	 * @param fieldName
	 */
	public void setNotField(String fieldName) {
		if (ValidateTool.isEmpty(fieldName)) {
			throw new HandleException("error: field is null");
		}
		if (this.notFields == null) {
			this.notFields = new HashMap<>();
		}
		this.notFields.put(fieldName, fieldName);
	}

	/**
	 * 添加查询条件，where后的字段;
	 * 参数分别为字段名称，比如name。条件类型，比如=，具体的值参考QueryParam的FILTER开头的常量。值
	 * ，比如张三。一起即可name='张三'; 该方法已过期，已由新的方法（addFilter*）替代，将会在后期版本中移除该方法;
	 * @param filterName
	 * @param filterType
	 * @param value
	 */
	private void andFilter(String filterName, FilterEnum filterType, Object value) {
		this.addFilter(filterName, filterType, value, JOIN_AND_EXPRESS);
	}

	/**
	 * 设置条件
	 * @param filterName
	 * @param filterType
	 * @param value
	 * @param joinType
	 */
	private void addFilter(String filterName, FilterEnum filterType, Object value, String joinType) {
		if (ValidateTool.isEmpty(filterName)) {
			throw new HandleException("error: filter field is null");
		} else if (FilterEnum.IS_NULL.equals(filterType) && FilterEnum.IS_NOT_NULL.equals(filterType) && null == value) {
			throw new HandleException("error: field is null");
		}
		if (this.filters == null) {
			this.filters = new ArrayList<>();
		}
		Object[] obj = {filterName, filterType, value, joinType};
		this.filters.add(obj);
	}

	/**
	 * 设置or 查询条件数据
	 * @param filterName
	 * @param filterType
	 * @param value
	 */
	private void or(String filterName, FilterEnum filterType, Object value) {
		this.addFilter(filterName, filterType, value, JOIN_OR_EXPRESS);
	}

	/**
	 * 增加 and 查询条件，模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void like(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.LIKE, value);
	}

	/**
	 * 增加 or 查询条件，模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void orLike(String filterName, Object value) {
		this.or(filterName, FilterEnum.LIKE, value);
	}

	/**
	 * 增加 and 查询条件，左模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void leftLike(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.LEFT_LIKE, value);
	}

	/**
	 * 增加 or 查询条件，左模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void orLeftLike(String filterName, Object value) {
		this.or(filterName, FilterEnum.LEFT_LIKE, value);
	}

	/**
	 * 增加 and 查询条件，右模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void rightLike(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.RIGHT_LIKE, value);
	}

	/**
	 * 增加 or 查询条件，右模糊查询, like
	 * @param filterName
	 * @param value
	 */
	public void orRightLike(String filterName, Object value) {
		this.or(filterName, FilterEnum.RIGHT_LIKE, value);
	}

	/**
	 * 增加 and 查询条件，等于查询，=
	 * @param filterName
	 * @param value
	 */
	public void equals(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.EQUAL, value);
	}

	/**
	 * 增加 or 查询条件，等于查询，=
	 * @param filterName
	 * @param value
	 */
	public void orEquals(String filterName, Object value) {
		this.or(filterName, FilterEnum.EQUAL, value);
	}

	/**
	 * 增加 and 查询条件，大于查询，>
	 * @param filterName
	 * @param value
	 */
	public void greateThan(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.GREATE_THAN, value);
	}

	/**
	 * 增加 or 查询条件，大于查询，>
	 * @param filterName
	 * @param value
	 */
	public void orGreateThan(String filterName, Object value) {
		this.or(filterName, FilterEnum.GREATE_THAN, value);
	}

	/**
	 * 增加 and 查询条件，大于等于查询，>=
	 * @param filterName
	 * @param value
	 */
	public void greateEqual(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.GREATE_EQUAL, value);
	}

	/**
	 * 增加 or 查询条件，大于等于查询，>=
	 * @param filterName
	 * @param value
	 */
	public void orGreateEqual(String filterName, Object value) {
		this.or(filterName, FilterEnum.GREATE_EQUAL, value);
	}

	/**
	 * 增加 and 大于等于0的条件表达式，传入字段名称即可
	 * @param filterName
	 */
	public void greateEqualZero(String filterName) {
		this.andFilter(filterName, FilterEnum.GREATE_EQUAL, 0);
	}

	/**
	 * 增加 or 大于等于0的条件表达式，传入字段名称即可
	 * @param filterName
	 */
	public void orGreateEqualZero(String filterName) {
		this.or(filterName, FilterEnum.GREATE_EQUAL, 0);
	}

	/**
	 * 增加 and 查询条件，小于查询，<
	 * @param filterName
	 * @param value
	 */
	public void lessThan(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.LESS_THAN, value);
	}

	/**
	 * 增加 or 查询条件，小于查询，<
	 * @param filterName
	 * @param value
	 */
	public void orLessThan(String filterName, Object value) {
		this.or(filterName, FilterEnum.LESS_THAN, value);
	}

	/**
	 * 增加 and 查询条件，小于等于查询，<=
	 * @param filterName
	 * @param value
	 */
	public void lessEqual(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.LESS_EQUAL, value);
	}

	/**
	 * 增加 or 查询条件，小于等于查询，<=
	 * @param filterName
	 * @param value
	 */
	public void orLessEqual(String filterName, Object value) {
		this.or(filterName, FilterEnum.LESS_EQUAL, value);
	}

	/**
	 * 增加 and 查询，不等于查询，<>
	 * @param filterName
	 * @param value
	 */
	public void notEqual(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.NOT_EQUAL, value);
	}

	/**
	 * 增加 or 查询，不等于查询，<>
	 * @param filterName
	 * @param value
	 */
	public void orNotEqual(String filterName, Object value) {
		this.or(filterName, FilterEnum.NOT_EQUAL, value);
	}

	/**
	 * 增加 and 查询条件，属于查询，in
	 * @param filterName
	 * @param value
	 */
	public void in(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.IN, value);
	}

	/**
	 * 增加 or 查询条件，属于查询，in
	 * @param filterName
	 * @param value
	 */
	public void orIn(String filterName, Object value) {
		this.or(filterName, FilterEnum.IN, value);
	}

	/**
	 * 增加 and 查询条件，不属于查询，not in
	 * @param filterName
	 * @param value
	 */
	public void notIn(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.NOT_IN, value);
	}

	/**
	 * 增加 or 查询条件，不属于查询，not in
	 * @param filterName
	 * @param value
	 */
	public void orNotIn(String filterName, Object value) {
		this.or(filterName, FilterEnum.NOT_IN, value);
	}

	/**
	 * 增加 and 查询条件，表示null值查询，is null
	 * @param filterName
	 */
	public void isNull(String filterName) {
		this.andFilter(filterName, FilterEnum.IS_NULL, null);
	}

	/**
	 * 增加 or 查询条件，表示null值查询，is null
	 * @param filterName
	 */
	public void orIsNull(String filterName) {
		this.or(filterName, FilterEnum.IS_NULL, null);
	}

	/**
	 * 增加 and 查询条件，表示null值查询，is not null
	 * @param filterName
	 */
	public void isNotNull(String filterName) {
		this.andFilter(filterName, FilterEnum.IS_NOT_NULL, null);
	}

	/**
	 * 增加 or 查询条件，表示null值查询，is not null
	 * @param filterName
	 */
	public void orIsNotNull(String filterName) {
		this.or(filterName, FilterEnum.IS_NOT_NULL, null);
	}

	/**
	 * 增加 and 设定值后大于条件判断，比如count + 10 > 0
	 * @param filterName
	 * @param value
	 */
	public void upGreateThanZero(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.UP_GREATE_THAN, value);
	}

	/**
	 * 增加 or 设定值后大于条件判断，比如count + 10 > 0
	 * @param filterName
	 * @param value
	 */
	public void orUpGreateThanZero(String filterName, Object value) {
		this.or(filterName, FilterEnum.UP_GREATE_THAN, value);
	}

	/**
	 * 增加 and 设定值后大于等于条件判断，比如count + 10 >= 0
	 * @param filterName
	 * @param value
	 */
	public void upGreateEqualZero(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.UP_GREATE_EQUAL, value);
	}

	/**
	 * 增加 or 设定值后大于等于条件判断，比如count + 10 >= 0
	 * @param filterName
	 * @param value
	 */
	public void orUpGreateEqualZero(String filterName, Object value) {
		this.or(filterName, FilterEnum.UP_GREATE_EQUAL, value);
	}

	/**
	 * 增加 and 设定值后大于条件判断，比如count + 10 > 0
	 * @param filterName
	 * @param value
	 */
	public void reduceGreateThanZero(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.REDUCE_GREATE_THAN, value);
	}

	/**
	 * 增加 or 设定值后大于条件判断，比如count + 10 > 0
	 * @param filterName
	 * @param value
	 */
	public void orReduceGreateThanZero(String filterName, Object value) {
		this.or(filterName, FilterEnum.REDUCE_GREATE_THAN, value);
	}

	/**
	 * 减少 and 设定值后小于等于条件判断，比如count - 10 >= 0
	 * @param filterName
	 * @param value
	 */
	public void reduceGreateEqualZero(String filterName, Object value) {
		this.andFilter(filterName, FilterEnum.REDUCE_GREATE_EQUAL, value);
	}

	/**
	 * 减少 or 设定值后小于等于条件判断，比如count - 10 >= 0
	 * @param filterName
	 * @param value
	 */
	public void orReduceGreateEqualZero(String filterName, Object value) {
		this.or(filterName, FilterEnum.REDUCE_GREATE_EQUAL, value);
	}

	/**
	 * 添加查询添加，比如 and (type = 1 or name = 2)，主要作用于拼接 and 后括号中的表达式，主要用于 or
	 * 查询的表达式，不然没必要。 如果 多条件拼接 or 查询(类似 where id = ? and name = ? or type = 1
	 * 的条件)，or 条件查询不能被当成第一个条件放入(type属性 orFilter 方法不能在第一个加入)，否则会被解析为 and 条件查询。 V
	 * @param queryProvider
	 */
	public void orProvider(QueryProvider queryProvider) {
		if (queryProvider == null) {
			throw new HandleException("error: queryProvider is null");
		}

		if (this.orProviders == null) {
			orProviders = new ArrayList<>();
		}

		this.orProviders.add(queryProvider);
	}

	/**
	 * 添加 left join 查询，会被拼接到left join 的连体SQL。 当使用这个属性时，必须设置 joinTableName的连接表名。
	 * @param fieldName        表示left join 前面一张关联字段。
	 * @param paramFieldName   表示left join 后紧跟表的关联字段。
	 * @param queryProvider    被left join的封装对象。
	 */
	public void leftJoinProvider(String fieldName, String paramFieldName, QueryProvider queryProvider) {
		if (fieldName == null) {
			throw new HandleException("error: left join fieldName is null");
		}
		if (paramFieldName == null) {
			throw new HandleException("error: left join paramFieldName is null");
		}
		if (queryProvider == null) {
			throw new HandleException("error: queryProvider can't null");
		}
		if (ValidateTool.isEmpty(queryProvider.getJoinTableName())) {
			throw new HandleException("error: queryProvider joinTableName is null");
		}

		if (this.leftJoinProviders == null) {
			leftJoinProviders = new ArrayList<>();
		}

		Object[] obj = { fieldName, paramFieldName, queryProvider };
		this.leftJoinProviders.add(obj);
	}

	/**
	 * 添加 left join 查询，会被拼接到left join 的连体SQL。 当使用这个属性时，必须设置 joinTableName
	 * 的连接表名。 针对多条件，两数组长度必须一致。
	 * @param fieldName         表示left join 前面一张关联字段。
	 * @param paramFieldName    表示left join 后紧跟表的关联字段。
	 * @param queryProvider             被left join的封装对象。
	 */
	public void leftJoinProvider(String[] fieldName, String[] paramFieldName, QueryProvider queryProvider) {
		int fieldLength = 0;
		if (fieldName == null || (fieldLength = fieldName.length) == 0) {
			throw new HandleException("error: left join fieldName is null");
		}
		int paramFieldLength = 0;
		if (paramFieldName == null || (paramFieldLength = paramFieldName.length) == 0) {
			throw new HandleException("error: left join paramFieldName is null");
		}
		if (fieldLength != paramFieldLength) {
			throw new HandleException("error: left join 'on' filter length must be equal");
		}
		if (queryProvider == null) {
			throw new HandleException("error: queryProvider is null");
		}

		if (this.leftJoinProviders == null) {
			leftJoinProviders = new ArrayList<>();
		}

		Object[] obj = { fieldName, paramFieldName, queryProvider };
		this.leftJoinProviders.add(obj);
	}

	/**
	 * 增加排序，参数分别为排序字段，排序值，排序值类型参考QueryParam中ORDER开头的常量
	 * @param orderName
	 * @param orderType
	 */
	public void setOrder(String orderName, OrderEnum orderType) {
		if (ValidateTool.isEmpty(orderName)) {
			throw new HandleException("error: order field is null");
		}

		if (this.orders == null) {
			this.orders = new ArrayList<>();
		}
		abstractOrder.addOrder(orders, orderName, orderType);
	}

	/**
	 * 增加分组，根据字段名称进行分组
	 * @param groupName
	 */
	public void setGroup(String groupName) {
		if (ValidateTool.isEmpty(groupName)) {
			throw new HandleException("error: group field is null");
		}
		if (this.groups == null) {
			this.groups = new ArrayList<>();
		}
		this.groups.add(groupName);
	}

	/**
	 * 移除所有查询条件
	 * @param
	 */
	public void removeFilter() {
		this.filters.clear();
	}

	/**
	 * 根据前端传入的 command 实体，获取查询属性的 @QueryFilter 注解值
	 * @author HuangLongPu
	 * @param obj
	 */
	public void setFilters(Object obj) {
		if (!(obj instanceof RequestParam)) {
			throw new HandleException("error: the filter is not instanceof RequestQueryParam");
		}
		QueryHandle.getFilters(obj, this);
	}

	/**
	 * 根据前端传入的 command 实体，获取修改属性的 @UpdateField 注解值
	 * @param obj
	 */
	public void setUpdate(Object obj) {
		if (!(obj instanceof RequestParam)) {
			throw new HandleException("error: the update is not instanceof RequestQueryParam");
		}
		QueryHandle.getUpdateField(obj, this);
	}
	
	/**
	 * 传入 ResultInfoOutput 的子类进行自动转换。
	 * 如果接收的属性与数据库字段不一致，用@Column 注解映射，映射可以是实体属性名和字段名。
	 * 如果有属性不想被添加到addField中，用@NotColumn 注解映射，将会自动过滤。
	 * @param cls
	 */
	public void setColumn(Class<?> cls) {
		if(!ResultInfoOutput.class.isAssignableFrom(cls)) {
			throw new HandleException("error: the select is not instanceof ResultInfoOutput");
		}
		
		List<String[]> result = BeanCacheConvert.getResultFields(cls);
		for (String[] field : result) {
			this.add(field[0], field[1]);
		}
	}

}
