package com.jeesuite.mybatis.plugin.datalog;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.mapping.MappedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.mybatis.core.BaseEntity;
import com.jeesuite.mybatis.core.InterceptorHandler;
import com.jeesuite.mybatis.parser.EntityInfo;
import com.jeesuite.mybatis.parser.MybatisMapperParser;
import com.jeesuite.mybatis.plugin.InvocationVals;
import com.jeesuite.mybatis.plugin.JeesuiteMybatisInterceptor;

/**
 * 数据变更日志mybatis层拦截处理器
 * <br>
 * Class Name   : DataChangeLogHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年10月26日
 */
public class DataChangeLogHandler implements InterceptorHandler {

	private final static Logger logger = LoggerFactory.getLogger("om.jeesuite.mybatis.plugin.datalog");
	
	private static ThreadLocal<List<String>> entityClassNameHolder = new ThreadLocal<>();
	private static String[] methodNames = new String[]{"insert","insertSelective","selectByPrimaryKey","deleteByPrimaryKey","updateByPrimaryKey","updateByPrimaryKeySelective"};
	
	public static void set(Class<? extends BaseEntity>[] entityClass){
		List<String> names = new ArrayList<String>(entityClass.length);
        for (Class<? extends BaseEntity> clazz : entityClass) {
        	EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByEntityName(clazz.getName());
        	names.add(entityInfo.getMapperClass().getName());
		}
        entityClassNameHolder.set(names);
	}
	
    public static void unset(){
    	if(entityClassNameHolder.get() == null)return;
    	entityClassNameHolder.get().clear();
    	entityClassNameHolder.remove();
	}

	@Override
	public void start(JeesuiteMybatisInterceptor context) {}

	@Override
	public Object onInterceptor(InvocationVals invocation) throws Throwable {
		return null;
	}

	@Override
	public void onFinished(InvocationVals invocation, Object result) {
		if(entityClassNameHolder.get() == null || entityClassNameHolder.get().isEmpty())return;
		Object[] args = invocation.getArgs();
		MappedStatement mt = (MappedStatement)args[0]; 
		
		String mapperClassName = mt.getId().substring(0,mt.getId().lastIndexOf("."));
		if(!entityClassNameHolder.get().contains(mapperClassName))return;
		EntityInfo entityInfo = MybatisMapperParser.getEntityInfoByMapper(mapperClassName);
		if(entityInfo == null)return;
		try {
			System.out.println(mt.getId());
			if(mt.getId().endsWith(methodNames[0]) || mt.getId().endsWith(methodNames[1])){
				//BehaviorLogContextHoler.onAddEntity(entityInfo.getEntityClass(), ((BaseEntity)args[1]).getId());
			}else if(mt.getId().endsWith(methodNames[2])){
				if(result != null && !((List)result).isEmpty()){					
					//BehaviorLogContextHoler.onUpdateEntityBefore((BaseEntity)((List)result).get(0));
				}
			}else if(mt.getId().endsWith(methodNames[3])){
	        	//BehaviorLogContextHoler.onDeleteEntity(entityInfo.getEntityClass(), args[1]);
			}else if(mt.getId().endsWith(methodNames[4]) || mt.getId().endsWith(methodNames[5])){
				//BehaviorLogContextHoler.onUpdateEntityAfter((BaseEntity)args[1]);
			}
		} catch (Exception e) {
			logger.warn("datachange_interceptor_error for["+mt.getId()+"]",e);
		}
	}

	@Override
	public int interceptorOrder() {
		return 9;
	}
	
	@Override
	public void close() {}

}
