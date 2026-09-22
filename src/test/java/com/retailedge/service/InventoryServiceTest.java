package com.retailedge.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.retailedge.dto.CreateInventoryRequest;
import com.retailedge.repository.InventoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository repository;

    @InjectMocks
    private InventoryService service;

    @Test
    void createRejectsNegativeStockBeforePersistence() {
        CreateInventoryRequest request = new CreateInventoryRequest(
            UUID.randomUUID(), -1);

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stock cannot be negative");
        verify(repository, never()).save(any());
    }
}