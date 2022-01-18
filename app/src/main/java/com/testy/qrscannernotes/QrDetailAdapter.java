package com.testy.qrscannernotes;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class QrDetailAdapter extends ArrayAdapter<QrDataModel> {
    List<QrDataModel> QrObject;
    public QrDetailAdapter(Context context, int resource, List<QrDataModel> objects) {
        super(context, resource, objects);
        this.QrObject = objects;
    }

    public void remove(int position) {
        QrDataModel qrDataModel = getItem(position);
        qrDataModel.getQrText();
        QrObject.remove(position);
        MainActivity.sendUniqueKey(qrDataModel.getSpecDate());
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.qr_list_items, parent, false);
        TextView qrText = convertView.findViewById(R.id.qrText);
        TextView qrDate = convertView.findViewById(R.id.qrDate);
        QrDataModel qrDataModel = getItem(position);
        qrText.setText(qrDataModel.getQrText());
        qrDate.setText(qrDataModel.getDate());
        return convertView;
    }
}
