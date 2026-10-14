package be.submanifold.pentelive;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import be.submanifold.pentelive.billing.BillingCodes;
import be.submanifold.pentelive.billing.BillingManager;
import be.submanifold.pentelive.billing.Products;
import be.submanifold.pentelive.billing.PurchaseSender;
import be.submanifold.pentelive.billing.StoreCards;
import be.submanifold.pentelive.billing.StoreInfo;
import be.submanifold.pentelive.net.AuthedHttp;

/** Native Google Play store screen (spec 3.4); replaces the PayPal subscriptions WebView. */
public class StoreActivity extends AppCompatActivity {

    private static final String TAG = "StoreActivity";
    static final String MANAGE_SUBSCRIPTION_URL =
            "https://play.google.com/store/account/subscriptions?sku=yearly_subscription&package=be.submanifold.pentelive";
    private static final String PRIVACY_URL = "https://www.pente.org/help/helpWindow.jsp?file=privacyPolicy";
    private static final String EULA_URL = "https://www.pente.org/help/helpWindow.jsp?file=eula";
    private static final String NO_PRICE = "–";

    private StoreInfo info;
    private boolean infoFailed;
    private boolean productsLoaded;
    private int productsCode = BillingCodes.OK;
    private Map<String, String> prices;
    /** False until the first onResume; onCreate already loads the info for that one. */
    private boolean resumedBefore;
    /** Reloads the server info after a grant; one instance so onDestroy removes only its own. */
    private final Runnable onEntitlementChanged = this::loadInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(getString(R.string.subscribe_pente_org));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        findViewById(R.id.buyYearlyButton).setOnClickListener(v -> buy(Products.YEARLY));
        findViewById(R.id.buyMonthPassButton).setOnClickListener(v -> buy(Products.MONTH_PASS));
        findViewById(R.id.buyVacation10Button).setOnClickListener(v -> buy(Products.VACATION_10));
        findViewById(R.id.buyVacation30Button).setOnClickListener(v -> buy(Products.VACATION_30));
        findViewById(R.id.buyVacation60Button).setOnClickListener(v -> buy(Products.VACATION_60));
        findViewById(R.id.manageSubscriptionButton).setOnClickListener(v -> openExternal(MANAGE_SUBSCRIPTION_URL));
        findViewById(R.id.privacyPolicyButton).setOnClickListener(v -> openInApp(PRIVACY_URL));
        findViewById(R.id.eulaButton).setOnClickListener(v -> openInApp(EULA_URL));
        ((TextView) findViewById(R.id.vacationNote)).setText(
                getString(R.string.store_vacation_note, Calendar.getInstance().get(Calendar.YEAR) + 1));

        render();
        loadInfo();
        billing().loadProducts((code, loadedPrices) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            productsCode = code;
            prices = loadedPrices;
            productsLoaded = true;
            render();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        billing().attach(this);
        billing().setEntitlementListener(onEntitlementChanged);
        if (resumedBefore) {
            // Back from "Manage subscription" in the Play app (or elsewhere): the server may
            // know of a cancellation or change, so the cards must not stay stale.
            loadInfo();
        }
        resumedBefore = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    protected void onDestroy() {
        billing().clearEntitlementListener(onEntitlementChanged);
        billing().detach(this);
        super.onDestroy();
    }

    private static BillingManager billing() {
        return MyApplication.billing();
    }

    private void loadInfo() {
        new BackgroundTask<Void, StoreInfo>() {
            @Override
            protected StoreInfo doInBackground(Void... params) {
                try {
                    AuthedHttp.Reply reply = AuthedHttp.shared().get(PurchaseSender.PATH);
                    return reply.code == 200 ? StoreInfo.parse(reply.body) : null;
                } catch (IOException | RuntimeException e) {
                    RedactingLog.e(TAG, "loading store info failed", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(StoreInfo result) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (result != null) {
                    info = result;
                    infoFailed = false;
                } else if (info == null) {
                    infoFailed = true;
                }
                render();
            }
        }.execute();
    }

    private void render() {
        boolean billingAvailable = productsCode != BillingCodes.BILLING_UNAVAILABLE;
        TextView status = findViewById(R.id.storeStatus);
        if (infoFailed) {
            showStatus(status, R.string.store_load_failed);
        } else if (info == null || !productsLoaded) {
            showStatus(status, R.string.store_loading);
        } else if (!billingAvailable) {
            showStatus(status, R.string.billing_unavailable);
        } else if (prices == null || prices.isEmpty()) {
            showStatus(status, R.string.store_products_unavailable);
        } else {
            status.setVisibility(View.GONE);
        }

        StoreCards cards = info == null ? null : StoreCards.of(info, System.currentTimeMillis());
        TextView until = findViewById(R.id.subscribedUntilText);
        if (cards != null && cards.showSubscribedUntil) {
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(info.subscribedUntil));
            until.setText(getString(R.string.store_subscribed_until, date));
            until.setVisibility(View.VISIBLE);
        } else {
            until.setVisibility(View.GONE);
        }
        boolean yearlyOwned = cards != null && cards.yearlyOwned;
        showView(R.id.yearlyOverlapWarning, cards != null && cards.showYearlyOverlapWarning);
        showView(R.id.yearlyOwnedHint, yearlyOwned);
        showView(R.id.monthPassCard, cards == null || cards.showMonthPass);
        showView(R.id.monthPassStacksNote, cards != null && cards.showMonthPassStacksNote);
        showView(R.id.vacationCard, cards != null && cards.showVacation);

        ((TextView) findViewById(R.id.yearlyPrice)).setText(
                getString(R.string.store_price_per_year, priceOrDash(Products.YEARLY)));
        setUpBuyButton(R.id.buyYearlyButton, Products.YEARLY, getString(R.string.store_buy_yearly), billingAvailable);
        if (yearlyOwned) {
            // Play would answer ITEM_ALREADY_OWNED; the hint points to "Manage subscription".
            findViewById(R.id.buyYearlyButton).setEnabled(false);
        }
        setUpBuyButton(R.id.buyMonthPassButton, Products.MONTH_PASS,
                getString(R.string.store_buy_month_pass, priceOrDash(Products.MONTH_PASS)), billingAvailable);
        setUpBuyButton(R.id.buyVacation10Button, Products.VACATION_10,
                getString(R.string.store_buy_vacation, 10, priceOrDash(Products.VACATION_10)), billingAvailable);
        setUpBuyButton(R.id.buyVacation30Button, Products.VACATION_30,
                getString(R.string.store_buy_vacation, 30, priceOrDash(Products.VACATION_30)), billingAvailable);
        setUpBuyButton(R.id.buyVacation60Button, Products.VACATION_60,
                getString(R.string.store_buy_vacation, 60, priceOrDash(Products.VACATION_60)), billingAvailable);
    }

    private void showStatus(TextView status, int textRes) {
        status.setText(textRes);
        status.setVisibility(View.VISIBLE);
    }

    private void showView(int viewId, boolean visible) {
        findViewById(viewId).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private String priceOrDash(String productId) {
        String price = prices == null ? null : prices.get(productId);
        return price == null || price.isEmpty() ? NO_PRICE : price;
    }

    private void setUpBuyButton(int buttonId, String productId, String label, boolean billingAvailable) {
        Button button = findViewById(buttonId);
        button.setText(label);
        button.setEnabled(StoreCards.buyEnabled(info != null, productsLoaded, billingAvailable,
                prices != null && prices.containsKey(productId)));
    }

    private void buy(String productId) {
        if (info == null) {
            return;
        }
        // When Play's sheet was requested, onPurchasesUpdated shows any error, including one
        // launchBillingFlow returns at once; only a product that is not loaded is reported here.
        if (!billing().launch(this, productId, info.accountId)) {
            billing().showResponseError(BillingCodes.classify(BillingCodes.ITEM_UNAVAILABLE));
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            RedactingLog.e(TAG, "no app opens " + url, e);
        }
    }

    private void openInApp(String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }
}
