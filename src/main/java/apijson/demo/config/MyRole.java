package apijson.demo.config;

import apijson.orm.IRequestRole;

public enum MyRole implements IRequestRole {
    STUDENT,
    TEACHER,
    PRINCIPAL;
}
