import java.io.IOException;
import java.net.ServerSocket;

public class Main {

	public static final int PORT = 6379;

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-redis");

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);

			final var client = serverSocket.accept();

			final var outputStream = client.getOutputStream();
			outputStream.write("+PONG\r\n".getBytes());
			outputStream.flush();

			client.close();
		}
	}

}