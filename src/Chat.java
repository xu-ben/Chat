import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 局域网聊天软件
 * 
 * @author ben
 * 
 */
public class Chat extends JFrame implements Runnable, ActionListener,
		ItemListener {
	private static final long serialVersionUID = 1L;

	/**
	 * 聊天内容文本域
	 */
	JTextArea areaContent = new JTextArea();

	JTextField fieldSelfIP = new JTextField("");

	JTextField fieldOtherIP = new JTextField("");

	JTextField fieldSelfName = new JTextField("");

	JTextField fieldOtherName = new JTextField("");

	JPasswordField fieldSelfPassword = new JPasswordField("");

	JPasswordField fieldOtherPassword = new JPasswordField("");

	JTextField fieldSentence = new JTextField();

	JButton buttonSend = new JButton("发送");

	JButton buttonExport = new JButton("导出聊天记录...");

	JButton buttonSendFile = new JButton("发送文件");

	Thread rec2010;

	Thread rec2011;

	Thread rec2012;

	Thread sendTread;

	JCheckBox checkEncry = new JCheckBox("收发加密");

	private static final String M_seg = "\r\t\n";

	private String tfilePath = null;

	private String tfileName = null;

	/**
	 * format对象是用来以指定的时间格式
	 */
	SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 当前收发是否需要解密加密
	 */
	boolean isEncry = false;

	public Chat() {
		try {
			InetAddress address;
			address = InetAddress.getLocalHost();
			fieldSelfIP.setText(address.getHostAddress());
		} catch (Exception e) {
			return;
		}

		JPanel south = new JPanel();

		south.setLayout(new BorderLayout(5, 15));

		JPanel southOfSouth = new JPanel();

		JPanel centerOfSouth = new JPanel();

		centerOfSouth.setLayout(new GridLayout(3, 2, 15, 15));

		southOfSouth.setLayout(new GridLayout(1, 1));

		JPanel[] p = new JPanel[7];
		for (int i = 0; i < 7; i++) {
			p[i] = new JPanel();
			p[i].setLayout(new BorderLayout());
		}
		p[0].add(BorderLayout.WEST, new JLabel("本机IP: "));
		p[0].add(BorderLayout.CENTER, fieldSelfIP);
		p[1].add(BorderLayout.WEST, new JLabel("对方IP: "));
		p[1].add(BorderLayout.CENTER, fieldOtherIP);
		p[2].add(BorderLayout.WEST, new JLabel("本人姓名: "));
		p[2].add(BorderLayout.CENTER, fieldSelfName);
		p[3].add(BorderLayout.WEST, new JLabel("对方姓名:"));
		p[3].add(BorderLayout.CENTER, fieldOtherName);
		// p[4].add(BorderLayout.WEST, new JLabel("本人密码: "));
		// p[4].add(BorderLayout.CENTER, fieldSelfPassword);
		p[4].add(checkEncry);
		p[5].add(buttonSendFile);
		// p[5].add(BorderLayout.WEST, new JLabel("对方密码: "));
		// p[5].add(BorderLayout.CENTER, fieldOtherPassword);
		p[6].add(BorderLayout.CENTER, fieldSentence);
		p[6].add(BorderLayout.EAST, buttonSend);
		p[6].add(BorderLayout.WEST, buttonExport);
		for (int i = 0; i < 6; i++) {
			centerOfSouth.add(p[i]);
		}

		southOfSouth.add(p[6]);

		south.add(centerOfSouth, BorderLayout.CENTER);

		south.add(southOfSouth, BorderLayout.SOUTH);

		Container con = this.getContentPane();

		con.add(south, BorderLayout.SOUTH);

		con.add(new JScrollPane(areaContent));

		areaContent.setEditable(false); // 设置显示聊天信息窗口只读

		fieldSelfIP.setEditable(false); // 设置本方IP只读

		buttonSend.addActionListener(this);

		buttonExport.addActionListener(this);

		buttonSendFile.addActionListener(this);

		checkEncry.addItemListener(this);
		checkEncry.setSelected(false);

		buttonSend.registerKeyboardAction(this,
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true),
				JComponent.WHEN_IN_FOCUSED_WINDOW); // 响应Enter键
		this.setTitle("局域网聊天——UDP端口2010、2012、2013,TCP端口2011");

		/**
		 * 使程序运行时在屏幕居中显示
		 */
		final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		final int width = 550;
		final int height = 650;
		final int left = (screen.width - width) / 2;
		final int top = (screen.height - height) / 2;
		this.setLocation(left, top);
		this.setSize(width, height);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		rec2010 = new Thread(this);
		rec2010.setName("2010");
		rec2011 = new Thread(this);
		rec2011.setName("2011");
		rec2012 = new Thread(this);
		rec2012.setName("2012");
		sendTread = new Thread(this);
		sendTread.setName("send");
		setVisible(true);
	}

	/**
	 * 连接服务器端
	 * 
	 * @param port
	 *            要连接的服务器SocketServer端口
	 */
	public Socket connectSocketServer(String IP, int port) {
		try {
			Socket s = new Socket(InetAddress.getByName(IP), port);
			return s;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 向对方发送传送文件请求
	 * 
	 * @param aimIP
	 * @param filePath
	 */
	public void request() {
		DatagramSocket socketSend;
		try {
			socketSend = new DatagramSocket();
			byte[] buf = this.tfileName.getBytes();
			InetAddress otherAddress = InetAddress.getByName(this.fieldOtherIP
					.getText());
			DatagramPacket packet = new DatagramPacket(buf, buf.length,
					otherAddress, 2012);
			socketSend.send(packet);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean tryToSend() {
		while (true) {
			try {
				DatagramSocket socketRecieve = new DatagramSocket(2013);
				while (true) {
					byte[] buf = new byte[1024];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socketRecieve.receive(packet);
					InetAddress address = packet.getAddress();
					String srcIP = address.getHostAddress();
					if (!srcIP.equals(this.fieldOtherIP.getText())) {
						continue;
					}
					int length = packet.getLength();
					String message = new String(buf, 0, length);

					String[] word = message.split(M_seg);
					if (word == null || word.length != 2) {
						continue;
					}
					if (!word[1].trim().equals(this.tfileName)) {
						continue;
					}
					if (word[0].equals("accept")) {
						if (sendFile()) {
							JOptionPane.showMessageDialog(this, "文件发送成功!");
							return true;
						} else {
							JOptionPane.showMessageDialog(this, "发送过程出错，文件发送失败!");
							return false;
						}
					} else {
						 JOptionPane.showMessageDialog(this, "对方拒绝接收，文件发送失败!");
						return false;
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	// 发送文件
	public boolean sendFile() {
		Socket s = connectSocketServer(this.fieldOtherIP.getText(), 2011);
		byte[] b = new byte[1024];
		File f = new File(this.tfilePath);
		try {
			// 数据输出流
			DataOutputStream dout = new DataOutputStream(
					new BufferedOutputStream(s.getOutputStream()));
			// 文件读入流
			FileInputStream fr = new FileInputStream(f);
			int n = fr.read(b);
			while (n != -1) {
				// 向网络中写入数据
				dout.write(b, 0, n);
				dout.flush();
				// 再次读取n字节
				n = fr.read(b);
			}

			// 关闭流
			fr.close();
			dout.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void listen2010() {
		try {
			DatagramSocket socketRecieve = new DatagramSocket(2010);
			while (true) {
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socketRecieve.receive(packet);
				InetAddress address;
				address = packet.getAddress();
				String currentOtherIP = address.getHostAddress();
				int length = packet.getLength();

				/**
				 * 解密
				 */
				if (isEncry) {
					buf = Chat.setAndGetEncryption(buf);
				}

				String message = new String(buf, 0, length);
				String strOtherIP = fieldOtherIP.getText().trim();
				if (currentOtherIP.trim().equals("")) {
					currentOtherIP = "???";
				}
				if (!strOtherIP.equals("") && !currentOtherIP.equals(strOtherIP)) {
					continue;
				}
				

				String strOtherName = fieldOtherName.getText().trim();
				if (strOtherName.length() == 0) {
					strOtherName = new String("???");
				}
				String strSentence = new String(message);

				this.printlnRecMessage(currentOtherIP, strOtherName, strSentence);
			}
		} catch (SocketException e) {
		} catch (Exception e) {
		}
	}

	/*
	 * 从对方读取文件
	 */
	public boolean listen2011() {
		try {
			byte[] b = new byte[1024];
			ServerSocket ss = new ServerSocket(2011);
			while (true) {
				Socket s = ss.accept();
				// 定义输入流，s.getInputStream();
				InputStream in = s.getInputStream();
				DataInputStream din = new DataInputStream(
						new BufferedInputStream(in));

				/**
				 * 创建要保存的文件
				 */
				String filePath = new String("");
				try {
					JFileChooser fileChooser = new JFileChooser(".");
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					int n = fileChooser.showSaveDialog(this);
					if (n == JFileChooser.APPROVE_OPTION) {
						filePath = fileChooser.getSelectedFile().getPath();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					return false;
				}

				File f = new File(filePath);
				RandomAccessFile fw = new RandomAccessFile(f, "rw");

				int num = din.read(b);
				while (num != -1) {
					// 向文件中写入0~num个字节
					fw.write(b, 0, num);
					// 跳过num个字节再次写入文件
					fw.skipBytes(num);
					// 读取num个字节
					num = din.read(b);
				}
				// 关闭输入，输出流
				din.close();
				fw.close();
				s.close();
				JOptionPane.showMessageDialog(this, "文件接收成功!");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 回应对方的传送文件请求
	 */
	public void response(boolean accept, String fileName, String aimIP) {
		DatagramSocket socketSend;
		try {
			socketSend = new DatagramSocket();
			String message = new String("");
			if (accept) {
				message += "accept" + M_seg + fileName;
			} else {
				message += "refuse" + M_seg + fileName;
			}

			byte[] buf = message.getBytes();
			InetAddress otherAddress = InetAddress.getByName(aimIP);
			DatagramPacket packet = new DatagramPacket(buf, buf.length,
					otherAddress, 2013);
			socketSend.send(packet);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listen2012() {
		String srcIP = null;
		try {
			DatagramSocket socketRecieve = new DatagramSocket(2012);
			while (true) {
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socketRecieve.receive(packet);
				InetAddress address = packet.getAddress();
				srcIP = address.getHostAddress();

				int length = packet.getLength();
				String message = new String(buf, 0, length);

				int result = JOptionPane.showConfirmDialog(this, "计算机" + srcIP
						+ "请求向您发送文件" + message + ",是否接受？");

				if (result == JOptionPane.YES_OPTION) {
					response(true, message, srcIP);
					// String fileName = new String(message);
					// if (listen2011()) {
					// JOptionPane.showMessageDialog(this, "文件接收成功!");
					// } else {
					// JOptionPane.showMessageDialog(this, "文件接收成功!");
					// }
				} else {
					response(false, message, srcIP);
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		String thread = Thread.currentThread().getName();
		if (thread.equals("2010")) {
			listen2010();
		} else if (thread.equals("2012")) {
			listen2012();
		} else if (thread.equals("2011")) {
			listen2011();
		} else if (thread.equals("send")) {
			tryToSend();
		}
	}

	public void actionPerformed(ActionEvent ae) {
		Object obj = ae.getSource();

		/**
		 * 传送文件
		 */
		if (obj == this.buttonSendFile) {
			JFileChooser chooser = new JFileChooser(".");
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				this.tfilePath = chooser.getSelectedFile().getAbsolutePath();
				this.tfileName = (new File(this.tfilePath)).getName();
				request();
				// this.sendTread.notify();
				// this.notifyAll();
				// new SendFile(this, fieldOtherIP.getText(), filename).send();
				// if (sf.send()) {
				// JOptionPane.showMessageDialog(this, "文件发送成功!");
				// } else {
				// JOptionPane.showMessageDialog(this, "文件发送失败!");
				// }
			} else {
			}
			return;
		}

		/**
		 * 发送
		 */
		if (obj == buttonSend) {
			try {
				DatagramSocket socketSend = new DatagramSocket();

				String strOtherIP = fieldOtherIP.getText().trim();
				if (strOtherIP.length() == 0) {
					String temp = this.fieldSelfIP.getText();
					int index = temp.lastIndexOf(".");
					strOtherIP = temp.substring(0, index);
					strOtherIP += ".255";
					JOptionPane.showMessageDialog(this, "对方的IP为空,将采用广播方式发送消息, 发送地址:" + strOtherIP);
				}

				String strOtherName = fieldOtherName.getText().trim();
				if (strOtherName.length() == 0) {
					strOtherName = new String("???");
					// JOptionPane.showMessageDialog(this, "对方的名字不能为空");
					// return;
				}

				String strSentence = fieldSentence.getText();
				if (strSentence.length() == 0) {
					JOptionPane.showMessageDialog(this, "要发送的内容不能为空");
					return;
				}

				this.printlnSendMessage(strOtherIP, strOtherName, strSentence);

				byte[] buf = strSentence.getBytes();

				/**
				 * 加密
				 */
				if (isEncry) {
					buf = Chat.setAndGetEncryption(buf);
				}

				InetAddress otherAddress;

				otherAddress = InetAddress.getByName(strOtherIP);

				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						otherAddress, 2010);
				socketSend.send(packet);
				fieldSentence.setText("");
			} catch (IOException e) {
			}
			return;
		}

		/**
		 * 导出聊天记录
		 */
		if (obj == buttonExport) {
			FileDialog fileDialog = new FileDialog(this, "导出聊天记录对话框",
					FileDialog.SAVE);
			fileDialog.setVisible(true);
			String filePath = fileDialog.getDirectory() + fileDialog.getFile(); // 得到文件路径
			if (fileDialog.getFile() == null) {
				// JOptionPane.showMessageDialog(this, "未指定文件名！", "提示",
				// JOptionPane.PLAIN_MESSAGE);
				return;
			}
			File f = new File(filePath.trim());
			try {
				FileWriter fw = new FileWriter(f);
				String text = areaContent.getText();
				fw.write(text);
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				return;
			}
			return;
		}
	}

	private void printlnSendMessage(String strOtherIP, String strOtherName,
			String strSentence) {

		String nowtimes = form.format(new Date());
		String show = nowtimes;
		show += "【我对 ";
		show += strOtherIP;
		show += " 的 ";
		show += strOtherName;
		show += " 说】 ";
		show += strSentence + "\r\n";
		areaContent.append(show);
		areaContent.setCaretPosition(areaContent.getText().length());
		insertDB(nowtimes, this.fieldSelfIP.getText(), strOtherIP, strSentence);
	}

	private void printlnRecMessage(String srcIP, String strOtherName,
			String strSentence) {
		String nowtimes = form.format(new Date());
		String show = nowtimes;
		show += "【 ";
		show += srcIP;
		show += " 的 ";
		show += strOtherName;
		show += " 对我说】";
		show += strSentence;
		show += "\r\n";
		areaContent.append(show);
		areaContent.setCaretPosition(areaContent.getText().length());
		insertDB(nowtimes, srcIP, this.fieldSelfIP.getText(), strSentence);
	}

	/**
	 * 加密解密函数
	 */
	public static byte[] setAndGetEncryption(byte[] buf) {
		int len = buf.length;
		for (int i = 0; i < len; i++) {
			buf[i] = (byte) (255 - buf[i]);
		}
		return buf;
	}

	/**
	 * 主函数，程序的入口
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Chat c = new Chat();
		c.rec2010.start();
		c.rec2011.start();
		c.rec2012.start();
		c.sendTread.start();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		// try {
		// new Socket(InetAddress.getByName(""), 2011);
		// } catch (UnknownHostException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		if (checkEncry.isSelected()) {
			this.isEncry = true;
		} else {
			this.isEncry = false;
		}
	}
	public void insertDB(String time, String srcIP, String aimIP,
			String content) {
		Connection con = null;
		PreparedStatement ps = null;
		String sentence = "insert into chat([time],[srcIP],[aimIP],[content]) values(?,?,?,?)";
		try {
			con = getConnection();
			ps = con.prepareStatement(sentence);
			ps.setString(1, time);
			ps.setString(2, srcIP);
			ps.setString(3, aimIP);
			ps.setString(4, content);
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				ps.close();
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	public Connection getConnection() throws Exception {
		Connection con = null;
		Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		con = DriverManager.getConnection("jdbc:odbc:XYchatDB");
		return con;
	}
}
