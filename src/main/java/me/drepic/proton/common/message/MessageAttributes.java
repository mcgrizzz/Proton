package me.drepic.proton.common.message;

import java.util.UUID;

public class MessageAttributes {

    final String namespace;
    final String subject;
    final String senderName;
    final UUID senderID;

    public MessageAttributes(String namespace, String subject, String senderName, UUID senderID) {
        this.namespace = namespace;
        this.subject = subject;
        this.senderName = senderName;
        this.senderID = senderID;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSubject() {
        return subject;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getSenderID() {
        return senderID;
    }
}
