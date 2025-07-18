package uk.gov.justice.laa.springboot.microservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.springboot.microservice.entity.ItemEntity;
import uk.gov.justice.laa.springboot.microservice.model.Item;

@ExtendWith(MockitoExtension.class)
class ItemMapperTest {
  private static final Long ITEM_ID = 123L;
  private static final String ITEM_NAME = "Item One";
  private static final String ITEM_DESCRIPTION = "This is Item One.";

  @InjectMocks private ItemMapper itemMapper = new ItemMapperImpl();

  @Test
  void shouldMapToItemEntity() {
    Item item = Item.builder().id(ITEM_ID).name(ITEM_NAME).description(ITEM_DESCRIPTION).build();

    ItemEntity result = itemMapper.toItemEntity(item);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(ITEM_ID);
    assertThat(result.getName()).isEqualTo(ITEM_NAME);
  }

  @Test
  void shouldMapToItem() {
    ItemEntity itemEntity = new ItemEntity(ITEM_ID, ITEM_NAME, ITEM_DESCRIPTION);

    Item result = itemMapper.toItem(itemEntity);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(ITEM_ID);
    assertThat(result.getName()).isEqualTo(ITEM_NAME);
  }
}
