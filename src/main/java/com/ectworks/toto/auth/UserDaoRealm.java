package com.ectworks.toto.auth;

import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import com.ectworks.toto.dao.UserDao;
import com.ectworks.toto.domain.User;

public class UserDaoRealm extends AuthorizingRealm
{
    static Logger log = LoggerFactory.getLogger(UserDao.class);

    private UserDao userDao;

    public void setUserDao(UserDao userDao)
    {
        this.userDao = userDao;
    }

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
        throws AuthenticationException
    {
        log.debug("doGetAuthenticationInfo: {}", token);

        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        if (username == null)
            throw new AccountException("Null usernames are not allowed by this realm.");

        User user = userDao.getUserByName(username);

        if (user == null)
            throw new UnknownAccountException("Unknown user name: " + username);

        SimpleAuthenticationInfo info =
            new SimpleAuthenticationInfo(user.getName(),
                                         user.getPassword().toCharArray(),
                                         getName());

        return info;
    }

    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals)
    {
        log.debug("doGetAuthorizationInfo: {}", principals);

        Set<String> roleNames = new HashSet<String>();

        roleNames.add("access");

        return new SimpleAuthorizationInfo(roleNames);
    }
}