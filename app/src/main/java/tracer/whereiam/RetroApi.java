package tracer.whereiam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RetroApi {
    final static String BASEURL="http://13.209.43.170:8000";

    @FormUrlEncoded
    @POST("/post/loadMap")
    Call<Map_Res> loadMap(@Field("imgName") String imgName, @Field("posX") Integer posX, @Field("posY") Integer posY);

    @FormUrlEncoded
    @POST("/post/signup")
    Call<Void> isMember(@FieldMap HashMap<String, String> hashMap);

    @FormUrlEncoded
    @POST("/post/userDrop")
    Call<Void> requestDelUser(@Field("userID") String id);

    @FormUrlEncoded
    @POST("/post/acceptFrd")
    Call<Frd_Res> acceptFrd(@Field("fromID") String id1, @Field("toID") String id2);

    @FormUrlEncoded
    @POST("/post/loadFrd")
    Call<List<ListViewItem>> loadFrd(@Field("req_id") String id);
}