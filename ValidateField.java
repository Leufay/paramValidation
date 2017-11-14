package com.chemcn.ec.servicecenter.finance.service.paramValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 验证的属性注解
 * @author Mrlxf
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ValidateField {
	
	/** 拦截的方法参数的索引 , 默认取第一个参数 */
	public int index() default 0 ;
	
	/** 
	 * <p>属性名称 (基本数据类型{@code or String} 无需指定该参数,如果是对象类型,需判断其某个属性时则需要指定判断的属性名称) 
	 * <p>格式要求为 对象名称.属性名称 , 例如 person对象中的name属性 , 则格式为 {@code person.name}
	 * */
	public String fieldName() default "" ;
	
	/** 不能为{@code null} , 默认false表示可以为null */
	public boolean notNull() default false ;
	
	/** 不能为空字符串 */
	public boolean notEmpty() default false ;
	
	/** 是否是集合,默认false表示不是集合类型 */
	public boolean isCollection() default false ;
	
	/** 错误消息 */
	public String errorMsg() default "" ;

}
