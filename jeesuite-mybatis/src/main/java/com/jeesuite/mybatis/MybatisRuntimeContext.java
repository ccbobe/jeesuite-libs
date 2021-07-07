package com.jeesuite.mybatis;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.mybatis.datasource.DataSourceContextVals;
import com.jeesuite.mybatis.plugin.cache.CacheHandler;

/**
 * 
 * <br>
 * Class Name   : MybatisRuntimeContext
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年2月14日
 */
public class MybatisRuntimeContext {


	private static final String CONTEXT_USER_ID_KEY = "_ctx_userId_";
	private static final String CONTEXT_TRANS_ON_KEY = "_ctx_trans_on_";
	private static final String CONTEXT_DATASOURCE_KEY = "_ctx_ds_";
	private static final String CONTEXT_DATA_PROFILE_KEY = "_ctx_dataprofile_";
	private static final String CONTEXT_DATA_PROFILE_OFF_KEY = "_ctx_dataprofile_off_";
	
	public static void setCurrentUserId(Serializable userId){
		ThreadLocalContext.set(CONTEXT_USER_ID_KEY, userId);
	}
	
	public static void setTenantId(String tenantId){
		if(StringUtils.isBlank(tenantId))return;
		ThreadLocalContext.set(ThreadLocalContext.TENANT_ID_KEY, tenantId);
	}
	
	public static void unsetTenantId(){
		ThreadLocalContext.remove(ThreadLocalContext.TENANT_ID_KEY);
	}
	
	public static String getContextParam(String paramName){
		if(StringUtils.isBlank(paramName))return null;
		if(CacheHandler.CURRENT_USER_CONTEXT_NAME.equals(paramName)){
			return getCurrentUserId();
		}
		return ThreadLocalContext.getStringValue(paramName);
	}
	
	public static void setContextParam(String name,String value){
		ThreadLocalContext.set(name, value);
	}
	
	public static void dataProfileIgnore(){
		ThreadLocalContext.set(CONTEXT_DATA_PROFILE_OFF_KEY, String.valueOf(true));
	}
	
	public static boolean isDataProfileIgnore(){
		return ThreadLocalContext.exists(CONTEXT_DATA_PROFILE_OFF_KEY);
	}
	
	public static void setTransactionalMode(boolean on){
		ThreadLocalContext.set(CONTEXT_TRANS_ON_KEY, String.valueOf(on));
		if(on){
			forceMaster();
		}
	}
	
	public static String getTransactionalMode(){
		return ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY);
	}
	
	public static String getCurrentUserId(){
		return ThreadLocalContext.getStringValue(CONTEXT_USER_ID_KEY);
	}
	
	public static String getTenantId(){
		return ThreadLocalContext.getStringValue(ThreadLocalContext.TENANT_ID_KEY);
	}
	
	public static boolean isTransactionalOn(){
		return Boolean.parseBoolean(ThreadLocalContext.getStringValue(CONTEXT_TRANS_ON_KEY));
	}
	
	public static boolean isEmpty(){
		return ThreadLocalContext.isEmpty();
	}
	
	/**
	 * 设置是否使用从库
	 * 
	 * @param useSlave
	 */
	public static void useSlave(boolean useSlave) {
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.userSlave = useSlave;
	}
	
	/**
	 * 设置强制使用master库
	 */
	public static void forceMaster(){
		DataSourceContextVals vals = MybatisRuntimeContext.getDataSourceContextVals();
		vals.forceMaster = true;
	}
	
	/**
	 * 判断是否强制使用一种方式
	 * 
	 * @return
	 */
	public static boolean isForceUseMaster() {
		return MybatisRuntimeContext.getDataSourceContextVals().forceMaster;
	}
	
	public static DataSourceContextVals getDataSourceContextVals(){
		DataSourceContextVals dataSourceContextVals = ThreadLocalContext.get(CONTEXT_DATASOURCE_KEY);
		if(dataSourceContextVals == null){
			dataSourceContextVals = new DataSourceContextVals();
			ThreadLocalContext.set(CONTEXT_DATASOURCE_KEY, dataSourceContextVals);
		}
		return dataSourceContextVals;
	}
	
	public static void addDataProfileMappings(String fieldName,String...fieldValues){
		Map<String, String[]> map = getDataProfileMappings();
		if(map == null){
			map = new HashMap<>(5);
			ThreadLocalContext.set(CONTEXT_DATA_PROFILE_KEY,map);
		}
		map.put(fieldName, fieldValues);
	}
	
	public static Map<String, String[]> getDataProfileMappings(){
		return ThreadLocalContext.get(CONTEXT_DATA_PROFILE_KEY);
	}

	/**
	 * 清理每一次数据库操作的上下文
	 */
	public static void unsetEveryTime(){
		ThreadLocalContext.remove(CONTEXT_DATASOURCE_KEY);
	}
	
}
