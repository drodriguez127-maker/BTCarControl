package com.example.btcarcontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final int REQUEST_BT = 41;
    private static final UUID SPP = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    );

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream output;
    private Spinner devices;
    private TextView status, lastCommand;
    private final ArrayList<BluetoothDevice> paired = new ArrayList<>();
    private volatile boolean connected = false;

    private final int BG = Color.rgb(5, 12, 19);
    private final int PANEL = Color.rgb(10, 22, 32);
    private final int BLUE = Color.rgb(0, 174, 255);
    private final int GREEN = Color.rgb(57, 255, 20);
    private final int RED = Color.rgb(255, 55, 65);
    private final int YELLOW = Color.rgb(255, 190, 0);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUI();
        refreshDevices();
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(20);
        d.setStroke(2, stroke);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER);
        return l;
    }

    private void buildUI() {
        LinearLayout root = column();
        root.setPadding(12, 8, 12, 8);
        root.setBackgroundColor(BG);

        // Encabezado
        LinearLayout header = row();
        header.setBackground(shape(PANEL, 0xff12354a));

        TextView title = text("⚡ BT CAR CONTROL", 23, Color.WHITE);
        header.addView(title, new LinearLayout.LayoutParams(0, 55, 1));

        status = text("● DESCONECTADO", 14, RED);
        header.addView(status,
            new LinearLayout.LayoutParams(175, 55));

        root.addView(header,
            new LinearLayout.LayoutParams(-1, 55));

        // Conexión Bluetooth
        LinearLayout connectRow = row();
        connectRow.setPadding(0, 7, 0, 7);

        devices = new Spinner(this);
        devices.setBackground(shape(PANEL, BLUE));
        connectRow.addView(devices,
            new LinearLayout.LayoutParams(0, 48, 1));

        Button refresh = button("↻", BLUE);
        connectRow.addView(refresh,
            new LinearLayout.LayoutParams(55, 48));
        refresh.setOnClickListener(v -> refreshDevices());

        Button connect = button("CONECTAR", GREEN);
        connectRow.addView(connect,
            new LinearLayout.LayoutParams(125, 48));
        connect.setOnClickListener(v -> connectBluetooth());

        root.addView(connectRow,
            new LinearLayout.LayoutParams(-1, 62));

        // Panel principal
        LinearLayout area = row();
        root.addView(area,
            new LinearLayout.LayoutParams(-1, 0, 1));

        // Joystick a la izquierda
        LinearLayout leftPanel = column();
        leftPanel.setGravity(Gravity.CENTER);
        leftPanel.setPadding(8, 8, 8, 8);
        leftPanel.setBackground(shape(PANEL, 0xff12354a));

        area.addView(leftPanel,
            new LinearLayout.LayoutParams(0, -1, 1));

        leftPanel.addView(text("JOYSTICK", 19, BLUE),
            new LinearLayout.LayoutParams(-1, 35));

        Joystick joystick = new Joystick();
        LinearLayout.LayoutParams jp =
            new LinearLayout.LayoutParams(-1, 0, 1);
        jp.setMargins(5, 5, 5, 5);
        leftPanel.addView(joystick, jp);

        // Botones a la derecha
        LinearLayout rightPanel = column();
        rightPanel.setGravity(Gravity.CENTER);
        rightPanel.setPadding(8, 8, 8, 8);
        rightPanel.setBackground(shape(PANEL, 0xff12354a));

        LinearLayout.LayoutParams rightParams =
            new LinearLayout.LayoutParams(0, -1, 1);
        rightParams.setMargins(8, 0, 0, 0);
        area.addView(rightPanel, rightParams);

        rightPanel.addView(text("CONTROLES", 19, BLUE),
            new LinearLayout.LayoutParams(-1, 35));

        LinearLayout r1 = row();
        LinearLayout r2 = row();

        LinearLayout.LayoutParams cell1 =
            new LinearLayout.LayoutParams(0, -1, 1);
        cell1.setMargins(4, 4, 4, 4);

        LinearLayout.LayoutParams cell2 =
            new LinearLayout.LayoutParams(0, -1, 1);
        cell2.setMargins(4, 4, 4, 4);

        r1.addView(direction("▲\nADELANTE", "F", GREEN), cell1);
        r1.addView(direction("▶\nDERECHA", "R", RED), cell2);

        LinearLayout.LayoutParams cell3 =
            new LinearLayout.LayoutParams(0, -1, 1);
        cell3.setMargins(4, 4, 4, 4);

        LinearLayout.LayoutParams cell4 =
            new LinearLayout.LayoutParams(0, -1, 1);
        cell4.setMargins(4, 4, 4, 4);

        r2.addView(direction("◀\nIZQUIERDA", "L", BLUE), cell3);
        r2.addView(direction("▼\nATRÁS", "B", YELLOW), cell4);

        rightPanel.addView(r1,
            new LinearLayout.LayoutParams(-1, 0, 1));
        rightPanel.addView(r2,
            new LinearLayout.LayoutParams(-1, 0, 1));

        Button stop = button("■  STOP / DETENER", RED);
        LinearLayout.LayoutParams stopParams =
            new LinearLayout.LayoutParams(-1, 52);
        stopParams.setMargins(4, 6, 4, 2);
        rightPanel.addView(stop, stopParams);
        stop.setOnClickListener(v -> send("S"));

        // Barra inferior
        lastCommand = text("ÚLTIMO COMANDO: —", 14, GREEN);
        root.addView(lastCommand,
            new LinearLayout.LayoutParams(-1, 34));

        setContentView(root);
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(color);
        b.setAllCaps(false);
        b.setBackground(shape(0xff101c27, color));
        b.setPadding(4, 2, 4, 2);
        return b;
    }

    private Button direction(String label, String command, int color) {
        Button b = button(label, color);
        b.setTextSize(15);
        b.setBackground(shape(0xff07151e, color));

        b.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                send(command);
                return true;
            }

            if (e.getAction() == MotionEvent.ACTION_UP ||
                e.getAction() == MotionEvent.ACTION_CANCEL) {
                send("S");
                return true;
            }

            return true;
        });

        return b;
    }

    // Joystick circular con movimiento táctil
    private class Joystick extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float cx, cy, radius, knobX, knobY;
        private boolean moved = false;
        private String current = "";

        Joystick() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            cx = getWidth() / 2f;
            cy = getHeight() / 2f;
            radius = Math.max(20,
                Math.min(getWidth(), getHeight()) * 0.42f);

            if (!moved) {
                knobX = cx;
                knobY = cy;
            }

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(5);
            p.setColor(BLUE);
            p.setShadowLayer(12, 0, 0, BLUE);
            c.drawCircle(cx, cy, radius, p);
            p.clearShadowLayer();

            p.setStrokeWidth(2);
            p.setColor(0xff24506a);
            c.drawCircle(cx, cy, radius * 0.68f, p);
            c.drawCircle(cx, cy, radius * 0.35f, p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(0xff122d43);
            c.drawCircle(knobX, knobY, radius * 0.30f, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            p.setColor(BLUE);
            p.setShadowLayer(10, 0, 0, BLUE);
            c.drawCircle(knobX, knobY, radius * 0.30f, p);
            p.clearShadowLayer();

            p.setStyle(Paint.Style.FILL);
            p.setColor(BLUE);
            p.setTextSize(24);
            p.setTextAlign(Paint.Align.CENTER);

            c.drawText("▲", cx, cy - radius * 0.72f, p);
            c.drawText("▼", cx, cy + radius * 0.84f, p);
            c.drawText("◀", cx - radius * 0.72f, cy + 8, p);
            c.drawText("▶", cx + radius * 0.72f, cy + 8, p);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float dx = e.getX() - cx;
            float dy = e.getY() - cy;

            if (e.getAction() == MotionEvent.ACTION_DOWN ||
                e.getAction() == MotionEvent.ACTION_MOVE) {

                float distance =
                    (float) Math.sqrt(dx * dx + dy * dy);

                if (distance > radius * 0.65f) {
                    float scale = radius * 0.65f / distance;
                    dx *= scale;
                    dy *= scale;
                }

                knobX = cx + dx;
                knobY = cy + dy;
                moved = true;
                invalidate();

                String cmd = "";

                if (Math.sqrt(dx * dx + dy * dy) >
                    radius * 0.18f) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        cmd = dx > 0 ? "R" : "L";
                    } else {
                        cmd = dy > 0 ? "B" : "F";
                    }
                }

                if (!cmd.equals(current)) {
                    current = cmd;
                    send(cmd.isEmpty() ? "S" : cmd);
                }

                return true;
            }

            if (e.getAction() == MotionEvent.ACTION_UP ||
                e.getAction() == MotionEvent.ACTION_CANCEL) {
                moved = false;
                current = "";
                invalidate();
                send("S");
                return true;
            }

            return true;
        }
    }

    private boolean permission() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                REQUEST_BT
            );
            return false;
        }

        return true;
    }

    private void refreshDevices() {
        if (adapter == null) {
            status.setText("BLUETOOTH NO DISPONIBLE");
            return;
        }

        if (!permission()) return;

        if (!adapter.isEnabled()) {
            status.setText("ACTIVA BLUETOOTH");
            return;
        }

        paired.clear();
        ArrayList<String> names = new ArrayList<>();

        try {
            Set<BluetoothDevice> list = adapter.getBondedDevices();

            for (BluetoothDevice d : list) {
                paired.add(d);
                String name = d.getName();

                names.add(
                    (name == null ? "Bluetooth" : name)
                    + " (" + d.getAddress() + ")"
                );
            }
        } catch (SecurityException ex) {
            status.setText("PERMISO BLUETOOTH REQUERIDO");
        }

        if (names.isEmpty()) {
            names.add("Empareja el HC-05 primero");
        }

        devices.setAdapter(new ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        ));

        status.setText(paired.isEmpty()
            ? "● SIN DISPOSITIVOS"
            : "● LISTO PARA CONECTAR");
    }

    private void connectBluetooth() {
        if (!permission()) return;

        if (adapter == null || !adapter.isEnabled()) {
            status.setText("ACTIVA BLUETOOTH");
            return;
        }

        int i = devices.getSelectedItemPosition();

        if (i < 0 || i >= paired.size()) {
            status.setText("EMPAREJA EL HC-05");
            return;
        }

        BluetoothDevice d = paired.get(i);
        status.setText("CONECTANDO...");

        new Thread(() -> {
            BluetoothSocket candidate = null;

            try {
                candidate =
                    d.createRfcommSocketToServiceRecord(SPP);

                adapter.cancelDiscovery();
                candidate.connect();

                socket = candidate;
                output = socket.getOutputStream();
                connected = true;

                runOnUiThread(() ->
                    status.setText("● CONECTADO")
                );

            } catch (IOException | SecurityException ex) {
                connected = false;

                if (candidate != null) {
                    try {
                        candidate.close();
                    } catch (IOException ignored) {}
                }

                android.util.Log.e("HC05", "Bluetooth connection failed", ex);
                
                runOnUiThread(() -> {
                    status.setText("ERROR DE CONEXIÓN");
                
                    Toast.makeText(
                        MainActivity.this,
                        String.valueOf(ex.getMessage()),
                        Toast.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private void send(String command) {
        runOnUiThread(() ->
            lastCommand.setText("ÚLTIMO COMANDO: " + command)
        );

        if (!connected || output == null) return;

        try {
            output.write(command.getBytes("US-ASCII"));
            output.flush();

        } catch (IOException ex) {
            connected = false;
            runOnUiThread(() ->
                status.setText("● CONEXIÓN PERDIDA")
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == REQUEST_BT &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshDevices();
        } else if (requestCode == REQUEST_BT) {
            status.setText("PERMITE EL USO DE BLUETOOTH");
        }
    }

    @Override
    protected void onDestroy() {
        connected = false;

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        super.onDestroy();
    }
}
