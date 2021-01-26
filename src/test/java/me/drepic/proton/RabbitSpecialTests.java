package me.drepic.proton;

import me.drepic.proton.exception.RegisterMessageHandlerException;
import me.drepic.proton.message.MessageHandler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertThatThrownBy(() -> client1ProtonManager.registerMessageHandlers(proton, client1Handler))
                .isInstanceOf(RegisterMessageHandlerException.class)
                .hasMessage("MessageContext already has defined data type");

    }
}
