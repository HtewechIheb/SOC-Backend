package com.soc.inventoryservice.services;

import com.soc.inventoryservice.dto.UpdateItemDto;
import com.soc.inventoryservice.dto.IsInStockDto;
import com.soc.inventoryservice.exceptions.StockExceededException;
import com.soc.inventoryservice.models.Inventory;
import com.soc.inventoryservice.repositories.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<IsInStockDto> isInStock(List<String> code){
        return inventoryRepository.findByCodeIn(code)
                .stream()
                .map(inventory ->
                        IsInStockDto
                                .builder()
                                .code(inventory.getCode())
                                .isInStock(inventory.getQuantity() > 0)
                                .build()
                ).toList();
    }

    @Transactional
    public void addStock(List<UpdateItemDto> updateItemDtoList) {
        for(UpdateItemDto updateItemDto: updateItemDtoList){
            Inventory inventoryToUpdate = inventoryRepository.findByCode(updateItemDto.getCode()).orElseThrow(() -> new NoSuchElementException(updateItemDto.getCode()));

            inventoryToUpdate.setQuantity(inventoryToUpdate.getQuantity() + updateItemDto.getQuantity());

            inventoryRepository.save(inventoryToUpdate);
        }
    }

    @Transactional
    public void setStock(List<UpdateItemDto> updateItemDtoList) {
        for(UpdateItemDto updateItemDto: updateItemDtoList){
            Optional<Inventory> inventoryToSetOptional = inventoryRepository.findByCode(updateItemDto.getCode());
            Inventory inventory;

            if(inventoryToSetOptional.isPresent()){
                inventory = inventoryToSetOptional.get();
                inventory.setQuantity(updateItemDto.getQuantity());
            }
            else {
                inventory = Inventory.builder()
                        .code(updateItemDto.getCode())
                        .quantity(updateItemDto.getQuantity())
                        .build();
            }

            inventoryRepository.save(inventory);
        }
    }

    @Transactional(rollbackFor = { StockExceededException.class })
    public void reduceStock(List<UpdateItemDto> updateItemDtoList) throws StockExceededException {
        for(UpdateItemDto updateItemDto: updateItemDtoList){
            Inventory inventoryToUpdate = inventoryRepository.findByCode(updateItemDto.getCode()).orElseThrow(() -> new NoSuchElementException(updateItemDto.getCode()));

            if(updateItemDto.getQuantity() > inventoryToUpdate.getQuantity()){
                throw new StockExceededException(updateItemDto.getCode());
            }

            inventoryToUpdate.setQuantity(inventoryToUpdate.getQuantity() - updateItemDto.getQuantity());

            inventoryRepository.save(inventoryToUpdate);
        }
    }
}
