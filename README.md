# Mybatis-Field-Interceptor
## 一、工作流程示意图
其中MyBatis隐去了无关组件
 ![image](https://user-images.githubusercontent.com/9764306/147635964-ab9651cc-9bcc-4833-a5b1-3ca4212b2a4e.png)

## 二、配置说明
2.1 导入Maven Jar包
```XML
暂时没有时间发中央仓库，可以自行拷贝源码到你的项目中
```

2.2 创建FieldAccessInterceptor
如果是使用SpringBoot+MyBatis只需要创建一个FieldAccessInterceptor对象并加载为Spring的bean，如以下配置。
```Java
    @Bean
public FieldAccessInterceptor fieldAccessInterceptor(){
        return new FieldAccessInterceptor("com.yourpackage.path.example.xxx"); //注解的类所在的package路径，范围越小启动越快
        }
```
如果自定义了MyBatis SessionFactory，还需要加入插件列表中
```Java
    @Bean
public SqlSessionFactory masterSqlSessionFactory()throws Exception{
        MybatisSqlSessionFactoryBean factoryBean=new MybatisSqlSessionFactoryBean();
        //xxxx 其它配置
        //添加FieldAccessInterceptor mybatis插件
        Interceptor[]plugins={fieldAccessInterceptor()};
        factoryBean.setPlugins(plugins);

        return factoryBean.getObject();
        }
```

## 三、注解使用说明
### 3.1 @EnableFieldAccessInterceptor
该注解标记在Mybatis实体类上，FieldAccessInterceptor在初始化时会扫描被该注解标记的类

### 3.2 @FieldAccess
该注解标记在Mybatis实体类的属性上，FieldAccessInterceptor初始化扫描时，把该注解标记的属性类型、属性名称、属性对应的get方法和set方法、注解内传入的handler类、handler类的参数缓存起来。
它有两个参数，分别是handler和handlerParams，其中handlerParams是个数组类型，支持传入多个参数。
|参数|类型|含义|
|:---|:---|:---|
|handler|	Class<? extends IFieldAccessHandler>|	对该字段执行拦截逻辑的类，必须继承自IFieldAccessHandler|
|handlerParams|	String[]	IFieldAccessHandler|执行过程中需要用到的参数，和具体的IFieldAccessHandler实现配套使用|
```Java
/**
 * admin_user表Data Object
 *
 * @TableName admin_user
 */
@EnableFieldAccessInterceptor
@TableName(value = "admin_user")
@Data
public class AdminUserDO implements Serializable {

 @TableId(value = "id", type = IdType.AUTO)
 private Long id;

 /**
  * 需要加密存储，解密读取的字段，其中EncryptHandler是自定义实现了IFieldAccessHandler的类
  */
 @FieldAccess(handler = EncryptHandler.class)
 @TableField(value = "password")
 private String password;

 @TableField(exist = false)
 private static final long serialVersionUID = 1L;
}
```

### 3.3 @ModifyParamDependency
该注解标记在Mybatis实体类的方法上，被该注解标记的方法，会在Mybatis写入数据库时先执行该方法，该方法返回true才会执行对应的handler逻辑。
方法名可以任意取，返回值和入参列表格式必须如下：

```Java
    /**
 * 判断是否需要在写入数据库前对该字段执行xxxxxHandler的逻辑
 *
 * @param obj 当前类要写入数据库的对象
 * @param fieldName 被@FieldAccess(handler = xxxxxHandler.class)标记的字段名称
 * @param fieldValue 被@FieldAccess(handler = xxxxxHandler.class)标记的字段值
 */
@ModifyParamDependency(xxxxxHandler.class)
public boolean allowExecuteXXXX(Object obj,String fieldName,Object fieldValue){
        // todo 实现你自己的逻辑
        return true;
        }
```

### 3.4 @ModifyResultDependency
该注解标记在Mybatis实体类的方法上，被该注解标记的方法，会在Mybatis返回数据库对象时先执行该方法，该方法返回true才会执行对应的handler逻辑。
方法名可以任意取，返回值和入参列表格式必须如下：
```Java
    /**
     * 判断是否需要在数据库返回对象前对该字段执行xxxxxHandler的逻辑
     *
     * @param obj 当前类从数据库返回时的对象
     * @param fieldName 被@FieldAccess(handler = xxxxxHandler.class)标记的字段名称
     * @param fieldValue 被@FieldAccess(handler = xxxxxHandler.class)标记的字段值
     */
    @ModifyResultDependency(xxxxxHandler.class)
    public boolean allowDecryptPassword(Object obj, String fieldName, Object fieldValue) {
        // todo 实现你自己的逻辑
        return true;
    }
```
