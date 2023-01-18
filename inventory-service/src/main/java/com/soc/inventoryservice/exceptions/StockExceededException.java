package com.soc.inventoryservice.exceptions;

public class StockExceededException extends Exception {
    public StockExceededException(){
        super();
    }

    public StockExceededException(String message){
        super(message);
    }

    public StockExceededException(String message, Exception innerException){
        super(message, innerException);
    }
}
