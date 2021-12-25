package com.github.mybatis.field.interceptor.handler;

import java.util.List;

/**
 * MyBatis属性拦截器的处理接口
 * Created on 2021-12-14
 */
public interface IFieldAccessHandler {

    /**
     * 判断是否允许修改读取结果
     *
     * @param fieldName  字段名
     * @param fieldValue 字段的值
     * @param resultObj  字段所属的对象
     * @return 只有返回true的时候才会执行execute()
     */
    boolean allowModifyResult(String fieldName, Object fieldValue, Object resultObj, List<String> handleParams)
            throws Exception;

    /**
     * 把数据库读出来的旧值转换为新值的方法，会在拦截器中被调用
     *
     * @param fieldName  字段名
     * @param fieldValue 字段的值
     * @return 新值
     */
    Object modifyResult(String fieldName, Object fieldValue, Object result, List<String> handleParams)
            throws Exception;

    /**
     * 判断是否允许修改写入参数
     *
     * @param fieldName  字段名
     * @param fieldValue 字段的值
     * @param paramObj   字段所属的对象
     * @return 只有返回true的时候才会执行execute()
     */
    boolean allowModifyParam(String fieldName, Object fieldValue, Object paramObj, List<String> handleParams)
            throws Exception;

    /**
     * 把要写入数据库的旧值转换为新值的方法，会在拦截器中被调用
     *
     * @param fieldName  字段名
     * @param fieldValue 字段的值
     * @return 新值
     */
    Object modifyParam(String fieldName, Object fieldValue, Object paramObj, List<String> handleParams)
            throws Exception;

}
