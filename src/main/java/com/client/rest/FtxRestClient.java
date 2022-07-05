package com.client.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class FtxRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String currency;

    public FtxRestClient(String currency){
        this.currency = currency;
    }

    public void connect(){

        HttpClient client = HttpClients.createDefault();
        HttpUriRequest request = RequestBuilder.get()
                .setUri("https://ftx.com/api/futures/"+currency+"-PERP/stats")
                .build();

        JSONObject jsonResponse;
        try {
            HttpResponse response = client.execute(request);

            jsonResponse = EntityHelper.getJson(response.getEntity());

            //{"result":{"volume":13950048.07,"nextFundingRate":-0.000016,"openInterest":5551535.71,"nextFundingTime":"2022-06-29T17:00:00+00:00"},"success":true}
            LOGGER.info(jsonResponse.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
