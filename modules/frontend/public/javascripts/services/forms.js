/* Scripting shared between forms */
define(["libs/jquery.watermark.min", "libs/iphonecheckboxes/iphone-style-checkboxes", "libs/twitter-bootstrap/bootstrap-alert"], function() {
  return {
    
    /** 
     * Allows HTML within placeholder text
     * @param selector the selectors to use
     * @param text the HTML placeholder text
     * @param params custom parameters for placeholder text (e.g. {color: '#333', left: -2, top: 10, fallback: true})
     **/
    setDefaultText: function(selector, text, params) {
      $.watermarker.setDefaults({color: '#b3b3b3', fallback: false});
      $(selector).watermark(text, params);
    },
    
    /** 
     * Creates iPhone style checkboxes
     * @param selector the selectors to use
     * @param params custom parameters for placeholder text (e.g. { checkedLabel: 'YES', uncheckedLabel: 'NO'})
     **/    
    setIphoneCheckbox: function(selector, params) {
      $(selector).iphoneStyle();
    },

    /** 
     * Creates closeable alert dialogues
     * @param selector the selectors to use
     **/    
    setAlert: function(selector) {
      $(selector).alert();
    }
    
  };
});