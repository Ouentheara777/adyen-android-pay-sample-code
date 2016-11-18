package com.adyen.adyenshop.adapter;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.adyen.adyenshop.R;
import com.adyen.adyenshop.model.Product;
import com.adyen.adyenshop.util.CurrencyUtil;

import java.util.List;

/**
 * Created by andrei on 3/9/16.
 */
public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.OrderItemsViewHolder> {

    private List<Product> mDataset;
    private String currency;

    public OrderItemsAdapter(List<Product> myDataset, String currency) {
        this.mDataset = myDataset;
        this.currency = currency;
    }

    public static class OrderItemsViewHolder extends RecyclerView.ViewHolder {
        public CardView mOrderItemCardView;
        public ImageView mOrderItemIcon;
        public TextView mOrderItemName;
        public TextView mOrderItemPrice;

        public OrderItemsViewHolder(View itemView) {
            super(itemView);
            mOrderItemCardView = (CardView)itemView.findViewById(R.id.order_item_card_view);
            mOrderItemIcon = (ImageView)itemView.findViewById(R.id.order_item_icon);
            mOrderItemName = (TextView)itemView.findViewById(R.id.order_item_name);
            mOrderItemPrice = (TextView)itemView.findViewById(R.id.order_item_price);
        }
    }

    @Override
    public OrderItemsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        // create a new view
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.order_list_item, viewGroup, false);
        OrderItemsViewHolder viewHolder = new OrderItemsViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(OrderItemsViewHolder orderItemsViewHolder, int position) {
        orderItemsViewHolder.mOrderItemIcon.setImageResource(mDataset.get(position).getPhotoId());
        orderItemsViewHolder.mOrderItemIcon.setTag(mDataset.get(position).getPhotoId());
        orderItemsViewHolder.mOrderItemName.setText(mDataset.get(position).getName());
        orderItemsViewHolder.mOrderItemPrice.setText(CurrencyUtil.getCurrencySymbol(currency) + String.valueOf(mDataset.get(position).getPrice()));
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}
