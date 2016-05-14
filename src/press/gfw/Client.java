/**
* 
*    GFW.Press
*    Copyright (C) 2016  chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*    
**/
package press.gfw;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;

import javax.crypto.SecretKey;
import org.json.simple.JSONObject;

/**
 * 
 * GFW.Press客户端
 * 
 * @author chinashiyu ( chinashiyu@gfw.press ; http://gfw.press )
 *
 */
public class Client extends Thread {

	private Config config = null;

	private int listenPort = 0;

	private String serverHost = null;

	private int serverPort = 0;

	private String password = null;

	private SecretKey key = null;

	private Encrypt aes = null;

	private boolean kill = false;

	private String status = null;

	private ServerSocket listenSocket = null;

	public static void main(String[] args) throws IOException {

		Client client = new Client();

		client.start();

	}

	public Client() {

		super();

		config = new Config();

		JSONObject json = config.getClientConfig();

		this.serverHost = json.get("ServerHost") == null ? null : (String) json.get("ServerHost");

		this.serverPort = json.get("ServerPort") == null ? 0 : Integer.parseInt( (String) json.get("ServerPort") );

		this.password = json.get("Password") == null ? null : (String) json.get("Password");

		this.listenPort = json.get("ProxyPort") == null ? 0 : Integer.parseInt( (String) json.get("ProxyPort") );

		aes = new Encrypt();

		if (aes.isPassword(this.password)) {

			key = aes.getPasswordKey(this.password);

		}

	}


	private void _sleep(long m) {

		try {

			sleep(m);

		} catch (InterruptedException ie) {

		}

	}

	/**
	 * @return the listenPort
	 */
	public synchronized int getListenPort() {

		return listenPort;
	}

	/**
	 * @return the password
	 */
	public synchronized String getPassword() {

		return password;
	}

	/**
	 * @return the serverHost
	 */
	public synchronized String getServerHost() {

		return serverHost;
	}

	/**
	 * @return the serverPort
	 */
	public synchronized int getServerPort() {

		return serverPort;
	}

	/**
	 * @return the status
	 */
	public synchronized String getStatus() {

		return status;
	}

	/**
	 * @return the kill
	 */
	public synchronized boolean isKill() {

		return kill;
	}

	public synchronized void kill() {

		kill = true;

		if (listenSocket != null && !listenSocket.isClosed()) {

			try {

				listenSocket.close();

			} catch (IOException ex) {

			}

			listenSocket = null;

		}

	}

	/**
	 * 打印信息
	 * 
	 * @param o
	 */
	@SuppressWarnings("unused")
	private void log(Object o) {

		String time = (new Timestamp(System.currentTimeMillis())).toString().substring(0, 19);

		System.out.println("[" + time + "] " + o.toString());

	}

	/**
	 * 启动客户端
	 * 
	 * @return
	 */
	public void run() {

		if (serverHost == null || (serverHost = serverHost.trim()).length() == 0 || serverPort == 0 || listenPort == 0 || key == null) {

			status = "启动失败：配置错误";

			kill = true;

			return;

		}

		File lockFile = new File("client.lock");

		try {

			listenSocket = new ServerSocket(listenPort);

		} catch (IOException ex) {

			if (lockFile.exists()) {

				System.exit(0);

			}

			kill = true;

			status = "线程" + getName() + "启动时监听" + listenPort + "端口出错，线程结束。";

			return;

		}

		lockFile.deleteOnExit();

		if (!lockFile.exists()) {

			try {

				lockFile.createNewFile();

			} catch (IOException ioe) {

			}

		}

		while (!kill) {

			Socket agentSocket = null;

			try {

				agentSocket = listenSocket.accept();

			} catch (IOException ex) {

				if (listenSocket != null && !listenSocket.isClosed()) {

					status = "线程" + getName() + "运行时监听" + listenPort + "端口出错，休息3秒钟后重试。";

					_sleep(3000L);

					continue;

				} else {

					status = "线程" + getName() + "运行时监听" + listenPort + "端口出错，线程结束。";

					break;

				}

			}

			ClientThread clientThread = new ClientThread(agentSocket, serverHost, serverPort, key);

			clientThread.start();

		}

		if (listenSocket != null && !listenSocket.isClosed()) {

			try {

				listenSocket.close();

			} catch (IOException ex) {

			}

			listenSocket = null;

		}

		kill = true;

	}

}
