package com.ectworks.toto.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class User
{
    static Logger log = LoggerFactory.getLogger(User.class);

    long id = -1;

    public void setId(long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return id;
    }

    String name = null;

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    String password = null;

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    String email = null;

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getEmail()
    {
        return email;
    }

    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }
}