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
        log.debug("Adding: {}", user);

        String sql = "insert into user (name, password, email_addr)"
            + " values(?, ?, ?)";

        jdbc.update(sql, new Object[] {
                user.getName(),
                user.getPassword(),
                user.getEmail()
            });

        int id = jdbc.queryForInt("call identity()");

        user.setId(id);
    }

    static class UserMapper implements RowMapper<User> {
        public User mapRow(ResultSet rs, int rowNum)
            throws SQLException
        {
            User user = new User();
            user.setId(rs.getInt("user_id"));
            user.setName(rs.getString("name"));
            user.setPassword(rs.getString("password"));
            user.setEmail(rs.getString("email_addr"));
            return user;
        }
    }

    boolean isValidUser(String name)
    {
        log.debug("Determining if user is valid: {}", name);

        String sql = "select count(*) from user where name = ?";

        int count = jdbc.queryForInt(sql, new Object[] { name });

        if (count == 0) {
            log.debug("User IS NOT valid: {}", name);
            return false;
        }

        log.debug("User IS valid: {}", name);

        return true;
    }

    public User getUserByName(String name)
    {
        log.debug("Getting user by name: {}", name);

        if (!isValidUser(name))
            return null;

        String sql = "select * from user where name = ?";

        return jdbc.queryForObject(sql,
                                   new Object[] { name },
                                   new UserMapper());
    }

    public Iterable<User> allUsers()
    {
        String sql = "select * from user";

        return jdbc.query(sql, new UserMapper());
    }
}