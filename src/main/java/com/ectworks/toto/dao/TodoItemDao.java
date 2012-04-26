package com.ectworks.toto.dao;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ectworks.toto.domain.TodoItem;
import com.ectworks.toto.domain.User;

public class TodoItemDao
{
    static Logger log = LoggerFactory.getLogger(TodoItemDao.class);

    private JdbcTemplate jdbc;

    public void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    UserDao userDao;

    public void setUserDao(UserDao userDao)
    {
        this.userDao = userDao;
    }


    public void addItem(TodoItem item)
    {
        log.debug("Adding: {}", item);

        String sql = "insert into todo_item (user_id, desc, completed)"
            + " values(?, ?, ?)";

        User user = userDao.getCurrentUser();

        jdbc.update(sql, new Object[] {
                user.getId(),
                item.getDescription(),
                false
            });

        int id = jdbc.queryForInt("call identity()");

        item.setId(id);
    }

    static class TodoItemMapper implements RowMapper<TodoItem> {
        public TodoItem mapRow(ResultSet rs, int rowNum)
            throws SQLException
        {
            TodoItem item = new TodoItem();
            item.setId(rs.getInt("item_id"));
            item.setDescription(rs.getString("desc"));
            item.setCompleted(rs.getBoolean("completed"));
            return item;
        }
    }

    public TodoItem getItem(int itemId)
    {
        log.debug("Getting item ID: {}", itemId);

        String sql = "select * from todo_item where item_id = ?";

        return jdbc.queryForObject(sql, new Object[] { itemId },
                                   new TodoItemMapper());
    }

    public void completeItem(int itemId)
    {
        log.debug("Completing item ID: {}", itemId);

        String sql = "update todo_item"
            + " set completed = true"
            + " where item_id = ?";

        int rows = jdbc.update(sql, new Object[] { itemId });

        if (rows < 1)
            log.warn("Attempt to complete unknown item ID: {}", itemId);
    }

    public void removeItem(int itemId)
    {
        String sql = "delete from todo_item where item_id = ?";

        int rows = jdbc.update(sql, new Object[] { itemId });

        if (rows < 1)
            log.warn("Attempt to remove unknown item ID: {}", itemId);
    }

    public Iterable<TodoItem> allItems()
    {
        User user = userDao.getCurrentUser();

        String sql = "select * from todo_item "
                   + " where user_id = ?";

        return jdbc.query(sql,
                          new Object[] { user.getId() },
                          new TodoItemMapper());
    }

    public Iterable<TodoItem> pendingItems()
    {
        User user = userDao.getCurrentUser();

        String sql = "select * from todo_item "
                   + "where user_id = ? "
                   + "  and completed = false";

        return jdbc.query(sql,
                          new Object[] { user.getId() },
                          new TodoItemMapper());
    }
}