package me.drepic.proton.exception;

public class MessageSendException extends RuntimeException {

    public MessageSendException(Exception e){
        super(e);
    }
}
