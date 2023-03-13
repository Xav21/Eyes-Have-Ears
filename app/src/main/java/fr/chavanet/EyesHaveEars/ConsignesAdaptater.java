package fr.chavanet.EyesHaveEars;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import com.example.EyesHaveEars.R;

import java.util.List;

public class ConsignesAdaptater extends ArrayAdapter<ConsignesModel> {

    public ConsignesAdaptater(Context context, List<ConsignesModel> item){
        super(context, 0, item);
    }

    @SuppressLint("WrongConstant")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate( R.layout.une_consigne, parent, false);
        }

        ItemViewHolder itemViewHolder = (ItemViewHolder) convertView.getTag();

        if(itemViewHolder == null){
            itemViewHolder = new ItemViewHolder();
            itemViewHolder.date = convertView.findViewById(R.id.heure);
            itemViewHolder.name = convertView.findViewById(R.id.name);
            itemViewHolder.text = convertView.findViewById(R.id.textMessage);
            itemViewHolder.status = convertView.findViewById(R.id.status);
            //itemViewHolder.color = convertView.findViewById(R.id.MasterBackground);
            itemViewHolder.color = convertView.findViewById(R.id.background);
            itemViewHolder.progressBar = convertView.findViewById(R.id.progressBar);
            itemViewHolder.okIcon = convertView.findViewById(R.id.okIcon);
            itemViewHolder.nokIcon = convertView.findViewById(R.id.nokIcon);


            convertView.setTag(itemViewHolder);
        }

        ConsignesModel item = getItem(position);

        itemViewHolder.date.setText(item.getDate());
        itemViewHolder.name.setText(item.getName());
        itemViewHolder.text.setText(item.getText());
        itemViewHolder.status.setText(item.getStatus());
        itemViewHolder.color.setBackgroundColor(item.getColor());
        itemViewHolder.progressBar.setVisibility(item.getProgresBar());
        itemViewHolder.okIcon.setVisibility(item.getOkIcon());
        itemViewHolder.nokIcon.setVisibility(item.getNokIcon());


        return convertView;
    }

    private static class ItemViewHolder{
        public TextView date;
        public TextView name;
        public TextView text;
        public TextView status;
        public RelativeLayout color;
        public ProgressBar progressBar;
        public ImageView okIcon;
        public ImageView nokIcon;


    }
}