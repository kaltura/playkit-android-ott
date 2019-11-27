/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.providers.api.phoenix.services;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.request.MultiRequestBuilder;
import com.kaltura.playkit.providers.api.phoenix.PhoenixConfigs;

import static com.kaltura.playkit.utils.Consts.HTTP_METHOD_POST;

/**
 * @hide
 */
public class PhoenixService {

    public static JsonObject getPhoenixConfigParams(){
        JsonObject params = new JsonObject();
        params.addProperty("clientTag", PhoenixConfigs.ClientTag);
        params.addProperty("apiVersion",PhoenixConfigs.ApiVersion);

        return params;
    }

    public static MultiRequestBuilder getMultirequest(String baseUrl, @Nullable String ks){
        JsonObject params = getPhoenixConfigParams();
        if(!TextUtils.isEmpty(ks)) {
            params.addProperty("ks", ks);
        }
        return (MultiRequestBuilder) new MultiRequestBuilder().service("multirequest").method(HTTP_METHOD_POST).url(baseUrl).params(params);
    }
}
