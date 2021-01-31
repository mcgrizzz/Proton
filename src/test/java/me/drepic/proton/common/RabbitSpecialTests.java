package me.drepic.proton.common;

import me.drepic.proton.common.exception.RegisterMessageHandlerException;
import me.drepic.proton.common.message.MessageHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RabbitSpecialTests extends RabbitTests {

    @Test
    public void testMultipleHandlers__mismatchDataType() {
        Object client1Handler = new Object() {
            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv(int recvInt) {
            }

            @MessageHandler(namespace = NAMESPACE, subject = SUBJECT, async = true)
            public void recv2(char recvChar) {
            }
        };

        assertThatThrownBy(() -> client1ProtonManager.registerMessageHandlers(client1Handler))
                .isInstanceOf(RegisterMessageHandlerException.class)
                .hasMessage("MessageContext already has defined data type");

    }
}
