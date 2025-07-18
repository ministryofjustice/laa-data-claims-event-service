package uk.gov.justice.laa.springboot.microservice.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.springboot.microservice.SpringBootMicroserviceApplication;

@SpringBootTest(classes = SpringBootMicroserviceApplication.class)
@AutoConfigureMockMvc
@Transactional
public class ItemControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGetAllItems() throws Exception {
    mockMvc
        .perform(get("/api/v1/items"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(5)));
  }

  @Test
  void shouldGetItem() throws Exception {
    mockMvc.perform(get("/api/v1/items/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Item One"))
        .andExpect(jsonPath("$.description").value("This is a description of Item One."));
  }

  @Test
  void shouldCreateItem() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Item Six\", \"description\": \"This is a description of Item Six.\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldUpdateItem() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/items/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 2, \"name\": \"Item Two\", \"description\": \"This is a updated description of Item Three.\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldDeleteItem() throws Exception {
    mockMvc.perform(delete("/api/v1/items/3")).andExpect(status().isNoContent());
  }
}
