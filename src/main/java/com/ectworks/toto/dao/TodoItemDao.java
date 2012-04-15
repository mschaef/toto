package com.ectworks.toto.dao;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ectworks.toto.domain.TodoItem;

public class TodoItemDao
{
    static Logger log = LoggerFactory.getLogger(TodoItemDao.class);

    private JdbcTemplate jdbc;

    public void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    int nextId = 1;

    Map<Integer, TodoItem> items = new HashMap<Integer, TodoItem>();

    public void addItem(TodoItem item)
    {
        log.debug("Adding: {}", item);

        String sql = "insert into todo_item (list_id, desc, completed)"
            + " values(?, ?, ?)";

        jdbc.update(sql, new Object[] {
                0,
                item.getDescription(),
                false
            });

        int id = jdbc.queryForInt("call identity()");

        item.setId(id);
    }

    public TodoItem getItem(int itemId)
    {
        log.debug("Getting item ID: {}", itemId);

        if (!items.containsKey(itemId)) {
            log.warn("Attempt to remove unknown item ID: {}", itemId);
            return null;
        }

        return items.get(itemId);
    }

    public void completeItem(int itemId)
    {
        log.debug("Completing item ID: {}", itemId);

        getItem(itemId).setCompleted(true);
    }

    public void removeItem(int itemId)
    {
        log.debug("Removing item ID: {}", itemId);

        if (!items.containsKey(itemId)) {
            log.warn("Attempt to remove unknown item ID: {}", itemId);
            return;
        }

        items.remove(itemId);
    }

    public Iterable<TodoItem> allItems()
    {
        log.debug("Finding all items.");

        return items.values();
    }

    public Iterable<TodoItem> pendingItems()
    {
        log.debug("Finding pending items.");

        List<TodoItem> pending = new LinkedList<TodoItem>();

        for(TodoItem item : allItems()) {
            if (item.getCompleted())
                continue;

            pending.add(item);
        }

        return pending;
    }
}