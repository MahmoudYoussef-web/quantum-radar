package com.quradar.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RuleAdminController.class)
@AutoConfigureMockMvc(addFilters = false) // slice test: auth filters land in P6
class RuleAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RuleConfigService service;

    private static RuleConfig config(String code, boolean enabled, int fee) throws Exception {
        RuleConfig config = new RuleConfig();
        set(config, "code", code);
        set(config, "displayName", code);
        set(config, "enabled", enabled);
        set(config, "fee", fee);
        set(config, "penaltyPoints", 0);
        return config;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void listReturnsAllConfigs() throws Exception {
        when(service.listAll()).thenReturn(List.of(config("SEATBELT", true, 100)));
        mvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SEATBELT"))
                .andExpect(jsonPath("$[0].fee").value(100));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        when(service.getRequired("NOPE")).thenThrow(new RuleNotFoundException("NOPE"));
        mvc.perform(get("/api/v1/rules/NOPE")).andExpect(status().isNotFound());
    }

    @Test
    void patchUpdatesAndReturnsConfig() throws Exception {
        when(service.update(eq("RED_LIGHT"), any(RuleConfigUpdateRequest.class)))
                .thenReturn(config("RED_LIGHT", false, 500));
        mvc.perform(patch("/api/v1/rules/RED_LIGHT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void patchRejectsNegativeFee() throws Exception {
        mvc.perform(patch("/api/v1/rules/SEATBELT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fee\":-5}"))
                .andExpect(status().isBadRequest());
    }
}
