package com.productapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutHome, layoutProductConfig, layoutProductRates;
    private EditText etProductName, etBigFrom, etBigTo, etSmallFrom, etSmallTo, etBigRate, etSmallRate;
    private LinearLayout layoutSizeRows, layoutOrderInputs, layoutSavedBoxes;
    private Spinner spinnerHomeProduct, spinnerCategory, spinnerRateProduct;
    private ArrayList<EditText> sizeInputList = new ArrayList<>();
    
    private ArrayList<JSONObject> productList = new ArrayList<>();
    private HashMap<String, JSONObject> ratesMap = new HashMap<>();
    private ArrayList<JSONObject> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- GLOBAL CRASH HANDLER ---
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            SharedPreferences sp = getSharedPreferences("crash_prefs", MODE_PRIVATE);
            sp.edit().putString("last_crash", stackTrace).apply();

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });

        setContentView(R.layout.activity_main);

        // Check if a previous crash occurred and show it
        SharedPreferences sp = getSharedPreferences("crash_prefs", MODE_PRIVATE);
        String crashLog = sp.getString("last_crash", null);
        if (crashLog != null) {
            sp.edit().remove("last_crash").apply();
            new AlertDialog.Builder(this)
                .setTitle("App Crash Detected")
                .setMessage(crashLog)
                .setPositiveButton("OK", null)
                .show();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutHome = findViewById(R.id.layoutHome);
        layoutProductConfig = findViewById(R.id.layoutProductConfig);
        layoutProductRates = findViewById(R.id.layoutProductRates);
        
        etProductName = findViewById(R.id.etProductName);
        etBigFrom = findViewById(R.id.etBigFrom);
        etBigTo = findViewById(R.id.etBigTo);
        etSmallFrom = findViewById(R.id.etSmallFrom);
        etSmallTo = findViewById(R.id.etSmallTo);
        etBigRate = findViewById(R.id.etBigRate);
        etSmallRate = findViewById(R.id.etSmallRate);
        
        layoutSizeRows = findViewById(R.id.layoutSizeRows);
        layoutOrderInputs = findViewById(R.id.layoutOrderInputs);
        layoutSavedBoxes = findViewById(R.id.layoutSavedBoxes);
        
        spinnerHomeProduct = findViewById(R.id.spinnerHomeProduct);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerRateProduct = findViewById(R.id.spinnerRateProduct);

        findViewById(R.id.btnAddSize).setOnClickListener(v -> addSizeInputField(""));
        findViewById(R.id.btnSaveProduct).setOnClickListener(v -> saveProductData());
        findViewById(R.id.btnSaveRates).setOnClickListener(v -> saveRateData());
        findViewById(R.id.btnSaveOrder).setOnClickListener(v -> saveOrderData());

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"-- Select Category --", "Big", "Small"});
        spinnerCategory.setAdapter(catAdapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderOrderInputs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerHomeProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderOrderInputs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerRateProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadRateHints(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addSizeInputField("");
        loadDataFromStorage();
        refreshSpinners();
        renderSavedOrders();
    }

    private void addSizeInputField(String val) {
        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        et.setHint("Enter Size (e.g. S, M, L)");
        if(!val.isEmpty()) et.setText(val);
        layoutSizeRows.addView(et);
        sizeInputList.add(et);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_product) {
            showSection(layoutProductConfig);
            return true;
        } else if (id == R.id.menu_rates) {
            showSection(layoutProductRates);
            refreshSpinners();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSection(LinearLayout target) {
        layoutHome.setVisibility(View.GONE);
        layoutProductConfig.setVisibility(View.GONE);
        layoutProductRates.setVisibility(View.GONE);
        target.setVisibility(View.VISIBLE);
        if(target == layoutHome) {
            refreshSpinners();
            renderSavedOrders();
        }
    }

    private void saveProductData() {
        try {
            String name = etProductName.getText().toString().trim();
            JSONArray sizesArr = new JSONArray();
            for(EditText et : sizeInputList) {
                String s = et.getText().toString().trim();
                if(!s.isEmpty()) sizesArr.put(s);
            }
            if(name.isEmpty() || sizesArr.length() == 0) {
                Toast.makeText(this, "Enter name and sizes", Toast.LENGTH_SHORT).show();
                return;
            }
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("sizes", sizesArr);
            obj.put("bigFrom", etBigFrom.getText().toString().trim());
            obj.put("bigTo", etBigTo.getText().toString().trim());
            obj.put("smallFrom", etSmallFrom.getText().toString().trim());
            obj.put("smallTo", etSmallTo.getText().toString().trim());

            productList.add(obj);
            saveDataToStorage();
            Toast.makeText(this, "Product Saved Successfully", Toast.LENGTH_SHORT).show();
            etProductName.setText("");
            showSection(layoutHome);
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void refreshSpinners() {
        ArrayList<String> names = new ArrayList<>();
        names.add("-- Choose Product --");
        for(JSONObject p : productList) {
            try { names.add(p.getString("name")); } catch(Exception e) {}
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerHomeProduct.setAdapter(adapter);
        spinnerRateProduct.setAdapter(adapter);
    }

    private void loadRateHints(int pos) {
        if(pos <= 0 || pos > productList.size()) return;
        try {
            JSONObject p = productList.get(pos - 1);
            String bFrom = p.getString("bigFrom");
            String bTo = p.getString("bigTo");
            String sFrom = p.getString("smallFrom");
            String sTo = p.getString("smallTo");

            TextView tvBigLabel = findViewById(R.id.tvBigRateLabel);
            TextView tvSmallLabel = findViewById(R.id.tvSmallRateLabel);
            tvBigLabel.setText("Big Product Rates (Range: " + bFrom + " to " + bTo + ")");
            tvSmallLabel.setText("Small Product Rates (Range: " + sFrom + " to " + sTo + ")");

            String name = p.getString("name");
            if(ratesMap.containsKey(name)) {
                JSONObject r = ratesMap.get(name);
                etBigRate.setText(r.optString("big", ""));
                etSmallRate.setText(r.optString("small", ""));
            } else {
                etBigRate.setText("");
                etSmallRate.setText("");
            }
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void saveRateData() {
        int pos = spinnerRateProduct.getSelectedItemPosition();
        if(pos <= 0) {
            Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject p = productList.get(pos - 1);
            JSONObject r = new JSONObject();
            r.put("big", etBigRate.getText().toString().trim());
            r.put("small", etSmallRate.getText().toString().trim());
            ratesMap.put(p.getString("name"), r);
            saveDataToStorage();
            Toast.makeText(this, "Rates Saved Successfully", Toast.LENGTH_SHORT).show();
            showSection(layoutHome);
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void renderOrderInputs() {
        layoutOrderInputs.removeAllViews();
        int pPos = spinnerHomeProduct.getSelectedItemPosition();
        int cPos = spinnerCategory.getSelectedItemPosition();
        if(pPos <= 0 || cPos <= 0) return;

        try {
            JSONObject p = productList.get(pPos - 1);
            JSONArray sizes = p.getJSONArray("sizes");
            for(int i = 0; i < sizes.length(); i++) {
                String size = sizes.getString(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                TextView tv = new TextView(this);
                tv.setText("Size: " + size);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                EditText et = new EditText(this);
                et.setHint("Enter Qty");
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setTag(size);
                et.setLayoutParams(new LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT));

                row.addView(tv);
                row.addView(et);
                layoutOrderInputs.addView(row);
            }
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void saveOrderData() {
        int pPos = spinnerHomeProduct.getSelectedItemPosition();
        int cPos = spinnerCategory.getSelectedItemPosition();
        if(pPos <= 0 || cPos <= 0) {
            Toast.makeText(this, "Select product and category", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject p = productList.get(pPos - 1);
            String pName = p.getString("name");
            String cat = spinnerCategory.getSelectedItem().toString();

            JSONObject rates = ratesMap.get(pName);
            double rateVal = 0;
            if(rates != null) {
                String rStr = rates.optString(cat.toLowerCase(), "0");
                rateVal = rStr.isEmpty() ? 0 : Double.parseDouble(rStr);
            }

            JSONArray items = new JSONArray();
            for(int i = 0; i < layoutOrderInputs.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) layoutOrderInputs.getChildAt(i);
                EditText et = (EditText) row.getChildAt(1);
                String qStr = et.getText().toString().trim();
                int qty = qStr.isEmpty() ? 0 : Integer.parseInt(qStr);
                if(qty > 0) {
                    JSONObject item = new JSONObject();
                    item.put("size", et.getTag().toString());
                    item.put("type", cat);
                    item.put("qty", qty);
                    item.put("rate", rateVal);
                    items.put(item);
                }
            }

            if(items.length() == 0) {
                Toast.makeText(this, "Enter quantity for at least one size", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject order = new JSONObject();
            order.put("productName", pName);
            order.put("items", items);
            orderList.add(order);
            saveDataToStorage();

            Toast.makeText(this, "Order Box Created!", Toast.LENGTH_SHORT).show();
            spinnerHomeProduct.setSelection(0);
            spinnerCategory.setSelection(0);
            layoutOrderInputs.removeAllViews();
            renderSavedOrders();
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void renderSavedOrders() {
        layoutSavedBoxes.removeAllViews();
        for(JSONObject order : orderList) {
            try {
                String pName = order.getString("productName");
                JSONArray items = order.getJSONArray("items");

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 16, 16, 16);
                card.setBackgroundColor(0xFFFFFFFF);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 16);
                card.setLayoutParams(lp);

                TextView tvHeading = new TextView(this);
                tvHeading.setText(pName);
                tvHeading.setTextSize(16);
                tvHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeading.setTextColor(0xFF2563EB);
                tvHeading.setPadding(0, 0, 0, 8);
                card.addView(tvHeading);

                TableLayout table = new TableLayout(this);
                table.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                // Header Row
                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(0xFFE5E7EB);
                headerRow.addView(makeTableCell("Size", true));
                headerRow.addView(makeTableCell("Type", true));
                headerRow.addView(makeTableCell("Qty", true));
                headerRow.addView(makeTableCell("Rate", true));
                table.addView(headerRow);

                int totalQty = 0;
                double totalAmount = 0;

                for(int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String size = item.getString("size");
                    String type = item.getString("type");
                    int qty = item.getInt("qty");
                    double rate = item.getDouble("rate");
                    double amount = qty * rate;

                    totalQty += qty;
                    totalAmount += amount;

                    TableRow row = new TableRow(this);
                    row.addView(makeTableCell(size, false));
                    row.addView(makeTableCell(type, false));
                    row.addView(makeTableCell(String.valueOf(qty), false));
                    row.addView(makeTableCell(String.valueOf(amount), false));
                    table.addView(row);
                }

                // Total Row
                TableRow totalRow = new TableRow(this);
                totalRow.setBackgroundColor(0xFFF3F4F6);
                TextView tvTotalLabel = makeTableCell("Total", true);
                tvTotalLabel.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                totalRow.addView(tvTotalLabel);
                totalRow.addView(makeTableCell(String.valueOf(totalQty), true));
                totalRow.addView(makeTableCell(String.valueOf(totalAmount), true));
                table.addView(totalRow);

                card.addView(table);
                layoutSavedBoxes.addView(card);
            } catch(Exception e) { e.printStackTrace(); }
        }
    }

    private TextView makeTableCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        if(isHeader) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void saveDataToStorage() {
        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        
        JSONArray pArr = new JSONArray(productList);
        editor.putString("products", pArr.toString());

        JSONObject rObj = new JSONObject(ratesMap);
        editor.putString("rates", rObj.toString());

        JSONArray oArr = new JSONArray(orderList);
        editor.putString("orders", oArr.toString());
        editor.apply();
    }

    private void loadDataFromStorage() {
        try {
            SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String pStr = sp.getString("products", "[]");
            JSONArray pArr = new JSONArray(pStr);
            productList.clear();
            for(int i = 0; i < pArr.length(); i++) productList.add(pArr.getJSONObject(i));

            String rStr = sp.getString("rates", "{}");
            JSONObject rObj = new JSONObject(rStr);
            ratesMap.clear();
            Iterator<String> keys = rObj.keys();
            while(keys.hasNext()) {
                String k = keys.next();
                ratesMap.put(k, rObj.getJSONObject(k));
            }

            String oStr = sp.getString("orders", "[]");
            JSONArray oArr = new JSONArray(oStr);
            orderList.clear();
            for(int i = 0; i < oArr.length(); i++) orderList.add(oArr.getJSONObject(i));
        } catch(Exception e) { e.printStackTrace(); }
    }
}
