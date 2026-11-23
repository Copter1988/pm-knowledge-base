package com.tpjy.dto;

/** 操作者信息（审计日志用；命令行测试时为 cli/命令行）。 */
public class Operator {

    private Long id;
    private String name;
    private String role;

    public Operator() {
    }

    public Operator(Long id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public static Operator cli() {
        return new Operator(null, "命令行", "CLI");
    }

    public static Operator system() {
        return new Operator(null, "系统定时任务", "SYSTEM");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
