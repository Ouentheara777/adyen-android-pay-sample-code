#Android Pay

- [Android Pay](#android-pay)
	- [Introduction](#introduction)
	- [Step 1 - Set up the sample and Google Play Services](#step-1-set-up-the-sample-and-google-play-services)
		- [Modify your Manifest](#modify-your-manifest)
	- [Step 2 - Check whether the user is enabled for Android Pay](#step-2-check-whether-the-user-is-enabled-for-android-pay)
	- [Step 3 - Create a Masked Wallet request](#step-3-create-a-masked-wallet-request)
	- [Step 4 - Add a purchase button and request the Masked Wallet](#step-4-add-a-purchase-button-and-request-the-masked-wallet)
	- [Step 5 - Confirm the purchase and set the Masked Wallet](#step-5-confirm-the-purchase-and-set-the-masked-wallet)
	- [Step 6 - Request the Full Wallet](#step-6-request-the-full-wallet)
	- [Step 7 - Retrieve the Full Wallet](#step-7-retrieve-the-full-wallet)
	- [Step 8 - Send the payment token to Adyen for processing](#step-8-send-the-payment-token-to-adyen-for-processing)
	- [Step 9 - Switch to live](#step-9-testing)

##Introduction

The documentation below will describe the Android Pay implementation as per the [Android Pay specifications](https://developers.google.com/android-pay/android/tutorial) of Google. The Adyen specific steps are included in the flow to describe the complete end-to-end integration process.

##Step 1 - Set up the sample and Google Play Services

Add the following dependency to your Gradle build file:

    dependencies {
        compile 'com.google.android.gms:play-services-wallet:8.4.0'
    }

###Modify your Manifest

Before you can use Android Pay in your app, you need to add the following tag to the <application> tag of your `AndroidManifest.xml`:

    <application
        ...
        <!-- Enables the Android Pay API -->
        <meta-data android:name="com.google.android.gms.wallet.api.enabled" android:value="true" />
    </application>

##Step 2 - Check whether the user is enabled for Android Pay

Before starting the Android Pay flow, use the `isReadyToPay` method to check whether the user has the Android Pay app installed and is ready to pay. When this method returns `true`, show the Android Pay button. When it returns `false`, display other checkout options along with text notifying the user to set up the Android Pay app.

    showProgressDialog();
    Wallet.Payments.isReadyToPay(mGoogleApiClient).setResultCallback(
            new ResultCallback<BooleanResult>() {
                @Override
                public void onResult(@NonNull BooleanResult booleanResult) {
                    hideProgressDialog();

                    if (booleanResult.getStatus().isSuccess()) {
                        if (booleanResult.getValue()) {
                            // Show Android Pay buttons and hide regular checkout button
                            // ...
                        } else {
                            // Hide Android Pay buttons, show a message that Android Pay
                            // cannot be used yet, and display a traditional checkout button
                            // ...
                        }
                    } else {
                        // Error making isReadyToPay call
                        Log.e(TAG, "isReadyToPay:" + booleanResult.getStatus());
                    }
                }
            });

##Step 3 - Create a Masked Wallet request

You'll need to create an instance of `MaskedWalletRequest` to invoke the Android Pay API to retrieve the Masked Wallet information (such as shipping address, masked backing instrument number, and cart items). The `MaskedWalletRequest` object must be passed in when you initialize the purchase wallet fragment in the next section.

At this point, you won't have the user's chosen shipping address, so you'll need to create an estimate of the shipping costs and tax. If you set the shopping cart as shown below (highly recommended), make sure the cart total matches the sum of the line items added to the cart.

Here is an example of creating the Masked Wallet request using the builder pattern:

    MaskedWalletRequest request = MaskedWalletRequest.newBuilder()
            .setMerchantName(Constants.MERCHANT_NAME)
            .setPhoneNumberRequired(true)
            .setShippingAddressRequired(true)
            .setCurrencyCode(Constants.CURRENCY_CODE_USD)
            .setEstimatedTotalPrice(cartTotal)
                    // Create a Cart with the current line items. Provide all the information
                    // available up to this point with estimates for shipping and tax included.
            .setCart(Cart.newBuilder()
                    .setCurrencyCode(Constants.CURRENCY_CODE_USD)
                    .setTotalPrice(cartTotal)
                    .setLineItems(lineItems)
                    .build())
            .setPaymentMethodTokenizationParameters(parameters)
            .build();

This example demonstrates how to receive encrypted Android Pay payment credentials. Set the tokenization type and add a publicKey parameter as shown:

    PaymentMethodTokenizationParameters parameters =
            PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
                .addParameter("publicKey", publicKey)
                .build();


The `publicKey` can be retrieved from Adyen backoffice. If you need help in retrieving the `publicKey` please contact Adyen support at support@adyen.com.

Until the test environment of Adyen is available, we advice to use the Google public test key for making the masked wallet request. This can be retrieved from the [Android Pay github of Google](https://github.com/android-pay/androidpay-quickstart/blob/master/app/src/main/res/values/ids.xml).

>Note:
>After finalizing the testing phase, another 'publicKey' needs to be retrieved for the switch to live.

## Step 4 - Add a purchase button and request the Masked Wallet

Next, construct an instance of WalletFragment to add to your checkout activity:

    WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
            .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
            .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
            .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);

    WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
            .setEnvironment(Constants.WALLET_ENVIRONMENT)
            .setFragmentStyle(walletFragmentStyle)
            .setTheme(WalletConstants.THEME_LIGHT)
            .setMode(WalletFragmentMode.BUY_BUTTON)
            .build();
    mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);

When you initialize the purchase fragment, pass in the `maskedWalletRequest` that you created in the previous step, as well as the code `REQUEST_CODE_MASKED_WALLET` used to uniquely identify this call in the `onActivityResult`() callback:

    WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
            .setMaskedWalletRequest(maskedWalletRequest)
            .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET)
            .setAccountName(accountName);
    mWalletFragment.initialize(startParamsBuilder.build());

    // add Wallet fragment to the UI
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.dynamic_wallet_button_fragment, mWalletFragment)
            .commit();

When the user clicks the buy button, the Masked Wallet is retrieved and returned in the `onActivityResult` of the enclosing activity as shown below. If the user has not authorized this app to use their payment information or future purchases, Android Pay presents a chooser dialog, handles preauthorization, and returns control to the app.

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // retrieve the error code, if available
        int errorCode = -1;
        if (data != null) {
            errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
        }
        switch (requestCode) {
            case REQUEST_CODE_MASKED_WALLET:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            MaskedWallet maskedWallet =
                                    data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                            launchConfirmationPage(maskedWallet);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;
            case WalletConstants.RESULT_ERROR:
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

##Step 5 - Confirm the purchase and set the Masked Wallet

After the app obtains the Masked Wallet, it should present a confirmation page showing the total cost of the items purchased in the transaction.

    WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
            .setEnvironment(Constants.WALLET_ENVIRONMENT)
            .setFragmentStyle(walletFragmentStyle)
            .setTheme(WalletConstants.THEME_LIGHT)
            .setMode(WalletFragmentMode.SELECTION_DETAILS)
            .build();
    mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);

At this point the app has the shipping address and billing address, so you can calculate the exact total purchase price and display it. This activity also allows the user to change the Android Pay payment instrument and change the shipping address for the purchase.

##Step 6 - Request the Full Wallet
When the user confirms the order, you are ready to request the Full Wallet. The Full Wallet Request should have the total charge that you are requesting including exact shipping, handling and tax. You must include the `GoogleTransactionId` that you received in the Masked Wallet response.

Create a `FullWalletRequest` object that contains the various line items (including tax and shipping if necessary) and a Cart object.

    FullWalletRequest request = FullWalletRequest.newBuilder()
            .setGoogleTransactionId(googleTransactionId)
            .setCart(Cart.newBuilder()
                    .setCurrencyCode(Constants.CURRENCY_CODE_USD)
                    .setTotalPrice(cartTotal)
                    .setLineItems(lineItems)
                    .build())
            .build();

## Step 7 - Retrieve the Full Wallet

Once you have constructed a `FullWalletRequest`, request the `FullWallet` object using `Wallet.Payments.loadFullWallet(...)`. Before you can call this method you'll need to construct a `GoogleApiClient` in the `onCreate` method of your Activity.

    mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
            .enableAutoManage(fragmentActivity, this /* onConnectionFailedListener */)
            .setAccountName(accountName) // optional
            .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                    .setEnvironment(Constants.WALLET_ENVIRONMENT)
                    .setTheme(WalletConstants.THEME_LIGHT)
                    .build())
            .build();

The value of the Environment parameter in `WalletOptions` indicates whether the server is running in a production or test environment. Its value can be `WalletConstants.ENVIRONMENT_PRODUCTION` or `WalletConstants.ENVIRONMENT_TEST.` For testing and development always use only the test environment.

Once you have constructed the full wallet request instance, call the `loadFullWallet` method. When the full wallet request is completed, Google calls the `onActivityResult` method, passing the intent result.

    Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET);

Once you have retrieved the Full Wallet in the `onActivityResult()` callback, you have enough information to proceed to payment processing for this transaction. You can access the details of the payment credentials from the Full Wallet as shown below:

    // Get payment method token
    PaymentMethodToken token = fullWallet.getPaymentMethodToken();

    // Get the JSON of the token object as a String
    String tokenJSON = token.getToken();

## Step 8 - Send the payment token to Adyen for processing (test)

Based on the Full Wallet retrieved in step 7 you will build an object structure to be sent to your merchant server.
On your merchant server,  convert the object structure received based on the Full Wallet data to a JSON object with the following structure:

    {  
       "additionalData":{  
          "androidpay.token":{  
             "ephemeralPublicKey":"==Android Pay Specific==",
             "encryptedMessage":"==Android Pay Specific==",
             "tag":"==Android Pay Specific=="
          }
       },
       "amount":{  
          "currency":"USD",
          "amount":"100"
       },
       "merchantReference":"",
       "shopperEmail":"shopper_email@domain.com",
       "deliveryAddress":{  
          "street":"274 Brannan Street",
          "houseNoOrName":"600",
          "city":"San Francisco",
          "postalCode":"94107",
          "stateOrProvince":"CA",
          "country":"US"
       },
       "billingAddress":{  
          "street":"274 Brannan Street",
          "houseNoOrName":"600",
          "city":"San Francisco",
          "postalCode":"94107",
          "stateOrProvince":"CA",
          "country":"US"
       }
    }

> Notes:

> - The `androidpay.token` can be retrieved from the Full Wallet by calling `fullWallet.getPaymentMethodToken().getToken()`
> - The `androidpay.token` is a JSON token in Base64 encoded format.
> - The amount is in minor units.

Please find the full code for the demo merchant server in this repository.

Send the test tokens to:

    https://pal-test.adyen.com/pal/servlet/Payment/V12/authorise

#### Test Cards and Custom Androidpay Tokens
Since you can only board real Credit Cards on Androidpay and Google uses their own test cards, payments sent to the Adyen test platform will receive the message:

"Refused (This is not a testCard)"

If you want to test payments that will not be refused, you can use the AndroidPayTokenCreator to create custom Androidpay tokens. Just be sure to replace the dpan field (Credit Card number) with a test card number that is recognized by our test platform.

We will support your testing activities from March 7 onwards. Please contact Adyen Support for more information.
>Note:
>If you are familiar with the Adyen Apple Pay integration this step will be the same and you will receive the same responses from our back-office platform.

## Step 9 - Switch to live

Once successfully completed the testing with the servers of Google and Adyen, you are ready for the switch to live.

Contact Adyen Support to request the 'publicKey' for live payment processing. Also review the [Android Pay setup guidelines] (https://developers.google.com/android-pay/android/tutorial) for obtaining live credentials of your application.

We will support your live testing activities from March 9 onwards. Please contact Adyen Support for more information.
