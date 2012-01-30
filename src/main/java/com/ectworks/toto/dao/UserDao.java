package com.ectworks.toto.dao;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ectworks.toto.domain.User;

public class UserDao
{
    static Logger log = LoggerFactory.getLogger(UserDao.class);

    Map<String, User> users = new HashMap<String, User>();

    public void addUser(User user)
    {
        if (users.containsKey(user.getName())) {
            log.warn("Attempt to add duplicate user: {}", user);
            return;
        }

        users.put(user.getName(), user);
    }

    public User getUser(String userName)
    {
        log.debug("Getting user: {}", userName);

        if (!users.containsKey(userName)) {
            log.warn("Attempt to get unknown user: {}", userName);
            return null;
        }

        return users.get(userName);
    }


}