package com.bino.bino1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.NonNull
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        databaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        userBd = "teste"  //este valor vai vir de login

        //recupera o email do usuário
        userMail = intent.getStringExtra("email")

        Log.d("teste", "o valor de usermail é "+userMail)

        if (!requestPermission()){
            requestThePermission()
        } else {

        }


        requestToOpenGpsLikeWaze()  //liga o GPS do user

        firstMeths()

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
        
        Log.d("teste", "chegou em update")

        if (this@MapsActivity::lastLocation.isInitialized) {

            Log.d("teste", "lastlocation ja havi sido inicializada")

            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                // Got last known location. In some rare situations this can be null.
                // 3

                if (location != null) {

                    lastLocation = location

                    val lat = lastLocation.latitude
                    val long = lastLocation.longitude
                    val statusUpDateRef = databaseReference.child("userOnline")

                    Log.d("teste", "lastlocation ja havi sido inicializada")
                    if (state.equals("online")) {

                        //coloca o user online
                        statusUpDateRef.child(userBd).child("latlong").setValue(lat + long)
                        statusUpDateRef.child(userBd).child("state").setValue(state)
                        //statusUpDateRef.child(userBd).child("img").setValue(img)
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

        val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("trucker!?!"+bdTrucker+"!?!"+latLng))
        arrayTruckersNerby.add(mark1)

        mark1.tag=0

        mMap.setOnMarkerClickListener(this)

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

                                //coloca o petFriend no mapa
                                placePlacesInMap(values, latFriend.toDouble(), longFriend.toDouble(), custo, nota, tipo, nome)

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

    fun placePlacesInMap(bd: String, lat: Double, long: Double, custo: String, nota: String, tipo: String, nome: String){

        val latLng = LatLng(lat, long)

        if (tipo.equals("restaurante")){
            val mark1 = mMap.addMarker(MarkerOptions().position(latLng).title("place!?!"+bd+"!?!"+latLng+"!?!"+custo+"!?!"+nota+"!?!"+tipo+"!?!"+nome).icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurante)))
            arrayTruckersNerby.add(mark1)
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
                //val img = tokens.nextToken()

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

                openPopUpPlaces(nome, "texto aqui", true, "Avaliar", "Fechar", "places", bdDoPlace, custo, nota.toDouble(), tipo)
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

    fun openPopUpPlaces (titulo: String, texto:String, exibeBtnOpcoes:Boolean, btnSim: String, btnNao: String, call: String, bd: String, custo: String, nota: Double, tipo: String) {
        //exibeBtnOpcoes - se for não, vai exibir apenas o botão com OK, sem opção. Senão, exibe dois botões e pega os textos deles de btnSim e btnNao

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

        //agora ajusta a nota
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
        if (call.equals("avaliar")) {
            //abrir Whatsapp
            Toast.makeText(this, "Avaliar", Toast.LENGTH_SHORT).show()
        }

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
