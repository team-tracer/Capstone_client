package tracer.whereiam;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetroApi {
    static final String BASEURL="http://192.168.0.11:8000";

    @POST("loadMap")
    Call<Scan_Res_Format> request(@Body Scan_Req_Format scan_req_format);
    //, @Query("posX") Integer posX, @Query("posY") Integer posY
}
