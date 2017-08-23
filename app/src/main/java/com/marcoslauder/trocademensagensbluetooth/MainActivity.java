package com.marcoslauder.trocademensagensbluetooth;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 1; // código de solicitação para ativar o Bluetooth

    private BluetoothAdapter mBluetoothAdapter; //rádio Bluetooth
    private ArrayAdapter<String> mArrayAdapter;

    private TextView status;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        this.status = (TextView) findViewById(R.id.txtStatus);

        mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.lista_padrao,R.id.item);
        ListView lista = (ListView) findViewById(R.id.listaPareados);
        lista.setAdapter(mArrayAdapter);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listaPareadosItemClick(parent, position);
            }
        });

        Button procurar = (Button) findViewById(R.id.btnProcurarDispositivo);
        procurar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                procurarClick();
            }
        });

        Button tornarVisivel = (Button) findViewById(R.id.btnTornarVisivel);
        tornarVisivel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tornarVisivelClick();
            }
        });

        Button btnMostrarPareados = (Button) findViewById(R.id.btnMostrarPareados);
        btnMostrarPareados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listaDispositivosPareados();
            }
        });

        Button btnEsperarConexao = (Button) findViewById(R.id.btnEsperarConexao);
        btnEsperarConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EsperarConexaoClick();
            }
        });

    }

    private void EsperarConexaoClick() {
        Intent conexaoActivity = new Intent(getApplicationContext(),ConexaoActivity.class);
        conexaoActivity.putExtra("tipo","esperar");
        startActivity(conexaoActivity);
    }

    private void tornarVisivelClick() {
        status.setText("Bluetooth Visivel");
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    private void procurarClick() {
        status.setText("Iniciando Descoberta de Dispositivos");
        mBluetoothAdapter.startDiscovery();
    }

    private void listaPareadosItemClick(AdapterView<?> parent, int position) {
        String selecionado = (String)parent.getAdapter().getItem(position);
        Intent conexaoActivity = new Intent(getApplicationContext(),ConexaoActivity.class);
        conexaoActivity.putExtra("tipo","conectar");
        conexaoActivity.putExtra("selecionado",selecionado);
        startActivity(conexaoActivity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!verificarDispositivoBluetooth()){ //se não tiver Bluetooth
            status.setText("Bluetooth não disponível!");
        }else {
            ativarBluetooth();
        }
    }

    protected void listaDispositivosPareados() {
        status.setText("Listando Dispositivos Pareados.");
        Set<BluetoothDevice> dispositivosPareados = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (dispositivosPareados.size() > 0) {

            // Loop through paired devices
            for (BluetoothDevice dispositivo : dispositivosPareados) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(dispositivo.getName() + "\n" + dispositivo.getAddress());
            }
            mArrayAdapter.notifyDataSetChanged();

        }
        status.setText("Dispositivos Pareados Listados.");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        if(requestCode == REQUEST_ENABLE_BT){
            if(requestCode == RESULT_OK){
                //Bluetooth Ativado
                status.setText("Bluetooth Ativo");
            }else if(requestCode == RESULT_CANCELED){
                status.setText("Bluetooth Inativo");
                //Ação cancelada, Bluetooth desativado
            }
        }
    }

    protected  boolean verificarDispositivoBluetooth(){
        status.setText("Verificando Dispositivo Bluetooth");
        //pega o rádio Bluetooth do aparelho
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //se não existir um rádio Bluetooth retorna falso
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    protected void ativarBluetooth(){
        status.setText("Verificado se o Bluetooth está ativo.");
        //Se o Bluetooth não estiver ativo
        if (!mBluetoothAdapter.isEnabled()) {
            status.setText("Solicitando ativação do Bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //retorna em onActivityResult
        }else{
            status.setText("Bluetooth está ativo.");
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                status.setText("Dispositivo Encontrado");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };
}
