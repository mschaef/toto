package com.ectworks.toto.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.ectworks.toto.domain.TodoItem;
import com.ectworks.toto.dao.TodoItemDao;

@Controller
@RequestMapping("/todo")
public class TodoListController
{
    Logger log = LoggerFactory.getLogger(TodoItemDao.class);

    @Autowired(required = true)
    TodoItemDao todoDao;

    @RequestMapping(method = RequestMethod.GET)
    public String todo(Model model)
    {
        log.debug("Fetching Todo List");

        model.addAttribute("todoItems",
                           todoDao.pendingItems());

        return "todo";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String todo(Model model,
                       @RequestParam(required = true) String desc)
    {
        log.debug("Posting Todo Item");

        todoDao.addItem(TodoItem.make(desc));

        return "redirect:/todo";
    }
}
