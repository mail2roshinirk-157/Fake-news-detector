package com.example.fakenewsdetector.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping({"/", "/home", "/dashboard", "/history", "/about", "/analyze"})
    public String index() {
        return "index";
    }
}
