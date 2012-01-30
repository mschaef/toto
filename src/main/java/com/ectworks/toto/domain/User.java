package com.ectworks.toto.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class User
{
    static Logger log = LoggerFactory.getLogger(User.class);

    String name = null;

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }
}