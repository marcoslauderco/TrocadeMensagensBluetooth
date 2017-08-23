package com.marcoslauder.trocademensagensbluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;


public class ConexaoActivity extends AppCompatActivity {

    private static final UUID MY_UUID = new UUID(2017,8);
    private static final String NAME = "trocademensagensBluetooth";
    private String selecionado;
    private String name;
    private String address;
    private BluetoothAdapter mBluetoothAdapter; //rádio Bluetooth
    private TextView txtConecatado;
    private ConnectedThread connectedThread;
    private ArrayAdapter<String> mArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexao);

        Bundle extras = getIntent().getExtras();

        verificarDispositivoBluetooth();

        if(extras.getString("tipo").equals("conectar")) {
            this.selecionado = extras.getString("selecionado");
            String[] splitSelecionado = this.selecionado.split("\n");
            this.address = splitSelecionado[1];
            this.conectarCom(this.address);
        }else{
            this.esperarConexao();
        }
        txtConecatado = (TextView)findViewById(R.id.txtConecatado);

        mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.lista_padrao,R.id.item);
        ListView lista = (ListView) findViewById(R.id.listaMensagem);
        lista.setAdapter(mArrayAdapter);

        Button enviar = (Button) findViewById(R.id.enviar);
        enviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarClick();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void conectarCom(String endereco){
        BluetoothDevice dispositivo = mBluetoothAdapter.getRemoteDevice(endereco);
        ConnectThread connectThread = new ConnectThread(dispositivo);
        connectThread.start();
    }

    private void esperarConexao(){
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void enviarClick(){
        EditText etMensagem = (EditText) findViewById(R.id.etMensagem);
        String mensagem = etMensagem.getText().toString();
        atualizaMensagens("Eu",mensagem);

        byte[] send = mensagem.getBytes();
        connectedThread.write(send);

    }

    protected  boolean verificarDispositivoBluetooth(){
        //pega o rádio Bluetooth do aparelho
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //se não existir um rádio Bluetooth retorna falso
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    String readMessage = new String(buffer, 0,bytes);
                    atualizaMensagens(mmSocket.getRemoteDevice().getName(),readMessage);
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void manageConnectedSocket(BluetoothSocket socket) {
        this.name = socket.getRemoteDevice().getName();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtConecatado.setText(name);
            }
        });

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private void atualizaMensagens(String quem,String mensagem){
        final String mensagemcompleta = quem+": "+mensagem;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mArrayAdapter.add(mensagemcompleta);
                mArrayAdapter.notifyDataSetChanged();
            }
        });

    }
}
