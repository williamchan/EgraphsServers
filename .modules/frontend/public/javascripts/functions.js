$(document).ready(function(){
	
	$('#top .account').click(function(e) {
	
		var account_options = $(this).find('.account-options');
	
		$('body').one('click',function(){
			account_options.removeClass('active');
		});

		account_options.addClass('active');
	
		e.stopPropagation();	
		e.preventDefault();
	
	});
	
});