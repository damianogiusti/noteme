package it.tsamstudio.noteme;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class NotesRecyclerViewAdapter extends RecyclerView.Adapter<NotesRecyclerViewAdapter.DataObjectHolder> {

    private ArrayList<Nota> mDataset;
    private static MyClickListener myClickListener;

    public static class DataObjectHolder extends RecyclerView.ViewHolder
            implements View
            .OnClickListener {
        TextView title;
        TextView content;
        TextView tag;
        TextView expirationDate;
        ImageView micImg;
        ImageView imgBackground;
        CardView card;

        public DataObjectHolder(View itemView) {
            super(itemView);
            card = (CardView) itemView.findViewById(R.id.card_view);
            title = (TextView) itemView.findViewById(R.id.noteTitle);
            content = (TextView) itemView.findViewById(R.id.noteContent);
            tag = (TextView) itemView.findViewById(R.id.tag);
            expirationDate = (TextView) itemView.findViewById(R.id.expireDate);
            micImg = (ImageView) itemView.findViewById(R.id.micImgView);
            imgBackground = (ImageView)itemView.findViewById(R.id.imgBackground);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    public NotesRecyclerViewAdapter(ArrayList<Nota> myDataset) {
        mDataset = myDataset;
    }

    @Override
    public DataObjectHolder onCreateViewHolder(ViewGroup parent,
                                               int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_cardview, parent, false);

        DataObjectHolder dataObjectHolder = new DataObjectHolder(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(final DataObjectHolder holder, int position) {
        holder.title.setText(mDataset.get(position).getTitle());
        holder.content.setText(mDataset.get(position).getText());
        holder.tag.setText(mDataset.get(position).getTag());
        Date d = mDataset.get(position).getLastModifiedDate();
        SimpleDateFormat sd = new SimpleDateFormat("dd MMMM yyyy hh:mm");

        String date = sd.format(d);
        holder.expirationDate.setText(date);
        holder.imgBackground.setImageDrawable(null);

        if (mDataset.get(position).getAudio() != null) {
            holder.micImg.setVisibility(View.VISIBLE);
            holder.micImg.setImageResource(R.drawable.ic_mic_card);
        }
        if (mDataset.get(position).getImage() != null) {
            Log.d("card con sfondo", mDataset.get(position).getImage());
            final Drawable drawable = Drawable.createFromPath(mDataset.get(position).getImage());

//            PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
            drawable.setColorFilter(Color.BLACK, PorterDuff.Mode.LIGHTEN);

            holder.card.post(new Runnable() {
                @Override
                public void run() {
//                    holder.imgBackground.setImageDrawable(NoteMeUtils.resizeDrawable(drawable, holder.card.getHeight(), holder.card.getWidth()));
                    holder.imgBackground.setImageDrawable(drawable);
                }
            });
//            holder.card.setAlpha(0.5f);

        }
    }

    public void addItem(Nota dataObj, int index) {
        mDataset.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void updateItem(Nota dataObj, int index) {
        mDataset.set(index, dataObj);
        notifyItemChanged(index);
    }

    public void deleteItem(int index) {
        mDataset.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public interface MyClickListener {
        public void onItemClick(int position, View v);
    }
}
