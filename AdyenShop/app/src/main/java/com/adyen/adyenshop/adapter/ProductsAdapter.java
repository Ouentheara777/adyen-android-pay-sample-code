package com.adyen.adyenshop.adapter;

import android.content.Context;
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
import com.adyen.adyenshop.util.PreferencesUtil;

import java.util.List;

/**
 * Created by andrei on 3/8/16.
 */
public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductsViewHolder> {

    private Context context;

    private List<Product> mDataset;

    public ProductsAdapter(List<Product> myDataset, Context context) {
        this.mDataset = myDataset;
        this.context = context;
    }

    public static class ProductsViewHolder extends RecyclerView.ViewHolder {
        public CardView mProductCardView;
        public ImageView mProductIcon;
        public TextView mProductName;
        public TextView mProductPrice;

        public ProductsViewHolder(View itemView) {
            super(itemView);
            mProductCardView = (CardView)itemView.findViewById(R.id.product_card_view);
            mProductIcon = (ImageView)itemView.findViewById(R.id.product_icon);
            mProductName = (TextView)itemView.findViewById(R.id.product_name);
            mProductPrice = (TextView)itemView.findViewById(R.id.product_price);
        }
    }

    @Override
    public ProductsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        // create a new view
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.product_item, viewGroup, false);
        ProductsViewHolder viewHolder = new ProductsViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ProductsViewHolder productsViewHolder, int position) {
        productsViewHolder.mProductIcon.setImageResource(mDataset.get(position).getPhotoId());
        productsViewHolder.mProductIcon.setTag(mDataset.get(position).getPhotoId());
        productsViewHolder.mProductName.setText(mDataset.get(position).getName());
        String currency = PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD");
        productsViewHolder.mProductPrice.setText(CurrencyUtil.getCurrencySymbol(currency) + String.valueOf(mDataset.get(position).getPrice()));
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
