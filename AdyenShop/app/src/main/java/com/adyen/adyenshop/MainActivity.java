package com.adyen.adyenshop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.adyen.adyenshop.adapter.ProductsAdapter;
import com.adyen.adyenshop.listener.RecyclerItemClickListener;
import com.adyen.adyenshop.model.Product;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String tag = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView itemsCount;
    private FloatingActionButton cartFab;

    private ProductsAdapter productsAdapter;

    private Context context;

    private List<Product> itemsInCartList = new ArrayList<Product>();
    private int itemsInCart;
    private float totalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState != null) {
            itemsInCart = savedInstanceState.getInt("itemsInCart");
            itemsInCartList = savedInstanceState.getParcelableArrayList("itemsInCartList");
            totalPrice = savedInstanceState.getFloat("totalPrice");
        } else {
            itemsInCart = 0;
            totalPrice = 0;
        }

        cartFab = (FloatingActionButton)findViewById(R.id.fab);
        mRecyclerView = (RecyclerView) findViewById(R.id.products_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        productsAdapter = new ProductsAdapter(initializeProductListData(this));
        mRecyclerView.setAdapter(productsAdapter);

        initializeView();
        getWindow().setStatusBarColor(getResources().getColor(R.color.black));

        cartFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proceedToCheckout();
            }
        });
    }

    public void initializeView() {
        context = this;
        itemsCount = (TextView) findViewById(R.id.items_count);
        itemsCount.setText(String.valueOf(itemsInCart));
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(context, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        itemsInCart++;
                        ImageView productIcon = (ImageView) view.findViewById(R.id.product_icon);
                        TextView productName = (TextView) view.findViewById(R.id.product_name);
                        TextView productPrice = (TextView) view.findViewById(R.id.product_price);

                        final Product book = new Product(productName.getText().toString(),
                                Double.valueOf(productPrice.getText().toString().replaceAll("\\$", "")),
                                (Integer) productIcon.getTag());
                        final float bookPriceD = Float.valueOf(productPrice.getText().toString().replaceAll("\\$", ""));
                        totalPrice = totalPrice + bookPriceD;
                        itemsInCartList.add(book);
                        initializeView();
                    }
                })
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("itemsInCart", itemsInCart);
        outState.putFloat("totalPrice", totalPrice);
        outState.putParcelableArrayList("itemsInCartList", new ArrayList<>(itemsInCartList));
    }

    public void proceedToCheckout() {
        Intent intent = new Intent(this, OrderConfirmationActivity.class);
        intent.putExtra("itemsInCart", itemsInCartList.toArray(new Product[itemsInCartList.size()]));
        intent.putExtra("totalPrice", totalPrice);
        startActivity(intent);
    }

    public List<Product> initializeProductListData(Context context) {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Notepad", 0.11, R.mipmap.notepad));
        products.add(new Product("Watch", 0.11, R.mipmap.watch));
        products.add(new Product("Ruler", 0.11, R.mipmap.ruler));
        products.add(new Product("Puzzle", 0.11, R.mipmap.puzzle));
        products.add(new Product("Pencil", 0.11, R.mipmap.pencil));
        products.add(new Product("Pen", 0.11, R.mipmap.pen));
        products.add(new Product("Box cutter", 0.11, R.mipmap.cutter));
        return products;
    }
}
