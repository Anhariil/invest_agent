package ru.invest.agent.api.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.invest.agent.api.BaseTest;
import ru.invest.agent.api.model.User;
import ru.invest.agent.api.service.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class ApiControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private UserService userService;

    private User testUser = new User();

    @Test
    void getUserById_should_return_user() throws Exception {
        testUser.setPhone("+79110003344");
        testUser.setId("3412");

        Mockito.when(userService.findById(anyString())).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/{id}", testUser.getId())
                        .header("id", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.phone").value(testUser.getPhone()));
    }

    @Test
    void getUserByParams_with_phone_should_return_user() throws Exception {
        testUser.setPhone("+79110003344");
        testUser.setId("3412");

        Mockito.when(userService.findByPhone(anyString())).thenReturn(testUser);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user")
                        .param("phone", testUser.getPhone()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(testUser.getPhone()))
                .andExpect(jsonPath("$.id").value(testUser.getId()));
    }

    @Test
    void getUserByParams_with_invalidPhone_should_return_badRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user")
                        .param("phone", "invalid-phone"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Номер телефона должен соответствовать формату")));
    }

    @Test
    void getUserByParams_without_params_should_return_badRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/user"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Должен быть заполнен как минимум один параметр")));
    }
}
