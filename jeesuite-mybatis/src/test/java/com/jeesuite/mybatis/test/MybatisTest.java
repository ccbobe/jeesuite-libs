/**
 * 
 */
package com.jeesuite.mybatis.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.jeesuite.cache.redis.JedisProviderFactory;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.mybatis.plugin.cache.EntityCacheHelper;
import com.jeesuite.mybatis.plugin.pagination.Page;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor;
import com.jeesuite.mybatis.plugin.pagination.PageExecutor.PageDataLoader;
import com.jeesuite.mybatis.plugin.pagination.PageParams;
import com.jeesuite.mybatis.test.entity.SnsAccounyBindingEntity;
import com.jeesuite.mybatis.test.entity.UserEntity;
import com.jeesuite.mybatis.test.mapper.SnsAccounyBindingEntityMapper;
import com.jeesuite.mybatis.test.mapper.UserEntityMapper;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;

import tk.mybatis.mapper.entity.Example;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:test-mybatis.xml"})
@Rollback(false)
public class MybatisTest implements ApplicationContextAware{
	
	@Autowired UserEntityMapper userMapper;
	
	@Autowired SnsAccounyBindingEntityMapper snsAccounyBindingMapper;
	
	@Autowired TransactionTemplate transactionTemplate;
	
    String[] mobiles = new String[5];
	
	@Before
	public void init(){
		for (int i = 0; i <mobiles.length; i++) {
			mobiles[i] = "1380013800"+i;
		}
	}
	
	@After
	public void after(){
//		List<UserEntity> list = userMapper.selectAll();
//		for (UserEntity userEntity : list) {
//			userMapper.deleteByPrimaryKey(userEntity.getId());
//		}
//		
//		List<SnsAccounyBindingEntity> list2 = snsAccounyBindingMapper.selectAll();
//		for (SnsAccounyBindingEntity snsAccounyBindingEntity : list2) {
//			snsAccounyBindingMapper.deleteByPrimaryKey(snsAccounyBindingEntity.getId());
//		}
	}
	
	private void printCacheKeys(String title){
		Set<String> keys = JedisProviderFactory.getMultiKeyCommands(null).keys("UserEntity*");
		System.out.println(title + ":\n" + keys.size() + "\n" + keys);
	}

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {	
		InstanceFactory.setInstanceProvider(new SpringInstanceProvider(arg0));
	}
	
	private void insertTestData(){
		
		for (int i = 0; i < mobiles.length; i++) {
			UserEntity entity = new UserEntity();
			entity.setCreatedAt(new Date());
			entity.setEmail(mobiles[i] + "@163.com");
			entity.setMobile(mobiles[i]);
			entity.setType((short)(i % 2 == 0 ? 1 : 2));
			entity.setStatus((short)(i % 3 == 0 ? 1 : 2));
			userMapper.insert(entity);
			
			SnsAccounyBindingEntity bindingEntity = new SnsAccounyBindingEntity();
			bindingEntity.setUserId(entity.getId());
			bindingEntity.setUnionId(DigestUtils.md5(mobiles[i] ));
			bindingEntity.setSnsType("weixin");
			bindingEntity.setEnabled(true);
			snsAccounyBindingMapper.insertSelective(bindingEntity);
		}
	}
	
	private void initCache() {
		UserEntity entity = userMapper.findByMobile(mobiles[0]);
		userMapper.findByMobile(mobiles[1]);
		userMapper.findByLoginName(mobiles[0]+"@163.com");
		userMapper.findByLoginName(mobiles[1]+"@163.com");
		userMapper.findByWxUnionId(DigestUtils.md5(mobiles[3] ));
		userMapper.findByStatus((short) 1);
		userMapper.findByStatus((short) 2);
		userMapper.findByType((short) 1);
		userMapper.findWxUnionIdByUserId(entity.getId());

		// 生成的缓存key为：UserEntity.findByStatus:2
		EntityCacheHelper.queryTryCache(UserEntity.class, "findByStatus:2", new Callable<List<UserEntity>>() {
			public List<UserEntity> call() throws Exception {
				// 查询语句
				List<UserEntity> entitys = userMapper.findByStatus((short) 2);
				return entitys;
			}
		});
		userMapper.countByType(1);
	}
	
	@Test 
	public void testBaseQuery() {
		insertTestData();
		userMapper.findByMobile(mobiles[0]);
		userMapper.findByMobile(mobiles[1]);
		userMapper.findByStatus((short) 1);
		userMapper.findByStatus((short) 2);
		userMapper.findByType((short) 1);

		// 生成的缓存key为：UserEntity.findByStatus:2
		EntityCacheHelper.queryTryCache(UserEntity.class, "findByStatus:2", new Callable<List<UserEntity>>() {
			public List<UserEntity> call() throws Exception {
				// 查询语句
				List<UserEntity> entitys = userMapper.findByStatus((short) 2);
				return entitys;
			}
		});

		userMapper.countByType(1);
	}
	
	@Test 
	public void testPropNotEntityFielddQuery() {
		insertTestData();
		initCache();
		System.out.println();
	}
	
	@Test
	public void testUpdate(){
		initCache();
		UserEntity entity = userMapper.findByMobile(mobiles[0]);
		entity.setEmail(entity.getMobile() + "@qq.com");
		userMapper.updateByPrimaryKey(entity);
	}
	
	@Test
	public void testDeleteByExample(){
		Example example = new Example(UserEntity.class);
		example.createCriteria().andGreaterThan("id", 100).andEqualTo("status", 1);
		userMapper.deleteByExample(example);
	}
	
	@Test
	public void testOnDeleteByIdUpdateCache(){
		printCacheKeys("before delete");
		UserEntity userEntity = userMapper.findByMobile(mobiles[0]);
		userEntity = new UserEntity();
		userEntity.setMobile(mobiles[0]);
		userMapper.delete(userEntity);
		printCacheKeys("after delete");
	}
	
	@Test
	public void testOnDeleteByQueryUpdateCache(){
		printCacheKeys("before delete");
		Example example = new Example(UserEntity.class);
		example.createCriteria().andEqualTo("mobile", mobiles[1]);
		userMapper.deleteByExample(example);
		printCacheKeys("after delete");
	}

	
	@Test
	public void testFindNotExistsThenInsert(){
		String mobile = "13800000002";
		UserEntity entity = userMapper.findByMobile(mobile);
		if(entity != null){
			System.out.println(entity.getMobile());
			return;
		}
		entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(mobile + "@163.com");
		entity.setMobile(mobile);
		entity.setType((short)1);
		entity.setStatus((short)1);
		userMapper.insert(entity);
	}
	
	@Test
	public void testFindBystatus(){
		List<UserEntity> list = userMapper.findByStatus((short)1);
		for (UserEntity userEntity : list) {
			System.out.println(userEntity.getMobile());
		}
	}
	
	@Test
	public void testFindNotExists(){
		String mobile = "13800000002";
		userMapper.findByMobile(mobile);
	}
	
	@Test
	public void testPage(){
		Page<UserEntity> pageInfo = PageExecutor.pagination(new PageParams(1,10), new PageDataLoader<UserEntity>() {
			@Override
			public List<UserEntity> load() {
				UserEntity example = new UserEntity();
				example.setType((short)1);
				return userMapper.queryByExample(example);
			}
		});
		
		System.out.println(pageInfo);
	}
	
	@Test
	public void testPage2(){
		Map<String, Object> conditions = new HashMap<>();
		conditions.put("status", 1);
		conditions.put("name", "vakin");
		Page<UserEntity> pageInfo = userMapper.pageQuery(new PageParams(1,5),conditions);
		
		System.out.println(pageInfo);
	}
	
	@Test
	public void testFindMobileByIds(){
		List<String> mobiles = userMapper.findMobileByIds(new ArrayList<>(Arrays.asList(21,23)));
		for (String mobile : mobiles) {
			System.out.println("------>>>>" + mobile);
		}
	}
	
	@Test
	@Transactional
	public void testRwRouteWithTransaction(){
		userMapper.findByStatus((short)2);
		
		UserEntity entity = new UserEntity();
		entity.setCreatedAt(new Date());
		entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
		entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
		entity.setType((short)1);
		entity.setStatus((short)1);
		userMapper.insert(entity);
	}
	
	@Test
	public void testRwRouteWithTransaction2(){
		
		userMapper.findByStatus((short)1);
		
		transactionTemplate.execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {

				userMapper.findByStatus((short)2);
				
				UserEntity entity = new UserEntity();
				entity.setCreatedAt(new Date());
				entity.setEmail(RandomStringUtils.random(6, true, true) + "@163.com");
				entity.setMobile("13800"+RandomUtils.nextLong(100000, 999999));
				entity.setType((short)1);
				entity.setStatus((short)1);
				userMapper.insert(entity);
				
				userMapper.findByStatus((short)2);
				
				return null;
			}
		});
		System.out.println();
	}
	
}
