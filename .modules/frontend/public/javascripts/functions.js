$(document).ready(function(){

	$('#top .account').hover(function() {
		$(this).addClass('hover');
	}, function(){
		$(this).removeClass('hover');	
	});	
	
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