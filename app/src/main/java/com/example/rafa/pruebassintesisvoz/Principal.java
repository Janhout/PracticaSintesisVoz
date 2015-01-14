package com.example.rafa.pruebassintesisvoz;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.rafa.pruebassintesisvoz.bot.ChatterBot;
import com.example.rafa.pruebassintesisvoz.bot.ChatterBotFactory;
import com.example.rafa.pruebassintesisvoz.bot.ChatterBotSession;
import com.example.rafa.pruebassintesisvoz.bot.ChatterBotType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;


public class Principal extends Activity implements TextToSpeech.OnInitListener{

    private boolean reproductor;
    private TextToSpeech tts;
    private EditText et;
    private RadioButton rb_esp;
    private RadioButton rb_eng;

    private ChatterBotFactory factory;
    private ChatterBot bot1;
    private ChatterBotSession bot1session;

    private final int CTE = 1;
    private final int CTR = 2;

    private float pitch;
    private float rate;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CTE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(getApplicationContext(), this);
            } else {
                Intent intent = new Intent(); //esto es para instalar el sintetizadro....no se sabe si se va a instalar, ya que se puede cancelar
                intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(intent);
            }
        }else if(requestCode == CTR){
            if(resultCode == Activity.RESULT_OK && data != null){
                ArrayList<String> textos = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                et.setText(textos.get(0));
                reproducir(null);
            }else {
                Toast.makeText(this, getString(R.string.error_reconocimiento), Toast.LENGTH_SHORT).show();
                et.setText(getString(R.string.error_reconocimiento));
                if (tts != null) {
                    tts.speak(et.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                } else {
                    Toast.makeText(Principal.this, getString(R.string.error_sintesis2), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(comprobarConexion() && comprobarIntenet()) {
            setContentView(R.layout.activity_principal);
            initComponents();
        }else{
            Toast.makeText(this, getString(R.string.error_internet), Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS){
            reproductor = true;
            cambiarIdioma(null);
        } else {
            reproductor = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        comprobarTTS();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public void cambiarIdioma(View v){
        if(tts != null) {
            if (rb_esp.isChecked()) {
                tts.setLanguage(new Locale("es", "ES"));
            } else {
                tts.setLanguage(new Locale("en", "EN"));
            }
        }
    }

    public void cambiarPitch(View v){
        if(tts != null) {
            Button b = (Button)v;
            if(b.getText().equals(getString(R.string.aumentar))){
                if(pitch < 2.0f) {
                    pitch = pitch + 0.1f;
                }
            }else{
                if(pitch > 0.0f) {
                    pitch = pitch - 0.1f;
                }
            }
            tts.setPitch(pitch);
        }
    }

    public void cambiarRate(View v){
        if(tts != null) {
            Button b = (Button)v;
            if(b.getText().equals(getString(R.string.aumentar))){
                if(rate < 2.0f){
                    rate = rate + 0.1f;
                }
            }else{
                if(rate > 0.0f) {
                    rate = rate - 0.1f;
                }
            }
            tts.setSpeechRate(rate);
        }
    }

    private boolean comprobarConexion(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private boolean comprobarIntenet(){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        return false;
    }

    private void comprobarTTS(){
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(intent, CTE);
    }

    public void hablar(View view){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        String idioma;
        if(rb_esp.isChecked()){
            idioma = getString(R.string.esp);
        }else{
            idioma = getString(R.string.eng);
        }
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, idioma);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.habla));
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);//el grabador estará abierto durante 5 segundos

        startActivityForResult(i, CTR);
    }

    private void initComponents(){
        reproductor = false;
        et = (EditText)findViewById(R.id.et);
        rb_eng = (RadioButton)findViewById(R.id.rb_eng);
        rb_esp = (RadioButton)findViewById(R.id.rb_esp);

        factory = new ChatterBotFactory();
        try {
            bot1 = factory.create(ChatterBotType.CLEVERBOT);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_bot) + "\n" + e.toString(), Toast.LENGTH_SHORT).show();
            bot1 = null;
            this.finish();
        }
        if(bot1 != null) {
            bot1session = bot1.createSession();
            comprobarTTS();
        }
        pitch = 1.0f;
        rate = 1.0f;
    }

    public void limpiarEt(View v){
        ((EditText)v).setText("");
    }

    public void reproducir(View v){
            HiloBot hb = new HiloBot(et.getText().toString());
            hb.execute();
    }



    public class HiloBot extends AsyncTask<Void, Void, String>{

        ProgressDialog progreso;
        String texto;

        public HiloBot(String t) {
            progreso = new ProgressDialog(Principal.this);
            progreso.setMessage(getString(R.string.pensando));
            progreso.setCancelable(false);
            this.texto = t;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progreso.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            String respuesta;
            try {
                respuesta = bot1session.think(texto);
            } catch (Exception e) {
                respuesta = getString(R.string.no_internet);
            }
            return respuesta;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            et.setText(s);
            if(!s.equals(getString(R.string.no_internet))) {
                if (reproductor) {
                    if (tts != null) {
                        tts.speak(s, TextToSpeech.QUEUE_ADD, null);
                    } else {
                        Toast.makeText(Principal.this, getString(R.string.error_sintesis2), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Principal.this, getString(R.string.error_sintesis), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(Principal.this, getString(R.string.error_bot), Toast.LENGTH_SHORT).show();
            }
            progreso.dismiss();
        }
    }
}