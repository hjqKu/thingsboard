package com.loit.dao.sql;

import com.google.common.collect.Lists;
import com.loit.dao.IotDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author hanjinqun
 * @date 2020/3/19
 */
@Slf4j
public abstract class JpaAbstractIotDao<E> implements IotDao<E> {
    protected abstract CrudRepository<E, String> getCrudRepository();
    @Override
    public List<E> find() {
        List<E> entities = Lists.newArrayList(getCrudRepository().findAll());
        return entities;
    }

    @Override
    public E findById(String id) {
        Optional<E> entity = getCrudRepository().findById(id);
        return entity.get();
    }


    @Override
    public E save(E t) {
        E entity;
        System.out.println(getCrudRepository());
        entity = getCrudRepository().save(t);
        return entity;
    }

    @Override
    public boolean removeById(String id) {
        getCrudRepository().deleteById(id);
        log.debug("Remove request: {}", id);
        return !getCrudRepository().existsById(id);
    }
}
