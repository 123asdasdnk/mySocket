import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 *
 * 整个服务器的设计思路如下：
 *
 * 创建一个 ServerSocket 实例，用于监听客户端连接请求。
 *
 * 当有客户端连接成功后，为该客户端创建一个新的线程，用于处理客户端发送的消息。
 *
 * 在处理客户端消息的线程中，通过 socket.getInputStream() 获取客户端发送的消息，根据消息内容进行判断。
 *
 * 如果是包含特定标识的用户名信息，将该用户名存入 Map 中并向所有客户端广播当前在线用户列表。
 *
 * 如果是普通聊天信息，则将该信息广播给所有在线客户端。
 *
 * 在处理消息的过程中，如果发生异常，则通过 e.printStackTrace() 打印异常信息。
 *
 * 在广播消息时，需要使用线程安全的 Map 实例来存储用户的 Socket 和用户名的映射关系，以确保在多线程环境下操作存储用户的 Socket 和用户名的映射关系时不会发生数据竞争和并发修改异常。
 *
 */

// 每当有一个客户端连接成功后，就会创建一个新的 ThreadServer 线程来处理该客户端的消息。
// 因此，如果有 n 个客户端连接到服务器，就会创建 n 个 ThreadServer 线程，每个线程负责处理一个客户端的消息。当客户端断开连接时，对应的线程也会结束。
public class Server {//服务器

    //定义Map集合用于存储用户的Socket以及用户的名字   key:Socket    Value:用户名
    //使用 Collections.synchronizedMap(new HashMap<Socket,String>());
    //的目的是创建一个线程安全的 Map 实例，用于在多线程环境下操作存储用户的 Socket 和用户名的映射关系。
    public final static Map<Socket,String> socketsMaps = Collections.synchronizedMap(new HashMap<Socket,String>());
    public static void main(String[] args) {
        try {
            // 创建服务端套接字
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("------服务端启动,等待客户端连接-------");
            while (true) {
                // 监听客户端套接字，若有客户端连接，则代码不会往下执行，否则会堵塞在此处。(有一个客户端连接就可以)
                Socket socket = serverSocket.accept();//接收客户端的socket
                System.out.println("------客户端已连接-------");
                // 开启线程，用于读取客户端发送的信息，并转发给每一个客户端
                //当有客户端连接成功后，为该客户端创建一个新的线程，用于处理客户端发送的消息。
                new ThreadServer(socket).start();
            }
        } catch (Exception e) {
            //异常处理
            e.printStackTrace();
        }
    }
}

//        用于处理客户端的消息。
//        在run()方法中，通过socket.getInputStream()获取客户端发送的消息，根据消息内容进行判断，如果是包含特定标识的用户名信息，
//        则将该用户名存入Map中并向所有客户端广播当前在线用户列表；
//        如果是普通聊天信息，则将该信息广播给所有在线客户端。在处理消息的过程中，如果发生异常，则通过e.printStackTrace()打印异常信息。
class ThreadServer extends Thread {
    private Socket socket;
    ThreadServer(){};
    ThreadServer(Socket socket)
    {
        this.socket = socket;
    }
    @Override
    public void run() {
        try {
            while(true)
            {
                //先获取数据流和客户端输入的内容
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                String data = dataInputStream.readUTF();

                if(data.startsWith("①②③④")&&data.endsWith("①②③④"))//发送过来的是用户名(①②③④是分隔符，中间的内容为用户)
                {
                    //将Socket以及用户名字都存放在Map集合中
                    Server.socketsMaps.put(socket, data.replace("①②③④",""));
                    //获取所有的key(Socket)，将所有用户的名字发送至客户端
                    Set<Socket> sockets = Server.socketsMaps.keySet();
                    //获取所有的用户的名字，将这些名字拼装成一个字符串
                    Collection<String> names = Server.socketsMaps.values();
                    StringBuffer sbf = new StringBuffer();
                    for(String userName :names)
                    {
                        sbf.append(userName).append(",");
                    }
                    System.out.println("sbf:"+sbf.toString());
                    for(Socket soc:sockets)
                    {
                        DataOutputStream dataOutputStream = new DataOutputStream(soc.getOutputStream());
                        dataOutputStream.writeUTF("①②③④"+sbf.toString()+"①②③④");
                        dataOutputStream.flush();//刷新缓存区,确保发送成功
                    }
                }
                else{
                    //发送过来的是聊天信息，要通过服务端将聊天信息广播给所有客户端
                    //获取所有的key(Socket)，将所有用户的名字发送至客户端
                    Set<Socket> sockets = Server.socketsMaps.keySet();
                    //將聊天信息广播出去
                    for(Socket soc:sockets)
                    {
                        DataOutputStream dataOutputStream = new DataOutputStream(soc.getOutputStream());
                        dataOutputStream.writeUTF("[ "+ Server.socketsMaps.get(socket)+" ]:"+data);
                        dataOutputStream.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}