package com.td.exoplayerdemo.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.LinearLayout;

import com.whatjay.recyclerview.adapter.BaseSmartAdapter;
import com.whatjay.recyclerview.viewholder.SmarViewHolder;

import java.util.List;

import com.td.exoplayer.utils.Utils;
import com.td.exoplayerdemo.R;

/**
 * Created by office on 2017/8/17.
 */

public class TVListAdapter extends BaseSmartAdapter<String> {
    private Context context;
    private int currentPosition = 0;
    private int currentFocusPosition = 0;
    private String type;
    private Paint paint;
    private Rect rect;
    private LinearLayout.LayoutParams layoutParams;
    private String title = "";

    public TVListAdapter(Context context, int layoutId, List<String> lists, String type) {
        super(context, layoutId, lists);
        this.context = context;
        this.type = type;
        paint = new Paint();
        paint.setTextSize(Utils.sp2px(context, 26));
        paint.setFakeBoldText(true);
        rect = new Rect();
        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void bindData(SmarViewHolder holder, String string) {
        paint.getTextBounds(title, 0, title.length(), rect);
        layoutParams.width = Utils.dp2px(context, 50) + Utils.dp2px(context, 38);
        holder.getView(R.id.ll_list).setLayoutParams(layoutParams);
        holder.setText(R.id.tv_tv_name, string);
        if (currentPosition == currentFocusPosition && holder.getAdapterPosition() == currentPosition) {
            holder.getView(R.id.ll_list).requestFocus();
        }
        if (holder.getAdapterPosition() == currentPosition) {
            holder.setVisible(R.id.v_select);
        } else {
            holder.getView(R.id.v_select).setVisibility(View.INVISIBLE);
        }
        if (currentPosition != currentFocusPosition && holder.getAdapterPosition() == currentFocusPosition) {
            holder.getView(R.id.ll_list).requestFocus();
        }
    }

    public void setPosition(int position) {
        this.currentPosition = position;
        notifyDataSetChanged();
    }

    public void setCurrentFocusPosition(int position) {
        this.currentFocusPosition = position;
    }
}
