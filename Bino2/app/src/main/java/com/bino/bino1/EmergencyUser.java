package com.bino.bino1;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.bino.bino1.Utils.SharePreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Ref;

public class EmergencyUser extends AppCompatActivity {

    Button offEmergency, onEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_user);

        androidx.appcompat.app.ActionBar actionbar = this.getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeButtonEnabled(true);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String name = bundle.getString("nome");
        String lat = bundle.getString("lat");
        String lng = bundle.getString("lng");
        String address = bundle.getString("address");
        String whats = bundle.getString("whats");
        String userBd = bundle.getString("userBd");

        onEmergency = findViewById(R.id.btn_on_emergency);
        offEmergency = findViewById(R.id.btn_off_emergency);

        setEmergency();

        onEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtons(true, name, lat, lng, address, whats, userBd);
            }
        });

        offEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtons(false,"","","","", "", "");
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // This Activity allow only one action (Back To Menu);
        finish();
        return true;
    }

    public void setEmergency(){

        if(SharePreferences.getEmergency(getApplicationContext())){
            onEmergency.setVisibility(View.INVISIBLE);
            offEmergency.setVisibility(View.VISIBLE);
        }else {
            onEmergency.setVisibility(View.VISIBLE);
            offEmergency.setVisibility(View.INVISIBLE);
        }
    }

    public void actionButtons(boolean emergency, String name, String lat, String lng,  String address, String whats, String userBd){
        if(emergency){
            SharePreferences.setEmergency(getApplicationContext(), true);
            onEmergency.setVisibility(View.INVISIBLE);
            offEmergency.setVisibility(View.VISIBLE);
            callWhatsapp(whats, name, lat, lng, address);
            sendHelpRequest("online", "nao", whats, name, "emergencia", lat, lng, userBd);
        }else{
            SharePreferences.setEmergency(getApplicationContext(), false);
            onEmergency.setVisibility(View.VISIBLE);
            offEmergency.setVisibility(View.INVISIBLE);
            sendHelpRequest("encerrar", "nao", whats, name, "emergencia", lat, lng, userBd);
        }
    }

private void callWhatsapp(String phone, String name, String lat, String lng,  String address){

    boolean isWhatsappInstalled = whatsappInstalledOrNot("com.whatsapp");
    if (isWhatsappInstalled) {

        String phoneNumberWithCountryCode = "+55"+phone;
        String message = "Atenção! Esta é uma mensagem de ajuda gerada automaticamente pelo aplicativo Bino.\n" +
                name+" está precisando de ajuda e relatou uma situação de periogo na seguinte localização:\n" +
                "Latitude: "+lat+"\n" +
                "Longitude: "+lng+"\n" +
                "Endereço aproximado: "+address+"\n" +
                "Se você puder contatar uma autoridade competente seria ótimo. Fica atento.";

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("https://api.whatsapp.com/send?phone=%s&text=%s", phoneNumberWithCountryCode, message))));
    } else {
        Toast.makeText(this, "WhatsApp not Installed", Toast.LENGTH_SHORT).show();
        Uri uri = Uri.parse("market://details?id=com.whatsapp");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(goToMarket);

    }
}

    private boolean whatsappInstalledOrNot(String uri) {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    private void sendHelpRequest(String state, String img, String whatsapp, String nome, String request, String lat, String lng, String userBd) {



                    //lastLocation = location

                    //val lat = lastLocation.latitude
                    //val long = lastLocation.longitude
                    DatabaseReference databaseReference;
                    databaseReference = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference statusUpDateRef = databaseReference.child("pedidosAjuda");

                    if (state.equals("online")) {

                        //coloca o user online
                        statusUpDateRef.child(userBd).child("latlong").setValue(lat + lng);
                        statusUpDateRef.child(userBd).child("img").setValue("nao");
                        statusUpDateRef.child(userBd).child("lat").setValue(lat);
                        statusUpDateRef.child(userBd).child("long").setValue(lng);
                        //statusUpDateRef.child(userBd).child("whats").setValue(arrayUserInfos.get(5))
                        //statusUpDateRef.child(userBd).child("nome").setValue(arrayUserInfos.get(1))
                        statusUpDateRef.child(userBd).child("whats").setValue(whatsapp);
                        statusUpDateRef.child(userBd).child("nome").setValue(nome);
                        statusUpDateRef.child(userBd).child("request").setValue(request);
                        Toast.makeText(this, "Seu pedido de ajuda foi lançado todas as pessoas próximas foram avisadas", Toast.LENGTH_SHORT).show();

                    } else {

                        //remove o user
                        statusUpDateRef.child(userBd).removeValue();
                    }


    }
}