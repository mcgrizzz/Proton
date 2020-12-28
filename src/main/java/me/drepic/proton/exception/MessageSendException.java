package me.drepic.proton.message;

public class MessageSendException extends RuntimeException {

    public MessageSendException(Exception e){
        super(e);
    }
}
