package tracer.whereiam;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RetroApi {
    final static String BASEURL="http://13.209.43.170:8000";

    @FormUrlEncoded
    @POST("/post/loadMap")
    Call<Map_Res> loadMap(@Field("imgName") String imgName, @Field("posX") Integer posX, @Field("posY") Integer posY);
}