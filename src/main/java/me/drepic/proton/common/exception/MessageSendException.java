package me.drepic.proton.common.exception;

public class MessageSendException extends RuntimeException {

    public MessageSendException(Exception e){
        super(e);
    }
}
