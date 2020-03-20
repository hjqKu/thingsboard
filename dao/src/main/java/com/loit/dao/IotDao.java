package com.loit.dao;


import java.util.List;

/**
 * @author hanjinqun
 * @date 2020/3/19
 */
public interface IotDao<E> {
    List<E> find();

    E findById(String id);

    E save(E t);

    boolean removeById(String id);
}
