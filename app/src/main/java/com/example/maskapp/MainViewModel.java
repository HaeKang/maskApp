package com.example.maskapp;

import android.location.Location;
import android.util.Log;

import com.example.maskapp.model.Store;
import com.example.maskapp.model.StoreInfo;
import com.example.maskapp.repository.MaskService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class MainViewModel extends ViewModel {
    public Location location;   // 위치

    public void setLocation(Location location){
        this.location = location;
    }

    public Location getLocation(){
        return this.location;
    }

    private static final String TAG = "TAG_TEST";

    public MutableLiveData<List<Store>> itemLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>();

    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(MaskService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build();

    private MaskService service = retrofit.create(MaskService.class);



    public void  fecthStoreInfo(){
        // 로딩 시작
        loadingLiveData.setValue(true);

        // data get part
        service.fetchStoreInfo(location.getLatitude(), location.getLongitude()).enqueue(new Callback<StoreInfo>() {
            @Override
            // 성공
            public void onResponse(Call<StoreInfo> call, Response<StoreInfo> response) {
                List<Store> items = response.body().getStores()
                        .stream()
                        .filter(item -> item.getRemainStat() != null)
                        .filter(item -> !item.getRemainStat().equals("empty"))
                        .collect(Collectors.toList());

                for(Store store:items){
                    double distance = LocationDistance.distance(location.getLatitude(), location.getLongitude(),
                            store.getLat(), store.getLng(), "k");
                    store.setDistance(distance);
                }

                Collections.sort(items);

                itemLiveData.postValue(items);

                // 로딩 끝
                loadingLiveData.postValue(false);
            }

            @Override
            // 실패
            public void onFailure(Call<StoreInfo> call, Throwable t) {
                Log.e(TAG, "onFailure : ", t);
                itemLiveData.postValue(Collections.emptyList());    // 빈 걸로 셋팅

                // 로딩 끝
                loadingLiveData.postValue(false);
            }
        });

    }
}
