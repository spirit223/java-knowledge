package cc.sika.nonblocking;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class Client {
    public static void main(String[] args) {
        try (SocketChannel client = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8088))) {
            log.debug("remote address: {}", client.getRemoteAddress());
            log.debug("send first message");
            client.write(StandardCharsets.UTF_8.encode("message1"));
            log.debug("send second message");
            client.write(StandardCharsets.UTF_8.encode("message2"));
            log.debug("send third message");
            client.write(StandardCharsets.UTF_8.encode("message3"));
        } catch (IOException ioException) {
            log.error("client error", ioException);
        }
    }
}
