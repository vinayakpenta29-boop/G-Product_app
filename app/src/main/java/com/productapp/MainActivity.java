package com.productapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutSavedBoxes, layoutOrderInputs;
    private Spinner spinnerHomeProduct;
    private TextView tvSummaryTotalQty, tvSummaryTotalAmount;
    
    private ArrayList<JSONObject> productList = new ArrayList<>();
    private HashMap<String, JSONObject> ratesMap = new HashMap<>();
    private ArrayList<JSONObject> orderList = new ArrayList<>();

    private final int COLOR_GREEN = Color.parseColor("#059669");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        layoutSavedBoxes = findViewById(R.id.layoutSavedBoxes);
        layoutOrderInputs = findViewById(R.id.layoutOrderInputs);
        spinnerHomeProduct = findViewById(R.id.spinnerHomeProduct);
        
        tvSummaryTotalQty = findViewById(R.id.tvSummaryTotalQty);
        tvSummaryTotalAmount = findViewById(R.id.tvSummaryTotalAmount);

        findViewById(R.id.btnSaveOrder).setOnClickListener(v -> saveOrderData());

        spinnerHomeProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderOrderInputs();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadDataFromStorage();
        refreshSpinners();
        renderSavedOrders();
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
            showProductConfigDialog();
            return true;
        } else if (id == R.id.menu_rates) {
            showProductRatesDialog();
            return true;
        } else if (id == R.id.menu_history) {
            showHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProductConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_config, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        EditText etProductName = view.findViewById(R.id.etProductName);
        LinearLayout layoutSizeRows = view.findViewById(R.id.layoutSizeRows);
        EditText etBigFrom = view.findViewById(R.id.etBigFrom);
        EditText etBigTo = view.findViewById(R.id.etBigTo);
        EditText etSmallFrom = view.findViewById(R.id.etSmallFrom);
        EditText etSmallTo = view.findViewById(R.id.etSmallTo);
        ArrayList<EditText> dialogSizeList = new ArrayList<>();

        Runnable addSizeRow = () -> {
            EditText et = new EditText(this);
            int heightPx = (int) (48 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightPx);
            lp.setMargins(0, 0, 0, 8);
            et.setLayoutParams(lp);
            et.setHint("Enter Size (e.g. S, M, L)");
            et.setPadding(12, 0, 12, 0);
            et.setBackgroundResource(R.drawable.bg_rounded_edittext);
            layoutSizeRows.addView(et);
            dialogSizeList.add(et);
        };

        addSizeRow.run();
        view.findViewById(R.id.btnAddSize).setOnClickListener(v -> addSizeRow.run());

        view.findViewById(R.id.btnSaveProduct).setOnClickListener(v -> {
            try {
                String name = etProductName.getText().toString().trim();
                JSONArray sizesArr = new JSONArray();
                for(EditText et : dialogSizeList) {
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
                refreshSpinners();
                Toast.makeText(this, "Product Saved Successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch(Exception e) { e.printStackTrace(); }
        });

        dialog.show();
    }

    private void showProductRatesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_rates, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Spinner spinnerRateProduct = view.findViewById(R.id.spinnerRateProduct);
        TextView tvBigLabel = view.findViewById(R.id.tvBigRateLabel);
        TextView tvSmallLabel = view.findViewById(R.id.tvSmallRateLabel);
        EditText etBigRate = view.findViewById(R.id.etBigRate);
        EditText etSmallRate = view.findViewById(R.id.etSmallRate);

        ArrayList<String> names = new ArrayList<>();
        names.add("-- Choose Product --");
        for(JSONObject p : productList) {
            try { names.add(p.getString("name")); } catch(Exception e) {}
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerRateProduct.setAdapter(adapter);

        spinnerRateProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if(position <= 0 || position > productList.size()) return;
                try {
                    JSONObject p = productList.get(position - 1);
                    tvBigLabel.setText("Big Product Rates (Range: " + p.optString("bigFrom") + " to " + p.optString("bigTo") + ")");
                    tvSmallLabel.setText("Small Product Rates (Range: " + p.optString("smallFrom") + " to " + p.optString("smallTo") + ")");
                    
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
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        view.findViewById(R.id.btnSaveRates).setOnClickListener(v -> {
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
                dialog.dismiss();
            } catch(Exception e) { e.printStackTrace(); }
        });

        dialog.show();
    }

    private void showHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(16, 16, 16, 16);
        LinearLayout layoutHistoryBoxes = new LinearLayout(this);
        layoutHistoryBoxes.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layoutHistoryBoxes);

        float density = getResources().getDisplayMetrics().density;
        float cardRadiusPx = 24f * density;
        float tableRadiusPx = 18f * density;

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for(int index = 0; index < orderList.size(); index++) {
            try {
                final JSONObject order = orderList.get(index);
                boolean isPaid = order.optBoolean("isPaid", false);
                if(!isPaid) continue;

                String pName = order.getString("productName");
                String date = order.optString("date", "");
                String paidDate = order.optString("paidDate", "");
                JSONArray items = order.getJSONArray("items");

                MaterialCardView card = new MaterialCardView(this);
                card.setRadius(cardRadiusPx);
                card.setCardElevation(4f * density);
                card.setStrokeWidth((int) (1 * density));
                card.setStrokeColor(Color.parseColor("#E2E8F0"));
                card.setCardBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, (int) (16 * density));
                card.setLayoutParams(lp);

                // Long Press Listener for History Table
                card.setOnLongClickListener(v -> {
                    showHistoryOrderOptionsDialog(order, dialogHolder[0]);
                    return true;
                });

                LinearLayout cardInner = new LinearLayout(this);
                cardInner.setOrientation(LinearLayout.VERTICAL);
                int padPx = (int) (18 * density);
                cardInner.setPadding(padPx, padPx, padPx, padPx);

                TextView tvHeading = new TextView(this);
                tvHeading.setText(pName);
                tvHeading.setTextSize(16);
                tvHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeading.setTextColor(COLOR_GREEN);
                tvHeading.setPadding(0, 0, 0, 2);
                cardInner.addView(tvHeading);

                TextView tvDates = new TextView(this);
                tvDates.setText("Created: " + date + " | Paid: " + paidDate);
                tvDates.setTextSize(11);
                tvDates.setTextColor(Color.parseColor("#64748B"));
                tvDates.setPadding(0, 0, 0, (int) (12 * density));
                cardInner.addView(tvDates);

                // Rounded Table Wrapper Container
                LinearLayout tableContainer = new LinearLayout(this);
                tableContainer.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable containerBg = new GradientDrawable();
                containerBg.setShape(GradientDrawable.RECTANGLE);
                containerBg.setCornerRadius(tableRadiusPx);
                containerBg.setColor(Color.WHITE);
                containerBg.setStroke((int) (1 * density), Color.parseColor("#CBD5E1"));
                tableContainer.setBackground(containerBg);
                tableContainer.setClipToOutline(true);

                TableLayout table = new TableLayout(this);
                table.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                table.setStretchAllColumns(true);

                // Header Row (5 Columns: Size | Type | Qty | Rate | Amount)
                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(Color.parseColor("#E0F2FE"));
                headerRow.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
                headerRow.addView(makeTableCell("Size", true, false));
                headerRow.addView(makeTableCell("Type", true, false));
                headerRow.addView(makeTableCell("Qty", true, false));
                headerRow.addView(makeTableCell("Rate", true, false));
                headerRow.addView(makeTableCell("Amount", true, false));
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
                    row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#FFFFFF") : Color.parseColor("#F8FAFC"));
                    row.setPadding(0, (int) (10 * density), 0, (int) (10 * density));
                    
                    row.addView(makeTableCell(size, false, false));
                    row.addView(makeTableCell(type, false, false));
                    row.addView(makeTableCell(String.valueOf(qty), false, false));
                    row.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", rate), false, false, COLOR_GREEN));
                    row.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", amount), false, false));
                    table.addView(row);
                }

                // Total Footer Row
                TableRow totalRow = new TableRow(this);
                totalRow.setBackgroundColor(Color.parseColor("#DCFCE7"));
                totalRow.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
                totalRow.addView(makeTableCell("Total", false, true));
                totalRow.addView(makeTableCell("", false, true));
                totalRow.addView(makeTableCell(String.valueOf(totalQty), false, true));
                totalRow.addView(makeTableCell("", false, true));
                totalRow.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", totalAmount), false, true, COLOR_GREEN));
                table.addView(totalRow);

                tableContainer.addView(table);
                cardInner.addView(tableContainer);
                card.addView(cardInner);
                layoutHistoryBoxes.addView(card);
            } catch(Exception e) { e.printStackTrace(); }
        }

        builder.setTitle("Paid Orders History");
        builder.setView(scrollView);
        builder.setPositiveButton("Close", (d, which) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialogHolder[0] = dialog;
        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void showHistoryOrderOptionsDialog(JSONObject order, AlertDialog historyDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (24 * density);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundResource(R.drawable.bg_dialog_rounded);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(order.optString("productName", "Order") + " - Payment Status");
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1E293B"));
        tvTitle.setPadding(0, 0, 0, (int) (16 * density));
        layout.addView(tvTitle);

        CheckBox cb = new CheckBox(this);
        cb.setText("Mark as Paid");
        cb.setChecked(true); // Checked by default in History
        cb.setTextColor(COLOR_GREEN);
        cb.setTextSize(15f);
        cb.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(cb);

        builder.setView(layout);
        AlertDialog statusDialog = builder.create();
        if(statusDialog.getWindow() != null) {
            statusDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isChecked) {
                try {
                    order.put("isPaid", false);
                    order.put("paidDate", "");
                    saveDataToStorage();
                    Toast.makeText(this, "Order moved back to Home Screen!", Toast.LENGTH_SHORT).show();
                    statusDialog.dismiss();
                    if(historyDialog != null && historyDialog.isShowing()) {
                        historyDialog.dismiss();
                    }
                    renderSavedOrders();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });

        statusDialog.show();
    }

    private void showHomeOrderOptionsDialog(JSONObject order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String pName = order.optString("productName", "Order");
        builder.setTitle(pName + " Options");
        String[] options = {"Edit Order Box", "Delete Order Box"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showEditOrderDialog(order);
            } else if (which == 1) {
                confirmDeleteOrder(order);
            }
        });
        builder.show();
    }

    private void confirmDeleteOrder(JSONObject order) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Order Box")
            .setMessage("Are you sure you want to delete this order box?")
            .setPositiveButton("Delete", (dialog, which) -> {
                orderList.remove(order);
                saveDataToStorage();
                renderSavedOrders();
                Toast.makeText(this, "Order Box Deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditOrderDialog(JSONObject order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundResource(R.drawable.bg_dialog_rounded);
        
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int padPx = (int) (20 * density);
        contentLayout.setPadding(padPx, padPx, padPx, padPx);
        scrollView.addView(contentLayout);

        String pName = order.optString("productName", "");
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Edit Order - " + pName);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#4F46E5"));
        tvTitle.setPadding(0, 0, 0, (int) (16 * density));
        contentLayout.addView(tvTitle);

        // Find product config to load sizes
        JSONObject product = null;
        for(JSONObject p : productList) {
            if(p.optString("name", "").equals(pName)) {
                product = p;
                break;
            }
        }

        // Existing quantities map
        HashMap<String, Integer> currentQtyMap = new HashMap<>();
        JSONArray oldItems = order.optJSONArray("items");
        if(oldItems != null) {
            for(int i = 0; i < oldItems.length(); i++) {
                JSONObject item = oldItems.optJSONObject(i);
                if(item != null) {
                    currentQtyMap.put(item.optString("size"), item.optInt("qty", 0));
                }
            }
        }

        if(product != null) {
            JSONArray sizes = product.optJSONArray("sizes");
            String bigFrom = product.optString("bigFrom", "");
            String bigTo = product.optString("bigTo", "");
            String smallFrom = product.optString("smallFrom", "");
            String smallTo = product.optString("smallTo", "");

            JSONObject rates = ratesMap.get(pName);
            double bigRate = 0, smallRate = 0;
            if(rates != null) {
                String bStr = rates.optString("big", "0");
                String sStr = rates.optString("small", "0");
                bigRate = bStr.isEmpty() ? 0 : Double.parseDouble(bStr);
                smallRate = sStr.isEmpty() ? 0 : Double.parseDouble(sStr);
            }

            if(sizes != null) {
                for(int i = 0; i < sizes.length(); i++) {
                    String size = sizes.optString(i);
                    String type = getSizeType(size, bigFrom, bigTo, smallFrom, smallTo);
                    double rate = type.equals("Big") ? bigRate : smallRate;
                    int currentQty = currentQtyMap.containsKey(size) ? currentQtyMap.get(size) : 0;

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
                    row.setGravity(Gravity.CENTER_VERTICAL);

                    TextView tv = new TextView(this);
                    tv.setText("Size: " + size + " (" + type + ") - Rate: " + rate);
                    tv.setTextColor(Color.parseColor("#334155"));
                    tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    EditText et = new EditText(this);
                    et.setHint("Qty");
                    et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    et.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
                    et.setBackgroundResource(R.drawable.bg_rounded_edittext);
                    if(currentQty > 0) et.setText(String.valueOf(currentQty));
                    et.setTag(size + "|" + type + "|" + rate);

                    int heightPx = (int) (48 * density);
                    et.setLayoutParams(new LinearLayout.LayoutParams((int) (90 * density), heightPx));

                    row.addView(tv);
                    row.addView(et);
                    contentLayout.addView(row);
                }
            }
        } else if(oldItems != null) {
            for(int i = 0; i < oldItems.length(); i++) {
                JSONObject item = oldItems.optJSONObject(i);
                if(item == null) continue;
                String size = item.optString("size");
                String type = item.optString("type");
                double rate = item.optDouble("rate", 0);
                int currentQty = item.optInt("qty", 0);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
                row.setGravity(Gravity.CENTER_VERTICAL);

                TextView tv = new TextView(this);
                tv.setText("Size: " + size + " (" + type + ") - Rate: " + rate);
                tv.setTextColor(Color.parseColor("#334155"));
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                EditText et = new EditText(this);
                et.setHint("Qty");
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setPadding((int) (12 * density), (int) (10 * density), (int) (12 * density), (int) (10 * density));
                et.setBackgroundResource(R.drawable.bg_rounded_edittext);
                if(currentQty > 0) et.setText(String.valueOf(currentQty));
                et.setTag(size + "|" + type + "|" + rate);

                int heightPx = (int) (48 * density);
                et.setLayoutParams(new LinearLayout.LayoutParams((int) (90 * density), heightPx));

                row.addView(tv);
                row.addView(et);
                contentLayout.addView(row);
            }
        }

        Button btnSave = new Button(this);
        btnSave.setText("Update Order Box");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4F46E5")));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (50 * density));
        btnLp.setMargins(0, (int) (18 * density), 0, 0);
        btnSave.setLayoutParams(btnLp);
        contentLayout.addView(btnSave);

        builder.setView(scrollView);
        AlertDialog dialog = builder.create();
        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnSave.setOnClickListener(v -> {
            try {
                JSONArray newItems = new JSONArray();
                for(int i = 1; i < contentLayout.getChildCount() - 1; i++) {
                    View child = contentLayout.getChildAt(i);
                    if(child instanceof LinearLayout) {
                        LinearLayout r = (LinearLayout) child;
                        EditText et = (EditText) r.getChildAt(1);
                        String qStr = et.getText().toString().trim();
                        int qty = qStr.isEmpty() ? 0 : Integer.parseInt(qStr);
                        if(qty > 0) {
                            String tag = et.getTag().toString();
                            String[] parts = tag.split("\\|");
                            JSONObject itm = new JSONObject();
                            itm.put("size", parts[0]);
                            itm.put("type", parts[1]);
                            itm.put("qty", qty);
                            itm.put("rate", Double.parseDouble(parts[2]));
                            newItems.put(itm);
                        }
                    }
                }

                if(newItems.length() == 0) {
                    Toast.makeText(this, "Quantity must be greater than 0 for at least one size", Toast.LENGTH_SHORT).show();
                    return;
                }

                order.put("items", newItems);
                saveDataToStorage();
                renderSavedOrders();
                Toast.makeText(this, "Order Updated Successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch(Exception e) {
                e.printStackTrace();
            }
        });

        dialog.show();
    }

    private void refreshSpinners() {
        ArrayList<String> names = new ArrayList<>();
        names.add("-- Choose Product --");
        for(JSONObject p : productList) {
            try { names.add(p.getString("name")); } catch(Exception e) {}
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerHomeProduct.setAdapter(adapter);
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
                row.setPadding(0, 6, 0, 6);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tv = new TextView(this);
                tv.setText("Size: " + size + " (" + type + ") - Rate: " + rate);
                tv.setTextColor(Color.parseColor("#334155"));
                tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                EditText et = new EditText(this);
                et.setHint("Qty");
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setPadding(12, 10, 12, 10);
                et.setBackgroundResource(R.drawable.bg_rounded_edittext);
                et.setTag(size + "|" + type + "|" + rate);
                
                int heightPx = (int) (48 * getResources().getDisplayMetrics().density);
                et.setLayoutParams(new LinearLayout.LayoutParams(220, heightPx));

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
                    String tag = et.getTag().toString();
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
        
        int grandTotalQty = 0;
        double grandTotalAmount = 0.0;

        float density = getResources().getDisplayMetrics().density;
        float cardRadiusPx = 24f * density;
        float tableRadiusPx = 18f * density;

        for(int index = 0; index < orderList.size(); index++) {
            final int oIndex = index;
            try {
                final JSONObject order = orderList.get(oIndex);
                boolean isPaid = order.optBoolean("isPaid", false);
                if(isPaid) continue;

                String pName = order.getString("productName");
                String date = order.optString("date", "");
                JSONArray items = order.getJSONArray("items");

                MaterialCardView card = new MaterialCardView(this);
                card.setRadius(cardRadiusPx);
                card.setCardElevation(4f * density);
                card.setStrokeWidth((int) (1 * density));
                card.setStrokeColor(Color.parseColor("#E2E8F0"));
                card.setCardBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, (int) (16 * density));
                card.setLayoutParams(lp);

                // Long-Press Listener for Home Screen Order Box
                card.setOnLongClickListener(v -> {
                    showHomeOrderOptionsDialog(order);
                    return true;
                });

                LinearLayout cardInner = new LinearLayout(this);
                cardInner.setOrientation(LinearLayout.VERTICAL);
                int padPx = (int) (18 * density);
                cardInner.setPadding(padPx, padPx, padPx, padPx);

                LinearLayout headerLayout = new LinearLayout(this);
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView tvHeading = new TextView(this);
                tvHeading.setText(pName);
                tvHeading.setTextSize(16);
                tvHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeading.setTextColor(Color.parseColor("#4F46E5"));
                tvHeading.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                headerLayout.addView(tvHeading);

                TextView tvDate = new TextView(this);
                tvDate.setText("Created: " + date);
                tvDate.setTextSize(11);
                tvDate.setTextColor(Color.parseColor("#64748B"));
                headerLayout.addView(tvDate);
                cardInner.addView(headerLayout);

                // Rounded Table Wrapper Container
                LinearLayout tableContainer = new LinearLayout(this);
                tableContainer.setOrientation(LinearLayout.VERTICAL);
                GradientDrawable containerBg = new GradientDrawable();
                containerBg.setShape(GradientDrawable.RECTANGLE);
                containerBg.setCornerRadius(tableRadiusPx);
                containerBg.setColor(Color.WHITE);
                containerBg.setStroke((int) (1 * density), Color.parseColor("#CBD5E1"));
                tableContainer.setBackground(containerBg);
                tableContainer.setClipToOutline(true);

                // Table Layout
                TableLayout table = new TableLayout(this);
                table.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                table.setStretchAllColumns(true);

                // Header Row (5 Columns: Size | Type | Qty | Rate | Amount)
                TableRow headerRow = new TableRow(this);
                headerRow.setBackgroundColor(Color.parseColor("#E0F2FE"));
                headerRow.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
                headerRow.addView(makeTableCell("Size", true, false));
                headerRow.addView(makeTableCell("Type", true, false));
                headerRow.addView(makeTableCell("Qty", true, false));
                headerRow.addView(makeTableCell("Rate", true, false));
                headerRow.addView(makeTableCell("Amount", true, false));
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
                    row.setBackgroundColor(i % 2 == 0 ? Color.parseColor("#FFFFFF") : Color.parseColor("#F8FAFC"));
                    row.setPadding(0, (int) (10 * density), 0, (int) (10 * density));
                    
                    row.addView(makeTableCell(size, false, false));
                    row.addView(makeTableCell(type, false, false));
                    row.addView(makeTableCell(String.valueOf(qty), false, false));
                    row.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", rate), false, false, COLOR_GREEN));
                    row.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", amount), false, false));
                    table.addView(row);
                }

                grandTotalQty += totalQty;
                grandTotalAmount += totalAmount;

                // Total Footer Row (Light Green Background, column-aligned)
                TableRow totalRow = new TableRow(this);
                totalRow.setBackgroundColor(Color.parseColor("#DCFCE7"));
                totalRow.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
                totalRow.addView(makeTableCell("Total", false, true));
                totalRow.addView(makeTableCell("", false, true));
                totalRow.addView(makeTableCell(String.valueOf(totalQty), false, true));
                totalRow.addView(makeTableCell("", false, true));
                totalRow.addView(makeTableCell(String.format(Locale.getDefault(), "%.1f", totalAmount), false, true, COLOR_GREEN));
                table.addView(totalRow);

                tableContainer.addView(table);
                cardInner.addView(tableContainer);

                CheckBox cbPaid = new CheckBox(this);
                cbPaid.setText("Mark as Paid");
                cbPaid.setTextColor(COLOR_GREEN);
                cbPaid.setTypeface(null, android.graphics.Typeface.BOLD);
                cbPaid.setPadding(0, (int) (8 * density), 0, 0);
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
                cardInner.addView(cbPaid);

                card.addView(cardInner);
                layoutSavedBoxes.addView(card);
            } catch(Exception e) { e.printStackTrace(); }
        }

        if(tvSummaryTotalQty != null && tvSummaryTotalAmount != null) {
            tvSummaryTotalQty.setText(String.valueOf(grandTotalQty));
            tvSummaryTotalAmount.setText(String.format(Locale.getDefault(), "%.1f", grandTotalAmount));
        }
    }

    private TextView makeTableCell(String text, boolean isHeader, boolean isTotal) {
        return makeTableCell(text, isHeader, isTotal, null);
    }

    private TextView makeTableCell(String text, boolean isHeader, boolean isTotal, Integer textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        int padH = (int) (12 * getResources().getDisplayMetrics().density);
        int padV = (int) (8 * getResources().getDisplayMetrics().density);
        tv.setPadding(padH, padV, padH, padV);
        tv.setGravity(Gravity.CENTER);
        
        if(isHeader) {
            tv.setTextColor(textColor != null ? textColor : Color.parseColor("#1E293B"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextSize(15f);
        } else if(isTotal) {
            tv.setTextColor(textColor != null ? textColor : Color.parseColor("#0F172A"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextSize(14f);
        } else {
            tv.setTextColor(textColor != null ? textColor : Color.parseColor("#334155"));
            tv.setTextSize(14f);
        }
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
