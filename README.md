# APIJSON Role Extend

源项目：[Tencent/APIJSON: 🚀 零代码、热更新、全自动 ORM 库，后端接口和文档零代码，前端(客户端) 定制返回 JSON 的数据和结构。 🚀 A JSON Transmission Protocol and an ORM Library for automatically providing APIs and Docs. (github.com)](https://github.com/Tencent/APIJSON/)

本项目尝试使用[基于接口的可扩展枚举](https://jiapengcai.gitbooks.io/effective-java/content/di-6-zhang-ff1a-mei-ju-he-zhu-jie/di-38-tiao-ff1a-cai-yong-jie-kou-lai-mo-fang-ke-kuo-zhan-de-mei-ju.html)，为 APIJSON 提供更多的权限支持。

## 问题描述

APIJSON 本身的权限定义在枚举`RequestRole` 中，且与框架本身的耦合度较高，如果想要在此基础上定义新的权限级别不是很方便。虽然可以通过重载 `AbstractVerifier.verifyAccess ` 方法来自定义鉴权，但这并不能视作为可扩展的权限系统，因为无法使用 `RequestRole` 中定义之外的权限。

## 解决思路

这一问题的核心是权限定义在了枚举里，所以难以扩展。「采用接口来模仿可扩展的枚举」是 *Effective Java* 中介绍的一种技巧，其原理是定义一个接口，然后让枚举实现这个接口，在所有使用枚举的地方换用接口，从而模仿了一个可扩展的枚举。

在本项目中，定义了一个新的接口 `IRequestRole`，令 `RequestRole` 实现这个接口，再依次修改框架中其他相关联的位置。通过 `IRequestRole.register` 方法，用户可以注册自己的权限枚举。通过 `IRequestRole.get` 方法，可以从权限字符串转换回对应的权限枚举。

## 主要修改

* 建立 `IRequestRole` （位于 `apijson.orm` 包中）
* 令 `RequestRole` 实现 `IRequestRole`，并将其内的 `get` 方法移至 `IRequestRole` 中实现
* `MethodAccess` 注解返回 `String[]`，因为 `IRequestRole[]` 编译时不确定
* `AbstractVerifier` 中增加一个 `convertStringToIRequestRole` 方法，从 `String[]` 转换到 `IRequestRole[]`。`verifyAccess` 方法中的 switch 转换为 if else。
* `APIJSONApplication` 的 static 块中使用 `IRequestRole.register` 注册内建的 `RequestRole`

## 用户使用

1. 首先定义自己的权限枚举类（可参照 `apijson.demo.config.MyRole`）

   ```java
   public enum MyRole implements IRequestRole {
       STUDENT,
       TEACHER,
       PRINCIPAL;
   }
   ```

2. 在 `DemoApplication` 的 static 块中注册自己的权限枚举类

   ```java
   static {
       IRequestRole.register(MyRole.class);
       APIJSONApplication.DEFAULT_APIJSON_CREATOR = // ...
   }
   ```

3. 在自己的用户类（`model.User`）中添加 `List<String> role` 属性，并添加对应的 getter, setter 方法 

4. 重载 `AbstractVerifier.verifyAccess`，补全一部分框架中未完成的鉴权逻辑（判断用户请求中声明的权限，是否的确存在于用户的权限列表中）

   > 此部分与用户自己的用户类的类名相关，因此没有在框架中实现（可能可以用 `Visitor` 接口实现？）

   ```java
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
   ```

5. 在数据库 `Access` 表中完成对应配置

   ```sql
   INSERT INTO apijson_role_extend.Access (id, debug, name, alias, get, head, gets, heads, post, put, `delete`, date, detail) VALUES (1, 0, 'Course', '', '[ "STUDENT", "TEACHER", "PRINCIPAL" ]', '[ "UNKNOWN", "STUDENT", "TEACHER","PRINCIPAL" ]', '[ "UNKNOWN", "STUDENT", "TEACHER","PRINCIPAL" ]', '[ "UNKNOWN", "STUDENT", "TEACHER","PRINCIPAL" ]', '["TEACHER"]', '["OWNER","PRINCIPAL"]', '["TEACHER"]', '2018-11-29 00:28:53', null);
   ```

6. 在调用时先登录，然后用 `@role` 声明对应的权限

   ```json
   // POST /get
   {
       "[]": {
           "Course": {
               "@role": "TEACHER"
           }
       }
   }
   ```

## 破坏性改动

* MethodAccess 类型修改



## 示例项目

本项目中内置了改动后 APIJSON ORM 和 Framework 的代码，导入 Intellij IDEA 或其他开发工具后应可以直接运行 `DemoApplication`。

> 开发环境：Windows 10，JDK 13，MariaDB 10.5.9

### 场景

本项目模拟一个选课系统，为简化场景，其中业务表仅有 `Course` （课程）一张。

在 APIJSON 内置的权限之外，定义了三个权限：

- STUDENT，学生，仅可以查看课程，但不能修改
- TEACHER，教师，可以新建、修改、删除自己的课程，但不能删除其他教师的课程
- PRINCIPAL，校长，可以修改老师创建的课程，但不能新建和修改

此外，因为课程是教师创建的，教师对课程持有 OWNER 权限。另外，未登录的用户不能访问系统的任何功能。

### 权限配置

基于以上场景，对 `Course` 表配置权限如下。

* GET/GETS/HEAD/HEADS：["STUDENT", "TEACHER","PRINCIPAL"]
* POST/DELETE：["TEACHER"]
* PUT：["OWNER","PRINCIPAL"]

### 可用用户

内置了四个用户，密码均为 `1234`。用户的公开信息存储在 `User`表中，私密信息存储在 `Credential` 表中。

| id   | username  | password | role                   |
| ---- | --------- | -------- | ---------------------- |
| 1    | jerry     | 1234     | ["STUDENT", "TEACHER"] |
| 2    | neko      | 1234     | ["STUDENT"]            |
| 3    | doge      | 1234     | ["TEACHER"]            |
| 4    | principal | 1234     | ["PRINCIPAL"]          |

### 初始化

1. 在 MySQL / MariaDB 中执行项目根目录 `initdb.sql`，并在 `DemoSQLConfig` 中配置相关连接属性。
2. （可选）使用 Postman，导入项目根目录的 `role_extend.postman_collection.json`，内有主要的几个测试接口。
3. 使用 IDEA 或其他开发工具打开项目目录，运行 `DemoApplication`。

### 部分调用示例

登录 /login

```json
{
    "username":"jerry",
    "password":"1234"
}
```

获取课程 /get

```json
{
    "[]": {
        "Course": {
            "@role": "TEACHER"
        }
    }
}
```

课程计数 /head

```json
{
    "Course": {
        "@role": "STUDENT"
    }
}
```

新建课程 /post

```json
{
    "Course": {
        "sysid": "CS244",
        "name": "分布式系统",
        "teacher": "t1",
        "location": "二教222",
        "capacity": 80,
        "@role": "TEACHER" // 换成 STUDENT 就会鉴权失败了
    },
    "tag": "Course"
}
```

修改课程 /put

```json
{
    "Course": {
        "id": 1627252238116,
        "capacity": 20,
        "@role": "OWNER" // 或者 "PRINCIPLE"
    },
    "tag": "Course"
}
```

删除课程 /delete

```json
{
    "Course": {
        "id": 1627772387598,
        "@role": "TEACHER"
    },
    "tag": "Course"
}
```

