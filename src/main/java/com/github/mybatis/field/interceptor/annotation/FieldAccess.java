package com.github.mybatis.field.interceptor.annotation;


import com.github.mybatis.field.interceptor.handler.IFieldAccessHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 用于标记哪些属性在MyBatis查询出来时需要被拦截
 * Created on 2021-12-13
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface FieldAccess {

    Class<? extends IFieldAccessHandler> handler();

    String[] handlerParams() default {};
}
