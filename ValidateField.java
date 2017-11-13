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
	
	/** 属性的索引 */
	public int index() default -1 ;
	
	/** 属性名称 (基本数据类型{@code or String} 无需指定该参数,如果是对象类型,需判断其某个属性时则需要指定判断的属性名称) */
	public String fieldName() default "" ;
	
	/** 不能为{@code null} */
	public boolean notNull() default false ;
	
	/** 不能为空字符串 */
	public boolean notEmpty() default false ;
	
	public boolean isCollection() default false ;
	
	/** 错误消息 */
	public String errorMsg() default "" ;

}
