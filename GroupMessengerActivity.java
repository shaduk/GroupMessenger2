package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */



public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};
    private String failedProcess = "";
    static final int SERVER_PORT = 10000;
    static private int globalCount = 0;
    private int count = 0;
    private int seq = 0;
    private int processNo = 0;

    PriorityQueue<Message> HoldBackQ = new PriorityQueue<Message>(100, Message.PriorityComparator);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        // code taken from PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        for(int i = 0; i < 5; i++) {
            if(myPort.equals(REMOTE_PORT[i]))
            processNo = i;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how
         * you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final Button mButton = (Button) findViewById(R.id.button4);
        final EditText eText = (EditText)findViewById(R.id.editText1);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                String msg = eText.getText().toString();
                msg = "Message-" + seq + "-" + "false" + "-" + count + "-" + msg + "-" + processNo + "-" + processNo  + "-"+failedProcess+"-\n";
                count++;
                eText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            //code taken from PA1
//            Socket sk;

            try {
                while(true) {
                    Socket sk = serverSocket.accept();
                    InputStream is = sk.getInputStream();
                    DataInputStream dis = new DataInputStream(is);
                    OutputStream os;
                    DataOutputStream dos;
                    String message = dis.readUTF();
                    String[] messSplit = message.split("-");
                    Message serverMessage = new Message();
                    serverMessage.mType = messSplit[0];
                    serverMessage.mPriority = Integer.parseInt(messSplit[1]);
                    serverMessage.mStatus = Boolean.parseBoolean(messSplit[2]);
                    serverMessage.mId = Integer.parseInt(messSplit[3]);
                    serverMessage.mContent = messSplit[4];
                    serverMessage.mProcess = Integer.parseInt(messSplit[5]);
                    serverMessage.kSuggestingProcess = Integer.parseInt(messSplit[6]);
                    serverMessage.failP = messSplit[7];
                    failedProcess = serverMessage.failP;
                    if(!failedProcess.equals("")) {
                        Log.d(TAG,"failedProcess "+ failedProcess);
                        for (Message messObj : HoldBackQ) {
                            Log.d("in loop mess obj", String.valueOf(messObj.mId));
                            if (REMOTE_PORT[messObj.mProcess].equals(failedProcess) && messObj.mStatus == false) {
                                HoldBackQ.remove(messObj);
                            }
                        }
                    }

                    if (serverMessage.mType.equals("Message")) {
                        seq++;
                        serverMessage.mType = "Proposed";
                        serverMessage.mPriority = seq;
                        serverMessage.kSuggestingProcess = processNo;
                        HoldBackQ.add(serverMessage);
                        Log.d("size of q, process:", String.valueOf(HoldBackQ.size()));
                        Log.d("process: ", String.valueOf(processNo));
                        os = sk.getOutputStream();
                        dos = new DataOutputStream(os);
                        dos.writeUTF(serverMessage.toString());
                        Log.d("send msg", "Proposal sent");
                        is.close();
                        dis.close();
//                    dos.close();
                    } else if (serverMessage.mType.equals("Agreed")) {
                        //Log.d(TAG, "Entered Agreed");
                        seq = Math.max(seq, serverMessage.mPriority);
                        Message temp = null;

                        for (Message messObj : HoldBackQ) {
                            Log.d("in loop mess obj", String.valueOf(messObj.mId));
                            if ((messObj.mId == serverMessage.mId) && (messObj.mProcess==serverMessage.mProcess)) {
                                temp = messObj;
                                Log.d("Found temp", temp.toString());
                                HoldBackQ.remove(temp);
                                temp.mPriority = serverMessage.mPriority;
                                temp.kSuggestingProcess = serverMessage.kSuggestingProcess;
                                temp.mStatus = true;
                                HoldBackQ.add(temp);
                                break;
                            }
                        }
                        os = sk.getOutputStream();
                        dos = new DataOutputStream(os);
                        dos.writeUTF("Completed");
                        dos.flush();
                    }

                    while (!HoldBackQ.isEmpty() && HoldBackQ.peek().mStatus == true) {
                    //Log.d("PQueueS", HoldBackQ.peek().mContent);
                        publishProgress(HoldBackQ.poll().mContent);

                    }
                    //Log.d(TAG,"Socket is closing now");
//                    sk.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived+"\n");


            //code taken from PA 2A specification
            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(globalCount));
            keyValueToInsert.put("value", strReceived);
            //Log.d("key", Integer.toString(globalCount));
            //Log.d("value", strReceived);
            getContentResolver().insert(uri, keyValueToInsert);
            globalCount++;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

                String msgToSend = msgs[0];
                //code taken from PA1
                Message clientMessage = new Message();
                clientMessage.failP = failedProcess;
                int maxProposal = 0;
                int suggestingProcess = 0;
                for(String remotePort: REMOTE_PORT) {
                    try {
                        if(!remotePort.equals(failedProcess)) {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            OutputStream os = socket.getOutputStream();
                            DataOutputStream dos = new DataOutputStream(os);
                            dos.writeUTF(msgToSend);

                            Log.d(TAG, String.valueOf(socket.isClosed()));
                            Log.d(TAG, "Code before");
                            InputStream is = socket.getInputStream();
                            Log.d(TAG, "Code after");
                            DataInputStream dis = new DataInputStream(is);
                            String message = dis.readUTF();
                            is.close();
                            dis.close();
                            Log.d(TAG, "Received proposal");
                            String[] msg = message.split("-");

                            clientMessage.mType = msg[0];
                            clientMessage.mPriority = Integer.parseInt(msg[1]);
                            clientMessage.mStatus = Boolean.parseBoolean(msg[2]);
                            clientMessage.mId = Integer.parseInt(msg[3]);
                            clientMessage.mContent = msg[4];
                            clientMessage.mProcess = Integer.parseInt(msg[5]);
                            clientMessage.kSuggestingProcess = Integer.parseInt(msg[6]);


                            if (clientMessage.mType.equals("Proposed")) {
                                Log.d(TAG, "Closing socket");
                                if (maxProposal <= clientMessage.mPriority) {
                                    maxProposal = clientMessage.mPriority;
                                    suggestingProcess = clientMessage.kSuggestingProcess;
                                }
                                socket.close();
                            }
                        }
                    }
                    catch (StreamCorruptedException e) {
                        failedProcess = remotePort;
                    }
                    catch (EOFException e) {
                        failedProcess = remotePort;
                    }
                    catch (IOException e) {
                        failedProcess = remotePort;
                        //Log.d(TAG,"failedProcess "+ failedProcess);
                    }

                }
                clientMessage.mPriority = maxProposal;
                clientMessage.mType = "Agreed";
                clientMessage.kSuggestingProcess = suggestingProcess;

                for(String remotePort: REMOTE_PORT) {
                    try {
                        if(!remotePort.equals(failedProcess)) {
                            clientMessage.failP = failedProcess;
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                            OutputStream os = socket.getOutputStream();
                            DataOutputStream dos = new DataOutputStream(os);
                            dos.writeUTF(clientMessage.toString());
                            Log.d("Socket in remote port", String.valueOf(socket.isClosed()));
                            InputStream is = socket.getInputStream();
                            DataInputStream dis = new DataInputStream(is);
                            String message = dis.readUTF();
                            if(message.equals("Completed")) {
                                Log.d(TAG, "socket closing after completion");
                                dos.flush();
                                dos.close();
                                socket.close();
                            }
                        }
                    }
                    catch (StreamCorruptedException e) {
                        failedProcess = remotePort;
                    }
                    catch (EOFException e) {
                        failedProcess = remotePort;
                    }
                    catch (IOException e) {
                        failedProcess = remotePort;
                        //Log.d(TAG,"failedProcess "+ failedProcess);
                    }

                }


            return null;
        }
    }
}
