package com.ectworks.toto.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;

import org.apache.shiro.realm.Realm;

import com.ectworks.toto.domain.User;

import com.ectworks.toto.dao.UserDao;

import com.ectworks.toto.auth.Password;

@Controller
public class UserController
{
    static Logger log = LoggerFactory.getLogger(UserController.class);

    private UserDao userDao;

    public void setUserDao(UserDao userDao)
    {
        this.userDao = userDao;
    }

    @RequestMapping(value = "/add-user",
                    method = RequestMethod.GET)
    public String showAddUser()
    {
        log.debug("Displaying Add User Page");

        return "add-user";
    }

    @RequestMapping(value = "/add-user",
                    method = RequestMethod.POST)
    public String doAddUser(Model model,
                            @RequestParam(required = true) String username,
                            @RequestParam(required = true) String password1,
                            @RequestParam(required = true) String password2,
                            @RequestParam(required = true) String email)
        throws IOException
    {
        log.debug("Accepting User");

        username = username.trim();

        if (userDao.isValidUser(username)) {
            model.addAttribute("failed", "User already exists: " + username);
            return "add-user";
        }

        if (username.equals("")) {
            model.addAttribute("failed", "Missing username.");
            return "add-user";
        }

        if (!password1.equals(password2)) {
            model.addAttribute("failed", "Passwords don't match.");
            return "add-user";
        }

        User user = new User();

        user.setName(username);
        user.setPassword(Password.hash(password1));
        user.setEmail(email);

        userDao.addUser(user);

        return "redirect:login";
    }
}
