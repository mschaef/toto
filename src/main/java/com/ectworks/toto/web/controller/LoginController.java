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

import com.ectworks.toto.domain.User;;
import com.ectworks.toto.dao.UserDao;

@Controller
public class LoginController
{
    static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired(required = true)
    UserDao todoDao;

    @RequestMapping(value = "/login",
                    method = RequestMethod.GET)
    public String showTodoList(Model model)
    {
        log.debug("Logging in");

        return "login";
    }

}