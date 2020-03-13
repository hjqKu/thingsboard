package com.loit.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import com.loit.common.data.BanStu;
import com.loit.common.data.Student;
import com.loit.dao.model.BaseSqlEntity;
import com.loit.dao.model.SearchTextEntity;
import com.loit.dao.util.mapping.JsonStringType;

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
