package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.BanStu;
import org.thingsboard.server.common.data.Student;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author hanjinqun
 * @date 2020/3/4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = "ban_stu")
public class BanStuEntity extends BaseSqlEntity<BanStu>implements SearchTextEntity<BanStu> {

    @Column(name = "bId")
    private String bId;
    @Column(name = "sId")
    private String sId;

    @Override
    public String getSearchTextSource() {
        return null;
    }

    @Override
    public void setSearchText(String searchText) {

    }

    @Override
    public BanStu toData() {
        return null;
    }
}
