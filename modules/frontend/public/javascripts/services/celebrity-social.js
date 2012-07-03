/* Scripting shared between both celebrity storefront pages */
define(["libs/jquery.tweet"], function() {
  var filterOutReplies = function(t){
    var matches = /^@\w+/.test(t['tweet_raw_text']);
    return !matches;
  };

  /**
   * Populates the tweets ABOUT a celebrity inside '.recent-tweets.tweets'.
   * This should really take a selector.
   */
  var populateTweetsOfCelebWithoutUsername = function(celebName) {
    $(".recent-tweets .tweets").tweet({
      query: celebName,
      loading_text: "Loading tweets...",
      filter: filterOutReplies,
      fetch: 20,
      template: "<!-- {avatar} --> <span class='tweet-heading'><strong>{user}</strong></span> {text} {time}"
    });
  };

  /**
   * Populates the tweet's of a celebrity inside '.recent-tweets .tweets'.
   * This should really take a selector.
   */
  var populateTweetsOfCelebWithUsername= function(celebTwitterInfo) {
    $(".recent-tweets .tweets").tweet({
      username: celebTwitterInfo.handle,
      filter: filterOutReplies,
      avatar_size: 40,
      fetch: 20, // the last twenty tweets should contain at least 3 that are not @replies ;-)
      count: 3,
      loading_text: "Loading tweets...",
      template: "<!-- {avatar} --> <span class='tweet-heading'><strong>" +
                     celebTwitterInfo.name +
                     "</strong> @" +
                     celebTwitterInfo.handle +
                     "</span> {text} {time}"
    });
  };

  return {
    /**
     * Populates a celebrity's tweets inside of the given selector
     * @param selector the selectors to fill with the celebrity's tweets
     * @param celebTwitterInfo an object with a .name and .handle field, that correspond
     *   to the celebrity's plain name and twitter handle.
     **/
    populateCelebrityTweets: function(selector, celebTwitterInfo) {
      if (celebTwitterInfo.handle === "") {
        populateTweetsOfCelebWithoutUsername(celebTwitterInfo.name);
      } else {
        populateTweetsOfCelebWithUsername(celebTwitterInfo);
      }
    }
  };
});