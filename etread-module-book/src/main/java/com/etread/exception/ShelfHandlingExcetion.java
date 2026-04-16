package com.etread.exception;

public class ShelfHandlingExcetion extends RuntimeException{
    private Integer code;

    public ShelfHandlingExcetion(String message) {
        super(message);
        this.code = 400;
    }

    public ShelfHandlingExcetion(Integer code, String message) {
        super(message);
        this.code = code;
    }

    // getter
    public Integer getCode() { return code; }
}
