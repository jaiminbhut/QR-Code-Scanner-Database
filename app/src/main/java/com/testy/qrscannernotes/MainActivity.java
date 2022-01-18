package com.testy.qrscannernotes;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.adapter.ListViewAdapter;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    static SQLiteDatabase myDatabase;
    ListView qrDetailList;
    List<QrDataModel> QrObject = new ArrayList<>();
    QrDetailAdapter qrDetailAdapter;
    Cursor c;
    SearchView inputSearch;
    List<QrDataModel> tempList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        qrDetailAdapter = new QrDetailAdapter(this, R.layout.qr_list_items, QrObject);
        qrDetailList.setAdapter(qrDetailAdapter);
        qrDetailAdapter.notifyDataSetChanged();
        performSql();

        //Main code for search view
        tempList.addAll(QrObject);
        inputSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                QrObject.clear();
                for (QrDataModel qr : tempList) {
                    if (qr.getQrText().contains(s)) {
                        //contains
                        QrObject.add(qr);
                    } else if (s.length() == 0) {
                        Toast.makeText(getApplicationContext(), "list is clear", Toast.LENGTH_LONG).show();
                        tempList.addAll(QrObject);
                    }
                }
                qrDetailAdapter.notifyDataSetChanged();
                return true;
            }
        });

        //To perform delete on swipe in the list
        final SwipeToDismissTouchListener<ListViewAdapter> touchListener =
                new SwipeToDismissTouchListener<>(
                        new ListViewAdapter(qrDetailList),
                        new SwipeToDismissTouchListener.DismissCallbacks<ListViewAdapter>() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListViewAdapter view, int position) {
                                qrDetailAdapter.remove(position);
                            }
                        });

        qrDetailList.setOnTouchListener(touchListener);
        qrDetailList.setOnScrollListener((AbsListView.OnScrollListener) touchListener.makeScrollListener());
        qrDetailList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (touchListener.existPendingDismisses()) {
                    touchListener.undoPendingDismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Position " + position, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    public void initializeViews() {
        qrDetailList = (ListView) findViewById(R.id.qrDetailList);
        qrDetailList.setTextFilterEnabled(true);
        inputSearch = (SearchView) findViewById(R.id.inputSearch);
    }

    public void scan() {
        startActivityForResult(new Intent(this, BarcodeActivity.class), 10);
    }

    public void handleCodeAdd(Intent data) {
        if (data.hasExtra("item")) {
            QrDataModel projectListModel = (QrDataModel) data.getSerializableExtra("item");
            QrObject.add(projectListModel);
            qrDetailAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode != RESULT_CANCELED) {
                handleCodeAdd(data);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.scanQrCode:
                scan();
                break;
            case R.id.sortByDate:
                orderByDate();
                break;
            case R.id.sortByName:
                orderByName();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void orderByDate() {
        Toast.makeText(getApplicationContext(), "order by date working", Toast.LENGTH_LONG).show();
        c = myDatabase.rawQuery("SELECT * FROM lastfourth ORDER BY date Desc", null);
        int nameIndex = c.getColumnIndex("name");
        int dateIndex = c.getColumnIndex("date");
        int dateIDIndex = c.getColumnIndex("spec");
        if (c.moveToFirst()) {
            do {
                Log.i("user-name", c.getString(nameIndex));
                Log.i("date id ", c.getString(dateIDIndex));
                QrDataModel qrDataModel = new QrDataModel(c.getString(nameIndex), c.getString(dateIndex), c.getString(dateIDIndex));
                QrObject.add(qrDataModel);
                qrDetailAdapter.notifyDataSetChanged();
                Log.i("user-age", c.getString(dateIndex));
            } while (c.moveToNext());
        }
    }

    public void orderByName() {
        Toast.makeText(getApplicationContext(), "order by date working", Toast.LENGTH_LONG).show();
        c = myDatabase.rawQuery("SELECT * FROM lastfourth ORDER BY name", null);
        int nameIndex = c.getColumnIndex("name");
        int dateIndex = c.getColumnIndex("date");
        int dateIDIndex = c.getColumnIndex("spec");

        if (c.moveToFirst()) {
            do {
                QrDataModel qrDataModel = new QrDataModel(c.getString(nameIndex), c.getString(dateIndex), c.getString(dateIDIndex));
                QrObject.add(qrDataModel);
                qrDetailAdapter.notifyDataSetChanged();
            } while (c.moveToNext());
        }
    }

    public static void sendUniqueKey(String s) {
        String sql = "DELETE FROM lastfourth WHERE spec = ? ";
        SQLiteStatement statement = myDatabase.compileStatement(sql);
        statement.bindString(1, s);
        statement.execute();
    }

    public void performSql() {
        myDatabase = this.openOrCreateDatabase("Users", MODE_PRIVATE, null);
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS lastfourth (name VARCHAR , date VARCHAR , spec VARCHAR)");
        showDatabaseInList();
    }

    public void showDatabaseInList() {
        try {
            c = myDatabase.rawQuery("SELECT * FROM lastfourth", null);
            int nameIndex = c.getColumnIndex("name");
            int dateIndex = c.getColumnIndex("date");
            int dateIDIndex = c.getColumnIndex("spec");

            if (c.moveToFirst()) {
                do {
                    QrDataModel qrDataModel = new QrDataModel(c.getString(nameIndex), c.getString(dateIndex), c.getString(dateIDIndex));
                    QrObject.add(qrDataModel);
                    qrDetailAdapter.notifyDataSetChanged();
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
