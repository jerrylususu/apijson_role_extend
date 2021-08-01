package apijson.demo.config;

import apijson.framework.APIJSONSQLConfig;

public class DemoSQLConfig extends APIJSONSQLConfig {


    static {
        DEFAULT_DATABASE = "MYSQL";  // MYSQL, POSTGRESQL, SQLSERVER, ORACLE, DB2
        DEFAULT_SCHEMA = "apijson_role_extend";  // TODO 默认模式名，改成你自己的，默认情况是 MySQL: sys, PostgreSQL: public, SQL Server: dbo, Oracle:
    }

    @Override
    public String getDBVersion() {
        return "8.0";
    }

    @Override
    public String getDBUri() {
        return "jdbc:mysql://192.168.99.100:33308?serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=UTF-8";
    }

    @Override
    public String getDBAccount() {
        return "root";
    }

    @Override
    public String getDBPassword() {
        return "apijson";
    }
}
