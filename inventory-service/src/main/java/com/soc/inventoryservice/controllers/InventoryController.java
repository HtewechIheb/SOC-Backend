package com.soc.inventoryservice.controllers;

import com.soc.inventoryservice.dto.UpdateItemDto;
import com.soc.inventoryservice.dto.IsInStockDto;
import com.soc.inventoryservice.exceptions.StockExceededException;
import com.soc.inventoryservice.services.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<IsInStockDto> isInStock(@RequestParam List<String> code){
        return inventoryService.isInStock(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void updateStock(@RequestBody List<UpdateItemDto> updateItemDtoList, @RequestParam String operation){
        switch (operation){
            case "add":
                addStock(updateItemDtoList);
                break;
            case "set":
                setStock(updateItemDtoList);
                break;
            case "reduce":
                reduceStock(updateItemDtoList);
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid operation.");
        }
    }

    private void addStock(List<UpdateItemDto> updateItemDtoList){
        try{
            inventoryService.addStock(updateItemDtoList);
        }
        catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with code %s does not exist.".formatted(ex.getMessage()));
        }
    }

    private void setStock(List<UpdateItemDto> updateItemDtoList){
        inventoryService.setStock(updateItemDtoList);
    }

    private void reduceStock(List<UpdateItemDto> updateItemDtoList){
        try {
            inventoryService.reduceStock(updateItemDtoList);
        }
        catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with code %s does not exist.".formatted(ex.getMessage()));
        }
        catch (StockExceededException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity of product %s exceeded the existing stock.".formatted(ex.getMessage()));
        }
    }
}
