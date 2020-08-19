package in.kashewdevelopers.airdistance;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class HistoryAdapter extends CursorAdapter {

    private LayoutInflater cursorInflator;

    HistoryAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        cursorInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return cursorInflator.inflate(R.layout.history_list_item, null);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        ConstraintLayout parent = view.findViewById(R.id.parent);
        TextView srcTV = view.findViewById(R.id.src);
        TextView dstTV = view.findViewById(R.id.dst);
        TextView srcLLTV = view.findViewById(R.id.srcLL);
        TextView dstLLTV = view.findViewById(R.id.dstLL);
        TextView distanceTV = view.findViewById(R.id.distance);
        ImageView deleteIcon = view.findViewById(R.id.delete_icon);

        String src = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.SRC_NAME));
        String dst = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DST_NAME));
        String srcLL = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.SRC_LL));
        String dstLL = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DST_LL));
        String distance = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.DISTANCE));
        final String hashCode = cursor.getString(cursor.getColumnIndex(HistoryDbHelper.HASH));

        srcTV.setText(src);
        dstTV.setText(dst);
        srcLLTV.setText(srcLL);
        dstLLTV.setText(dstLL);
        distanceTV.setText(distance);

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onHistoryClickListener != null) {
                    onHistoryClickListener.onHistoryClickListener(view);
                }
            }
        });

        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( onDeleteClickListener != null) {
                    onDeleteClickListener.onDeleteClickListener(hashCode);
                }
            }
        });
    }

    void setOnHistoryClickListener(OnHistoryClickListener onHistoryClickListener) {
        this.onHistoryClickListener = onHistoryClickListener;
    }

    void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    interface OnHistoryClickListener {
        void onHistoryClickListener(View view);
    }

    private OnHistoryClickListener onHistoryClickListener;

    interface OnDeleteClickListener {
        void onDeleteClickListener(String hashCode);
    }

    private OnDeleteClickListener onDeleteClickListener;

}
