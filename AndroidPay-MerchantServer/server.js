var express = require('express');
var bodyParser = require('body-parser');
var request = require('request');

var app = express();

app.use(bodyParser.json());

/**
 * This URL is called once the FullWallet is retrieved and a JSON
 * object is created from it and sent to the merchant server
 */
app.post('/api/payment', function(req, res) {

    var paymentData = req.body;
    console.log('Payment', req.body);
    
    createAdyenRequest(paymentData, function(adyenRequest, error) {
        if (adyenRequest && adyenRequest.pspReference && adyenRequest.resultCode == 'Authorised') {
            return res.json(adyenRequest);
        }
        res.status(500).end();
    });

});

app.listen(8080);

/**
 * This method creates the Adyen request object from the payment data
 * received from the client application
 */
function createAdyenRequest(paymentData, callback) {
    var token = paymentData.additionalData.token;
    
    if (!token || token.length == 0) {
        console.log('No token found');
    }
    
    var amountMinorUnits = String((paymentData.amount * 100).toFixed(0));
    
    /**
     * Creating the Adyen Request object
     */
    var data = {
        additionalData: {
            'androidpay.token': token
        },
        amount: {
            currency: paymentData.currencyCode,
            value: amountMinorUnits
        },
        merchantAccount: 'TestMerchantAP',
        reference: paymentData.merchantReference,
        shopperEmail: paymentData.shopperEmail,
        deliveryAddress: {
            street: paymentData.deliveryAddress.street,
            houseNoOrName: paymentData.deliveryAddress.houseNoOrName,
            city: paymentData.deliveryAddress.city,
            postalCode: paymentData.deliveryAddress.postalCode,
            stateOrProvince: paymentData.deliveryAddress.stateOrProvince,
            country: paymentData.deliveryAddress.country
        },
        billingAddress: {
            street: paymentData.billingAddress.street,
            houseNoOrName: paymentData.billingAddress.houseNoOrName,
            city: paymentData.billingAddress.city,
            postalCode: paymentData.billingAddress.postalCode,
            stateOrProvince: paymentData.billingAddress.stateOrProvince,
            country: paymentData.billingAddress.country
        }
    }
    
    var auth = {user: "", pass: ""};
    
    // var url = 'https://pal-test.adyen.com/pal/servlet/Payment/V12/authorise'
    
    // request.post({url: url, auth: auth, json: data}, function (error, response, body){
    //     console.log('Adyen response: ', response.statusCode, body, error);
       
    //     if (!error && response.statusCode == 200) {
    //         callback(body, null);
    //     } else {
    //         callback(null, error);
    //     } 
    // });
}