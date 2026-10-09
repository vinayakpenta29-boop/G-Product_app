package com.productapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutHome, layoutProductConfig, layoutProductRates, layoutHistory, layoutSavedBoxes, layoutHistoryBoxes;
    private EditText etProductName, etBigFrom, etBigTo, etSmallFrom, etSmallTo, etBigRate, etSmallRate;
    private LinearLayout layoutSizeRows, layoutOrderInputs;
    private Spinner spinnerHomeProduct, spinnerRateProduct;
    private ArrayList<EditText> sizeInputList = new ArrayList<>();
    
    private ArrayList<JSONObject> productList = new ArrayList<>();
    private HashMap<String, JSONObject> ratesMap = new HashMap<>();
    private ArrayList<JSONObject> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutHome = findViewById(R.id.layoutHome);
        layoutProductConfig = findViewById(R.id.layoutProductConfig);
        layoutProductRates = findViewById(R.id.layoutProductRates);
        layoutHistory = findViewById(R.id.layoutHistory);
        layoutSavedBoxes = findViewById(R.id.layoutSavedBoxes);
        layoutHistoryBoxes = findViewById(R.id.layoutHistoryBoxes);
        
        etProductName = findViewById(R.id.etProductName);
        etBigFrom = findViewById(R.id.etBigFrom);
        etBigTo = findViewById(R.id.etBigTo);
        etSmallFrom = findViewById(R.id.etSmallFrom);
        etSmallTo = findViewById(R.id.etSmallTo);
        etBigRate = findViewById(R.id.etBigRate);
        etSmallRate = findViewById(R.id.etSmallRate);
        
        layoutSizeRows = findViewById(R.id.layoutSizeRows);
        layoutOrderInputs = findViewById(R.id.layoutOrderInputs);
        
        spinnerHomeProduct = findViewById(R.id.spinnerHomeProduct);
        spinnerRateProduct = findViewById(R.id.spinnerRateProduct);

        findViewById(R.id.btnAddSize).setOnClickListener(v -> addSizeInputField(""));
        findViewById(R.id.btnSaveProduct).setOnClickListener(v -> saveProductData());
        findViewById(R.id.btnSaveRates).setOnClickListener(v -> saveRateData());
        findViewById(R.id.btnSaveOrder).setOnClickListener(v -> saveOrderData());

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
        if (id == R.id.menu_home) {
            showSection(layoutHome);
            return true;
        } else if (id == R.id.menu_product) {
            showSection(layoutProductConfig);
            return true;
        } else if (id == R.id.menu_rates) {
            showSection(layoutProductRates);
            refreshSpinners();
            return true;
        } else if (id == R.id.menu_history) {
            showSection(layoutHistory);
            renderHistoryOrders();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSection(LinearLayout target) {
        layoutHome.setVisibility(View.GONE);
        layoutProductConfig.setVisibility(View.GONE);
        layoutProductRates.setVisibility(View.GONE);
        layoutHistory.setVisibility(View.GONE);
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
        if(pPos <= 0) return;

        try {
            JSONObject p = productList.get(pPos - 1);
            JSONArray sizes = p.getJSONArray("sizes");
            String bigFrom = p.optString("bigFrom", "");
            String bigTo = p.optString("bigTo", "");
            String smallFrom = p.optString("smallFrom", "");
            String smallTo = p.optString("smallTo", "");

            JSONObject rates = ratesMap.get(p.getString("name"));
            double bigRate = 0, smallRate = 0;
            if(rates != null) {
                String bStr = rates.optString("big", "0");
                String sStr = rates.optString("small", "0");
                bigRate = bStr.isEmpty() ? 0 : Double.parseDouble(bStr);
                smallRate = sStr.isEmpty() ? 0 : Double.parseDouble(sStr);
            }

            for(int i = 0; i < sizes.length(); i++) {
                String size = sizes.getString(i);
                String type = getSizeType(size, bigFrom, bigTo, smallFrom, smallTo);
                double rate = type.equals("Big") ? bigRate : smallRate;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 4, 0, 4);

                TextView tv = new TextView(this);
                tv.setText("Size: " + size + " (" + type + ") - Rate: " + rate);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                EditText et = new EditText(this);
                et.setHint("Enter Qty");
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setTag(size + "|" + type + "|" + rate);
                et.setLayoutParams(new LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT));

                row.addView(tv);
                row.addView(et);
                layoutOrderInputs.addView(row);
            }
        } catch(Exception e) { e.printStackTrace(); }
    }

    private String getSizeType(String size, String bigFrom, String bigTo, String smallFrom, String smallTo) {
        try {
            double sVal = Double.parseDouble(size);
            double bFrom = bigFrom.isEmpty() ? 0 : Double.parseDouble(bigFrom);
            double bTo = bigTo.isEmpty() ? 0 : Double.parseDouble(bigTo);
            if(!bigFrom.isEmpty() && !bigTo.isEmpty() && sVal >= bFrom && sVal <= bTo) return "Big";
        } catch(Exception e) {}
        try {
            double sVal = Double.parseDouble(size);
            double sFrom = smallFrom.isEmpty() ? 0 : Double.parseDouble(smallFrom);
            double sTo = smallTo.isEmpty() ? 0 : Double.parseDouble(smallTo);
            if(!smallFrom.isEmpty() && !smallTo.isEmpty() && sVal >= sFrom && sVal <= sTo) return "Small";
        } catch(Exception e) {}
        return "Big";
    }

    private void saveOrderData() {
        int pPos = spinnerHomeProduct.getSelectedItemPosition();
        if(pPos <= 0) {
            Toast.makeText(this, "Select a product first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject p = productList.get(pPos - 1);
            String pName = p.getString("name");

            JSONArray items = new JSONArray();
            for(int i = 0; i < layoutOrderInputs.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) layoutOrderInputs.getChildAt(i);
                EditText et = (EditText) row.getChildAt(1);
                String qStr = et.getText().toString().trim();
                int qty = qStr.isEmpty() ? 0 : Integer.parseInt(qStr);
                
                if(qty > 0) {
                    String tag = et.getTag().toString(); // size|type|rate
                    String[] parts = tag.split("\\|");
                    String size = parts[0];
                    String type = parts[1];
                    double rate = Double.parseDouble(parts[2]);

                    JSONObject item = new JSONObject();
                    item.put("size", size);
                    item.put("type", type);
                    item.put("qty", qty);
                    item.put("rate", rate);
                    items.put(item);
                }
            }

            if(items.length() == 0) {
                Toast.makeText(this, "Enter quantity for at least one size", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

            JSONObject order = new JSONObject();
            order.put("productName", pName);
            order.put("items", items);
            order.put("date", currentDate);
            order.put("isPaid", false);
            order.put("paidDate", "");
            orderList.add(order);
            saveDataToStorage();

            Toast.makeText(this, "Order Box Created!", Toast.LENGTH_SHORT).show();
            spinnerHomeProduct.setSelection(0);
            layoutOrderInputs.removeAllViews();
            renderSavedOrders();
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void renderSavedOrders() {
        layoutSavedBoxes.removeAllViews();
        for(int index = 0; index < orderList.size(); index++) {
            final int oIndex = index;
            try {
                JSONObject order = orderList.get(oIndex);
                boolean isPaid = order.optBoolean("isPaid", false);
                if(isPaid) {
                    continue; // EXCLUDE PAID TABLES FROM HOME SCREEN
                }

                String pName = order.getString("productName");
                String date = order.optString("date", "");
                JSONArray items = order.getJSONArray("items");

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(16, 16, 16, 16);
                card.setBackgroundColor(0xFFFFFFFF);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 16);
                card.setLayoutParams(lp);

                // Header & Date
                LinearLayout headerLayout = new LinearLayout(this);
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView tvHeading = new TextView(this);
                tvHeading.setText(pName);
                tvHeading.setTextSize(16);
                tvHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeading.setTextColor(0xFF4F46E5);
                tvHeading.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                headerLayout.addView(tvHeading);

                TextView tvDate = new TextView(this);
                tvDate.setText("Created: " + date);
                tvDate.setTextSize(11);
                tvDate.setTextColor(0xFF64748B);
                headerLayout.addView(tvDate);
                card.addView(headerLayout);

                // Table
                TableLayout table = new TableLayout(this);
                table.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                table.setPadding(0, 8, 0, 8);

                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(0xFFE2E8F0);
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

                TableRow totalRow = new TableRow(this);
                totalRow.setBackgroundColor(0xFFF1F5F9);
                TextView tvTotalLabel = makeTableCell("Total", true);
                tvTotalLabel.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                totalRow.addView(tvTotalLabel);
                totalRow.addView(makeTableCell(String.valueOf(totalQty), true));
                totalRow.addView(makeTableCell(String.valueOf(totalAmount), true));
                table.addView(totalRow);

                card.addView(table);

                // Paid Checkbox
                CheckBox cbPaid = new CheckBox(this);
                cbPaid.setText("Mark as Paid");
                cbPaid.setTextColor(0xFF059669);
                cbPaid.setTypeface(null, android.graphics.Typeface.BOLD);
                cbPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if(isChecked) {
                        try {
                            String paidDateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                            order.put("isPaid", true);
                            order.put("paidDate", paidDateStr);
                            saveDataToStorage();
                            Toast.makeText(this, "Moved to History!", Toast.LENGTH_SHORT).show();
                            renderSavedOrders();
                        } catch(Exception e) { e.printStackTrace(); }
                    }
                });
                card.addView(cbPaid);

                layoutSavedBoxes.addView(card);
            } catch(Exception e) { e.printStackTrace(); }
        }
    }

    private void renderHistoryOrders() {
        layoutHistoryBoxes.removeAllViews();
        for(int index = 0; index < orderList.size(); index++) {
            try {
                JSONObject order = orderList.get(index);
                boolean isPaid = order.optBoolean("isPaid", false);
                if(!isPaid) {
                    continue; // EXCLUDE UNPAID TABLES FROM HISTORY SCREEN
                }

                String pName = order.getString("productName");
                String date = order.optString("date", "");
                String paidDate = order.optString("paidDate", "");
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
                tvHeading.setTextColor(0xFF059669);
                tvHeading.setPadding(0, 0, 0, 4);
                card.addView(tvHeading);

                TextView tvDates = new TextView(this);
                tvDates.setText("Created: " + date + " | Paid: " + paidDate);
                tvDates.setTextSize(12);
                tvDates.setTextColor(0xFF64748B);
                tvDates.setPadding(0, 0, 0, 8);
                card.addView(tvDates);

                TableLayout table = new TableLayout(this);
                table.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(0xFFE2E8F0);
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

                TableRow totalRow = new TableRow(this);
                totalRow.setBackgroundColor(0xFFF1F5F9);
                TextView tvTotalLabel = makeTableCell("Total", true);
                tvTotalLabel.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                totalRow.addView(tvTotalLabel);
                totalRow.addView(makeTableCell(String.valueOf(totalQty), true));
                totalRow.addView(makeTableCell(String.valueOf(totalAmount), true));
                table.addView(totalRow);

                card.addView(table);
                layoutHistoryBoxes.addView(card);
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
