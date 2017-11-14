package com.chemcn.ec.servicecenter.finance.service.paramValidation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.baosight.canacsa.core.common.base.utils.BeanUtil;
import com.chemcn.ec.api.common.pojo.ResultDO;

/**
 * <p>
 * 自定义注解对接口参数进行代理验证
 * <p>
 * 使用方式如下:
 * <p>
 * 若接口方法{@code public MarginPayRes marginPay(MarginPayReq req)} 需要进行参数的非空验证 <br>
 * 只需要在方法上增加注解 , 注解的格式如下
 * <p>
 * {@code @ValidateGroup(fields = (
 * 				{@code @ValidateField(index = 0, fieldName = "marginPayDto.buyerId", notNull = true, errorMsg = "买家ID不能为null")}...}
 * 
 * @author Mrlxf
 *
 */
@Component
@Aspect
public class ParameterValidHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String SEPERATOR = "\\.";
	
	private final String REGEXP_FIELD_NAME = "\\w+\\.\\w+" ;

	private final Boolean FAILURE = false;

	/**
	 * <p>
	 * aop形式进行方法参数校验
	 * 
	 * @param joinPoint
	 * @return
	 * @throws 
	 */
	@Around("@annotation(com.chemcn.ec.servicecenter.finance.service.paramValidation.ValidateGroup)")
	public Object validHandle(ProceedingJoinPoint joinPoint) throws Throwable {
		ResultDO result; // 接口统一返回值
		String methodName = joinPoint.getSignature().getName(); // 拦截的方法名
		Class<?> clazz = joinPoint.getTarget().getClass(); // 类
		Method method = this.getMethodByNameAndClazz(clazz, methodName); // 拦截的方法
		Class<?> returnClass = method.getReturnType(); // 返回值类型
		Object returnObj = null;
		Object[] args = joinPoint.getArgs(); // 拦截方法的参数
		ValidateGroup an = (ValidateGroup) method.getAnnotation(ValidateGroup.class); // 获取@ValidateGroup注解
		this.printStartInfo(args, methodName, clazz); // 打印方法调用开始日志
		returnObj = returnClass.newInstance(); // 生成返回值实例
		result = this.validateFields(an.fields(), args); // 验证参数
		BeanUtil.copyProperties(result, returnObj);
		if (result.isSuccess()) {
			returnObj = joinPoint.proceed(); // 验证通过 , 0执行方法体
			this.printEndInfo(methodName, returnObj, clazz); // 打印方法调用结束日志
		}
		return returnObj;
	}

	/** 通过类和方法名称获取对应的Method对象 */
	private Method getMethodByNameAndClazz(Class<?> clazz, String methodName) {
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * 验证
	 * 
	 * @param validFields 拦截方法参数对应的validField
	 * @param args 拦截的方法参数
	 * @return {@code result.isSuccess() == true 表示成功 else result.isSuccess() == false 表示验证失败}
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	private ResultDO validateFields(ValidateField[] validFields, Object[] args) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, ClassNotFoundException,
			InstantiationException {
		Object arg = null; // 需要验证的参数
		ResultDO result = new ResultDO();
		// 遍历validField
		for (ValidateField validField : validFields) {

			// 空字符串表示未指定属性名则当前为基本数据类型
			if ("".equals(validField.fieldName())) {
				arg = args[validField.index()];
				result = this.doValidation(validField, arg);
			}else{
				// 校验fieldName格式
				if(this.validRegex(validField.fieldName())){
					// if collection
					if(validField.isCollection()){
						// 获取到集合
						Object collection = getObjectFieldByName(args[validField.index()],
								validField.fieldName().split(SEPERATOR)[0]);
						// 验证集合
						result = this.validateCollection(collection , validField);
					}
					// if object
					else{
						result = validateObject(args[validField.index()], validField);
					}
				}else{
					result.setSuccess(FAILURE);
					result.setMessage("参数验证失败:fieldName格式错误");
				}
			}
			if (!result.isSuccess()) {
				return result;
			}
		}
		return result;
	}

	/**
	 * <p>
	 * 验证拦截方法参数对象引用的对象的属性
	 * <p>
	 * 例如:拦截的方法为 {@code public MarginPayRes marginPay(MarginPayReq req)}
	 * <p>
	 * 注解 为{@code @ValidateField(index = 0, fieldName = "marginPayDto.buyerId", 
	 * 		notNull = true, errorMsg = "买家ID不能为null")}
	 * <p>
	 * 则当前验证将会验证{@code req}中的的{@code marginPayDto}中的{@code buyerId}
	 * 
	 * @param obj 拦截方法参数对象
	 * @param objFieldName 指定属性名称：格式为 <strong>对象名.属性名</strong>
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 */
	private ResultDO validateObject(Object obj, ValidateField validField) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		ResultDO result = new ResultDO();
		result.setMessage("");
		String objFieldName = validField.fieldName();
		String objName = objFieldName.split(this.SEPERATOR)[0]; // 对象名
		String fieldName = objFieldName.split(this.SEPERATOR)[1]; // 对象属性名
		Object objField = getObjectFieldByName(obj, objName);
		if(objField == null){
			result.setMessage(objName+"不能为null");
			result.setSuccess(FAILURE);
			return result ;
		}
		Object value = getObjectFieldByName(objField, fieldName);
		result = this.doValidation(validField, value);
		return result;
	}

	private ResultDO validateCollection(Object c, ValidateField validField) throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		ResultDO result = new ResultDO();
		result.setMessage("");
		Collection<?> collection ;
		if(c == null){
			result.setMessage(validField.fieldName().split(SEPERATOR)[0]+"不能为空");
			result.setSuccess(FAILURE);
			return result ;
		}
		if(c instanceof Collection<?>){
			collection = (Collection<?>)c ;
			Object arg;
			for (Object object : collection) {
				String fieldName = validField.fieldName().split(SEPERATOR)[1];
				arg = this.getObjectFieldByName(object, fieldName);
				result = this.doValidation(validField, arg);
				if (!result.isSuccess()) {
					break;
				}
			}
		}else {
			result.setSuccess(FAILURE);
			result.setMessage("参数验证失败:" + validField.fieldName().split(SEPERATOR)[0] + "不是集合类型");
		}
		return result;
	}

	private ResultDO doValidation(ValidateField validField, Object value) {
		ResultDO result = new ResultDO();
		result.setMessage("");
		StringBuilder builder = new StringBuilder(result.getMessage());
		if(validField.notNull()){
			if(value == null){
				result.setSuccess(FAILURE);
				builder = builder.append(validField.errorMsg());
			}
		}
		// 非null及非空字符串验证
		if (validField.notEmpty()) {
			if (value == null || "".equals(value)) {
				result.setSuccess(FAILURE);
				builder = builder.append(validField.errorMsg());
			}
		}

		// ...其他验证
		result.setMessage(builder.toString());
		return result;
	}

	/**
	 * 通过属性名获取指定对象的属性值
	 * 
	 * @param obj 指定对象
	 * @param fieldName 属性名称
	 * @return 属性值
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private Object getObjectFieldByName(Object obj, String fieldName) throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Object value = field.get(obj);
		return value;
	}

	/**
	 * 打印方法调用开始日志
	 * 
	 * @param args 方法入参
	 * @param methodName 方法名称
	 * @param clazz 当前类
	 */
	private void printStartInfo(Object[] args, String methodName, Class<?> clazz) {
		StringBuilder builder = new StringBuilder();
		for (Object object : args) {
			builder.append(JSON.toJSON(object));
		}
		logger.info("类：" + clazz.getName() + "方法为:" + methodName + "开始执行:入参为:" + builder.toString());
	}

	/**
	 * 打印方法调用结束日志
	 * 
	 * @param methodName 方法名称
	 * @param returnParam 返回参数
	 * @param clazz 当前类
	 */
	private void printEndInfo(String methodName, Object returnParam, Class<?> clazz) {
		logger.info("类：" + clazz.getName() + "方法为:" + methodName + "执行结束:出参为:" + JSON.toJSONString(returnParam));
	}
	
	private boolean validRegex(String fieldName){
		return fieldName.matches(REGEXP_FIELD_NAME) ;
	}

}
