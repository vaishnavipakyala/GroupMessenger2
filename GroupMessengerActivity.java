package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
    static final String FINALS = GroupMessengerActivity.class.getSimpleName();
    static int Sequence_num = 0;
    static double proposedSeq = 0;
    static boolean isfailed= false;
    static String failedPort = "";
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    static double SequenceNumber =0;
    static double AgreedSeqNumber =0;
    static final int SERVER_PORT = 10000;
    public static int counterNumber =0;
  //  public static int index =0;
    static final String ACK = "Message received";
    ArrayList<Double> arrayList_FindMax=new ArrayList<Double>();
    //The reference for priority queue is from https://www.geeksforgeeks.org/implement-priorityqueue-comparator-java/
    // https://stackoverflow.com/questions/683041/how-do-i-use-a-priorityqueue
    //https://www.callicoder.com/java-priority-queue/
    //https://stackoverflow.com/questions/12917372/priority-queues-of-objects-in-java
    PriorityQueue<Queue_Object> HoldBackQueue = new PriorityQueue<Queue_Object>(100, Queue_Object.ComparingMechanism);

    static class Queue_Object extends Object {
        int counter;
        String MessageId;
        String Message;
        String portPort;
        double SeqNo;
        double portNo;
        String done;

        public Queue_Object(){
            this.counter= 0;
            this.SeqNo =0;
            this.portPort="";
            this.portNo =0;
            this.MessageId ="";
            this.Message= "";
            this.done= "";

        }
        public static Comparator<Queue_Object> ComparingMechanism = new Comparator<Queue_Object>() {

            public int compare(Queue_Object x, Queue_Object y)
            {
                if(x.SeqNo < y.SeqNo) {
                    return -1;
                }
                else if(x.SeqNo > y.SeqNo)
                {
                    return 1;
                }
                else{
                    if((x.portNo)<(y.portNo))
                    {
                        return -1;
                    }
                    else if((x.portNo)>(y.portNo))
                    {
                        return 1;
                    }
                }
                return 0;
            }
        };
        public String toString() {
            return " Message No: " + this.MessageId+" Sequence No: " + this.SeqNo + " Msg: " + this.Message + " Status: "+ this.done +" Port Number: " + this.portNo;
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        // The reference of TelephonyManager, server socket is obtained from SimpleMessenger (Project Assignment 1)
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        // The following is the implementation of OnClickListener for the send Button(id : button4) and the onClick is obtained from SimpleMessenger (Project Assignment 1)
        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Queue_Object obj;
            try {
                while (true) {
                    /* The reference for socket programming is from https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/SocketProgramming/SocketProgram.html#*/
                    Socket clientSocket = serverSocket.accept();
                 /*   InputStream is = clientSocket.getInputStream();
                    DataInputStream dis = new DataInputStream(is);
                    OutputStream os;
                    DataOutputStream dos;
                    String input = dis.readUTF(); */
                    // String Proposal= "";
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter pw = new PrintWriter(clientSocket.getOutputStream(), true);
                    Queue_Object serverMsg = new Queue_Object();
                    String input = in.readLine();
                    if (null != input && !input.isEmpty())
                    {
                    Log.i(TAG, "Entered Server Socket");
                    String[] Input_Split = input.split(":");
                    int leg = Input_Split.length;
                    Log.i(TAG, "Input_Split length " + leg);

                    if (isfailed == true) {
                        for (Queue_Object messObj : HoldBackQueue) {
                            Log.d("in loop mess obj", String.valueOf(messObj.MessageId));
                            if (messObj.portPort.equals(failedPort) && messObj.done.equals("Undeliverable")) {
                                HoldBackQueue.remove(messObj);
                                Log.i(TAG, "FAILED QUEUE REMOVAL " + messObj.Message + ":" + messObj.done + ":" + messObj.portPort);

                            }
                        }
                    }

                    if (!Input_Split[0].equals("Y")) {
                        Log.i(TAG, "Entered Msg Only");
                        String msgId = Input_Split[0];
                        String msg = Input_Split[3];
                        String portports = Input_Split[2];
                        double portnumber = Double.parseDouble(Input_Split[1]);

                        //  SequenceNumber = Math.max(SequenceNumber,AgreedSeqNumber) +1;
                        //String Proposal= "";
                        SequenceNumber++;
                        double Proposal = SequenceNumber + portnumber;
                        //  Proposal = Integer.toString(portnumber)++Float.toString(SequenceNumber);
                        serverMsg.Message = msg;
                        serverMsg.MessageId = msgId;
                        //serverMsg.SeqNo = SequenceNumber;
                        serverMsg.SeqNo = Proposal;
                        serverMsg.portNo = portnumber;
                        serverMsg.portPort = portports;
                        serverMsg.done = "Undeliverable";
                        //The reference for synchronized function is from https://www.geeksforgeeks.org/synchronized-in-java/
                        //     synchronized (HoldBackQueue) {
                        HoldBackQueue.add(serverMsg);
                        Log.i(TAG, "Added to queue");
                        //   }
                        Log.i("TEST", "msg - " + serverMsg.toString());
                        Log.i(TAG, Double.toString(portnumber) + ":" + Double.toString(Proposal) + ":" + "Proposed");
                       /* os = clientSocket.getOutputStream();
                        dos = new DataOutputStream(os);
                        dos.writeUTF(portnumber+"-"+Integer.toString(SequenceNumber)+"-"+"Proposed");
                        is.close();
                        dis.close(); */
                        //  pw.println(portnumber+":"+Integer.toString(SequenceNumber)+":"+"Proposed");
                        pw.println(Double.toString(portnumber) + ":" + Double.toString(Proposal) + ":" + "Proposed");
                        //clientSocket.close();

                    }
                    if (Input_Split[0].equals("Y")) {
                        Log.i(TAG, "Entered part agreement");
                        AgreedSeqNumber = Double.parseDouble(Input_Split[2]);
                        SequenceNumber = Math.max(SequenceNumber, AgreedSeqNumber);
                        Double msg_port = Double.parseDouble(Input_Split[1]);

                        String msggID = Input_Split[3];
                        String portss = Input_Split[4];
                        String messag = Input_Split[5];
                        Queue_Object temp = null;
                        Queue_Object serverMsg2 = new Queue_Object();
                        serverMsg2.SeqNo = AgreedSeqNumber;
                        serverMsg2.portPort = portss;
                        serverMsg2.portNo = msg_port;
                        serverMsg2.MessageId = msggID;
                        serverMsg2.Message = messag;
                        serverMsg2.done = "Deliver";
                        Log.i("TEST", "agree - " + serverMsg.toString());
                        Log.i(TAG, "Max agreed number : " + Double.toString(AgreedSeqNumber));
                        for (Queue_Object mess : HoldBackQueue) {
                            if ((mess.MessageId.equals(serverMsg2.MessageId)) && (mess.portNo == serverMsg2.portNo)) {

                                Log.i(TAG, "Queue for loop entered");
                                //  temp = mess;
                                //Log.i(TAG, "Queue print temp" + temp.toString());
                                //  synchronized (HoldBackQueue) {
                                HoldBackQueue.remove(mess);
                                Log.i(TAG, "Queue Sync remove" + HoldBackQueue.toString());
                                //    }
//                                    temp.SeqNo = serverMsg2.SeqNo;
//                                    temp.portNo = serverMsg2.portNo;
//                                    temp.done = "Deliver";
                                //   synchronized (HoldBackQueue) {
                                HoldBackQueue.add(serverMsg2);
                                //  }
                                break;

                            }
                        }
                        Log.i(TAG, "AFTER Queue Sync remove" + HoldBackQueue.toString());
                        pw.println("Delivered");
                        // clientSocket.close();

                    }
                }
                    Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                    while (!HoldBackQueue.isEmpty() && HoldBackQueue.peek().done.equals("Deliver") ) {
                        Log.i(FINALS,"FINAL SEQ " +HoldBackQueue.peek().Message + " " + HoldBackQueue.peek().SeqNo);
                        Log.i(TAG,"PQueueS while" +HoldBackQueue.peek().Message);
                    /*    String msg = HoldBackQueue.poll().Message;
                        publishProgress(msg);
                        ContentResolver ContentRes = getContentResolver();
                        String KEY_FIELD = "key";
                        String VALUE_FIELD = "value";
                        ContentValues ContentVal = new ContentValues();
                        ContentVal.put(KEY_FIELD, Integer.toString(Sequence_num));
                        ContentVal.put(VALUE_FIELD, msg);
                        ContentRes.insert(mUri, ContentVal);
                        Sequence_num += 1; */
                        Queue_Object q = HoldBackQueue.poll();
                        String msg = q.Message;
                        publishProgress(msg);
                        ContentResolver ContentRes = getContentResolver();
                        String KEY_FIELD = "key";
                        String VALUE_FIELD = "value";
                        ContentValues ContentVal = new ContentValues();
                        ContentVal.put(KEY_FIELD, Integer.toString(Sequence_num));
                        ContentVal.put(VALUE_FIELD, msg);
                        ContentRes.insert(mUri, ContentVal);
                        Sequence_num += 1;
                        Log.d("deb", String.valueOf(Sequence_num - 1) + " " + Double.toString(q.SeqNo) + " " + msg);


                    }

                }

            } catch (IOException e) {
                Log.e(TAG, "failed");
            }
            return null;
        }
        // The following is the implementation of displaying what is received is obtained from SimpleMessenger (Project Assignment 1)
        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            super.onProgressUpdate(strings);
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            return;
        }
    }

    // The reference for this buildUri is obtained from OnPTestClickListener
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msgToSend = msgs[0];
            String port_number= msgs[1];

            // This is to send the initial messae with the sequence number
            for(String remoteports : remotePorts) {

                if (remoteports.contentEquals(REMOTE_PORT0)) proposedSeq = 0.1;
                else if (remoteports.contentEquals(REMOTE_PORT1)) proposedSeq = 0.2;
                else if (remoteports.contentEquals(REMOTE_PORT2)) proposedSeq = 0.3;
                else if (remoteports.contentEquals(REMOTE_PORT3)) proposedSeq = 0.4;
                else if (remoteports.contentEquals(REMOTE_PORT4)) proposedSeq = 0.5;
                else proposedSeq = 99999;

                String msgID=port_number+Integer.toString(counterNumber);
                try {

                    /* The reference for socket programming is from https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/SocketProgramming/SocketProgram.html#*/
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remoteports));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    //out.println(Double.toString(proposedSeq)+":"+msgToSend);
                    out.println(msgID+":"+Double.toString(proposedSeq)+":"+port_number+":"+msgToSend);
                    Log.i(TAG,"Client Task: "+Double.toString(proposedSeq)+":"+msgToSend);

                    //This is the reply from the server with their own highest priorities
                    BufferedReader buf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String readstring = buf.readLine();
                    if(readstring != null && !readstring.isEmpty()) {
                        String[] fragments = readstring.split(":");
                        // String ports=fragments[0];
                        String proposedNumber = fragments[1];
                        Log.i(TAG, "Server reply at client: " + readstring);
                        double Proposed_no = Double.parseDouble(proposedNumber);
                        //Find the maximum of the Proposals received
                        arrayList_FindMax.add(Proposed_no);
                    }
                    out.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    int i;
                    int j = 0;
                    failedPort= remoteports;
                    isfailed =true;
                    String[] lst = new String[4];
                    for(i=0;i<remotePorts.length;i++){
                        if (!remotePorts[i].equals(remoteports)){
                            lst[j] = remotePorts[i];
                            j++;
                        }
                    }
                    remotePorts = lst;
                    continue;
                }
            }
            double maxProposal = 0;
            for(int i=0;i<arrayList_FindMax.size();i++)
            {
                if(arrayList_FindMax.get(i)>maxProposal)
                    maxProposal=arrayList_FindMax.get(i);
            }
        //    Log.i("TAGS", "MAX AND MSG "+ maxProposal);
            for(String remoteports : remotePorts) {
                try {

                    if (remoteports.contentEquals(REMOTE_PORT0)) proposedSeq = 0.1;
                    else if (remoteports.contentEquals(REMOTE_PORT1)) proposedSeq = 0.2;
                    else if (remoteports.contentEquals(REMOTE_PORT2)) proposedSeq = 0.3;
                    else if (remoteports.contentEquals(REMOTE_PORT3)) proposedSeq = 0.4;
                    else if (remoteports.contentEquals(REMOTE_PORT4)) proposedSeq = 0.5;
                    else proposedSeq = 99999;


                    String msgID=port_number+Integer.toString(counterNumber);
                    String maxString = Double.toString(maxProposal);

                    /* The reference for socket programming is from https://www.oracle.com/webfolder/technetwork/tutorials/obe/java/SocketProgramming/SocketProgram.html#*/
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remoteports));
                   /* OutputStream os = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.writeUTF(msgID+"-"+port_number+"-"+msgToSend+"-"+maxProposal); */
                   // Thread.sleep(500);

                   /* InputStream is = socket.getInputStream();
                    DataInputStream dis = new DataInputStream(is);
                    String readstring = dis.readUTF(); */

                    PrintWriter pout = new PrintWriter(socket.getOutputStream(), true);
                   // pout.println("Y"+":"+Double.toString(proposedSeq)+":"+maxString+":"+msgToSend);
                    pout.println("Y"+":"+Double.toString(proposedSeq)+":"+maxString+":"+msgID+":"+port_number+":"+msgToSend);
                    Log.i(TAG,"Agreement: "+"Y"+":"+Double.toString(proposedSeq)+":"+maxString+":"+msgToSend);

                    //This is the reply from the server with their own highest priorities
                    BufferedReader buf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String readstring = buf.readLine();


                      if(readstring!=null && !readstring.isEmpty() && readstring.equals("Delivered")){
                          Log.i(TAG, "DONEEEEEEEEE");

                      }

                  //  dos.flush();
                  //  dos.close();
                    pout.close();
                    socket.close();

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                    Log.e(TAG, "ClientTask socket IOException");
                    int i;
                    int j = 0;
                    failedPort= remoteports;
                    isfailed= true;
                    String[] lst = new String[4];
                    for(i=0;i<remotePorts.length;i++){
                        if (!remotePorts[i].equals(remoteports)){
                            lst[j] = remotePorts[i];
                            j++;
                        }
                    }
                    remotePorts = lst;
                    continue;

                }
            }

            counterNumber++;
            return null;

        }
    }


    }
