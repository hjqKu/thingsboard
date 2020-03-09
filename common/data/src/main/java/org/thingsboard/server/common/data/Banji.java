package org.thingsboard.server.common.data;

import java.io.Serializable;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
public class Banji implements Serializable {
    private String id;
    private String name;
    private String tag;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
