import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class Main {

	public static final int PORT = 6379;

	public static void main(String[] args) throws IOException {
		System.out.println("codecrafters build-your-own-redis");

		try (final var serverSocket = new ServerSocket(PORT)) {
			serverSocket.setReuseAddress(true);

			final var client = serverSocket.accept();

			final var reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
			final var outputStream = client.getOutputStream();

			String line;
			while ((line = reader.readLine()) != null) {
				//				System.out.println(line);

				if ("DOCS".equalsIgnoreCase(line)) {
					outputStream.write("*0\r\n".getBytes());
				} else if ("PING".equalsIgnoreCase(line)) {
					outputStream.write("+PONG\r\n".getBytes());
				}

				outputStream.flush();
			}

			client.close();
		}
	}

}