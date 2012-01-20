package com.ectworks.toto.domain;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TodoItem
{
    public static TodoItem make(String description)
    {
        TodoItem item = new TodoItem();

        item.setDescription(description);

        return item;
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