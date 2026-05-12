package Bioracer.BachelorProject.Backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/apitest")
public class TestController {

    public TestController() {
    }

    @GetMapping
    public String test() {
        return "This is working!";
    }

}