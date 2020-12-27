package me.drepic.proton.message;

import java.util.Objects;

public class MessageContext {

    final String namespace;
    final String subject;

    public MessageContext(String namespace, String subject){
        this.namespace = namespace;
        this.subject = subject;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageContext that = (MessageContext) o;
        return namespace.equals(that.namespace) && subject.equals(that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, subject);
    }

    public String toContextString(){
        return this.namespace + "." + this.subject;
    }

    public static MessageContext fromString(String s){
        String[] l = s.split("\\.");
        if(l.length != 2){
            throw new IllegalArgumentException();
        }
        return new MessageContext(l[0], l[1]);
    }
}
