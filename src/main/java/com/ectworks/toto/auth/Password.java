package com.ectworks.toto.auth;;

import org.apache.shiro.crypto.hash.Sha256Hash;

public class Password
{
    public static String hash(String password)
    {
        return new Sha256Hash(password).toHex();
    }
}