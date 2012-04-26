package com.ectworks.toto.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.ectworks.toto.domain.TodoItem;
import com.ectworks.toto.dao.TodoItemDao;

@Controller
public class TodoListController
{
    static Logger log = LoggerFactory.getLogger(TodoListController.class);

    private TodoItemDao todoItemDao;

    public void setTodoItemDao(TodoItemDao todoItemDao)
    {
        this.todoItemDao = todoItemDao;
    }

    @RequestMapping(value = "/todo",
                    method = RequestMethod.GET)
    public String showTodoList(Model model,
                               HttpServletResponse response)
    {
        log.debug("Fetching Todo List");

        response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        model.addAttribute("todoItems", todoItemDao.pendingItems());

        return "todo";
    }

    @RequestMapping(value = "/todo",
                    method = RequestMethod.POST)
    public String addTodoItem(Model model,
                              @RequestParam(required = true) String desc)
    {
        log.debug("Posting Todo Item, desc: {}", desc);

        todoItemDao.addItem(TodoItem.make(desc));

        return "redirect:/todo";
    }

    @RequestMapping(value = "/remove",
                    method = RequestMethod.POST)
    public void removeTodoItem(Model model,
                               @RequestParam(required = true) int itemId,
                               HttpServletResponse response)
    {
        log.debug("Removing Todo Item: {}", itemId);

        todoItemDao.removeItem(itemId);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping(value = "/complete",
                    method = RequestMethod.POST)
    public void completeTodoItem(Model model,
                                 @RequestParam(required = true) int itemId,
                                 HttpServletResponse response)
    {
        log.debug("Completing Todo Item: {}", itemId);

        todoItemDao.completeItem(itemId);

        response.setStatus(HttpServletResponse.SC_OK);
    }
}
