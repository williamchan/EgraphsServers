/*global describe, it, browser, input*/
describe('Checkout', function() {
  it('should apply all error invalidities', function() {
    browser().navigateTo('http://localhost:9000/Storefront-A/checkout?testcase=all-errors');
    input('recipient.email').enter('recipient@derp.com');
    // input('buyer.email').enter('buyer@derp.com');
    // input('buyer.email').enter('buyer@derp.com');
    // input('shippingAddress.name').enter('Joe Recipient');
    // input('shippingAddress.addressLine1').enter('1414 Church Street');
    // input('shippingAddress.addressLine2').enter('#404');
    // input('shippingAddress.city').enter('Seattle');
    // input('shippingAddress.state').enter('WA');
    // input('shippingAddress.postalCode').enter('11111');
    pause();
  });
});