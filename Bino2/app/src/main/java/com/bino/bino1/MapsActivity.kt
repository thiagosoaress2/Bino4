package com.bino.bino1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

import kotlinx.android.synthetic.main.activity_menu.*
import java.io.IOException

import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    NavigationView.OnNavigationItemSelectedListener{

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    var userBd: String = "nao"
    var userMail: String = "nao"
    val raioBusca = 1.0 //  0.1 = 1km no mapa              obs: Mudamos para 10 km
    val arrayTruckersNerby: MutableList<Marker> = ArrayList()
    val arrayPlacesNerby: MutableList<Marker> = ArrayList()

    val arrayUserInfos: MutableList<String> = ArrayList()

    var userIsVisibile = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        databaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //recupera o email do usuário
        userMail = intent.getStringExtra("email")


        if (!requestPermission()){
            requestThePermission()
        }

        fab.setOnClickListener(View.OnClickListener { view ->
            val intent = Intent(this, EmergencyUser::class.java)
            startActivity(intent)
        })

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle =
            ActionBarDrawerToggle(
                this,
                drawer,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        requestToOpenGpsLikeWaze()  //liga o GPS do user
        firstMeths()

        if (!userMail.equals("semlogin")){
            //verificar se já inseriu código
            queryGetUserInfos()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.home -> drawer.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        if (id == R.id.nav_user) {
            toastDeveloping()
        } else if (id == R.id.nav_friendsUser) {
            toastDeveloping()
        } else if (id == R.id.nav_getHelp) {
            toastDeveloping()
        }else if (id == R.id.nav_myHealth) {
            toastDeveloping()
        }else if (id == R.id.nav_newLocations) {
            toastDeveloping()
        }else if (id == R.id.nav_historyUser) {
            toastDeveloping()
        }else if (id == R.id.nav_settings) {
            toastDeveloping()
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }



    fun toastDeveloping(){
        Toast.makeText(applicationContext, R.string.developing, Toast.LENGTH_LONG).show()
    }

    fun firstMeths(){  //métodos retirados do onCreate pra acelerar o processo de abertura da activity

        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        btnLogout.setOnClickListener {  //este botao nao vai ficar. Só usaremos nos testes para configurar o login
            auth = FirebaseAuth.getInstance()
            auth.signOut()
            finish()
        }

        btnLogin.setOnClickListener {
            auth = FirebaseAuth.getInstance()
            auth.signOut()
            finish()
        }

        val btnAddLugar: Button = findViewById(R.id.btnAddPlace)
        btnAddLugar.setOnClickListener {
            addNewPlace()
        }

        val btnGenerateCode: Button = findViewById(R.id.btnGenerateCode)
        btnGenerateCode.setOnClickListener {
            generateCode()
        }

        val btnVisibleInvisible: Button = findViewById(R.id.btnVisibleInvisible)
        btnVisibleInvisible.setOnClickListener {

            if (userMail.equals("semLogin")){
                showToast("Você precisa estar logado para fazer isso")
            } else {

                if (userIsVisibile) {
                    updateUserStatus("offline", "null")
                    showToast("Você está invisivel")
                    btnVisibleInvisible.setText("Ficar visível")
                } else {
                    updateUserStatus("online", arrayUserInfos.get(2).toString())
                    showToast("Você está visível")
                    btnVisibleInvisible.setText("Ficar invisivel")
                }
            }
        }

        val btnEditarPerfil : Button = findViewById(R.id.btnPerfil)
        btnEditarPerfil.setOnClickListener {

            if (userMail.equals("semLogin")){
                showToast("Você precisa estar logado para fazer isso")
            } else {

                val intent = Intent(this, perfilActivity::class.java)
                intent.putExtra("nEmergencia", arrayUserInfos.get(0))
                intent.putExtra("nome", arrayUserInfos.get(1))
                intent.putExtra("img", arrayUserInfos.get(2))
                intent.putExtra("userBd", arrayUserInfos.get(3))

                startActivity(intent)
            }
        }
    }

    fun queryGetUserInfos() {

        ChamaDialog()
        val rootRef = databaseReference.child("usuarios")
        rootRef.orderByChild("email").equalTo(userMail).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()){

                        for (querySnapshot in dataSnapshot.children) {

                            var values: String= "nao"
                            values = querySnapshot.child("nEmergencia").value.toString()
                            arrayUserInfos.add(values)

                            values = querySnapshot.child("nome").value.toString()
                            arrayUserInfos.add(values)

                            values = querySnapshot.child("img").value.toString()
                            arrayUserInfos.add(values)

                            values = querySnapshot.key.toString()
                            arrayUserInfos.add(values)
                            userBd = values

                            values = querySnapshot.child("avaliacoes").value.toString()
                            arrayUserInfos.add(values)

                            values = querySnapshot.child("code").value.toString()
                            if (values.equals("nao")){
                                verificaCode()
                            }

                            /*
                            pos 0 - nEmergencia
                            pos 1 - nome
                            pos 2 - img
                            pos 3 - bd
                            pos 4 - avaliacoes

                             */

                            EncerraDialog()

                        }

                    } else {
                        showToast("Usuário não encontrado")
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    EncerraDialog()
                    // ...
                }
            })


    }

    fun verificaCode(){

        val layVerifyCode: ConstraintLayout = findViewById(R.id.layVerifyCode)
        val layMaps: ConstraintLayout = findViewById(R.id.layMapa)

        layMaps.visibility = View.GONE
        layVerifyCode.visibility = View.VISIBLE

        val etCode: EditText = findViewById(R.id.verifyCode_etCode)
        val btnVerify : Button = findViewById(R.id.verifyCode_btnVerifica)

        Log.d("teste", "chegou na verificaCode")
        btnVerify.setOnClickListener {
            if (etCode.text.isEmpty()){
                etCode.performClick()
                etCode.setError("Informe o código")
            } else {
                queryCode(etCode.text.toString())
            }
        }

    }

    //prpcura se o codigo realmente existe
    fun queryCode(code: String){

        ChamaDialog()
        Log.d("teste", "Chegou aqui")
        val rootRef = databaseReference.child("code")
        rootRef.orderByChild("code").equalTo(code).limitToFirst(1)
            //getInstance().reference.child("usuarios").orderByChild("email").equalTo(userMail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()){

                        for (querySnapshot in dataSnapshot.children) {

                            Log.d("teste", "Entrou na query")

                            val fiador = querySnapshot.child("fiador").getValue().toString()
                            databaseReference.child("usuarios").child(arrayUserInfos.get(3)).child("code").setValue("sim")
                            databaseReference.child("usuarios").child(arrayUserInfos.get(3)).child("fiador").setValue(fiador)

                            val key = querySnapshot.key.toString()
                            databaseReference.child("code").child(key).removeValue()

                            val layVerifyCode: ConstraintLayout = findViewById(R.id.layVerifyCode)
                            val layMaps: ConstraintLayout = findViewById(R.id.layMapa)

                            layMaps.visibility = View.VISIBLE
                            layVerifyCode.visibility = View.GONE

                            EncerraDialog()

                        }

                    } else {
                        showToast("Este código não existe.")
                        EncerraDialog()
                    }


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message
                    showToast("Este código não existe.")
                    EncerraDialog()
                    // ...
                }
            })

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (hasGpsPermission()){
            getUserLocation()
        }
    }


    // Métodos de localização
    //pega a posição do usuário e marca o circulo no mapa
    private fun getUserLocation() {

        if (hasGpsPermission()) {
            // 1
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mMap.isMyLocationEnabled = true

            // 2
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null.
                // 3

                if (location != null) {

                    //minha localização
                    lastLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f))

                    if (!userMail.equals("semLogin")){ //Se for semLogin então não coloca ele online pois os outros não poderão ve-lo também
                        updateUserStatus("online", "aindanao")
                        findUsersNerby(location.latitude, location.longitude)
                        findPlacesNerby(location.latitude, location.longitude)
                        findNewPlacesAsUserMoves(location.latitude, location.longitude)

                    } else {

                        findPlacesNerby(location.latitude, location.longitude)
                        findNewPlacesAsUserMoves(location.latitude, location.longitude)
                    }
                } else {
                  
                    //para aparelhos antigos não estava encontrando a localização
                    val toast = Toast.makeText(
                        this@MapsActivity,
                        "Não foi possível encontrar sua localização. Ligue o GPS.",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 100)
                    toast.show()
                }
            }

        } else {
            //Não tem permissão
            requestGpsPermission()
        }
    }

    //procedimentos para colocar o user online e offline
    //define status do user como online ou offline
    fun updateUserStatus(state: String, img: String){

        if (this@MapsActivity::lastLocation.isInitialized) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null.
                // 3

                if (location != null) {

                    lastLocation = location

                    val lat = lastLocation.latitude
                    val long = lastLocation.longitude
                    val statusUpDateRef = databaseReference.child("userOnline")

                    if (state.equals("online")) {

                        //coloca o user online
                        statusUpDateRef.child(userBd).child("latlong").setValue(lat + long)
                        statusUpDateRef.child(userBd).child("state").setValue(state)
                        statusUpDateRef.child(userBd).child("img").setValue(img)
                        statusUpDateRef.child(userBd).child("lat").setValue(lat)
                        statusUpDateRef.child(userBd).child("long").setValue(long)

                    } else {

                        //remove o user
                        statusUpDateRef.child(userBd).removeValue()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        updateUserStatus("offline", "aindanao")
    }

    override fun onDestroy() {
        super.onDestroy()
        updateUserStatus("offline", "aindanao")
    }

    //requisição para ligar o gps do user direto
    fun requestToOpenGpsLikeWaze (){
        val locationRequest : LocationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(30*1000)
        locationRequest.setFastestInterval(5*1000)
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener(object : OnCompleteListener<LocationSettingsResponse?> {
            override fun onComplete(@NonNull task: Task<LocationSettingsResponse?>) {
                try {
                    task.getResult(ApiException::class.java)
                } catch (exception: ApiException) {
                    when (exception.getStatusCode()) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                            val resolvable: ResolvableApiException =
                                exception as ResolvableApiException
                            resolvable.startResolutionForResult(this@MapsActivity, 100)
                        } catch (e: IntentSender.SendIntentException) {
                            Log.d("testeGps", e.message)
                        } catch (e: ClassCastException) {
                            Log.d("testeGps", e.message)
                        }
                    }
                }
            }
        })
    }











    fun generateCode(){

        val layMaps: ConstraintLayout = findViewById(R.id.layMapa)
        val layGenerateCode: ConstraintLayout = findViewById(R.id.layGenerateCode)

        layMaps.visibility = View.GONE
        layGenerateCode.visibility = View.VISIBLE

        val btnGerarCodigo: Button = findViewById(R.id.generateCode_btnGerar)
        val etCodigo: EditText = findViewById(R.id.generateCode_etCode)

        val btnFechar: Button = findViewById(R.id.generateCode_btnFechar)
        btnFechar.setOnClickListener {
            layMaps.visibility = View.VISIBLE
            layGenerateCode.visibility = View.GONE
        }

        btnGerarCodigo.setOnClickListener {

            if (userMail.equals("semLogin")){
                showToast("Você precisa fazer login para isso")
            } else {
                val code1 = rand(0, 9)
                val code2 = rand(0, 9)
                val code3 = rand(0, 9)
                val code4 = rand(0, 9)
                val code5 = rand(0, 9)

                val codeFinal = (code1.toString()+code2.toString()+code3.toString()+code4.toString()+code5.toString()).toString()
                val newCad: DatabaseReference = databaseReference.child("code").child("code").push()
                newCad.child("code").setValue(codeFinal)
                newCad.child("fiador").setValue(userBd)

                showToast("Codigo criado")
                etCodigo.setText(codeFinal)
            }
        }
    }

    fun rand(start: Int, end: Int): Int {
        require(start <= end) { "Illegal Argument" }
        return (start..end).random()
    }





    //procura usuarios proximos online
    fun findUsersNerby(lat: Double, long: Double) {

        var latlong = lat + long

        var startAtval = latlong-(0.01f*raioBusca)
        val endAtval = latlong+(0.01f*raioBusca)

        //nova regra de ouro
        //Por conta das características da latitude e longitude, nao podemos usar o mesmo valor para startAtVal (pois fica a esquerda) e endAtVal(que fica a direita).
        //O que ocorre é que itens que ficam a esquerda acumulam a soma de valores negativos de latitude e longitude. Já os que ficam em endVal pegam o valor negativo da longitude mas as vezes pega positivo de latitude. Isso dava resulltado no final.
        //Então agora o que vamos fazer.
        //a val dif armazena a diferença que encontramos entre startatVal e até onde faria 6km no mapa. Se alguim dia for mudar o raio (agora é 0.6) vai ter que mexer nisso.
        //entao basta adiconar essa diferença a startAtVal antes da busca para ele corrigir o erro. A verificar se isto também precisa ser feito para endAtAval.


        //startAtval = (dif+startAtval) //ajuste

        arrayTruckersNerby.clear()
        FirebaseDatabase.getInstance().reference.child("userOnline").orderByChild("latlong").startAt(startAtval)
            .endAt(endAtval)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            if (!querySnapshot.key.toString().equals(userBd)){
                                var values: String
                                var img: String
                                img = querySnapshot.child("img").value.toString()
                                values = querySnapshot.key.toString()
                                val latFriend = querySnapshot.child("lat").value.toString()
                                val longFriend = querySnapshot.child("long").value.toString()

                                //coloca o petFriend no mapa
                                placeTruckersInMap(img, values, latFriend.toDouble(), longFriend.toDouble())
                            }
                        }
                    } else {
                        showToast("Ninguém próximo de você.")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener
    }

    //coloca os caminhoneiros proximos no mapa
    //coloca os usuarios proximos online no mapa
    //também tem o click do botão que esconde e mostra os usuários no mapa.
    fun placeTruckersInMap(img: String, bdTrucker: String, lat: Double, long: Double){

        val latLng = LatLng(lat, long)

        //val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("trucker!?!"+bdTrucker+"!?!"+img+"!?!"+latLng))
        //arrayTruckersNerby.add(mark1)

        //mark1.tag=0

        //mMap.setOnMarkerClickListener(this)

        var img2 = "nao"
        var bitmapFinal : Bitmap?

        //pega o tamanho da tela para ajustar a qualquer celular na mesma proporção
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y

        //aqui é o tamanho total da imagem do user. Alterar aqui se quiser aumentar ou diminuir
        val withPercent  = ((12*width)/100).toInt()
        val heigthPercent : Int = ((7*height)/100).toInt()



        if (img.equals("nao")){  //se nao tem foto exibe somente o pin
            //img2 = "https://firebasestorage.googleapis.com/v0/b/store-2fa27.appspot.com/o/avatar.jpg?alt=media&token=7cc4587a-c99f-4017-b14b-09ecf7910729"

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("trucker!?!"+bdTrucker+"!?!"+img+"!?!"+latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholdersmall)))
            arrayTruckersNerby.add(mark1)

            mark1.tag=0

            mMap.setOnMarkerClickListener(this)


        } else {

            img2 = img

            Glide.with(this)
                .asBitmap()
                .load(img2)
                .apply(RequestOptions().override(withPercent, heigthPercent))
                .apply(RequestOptions.circleCropTransform())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {

                        val bit = BitmapFactory.decodeResource(
                            this@MapsActivity.getResources(),
                            R.drawable.placeholder
                        )

                        bitmapFinal = createUserBitmapFinalJustRound(
                            resource,
                            bit
                        )  //here we will insert the bitmap we got with the link in a placehold with white border.

                        val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("trucker!?!"+bdTrucker+"!?!"+img2+"!?!"+latLng)
                                .icon(
                                    BitmapDescriptorFactory.fromBitmap(bitmapFinal)
                                )
                        )
                        arrayTruckersNerby.add(mark1)

                        mark1.tag = 0

                        mMap.setOnMarkerClickListener(this@MapsActivity)

                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }
                })

        }

        //aqui esconde ou mostra os usuarios
        //OBS: SE DER ERRO QUANDO TIVER MAIS MARKERS OLHAR NO METODO GET MARK. PODE SER QUE TENHA QUE MUDAR O CODIGO LA DENTRO, POIS ESTA .get(0) e nao get(position)
        /*
        val btnShowHidePetFriends = findViewById<Button>(R.id.btnShowHidePetFriends)
        btnShowHidePetFriends.visibility = View.VISIBLE
        btnShowHidePetFriends.setOnClickListener {
            var cont=0
            while (cont<arrayPetFriendMarker.size){
                if (arrayPetFriendMarker.get(cont).isVisible){
                    arrayPetFriendMarker.get(cont).isVisible=false
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriendsnot, 0, 0)
                    makeToast("Usuários removidos do mapa")
                } else {
                    arrayPetFriendMarker.get(cont).isVisible=true
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriends, 0, 0)
                    makeToast("Usuários de volta ao mapa")
                }
                cont++
            }

        }
         */
    }












    //procura usuarios proximos online
    fun findPlacesNerby(lat: Double, long: Double) {

        var latlong = lat + long

        var startAtval = latlong-(0.01f*raioBusca)
        val endAtval = latlong+(0.01f*raioBusca)

        arrayPlacesNerby.clear()
        FirebaseDatabase.getInstance().reference.child("places").orderByChild("latlong").startAt(startAtval)
            .endAt(endAtval)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (dataSnapshot.exists()) {
                        for (querySnapshot in dataSnapshot.children) {

                            if (!querySnapshot.key.toString().equals(userBd)){
                                var values: String
                                values = querySnapshot.key.toString()
                                val latFriend = querySnapshot.child("lat").value.toString()
                                val longFriend = querySnapshot.child("long").value.toString()
                                val custo = querySnapshot.child("custo").value.toString()
                                val nota = querySnapshot.child("nota").value.toString()
                                val tipo = querySnapshot.child("tipo").value.toString()
                                val nome = querySnapshot.child("nome").value.toString()
                                val avaliacoes = querySnapshot.child("avaliacoes").value.toString()

                                //coloca o petFriend no mapa
                                placePlacesInMap(values, latFriend.toDouble(), longFriend.toDouble(), custo, nota, tipo, nome, avaliacoes)

                            }

                        }
                    } else {
                        showToast("Ninguém próximo de você.")
                    }

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Getting Post failed, log a message

                    // ...
                }
            })   //addValueEventListener

    }

    fun placePlacesInMap(bd: String, lat: Double, long: Double, custo: String, nota: String, tipo: String, nome: String, avaliacoes: String){

        val latLng = LatLng(lat, long)

        if (tipo.equals("Restaurante")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Borracharia")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Espaço público")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Hotel")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Oficina")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Parada CCR")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Posto gasolina")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        } else if (tipo.equals("Posto de saúde")){

            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome+"!?!"+avaliacoes).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayPlacesNerby.add(mark1)
            mark1.tag=0
            mMap.setOnMarkerClickListener(this)

        }


        /*
        var img2 = "nao"
        if (img.equals("nao")){
            img2 = "https://firebasestorage.googleapis.com/v0/b/farejadorapp.appspot.com/o/imgs_sistema%2Fimgusernoimg.png?alt=media&token=8a119c04-3295-4c5a-8071-dde1fe7849ea"
        } else {
            img2 = img
        }

        Glide.with(this)
            .asBitmap()
            .load(img2)
            .apply(RequestOptions().override(withPercent, heigthPercent))
            .apply(RequestOptions.circleCropTransform())
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                    val bit = BitmapFactory.decodeResource(
                        this@MapsActivity.getResources(),
                        R.drawable.placeholder
                    )

                    bitmapFinal = createUserBitmapFinalJustRound(resource, bit)  //here we will insert the bitmap we got with the link in a placehold with white border.

                    val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("petFriend!?!"+BdPetFriend+"!?!"+img+"!?!"+latLng).icon(
                        BitmapDescriptorFactory.fromBitmap(bitmapFinal)))
                    arrayPetFriendMarker.add(mark1)

                    mark1.tag=0

                    mMap.setOnMarkerClickListener (this@MapsActivity)

                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    // this is called when imageView is cleared on lifecycle call or for
                    // some other reason.
                    // if you are referencing the bitmap somewhere else too other than this imageView
                    // clear it here as you can no longer have the bitmap
                }
            })


         */

        //aqui esconde ou mostra os usuarios
        //OBS: SE DER ERRO QUANDO TIVER MAIS MARKERS OLHAR NO METODO GET MARK. PODE SER QUE TENHA QUE MUDAR O CODIGO LA DENTRO, POIS ESTA .get(0) e nao get(position)
        /*
        val btnShowHidePetFriends = findViewById<Button>(R.id.btnShowHidePetFriends)
        btnShowHidePetFriends.visibility = View.VISIBLE
        btnShowHidePetFriends.setOnClickListener {
            var cont=0
            while (cont<arrayPetFriendMarker.size){
                if (arrayPetFriendMarker.get(cont).isVisible){
                    arrayPetFriendMarker.get(cont).isVisible=false
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriendsnot, 0, 0)
                    makeToast("Usuários removidos do mapa")
                } else {
                    arrayPetFriendMarker.get(cont).isVisible=true
                    btnShowHidePetFriends.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.petfriends, 0, 0)
                    makeToast("Usuários de volta ao mapa")
                }
                cont++
            }

        }
         */

    }

    //a cada 3,5 min (5 km a 80km/h) refaz as queryes para exibir os novos lugares em volta
    fun findNewPlacesAsUserMoves(lat: Double, long: Double){

        //primeiro parametro é o tempo do timer em millis e o segundo é
        //o intervalo entre um timer e outro. Se for 0, começa direto o proximo.
        val timer = object : CountDownTimer(210000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                findPlacesNerby(lat, long)
                findUsersNerby(lat, long)
            }

        }
        timer.start()

    }








    //todos os cliques nos markers do mapa
    override fun onMarkerClick(p0: Marker?): Boolean {
        // Retrieve the data from the marker.

        val bd = p0?.title

        if (bd != null){

            if (bd.contains("trucker!?!")){

                val tokens = StringTokenizer(bd.toString(), "!?!")
                val descart = tokens.nextToken() // this will contain "trucker"
                val bdDoUser = tokens.nextToken() // this will contain "bd"
                val img = tokens.nextToken()

                //abrir popup
                openPopUp("Chamar este caminhoneiro?", "Você deseja abrir o whatsapp?", true, "Sim, abrir", "Não", "trucker", bdDoUser)

            } else if (bd.contains("place!?!")){

                val tokens = StringTokenizer(bd.toString(), "!?!")
                val discart = tokens.nextToken() // this will contain "place"
                val bdDoPlace = tokens.nextToken() // this will contain "bd"
                val discart2 = tokens.nextToken() // this will contain "latlong"
                val custo = tokens.nextToken() // this will contain "custo"
                val nota = tokens.nextToken() // this will contain "nota"
                val tipo = tokens.nextToken() // this will contain "tipo"
                val nome = tokens.nextToken() // this will contain "nome"
                val avaliacoes = tokens.nextToken() // this will contain "avaliacoes"

                openPopUpPlaces(nome, "texto aqui", true, "Avaliar", "Fechar", "places", bdDoPlace, custo, nota.toDouble(), tipo, avaliacoes)
                //("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo)

            }

        }

        //return false
        return true
    }







    //Abre a popup
    fun openPopUp (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String, bd: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_model,null)

        // Initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )



        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.btnReclamar)
        val buttonPopupS = view.findViewById<Button>(R.id.BtnRecebimento)
        val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.popupTitulo)
        val txtTexto = view.findViewById<TextView>(R.id.popupTexto)


        if (exibeBtnOpcoes){
            //vai exibir os botões com textos e esconder o btn ok
            buttonPopupOk.visibility = View.GONE
            //exibe e ajusta os textos dos botões
            buttonPopupN.text = btnNao
            buttonPopupS.text = btnSim

            // Set a click listener for popup's button widget
            buttonPopupN.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

        } else {

            //vai esconder os botões com textos e exibir o btn ok
            buttonPopupOk.visibility = View.VISIBLE
            //exibe e ajusta os textos dos botões
            buttonPopupN.visibility = View.GONE
            buttonPopupS.visibility = View.GONE


            buttonPopupOk.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

        }

        txtTitulo.text = titulo
        txtTexto.text = texto


        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layPai)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

        //aqui colocamos os ifs com cada call de cada vez que a popup for chamada
        if (call.equals("trucker")) {
            //abrir Whatsapp
            Toast.makeText(this, "Funcionou", Toast.LENGTH_SHORT).show()
        }

    }

    //aqui estão os clickes e os processos de salvar no banco de dados a avaliaçao
    fun openPopUpPlaces (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String, bd: String, custo: String, nota: Double, tipo: String, avaliacoes: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao
        //obs: titulo é o também o nome do lugar

        //EXIBIR POPUP
        // Initialize a new layout inflater instance
        val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(R.layout.popup_places,null)

        // Initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.MATCH_PARENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )



        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }


        // Get the widgets reference from custom view
        val buttonPopupN = view.findViewById<Button>(R.id.placesPopup_btnFechar)
        val buttonPopupS = view.findViewById<Button>(R.id.placesPopup_btnAvaliar)
        //val buttonPopupOk = view.findViewById<Button>(R.id.popupBtnOk)
        val txtTitulo = view.findViewById<TextView>(R.id.placesPopup_titulo)
        val txtTexto = view.findViewById<TextView>(R.id.placesPopup_texto)
        val txtCusto = view.findViewById<TextView>(R.id.placesPopup_custo)
        val imageView = view.findViewById<ImageView>(R.id.placesPopup_img)
        val star1 = view.findViewById<ImageView>(R.id.placesPopup_star1)
        val star2 = view.findViewById<ImageView>(R.id.placesPopup_star2)
        val star3 = view.findViewById<ImageView>(R.id.placesPopup_star3)
        val star4 = view.findViewById<ImageView>(R.id.placesPopup_star4)
        val star5 = view.findViewById<ImageView>(R.id.placesPopup_star5)



        if (exibeBtnOpcoes){
            //vai exibir os botões com textos e esconder o btn ok
            //buttonPopupOk.visibility = View.GONE
            //exibe e ajusta os textos dos botões
            buttonPopupN.text = btnNao
            buttonPopupS.text = btnSim

            // Set a click listener for popup's button widget
            buttonPopupN.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

        } else {

            //vai esconder os botões com textos e exibir o btn ok
            //buttonPopupOk.visibility = View.VISIBLE
            //exibe e ajusta os textos dos botões
            buttonPopupN.visibility = View.GONE
            buttonPopupS.visibility = View.GONE


            /*
            buttonPopupOk.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }
             */

        }

        txtTitulo.text = titulo
        txtTexto.text = texto

        //ajusta o valor
        /*
        if (custo.equals("1")){
            txtCusto.setText("$")
        } else if (custo.equals("2")){
            txtCusto.setText("$ $")
        } else if (custo.equals("3")){
            txtCusto.setText("$ $ $")
        } else if (custo.equals("4")){
            txtCusto.setText("$ $ $ $")
        } else {
            txtCusto.setText("$ $ $ $ $")
        }
         */


        //agora ajusta a nota e o custo
        val dolar1 = view.findViewById<ImageView>(R.id.placesPopup_custo_dolar1)
        val dolar2: ImageView = view.findViewById(R.id.placesPopup_custo_dolar2)
        val dolar3: ImageView = view.findViewById(R.id.placesPopup_custo_dolar3)
        val dolar4: ImageView = view.findViewById(R.id.placesPopup_custo_dolar4)
        val dolar5: ImageView = view.findViewById(R.id.placesPopup_custo_dolar5)
        val txtResumo: TextView = view.findViewById(R.id.placesPopup_custo)


        if (call.equals("avaliar")){
            star1.setImageResource(R.drawable.ic_star_border)
            star2.setImageResource(R.drawable.ic_star_border)
            star3.setImageResource(R.drawable.ic_star_border)
            star4.setImageResource(R.drawable.ic_star_border)
            star5.setImageResource(R.drawable.ic_star_border)

            //os listeners estão no final
            dolar1.setImageResource(R.drawable.dollarcinza)
            dolar2.setImageResource(R.drawable.dollarcinza)
            dolar3.setImageResource(R.drawable.dollarcinza)
            dolar4.setImageResource(R.drawable.dollarcinza)
            dolar5.setImageResource(R.drawable.dollarcinza)

        } else {
            if (nota == 5.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_gold)
                star5.setImageResource(R.drawable.ic_star_gold)
            } else if (nota > 4.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_gold)
                star5.setImageResource(R.drawable.ic_star_half)
            } else if (nota == 4.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_gold)
                star5.setImageResource(R.drawable.ic_star_border)
            } else if (nota > 3.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_half)
                star5.setImageResource(R.drawable.ic_star_half)
            } else if (nota == 3.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
            } else if (nota > 2.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_half)
                star4.setImageResource(R.drawable.ic_star_half)
                star5.setImageResource(R.drawable.ic_star_half)
            } else if (nota == 2.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_border)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
            } else if (nota > 1.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_half)
                star3.setImageResource(R.drawable.ic_star_half)
                star4.setImageResource(R.drawable.ic_star_half)
                star5.setImageResource(R.drawable.ic_star_half)
            } else if (nota == 1.0){
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_border)
                star3.setImageResource(R.drawable.ic_star_border)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
            } else if (nota > 0.0){
                star1.setImageResource(R.drawable.ic_star_half)
                star2.setImageResource(R.drawable.ic_star_half)
                star3.setImageResource(R.drawable.ic_star_half)
                star4.setImageResource(R.drawable.ic_star_half)
                star5.setImageResource(R.drawable.ic_star_half)
            } else if (nota == 0.0){
                star1.setImageResource(R.drawable.ic_star_border)
                star2.setImageResource(R.drawable.ic_star_border)
                star3.setImageResource(R.drawable.ic_star_border)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
            }


            var str:String = custo.replace("R$", "")
            str = str.replace(",", "").trim()
            str = str.replace(".", "").trim()
            val valorFormatado = str.toInt()


            if (valorFormatado > 400) {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
                dolar4.setImageResource(R.drawable.dollar)
                dolar5.setImageResource(R.drawable.dollar)

            } else if (valorFormatado > 200) {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
                dolar4.setImageResource(R.drawable.dollar)
            } else if (valorFormatado > 100) {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
            } else if (valorFormatado > 50) {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
            } else {
                dolar1.setImageResource(R.drawable.dollar)
            }

            if (custo.equals("R$0,00")){
                txtResumo.setText("Este lugar nunca foi avaliado.")
            } else {
                txtResumo.setText("Custo médio deste lugar é "+currencyTranslation(valorFormatado))
            }


        }



        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            //Fecha a janela ao clicar fora também
            popupWindow.dismiss()
        }

        //lay_root é o layout parent que vou colocar a popup
        val lay_root: ConstraintLayout = findViewById(R.id.layPai)

        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(lay_root)
        popupWindow.showAtLocation(
            lay_root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )

        //aqui colocamos os ifs com cada call de cada vez que a popup for chamada
        if (call.equals("places")) {
            //é a abertura normal, exibindo o lugar, a nota e o custo
            if (!userMail.equals("semLogin")){
                buttonPopupS.setOnClickListener {
                    //ao clicar em avaliar chama este mesmo método desta vez para avaliar.
                    openPopUpPlaces("Avaliar "+titulo, "Você está avaliando", true, "Avaliar", "Cancelar", "avaliar", bd, custo, nota, tipo, avaliacoes)
                    popupWindow.dismiss()
                }
            } else {
                showToast("Você precisa estar logado para avaliar.")
            }


        } else if (call.equals("avaliar")){

            var notaInformada = 0
            var custoInformado = 0
            //a avaliacao das estrelas será feita aqui mesmo pra aproveitar que os widgets ja foram carregados em memória
            star1.setOnClickListener {
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_border)
                star3.setImageResource(R.drawable.ic_star_border)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
                notaInformada=1
            }
            star2.setOnClickListener {
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_border)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
                notaInformada=2
            }
            star3.setOnClickListener {
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_border)
                star5.setImageResource(R.drawable.ic_star_border)
                notaInformada=3
            }
            star4.setOnClickListener {
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_gold)
                star5.setImageResource(R.drawable.ic_star_border)
                notaInformada=4
            }
            star5.setOnClickListener {
                star1.setImageResource(R.drawable.ic_star_gold)
                star2.setImageResource(R.drawable.ic_star_gold)
                star3.setImageResource(R.drawable.ic_star_gold)
                star4.setImageResource(R.drawable.ic_star_gold)
                star5.setImageResource(R.drawable.ic_star_gold)
                notaInformada=5
            }

            //listeners dos clicks
            dolar1.setOnClickListener {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollarcinza)
                dolar3.setImageResource(R.drawable.dollarcinza)
                dolar4.setImageResource(R.drawable.dollarcinza)
                dolar5.setImageResource(R.drawable.dollarcinza)
                custoInformado=25

                txtResumo.setText("Você gastou menos de R$ 50,00")
            }

            dolar2.setOnClickListener {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollarcinza)
                dolar4.setImageResource(R.drawable.dollarcinza)
                dolar5.setImageResource(R.drawable.dollarcinza)

                txtResumo.setText("Você gastou até R$ 50,00")
                custoInformado=50
            }

            dolar3.setOnClickListener {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
                dolar4.setImageResource(R.drawable.dollarcinza)
                dolar5.setImageResource(R.drawable.dollarcinza)

                txtResumo.setText("Você gastou até R$ 100,00")
                custoInformado=100
            }

            dolar4.setOnClickListener {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
                dolar4.setImageResource(R.drawable.dollar)
                dolar5.setImageResource(R.drawable.dollarcinza)

                txtResumo.setText("Você gastou até R$ 400,00")
                custoInformado=250
            }


            dolar5.setOnClickListener {
                dolar1.setImageResource(R.drawable.dollar)
                dolar2.setImageResource(R.drawable.dollar)
                dolar3.setImageResource(R.drawable.dollar)
                dolar4.setImageResource(R.drawable.dollar)
                dolar5.setImageResource(R.drawable.dollar)

                txtResumo.setText("Você gastou R$ 400,00 ou mais")
                custoInformado=400

            }

            buttonPopupS.setOnClickListener {
                //avaliar
                if (notaInformada==0){
                    showToast("Avalie a qualidade do lugar. Isso ajuda seus amigos.")
                } else if (custoInformado==0){
                    showToast("Avalie o custo. Isso ajuda seus amigos.")
                } else {


                    var str:String = custo.replace("R$", "")
                    str = str.replace(",", "").trim()
                    str = str.replace(".", "").trim()
                    val valorFormatado = str.toInt()

                    val novaNota = (notaInformada + nota) / avaliacoes.toInt()
                    var novoCusto = 0
                    if (avaliacoes.toInt()==0){
                        novoCusto = custoInformado
                    } else {
                        novoCusto  = (custoInformado + valorFormatado) / avaliacoes.toInt()
                    }

                    databaseReference.child("places").child(bd).child("nota").setValue(novaNota.toInt())
                    databaseReference.child("places").child(bd).child("custo").setValue(currencyTranslation(novoCusto))
                    databaseReference.child("places").child(bd).child("avaliacoes").setValue(avaliacoes.toInt()+1)
                    showToast("Você avaliou este lugar. Agora seus amigos poderão saber o que você achou.")
                    popupWindow.dismiss()
                }

            }

            //fun openPopUpPlaces (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String, bd: String, custo: String, nota: Double, tipo: String) {
        }

    }


    //corrige o valor informado pelo seekBar em dinheiro
    fun currencyTranslation(valorOriginal: Int): String{

        //passar o valor para string para poder ver o tamanho
        var valorString = valorOriginal.toString()
        valorString = valorString.trim()
        valorString.replace("R$", "")
        valorString.replace(".", "")
        valorString.replace(",", "")

        //na casa de menos de 100 mil
        //90.000 - 5 casas
        //entre 100 mil e 1 mi
        //100.000
        //entre 1 milhão pra cima
        //1.000,000
        if (valorString.length ==3){ //exemplo 002 222 012  fica 0,02 2,22 0,12

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()

        } else if (valorString.length == 4){ // 1234  fica 12,34

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()
        } else if (valorString.length==5){ //12345  fica 123,45

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            valorString = sb.toString()

        } else if (valorString.length==6){ //123456  fica 1.234,56

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(1, ".")
            valorString = sb.toString()

        } else if (valorString.length==7){ //1234567  fica 12.345,67

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(2, ".")
            valorString = sb.toString()

        } else if (valorString.length==8){ //12345678  fica 123.456,78

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(3, ".")
            valorString = sb.toString()

        }  else if (valorString.length==9){ //123456789  fica 1.234.567,89

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(4, ".")
            sb.insert(1, ".")
            valorString = sb.toString()

        }  else if (valorString.length==10){ //1234567890  fica 12.345.678,90

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(5, ".")
            sb.insert(2, ".")
            valorString = sb.toString()

        }  else if (valorString.length==11){ //12345678901  fica 123.456.789,01

            val sb: StringBuilder = StringBuilder(valorString)
            //coloca o ponto no lugar certo
            sb.insert(valorString.length - 2, ",")
            sb.insert(6, ".")
            sb.insert(3, ".")
            valorString = sb.toString()

        }

        valorString = "R$"+valorString
        return valorString

    }












    //
    fun addNewPlace(){

        val layMaps: ConstraintLayout = findViewById(R.id.layMapa)
        val layCad: ConstraintLayout = findViewById(R.id.layCadPlace)
        layMaps.visibility = View.GONE
        layCad.visibility = View.VISIBLE


        val btnCad: Button = findViewById(R.id.cadPlace_btnCad)


        val etEndereco: EditText = findViewById(R.id.cadPlace_etEndereco)
        val btnDigitarEndereco: Button = findViewById(R.id.cadPlace_btnNaoEstaNoLugar)
        btnDigitarEndereco.setOnClickListener {
            etEndereco.visibility = View.VISIBLE
            etEndereco.hint = "Digite o endereço aqui"
        }

        val btnPegarEndereco: Button = findViewById(R.id.cadPlace_btnEstaNoLugar)
        btnPegarEndereco.setOnClickListener {
            etEndereco.visibility = View.VISIBLE
            val latLong : LatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            etEndereco.setText(getAddress(latLong))
        }




        val spinner: Spinner = findViewById(R.id.cadPlace_spinner)
        var list_of_items = arrayOf(
            "Selecione o tipo",
            "Borracharia",
            "Espaço público",
            "Hotel",
            "Oficina",
            "Parada CCR",
            "Posto gasolina",
            "Posto de saúde",
            "Restaurante"
        )

        var tipo = "Selecione o tipo"

        //Adapter for spinner
        spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list_of_items)

        //item selected listener for spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tipo = list_of_items[position]

            }
        }



        btnCad.setOnClickListener {
            val etNome: EditText = findViewById(R.id.cadPlace_etNome)

            if (etNome.text.isEmpty()){
                etNome.requestFocus()
                etNome.setError("Informe o nome do lugar")
            } else if (etEndereco.text.isEmpty()){
                etEndereco.requestFocus()
                etEndereco.setError("Informe o endereço")
            } else if (tipo.equals("Selecione o tipo")){
                showToast("Informe o tipo de estabelecimento")
            } else {

                ChamaDialog()
                val newCad: String = databaseReference.child("places").push().key.toString()
                getLatLong(etEndereco.text.toString(), newCad) //aqui vai salvar as informações de localização neste mesmo bd
                databaseReference.child("places").child(newCad).child("avaliacoes").setValue(0)
                databaseReference.child("places").child(newCad).child("custo").setValue("R$0,00")
                databaseReference.child("places").child(newCad).child("nome").setValue(etNome.text.toString())
                databaseReference.child("places").child(newCad).child("nota").setValue(0)
                databaseReference.child("places").child(newCad).child("tipo").setValue(tipo)

                layCad.visibility = View.GONE
                layMaps.visibility = View.VISIBLE
                EncerraDialog()
                showToast("Pronto! O lugar foi adicionado")
                findPlacesNerby(lastLocation.latitude, lastLocation.longitude)

            }


        }


    }









    //métodos de busca de enderço a partir de Latitude e Longitude ou o contrário
    //private fun getAddress(latLng: LatLng): String {
    private fun getAddress(latLng: LatLng): String {
        // 1
        ChamaDialog()
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        //val address: Address?
        var addressText = ""

        val enderecoUser: MutableList<String> = ArrayList()

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {


                if (addresses[0].countryName == null){

                } else {
                    enderecoUser.add(addresses[0].countryName)
                }

                if (addresses[0].postalCode == null){

                } else {
                    enderecoUser.add(addresses[0].postalCode)
                }

                if (addresses[0].adminArea == null){ //estado

                } else {
                    enderecoUser.add(addresses[0].adminArea)
                }

                //este é diferente pq as vezes o estado vem em subadminarea e as vezes em locality. Entao ele testa
                if (addresses[0].locality == null) {
                    //mUserCidade = addresses[0].subAdminArea
                    enderecoUser.add(addresses[0].subAdminArea)
                } else {
                    //mUserCidade = addresses[0].locality
                    enderecoUser.add(addresses[0].locality)
                }

                if (addresses[0].subLocality == null){

                } else{
                    enderecoUser.add(addresses[0].subLocality)
                }

                if (addresses[0].subThoroughfare == null){

                } else {
                    enderecoUser.add(addresses[0].subThoroughfare)
                }

                if (addresses[0].thoroughfare == null){

                } else {
                    enderecoUser.add(addresses[0].thoroughfare)
                }



                var cont=0
                val size = enderecoUser.size-1  //pq o tamanho conta o 0. Entãodigamos, um array de tamanho 6 vai só até 5. Ai dava erro.
                while (cont<enderecoUser.size){
                    addressText = addressText+" "+enderecoUser.get(size-cont).toString()
                    cont++
                }
                /*
                /*
                array   pos 0 - cidade
                        pos 1 - estado
                        pos 2 - bairro
                        pos 3 - numero Casa
                        pos 4 - rua
                        pos 5 - cep
                 */
                addressText =
                    enderecoUser.get(4) + " nº " + enderecoUser.get(3) + ", " + enderecoUser.get(2) + ", " + enderecoUser.get(0) + " - " + enderecoUser.get(1)

                 */
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        EncerraDialog()
        return addressText

    }

    //pega a latitude e longitude a partir de um endereço
    private fun getLatLong (endereco: String, bd: String){

        ChamaDialog()
        val geocoder = Geocoder(this)
        //val addresses: List<Address>?
        //val address: Address?
        //var addressText = ""

        //Geocoder coder = new Geocoder(this);
        val address : List<Address>?
        //GeoPoint p1 = null;

        try {
            address = geocoder.getFromLocationName(endereco,1)

            if (address==null) {
                Toast.makeText(this, "Não foi possível encontrar a localização ainda. Aguarde", Toast.LENGTH_SHORT).show()
                val timer = object: CountDownTimer(40000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        getLatLong(endereco, bd)
                    }
                }
                timer.start()
            } else {
                var location: Address = address.get(0)
                //location.getLatitude();
                //location.getLongitude();

                databaseReference.child("places").child(bd).child("lat")
                    .setValue(location.latitude)
                databaseReference.child("places").child(bd).child("long")
                    .setValue(location.longitude)
                databaseReference.child("places").child(bd).child("latlong")
                    .setValue(location.latitude + location.longitude)

            }
        }catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        EncerraDialog()
    }




























    //permissões do gps
    //checa se o user deu permissão para achar sua localização
    fun hasGpsPermission(): Boolean{
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            //permissão concedida
            return true
        } else {
            return false
        }
    } //antigo permissaoSeguindoGooogle

    //pede em tempo real para o user liberar
    fun requestGpsPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {

        } else {
            //nothing
        }
    }

    //faz requisição de permissão no inicio da activity
    fun requestPermission():Boolean{
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                return false
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    171)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
                return false
            }

        } else {
            //permission granted
            return true
        }
    }

    fun requestThePermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            171)

    }

    //resultado da resposta do user
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            171 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Toast.makeText(this, "Permissão concedida. Reiniciando a aplicação para achar sua localização.", Toast.LENGTH_SHORT).show()
                    finish()
                    //getUserLocation(raioUser, 0) REFORMA

                } else {
                    Toast.makeText(this, "Permissão não concedida. Não podemos acessar sua localização", Toast.LENGTH_SHORT).show()
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    /*
                    //colocando clicks para chamar a permissão em tudo
                    val btnMenu: ImageView = findViewById(com.google.android.gms.location.R.id.lay_Maps_MenuBtn)
                    btnMenu.setOnClickListener {
                        requestTheDamnPermission()
                    }
                    val btnInserirEndereco : Button = findViewById(com.google.android.gms.location.R.id.btnInserirEndereco)
                    btnInserirEndereco.setOnClickListener {
                        requestTheDamnPermission()
                    }
                    val btnShowHideLista : Button = findViewById(com.google.android.gms.location.R.id.btnShowHideLista)
                    btnShowHideLista.visibility = View.GONE
                     */ //apagar depois

                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun showToast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //ajusta a imagem para o marker com imagem
    private fun createUserBitmapFinalJustRound(bitmapImgUser: Bitmap?, bitmapPlaceHolder: Bitmap?): Bitmap? {

        //vamos ajustar o fundo branco ao tamanho que colocamos na imagem do user
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        //val height: Int = size.y

        val withPercent  = ((18*width)/100).toFloat()   //um pouco maior do que a imagem do user
        val differenceAdjust = ((8*withPercent)/100).toFloat()

        //ajusta ao tamanho que queremos
        val newPlaceHolder = scaleDown(bitmapPlaceHolder!!, withPercent, true)

        //agora colocamos a imagem do bolão ao fundo e a imagem do user a frente
        val bmOverlay = Bitmap.createBitmap(newPlaceHolder!!.getWidth(), newPlaceHolder.getHeight(), newPlaceHolder.getConfig())
        val canvas = Canvas(bmOverlay)
        val customMatrix = Matrix()
        customMatrix.setTranslate(differenceAdjust, differenceAdjust)
        canvas.drawBitmap(newPlaceHolder!!, Matrix(), null)
        canvas.drawBitmap(bitmapImgUser!!, customMatrix, null)

        return bmOverlay

    }

    fun scaleDown(realImage: Bitmap, maxImageSize: Float, filter: Boolean): Bitmap? {
        val ratio = Math.min(maxImageSize / realImage.width,maxImageSize / realImage.height)
        val width = Math.round(ratio * realImage.width)
        val height = Math.round(ratio * realImage.height)
        return Bitmap.createScaledBitmap(realImage, width, height, filter)
    }

    fun ChamaDialog() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ) //este serve para bloquear cliques que pdoeriam dar erros
        val layout = findViewById<RelativeLayout>(R.id.LayoutProgressBar)
        layout.visibility = View.VISIBLE
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.visibility = View.VISIBLE
    }

    //este método torna invisivel um layout e encerra o dialogbar spinner.
    fun EncerraDialog() {
        val layout = findViewById<RelativeLayout>(R.id.LayoutProgressBar)
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        layout.visibility = View.GONE
        spinner.visibility = View.GONE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) //libera os clicks
    }

}
