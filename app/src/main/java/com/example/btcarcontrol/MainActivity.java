package com.example.btcarcontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_BT = 41;
    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream output;
    private Spinner spinner;
    private TextView status;
    private final ArrayList<BluetoothDevice> paired = new ArrayList<>();
    private volatile boolean connected = false;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); adapter = BluetoothAdapter.getDefaultAdapter(); buildUi(); refresh();
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(12,8,12,8); root.setBackgroundColor(Color.rgb(18,24,35));
        TextView title = label("BT CAR CONTROL"); title.setTextSize(22); title.setTypeface(null,Typeface.BOLD); root.addView(title,new LinearLayout.LayoutParams(-1,38));
        LinearLayout connectRow = new LinearLayout(this); connectRow.setGravity(Gravity.CENTER_VERTICAL);
        spinner = new Spinner(this); connectRow.addView(spinner,new LinearLayout.LayoutParams(0,48,1));
        Button refresh = btn("↻",55,Color.DKGRAY); refresh.setOnClickListener(v->refresh()); connectRow.addView(refresh);
        Button connect = btn("CONECTAR",125,Color.rgb(0,145,110)); connect.setOnClickListener(v->connect()); connectRow.addView(connect); root.addView(connectRow);
        status = label("Empareja el HC-05 desde Ajustes Bluetooth"); root.addView(status,new LinearLayout.LayoutParams(-1,28));
        LinearLayout area = new LinearLayout(this); area.setGravity(Gravity.CENTER); root.addView(area,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout joy = new LinearLayout(this); joy.setOrientation(1); joy.setGravity(Gravity.CENTER); area.addView(joy,new LinearLayout.LayoutParams(0,-1,1)); joy.addView(label("JOYSTICK"));
        Button up=btn("▲",76,0xff2a6fb4), down=btn("▼",76,0xff2a6fb4), left=btn("◀",76,0xff2a6fb4), right=btn("▶",76,0xff2a6fb4), stop=btn("STOP",76,0xffbe3237);
        LinearLayout row=row(); row.addView(blank(76)); row.addView(up); row.addView(blank(76)); joy.addView(row);
        row=row(); row.addView(left); row.addView(stop); row.addView(right); joy.addView(row);
        row=row(); row.addView(blank(76)); row.addView(down); row.addView(blank(76)); joy.addView(row);
        hold(up,"F"); hold(down,"B"); hold(left,"L"); hold(right,"R"); stop.setOnClickListener(v->send("S"));
        LinearLayout dirs = new LinearLayout(this); dirs.setOrientation(1); dirs.setGravity(Gravity.CENTER); area.addView(dirs,new LinearLayout.LayoutParams(0,-1,1)); dirs.addView(label("DIRECCIÓN"));
        Button f=btn("↑ ADELANTE",170,0xff2a6fb4), b=btn("↓ ATRÁS",170,0xff2a6fb4), l=btn("← IZQUIERDA",140,0xff2a6fb4), r=btn("DERECHA →",140,0xff2a6fb4), s=btn("■ STOP",190,0xffbe3237);
        row=row(); row.addView(f); dirs.addView(row); row=row(); row.addView(l); row.addView(r); dirs.addView(row); row=row(); row.addView(b); dirs.addView(row); dirs.addView(s);
        hold(f,"F"); hold(b,"B"); hold(l,"L"); hold(r,"R"); s.setOnClickListener(v->send("S")); setContentView(root);
    }
    private TextView label(String text){ TextView t=new TextView(this); t.setText(text); t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); return t; }
    private LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setGravity(Gravity.CENTER); return r; }
    private View blank(int w){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w,58)); return v; }
    private Button btn(String text,int width,int color){ Button b=new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false); b.setTypeface(null,Typeface.BOLD); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color)); b.setPadding(4,2,4,2); b.setLayoutParams(new LinearLayout.LayoutParams(width,58)); return b; }
    private void hold(Button b,String c){ b.setOnTouchListener((v,e)->{ if(e.getAction()==MotionEvent.ACTION_DOWN){send(c);return true;} if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){send("S");return true;} return true; }); }
    private boolean permission(){ if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT);return false;} return true; }
    private void refresh(){ if(adapter==null){status.setText("Este teléfono no admite Bluetooth");return;} if(!permission())return; if(!adapter.isEnabled()){status.setText("Activa Bluetooth en Ajustes");return;} paired.clear(); ArrayList<String> names=new ArrayList<>(); try{ Set<BluetoothDevice> ds=adapter.getBondedDevices(); for(BluetoothDevice d:ds){paired.add(d);names.add((d.getName()==null?"Bluetooth":d.getName())+" ("+d.getAddress()+")");} }catch(SecurityException e){status.setText("Permiso Bluetooth requerido");} if(names.isEmpty())names.add("No hay dispositivos emparejados"); spinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names)); status.setText(paired.isEmpty()?"Empareja el HC-05 en Ajustes Bluetooth":"Elige HC-05 y pulsa CONECTAR"); }
    private void connect(){ if(!permission())return; if(adapter==null||!adapter.isEnabled()){status.setText("Activa Bluetooth primero");return;} int i=spinner.getSelectedItemPosition(); if(i<0||i>=paired.size()){status.setText("No hay dispositivo seleccionado");return;} BluetoothDevice d=paired.get(i); status.setText("Conectando..."); new Thread(()->{ BluetoothSocket candidate=null; try{candidate=d.createRfcommSocketToServiceRecord(SPP);adapter.cancelDiscovery();candidate.connect();socket=candidate;output=socket.getOutputStream();connected=true;runOnUiThread(()->status.setText("Conectado: "+d.getAddress()));}catch(IOException|SecurityException e){connected=false;if(candidate!=null)try{candidate.close();}catch(IOException ignored){}runOnUiThread(()->status.setText("No conectó: revisa HC-05 y emparejamiento"));}}).start(); }
    private void send(String c){ if(!connected||output==null)return; try{output.write(c.getBytes("US-ASCII"));output.flush();}catch(IOException e){connected=false;runOnUiThread(()->status.setText("Conexión perdida"));} }
    @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){super.onRequestPermissionsResult(req,p,g);if(req==REQUEST_BT&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)refresh();else if(req==REQUEST_BT)status.setText("Permite Bluetooth para conectar");}
    @Override protected void onDestroy(){super.onDestroy();connected=false;try{if(socket!=null)socket.close();}catch(IOException ignored){}}
}
