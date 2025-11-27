package com.bazunia.vps.controller;

import com.bazunia.vps.dto.GatewayDto;
import com.bazunia.vps.model.User;
import com.bazunia.vps.repository.GatewayRepository;
import com.bazunia.vps.repository.UserGatewayPermissionRepository;
import com.bazunia.vps.repository.UserRepository;
import com.bazunia.vps.service.GatewayService;
import com.bazunia.vps.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GatewayController.class)
@AutoConfigureMockMvc(addFilters = false)
class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GatewayService gatewayService;
    @MockitoBean private GatewayRepository gatewayRepository;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserGatewayPermissionRepository permissionRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void getGateways_ShouldReturnList_WhenUserIsAuthenticated() throws Exception {
        // ARRANGE
        User customUser = new User();
        customUser.setId(10L);
        customUser.setEmail("test@bazunia.pl");

        Authentication authMock = mock(Authentication.class);
        when(authMock.getPrincipal()).thenReturn(customUser);

        GatewayDto dto = new GatewayDto(
                1L, "Salon", "ONLINE", "Dom", "Główna bramka",
                "2023-01-01T12:00:00", 10L, Collections.emptyList()
        );

        when(gatewayService.getGatewaysForUser(customUser)).thenReturn(List.of(dto));

        // ACT & ASSERT
        mockMvc.perform(get("/api/gateways")
                        .principal(authMock) // <--- KLUCZOWE: Wstrzykujemy mocka autentykacji
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Salon"))
                .andExpect(jsonPath("$[0].status").value("ONLINE"))
                .andExpect(jsonPath("$[0].folder").value("Dom"));
    }
}