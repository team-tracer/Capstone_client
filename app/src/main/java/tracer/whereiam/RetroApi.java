package tracer.whereiam;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetroApi {
    static final String BASEURL="http://127.0.0.1:8080";
    @POST("/loadMap")
    Call<Scan_Res_Format> request(@Body JSONObject jsonObject);
}
