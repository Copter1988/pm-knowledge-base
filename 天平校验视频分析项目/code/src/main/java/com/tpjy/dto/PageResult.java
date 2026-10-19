package com.tpjy.dto;

import java.util.ArrayList;
import java.util.List;

/** 分页结果（供页面查询使用）。 */
public class PageResult<T> {

    private long total;
    private int page;
    private int pageSize;
    private List<T> list = new ArrayList<T>();

    public static <T> PageResult<T> of(long total, int page, int pageSize, List<T> list) {
        PageResult<T> r = new PageResult<T>();
        r.total = total;
        r.page = page;
        r.pageSize = pageSize;
        r.list = list;
        return r;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
