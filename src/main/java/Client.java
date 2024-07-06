

import java.awt.*;

import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.*;


//在init()方法中，首先通过弹出对话框获取用户输入的用户名，然后创建套接字连接到服务器，并向服务器发送用户名信息。
// 接着创建了一个用于读取服务器发送的信息的线程，并初始化了客户端的界面，包括消息展示框、在线用户名称展示框、消息发送框和发送按钮。
// 当用户点击发送按钮时，会将消息发送到服务器。
// 整个程序主要实现了用户登录、消息发送和接收功能。


public class Client {

    private JFrame mainWin = new JFrame("聊天窗口");//创建一个窗口

    // 消息展示框
    private JTextArea displayTa = new JTextArea(14, 40);

    // 在线用户名称展示框
    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);


//用于实现在线用户名称的展示。具体来说：
//DefaultListModel<String> userListModel = new DefaultListModel<>();
//创建了一个`DefaultListModel`对象，用于存储在线用户列表。`DefaultListModel`是一个可动态添加、删除元素的列表模型，适合用于JList组件。
//JList<String> userList = new JList<>(userListModel);
//创建了一个JList组件，用于显示用户列表。这里将userListModel作为参数传递给JList的构造函数，使得JList组件的数据源是userListModel。
//当你需要更新在线用户列表时，
// 只需调用`userListModel.addElement()`或`userListModel.removeElement()`方法来添加或删除元素，然后JList组件会自动更新显示的内容。

    // 消息发送框
    private JTextArea inputTF = new JTextArea(4, 40);

    // 消息按钮
    private JButton sendBn = new JButton("发送");

    // 用户记录当前聊天用户名
    private String curUser;

    public static void main(String[] args) {
        new Client().init();
    }

    private void init() {//初始化
        try {
            // 通过弹出对话框获取用户输入的用户名
            String userName = JOptionPane.showInputDialog(mainWin, "请输入您的用户名：");
            // 把用户输入的用户名，赋给curUser
            curUser = userName;
            mainWin.setTitle(curUser + "的聊天窗口");

            // 创建套接字
            Socket socket = new Socket("localhost", 8080);//会传到服务器
            // 向服务器声明
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            //发送用户名到服务端
            dataOutputStream.writeUTF("①②③④"+userName+"①②③④");
            //其中包含了一个特殊的分隔符①②③④和一个变量userName。
            // 这个字符串的目的是将userName和分隔符组合成一个完整的字符串，以便在接收端可以根据分隔符将userName提取出来。
            dataOutputStream.flush();

            // 开启线程，用于读取服务器发送的信息
            new ThreadClient(socket, this).start();

            //创建panel
            JPanel bottomPanel = new JPanel();

            // 将消息框和按钮添加到窗口的底端
            mainWin.add(bottomPanel, BorderLayout.SOUTH);
            bottomPanel.add(inputTF);
            bottomPanel.add(sendBn);
            //创建监听器
            ActionListener listener = e -> {
                // 获取用户发送的消息
                String message = inputTF.getText();//获取输入框里面的文本
                sendSms(message,socket);
            };
            // 给发送消息按钮绑定点击事件监听器
            sendBn.addActionListener(listener);
            //创建中间面板容器
            JPanel centerPanel = new JPanel();

            // 将展示消息区centerPanel添加到窗口的中间
            mainWin.add(centerPanel);
            // 让展示消息区可以滚动
            centerPanel.add(new JScrollPane(displayTa));//设定滚动
            displayTa.setEditable(false);
            // 用户列表放到窗口的最右边
            Box rightBox = new Box(BoxLayout.Y_AXIS);
            userList.setFixedCellWidth(60);
            userList.setVisibleRowCount(13);
            rightBox.add(new JLabel("用户列表："));
            rightBox.add(new JScrollPane(userList));//也可以滚动
            centerPanel.add(rightBox);


            // 关闭窗口退出当前程序
            mainWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainWin.pack(); // swing加上这句就可以拥有关闭窗口的功能
            mainWin.setVisible(true);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    //点击发送后将消息发送到服务器
    protected void sendSms(String sms, Socket socket) {
        try {
            //发送聊天消息到服务端
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(sms);
            dataOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public DefaultListModel<String> getUserListModel() {//获取用户列表
        return userListModel;
    }
    public JTextArea getDisplayTa() {//获取消息区域
        return displayTa;
    }
    public JTextArea getInputTF()//获取消息发送框
    {
        return inputTF;
    }
}

//---------------------------------------
// 定义线程类，用来读取服务器发送的信息
class ThreadClient extends Thread {//由于作为外部类，不能为public
    private Socket socket;
    private Client client;

    ThreadClient() {
    }

    ThreadClient(Socket socket, Client client) {
        this.socket = socket;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (true) {
                DataInputStream DataInputStream = new DataInputStream(socket.getInputStream());
                String message = DataInputStream.readUTF();

                if(message.startsWith("①②③④")&&message.endsWith("①②③④"))
                {
                    //说明信息是用户名
                    String[] names = message.replace("①②③④","").split(",");
                    // 将用户列表先清空
                    client.getUserListModel().clear();
                    for (int i = 0; i < names.length; ++i) {
                        client.getUserListModel().addElement(names[i]);//然后将这些用户名添加到列表中。
                    }
                }
                else
                {
                    //说明是聊天信息，将聊天信息放在displayTa中
                    client.getInputTF().setText("");//清空客户端聊天界面中的输入文本框内容。
                    client.getDisplayTa().append(message+"\t\n");//将信息添加到信息界面中
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
