package com.example.maskapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;

import com.example.maskapp.model.Store;
import com.example.maskapp.model.StoreInfo;
import com.example.maskapp.repository.MaskService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MapActivity extends AppCompatActivity implements  OnMapReadyCallback, NaverMap.OnCameraChangeListener, NaverMap.OnCameraIdleListener {
    private static final int ACCESS_LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationSource locationSource;
    private NaverMap naverMap;
    private List<Marker> markerList = new ArrayList<Marker>();
    private boolean isCameraAnimated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        locationSource = new FusedLocationSource(this, 100);
        naverMap.setLocationSource(locationSource);
        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        naverMap.addOnCameraChangeListener(this);
        naverMap.addOnCameraIdleListener(this);

        LatLng mapCenter = naverMap.getCameraPosition().target;
        //fetchStoreSale(mapCenter.latitude, mapCenter.longitude);

        fetchStoreSale(37.6254368, 127.0164099);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ACCESS_LOCATION_PERMISSION_REQUEST_CODE:
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
        }
    }

    public void fetchStoreSale(double lat, double lng){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MaskService.BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build();

        MaskService service = retrofit.create(MaskService.class);

        // data get part
        service.fetchStoreInfo(lat,lng).enqueue(new Callback<StoreInfo>() {
            @Override
            // 성공
            public void onResponse(Call<StoreInfo> call, Response<StoreInfo> response) {
                List<Store> items = response.body().getStores()
                        .stream()
                        .filter(item -> item.getRemainStat() != null)
                        .filter(item -> !item.getRemainStat().equals("empty"))
                        .collect(Collectors.toList());

                Log.d("test", "here before update");
                updateMapMarkers(items);
            }

            @Override
            // 실패
            public void onFailure(Call<StoreInfo> call, Throwable t) {
                Log.e("test", "onFailure : ", t);
            }
        });
    }

    private void updateMapMarkers(List<Store> result) {
        resetMarkerList();
        Log.d("test", "here after update");
        for(Store store:result){
            Marker marker = new Marker();
            marker.setTag(store);
            marker.setPosition(new LatLng(store.getLat(), store.getLng()));
            marker.setAnchor(new PointF(0.5f, 1.0f));
            marker.setMap(naverMap);
            markerList.add(marker);
        }
    }

    private void resetMarkerList() {
        if (markerList != null && markerList.size() > 0) {
            for (Marker marker : markerList) {
                marker.setMap(null);
            }
            markerList.clear();
        }
    }

    @Override
    public void onCameraChange(int reason, boolean animated) {
        isCameraAnimated = animated;
    }

    // 지도 움직일 시 다시 정보 받아옴
    @Override
    public void onCameraIdle() {
        if (isCameraAnimated) {
            LatLng mapCenter = naverMap.getCameraPosition().target;
            fetchStoreSale(mapCenter.latitude, mapCenter.longitude);
        }
    }
}