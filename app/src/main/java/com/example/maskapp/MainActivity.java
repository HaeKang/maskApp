package com.example.maskapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maskapp.model.Store;
import com.example.maskapp.model.StoreInfo;
import com.example.maskapp.repository.MaskService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MainViewModel viewModel;

    private FusedLocationProviderClient fusedLocationClient; // 위치

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // 현재 위치
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // TedPermission
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            // permission 승인 시
            public void onPermissionGranted() {
                performAction();
            }

            @Override
            // permission 거부 시
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }


        };
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                .check();
    }


    @SuppressLint("MissingPermission")
    private void performAction() {
        // location으로 현재 위치 받음
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if(location != null){
                     Log.d(TAG, "performAction : " + location.getLatitude());
                     Log.d(TAG, "performAction : " + location.getLongitude());

                     location.setLatitude(37.6254368);
                     location.setLongitude(127.0164099);
                     viewModel.setLocation(location);
                     viewModel.fecthStoreInfo();
                    }
                });

        // 리사이클러뷰
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        final StoreAdapter adapter = new StoreAdapter();
        recyclerView.setAdapter(adapter);

        // UI 변경 감지
        viewModel.itemLiveData.observe(this, stores -> {
            adapter.updateItems(stores);
            getSupportActionBar().setTitle("마스크 재고 있는 곳 " + stores.size());
        });

        // 로딩 바
        viewModel.loadingLiveData.observe(this, isLoading -> {
            if (isLoading) {
                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
            }
        });
    }

    // action bar icon add
    public boolean onCreateOptionMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // menu에서 item 클릭 시 이벤트
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // refresh 요청
                viewModel.fecthStoreInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

// recyclerview adapter
class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    // 아이템 뷰 정보 클래스
    static class StoreViewHolder extends RecyclerView.ViewHolder{
        TextView nameTextView;
        TextView distanceTextView;
        TextView addressTextView;
        TextView remainTextView;
        TextView countTextView;

        public StoreViewHolder(@NonNull View itemView){
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_text_view);
            distanceTextView = itemView.findViewById(R.id.distance_text_view);
            addressTextView = itemView.findViewById(R.id.addr_text_view);
            remainTextView = itemView.findViewById(R.id.remain_text_view);
            countTextView = itemView.findViewById(R.id.count_text_view);
        }
    }

    private List<Store> mItems = new ArrayList<>();

    public void updateItems(List<Store> items){
        mItems = items;
        notifyDataSetChanged(); // ui 갱신
    }


    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int i) {
        Store store = mItems.get(i);

        holder.nameTextView.setText(store.getName());
        holder.addressTextView.setText(store.getAddr());
        holder.distanceTextView.setText(String.format("%.2fkm", store.getDistance()));

        String count_text = "100개 이상";
        String remain_text = "충분";
        int color = Color.GREEN;
        switch (store.getRemainStat()){
            case "plenty":
                count_text = "100개 이상";
                remain_text = "충분";
                color = Color.GREEN;
                break;
            case "some":
                count_text = "30개 이상";
                remain_text = "여유";
                color = Color.BLUE;
                break;
            case "few":
                count_text = "2개 이상";
                remain_text = "매진 임박";
                color = Color.RED;
                break;
            case "empty":
                count_text = "1개 이하";
                remain_text = "재고 없음";
                color = Color.DKGRAY;
                break;
        }

        holder.remainTextView.setText(remain_text);
        holder.countTextView.setText(count_text);
        holder.remainTextView.setTextColor(color);
        holder.countTextView.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}