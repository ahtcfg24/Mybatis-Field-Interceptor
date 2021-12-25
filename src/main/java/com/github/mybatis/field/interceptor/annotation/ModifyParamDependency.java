package com.github.mybatis.field.interceptor.annotation;


import com.github.mybatis.field.interceptor.handler.IFieldAccessHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 被该注解标注的方法，会在执行对应属性的handler之前先调用，判断是否能调用
 * 被注解的方法必须返回boolean,并且有一个String参数和一个Object参数
 * Created on 2021-12-13
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ModifyParamDependency {

    Class<? extends IFieldAccessHandler> value();
}
