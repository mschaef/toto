package com.ectworks.toto.dao;

import java.util.List;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ectworks.toto.domain.TodoItem;

public class TodoItemDao
{
    Logger log = LoggerFactory.getLogger(TodoItemDao.class);

    List<TodoItem> items = new LinkedList<TodoItem>();

    public void addItem(TodoItem item)
    {
        log.debug("Adding: {}", item);

        items.add(item);
    }

    public void removeItem(TodoItem item)
    {
        log.debug("Removing: {}", item);

        if (!items.contains(item)) {
            log.warn("Attempt to remove unknown item: {}", item);
            return;
        }
    }

    public List<TodoItem> allItems()
    {
        log.debug("Finding all items.");

        return new LinkedList<TodoItem>(items);
    }

    public List<TodoItem> pendingItems()
    {
        log.debug("Finding pending items.");

        List<TodoItem> pending = new LinkedList<TodoItem>();

        for(TodoItem item : items) {
            if (item.getCompleted())
                continue;

            pending.add(item);
        }

        return pending;
    }
}