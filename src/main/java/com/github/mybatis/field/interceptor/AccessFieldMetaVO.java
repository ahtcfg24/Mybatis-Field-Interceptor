package com.github.mybatis.field.interceptor;


import com.github.mybatis.field.interceptor.handler.IFieldAccessHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 字段元信息Value Object
 * Created on 2021-12-14
 */
public class AccessFieldMetaVO {

    /**
     * 字段
     */
    private Field field;
    /**
     * 字段上的读方法 getXXX()/isXXX()
     */
    private Method readMethod;
    /**
     * 字段上的写方法 setXXX()
     */
    private Method writeMethod;
    /**
     * 修改入参之前的判断方法，可以为null
     */
    private Method paramDependencyMethod;
    /**
     * 修改返回值之前的判断方法，可以为null
     */
    private Method resultDependencyMethod;
    /**
     * 注解上参数对应的handler对象
     */
    private IFieldAccessHandler handler;
    /**
     * 注解上参数对应的handler对象所需的参数，可以为null
     */
    private List<String> handlerParams;

    AccessFieldMetaVO(final Field field, final Method readMethod, final Method writeMethod,
                      final Method paramDependencyMethod, final Method resultDependencyMethod, final IFieldAccessHandler handler,
                      final List<String> handlerParams) {
        this.field = field;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.paramDependencyMethod = paramDependencyMethod;
        this.resultDependencyMethod = resultDependencyMethod;
        this.handler = handler;
        this.handlerParams = handlerParams;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    public Method getWriteMethod() {
        return writeMethod;
    }

    public void setWriteMethod(Method writeMethod) {
        this.writeMethod = writeMethod;
    }

    public Method getParamDependencyMethod() {
        return paramDependencyMethod;
    }

    public void setParamDependencyMethod(Method paramDependencyMethod) {
        this.paramDependencyMethod = paramDependencyMethod;
    }

    public Method getResultDependencyMethod() {
        return resultDependencyMethod;
    }

    public void setResultDependencyMethod(Method resultDependencyMethod) {
        this.resultDependencyMethod = resultDependencyMethod;
    }

    public IFieldAccessHandler getHandler() {
        return handler;
    }

    public void setHandler(IFieldAccessHandler handler) {
        this.handler = handler;
    }

    public List<String> getHandlerParams() {
        return handlerParams;
    }

    public void setHandlerParams(List<String> handlerParams) {
        this.handlerParams = handlerParams;
    }
}
