package apijson.demo.config;

import apijson.*;
import apijson.demo.model.User;
import apijson.framework.APIJSONVerifier;
import apijson.orm.IRequestRole;
import apijson.orm.SQLConfig;
import com.alibaba.fastjson.JSONArray;

import javax.activation.UnsupportedDataTypeException;
import java.util.*;

import static apijson.RequestMethod.POST;

public class DemoVerifier extends APIJSONVerifier {

    @Override
    public boolean verifyAccess(SQLConfig config) throws Exception {

        System.out.println("debug -------------------------");
        System.out.println("config = " + config);
        System.out.println("config.getTable() = " + config.getTable());
        System.out.println("config.getRole() = " + config.getRole());
        System.out.println("config.getMethod() = " + config.getMethod());
        System.out.println("config.getColumn() = " + config.getColumn());
        System.out.println("config.getTable() = " + config.getTable());
        System.out.println("config.getContent() = " + config.getContent());
        System.out.println("config.getIdKey() = " + config.getIdKey());
        System.out.println("config.getId() = " + config.getId());
        System.out.println("config.getUserIdKey() = " + config.getUserIdKey());
        System.out.println("debug end -------------------------");

        String table = config == null ? null : config.getTable();
        if (table == null) {
            return true;
        }

        // 用户在请求中用 @role 主动传入的 role
        IRequestRole role = config.getRole();
        if (role == null) {
            role = RequestRole.UNKNOWN;
        }

        if (role != RequestRole.UNKNOWN) {//未登录的角色
            verifyLogin();
        }

        // 已经有登录态了，没必要再验证，一定是 User
//        if (!(this.visitor instanceof User)){
//            throw new IllegalStateException("visitor not user?");
//        }

        // 自定义的权限，需要检查是否存在于列表中
        if ( !(config.getRole() instanceof RequestRole) ) {
            List<String> visitorRoleList = new ArrayList<>();
            // 处理数据库中 role 列为 NULL
            if (((User) this.visitor).getRole() != null) {
                visitorRoleList = ((User) this.visitor).getRole();
            }

            if (!visitorRoleList.contains(config.getRole().toString())){
                // 用户声明的权限不在自己的 role 列表中（伪造权限）
                throw new IllegalAccessException("当前用户没有声明的权限！");
            }

        }


        // 表 + 方法级检查
        RequestMethod method = config.getMethod();

        verifyRole(table, method, role);//验证允许的角色


        //验证角色，假定真实强制匹配<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

        // 行级检查
        String visitorIdkey = getVisitorIdKey(config);

        Object requestId;
        //unknown，verifyRole通过就行
        // 这里只是把 switch 改成了 if else，因为没法对接口 switch
        if (RequestRole.LOGIN.equals(role))
        { }
        else if (RequestRole.CONTACT.equals(role) || RequestRole.CIRCLE.equals(role))
        {   //TODO 做一个缓存contactMap<visitorId, contactArray>，提高[]:{}查询性能， removeAccessInfo时map.remove(visitorId)
            //不能在Visitor内null -> [] ! 否则会导致某些查询加上不需要的条件！
            List<Object> list = visitor.getContactIdList() == null
                    ? new ArrayList<Object>() : new ArrayList<Object>(visitor.getContactIdList());
            if (role == RequestRole.CIRCLE) {
                list.add(visitorId);
            }

            //key!{}:[] 或 其它没有明确id的条件 等 可以和key{}:list组合。类型错误就报错
            requestId = (Number) config.getWhere(visitorIdkey, true);//JSON里数值不能保证是Long，可能是Integer
            @SuppressWarnings("unchecked")
            Collection<Object> requestIdArray = (Collection<Object>) config.getWhere(visitorIdkey + "{}", true);//不能是 &{}， |{} 不要传，直接{}
            if (requestId != null) {
                if (requestIdArray == null) {
                    requestIdArray = new JSONArray();
                }
                requestIdArray.add(requestId);
            }

            if (requestIdArray == null) {//可能是@得到 || requestIdArray.isEmpty()) {//请求未声明key:id或key{}:[...]条件，自动补全
                config.putWhere(visitorIdkey + "{}", JSON.parseArray(list), true); //key{}:[]有效，SQLConfig里throw NotExistException
            } else {//请求已声明key:id或key{}:[]条件，直接验证
                for (Object id : requestIdArray) {
                    if (id == null) {
                        continue;
                    }
                    if (id instanceof Number == false) {//不能准确地判断Long，可能是Integer
                        throw new UnsupportedDataTypeException(table + ".id类型错误，id类型必须是Long！");
                    }
                    if (list.contains(Long.valueOf("" + id)) == false) {//Integer等转为Long才能正确判断。强转崩溃
                        throw new IllegalAccessException(visitorIdkey + " = " + id + " 的 " + table
                                + " 不允许 " + role.toString() + " 用户的 " + method.name() + " 请求！");
                    }
                }
            }
        }
        else if (RequestRole.OWNER.equals(role))
        {
            if (config.getMethod() == POST) {
                List<String> c = config.getColumn();
                List<List<Object>> ovs = config.getValues();
                if ((c == null || c.isEmpty()) || (ovs == null || ovs.isEmpty())) {
                    throw new IllegalArgumentException("POST 请求必须在Table内设置要保存的 key:value ！");
                }

                int index = c.indexOf(visitorIdkey);
                if (index >= 0) {
                    Object oid;
                    for (List<Object> ovl : ovs) {
                        oid = ovl == null || index >= ovl.size() ? null : ovl.get(index);
                        if (oid == null || StringUtil.getString(oid).equals("" + visitorId) == false) {
                            throw new IllegalAccessException(visitorIdkey + " = " + oid + " 的 " + table
                                    + " 不允许 " + role.toString() + " 用户的 " + method.name() + " 请求！");
                        }
                    }
                } else {
                    List<String> nc = new ArrayList<>(c);
                    nc.add(visitorIdkey);
                    config.setColumn(nc);

                    List<List<Object>> nvs = new ArrayList<>();
                    List<Object> nvl;
                    for (List<Object> ovl : ovs) {
                        nvl = ovl == null || ovl.isEmpty() ? new ArrayList<>() : new ArrayList<>(ovl);
                        nvl.add(visitorId);
                        nvs.add(nvl);
                    }

                    config.setValues(nvs);
                }
            } else {
                requestId = config.getWhere(visitorIdkey, true);//JSON里数值不能保证是Long，可能是Integer
                if (requestId != null && StringUtil.getString(requestId).equals(StringUtil.getString(visitorId)) == false) {
                    throw new IllegalAccessException(visitorIdkey + " = " + requestId + " 的 " + table
                            + " 不允许 " + role.toString() + " 用户的 " + method.name() + " 请求！");
                }

                config.putWhere(visitorIdkey, visitorId, true);
            }
        }
        else if (RequestRole.ADMIN.equals(role))
        {//这里不好做，在特定接口内部判。 可以是  /get/admin + 固定秘钥  Parser#noVerify，之后全局跳过验证
            verifyAdmin();
        }
        // 以上判断完了内置的权限，下面就是自己的权限处理了
        else if (role instanceof MyRole) {
            // 我自己的类的权限设定
            System.out.println("进入自定义权限部分");
            // 自己的权限设定实现
        } else if (IRequestRole.values.contains(role)){
            System.out.println("不在预定义范围内的 role: " + role);
        }
        else {
            // UNKNOWN，直接过
        }

        //验证角色，假定真实强制匹配>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


        return true;
    }



}
