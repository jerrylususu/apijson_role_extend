package apijson.demo.model;

import apijson.orm.IRequestRole;

import java.util.List;

public class Credential {

    Long id;
    String pwdHash;
    List<String> role;

    public Long getId() {
        return id;
    }

    public Credential setId(Long id) {
        this.id = id;
        return this;
    }

    public String getPwdHash() {
        return pwdHash;
    }

    public Credential setPwdHash(String pwdHash) {
        this.pwdHash = pwdHash;
        return this;
    }

    public List<String> getRole() {
        return role;
    }

    public Credential setRole(List<String> role) {
        this.role = role;
        return this;
    }
}
