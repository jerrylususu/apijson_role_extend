package apijson.demo.model;

import apijson.orm.Visitor;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.Collections;
import java.util.List;

public class User implements Visitor<Long> {
    Long id;
    String username;
    String realname;
    String note;

    List<String> role;

    public Long getId() {
        return id;
    }

    @JSONField(serialize = false)
    @Override
    public List<Long> getContactIdList() {
        // placeholder
        return Collections.emptyList();
    }

    public User setId(Long id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getRealname() {
        return realname;
    }

    public User setRealname(String realname) {
        this.realname = realname;
        return this;
    }

    public String getNote() {
        return note;
    }

    public User setNote(String note) {
        this.note = note;
        return this;
    }

    @JSONField(serialize = false)
    public List<String> getRole() {
        return role;
    }

    public User setRole(List<String> role) {
        this.role = role;
        return this;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", realname='" + realname + '\'' +
                ", note='" + note + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
