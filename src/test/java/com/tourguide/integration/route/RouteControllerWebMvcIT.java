package com.tourguide.integration.route;

import com.tourguide.common.exception.GlobalExceptionHandler;
import com.tourguide.route.RouteController;
import com.tourguide.route.RouteService;
import com.tourguide.route.dto.RouteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RouteControllerWebMvcIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteService routeService;

    @Test
    void getAllRoutes_returnsOkWithPayload() throws Exception {
        RouteResponse response = RouteResponse.builder().name("Kordon Walk").build();
        when(routeService.findAll(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kordon Walk"));
    }
}

