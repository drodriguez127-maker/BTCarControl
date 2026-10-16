package com.ejemplo.btcarcontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ_BT = 41;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream output;
    private TextView status, lastCommand;
    private Button connectButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String activeCommand = "S";
    private boolean closing = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); return t;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(8), dp(16), dp(8)); root.setBackgroundColor(Color.rgb(7,11,18));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = text("⚡  BT CAR CONTROL", 22, Color.rgb(53,167,255)); title.setTypeface(null, Typeface.BOLD); top.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        status = text("● DESCONECTADO", 13, Color.rgb(255,105,105)); top.addView(status, new LinearLayout.LayoutParams(dp(150), dp(44)));
        connectButton = new Button(this); connectButton.setText("CONECTAR HC-05"); connectButton.setTextSize(11); connectButton.setOnClickListener(v -> connectOrDisconnect()); top.addView(connectButton, new LinearLayout.LayoutParams(dp(150), dp(44)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout arena = new LinearLayout(this); arena.setOrientation(LinearLayout.HORIZONTAL); arena.setGravity(Gravity.CENTER_VERTICAL); arena.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout left = new LinearLayout(this); left.setGravity(Gravity.CENTER); left.setOrientation(LinearLayout.VERTICAL);
        TextView jLabel = text("JOYSTICK DIGITAL", 13, Color.rgb(154,183,214)); jLabel.setTypeface(null, Typeface.BOLD); left.addView(jLabel, new LinearLayout.LayoutParams(-1, dp(26)));
        Joystick joystick = new Joystick(); left.addView(joystick, new LinearLayout.LayoutParams(-1, 0, 1));
        arena.addView(left, new LinearLayout.LayoutParams(0, -1, 1.05f));

        LinearLayout right = new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.CENTER); right.setPadding(dp(10), 0, dp(10), 0);
        TextView bLabel = text("CONTROLES", 13, Color.rgb(154,183,214)); bLabel.setTypeface(null, Typeface.BOLD); right.addView(bLabel, new LinearLayout.LayoutParams(-1, dp(26)));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER); row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(64), 1));
        row.addView(directionButton("▲", "ADELANTE", "F", Color.rgb(76,210,128)), new LinearLayout.LayoutParams(0, dp(64), 1.4f));
        row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(64), 1)); right.addView(row, new LinearLayout.LayoutParams(-1, dp(68)));
        LinearLayout mid = new LinearLayout(this); mid.setGravity(Gravity.CENTER); mid.setOrientation(LinearLayout.HORIZONTAL);
        mid.addView(directionButton("◀", "IZQUIERDA", "L", Color.rgb(255,112,104)), new LinearLayout.LayoutParams(0, dp(72), 1));
        Button stop = directionButton("■", "STOP", "S", Color.WHITE); mid.addView(stop, new LinearLayout.LayoutParams(0, dp(72), 1));
        mid.addView(directionButton("▶", "DERECHA", "R", Color.rgb(255,211,80)), new LinearLayout.LayoutParams(0, dp(72), 1)); right.addView(mid, new LinearLayout.LayoutParams(-1, dp(76)));
        LinearLayout bottom = new LinearLayout(this); bottom.setGravity(Gravity.CENTER); bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.addView(new View(this), new LinearLayout.LayoutParams(0, dp(64), 1));
        bottom.addView(directionButton("▼", "ATRÁS", "B", Color.rgb(83,168,255)), new LinearLayout.LayoutParams(0, dp(64), 1.4f));
        bottom.addView(new View(this), new LinearLayout.LayoutParams(0, dp(64), 1)); right.addView(bottom, new LinearLayout.LayoutParams(-1, dp(68)));
        arena.addView(right, new LinearLayout.LayoutParams(0, -1, 1)); root.addView(arena, new LinearLayout.LayoutParams(-1, 0, 1));
        lastCommand = text("LISTO · COMANDO: S (STOP)", 13, Color.rgb(53,167,255)); root.addView(lastCommand, new LinearLayout.LayoutParams(-1, dp(28)));
        TextView foot = text("HC-05  •  TX/RX  •  Mantén pulsado para moverte", 10, Color.rgb(113,137,163)); root.addView(foot, new LinearLayout.LayoutParams(-1, dp(20)));
        setContentView(root);
    }

    private Button directionButton(String icon, String label, String cmd, int color) {
        Button b = new Button(this); b.setText(icon + "\n" + label); b.setTextSize(12); b.setTextColor(color); b.setAllCaps(false); b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(20,31,47)));
        b.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) { send(cmd); v.setAlpha(0.65f); return true; }
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) { send("S"); v.setAlpha(1f); return true; }
            return true;
        }); return b;
    }

    private void connectOrDisconnect() {
        if (socket != null && socket.isConnected()) { disconnect(); return; }
        if (adapter == null) { toast("Este dispositivo no tiene Bluetooth."); return; }
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQ_BT); return;
        }
        if (!adapter.isEnabled()) { toast("Activa Bluetooth y vuelve a intentar."); return; }
        try {
            Set<BluetoothDevice> paired = adapter.getBondedDevices(); ArrayList<BluetoothDevice> list = new ArrayList<>();
            for (BluetoothDevice d : paired) list.add(d);
            if (list.isEmpty()) { toast("Primero empareja el HC-05 en Ajustes Bluetooth (PIN común: 1234 o 0000)."); return; }
            String[] names = new String[list.size()];
            for (int i=0;i<list.size();i++) { BluetoothDevice d=list.get(i); String n=d.getName(); names[i]=(n==null?"Dispositivo":n)+"\n"+d.getAddress(); }
            new android.app.AlertDialog.Builder(this).setTitle("Elige tu módulo Bluetooth").setItems(names, (dialog, which) -> connect(list.get(which))).setNegativeButton("Cancelar", null).show();
        } catch (SecurityException e) { toast("Permiso Bluetooth no concedido."); }
    }
    private void connect(BluetoothDevice device) {
        status.setText("● CONECTANDO…"); status.setTextColor(Color.YELLOW); connectButton.setEnabled(false);
        new Thread(() -> {
            BluetoothSocket s = null;
            try {
                if (adapter != null) adapter.cancelDiscovery();
                s = device.createRfcommSocketToServiceRecord(SPP_UUID); s.connect(); OutputStream out=s.getOutputStream();
                BluetoothSocket finalS=s;
                runOnUiThread(() -> { socket=finalS; output=out; status.setText("● CONECTADO"); status.setTextColor(Color.rgb(76,210,128)); connectButton.setText("DESCONECTAR"); connectButton.setEnabled(true); send("S"); toast("Bluetooth conectado"); });
            } catch (Exception ex) {
                try { if (s!=null) s.close(); } catch (IOException ignored) {}
                runOnUiThread(() -> { status.setText("● ERROR DE CONEXIÓN"); status.setTextColor(Color.rgb(255,105,105)); connectButton.setEnabled(true); toast("No se pudo conectar. Revisa que el HC-05 esté emparejado y encendido."); });
            }
        }).start();
    }
    private synchronized void send(String cmd) {
        activeCommand=cmd;
        if (lastCommand != null) lastCommand.setText("COMANDO: " + commandName(cmd) + "  ·  " + cmd);
        try { if (output != null && socket != null && socket.isConnected()) { output.write((cmd).getBytes("US-ASCII")); output.flush(); } }
        catch (Exception e) { runOnUiThread(() -> { status.setText("● CONEXIÓN PERDIDA"); status.setTextColor(Color.RED); }); }
    }
    private String commandName(String c) { switch(c) {case "F":return "ADELANTE";case "B":return "ATRÁS";case "L":return "IZQUIERDA";case "R":return "DERECHA";default:return "STOP";} }
    private void disconnect() { send("S"); try { if(output!=null) output.close(); if(socket!=null) socket.close(); } catch(Exception ignored){} output=null; socket=null; status.setText("● DESCONECTADO"); status.setTextColor(Color.rgb(255,105,105)); connectButton.setText("CONECTAR HC-05"); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) { super.onRequestPermissionsResult(requestCode, permissions, results); if(requestCode==REQ_BT) { boolean ok=true; for(int r:results) if(r!=PackageManager.PERMISSION_GRANTED) ok=false; if(ok) connectOrDisconnect(); else toast("Se necesita permiso Bluetooth para conectar con el HC-05."); } }
    @Override protected void onPause() { super.onPause(); send("S"); }
    @Override protected void onDestroy() { closing=true; disconnect(); handler.removeCallbacksAndMessages(null); super.onDestroy(); }

    private class Joystick extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); float cx,cy,r,knobX,knobY; boolean dragging=false; String direction="S";
        Joystick(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c); cx=getWidth()/2f;cy=getHeight()/2f;r=Math.min(getWidth(),getHeight())*.39f;if(!dragging){knobX=cx;knobY=cy;}
            p.setShader(new RadialGradient(cx,cy,r*.2f,new int[]{Color.rgb(33,68,106),Color.rgb(15,25,39),Color.rgb(8,13,21)},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,r,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(43,111,170));c.drawCircle(cx,cy,r,p);p.setStrokeWidth(dp(1));p.setColor(Color.rgb(41,56,74));c.drawLine(cx-r*.7f,cy,cx+r*.7f,cy,p);c.drawLine(cx,cy-r*.7f,cx,cy+r*.7f,p);
            p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(knobX,knobY,r*.42f,new int[]{Color.rgb(66,170,255),Color.rgb(12,94,181),Color.rgb(7,47,91)},null,Shader.TileMode.CLAMP));c.drawCircle(knobX,knobY,r*.39f,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(71,174,255));c.drawCircle(knobX,knobY,r*.39f,p);p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(18));c.drawText("▲",cx,cy-r*.68f,p);c.drawText("▼",cx,cy+r*.79f,p);c.drawText("◀",cx-r*.69f,cy+dp(6),p);c.drawText("▶",cx+r*.69f,cy+dp(6),p);
        }
        @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){dragging=true;float dx=x-cx,dy=y-cy;float max=r*.62f;float len=(float)Math.sqrt(dx*dx+dy*dy);if(len>max){dx=dx/len*max;dy=dy/len*max;}knobX=cx+dx;knobY=cy+dy;String d="S";if(Math.abs(dx)>Math.abs(dy)){if(Math.abs(dx)>r*.18f)d=dx<0?"L":"R";}else{if(Math.abs(dy)>r*.18f)d=dy<0?"F":"B";}if(!d.equals(direction)){direction=d;send(d);}invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){dragging=false;direction="S";knobX=cx;knobY=cy;send("S");invalidate();return true;}return true;}
    }
}
