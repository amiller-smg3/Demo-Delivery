/*
 * Copyright (c) 2012-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.chicagocloudgroup.dev;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import objects.Order;
import objects.OrderItem;

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {

    private RestClient client;
	private MyAdapter adapter;
	private ArrayList<String> listAdapter;
	private RecyclerView recyclerView;
	private ArrayList<Order> listOfOrders;
    private ArrayList<OrderItem> listOfOrderItems;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup view
		setContentView(R.layout.main);

		// data to populate the RecyclerView with
		listAdapter = new ArrayList<>();
        listOfOrders = new ArrayList<>();
        listOfOrderItems = new ArrayList<>();

		// set up the RecyclerView
		recyclerView = findViewById(R.id.orderList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new MyAdapter(this, listAdapter);
		adapter.setClickListener(new MyAdapter.ItemClickListener() {
			@Override
			public void onItemClick(View view, int position) {

			    Order currentOrder = listOfOrders.get(position);

                Intent intent = new Intent(getApplicationContext(), OrderActivity.class);
                intent.putExtra("productRecord", currentOrder); // Want to pass only the product and items below it
                startActivity(intent);
			}
		});
		recyclerView.setAdapter(adapter);
	}
	
	@Override 
	public void onResume() {
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);
		
		super.onResume();
	}
	
	@Override
	public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client;

        try {
			onFecthOrders();
            onFecthOrderProducts();
		}catch(UnsupportedEncodingException e){
        	Log.e("Main.onResume",e.getMessage());
		};
		// Show everything
		findViewById(R.id.root).setVisibility(View.VISIBLE);
	}

	/**
	 * Called when "Logout" button is clicked. 
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		SalesforceSDKManager.getInstance().logout(this);
	}

    /**
     * Called when "Fetch Order Products" button is clicked
     *
     * @throws UnsupportedEncodingException
     */
    public void onFecthOrderProducts() throws UnsupportedEncodingException {

        String soql = "SELECT OrderItemNumber, Description, OrderId, Product2Id, Product2.Name, Quantity, TotalPrice, UnitPrice FROM OrderItem";

        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        client.sendAsync(restRequest, new AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listOfOrderItems.clear();

                            JSONObject jsnobject = new JSONObject(result.toString());
                            JSONArray records = jsnobject.getJSONArray("records");

                            for (int i = 0; i < records.length(); i++) {

                                // 1. Map all of the values to the runtime object
                                OrderItem tempOrderItem = new OrderItem();

                                tempOrderItem.OrderItemNumber = records.getJSONObject(i).getString("OrderItemNumber");
                                tempOrderItem.OrderId = records.getJSONObject(i).getString("OrderId");
                                tempOrderItem.Product2Id = records.getJSONObject(i).getString("Product2Id");
                                tempOrderItem.Description = records.getJSONObject(i).getString("Description");

                                tempOrderItem.UnitPrice = records.getJSONObject(i).getDouble("UnitPrice");
                                tempOrderItem.Quantity = records.getJSONObject(i).getDouble("Quantity");
                                tempOrderItem.TotalPrice = records.getJSONObject(i).getDouble("TotalPrice");

                                JSONObject productJSON = records.getJSONObject(i).getJSONObject("Product2");
                                tempOrderItem.ProductName = productJSON.getString("Name");

                                Log.d("PRODUCT PRINTING", tempOrderItem.OrderId);

                                // 2. Map to existing Orders Array for better storage access
                                for(Order tempOrder : listOfOrders)
                                {
                                    if(tempOrder.Id.equals(tempOrderItem.OrderId))
                                    {
                                        if(tempOrder.listOfItems == null)
                                        {
                                            tempOrder.listOfItems = new ArrayList<>();
                                        }

                                        tempOrder.listOfItems.add(tempOrderItem);
                                    }
                                }

                                listOfOrderItems.add(tempOrderItem);
                            }
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                MainActivity.this.getString(R.string.sf__generic_error, exception.toString()),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

	/**
	 * Called when "Fetch Order" button is clicked
	 *
	 * @throws UnsupportedEncodingException
	 */
	public void onFecthOrders() throws UnsupportedEncodingException {

        String soql = "SELECT Id, OrderNumber FROM Order";

        RestRequest restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);

        client.sendAsync(restRequest, new AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, final RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listAdapter.clear();

                            JSONObject jsnobject = new JSONObject(result.toString());

                            Log.d("ORDER PRINTING", jsnobject.toString());

                            JSONArray records = jsnobject.getJSONArray("records");

                            for (int i = 0; i < records.length(); i++) {

                                listAdapter.add(records.getJSONObject(i).getString("OrderNumber"));

                                Order tempOrder = new Order();

                                tempOrder.Id = records.getJSONObject(i).getString("Id");
                                tempOrder.orderNumber = records.getJSONObject(i).getString("OrderNumber");

                                Log.d("ORDER PRINTING", tempOrder.Id);

                                listOfOrders.add(tempOrder);
                            }
                        } catch (Exception e) {
                            onError(e);
                        }
                    }
                });
            }

            @Override
            public void onError(final Exception exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                MainActivity.this.getString(R.string.sf__generic_error, exception.toString()),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
