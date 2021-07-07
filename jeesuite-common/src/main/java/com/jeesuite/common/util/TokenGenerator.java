package com.jeesuite.common.util;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.crypt.Base58;
import com.jeesuite.common.crypt.DES;

/**
 * 生成token
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年11月30日
 */
public class TokenGenerator {

	private static final String LINE_THROUGH = "-";
	private static final int EXPIRE = 1000*60*3;
	
	public static String generateFrom(String base){
		String str = DigestUtils.md5(base);
		return new String(Base58.encode(str.getBytes())); 
	}

	public static String generate(String...prefixs){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		if(prefixs != null && prefixs.length > 0 &&  StringUtils.isNotBlank(prefixs[0])){
			str = DigestUtils.md5Short(prefixs[0]).concat(str);
		}
		return new String(Base58.encode(str.getBytes()));
	}
	
	public static String generateWithSign(){
		return generateWithSign(null);
	}
	/**
	 * 生成带签名信息的token
	 * @return
	 */
	public static String generateWithSign(String tokenType){
		String timeString = String.valueOf(System.currentTimeMillis());
		String str = DigestUtils.md5Short(timeString).concat(timeString);	
		if(tokenType == null){
			return SimpleCryptUtils.encrypt(str);
		}
		String cryptKey = getCryptKey(tokenType);
		return DES.encrypt(cryptKey, str);
	}
	
	
	public static void validate(String token,boolean validateExpire){
		validate(null, token, validateExpire);
	}
	
	/**
	 * 验证带签名信息的token
	 */
	public static void validate(String tokenType,String token,boolean validateExpire){
		long timestamp = 0;
		Date date = new Date();
		try {
			if(tokenType == null){
				timestamp = Long.parseLong(SimpleCryptUtils.decrypt(token).substring(6));
			}else{ 
				String cryptKey = getCryptKey(tokenType);
				timestamp = Long.parseLong(DES.decrypt(cryptKey,token).substring(6));
			}
		} catch (Exception e) {
			throw new JeesuiteBaseException(4005, "token格式错误");
		}
		if(validateExpire && date.getTime() - timestamp > EXPIRE){
			throw new JeesuiteBaseException(4005, "token已过期");
		}
	}
	
	private static String getCryptKey(String tokenType){
		String key = StringUtils.EMPTY;
		if(StringUtils.isNotBlank(tokenType)){
			key = ResourceUtils.getAndValidateProperty(tokenType + ".cryptKey");
		}else {			
			key  = SimpleCryptUtils.GLOBAL_CRYPT_KEY;
		}
		return key;
	}
	
	public static void main(String[] args) {
		String token  = generateWithSign();
		System.out.println(token);
		validate(token, true);
	}
}
