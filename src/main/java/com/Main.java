package com;

import com.github.instagram4j.instagram4j.IGClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        Properties properties = null;
        try (InputStream input = new FileInputStream(args[0])) {
            properties = new Properties();
            properties.load(input);
        }



        IGClient client = IGClient.builder()
                .username(System.getProperty("username"))
                .password(System.getProperty("password"))
                .login();


        TargetAccountProvider targetAccountProvider = new TargetAccountProvider();


        String account = targetAccountProvider.getPotentialAccount();



        //ApplicationContext context = new ClassPathXmlApplicationContext(args[0]);

    }
}
