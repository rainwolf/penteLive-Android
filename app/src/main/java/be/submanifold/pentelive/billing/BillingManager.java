package be.submanifold.pentelive.billing;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.submanifold.pentelive.BackgroundTask;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.R;
import be.submanifold.pentelive.net.AuthedHttp;
import be.submanifold.pentelive.net.Forms;

/**
 * The app's one Play Billing client (spec 3.2), owned by MyApplication. It is the only class
 * that touches Play Billing types; every decision is delegated to the Play-free classes in
 * this package. Call every public method on the main thread; listeners run on the main thread.
 * The client never acknowledges or consumes: the server does both after verifying.
 */
public final class BillingManager implements PurchasesUpdatedListener {

    /** Result of {@link #loadProducts}. */
    public interface ProductsListener {
        /**
         * @param responseCode a {@link BillingCodes} response code; OK when at least one
         *                     product loaded
         * @param prices       Play's formatted price per product ID that can be bought now
         *                     (the yearly price is per year); empty when none
         */
        void onProducts(int responseCode, Map<String, String> prices);
    }

    private interface ReadyListener {
        void onReady(int responseCode);
    }

    private final Context appContext;
    private final BillingClient client;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final PurchaseSender sender = new PurchaseSender((productId, purchaseToken) -> {
        AuthedHttp.Reply reply = AuthedHttp.shared().postForm(PurchaseSender.PATH,
                Forms.encode("productId", productId, "purchaseToken", purchaseToken));
        return reply.code == 200 ? reply.body : null;
    });
    private final List<ReadyListener> waitingForConnection = new ArrayList<>();
    private boolean connecting;
    /** Tokens the server answered "success:shared" or "invalid receipt" for in this process. */
    private final Set<String> rejectedTokens = new HashSet<>();
    private final Map<String, ProductDetails> productDetails = new HashMap<>();
    private final Map<String, String> offerTokens = new HashMap<>();
    private WeakReference<Activity> host = new WeakReference<>(null);
    private Runnable entitlementListener;

    public BillingManager(Context context) {
        appContext = context.getApplicationContext();
        client = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
    }

    /** Makes {@code activity} the one that shows billing dialogs. Call from onResume. */
    public void attach(Activity activity) {
        host = new WeakReference<>(activity);
    }

    /** Stops using {@code activity} for dialogs. Call from onDestroy. */
    public void detach(Activity activity) {
        if (host.get() == activity) {
            host = new WeakReference<>(null);
        }
    }

    /** Runs {@code listener} after the server granted or confirmed an entitlement. */
    public void setEntitlementListener(Runnable listener) {
        entitlementListener = listener;
    }

    /** Removes {@code listener} if it is still the current one. */
    public void clearEntitlementListener(Runnable listener) {
        if (entitlementListener == listener) {
            entitlementListener = null;
        }
    }

    /** Queries the yearly subscription and the one-time products, and keeps them for {@link #launch}. */
    public void loadProducts(ProductsListener listener) {
        whenReady(code -> {
            if (code != BillingCodes.OK) {
                listener.onProducts(code, Collections.emptyMap());
                return;
            }
            productDetails.clear();
            offerTokens.clear();
            Map<String, String> prices = new HashMap<>();
            int[] remaining = {2};
            int[] firstError = {BillingCodes.OK};
            ProductDetailsResponseListener onResult = (result, queryResult) -> {
                int resultCode = result.getResponseCode();
                List<ProductDetails> found = queryResult == null ? null : queryResult.getProductDetailsList();
                main.post(() -> {
                    if (resultCode == BillingCodes.OK && found != null) {
                        for (ProductDetails details : found) {
                            addProduct(details, prices);
                        }
                    } else if (firstError[0] == BillingCodes.OK) {
                        firstError[0] = resultCode;
                    }
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        listener.onProducts(prices.isEmpty() ? firstError[0] : BillingCodes.OK, prices);
                    }
                });
            };
            client.queryProductDetailsAsync(productQuery(Products.SUBS, BillingClient.ProductType.SUBS), onResult);
            client.queryProductDetailsAsync(productQuery(Products.INAPP, BillingClient.ProductType.INAPP), onResult);
        });
    }

    /**
     * Opens Play's purchase sheet for a product {@link #loadProducts} returned, tagged with
     * the server's account id. Returns a {@link BillingCodes} response code; OK means the
     * sheet opened and the result arrives in {@link #onPurchasesUpdated}.
     */
    public int launch(Activity activity, String productId, String accountId) {
        ProductDetails details = productDetails.get(productId);
        String offerToken = offerTokens.get(productId);
        if (details == null || (Products.isSubscription(productId) && offerToken == null)) {
            return BillingCodes.ITEM_UNAVAILABLE;
        }
        BillingFlowParams.ProductDetailsParams.Builder product =
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details);
        if (offerToken != null) {
            product.setOfferToken(offerToken);
        }
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(product.build()))
                .setObfuscatedAccountId(accountId)
                .build();
        return client.launchBillingFlow(activity, params).getResponseCode();
    }

    /**
     * Sends purchases that still need the server: {@code explicit=false} from
     * MainActivity.onResume, {@code explicit=true} from "Restore purchases".
     */
    public void resync(boolean explicit) {
        whenReady(code -> {
            if (code != BillingCodes.OK) {
                if (explicit) {
                    showResponseError(BillingCodes.classify(code));
                }
                return;
            }
            List<PurchaseRecord> all = new ArrayList<>();
            int[] remaining = {2};
            boolean[] failed = {false};
            PurchasesResponseListener onResult = (result, purchases) -> {
                int resultCode = result.getResponseCode();
                List<PurchaseRecord> records = toRecords(purchases);
                main.post(() -> {
                    if (resultCode == BillingCodes.OK) {
                        all.addAll(records);
                    } else {
                        failed[0] = true;
                    }
                    remaining[0]--;
                    if (remaining[0] > 0) {
                        return;
                    }
                    List<PurchaseRecord> selected = ResyncSelector.forResync(all, explicit, rejectedTokens);
                    if (failed[0] && selected.isEmpty()) {
                        if (explicit) {
                            showResponseError(BillingCodes.Outcome.ERROR);
                        }
                        return;
                    }
                    if (selected.isEmpty() && !explicit) {
                        return;
                    }
                    sendInBackground(selected, explicit, false);
                });
            };
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS).build(), onResult);
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP).build(), onResult);
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        int code = result.getResponseCode();
        List<PurchaseRecord> records = toRecords(purchases);
        main.post(() -> {
            BillingCodes.Outcome outcome = BillingCodes.classify(code);
            if (outcome != BillingCodes.Outcome.PROCEED) {
                showResponseError(outcome);
                if (outcome == BillingCodes.Outcome.ALREADY_OWNED) {
                    // Often an earlier one-time purchase the server has not consumed yet:
                    // send it now so the server grants and consumes it.
                    resync(false);
                }
                return;
            }
            List<PurchaseRecord> selected = ResyncSelector.forUpdate(records);
            if (!selected.isEmpty()) {
                sendInBackground(selected, false, true);
            }
        });
    }

    /** Shows the message for a failed Play call; nothing for PROCEED or SILENT (user cancelled). */
    public void showResponseError(BillingCodes.Outcome outcome) {
        switch (outcome) {
            case ALREADY_OWNED:
                show(R.string.billing_already_owned);
                break;
            case BILLING_UNAVAILABLE:
                show(R.string.billing_unavailable);
                break;
            case ERROR:
                show(R.string.billing_error);
                break;
            default:
                break;
        }
    }

    private void whenReady(ReadyListener listener) {
        if (client.isReady()) {
            listener.onReady(BillingCodes.OK);
            return;
        }
        waitingForConnection.add(listener);
        if (connecting) {
            return;
        }
        connecting = true;
        startConnecting(true);
    }

    private void startConnecting(boolean retryOnDeveloperError) {
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                int code = result.getResponseCode();
                main.post(() -> {
                    if (code == BillingClient.BillingResponseCode.DEVELOPER_ERROR && retryOnDeveloperError) {
                        // Most likely enableAutoServiceReconnection is already reconnecting
                        // ("already connecting"): look again once, a second later.
                        main.postDelayed(() -> {
                            if (!connecting) {
                                return;
                            }
                            if (client.isReady()) {
                                finishConnecting(BillingCodes.OK);
                            } else {
                                startConnecting(false);
                            }
                        }, 1000);
                        return;
                    }
                    finishConnecting(code);
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                // enableAutoServiceReconnection reconnects on the next call; fail what waits now.
                main.post(() -> finishConnecting(BillingCodes.SERVICE_DISCONNECTED));
            }
        });
    }

    private void finishConnecting(int code) {
        connecting = false;
        List<ReadyListener> waiting = new ArrayList<>(waitingForConnection);
        waitingForConnection.clear();
        for (ReadyListener listener : waiting) {
            listener.onReady(code);
        }
    }

    private static QueryProductDetailsParams productQuery(List<String> productIds, String productType) {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String productId : productIds) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build());
        }
        return QueryProductDetailsParams.newBuilder().setProductList(products).build();
    }

    private void addProduct(ProductDetails details, Map<String, String> prices) {
        String productId = details.getProductId();
        if (Products.isSubscription(productId)) {
            List<OfferPicker.OfferRef> offers = new ArrayList<>();
            List<ProductDetails.SubscriptionOfferDetails> subscriptionOffers = details.getSubscriptionOfferDetails();
            if (subscriptionOffers != null) {
                for (ProductDetails.SubscriptionOfferDetails offer : subscriptionOffers) {
                    String price = null;
                    if (offer.getPricingPhases() != null) {
                        List<ProductDetails.PricingPhase> phases = offer.getPricingPhases().getPricingPhaseList();
                        if (phases != null && !phases.isEmpty()) {
                            price = phases.get(phases.size() - 1).getFormattedPrice();
                        }
                    }
                    offers.add(new OfferPicker.OfferRef(offer.getBasePlanId(), offer.getOfferId(),
                            offer.getOfferToken(), price));
                }
            }
            OfferPicker.OfferRef chosen = OfferPicker.pick(offers, Products.YEARLY_BASE_PLAN);
            if (chosen == null) {
                return;
            }
            productDetails.put(productId, details);
            offerTokens.put(productId, chosen.offerToken);
            prices.put(productId, chosen.formattedPrice == null ? "" : chosen.formattedPrice);
        } else if (Products.isKnown(productId)) {
            ProductDetails.OneTimePurchaseOfferDetails offer = details.getOneTimePurchaseOfferDetails();
            if (offer == null) {
                List<ProductDetails.OneTimePurchaseOfferDetails> offers = details.getOneTimePurchaseOfferDetailsList();
                if (offers == null || offers.isEmpty()) {
                    return;
                }
                offer = offers.get(0);
            }
            productDetails.put(productId, details);
            if (offer.getOfferToken() != null) {
                offerTokens.put(productId, offer.getOfferToken());
            }
            prices.put(productId, offer.getFormattedPrice() == null ? "" : offer.getFormattedPrice());
        }
    }

    private static List<PurchaseRecord> toRecords(List<Purchase> purchases) {
        List<PurchaseRecord> records = new ArrayList<>();
        if (purchases == null) {
            return records;
        }
        for (Purchase purchase : purchases) {
            List<String> products = purchase.getProducts();
            if (products == null || products.isEmpty()) {
                continue;
            }
            records.add(new PurchaseRecord(products.get(0), purchase.getPurchaseToken(),
                    purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED,
                    purchase.isAcknowledged()));
        }
        return records;
    }

    private void sendInBackground(List<PurchaseRecord> purchases, boolean explicit, boolean fromPurchaseFlow) {
        new BackgroundTask<Void, PurchaseSender.Outcome>() {
            @Override
            protected PurchaseSender.Outcome doInBackground(Void... params) {
                return sender.sendAll(purchases, explicit, fromPurchaseFlow);
            }

            @Override
            protected void onPostExecute(PurchaseSender.Outcome outcome) {
                present(outcome);
            }
        }.execute();
    }

    private void present(PurchaseSender.Outcome outcome) {
        rejectedTokens.addAll(outcome.rejectedTokens);
        for (ReplyMessage message : outcome.messages) {
            int res = messageRes(message);
            if (res != 0) {
                show(res);
            }
        }
        if (outcome.reloadPlayer) {
            PentePlayer.reloadSubscriberFlags();
            if (entitlementListener != null) {
                entitlementListener.run();
            }
        }
    }

    private static int messageRes(ReplyMessage message) {
        switch (message) {
            case THANK_YOU:
                return R.string.billing_thank_you;
            case LINKED_TO_OTHER_ACCOUNT:
                return R.string.billing_linked_other_account;
            case VACATION_REFUNDED:
                return R.string.billing_vacation_refunded;
            case NO_VALID_PURCHASE:
                return R.string.billing_no_valid_purchase;
            case NO_PURCHASES_TO_RESTORE:
                return R.string.billing_nothing_to_restore;
            case RESTORE_FAILED:
                return R.string.billing_error;
            default:
                return 0;
        }
    }

    private void show(int messageRes) {
        Activity activity = host.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            new AlertDialog.Builder(activity)
                    .setMessage(messageRes)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else {
            Toast.makeText(appContext, messageRes, Toast.LENGTH_LONG).show();
        }
    }
}
