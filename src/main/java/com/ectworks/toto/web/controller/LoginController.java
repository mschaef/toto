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

import com.ectworks.toto.auth.UserDaoRealm;

@Controller
public class LoginController
{
    static Logger log = LoggerFactory.getLogger(LoginController.class);

    private Realm realm;

    public void setRealm(Realm realm)
    {
        this.realm = realm;
    }

    @RequestMapping(value = "/login",
                    method = RequestMethod.GET)
    public String showLogin(Model model)
    {
        log.debug("Displaying Login Page");

        return "login";
    }

    @RequestMapping(value = "/login",
                    method = RequestMethod.POST)
    public String showTodoList(Model model,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               @RequestParam(required = true) String username,
                               @RequestParam(required = true) String password)
        throws IOException
    {
        log.debug("Accepting Credentials");

        UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        boolean authenticationSucceeded = false;

        try {
            SecurityUtils.getSubject().login(token);

            authenticationSucceeded = true;
        } catch (AuthenticationException ex) {
            log.error("Error authenticating user: " + username, ex);
        }

        log.debug("authenticationSucceeded: {}", authenticationSucceeded);

        if (authenticationSucceeded) {
            WebUtils.redirectToSavedRequest(request,response,"todo");
            return null;
        } else
            model.addAttribute("failed", "true");

        return "login";
    }

    @RequestMapping(value="/logout", method=RequestMethod.GET)
    public String logout(Model model,
                         HttpSession session)
    {
        Subject subject = SecurityUtils.getSubject();

        log.info("logging out user: {}", subject.getPrincipal());
        subject.logout();

        return "redirect:login";
    }
}
