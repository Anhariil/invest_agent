package ru.invest.agent.api.controller;

import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.UserService;

import java.util.List;

@RestController
@Slf4j
@Validated // для работы паттерна с выбросом ошибки
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUserById(@RequestHeader String id){
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user")
    public ResponseEntity<User> getUserByParams(@RequestParam(required = false) String id,
                                                @RequestParam(required = false)
                                                @Pattern(regexp = "\\+?\\d{10,15}", message = "Номер телефона должен соответствовать формату +7XXXXXXXXXX") String phone){
        User user;
        if (id != null) {
            user = userService.findById(id);
        } else if (phone != null) {
            user = userService.findByPhone(phone);
        } else {
            throw new IllegalArgumentException("Должен быть заполнен как минимум один параметр - id или телефон");
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@RequestBody User user){
        User response = userService.createUser(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user")
    public ResponseEntity<User> updateUser(@RequestBody User user){
        User response = userService.updateUser(user);
        return ResponseEntity.ok(response);
    }
}
