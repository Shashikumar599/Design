package redis;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Redis client over a raw socket speaking RESP (no jar needed).
 * Not thread safe: one command at a time per instance, so use one client per thread.
 */
public class RedisClient implements Closeable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    public RedisClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = socket.getOutputStream();
    }

    public RedisClient() throws IOException {
        this("127.0.0.1", 6379);
    }

    public void put(String key, String value) throws IOException {
        command("SET", key, value);
    }

    /** @return the value, or null if the key does not exist */
    public String get(String key) throws IOException {
        return (String) command("GET", key);
    }

    /**
     * Sends any command, e.g. command("DECR", "stock").
     * Returns String (simple/bulk), Long (integer) or null (nil). Error replies throw.
     */
    public Object command(String... args) throws IOException {
        writeCommand(args);
        return readReply();
    }

    // RESP request: *<argc>\r\n then for each arg $<byteLen>\r\n<bytes>\r\n
    private void writeCommand(String[] args) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            buf.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            buf.write(bytes);
            buf.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(buf.toByteArray());
        out.flush();
    }

    // RESP reply: first byte tells the type: + simple, - error, : integer, $ bulk string
    private Object readReply() throws IOException {
        int type = in.read();
        if (type == -1) throw new IOException("Connection closed by server");
        String line = readLine();
        switch (type) {
            case '+': return line;
            case '-': throw new IOException("Redis error: " + line);
            case ':': return Long.parseLong(line);
            case '$': {
                int len = Integer.parseInt(line);
                if (len == -1) return null;
                byte[] data = in.readNBytes(len);
                in.readNBytes(2); // trailing \r\n
                return new String(data, StandardCharsets.UTF_8);
            }
            default: throw new IOException("Unsupported reply type: " + (char) type);
        }
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != '\r') {
            if (c == -1) throw new IOException("Connection closed by server");
            sb.append((char) c);
        }
        in.read(); // \n
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
