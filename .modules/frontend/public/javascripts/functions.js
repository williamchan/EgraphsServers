$(document).ready(function(){
	
	$('#top .account-options a:first').click(function(e) {
	
		var this_anchor = $(this);
	
		$('body').one('click',function(){
			this_anchor.parent().removeClass('active');
		});

		this_anchor.parent().addClass('active');
	
		e.stopPropagation();	
		e.preventDefault();
	
	});
	
});