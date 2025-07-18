package uk.gov.justice.laa.springboot.microservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.springboot.microservice.entity.ItemEntity;
import uk.gov.justice.laa.springboot.microservice.exception.ItemNotFoundException;
import uk.gov.justice.laa.springboot.microservice.mapper.ItemMapper;
import uk.gov.justice.laa.springboot.microservice.model.Item;
import uk.gov.justice.laa.springboot.microservice.model.ItemRequestBody;
import uk.gov.justice.laa.springboot.microservice.repository.ItemRepository;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock
  private ItemRepository mockItemRepository;

  @Mock
  private ItemMapper mockItemMapper;

  @InjectMocks
  private ItemService itemService;

  @Test
  void shouldGetAllItems() {
    ItemEntity firstItemEntity = new ItemEntity(1L, "Item One", "This is Item One.");
    ItemEntity secondItemEntity = new ItemEntity(2L, "Item Two", "This is Item Two.");
    Item firstItem = Item.builder().id(1L).name("Item One").description("This is Item One.").build();
    Item secondItem = Item.builder().id(2L).name("Item Two").description("This is Item Two.").build();
    when(mockItemRepository.findAll()).thenReturn(List.of(firstItemEntity, secondItemEntity));
    when(mockItemMapper.toItem(firstItemEntity)).thenReturn(firstItem);
    when(mockItemMapper.toItem(secondItemEntity)).thenReturn(secondItem);

    List<Item> result = itemService.getAllItems();

    assertThat(result).hasSize(2).contains(firstItem, secondItem);
  }

  @Test
  void shouldGetItemById() {
    Long id = 1L;
    String name = "Item One";
    String description = "This is Item One.";
    ItemEntity itemEntity = new ItemEntity(id, name, description);
    Item item = Item.builder().id(id).name(name).description(description).build();
    when(mockItemRepository.findById(id)).thenReturn(Optional.of(itemEntity));
    when(mockItemMapper.toItem(itemEntity)).thenReturn(item);

    Item result = itemService.getItem(id);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getName()).isEqualTo(name);
  }

  @Test
  void shouldNotGetItemById_whenItemNotFoundThenThrowsException() {
    Long id = 5L;
    when(mockItemRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ItemNotFoundException.class, () -> itemService.getItem(id));

    verify(mockItemMapper, never()).toItem(any(ItemEntity.class));
  }

  @Test
  void shouldCreateItem() {
    ItemRequestBody itemRequestBody = ItemRequestBody.builder().name("Item Three").description("This is Item Three.").build();
    ItemEntity itemEntity = new ItemEntity(3L, "Item Three", "This is Item Three.");
    when(mockItemRepository.save(new ItemEntity(null, "Item Three", "This is Item Three.")))
        .thenReturn(itemEntity);

    Long result = itemService.createItem(itemRequestBody);

    assertThat(result).isNotNull().isEqualTo(3L);
  }

  @Test
  void shouldUpdateItem() {
    Long id = 1L;
    String name = "Item One";
    String description = "This is Item One.";
    ItemRequestBody itemRequestBody = ItemRequestBody.builder().name(name).description(description).build();
    ItemEntity itemEntity = new ItemEntity(id, name, description);
    when(mockItemRepository.findById(id)).thenReturn(Optional.of(itemEntity));

    itemService.updateItem(id, itemRequestBody);

    verify(mockItemRepository).save(itemEntity);
  }

  @Test
  void shouldNotUpdateItem_whenItemNotFoundThenThrowsException() {
    Long id = 5L;
    ItemRequestBody itemRequestBody = ItemRequestBody.builder().name("Item Five").description("This is Item Five.").build();
    when(mockItemRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ItemNotFoundException.class, () -> itemService.updateItem(id, itemRequestBody));

    verify(mockItemRepository, never()).save(any(ItemEntity.class));
  }

  @Test
  void shouldDeleteItem() {
    Long id = 1L;
    ItemEntity itemEntity = new ItemEntity(id, "Item One", "This is Item One.");
    when(mockItemRepository.findById(id)).thenReturn(Optional.of(itemEntity));

    itemService.deleteItem(id);

    verify(mockItemRepository).deleteById(id);
  }

  @Test
  void shouldNotDeleteItem_whenItemNotFoundThenThrowsException() {
    Long id = 5L;
    when(mockItemRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ItemNotFoundException.class, () -> itemService.deleteItem(id));

    verify(mockItemRepository, never()).deleteById(id);
  }
}
