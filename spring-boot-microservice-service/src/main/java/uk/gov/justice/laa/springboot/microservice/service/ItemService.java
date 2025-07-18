package uk.gov.justice.laa.springboot.microservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.springboot.microservice.entity.ItemEntity;
import uk.gov.justice.laa.springboot.microservice.exception.ItemNotFoundException;
import uk.gov.justice.laa.springboot.microservice.mapper.ItemMapper;
import uk.gov.justice.laa.springboot.microservice.model.Item;
import uk.gov.justice.laa.springboot.microservice.model.ItemRequestBody;
import uk.gov.justice.laa.springboot.microservice.repository.ItemRepository;

/**
 * Service class for handling items requests.
 */
@RequiredArgsConstructor
@Service
public class ItemService {

  private final ItemRepository itemRepository;
  private final ItemMapper itemMapper;

  /**
   * Gets all items.
   *
   * @return the list of items
   */
  public List<Item> getAllItems() {
    return itemRepository.findAll().stream().map(itemMapper::toItem).toList();
  }

  /**
   * Gets an item for a given id.
   *
   * @param id the item id
   * @return the requested item
   */
  public Item getItem(Long id) {
    ItemEntity itemEntity = checkIfItemExist(id);
    return itemMapper.toItem(itemEntity);
  }

  /**
   * Creates an item.
   *
   * @param itemRequestBody the item to be created
   * @return the id of the created item
   */
  public Long createItem(ItemRequestBody itemRequestBody) {
    ItemEntity itemEntity = new ItemEntity();
    itemEntity.setName(itemRequestBody.getName());
    itemEntity.setDescription(itemRequestBody.getDescription());
    ItemEntity createdItemEntity = itemRepository.save(itemEntity);
    return createdItemEntity.getId();
  }

  /**
   * Updates an item.
   *
   * @param id the id of the item to be updated
   * @param itemRequestBody the updated item
   */
  public void updateItem(Long id, ItemRequestBody itemRequestBody) {
    ItemEntity itemEntity = checkIfItemExist(id);
    itemEntity.setName(itemRequestBody.getName());
    itemEntity.setDescription(itemRequestBody.getDescription());
    itemRepository.save(itemEntity);
  }

  /**
   * Deletes an item.
   *
   * @param id the id of the item to be deleted
   */
  public void deleteItem(Long id) {
    checkIfItemExist(id);

    itemRepository.deleteById(id);
  }

  private ItemEntity checkIfItemExist(Long id) {
    return itemRepository
        .findById(id)
        .orElseThrow(
            () -> new ItemNotFoundException(String.format("No item found with id: %s", id)));
  }
}
