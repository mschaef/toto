package com.ectworks.toto.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TodoItem
{
    static Logger log = LoggerFactory.getLogger(TodoItem.class);

    public static TodoItem make(String description)
    {
        TodoItem item = new TodoItem();

        item.setDescription(description);

        return item;
    }

    int id = -1;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        if (this.id != -1)
            log.error("Attemping to update ID from {}", this.id);

        this.id = id;
    }

    String description = null;

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    boolean completed = false;

    public void setCompleted(boolean completed)
    {
        this.completed = completed;
    }

    public boolean getCompleted()
    {
        return completed;
    }

    public String toString()
    {
        return ReflectionToStringBuilder.toString(this);
    }
}