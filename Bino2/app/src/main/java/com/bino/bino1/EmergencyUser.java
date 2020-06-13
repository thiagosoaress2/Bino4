package com.bino.bino1;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bino.bino1.Utils.SharePreferences;

public class EmergencyUser extends AppCompatActivity {

    ImageView onEmergency;
    Button offEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_user);

        onEmergency = findViewById(R.id.btn_on_emergency);
        offEmergency = findViewById(R.id.btn_off_emergency);

        setEmergency();

        onEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtons(true);
            }
        });

        offEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtons(false);
            }
        });
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

    public void actionButtons(boolean emergency){

        if(emergency){
            SharePreferences.setEmergency(getApplicationContext(), true);
            onEmergency.setVisibility(View.INVISIBLE);
            offEmergency.setVisibility(View.VISIBLE);
            callWhatsapp("021969384620");
        }else{
            SharePreferences.setEmergency(getApplicationContext(), false);
            onEmergency.setVisibility(View.VISIBLE);
            offEmergency.setVisibility(View.INVISIBLE);
        }
    }

private void callWhatsapp(String phone){

    boolean isWhatsappInstalled = whatsappInstalledOrNot("com.whatsapp");
    if (isWhatsappInstalled) {
        Uri uri = Uri.parse("smsto:" + "55"+phone);
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Mensagem!!! Emergenciaaa");
        sendIntent.setPackage("com.whatsapp");
        startActivity(sendIntent);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}