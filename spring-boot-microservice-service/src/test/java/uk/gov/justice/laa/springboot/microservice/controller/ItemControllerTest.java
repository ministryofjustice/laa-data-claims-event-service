package uk.gov.justice.laa.springboot.microservice.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.springboot.microservice.model.Item;
import uk.gov.justice.laa.springboot.microservice.model.ItemRequestBody;
import uk.gov.justice.laa.springboot.microservice.service.ItemService;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ItemService mockItemService;

  @Test
  void getItems_returnsOkStatusAndAllItems() throws Exception {
    List<Item> items = List.of(Item.builder().id(1L).name("Item One").description("This is a test item one.").build(),
        Item.builder().id(2L).name("Item Two").description("This is a test item two.").build());
    when(mockItemService.getAllItems()).thenReturn(items);

    mockMvc.perform(get("/api/v1/items"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(2)));
  }

  @Test
  void getItemById_returnsOkStatusAndOneItem() throws Exception {
    when(mockItemService.getItem(1L)).thenReturn(Item.builder().id(1L).name("Item One").description("This is a test item one.").build());

    mockMvc.perform(get("/api/v1/items/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Item One"))
        .andExpect(jsonPath("$.description").value("This is a test item one."));
  }

  @Test
  void createItem_returnsCreatedStatusAndLocationHeader() throws Exception {
    ItemRequestBody itemRequestBody = ItemRequestBody.builder().name("Item Three").description("This is an updated item three.").build();
    when(mockItemService.createItem(itemRequestBody))
        .thenReturn(3L);

    mockMvc
        .perform(post("/api/v1/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"Item Three\", \"description\": \"This is an updated item three.\"}")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/v1/items/3")));
  }

  @Test
  void createItem_returnsBadRequestStatus() throws Exception {
    mockMvc
        .perform(post("/api/v1/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"Item Three\"}")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("{\"type\":\"about:blank\",\"title\":\"Bad Request\"," +
            "\"status\":400,\"detail\":\"Invalid request content.\",\"instance\":\"/api/v1/items\"}"));

    verify(mockItemService, never()).createItem(any(ItemRequestBody.class));
  }

  @Test
  void updateItem_returnsNoContentStatus() throws Exception {
    mockMvc
        .perform(put("/api/v1/items/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"Item Two\", \"description\": \"This is an updated item two.\"}")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(mockItemService).updateItem(eq(2L), any(ItemRequestBody.class));
  }

  @Test
  void updateItem_returnsBadRequestStatus() throws Exception {
    mockMvc
        .perform(put("/api/v1/items/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"description\": \"This is an updated item two.\"}")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("{\"type\":\"about:blank\",\"title\":\"Bad Request\"," +
            "\"status\":400,\"detail\":\"Invalid request content.\",\"instance\":\"/api/v1/items/2\"}"));

    verify(mockItemService, never()).updateItem(eq(2L), any(ItemRequestBody.class));
  }

  @Test
  void deleteItem_returnsNoContentStatus() throws Exception {
    mockMvc.perform(delete("/api/v1/items/3")).andExpect(status().isNoContent());

    verify(mockItemService).deleteItem(3L);
  }
}
