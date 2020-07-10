package server;


import javax.management.RuntimeOperationsException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;


public class ClientHandler {
    Server server;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;
    private String nick;
    private String login;

    private Timer mTimer;
    private TimerTask mMyTimerTask;
    public ClientHandler(Server server, Socket socket) {
  //тфймер по истечкнию которого будут исполнятся действия в таймертаск
        mTimer = new Timer();
   //команды для исполнения по окончаеию таймера
        mMyTimerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    out.writeUTF("/timeout ");
                    out.writeUTF("/timeout ");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
      //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server
                                    .getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                sendMsg("/authok " + newNick);
                                nick = newNick;
                                login = token[1];
                                server.subscribe(this);
                                System.out.printf("Клиент %s подключился \n", nick);
                                //После подключения таймер обнуляется.
                                break;
                            } else {
                                sendMsg("Неверный логин / пароль");
 //Использование таймера с командами
                                mTimer.schedule(mMyTimerTask,5000);
                            }
                        }

                        server.broadcastMsg(str);


                    }
                    //цикл работы
                    while(true){

                            while (true) {
                                String str = in.readUTF();

                                if (str.equals("/end")) {
                                    out.writeUTF("/end");
                                    break;
                                }

                                server.broadcastMsg(str);
                            }

                    }

                } catch (IOException e) {
                    e.printStackTrace();

                } finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this);
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void sendMsg(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
