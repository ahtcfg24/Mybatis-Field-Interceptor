package com.github.mybatis.field.interceptor;

import com.github.mybatis.field.interceptor.annotation.EnableFieldAccessInterceptor;
import com.github.mybatis.field.interceptor.annotation.FieldAccess;
import com.github.mybatis.field.interceptor.annotation.ModifyParamDependency;
import com.github.mybatis.field.interceptor.annotation.ModifyResultDependency;
import com.github.mybatis.field.interceptor.handler.IFieldAccessHandler;
import com.google.common.collect.Lists;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 拦截属性读写逻辑
 * Created on 2021-12-17
 */
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class FieldAccessInterceptor implements Interceptor {

    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    //  Map<类名,Map<被@FieldAccess注解的字段名,字段上的元信息>>
    private final Map<String, Map<String, AccessFieldMetaVO>> accessClzMetaMap = new HashMap<>();

    public FieldAccessInterceptor(String scanPath) {
        this(scanPath, new HashMap<>());
    }


    public FieldAccessInterceptor(String scanPath,
                                  Map<Class<? extends IFieldAccessHandler>, IFieldAccessHandler> handlerMap) {
        long startTime = System.currentTimeMillis();

        Set<Class<?>> accessClzSet =
                new Reflections(scanPath).getTypesAnnotatedWith(EnableFieldAccessInterceptor.class);
        //保存executor判断方法元信息
        Map<Class<? extends IFieldAccessHandler>, Method> modifyParamDependencyMethodMap = new HashMap<>();
        Map<Class<? extends IFieldAccessHandler>, Method> modifyResultDependencyMethodMap = new HashMap<>();
        buildDependencyMethodMap(accessClzSet, modifyParamDependencyMethodMap, modifyResultDependencyMethodMap);
        //缓存字段上的其它元信息
        for (Class<?> clz : accessClzSet) {
            Map<String, AccessFieldMetaVO> fieldMetaMap =
                    buildFieldMetaMap(clz, handlerMap, modifyParamDependencyMethodMap, modifyResultDependencyMethodMap);
            String clzName = clz.getName();
            if (!fieldMetaMap.isEmpty()) {
                accessClzMetaMap.put(clzName, fieldMetaMap);
            } else {
                log.warn("class {} marked by @EnableMyBatisFieldInterceptor, but not find marked field", clzName);
            }
        }

        log.info("init FieldAccessInterceptor success, cost{}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 构造同一个类中被注解的属性元信息
     */
    private Map<String, AccessFieldMetaVO> buildFieldMetaMap(Class<?> clz,
                                                             Map<Class<? extends IFieldAccessHandler>, IFieldAccessHandler> handlerMap,
                                                             Map<Class<? extends IFieldAccessHandler>, Method> modifyParamDependencyMethodMap,
                                                             Map<Class<? extends IFieldAccessHandler>, Method> modifyResultDependencyMethodMap) {
        Map<String, AccessFieldMetaVO> fieldMetaMap = new HashMap<>();
        for (Field field : clz.getDeclaredFields()) {
            FieldAccess annotation = field.getAnnotation(FieldAccess.class);
            if (annotation == null) {
                continue;
            }
            //只处理被FieldAccess注解的属性
            String fieldName = field.getName();
            Class<? extends IFieldAccessHandler> handlerClz = annotation.handler();
            IFieldAccessHandler handler = handlerMap.get(handlerClz);
            try {
                if (handler == null) {
                    // 没有指定的handler对象，使用反射创建一个
                    IFieldAccessHandler genHandler = handlerClz.getDeclaredConstructor().newInstance();
                    handlerMap.put(handlerClz, genHandler);
                    handler = genHandler;
                }
                PropertyDescriptor p = new PropertyDescriptor(fieldName, clz);
                fieldMetaMap.put(fieldName, new AccessFieldMetaVO(field, p.getReadMethod(), p.getWriteMethod(),
                        modifyParamDependencyMethodMap.get(handlerClz), modifyResultDependencyMethodMap.get(handlerClz),
                        handler, Lists.newArrayList(annotation.handlerParams())));
            } catch (InstantiationException | IntrospectionException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("FieldAccessInterceptor init failed clz={}, field={}", clz.getName(), fieldName, e);
                throw new RuntimeException(e);
            }
        }
        return fieldMetaMap;
    }

    private void buildDependencyMethodMap(Set<Class<?>> clzSet,
                                          Map<Class<? extends IFieldAccessHandler>, Method> modifyParamDependencyMethodMap,
                                          Map<Class<? extends IFieldAccessHandler>, Method> modifyResultDependencyMethodMap) {
        clzSet.forEach(clz -> {
            for (Method method : clz.getDeclaredMethods()) {
                ModifyParamDependency paramDependency = method.getAnnotation(ModifyParamDependency.class);
                ModifyResultDependency resultDependency = method.getAnnotation(ModifyResultDependency.class);
                if (paramDependency != null || resultDependency != null) {
                    if (!Boolean.TYPE.equals(method.getReturnType())) {
                        log.error("class {} method {} was marked by @ModifyXXXXDependency, but not return type boolean", clz.getName(), method.getName());
                        throw new RuntimeException("invalid @ModifyXXXXDependency with " + method.getName());
                    }
                    if (!checkDependencyMethodParam(method.getParameterTypes())) {
                        log.error("class {} method {} was marked by @ModifyXXXXDependency, but params need set to type of [Object, String, Object]",
                                clz.getName(), method.getName());
                        throw new RuntimeException("invalid @ModifyXXXXDependency with " + method.getName());
                    }
                    if (paramDependency != null) {
                        modifyParamDependencyMethodMap.put(paramDependency.value(), method);
                    } else {
                        modifyResultDependencyMethodMap.put(resultDependency.value(), method);
                    }
                }
            }
        });
    }

    private boolean checkDependencyMethodParam(Class<?>[] parameterTypes) {
        return parameterTypes != null
                && parameterTypes.length == 3
                && Object.class.equals(parameterTypes[0])
                && String.class.equals(parameterTypes[1])
                && Object.class.equals(parameterTypes[2]);
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof ResultSetHandler && args != null && args.length == 1) {
            // 拦截读取 ResultSetHandler#handleResultSets
            return interceptResult(invocation);
        }
        if (target instanceof Executor && args != null && args.length == 2) {
            // 拦截写入 Executor#update
            return interceptParam(invocation);
        }
        // 啥也不干
        return invocation.proceed();
    }


    /**
     * 拦截数据库返回值
     * 先正常执行mybatis的handleResultSets方法，再对已经赋值完毕的对象修改属性值
     */
    private Object interceptResult(Invocation invocation) throws Exception {
        Object result = invocation.proceed();
        Set<Object> resultObjSet = new HashSet<>();
        //返回值可能是单个对象，也可能是对象列表，用Set收集起来防止重复拦截
        if (result instanceof ArrayList) {
            resultObjSet.addAll((ArrayList) result);
        } else {
            resultObjSet.add(result);
        }
        //处理具体对象
        for (Object resultObj : resultObjSet) {
            String clzName = resultObj.getClass().getName();
            Map<String, AccessFieldMetaVO> fieldMetaMap = accessClzMetaMap.get(clzName);
            if (fieldMetaMap == null) {
                //查不到说明这个对象的类没有被注解标注，不需要修改属性
                continue;
            }
            for (Entry<String, AccessFieldMetaVO> entry : fieldMetaMap.entrySet()) {
                String fieldName = entry.getKey();
                AccessFieldMetaVO fieldMeta = entry.getValue();
                modifyResultObjectField(resultObj, fieldName, fieldMeta);
            }
        }
        //返回已经修改后的执行结果
        return result;
    }

    /**
     * 修改mybatis返回的的对象属性
     */
    private void modifyResultObjectField(Object resultObj, String fieldName, AccessFieldMetaVO fieldMeta)
            throws Exception {
        // 1. 先执行被注解属性的前置依赖方法
        Method dependencyMethod = fieldMeta.getResultDependencyMethod();
        Object oldValue = fieldMeta.getReadMethod().invoke(resultObj);
        boolean dependencyResult = true;
        if (dependencyMethod != null) {
            dependencyResult = (boolean) dependencyMethod.invoke(resultObj, resultObj, fieldName, oldValue);
        }
        // 2. 再回调handler的allowRead方法
        IFieldAccessHandler handler = fieldMeta.getHandler();
        if (dependencyResult && handler
                .allowModifyResult(fieldName, oldValue, resultObj, fieldMeta.getHandlerParams())) {
            // 3. 最后回调handler的read方法
            Object newValue = handler.modifyResult(fieldName, oldValue, resultObj, fieldMeta.getHandlerParams());
            fieldMeta.getWriteMethod().invoke(resultObj, newValue);
        }
    }


    /**
     * 拦截写入到数据库的参数对象
     */
    private Object interceptParam(Invocation invocation) throws Exception {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        if ((SqlCommandType.INSERT == ms.getSqlCommandType() || SqlCommandType.UPDATE == ms.getSqlCommandType())
                && parameter != null) {
            //入参可能是单个对象，也可能是对象列表或Map，用Set收集起来防止重复拦截
            Set<Object> needHandleParamObject = new HashSet<>();
            if (parameter instanceof Map) {
                Map map = (Map) parameter;
                for (Object obj : map.values()) {
                    if (obj instanceof List) {
                        List objList = (List) obj;
                        needHandleParamObject.addAll(objList);
                    } else {
                        needHandleParamObject.add(obj);
                    }
                }
            } else if (parameter instanceof Collection) {
                needHandleParamObject.addAll((Collection) parameter);
            } else {
                needHandleParamObject.add(parameter);
            }

            for (Object obj : needHandleParamObject) {
                String clzName = obj.getClass().getName();
                Map<String, AccessFieldMetaVO> fieldMetaMap = accessClzMetaMap.get(clzName);
                if (fieldMetaMap == null) {
                    //找不到说明没有被注解标注，不需要修改
                    continue;
                }
                for (Entry<String, AccessFieldMetaVO> entry : fieldMetaMap.entrySet()) {
                    String fieldName = entry.getKey();
                    AccessFieldMetaVO fieldMeta = entry.getValue();
                    modifyParamObjectField(obj, fieldName, fieldMeta);
                }
            }
        }
        //最后写入数据库
        return invocation.proceed();
    }

    /**
     * 修改传到mybatis的参数对象属性
     */
    private void modifyParamObjectField(Object parameterObj, String fieldName, AccessFieldMetaVO fieldMeta)
            throws Exception {
        IFieldAccessHandler handler = fieldMeta.getHandler();
        Object oldValue = fieldMeta.getReadMethod().invoke(parameterObj);
        Method dependencyMethod = fieldMeta.getParamDependencyMethod();
        boolean dependencyResult = true;
        if (dependencyMethod != null) {
            dependencyResult =
                    (boolean) dependencyMethod.invoke(parameterObj, parameterObj, fieldName, oldValue);
        }
        if (dependencyResult && handler
                .allowModifyParam(fieldName, oldValue, parameterObj, fieldMeta.getHandlerParams())) {
            fieldMeta.getWriteMethod().invoke(parameterObj,
                    handler.modifyParam(fieldName, oldValue, parameterObj, fieldMeta.getHandlerParams()));
        }
    }


    @Override
    public Object plugin(Object target) {
        if (target instanceof ResultSetHandler || target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}
