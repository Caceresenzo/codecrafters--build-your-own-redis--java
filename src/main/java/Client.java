import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Client implements Runnable {

	private static final AtomicInteger ID_INCREMENT = new AtomicInteger();

	private final Socket socket;
	private final int id = ID_INCREMENT.incrementAndGet();

	@Override
	public void run() {
		System.out.println("%d: connected".formatted(id));
		
		try (socket) {
			final var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			final var outputStream = socket.getOutputStream();

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

			socket.close();
		} catch (IOException exception) {
			System.err.println("%d returned an error: %s".formatted(id, exception.getMessage()));
			exception.printStackTrace();
		}

		System.out.println("%d: disconnected".formatted(id));
	}

}