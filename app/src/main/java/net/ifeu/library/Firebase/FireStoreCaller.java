package net.ifeu.library.Firebase;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.Priority;

import net.ifeu.edicards.Constants.ConstantsFirecloud;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FireStoreCaller {

    public String getToken() throws JSONException {

        ANRequest request = AndroidNetworking.post(ConstantsFirecloud.FIRECLOUD_URL_TOKEN)
                .addBodyParameter("email", ConstantsFirecloud.FIRECLOUD_EMAIL)
                .addBodyParameter("password", ConstantsFirecloud.FIRECLOUD_PASSWORD)
                .addBodyParameter("returnSecureToken", "true")
                .setPriority(Priority.MEDIUM)
                .build();

        ANResponse<JSONObject> response = request.executeForJSONObject();

        if (response.isSuccess()) {
            return response.getResult().getString("idToken");
        }

        return "";
    }

    public ArticuloStockResponse getStock(String idToken) throws JSONException {

        ArticuloStockResponse articuloStockResponse = new ArticuloStockResponse();

        Map<String, ArticuloStock> list = new HashMap<>();
        ANRequest request = AndroidNetworking.get(ConstantsFirecloud.FIRECLOUD_URL_DATABASE)
                .addHeaders("Authorization", "Bearer " + idToken)
                .setPriority(Priority.MEDIUM)
                .build();

        ANResponse<JSONObject> response = request.executeForJSONObject();

        if (response.isSuccess()) {
            JSONArray result = response.getResult().getJSONArray("documents");
            JSONObject obj = (JSONObject) result.get(0);

            String jsonStringName = obj.getString("name");

            String jsonString = obj.getString("fields");
            JSONObject jsonObject = new JSONObject(jsonString);
            String jsonStringLista =jsonObject.getString("lista");
            JSONObject jsonObjectLista = new JSONObject(jsonStringLista);
            String jsonStringArrayValue =jsonObjectLista.getString("arrayValue");
            JSONObject jsonObjectArrayValue = new JSONObject(jsonStringArrayValue);
            JSONArray values = (JSONArray) jsonObjectArrayValue.get("values");

            for (int j = 0; j <= values.length()-1; j++) {

                ArticuloStock art = new ArticuloStock();
                String value = values.getString(j).substring(values.getString(j).indexOf(":")+1).replace("\\","").replace("\\/","").replace("{","").replace("}","");
                value = value.substring(1, value.length()-1);

                art.idArticulo = value.split(":")[0];
                art.descripcion = value.split(":")[1];
                art.stock = Boolean.parseBoolean(value.split(":")[2]);
                list.put(art.idArticulo, art);
            }

            articuloStockResponse.name =jsonStringName;
            articuloStockResponse.articulos = list;
            return articuloStockResponse;
        }

        return null;
    }

    public boolean createStock(String idToken, String name, Map<String, ArticuloStock> articulos) {

        String content = "";
        for (ArticuloStock stock : articulos.values()) {
            content = content + "{" + "'stringValue': '" + stock.idArticulo + ":" + stock.descripcion + ":" + stock.stock + "'},";
        }

        String body = "{" +
                "'fields': {" +
                "'lista': {" +
                "'arrayValue': { " +
                "'values': [ " +
                content.substring(0, content.length() - 1) +
                "]" +
                "}" +
                "}" +
                "}" +
                "}";

        ANRequest request = AndroidNetworking.patch(ConstantsFirecloud.FIRECLOUD_URL_BASE + name)
                .addHeaders("Authorization", "Bearer " + idToken)
                .addStringBody(body)
                .setPriority(Priority.MEDIUM)
                .build();

        ANResponse<JSONObject> response = request.executeForJSONObject();

        return response.isSuccess();
    }
}
