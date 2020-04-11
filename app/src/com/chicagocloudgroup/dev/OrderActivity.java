package com.chicagocloudgroup.dev;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import objects.Order;

public class OrderActivity extends SalesforceActivity {

    private RestClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup view
        setContentView(R.layout.activity_scrolling);

        final Order currentOrder = (Order) getIntent().getSerializableExtra("productRecord");

        Log.d("OrderActivity", currentOrder.Id);

        ArrayList<Map<String,Object>> itemDataList = new ArrayList<Map<String,Object>>();;

        int titleLen = currentOrder.listOfItems.size();
        for(int i =0; i < titleLen; i++) {
            Map<String,Object> listItemMap = new HashMap<String,Object>();
            listItemMap.put("title", currentOrder.listOfItems.get(i).ProductName);
            listItemMap.put("description", currentOrder.listOfItems.get(i).Description);
            itemDataList.add(listItemMap);
        }

        SimpleAdapter simpleAdapter = new SimpleAdapter(this, itemDataList,android.R.layout.simple_list_item_2, new String[]{"title","description"},new int[]{android.R.id.text1,android.R.id.text2});

        ListView listView = (ListView)findViewById(R.id.listViewExample);
        listView.setAdapter(simpleAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                Object clickItemObj = adapterView.getAdapter().getItem(index);
                HashMap clickItemMap = (HashMap)clickItemObj;
                String itemTitle = (String)clickItemMap.get("title");
                String itemDescription = (String)clickItemMap.get("description");

                Toast.makeText(OrderActivity.this, "You select item is  " + itemTitle + " , " + itemDescription, Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("OrdersActivity", currentOrder.Id);
                updateOrderToComplete(currentOrder.Id);
            }
        });
    }

    @Override
    public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client;
    }

    /**
     * Helper method to save details back to server
     * @param id
     */
    private void updateOrderToComplete(String id)  {

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("Status", "Completed");

        RestRequest restRequest;
        try {
            restRequest = RestRequest.getRequestForUpdate(getString(R.string.api_version), "Order", id, fields);
        } catch (Exception e) {
            Toast.makeText(OrderActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {

                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Log.d("OrderActivity", "result: " + result.getStatusCode());

                            // Failed due to validation rule
                            if(result.getStatusCode() == 400)
                            {
                                JSONArray jsonarray = new JSONArray(result.asString());
                                JSONObject jsnobject = jsonarray.getJSONObject(0);
                                Log.d("OrderActivity", jsnobject.getString("message"));
                                Toast.makeText(OrderActivity.this, jsnobject.getString("message"), Toast.LENGTH_LONG).show();
                            }
                            // 204 status code means it was saved correctly.
                            else {
                                Toast.makeText(OrderActivity.this, "Successfully updated", Toast.LENGTH_SHORT).show();
                                OrderActivity.this.finish();
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(OrderActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}

