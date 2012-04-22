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

import com.ectworks.toto.domain.User;

public class UserDao
{
    static Logger log = LoggerFactory.getLogger(UserDao.class);

    private JdbcTemplate jdbc;

    public void setDataSource(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
    }

    public void addUser(User user)
    {
    }

    public User getUserByName(String name)
    {
        return null;
    }

    public Iterable<User> allUsers()
    {
        return null;
    }
}