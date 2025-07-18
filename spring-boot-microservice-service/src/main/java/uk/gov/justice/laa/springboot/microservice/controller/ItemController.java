package uk.gov.justice.laa.springboot.microservice.controller;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.justice.laa.springboot.microservice.api.ItemsApi;
import uk.gov.justice.laa.springboot.microservice.model.Item;
import uk.gov.justice.laa.springboot.microservice.model.ItemRequestBody;
import uk.gov.justice.laa.springboot.microservice.service.ItemService;

/**
 * Controller for handling items requests.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ItemController implements ItemsApi {
  private final ItemService service;

  @Override
  public ResponseEntity<List<Item>> getItems() {
    log.info("Getting all items");

    return ResponseEntity.ok(service.getAllItems());
  }

  @Override
  public ResponseEntity<Item> getItemById(Long id) {
    log.info("Getting item {}", id);

    return ResponseEntity.ok(service.getItem(id));
  }

  @Override
  public ResponseEntity<Void> createItem(@RequestBody ItemRequestBody itemRequestBody) {
    log.info("Creating item {}", itemRequestBody);

    Long id = service.createItem(itemRequestBody);
    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(id).toUri();
    return ResponseEntity.created(uri).build();
  }

  @Override
  public ResponseEntity<Void> updateItem(Long id, ItemRequestBody itemRequestBody) {
    log.info("Updating item {}", id);

    service.updateItem(id, itemRequestBody);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteItem(Long id) {
    log.info("Deleting item {}", id);

    service.deleteItem(id);
    return ResponseEntity.noContent().build();
  }
}
